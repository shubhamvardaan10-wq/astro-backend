"""
Main Window for Astro Enterprise Desktop Suite.
Provides responsive sidebar navigation, global API configuration, and seamless view switching.
"""
import tkinter as tk
import customtkinter as ctk
from typing import Dict, Any, Optional

from .config import (
    DEFAULT_INGRESS_URL,
    DEFAULT_API_KEY,
    COLORS
)
from .api_client import AstroApiClient
from .views.api_test_view import ApiTestView
from .views.chat_view import ChatView
from .views.timeline_view import TimelineView
from .views.kundli_view import KundliView
from .views.btr_view import BtrView
from .views.transit_view import TransitView
from .views.mesh_monitor_view import MeshMonitorView

class AstroMainWindow(ctk.CTk):
    def __init__(self):
        super().__init__()

        # Window configuration
        self.title("Astro Enterprise Studio | Full Microservices & API Testing Suite")
        self.geometry("1340x840")
        self.minsize(1120, 720)
        self.configure(fg_color=COLORS["bg_dark"])

        # Core API Client
        self.client = AstroApiClient(
            ingress_url=DEFAULT_INGRESS_URL,
            api_key=DEFAULT_API_KEY
        )

        # State
        self.current_view_name = "api_test"
        self.view_instances: Dict[str, ctk.CTkFrame] = {}
        self.nav_buttons: Dict[str, ctk.CTkButton] = {}

        self._build_layout()
        self._show_view("api_test")

    def _build_layout(self):
        # ── Sidebar Frame ─────────────────────────────────────────────────────
        self.sidebar = ctk.CTkFrame(self, width=240, fg_color=COLORS["sidebar_bg"], corner_radius=0)
        self.sidebar.pack(side="left", fill="y")
        self.sidebar.pack_propagate(False)

        # Brand header
        brand_frame = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        brand_frame.pack(fill="x", padx=16, pady=(20, 24))

        ctk.CTkLabel(
            brand_frame,
            text="✨ ASTRO STUDIO",
            font=ctk.CTkFont(size=20, weight="bold"),
            text_color=COLORS["accent_gold"]
        ).pack(anchor="w")

        ctk.CTkLabel(
            brand_frame,
            text="Enterprise Platform v2.0",
            font=ctk.CTkFont(size=11),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        # Navigation Links
        nav_items = [
            ("api_test", "🚀 API Test Suite", "Full 64 Endpoints Sweep"),
            ("chat", "💬 AI Astrologer", "Conversational Bot"),
            ("timeline", "📈 Life Event Timing", "Monthly Forecast"),
            ("kundli", "☸ Kundli & Planets", "Vedic Chart Analysis"),
            ("btr", "🎯 Birth Rectifier", "BTR Timing Calibration"),
            ("transit", "🔔 Daily Transits", "Gochara & Panchangam"),
            ("mesh", "🌐 Cluster Monitor", "7 Microservices Status")
        ]

        self.nav_frame = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        self.nav_frame.pack(fill="x", padx=12, pady=0)

        for view_key, title, subtitle in nav_items:
            btn = ctk.CTkButton(
                self.nav_frame,
                text=title,
                anchor="w",
                height=42,
                corner_radius=8,
                font=ctk.CTkFont(size=13, weight="bold"),
                fg_color="transparent",
                text_color=COLORS["text_secondary"],
                hover_color=COLORS["entry_bg"],
                command=lambda vk=view_key: self._show_view(vk)
            )
            btn.pack(fill="x", pady=3)
            self.nav_buttons[view_key] = btn

        # Sidebar Bottom Settings Card
        bottom_settings = ctk.CTkFrame(self.sidebar, fg_color=COLORS["card_bg"], corner_radius=8, border_width=1, border_color=COLORS["card_border"])
        bottom_settings.pack(side="bottom", fill="x", padx=12, pady=16)

        ctk.CTkLabel(
            bottom_settings,
            text="Connection Settings",
            font=ctk.CTkFont(size=11, weight="bold"),
            text_color=COLORS["accent_cyan"]
        ).pack(anchor="w", padx=12, pady=(10, 4))

        ctk.CTkLabel(bottom_settings, text="Ingress URL:", font=ctk.CTkFont(size=10), text_color=COLORS["text_secondary"]).pack(anchor="w", padx=12)
        self.ent_url = ctk.CTkEntry(
            bottom_settings,
            height=26,
            font=ctk.CTkFont(size=11),
            fg_color=COLORS["entry_bg"],
            border_color=COLORS["entry_border"]
        )
        self.ent_url.insert(0, self.client.ingress_url)
        self.ent_url.pack(fill="x", padx=12, pady=(2, 6))

        ctk.CTkLabel(bottom_settings, text="Master API Key:", font=ctk.CTkFont(size=10), text_color=COLORS["text_secondary"]).pack(anchor="w", padx=12)
        self.ent_key = ctk.CTkEntry(
            bottom_settings,
            height=26,
            font=ctk.CTkFont(size=11),
            show="*",
            fg_color=COLORS["entry_bg"],
            border_color=COLORS["entry_border"]
        )
        self.ent_key.insert(0, self.client.api_key)
        self.ent_key.pack(fill="x", padx=12, pady=(2, 8))

        btn_apply = ctk.CTkButton(
            bottom_settings,
            text="Apply Settings",
            height=26,
            font=ctk.CTkFont(size=11, weight="bold"),
            fg_color=COLORS["accent_gold"],
            text_color="#000000",
            hover_color="#D9941C",
            command=self._apply_connection_settings
        )
        btn_apply.pack(fill="x", padx=12, pady=(0, 10))

        # ── Main Content Container ────────────────────────────────────────────
        self.container = ctk.CTkFrame(self, fg_color="transparent")
        self.container.pack(side="right", fill="both", expand=True)

    def _apply_connection_settings(self):
        new_url = self.ent_url.get().strip()
        new_key = self.ent_key.get().strip()
        self.client.update_config(ingress_url=new_url, api_key=new_key)

    def _show_view(self, view_name: str):
        self.current_view_name = view_name

        # Highlight active nav button
        for vk, btn in self.nav_buttons.items():
            if vk == view_name:
                btn.configure(
                    fg_color=COLORS["card_bg"],
                    text_color=COLORS["accent_gold"],
                    border_width=1,
                    border_color=COLORS["accent_gold"]
                )
            else:
                btn.configure(
                    fg_color="transparent",
                    text_color=COLORS["text_secondary"],
                    border_width=0
                )

        # Hide all existing views
        for v in self.view_instances.values():
            v.pack_forget()

        # Instantiate or show view
        if view_name not in self.view_instances:
            if view_name == "api_test":
                self.view_instances[view_name] = ApiTestView(self.container, self.client)
            elif view_name == "chat":
                self.view_instances[view_name] = ChatView(self.container, self.client)
            elif view_name == "timeline":
                self.view_instances[view_name] = TimelineView(self.container, self.client)
            elif view_name == "kundli":
                self.view_instances[view_name] = KundliView(self.container, self.client)
            elif view_name == "btr":
                self.view_instances[view_name] = BtrView(self.container, self.client)
            elif view_name == "transit":
                self.view_instances[view_name] = TransitView(self.container, self.client)
            elif view_name == "mesh":
                self.view_instances[view_name] = MeshMonitorView(self.container, self.client)

        self.view_instances[view_name].pack(fill="both", expand=True)


def main():
    ctk.set_appearance_mode("Dark")
    ctk.set_default_color_theme("blue")
    app = AstroMainWindow()
    app.mainloop()

if __name__ == "__main__":
    main()
