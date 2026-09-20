"""
Life Event Timing & Monthly Forecast View for Astro Desktop Application.
Calculates high-granularity month-by-month domain trajectories across Career, Wealth, Love, and Health.
"""
import threading
import json
import tkinter as tk
import customtkinter as ctk
from typing import Dict, Any, List
from ..api_client import AstroApiClient
from ..config import COLORS, DEFAULT_BIRTH_PROFILE

class TimelineView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self._build_ui()

    def _build_ui(self):
        # ── Configuration Card ────────────────────────────────────────────────
        config_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        config_card.pack(fill="x", padx=16, pady=(12, 8))

        top_row = ctk.CTkFrame(config_card, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=12)

        # Title
        title_box = ctk.CTkFrame(top_row, fg_color="transparent")
        title_box.pack(side="left")
        ctk.CTkLabel(
            title_box,
            text="Life Event Timing & Monthly Trajectory Forecast",
            font=ctk.CTkFont(size=18, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        ctk.CTkLabel(
            title_box,
            text="High-granularity multi-domain scoring (Career, Wealth, Love, Health) powered by Gochara & Vimshottari Dasha",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        # Inputs row
        inputs_row = ctk.CTkFrame(config_card, fg_color="transparent")
        inputs_row.pack(fill="x", padx=16, pady=(0, 12))

        ctk.CTkLabel(inputs_row, text="DOB:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_dob = ctk.CTkEntry(inputs_row, width=110, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_dob.insert(0, DEFAULT_BIRTH_PROFILE["dob"])
        self.ent_dob.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="Time:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_tob = ctk.CTkEntry(inputs_row, width=80, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_tob.insert(0, DEFAULT_BIRTH_PROFILE["time"])
        self.ent_tob.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="City:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_city = ctk.CTkEntry(inputs_row, width=120, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_city.insert(0, DEFAULT_BIRTH_PROFILE["city"])
        self.ent_city.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="Horizon:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.horizon_var = ctk.StringVar(value="12 Months")
        self.horizon_menu = ctk.CTkOptionMenu(
            inputs_row,
            values=["12 Months", "24 Months", "36 Months"],
            variable=self.horizon_var,
            width=120,
            height=30,
            fg_color=COLORS["entry_bg"],
            button_color=COLORS["card_border"]
        )
        self.horizon_menu.pack(side="left", padx=(0, 16))

        self.btn_calculate = ctk.CTkButton(
            inputs_row,
            text="✨ Compute Forecast Trajectory",
            width=220,
            height=32,
            fg_color=COLORS["accent_gold"],
            hover_color="#D9941C",
            text_color="#000000",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._start_compute
        )
        self.btn_calculate.pack(side="left")

        # ── Highlights & Peak Windows ─────────────────────────────────────────
        self.highlights_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        self.highlights_card.pack(fill="x", padx=16, pady=(0, 8))

        hl_content = ctk.CTkFrame(self.highlights_card, fg_color="transparent")
        hl_content.pack(fill="x", padx=16, pady=10)

        ctk.CTkLabel(hl_content, text="Key Peak Windows:", font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["accent_gold"]).pack(side="left", padx=(0, 12))
        
        self.pill_career = self._create_summary_pill(hl_content, "💼 Best Career Month", "Pending calculation...")
        self.pill_wealth = self._create_summary_pill(hl_content, "💰 Best Wealth Month", "Pending calculation...")
        self.pill_love = self._create_summary_pill(hl_content, "💍 Best Marriage Window", "Pending calculation...")

        # ── Scrollable Monthly Trajectory Grid ─────────────────────────────────
        list_container = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        list_container.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        # Header Row
        h_row = ctk.CTkFrame(list_container, fg_color="transparent", height=32)
        h_row.pack(fill="x", padx=16, pady=(10, 4))
        ctk.CTkLabel(h_row, text="MONTH", width=90, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(h_row, text="CAREER", width=140, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", padx=8)
        ctk.CTkLabel(h_row, text="WEALTH", width=140, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", padx=8)
        ctk.CTkLabel(h_row, text="LOVE / MARRIAGE", width=140, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", padx=8)
        ctk.CTkLabel(h_row, text="HEALTH", width=140, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", padx=8)
        ctk.CTkLabel(h_row, text="KEY TRANSIT / DASHA INFLUENCE", anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left", fill="x", expand=True)

        self.scroll_timeline = ctk.CTkScrollableFrame(list_container, fg_color="transparent")
        self.scroll_timeline.pack(fill="both", expand=True, padx=8, pady=(0, 8))

        # Initial prompt
        ctk.CTkLabel(
            self.scroll_timeline,
            text="Click 'Compute Forecast Trajectory' to generate month-by-month scores.",
            font=ctk.CTkFont(size=13),
            text_color=COLORS["text_secondary"]
        ).pack(pady=40)

    def _create_summary_pill(self, parent, label: str, value: str):
        pill = ctk.CTkFrame(parent, fg_color=COLORS["entry_bg"], corner_radius=6, border_width=1, border_color=COLORS["card_border"])
        pill.pack(side="left", padx=6)
        ctk.CTkLabel(pill, text=label, font=ctk.CTkFont(size=10, weight="bold"), text_color=COLORS["text_secondary"]).pack(anchor="w", padx=8, pady=(4, 0))
        val_lbl = ctk.CTkLabel(pill, text=value, font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["text_primary"])
        val_lbl.pack(anchor="w", padx=8, pady=(0, 4))
        return val_lbl

    def _start_compute(self):
        self.btn_calculate.configure(state="disabled", text="Computing...")
        dob = self.ent_dob.get().strip()
        tob = self.ent_tob.get().strip()
        city = self.ent_city.get().strip()
        horizon_str = self.horizon_var.get()
        horizon = 12
        if "24" in horizon_str:
            horizon = 24
        elif "36" in horizon_str:
            horizon = 36

        def task():
            try:
                resp = self.client.get_timeline_forecast(dob, tob, city, horizon)
                self.after(0, lambda r=resp: self._render_results(r))
            except Exception as e:
                self.after(0, lambda err=str(e): self._render_error(err))

        threading.Thread(target=task, daemon=True).start()

    def _render_results(self, data: Dict[str, Any]):
        self.btn_calculate.configure(state="normal", text="✨ Compute Forecast Trajectory")

        # Clear scroll area
        for child in self.scroll_timeline.winfo_children():
            child.destroy()

        timeline = data.get("timeline") or data.get("forecast") or []
        if not timeline:
            ctk.CTkLabel(self.scroll_timeline, text=f"Received response:\n{json.dumps(data, indent=2)}").pack(pady=20)
            return

        # Identify peak months
        best_career = max(timeline, key=lambda m: m.get("careerScore", 0) or m.get("career", 0), default=None)
        best_wealth = max(timeline, key=lambda m: m.get("wealthScore", 0) or m.get("wealth", 0), default=None)
        best_love = max(timeline, key=lambda m: m.get("loveScore", 0) or m.get("love", 0), default=None)

        if best_career:
            c_score = best_career.get("careerScore") or best_career.get("career", 0)
            self.pill_career.configure(text=f"{best_career.get('month', '')} ({c_score}/100)")
        if best_wealth:
            w_score = best_wealth.get("wealthScore") or best_wealth.get("wealth", 0)
            self.pill_wealth.configure(text=f"{best_wealth.get('month', '')} ({w_score}/100)")
        if best_love:
            l_score = best_love.get("loveScore") or best_love.get("love", 0)
            self.pill_love.configure(text=f"{best_love.get('month', '')} ({l_score}/100)")

        # Populate rows
        for item in timeline:
            row = ctk.CTkFrame(self.scroll_timeline, fg_color=COLORS["entry_bg"], corner_radius=6, height=42)
            row.pack(fill="x", pady=2)
            row.pack_propagate(False)

            month_str = item.get("month") or item.get("date") or "Unknown"
            ctk.CTkLabel(row, text=month_str, width=90, anchor="w", font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["accent_cyan"]).pack(side="left", padx=8)

            c_val = item.get("careerScore") or item.get("career", 50)
            w_val = item.get("wealthScore") or item.get("wealth", 50)
            l_val = item.get("loveScore") or item.get("love", 50)
            h_val = item.get("healthScore") or item.get("health", 50)

            self._create_meter(row, c_val, COLORS["accent_purple"], width=140)
            self._create_meter(row, w_val, COLORS["accent_gold"], width=140)
            self._create_meter(row, l_val, "#EC4899", width=140)
            self._create_meter(row, h_val, COLORS["success"], width=140)

            # Details/event text
            desc = item.get("summary") or item.get("notes") or item.get("transitHighlight") or "Favorable planetary transits"
            ctk.CTkLabel(
                row,
                text=str(desc),
                anchor="w",
                font=ctk.CTkFont(size=11),
                text_color=COLORS["text_secondary"]
            ).pack(side="left", fill="x", expand=True, padx=8)

    def _create_meter(self, parent, score: int, color: str, width: int = 140):
        frame = ctk.CTkFrame(parent, fg_color="transparent", width=width)
        frame.pack(side="left", padx=8)
        
        top = ctk.CTkFrame(frame, fg_color="transparent")
        top.pack(fill="x")
        ctk.CTkLabel(top, text=f"{score}%", font=ctk.CTkFont(size=10, weight="bold"), text_color=color).pack(side="right")
        
        bar = ctk.CTkProgressBar(frame, width=width, height=6, fg_color="#1E2A47", progress_color=color)
        bar.set(max(0.0, min(1.0, score / 100.0)))
        bar.pack(fill="x", pady=(2, 0))

    def _render_error(self, err_msg: str):
        self.btn_calculate.configure(state="normal", text="✨ Compute Forecast Trajectory")
        for child in self.scroll_timeline.winfo_children():
            child.destroy()
        ctk.CTkLabel(self.scroll_timeline, text=f"⚠️ Failed to compute forecast: {err_msg}", text_color=COLORS["danger"]).pack(pady=20)
