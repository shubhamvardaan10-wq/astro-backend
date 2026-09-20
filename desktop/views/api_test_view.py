"""
API Test Suite Integration View for Astro Desktop Application.
Runs all 64+ platform endpoints with real-time feedback, category sweeps, and deep JSON payload inspection.
Uses thread-safe queue event dispatcher for robust cross-thread UI synchronization.
"""
import json
import queue
import threading
import tkinter as tk
import customtkinter as ctk
from typing import List, Dict, Any, Optional
from ..api_catalog import ENDPOINTS, API_CATEGORIES
from ..api_client import AstroApiClient, ApiResponse
from ..config import COLORS

class ApiTestView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self.all_endpoints = list(ENDPOINTS)
        self.test_results: Dict[str, ApiResponse] = {}
        self.is_running = False
        self.stop_requested = False
        self.selected_endpoint_id: Optional[str] = None
        self.row_widgets: Dict[str, Dict[str, Any]] = {}
        self.ui_queue = queue.Queue()

        self._build_ui()
        self._populate_test_rows()
        self._poll_queue()

    def _poll_queue(self):
        try:
            while True:
                item = self.ui_queue.get_nowait()
                msg_type, payload = item
                if msg_type == "RUNNING":
                    ep_id = payload
                    if ep_id in self.row_widgets:
                        self.row_widgets[ep_id]["badge_label"].configure(text="RUN...", text_color=COLORS["accent_gold"])
                elif msg_type == "RESULT":
                    res, progress_val = payload
                    self._update_row_ui(res)
                    self.progress_bar.set(progress_val)
                elif msg_type == "SINGLE_RESULT":
                    res = payload
                    self._update_row_ui(res)
                elif msg_type == "COMPLETE":
                    self._on_suite_complete()
        except queue.Empty:
            pass
        except Exception as e:
            print(f"[ApiTestView] Queue dispatch error: {e}")
        finally:
            self.after(25, self._poll_queue)

    def _build_ui(self):
        # ── Header & Action Controls ──────────────────────────────────────────
        header_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        header_card.pack(fill="x", padx=16, pady=(12, 8))

        top_row = ctk.CTkFrame(header_card, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=12)

        # Title & Subtitle
        title_box = ctk.CTkFrame(top_row, fg_color="transparent")
        title_box.pack(side="left", fill="y")
        ctk.CTkLabel(
            title_box,
            text="API Integration Test Suite",
            font=ctk.CTkFont(size=20, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        ctk.CTkLabel(
            title_box,
            text=f"Direct Ingress & Microservices Verification Engine ({len(self.all_endpoints)} Endpoints Registered)",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        # Live Metrics Counter
        self.metrics_box = ctk.CTkFrame(top_row, fg_color="transparent")
        self.metrics_box.pack(side="right", fill="y")

        self.lbl_total = self._create_metric_pill(self.metrics_box, "TOTAL", str(len(self.all_endpoints)), COLORS["text_secondary"])
        self.lbl_passed = self._create_metric_pill(self.metrics_box, "PASSED", "0", COLORS["success"])
        self.lbl_failed = self._create_metric_pill(self.metrics_box, "FAILED", "0", COLORS["danger"])
        self.lbl_avg_ms = self._create_metric_pill(self.metrics_box, "AVG LATENCY", "0 ms", COLORS["accent_cyan"])

        # Control Toolbar Row
        controls_row = ctk.CTkFrame(header_card, fg_color="transparent")
        controls_row.pack(fill="x", padx=16, pady=(0, 12))

        self.btn_run_all = ctk.CTkButton(
            controls_row,
            text="🚀 Run All Tests (64)",
            fg_color=COLORS["accent_gold"],
            text_color="#000000",
            hover_color="#D9941C",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._start_run_all,
            width=160,
            height=34
        )
        self.btn_run_all.pack(side="left", padx=(0, 8))

        self.category_var = ctk.StringVar(value="All")
        self.category_dropdown = ctk.CTkOptionMenu(
            controls_row,
            values=API_CATEGORIES,
            variable=self.category_var,
            fg_color=COLORS["entry_bg"],
            button_color=COLORS["card_border"],
            text_color=COLORS["text_primary"],
            width=180,
            height=34,
            command=self._on_category_selected
        )
        self.category_dropdown.pack(side="left", padx=(0, 8))

        self.btn_run_category = ctk.CTkButton(
            controls_row,
            text="⚡ Run Category",
            fg_color=COLORS["accent_purple"],
            hover_color="#7C3AED",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._start_run_category,
            width=130,
            height=34
        )
        self.btn_run_category.pack(side="left", padx=(0, 8))

        self.btn_stop = ctk.CTkButton(
            controls_row,
            text="⏹ Stop",
            fg_color="#374151",
            hover_color=COLORS["danger"],
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._request_stop,
            width=80,
            height=34,
            state="disabled"
        )
        self.btn_stop.pack(side="left", padx=(0, 12))

        # Search box
        self.search_var = ctk.StringVar()
        self.search_var.trace_add("write", self._on_search_filter)
        self.search_entry = ctk.CTkEntry(
            controls_row,
            placeholder_text="🔍 Filter endpoints...",
            textvariable=self.search_var,
            fg_color=COLORS["entry_bg"],
            border_color=COLORS["entry_border"],
            width=200,
            height=34
        )
        self.search_entry.pack(side="right", padx=(8, 0))

        # Progress bar
        self.progress_bar = ctk.CTkProgressBar(
            header_card,
            fg_color="#1E2A47",
            progress_color=COLORS["accent_cyan"],
            height=6
        )
        self.progress_bar.set(0)
        self.progress_bar.pack(fill="x", padx=16, pady=(0, 8))

        # ── Body: Split View (Left: Test List | Right: Inspector) ──────────────
        split_frame = ctk.CTkFrame(self, fg_color="transparent")
        split_frame.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        # Left: Scrollable List of Endpoints
        left_container = ctk.CTkFrame(split_frame, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        left_container.pack(side="left", fill="both", expand=True, padx=(0, 8))

        list_header = ctk.CTkFrame(left_container, fg_color="transparent", height=32)
        list_header.pack(fill="x", padx=12, pady=(10, 4))
        ctk.CTkLabel(list_header, text="STATUS", width=70, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(list_header, text="METHOD", width=65, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(list_header, text="ENDPOINT / ACTION", anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", fill="x", expand=True)
        ctk.CTkLabel(list_header, text="HTTP", width=55, anchor="center", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(list_header, text="LATENCY", width=75, anchor="e", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(list_header, text="", width=60).pack(side="left")

        self.scroll_list = ctk.CTkScrollableFrame(left_container, fg_color="transparent")
        self.scroll_list.pack(fill="both", expand=True, padx=4, pady=4)

        # Right: JSON Inspector & Details Pane
        right_container = ctk.CTkFrame(split_frame, fg_color=COLORS["card_bg"], width=460, corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        right_container.pack(side="right", fill="both", padx=(8, 0))
        right_container.pack_propagate(False)

        inspector_header = ctk.CTkFrame(right_container, fg_color="transparent")
        inspector_header.pack(fill="x", padx=14, pady=(12, 6))
        ctk.CTkLabel(
            inspector_header,
            text="Payload Inspector",
            font=ctk.CTkFont(size=16, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(side="left")

        # Copy Curl button
        self.btn_copy_curl = ctk.CTkButton(
            inspector_header,
            text="📋 Copy curl",
            width=90,
            height=26,
            fg_color="#1E2A47",
            hover_color=COLORS["accent_cyan"],
            font=ctk.CTkFont(size=11),
            command=self._copy_curl_to_clipboard
        )
        self.btn_copy_curl.pack(side="right")

        # Metadata banner
        self.detail_banner = ctk.CTkLabel(
            right_container,
            text="Select an endpoint on the left to inspect HTTP request & response.",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"],
            wraplength=420,
            justify="left"
        )
        self.detail_banner.pack(fill="x", padx=14, pady=(0, 8))

        # Inspector Tabs: Response Data vs Request Payload
        self.inspector_tabview = ctk.CTkTabview(
            right_container,
            fg_color="transparent",
            segmented_button_fg_color=COLORS["entry_bg"],
            segmented_button_selected_color=COLORS["accent_purple"],
            segmented_button_selected_hover_color="#7C3AED"
        )
        self.inspector_tabview.pack(fill="both", expand=True, padx=12, pady=(0, 12))

        tab_response = self.inspector_tabview.add("Response JSON")
        tab_request = self.inspector_tabview.add("Request Body")
        tab_headers = self.inspector_tabview.add("Meta & Headers")

        # Response text box
        self.txt_response = ctk.CTkTextbox(
            tab_response,
            font=ctk.CTkFont(family="Consolas", size=11),
            fg_color=COLORS["entry_bg"],
            text_color=COLORS["text_primary"],
            wrap="none"
        )
        self.txt_response.pack(fill="both", expand=True, padx=4, pady=4)

        # Request text box
        self.txt_request = ctk.CTkTextbox(
            tab_request,
            font=ctk.CTkFont(family="Consolas", size=11),
            fg_color=COLORS["entry_bg"],
            text_color=COLORS["text_primary"],
            wrap="none"
        )
        self.txt_request.pack(fill="both", expand=True, padx=4, pady=4)

        # Meta text box
        self.txt_meta = ctk.CTkTextbox(
            tab_headers,
            font=ctk.CTkFont(family="Consolas", size=11),
            fg_color=COLORS["entry_bg"],
            text_color=COLORS["text_primary"],
            wrap="none"
        )
        self.txt_meta.pack(fill="both", expand=True, padx=4, pady=4)

    def _create_metric_pill(self, parent, label: str, value: str, color: str):
        pill = ctk.CTkFrame(parent, fg_color=COLORS["card_border"], corner_radius=6)
        pill.pack(side="left", padx=4)
        ctk.CTkLabel(pill, text=label, font=ctk.CTkFont(size=9, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", padx=(8, 4), pady=4)
        val_lbl = ctk.CTkLabel(pill, text=value, font=ctk.CTkFont(size=12, weight="bold"), text_color=color)
        val_lbl.pack(side="left", padx=(0, 8), pady=4)
        return val_lbl

    def _populate_test_rows(self):
        for child in self.scroll_list.winfo_children():
            child.destroy()
        self.row_widgets.clear()

        query = self.search_var.get().strip().lower()
        selected_cat = self.category_var.get()

        for ep in self.all_endpoints:
            if selected_cat != "All" and ep.get("category") != selected_cat:
                continue
            if query:
                matches_name = query in ep.get("name", "").lower()
                matches_cat = query in ep.get("category", "").lower()
                matches_url = query in ep.get("url", "").lower()
                if not (matches_name or matches_cat or matches_url):
                    continue

            ep_id = ep["id"]
            res = self.test_results.get(ep_id)

            row = ctk.CTkFrame(self.scroll_list, fg_color=COLORS["entry_bg"], corner_radius=6, height=36)
            row.pack(fill="x", pady=2)
            row.pack_propagate(False)

            row.bind("<Button-1>", lambda e, eid=ep_id: self._select_endpoint(eid))

            status_text = "READY"
            status_bg = "#374151"
            status_fg = "#D1D5DB"
            if res:
                if res.passed:
                    status_text = "PASS"
                    status_bg = "#064E3B"
                    status_fg = COLORS["success"]
                else:
                    status_text = "FAIL"
                    status_bg = "#7F1D1D"
                    status_fg = COLORS["danger"]

            badge_frame = ctk.CTkFrame(row, fg_color=status_bg, corner_radius=4, width=64, height=22)
            badge_frame.pack(side="left", padx=(8, 6), pady=6)
            badge_frame.pack_propagate(False)
            lbl_badge = ctk.CTkLabel(badge_frame, text=status_text, font=ctk.CTkFont(size=10, weight="bold"), text_color=status_fg)
            lbl_badge.pack(expand=True)

            method = ep.get("method", "GET").upper()
            m_color = COLORS["accent_cyan"] if method == "GET" else COLORS["accent_purple"]
            lbl_method = ctk.CTkLabel(row, text=method, width=50, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=m_color)
            lbl_method.pack(side="left", padx=4)

            lbl_name = ctk.CTkLabel(
                row,
                text=ep.get("name", ""),
                anchor="w",
                font=ctk.CTkFont(size=12, weight="normal"),
                text_color=COLORS["text_primary"]
            )
            lbl_name.pack(side="left", fill="x", expand=True, padx=4)
            lbl_name.bind("<Button-1>", lambda e, eid=ep_id: self._select_endpoint(eid))

            http_text = str(res.status_code) if res else "-"
            lbl_http = ctk.CTkLabel(row, text=http_text, width=50, anchor="center", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"])
            lbl_http.pack(side="left", padx=4)

            lat_text = f"{res.duration_ms}ms" if res else "-"
            lbl_lat = ctk.CTkLabel(row, text=lat_text, width=65, anchor="e", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"])
            lbl_lat.pack(side="left", padx=4)

            btn_single = ctk.CTkButton(
                row,
                text="▶",
                width=28,
                height=22,
                fg_color="#1E2A47",
                hover_color=COLORS["accent_gold"],
                text_color=COLORS["text_primary"],
                command=lambda eid=ep_id: self._run_single_test(eid)
            )
            btn_single.pack(side="right", padx=(4, 8))

            self.row_widgets[ep_id] = {
                "frame": row,
                "badge_frame": badge_frame,
                "badge_label": lbl_badge,
                "http_label": lbl_http,
                "lat_label": lbl_lat
            }

    def _on_category_selected(self, choice):
        self._populate_test_rows()

    def _on_search_filter(self, *args):
        self._populate_test_rows()

    def _select_endpoint(self, ep_id: str):
        self.selected_endpoint_id = ep_id
        ep = next((e for e in self.all_endpoints if e["id"] == ep_id), None)
        if not ep:
            return

        res = self.test_results.get(ep_id)
        resolved_url = self.client._resolve_url(ep.get("url", ""))

        banner_text = f"[{ep.get('category')}] {ep.get('name')}\n{ep.get('method')} {resolved_url}"
        if ep.get("description"):
            banner_text += f"\nDescription: {ep.get('description')}"
        self.detail_banner.configure(text=banner_text)

        # Request Body tab
        req_body = ep.get("body")
        self.txt_request.delete("1.0", "end")
        if req_body:
            self.txt_request.insert("1.0", json.dumps(req_body, indent=2, ensure_ascii=False))
        else:
            self.txt_request.insert("1.0", "<Empty Body / GET Request>")

        # Response JSON tab
        self.txt_response.delete("1.0", "end")
        if res:
            if isinstance(res.data, (dict, list)):
                self.txt_response.insert("1.0", json.dumps(res.data, indent=2, ensure_ascii=False))
            else:
                self.txt_response.insert("1.0", str(res.data or res.error or f"HTTP {res.status_code}"))
        else:
            self.txt_response.insert("1.0", "Click '▶' on the row or run the suite to view live response.")

        # Meta & Headers tab
        self.txt_meta.delete("1.0", "end")
        meta_info = {
            "endpoint_id": ep_id,
            "category": ep.get("category"),
            "method": ep.get("method"),
            "expected_code": ep.get("expected_code", 200),
            "skip_auth": ep.get("skip_auth", False),
            "requires_session": ep.get("requires_session", False),
            "resolved_url": resolved_url
        }
        if res:
            meta_info["executed_status_code"] = res.status_code
            meta_info["duration_ms"] = res.duration_ms
            meta_info["passed"] = res.passed
            meta_info["error"] = res.error
        self.txt_meta.insert("1.0", json.dumps(meta_info, indent=2))

    def _copy_curl_to_clipboard(self):
        if not self.selected_endpoint_id:
            return
        ep = next((e for e in self.all_endpoints if e["id"] == self.selected_endpoint_id), None)
        if not ep:
            return

        resolved_url = self.client._resolve_url(ep.get("url", ""))
        method = ep.get("method", "GET").upper()
        cmd_parts = [f"curl -k -X {method} \"{resolved_url}\""]

        if not ep.get("skip_auth"):
            key = ep.get("api_key") or self.client.api_key
            cmd_parts.append(f"-H \"X-API-Key: {key}\"")

        if ep.get("body"):
            body_str = json.dumps(ep["body"]).replace('"', '\\"')
            cmd_parts.append(f"-H \"Content-Type: application/json\" -d \"{body_str}\"")

        curl_cmd = " ".join(cmd_parts)
        self.clipboard_clear()
        self.clipboard_append(curl_cmd)
        self.btn_copy_curl.configure(text="✅ Copied!")
        self.after(2000, lambda: self.btn_copy_curl.configure(text="📋 Copy curl"))

    def _update_row_ui(self, res: ApiResponse):
        ep_id = res.endpoint_id
        if ep_id in self.row_widgets:
            widgets = self.row_widgets[ep_id]
            if res.passed:
                widgets["badge_frame"].configure(fg_color="#064E3B")
                widgets["badge_label"].configure(text="PASS", text_color=COLORS["success"])
            else:
                widgets["badge_frame"].configure(fg_color="#7F1D1D")
                widgets["badge_label"].configure(text="FAIL", text_color=COLORS["danger"])

            widgets["http_label"].configure(text=str(res.status_code))
            widgets["lat_label"].configure(text=f"{res.duration_ms}ms")

        passed = sum(1 for r in self.test_results.values() if r.passed)
        failed = sum(1 for r in self.test_results.values() if not r.passed)
        total_executed = len(self.test_results)
        avg_ms = int(sum(r.duration_ms for r in self.test_results.values()) / max(1, total_executed))

        self.lbl_passed.configure(text=str(passed))
        self.lbl_failed.configure(text=str(failed))
        self.lbl_avg_ms.configure(text=f"{avg_ms} ms")

        if self.selected_endpoint_id == ep_id:
            self._select_endpoint(ep_id)

    def _run_single_test(self, ep_id: str):
        ep = next((e for e in self.all_endpoints if e["id"] == ep_id), None)
        if not ep:
            return

        def task():
            res = self.client.execute_catalog_endpoint(ep)
            self.test_results[ep_id] = res
            self.ui_queue.put(("SINGLE_RESULT", res))

        threading.Thread(target=task, daemon=True).start()

    def _start_run_all(self):
        if self.is_running:
            return
        self._run_suite(self.all_endpoints)

    def _start_run_category(self):
        if self.is_running:
            return
        cat = self.category_var.get()
        if cat == "All":
            subset = self.all_endpoints
        else:
            subset = [e for e in self.all_endpoints if e.get("category") == cat]
        self._run_suite(subset)

    def _request_stop(self):
        self.stop_requested = True

    def _run_suite(self, endpoints: List[Dict[str, Any]]):
        self.is_running = True
        self.stop_requested = False
        self.btn_run_all.configure(state="disabled")
        self.btn_run_category.configure(state="disabled")
        self.btn_stop.configure(state="normal", fg_color=COLORS["danger"])
        self.progress_bar.set(0)

        total_to_run = len(endpoints)

        def runner():
            for i, ep in enumerate(endpoints):
                if self.stop_requested:
                    break

                ep_id = ep["id"]
                self.ui_queue.put(("RUNNING", ep_id))

                res = self.client.execute_catalog_endpoint(ep)
                self.test_results[ep_id] = res

                progress_val = (i + 1) / total_to_run
                self.ui_queue.put(("RESULT", (res, progress_val)))

            self.ui_queue.put(("COMPLETE", None))

        threading.Thread(target=runner, daemon=True).start()

    def _on_suite_complete(self):
        self.is_running = False
        self.stop_requested = False
        self.btn_run_all.configure(state="normal")
        self.btn_run_category.configure(state="normal")
        self.btn_stop.configure(state="disabled", fg_color="#374151")
