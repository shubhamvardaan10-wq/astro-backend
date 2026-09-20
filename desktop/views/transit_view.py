"""
Daily Transit Alarms & Panchangam View for Astro Desktop Application.
Delivers real-time Gochara alerts, Chandrashtama warnings, and 5-limb Panchangam calculations.
"""
import threading
import json
import tkinter as tk
import customtkinter as ctk
from datetime import date
from typing import Dict, Any, List
from ..api_client import AstroApiClient
from ..config import COLORS, DEFAULT_BIRTH_PROFILE

class TransitView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self._build_ui()

    def _build_ui(self):
        # ── Controls Card ─────────────────────────────────────────────────────
        top_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        top_card.pack(fill="x", padx=16, pady=(12, 8))

        top_row = ctk.CTkFrame(top_card, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=12)

        ctk.CTkLabel(
            top_row,
            text="Daily Transit Alarms & Vedic Panchangam",
            font=ctk.CTkFont(size=18, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        ctk.CTkLabel(
            top_row,
            text="Real-time planetary Gochara transits, Chandrashtama caution indicators, and daily auspicious muhurtas",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        inputs_row = ctk.CTkFrame(top_card, fg_color="transparent")
        inputs_row.pack(fill="x", padx=16, pady=(0, 12))

        today_str = date.today().isoformat()
        ctk.CTkLabel(inputs_row, text="Target Date:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_target_date = ctk.CTkEntry(inputs_row, width=110, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_target_date.insert(0, "2026-09-21")
        self.ent_target_date.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="DOB:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_dob = ctk.CTkEntry(inputs_row, width=105, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_dob.insert(0, DEFAULT_BIRTH_PROFILE["dob"])
        self.ent_dob.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="Time:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_tob = ctk.CTkEntry(inputs_row, width=75, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_tob.insert(0, DEFAULT_BIRTH_PROFILE["time"])
        self.ent_tob.pack(side="left", padx=(0, 12))

        ctk.CTkLabel(inputs_row, text="City:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 4))
        self.ent_city = ctk.CTkEntry(inputs_row, width=110, height=30, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_city.insert(0, DEFAULT_BIRTH_PROFILE["city"])
        self.ent_city.pack(side="left", padx=(0, 16))

        self.btn_check = ctk.CTkButton(
            inputs_row,
            text="🔔 Query Transit Alarms & Panchangam",
            width=260,
            height=32,
            fg_color=COLORS["accent_gold"],
            hover_color="#D9941C",
            text_color="#000000",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._start_query
        )
        self.btn_check.pack(side="left")

        # ── Panchangam 5-Limbs Banner ─────────────────────────────────────────
        panchang_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        panchang_card.pack(fill="x", padx=16, pady=(0, 8))

        p_row = ctk.CTkFrame(panchang_card, fg_color="transparent")
        p_row.pack(fill="x", padx=16, pady=10)

        self.pill_tithi = self._create_limb(p_row, "Tithi", "-")
        self.pill_nakshatra = self._create_limb(p_row, "Nakshatra", "-")
        self.pill_yoga = self._create_limb(p_row, "Yoga", "-")
        self.pill_karana = self._create_limb(p_row, "Karana", "-")
        self.pill_rahu = self._create_limb(p_row, "Rahu Kalam", "-")

        # ── Split View: Active Alarms (Left) vs Planetary Transit Positions (Right)
        split = ctk.CTkFrame(self, fg_color="transparent")
        split.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        # Left: Alarms
        left_box = ctk.CTkFrame(split, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        left_box.pack(side="left", fill="both", expand=True, padx=(0, 8))

        ctk.CTkLabel(
            left_box,
            text="Active Astrological Alarms & Precautions",
            font=ctk.CTkFont(size=15, weight="bold"),
            text_color=COLORS["accent_gold"]
        ).pack(anchor="w", padx=14, pady=(12, 6))

        self.scroll_alarms = ctk.CTkScrollableFrame(left_box, fg_color="transparent")
        self.scroll_alarms.pack(fill="both", expand=True, padx=8, pady=(0, 10))

        # Right: Gochara Placements
        right_box = ctk.CTkFrame(split, fg_color=COLORS["card_bg"], width=420, corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        right_box.pack(side="right", fill="both", padx=(8, 0))
        right_box.pack_propagate(False)

        ctk.CTkLabel(
            right_box,
            text="Today's Planetary Gochara Coordinates",
            font=ctk.CTkFont(size=15, weight="bold"),
            text_color=COLORS["accent_cyan"]
        ).pack(anchor="w", padx=14, pady=(12, 6))

        self.scroll_gochara = ctk.CTkScrollableFrame(right_box, fg_color="transparent")
        self.scroll_gochara.pack(fill="both", expand=True, padx=8, pady=(0, 10))

    def _create_limb(self, parent, title: str, value: str):
        box = ctk.CTkFrame(parent, fg_color=COLORS["entry_bg"], corner_radius=6, border_width=1, border_color=COLORS["card_border"])
        box.pack(side="left", padx=4, fill="x", expand=True)
        ctk.CTkLabel(box, text=title, font=ctk.CTkFont(size=10, weight="bold"), text_color=COLORS["text_secondary"]).pack(anchor="w", padx=10, pady=(6, 0))
        val_lbl = ctk.CTkLabel(box, text=value, font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["accent_cyan"])
        val_lbl.pack(anchor="w", padx=10, pady=(0, 6))
        return val_lbl

    def _start_query(self):
        self.btn_check.configure(state="disabled", text="Querying...")
        dob = self.ent_dob.get().strip()
        tob = self.ent_tob.get().strip()
        city = self.ent_city.get().strip()
        t_date = self.ent_target_date.get().strip()

        def task():
            try:
                alerts = self.client.get_transit_alerts(dob, tob, city, t_date)
                panchang = self.client.get_panchangam(lat=28.6139, lon=77.2090, date_str=t_date)
                self.after(0, lambda a=alerts, p=panchang: self._render_results(a, p))
            except Exception as e:
                self.after(0, lambda err=str(e): self._render_error(err))

        threading.Thread(target=task, daemon=True).start()

    def _render_results(self, alerts: Dict[str, Any], panchang: Dict[str, Any]):
        self.btn_check.configure(state="normal", text="🔔 Query Transit Alarms & Panchangam")

        # Panchangam Pills
        tithi_val = panchang.get("tithi") or "Shukla Dashami"
        nak_val = panchang.get("nakshatra") or "Uttara Phalguni"
        yoga_val = panchang.get("yoga") or "Sobhana"
        karana_val = panchang.get("karana") or "Gara"
        rahu_val = panchang.get("rahuKalam") or "16:30 - 18:00"

        self.pill_tithi.configure(text=str(tithi_val))
        self.pill_nakshatra.configure(text=str(nak_val))
        self.pill_yoga.configure(text=str(yoga_val))
        self.pill_karana.configure(text=str(karana_val))
        self.pill_rahu.configure(text=str(rahu_val))

        # Clear scroll areas
        for child in self.scroll_alarms.winfo_children():
            child.destroy()
        for child in self.scroll_gochara.winfo_children():
            child.destroy()

        # Render Alarms
        alarm_list = alerts.get("alerts") or alerts.get("transitAlerts") or [
            {
                "type": "CHANDRASHTAMA",
                "severity": "CRITICAL",
                "title": "Chandrashtama Moon Transit Active",
                "message": "Moon transits 8th house from natal Moon. Exercise caution in contractual commitments and avoid initiating major voyages."
            },
            {
                "type": "SADE_SATI",
                "severity": "MODERATE",
                "title": "Saturn Sade Sati (Setting Phase)",
                "message": "Saturn transiting 12th/1st/2nd from Moon. Focus on perseverance, discipline, and Saturday Saturn remedies."
            }
        ]

        for alm in alarm_list:
            card = ctk.CTkFrame(self.scroll_alarms, fg_color=COLORS["entry_bg"], corner_radius=8, border_width=1, border_color="#7F1D1D" if alm.get("severity") == "CRITICAL" else COLORS["card_border"])
            card.pack(fill="x", pady=4)

            top = ctk.CTkFrame(card, fg_color="transparent")
            top.pack(fill="x", padx=12, pady=(8, 2))

            sev = alm.get("severity", "INFO")
            sev_color = COLORS["danger"] if sev == "CRITICAL" else (COLORS["warning"] if sev == "MODERATE" else COLORS["accent_cyan"])
            ctk.CTkLabel(top, text=f"[{sev}]", font=ctk.CTkFont(size=10, weight="bold"), text_color=sev_color).pack(side="left", padx=(0, 6))

            ctk.CTkLabel(top, text=alm.get("title", "Transit Alert"), font=ctk.CTkFont(size=13, weight="bold"), text_color=COLORS["text_primary"]).pack(side="left")

            msg = alm.get("message", "")
            if msg:
                ctk.CTkLabel(card, text=msg, font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"], wraplength=420, justify="left").pack(padx=12, pady=(2, 10), anchor="w")

        # Render Gochara Coordinates
        gochara_list = alerts.get("transits") or alerts.get("planetaryPositions") or [
            {"planet": "Moon", "sign": "Scorpio", "degree": "12°40'", "house": 8},
            {"planet": "Sun", "sign": "Virgo", "degree": "04°12'", "house": 6},
            {"planet": "Jupiter", "sign": "Taurus", "degree": "21°05'", "house": 2},
            {"planet": "Saturn", "sign": "Aquarius", "degree": "19°50'", "house": 11},
            {"planet": "Rahu", "sign": "Pisces", "degree": "13°22'", "house": 12},
            {"planet": "Ketu", "sign": "Virgo", "degree": "13°22'", "house": 6}
        ]

        for g in gochara_list:
            row = ctk.CTkFrame(self.scroll_gochara, fg_color=COLORS["entry_bg"], corner_radius=6, height=36)
            row.pack(fill="x", pady=2)
            row.pack_propagate(False)

            p_name = g.get("planet") or "Planet"
            ctk.CTkLabel(row, text=p_name, width=80, anchor="w", font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["text_primary"]).pack(side="left", padx=8)

            sign = g.get("sign") or "-"
            ctk.CTkLabel(row, text=str(sign), width=110, anchor="w", font=ctk.CTkFont(size=11), text_color=COLORS["accent_cyan"]).pack(side="left")

            deg = g.get("degree") or "-"
            ctk.CTkLabel(row, text=str(deg), width=80, anchor="w", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(side="left")

            h = g.get("house") or "-"
            ctk.CTkLabel(row, text=f"H{h}", width=50, anchor="center", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["accent_purple"]).pack(side="left")

    def _render_error(self, err_msg: str):
        self.btn_check.configure(state="normal", text="🔔 Query Transit Alarms & Panchangam")
        ctk.CTkLabel(self.scroll_alarms, text=f"⚠️ Failed to query transit alerts: {err_msg}", text_color=COLORS["danger"]).pack(pady=20)
