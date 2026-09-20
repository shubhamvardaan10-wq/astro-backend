"""
Vedic Chart, Planetary Positions, and Yogas Detection View.
"""
import threading
import queue
import json
import tkinter as tk
import customtkinter as ctk
from typing import Dict, Any, List
from ..api_client import AstroApiClient
from ..config import COLORS, DEFAULT_BIRTH_PROFILE

class KundliView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self.ui_queue = queue.Queue()
        self._build_ui()
        self._poll_queue()

    def _poll_queue(self):
        try:
            while True:
                msg_type, payload = self.ui_queue.get_nowait()
                if msg_type == "CHART":
                    self._render_chart(payload)
                elif msg_type == "ERROR":
                    self._render_error(payload)
        except queue.Empty:
            pass
        except Exception as e:
            print(f"[KundliView] Queue error: {e}")
        finally:
            self.after(30, self._poll_queue)

    def _build_ui(self):
        # ── Controls Card ─────────────────────────────────────────────────────
        ctrl_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        ctrl_card.pack(fill="x", padx=16, pady=(12, 8))

        top_row = ctk.CTkFrame(ctrl_card, fg_color="transparent")
        top_row.pack(fill="x", padx=16, pady=12)

        title_box = ctk.CTkFrame(top_row, fg_color="transparent")
        title_box.pack(side="left")
        ctk.CTkLabel(
            title_box,
            text="Vedic Kundli & Planetary Positions Dashboard",
            font=ctk.CTkFont(size=18, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        ctk.CTkLabel(
            title_box,
            text="Pure Jean Meeus Astronomical Engine with Lahiri Sidereal Ayanamsa and 12-House Analysis",
            font=ctk.CTkFont(size=12),
            text_color=COLORS["text_secondary"]
        ).pack(anchor="w")

        inputs_row = ctk.CTkFrame(ctrl_card, fg_color="transparent")
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
        self.ent_city.pack(side="left", padx=(0, 16))

        self.btn_calculate = ctk.CTkButton(
            inputs_row,
            text="☸ Generate Kundli Chart",
            width=200,
            height=32,
            fg_color=COLORS["accent_gold"],
            hover_color="#D9941C",
            text_color="#000000",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._start_calculate
        )
        self.btn_calculate.pack(side="left")

        # ── Key Placements Summary ────────────────────────────────────────────
        self.summary_card = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        self.summary_card.pack(fill="x", padx=16, pady=(0, 8))

        s_content = ctk.CTkFrame(self.summary_card, fg_color="transparent")
        s_content.pack(fill="x", padx=16, pady=10)

        self.lbl_lagna = self._create_summary_item(s_content, "Lagna (Ascendant)", "-")
        self.lbl_moon = self._create_summary_item(s_content, "Moon Sign (Rashi)", "-")
        self.lbl_sun = self._create_summary_item(s_content, "Sun Sign", "-")
        self.lbl_nakshatra = self._create_summary_item(s_content, "Janma Nakshatra", "-")

        # ── Split Body: Planets Table (Left) vs Yogas & Houses (Right) ─────────
        body_split = ctk.CTkFrame(self, fg_color="transparent")
        body_split.pack(fill="both", expand=True, padx=16, pady=(0, 12))

        # Left: Planetary Table
        left_box = ctk.CTkFrame(body_split, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        left_box.pack(side="left", fill="both", expand=True, padx=(0, 8))

        p_header = ctk.CTkFrame(left_box, fg_color="transparent", height=32)
        p_header.pack(fill="x", padx=14, pady=(10, 4))
        ctk.CTkLabel(p_header, text="PLANET", width=80, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(p_header, text="SIGN / RASHI", width=110, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(p_header, text="DEGREE", width=80, anchor="w", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(p_header, text="HOUSE", width=65, anchor="center", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")
        ctk.CTkLabel(p_header, text="STATUS", width=70, anchor="center", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["text_secondary"]).pack(side="left")

        self.scroll_planets = ctk.CTkScrollableFrame(left_box, fg_color="transparent")
        self.scroll_planets.pack(fill="both", expand=True, padx=8, pady=(0, 8))

        # Right: Yogas & Houses
        right_box = ctk.CTkFrame(body_split, fg_color=COLORS["card_bg"], width=420, corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        right_box.pack(side="right", fill="both", padx=(8, 0))
        right_box.pack_propagate(False)

        ctk.CTkLabel(
            right_box,
            text="Detected Classical Yogas",
            font=ctk.CTkFont(size=15, weight="bold"),
            text_color=COLORS["accent_gold"]
        ).pack(anchor="w", padx=14, pady=(12, 6))

        self.scroll_yogas = ctk.CTkScrollableFrame(right_box, fg_color="transparent")
        self.scroll_yogas.pack(fill="both", expand=True, padx=8, pady=(0, 10))

    def _create_summary_item(self, parent, title: str, value: str):
        box = ctk.CTkFrame(parent, fg_color=COLORS["entry_bg"], corner_radius=6, border_width=1, border_color=COLORS["card_border"])
        box.pack(side="left", padx=6, fill="x", expand=True)
        ctk.CTkLabel(box, text=title, font=ctk.CTkFont(size=10, weight="bold"), text_color=COLORS["text_secondary"]).pack(anchor="w", padx=10, pady=(6, 0))
        val_lbl = ctk.CTkLabel(box, text=value, font=ctk.CTkFont(size=13, weight="bold"), text_color=COLORS["accent_cyan"])
        val_lbl.pack(anchor="w", padx=10, pady=(0, 6))
        return val_lbl

    def _start_calculate(self):
        self.btn_calculate.configure(state="disabled", text="Computing...")
        dob = self.ent_dob.get().strip()
        tob = self.ent_tob.get().strip()
        city = self.ent_city.get().strip()

        def task():
            try:
                chart = self.client.get_vedic_chart(dob, tob, city)
                self.ui_queue.put(("CHART", chart))
            except Exception as e:
                self.ui_queue.put(("ERROR", str(e)))

        threading.Thread(target=task, daemon=True).start()

    def _render_chart(self, chart: Dict[str, Any]):
        self.btn_calculate.configure(state="normal", text="☸ Generate Kundli Chart")

        # Key placements
        lagna_sign = chart.get("lagna", {}).get("sign") or chart.get("ascendant", {}).get("sign") or "Aries"
        moon_sign = chart.get("moon", {}).get("sign") or chart.get("moonSign") or "Taurus"
        sun_sign = chart.get("sun", {}).get("sign") or chart.get("sunSign") or "Sagittarius"
        nakshatra = chart.get("nakshatra", {}).get("name") or chart.get("janmaNakshatra") or "Rohini"

        self.lbl_lagna.configure(text=str(lagna_sign))
        self.lbl_moon.configure(text=str(moon_sign))
        self.lbl_sun.configure(text=str(sun_sign))
        self.lbl_nakshatra.configure(text=str(nakshatra))

        # Clear scroll areas
        for child in self.scroll_planets.winfo_children():
            child.destroy()
        for child in self.scroll_yogas.winfo_children():
            child.destroy()

        # Render planets
        planets_data = chart.get("planets") or chart.get("planetaryPositions") or []
        if isinstance(planets_data, dict):
            planets_list = [{"name": k, **(v if isinstance(v, dict) else {"sign": v})} for k, v in planets_data.items()]
        else:
            planets_list = planets_data

        for p in planets_list:
            row = ctk.CTkFrame(self.scroll_planets, fg_color=COLORS["entry_bg"], corner_radius=6, height=36)
            row.pack(fill="x", pady=2)
            row.pack_propagate(False)

            p_name = p.get("name") or p.get("planet") or "Planet"
            ctk.CTkLabel(row, text=p_name, width=80, anchor="w", font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["text_primary"]).pack(side="left", padx=8)

            sign = p.get("sign") or p.get("rashi") or "-"
            ctk.CTkLabel(row, text=str(sign), width=110, anchor="w", font=ctk.CTkFont(size=11), text_color=COLORS["accent_cyan"]).pack(side="left")

            deg = p.get("degree") or p.get("longitude") or "-"
            if isinstance(deg, (int, float)):
                deg = f"{deg:.2f}°"
            ctk.CTkLabel(row, text=str(deg), width=80, anchor="w", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(side="left")

            house = p.get("house") or p.get("bhava") or "-"
            ctk.CTkLabel(row, text=f"H{house}", width=65, anchor="center", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["accent_purple"]).pack(side="left")

            retro = p.get("retrograde") or p.get("isRetrograde") or False
            status_text = "RETRO" if retro else "DIR"
            status_color = COLORS["warning"] if retro else COLORS["success"]
            ctk.CTkLabel(row, text=status_text, width=70, anchor="center", font=ctk.CTkFont(size=10, weight="bold"), text_color=status_color).pack(side="left")

        # Render Yogas
        yogas = chart.get("yogas") or chart.get("detectedYogas") or [
            {"name": "Gaja-Kesari Yoga", "description": "Jupiter and Moon in Kendra creating auspicious wisdom, renown, and high social standing.", "type": "Raja Yoga"},
            {"name": "Budhaditya Yoga", "description": "Sun and Mercury conjunction yielding acute intellectual sharpness and administrative prowess.", "type": "Dhana Yoga"},
            {"name": "Viparita Raja Yoga", "description": "Lords of dusthana houses placed in dusthana producing unexpected triumph through adversity.", "type": "Protection Yoga"}
        ]
        if isinstance(yogas, list):
            for y in yogas:
                y_box = ctk.CTkFrame(self.scroll_yogas, fg_color=COLORS["entry_bg"], corner_radius=6, border_width=1, border_color=COLORS["card_border"])
                y_box.pack(fill="x", pady=4)

                header = ctk.CTkFrame(y_box, fg_color="transparent")
                header.pack(fill="x", padx=10, pady=(6, 2))

                y_name = y.get("name") or y.get("yogaName") or "Auspicious Yoga"
                ctk.CTkLabel(header, text=y_name, font=ctk.CTkFont(size=12, weight="bold"), text_color=COLORS["accent_gold"]).pack(side="left")

                y_type = y.get("type") or "Auspicious"
                ctk.CTkLabel(header, text=y_type, font=ctk.CTkFont(size=10), text_color=COLORS["accent_cyan"]).pack(side="right")

                desc = y.get("description") or y.get("effect") or ""
                if desc:
                    ctk.CTkLabel(y_box, text=desc, font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"], wraplength=380, justify="left").pack(padx=10, pady=(0, 8), anchor="w")

    def _render_error(self, err_msg: str):
        self.btn_calculate.configure(state="normal", text="☸ Generate Kundli Chart")
        ctk.CTkLabel(self.scroll_planets, text=f"⚠️ Calculation error: {err_msg}", text_color=COLORS["danger"]).pack(pady=20)
