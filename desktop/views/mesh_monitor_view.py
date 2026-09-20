"""
Docker Cluster Mesh Health & Latency Monitor View for Astro Desktop Application.
Pings and benchmarks all 7 microservices in real-time.
"""
import threading
import queue
import time
import tkinter as tk
import customtkinter as ctk
from typing import List, Dict, Any
from ..api_client import AstroApiClient
from ..config import COLORS

class MeshMonitorView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self.auto_refresh = False
        self.cards: Dict[str, Dict[str, Any]] = {}
        self.ui_queue = queue.Queue()
        self._build_ui()
        self._poll_queue()

    def _poll_queue(self):
        try:
            while True:
                msg_type, payload = self.ui_queue.get_nowait()
                if msg_type == "NODES":
                    self._render_nodes(payload)
        except queue.Empty:
            pass
        except Exception as e:
            print(f"[MeshMonitorView] Queue error: {e}")
        finally:
            self.after(50, self._poll_queue)

    def _build_ui(self):
        # Header Card
        header = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        header.pack(fill="x", padx=16, pady=(12, 8))

        top_row = ctk.CTkFrame(header, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=12)

        # Title
        title_box = ctk.CTkFrame(top_row, fg_color="transparent")
        title_box.pack(side="left")
        ctk.CTkLabel(
            title_box,
            text="Microservice Cluster Mesh Monitor",
            font=ctk.CTkFont(size=18, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        ctk.CTkLabel(
            title_box,
            text="Real-time telemetry, TLS termination health, and latency benchmarking across all 7 core containers",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        # Controls
        controls_box = ctk.CTkFrame(top_row, fg_color="transparent")
        controls_box.pack(side="right")

        self.btn_refresh = ctk.CTkButton(
            controls_box,
            text="🔄 Refresh Mesh Status",
            width=160,
            height=32,
            fg_color=COLORS["accent_purple"],
            hover_color="#7C3AED",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self._start_poll
        )
        self.btn_refresh.pack(side="left", padx=(0, 10))

        self.switch_auto = ctk.CTkSwitch(
            controls_box,
            text="Auto Refresh (5s)",
            font=ctk.CTkFont(size=12),
            command=self._toggle_auto
        )
        self.switch_auto.pack(side="left")

        # Service Cards Grid Frame
        self.scroll_services = ctk.CTkScrollableFrame(self, fg_color="transparent")
        self.scroll_services.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        # Initial probe
        self._start_poll()

    def _toggle_auto(self):
        self.auto_refresh = (self.switch_auto.get() == 1)
        if self.auto_refresh:
            self._schedule_auto_refresh()

    def _schedule_auto_refresh(self):
        if self.auto_refresh:
            self._start_poll()
            self.after(5000, self._schedule_auto_refresh)

    def _start_poll(self):
        self.btn_refresh.configure(state="disabled", text="Probing Mesh...")

        def task():
            nodes = self.client.check_mesh_nodes()
            self.ui_queue.put(("NODES", nodes))

        threading.Thread(target=task, daemon=True).start()

    def _render_nodes(self, nodes: List[Dict[str, Any]]):
        self.btn_refresh.configure(state="normal", text="🔄 Refresh Mesh Status")

        # Clear existing cards if first time or rebuild
        for child in self.scroll_services.winfo_children():
            child.destroy()

        for n in nodes:
            name = n["service"]
            status = n["status"]
            latency = n["latency_ms"]
            url = n["url"]
            detail = n["detail"]

            card = ctk.CTkFrame(self.scroll_services, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
            card.pack(fill="x", pady=4)

            top = ctk.CTkFrame(card, fg_color="transparent")
            top.pack(fill="x", padx=16, pady=(12, 4))

            # Status Badge
            is_online = (status == "ONLINE")
            badge_bg = "#064E3B" if is_online else "#7F1D1D"
            badge_fg = COLORS["success"] if is_online else COLORS["danger"]

            b_frame = ctk.CTkFrame(top, fg_color=badge_bg, corner_radius=4, width=80, height=24)
            b_frame.pack(side="left", padx=(0, 10))
            b_frame.pack_propagate(False)
            ctk.CTkLabel(b_frame, text=status, font=ctk.CTkFont(size=11, weight="bold"), text_color=badge_fg).pack(expand=True)

            ctk.CTkLabel(top, text=name, font=ctk.CTkFont(size=14, weight="bold"), text_color=COLORS["text_primary"]).pack(side="left")

            # Latency Pill
            lat_color = COLORS["success"] if latency < 100 else (COLORS["warning"] if latency < 300 else COLORS["danger"])
            lat_pill = ctk.CTkFrame(top, fg_color=COLORS["entry_bg"], corner_radius=4)
            lat_pill.pack(side="right")
            ctk.CTkLabel(lat_pill, text=f"{latency} ms", font=ctk.CTkFont(size=12, weight="bold"), text_color=lat_color).pack(padx=8, pady=2)

            # URL and Details
            bot = ctk.CTkFrame(card, fg_color="transparent")
            bot.pack(fill="x", padx=16, pady=(0, 12))
            ctk.CTkLabel(bot, text=f"Probe Target: {url}", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(anchor="w")
            if detail:
                ctk.CTkLabel(bot, text=f"Telemetry: {detail}", font=ctk.CTkFont(size=11), text_color=COLORS["accent_cyan"]).pack(anchor="w", pady=(2, 0))
