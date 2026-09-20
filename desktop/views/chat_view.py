"""
Conversational AI Chat View for Astro Desktop Application.
Supports multi-turn stateful consultations, natal birth grounding, and bilingual English/Hindi dialogue.
"""
import threading
import tkinter as tk
import customtkinter as ctk
from typing import Optional, Dict, Any
from ..api_client import AstroApiClient
from ..config import COLORS, DEFAULT_BIRTH_PROFILE

class ChatView(ctk.CTkFrame):
    def __init__(self, master, client: AstroApiClient, **kwargs):
        super().__init__(master, fg_color="transparent", **kwargs)
        self.client = client
        self.session_id: Optional[str] = None
        self.language = "en"
        self.messages = []

        self._build_ui()

    def _build_ui(self):
        # Top Header Bar: Session info, Birth Profile, Language toggle
        top_bar = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        top_bar.pack(fill="x", padx=16, pady=(12, 8))

        top_content = ctk.CTkFrame(top_bar, fg_color="transparent")
        top_content.pack(fill="x", padx=16, pady=12)

        # Title
        left_box = ctk.CTkFrame(top_content, fg_color="transparent")
        left_box.pack(side="left")
        ctk.CTkLabel(
            left_box,
            text="Astro AI Conversational Assistant",
            font=ctk.CTkFont(size=18, weight="bold"),
            text_color=COLORS["text_primary"]
        ).pack(anchor="w")
        
        self.lbl_session = ctk.CTkLabel(
            left_box,
            text="Session: Ready to start new consultation",
            font=ctk.CTkFont(size=11),
            text_color=COLORS["text_secondary"]
        )
        self.lbl_session.pack(anchor="w")

        # Right Controls: Language & Reset
        right_box = ctk.CTkFrame(top_content, fg_color="transparent")
        right_box.pack(side="right")

        ctk.CTkLabel(right_box, text="Language:", font=ctk.CTkFont(size=12), text_color=COLORS["text_secondary"]).pack(side="left", padx=(0, 6))
        self.lang_var = ctk.StringVar(value="English (en)")
        self.lang_menu = ctk.CTkOptionMenu(
            right_box,
            values=["English (en)", "Hindi (hi)", "Tamil (ta)", "Telugu (te)"],
            variable=self.lang_var,
            width=130,
            height=30,
            fg_color=COLORS["entry_bg"],
            button_color=COLORS["card_border"],
            command=self._on_lang_changed
        )
        self.lang_menu.pack(side="left", padx=(0, 10))

        self.btn_reset = ctk.CTkButton(
            right_box,
            text="🔄 New Session",
            width=110,
            height=30,
            fg_color="#374151",
            hover_color=COLORS["card_border"],
            command=self._reset_session
        )
        self.btn_reset.pack(side="left")

        # Birth profile quick bar
        profile_bar = ctk.CTkFrame(top_bar, fg_color="transparent")
        profile_bar.pack(fill="x", padx=16, pady=(0, 10))

        ctk.CTkLabel(profile_bar, text="Grounding Birth Chart:", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["accent_gold"]).pack(side="left", padx=(0, 8))

        ctk.CTkLabel(profile_bar, text="DOB:", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(side="left", padx=(4, 2))
        self.ent_dob = ctk.CTkEntry(profile_bar, width=95, height=26, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_dob.insert(0, DEFAULT_BIRTH_PROFILE["dob"])
        self.ent_dob.pack(side="left", padx=(0, 8))

        ctk.CTkLabel(profile_bar, text="Time:", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(side="left", padx=(4, 2))
        self.ent_tob = ctk.CTkEntry(profile_bar, width=65, height=26, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_tob.insert(0, DEFAULT_BIRTH_PROFILE["time"])
        self.ent_tob.pack(side="left", padx=(0, 8))

        ctk.CTkLabel(profile_bar, text="City:", font=ctk.CTkFont(size=11), text_color=COLORS["text_secondary"]).pack(side="left", padx=(4, 2))
        self.ent_city = ctk.CTkEntry(profile_bar, width=100, height=26, fg_color=COLORS["entry_bg"], border_color=COLORS["entry_border"])
        self.ent_city.insert(0, DEFAULT_BIRTH_PROFILE["city"])
        self.ent_city.pack(side="left", padx=(0, 8))

        # Quick Suggestion Chips
        chips_frame = ctk.CTkFrame(self, fg_color="transparent")
        chips_frame.pack(fill="x", padx=16, pady=(0, 6))

        suggestions = [
            "💍 When will I get married?",
            "💼 Career growth & next promotion?",
            "💰 Financial outlook & wealth?",
            "🌿 Recommended remedies for Saturn?",
            "कैरियर में सफलता कब मिलेगी?"
        ]
        for s in suggestions:
            btn = ctk.CTkButton(
                chips_frame,
                text=s,
                height=26,
                fg_color=COLORS["card_bg"],
                hover_color=COLORS["card_border"],
                text_color=COLORS["text_secondary"],
                font=ctk.CTkFont(size=11),
                command=lambda text=s: self._send_suggestion(text)
            )
            btn.pack(side="left", padx=(0, 6))

        # ── Main Split View: Chat Stream (Left) vs Astrology Insights (Right) ─
        main_split = ctk.CTkFrame(self, fg_color="transparent")
        main_split.pack(fill="both", expand=True, padx=16, pady=(0, 10))

        # Chat stream box
        chat_box = ctk.CTkFrame(main_split, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        chat_box.pack(side="left", fill="both", expand=True, padx=(0, 8))

        self.chat_scroll = ctk.CTkScrollableFrame(chat_box, fg_color="transparent")
        self.chat_scroll.pack(fill="both", expand=True, padx=12, pady=12)

        # Right: Astrology Insight Card
        self.insight_box = ctk.CTkFrame(main_split, fg_color=COLORS["card_bg"], width=320, corner_radius=10, border_width=1, border_color=COLORS["card_border"])
        self.insight_box.pack(side="right", fill="both", padx=(8, 0))
        self.insight_box.pack_propagate(False)

        ctk.CTkLabel(
            self.insight_box,
            text="Grounded Astrological Context",
            font=ctk.CTkFont(size=14, weight="bold"),
            text_color=COLORS["accent_gold"]
        ).pack(anchor="w", padx=14, pady=(12, 8))

        self.txt_insights = ctk.CTkTextbox(
            self.insight_box,
            fg_color=COLORS["entry_bg"],
            font=ctk.CTkFont(size=11),
            text_color=COLORS["text_primary"],
            wrap="word"
        )
        self.txt_insights.pack(fill="both", expand=True, padx=12, pady=(0, 12))
        self.txt_insights.insert("1.0", "As you converse, astrological telemetry, planetary periods, and prescribed classical remedies will populate here in real-time.")

        # Bottom Input Toolbar
        input_bar = ctk.CTkFrame(self, fg_color=COLORS["card_bg"], corner_radius=10, border_width=1, border_color=COLORS["card_border"], height=52)
        input_bar.pack(fill="x", padx=16, pady=(0, 12))
        input_bar.pack_propagate(False)

        self.ent_input = ctk.CTkEntry(
            input_bar,
            placeholder_text="Ask any question about your chart, timing, marriage, career, or life...",
            fg_color=COLORS["entry_bg"],
            border_color=COLORS["entry_border"],
            text_color=COLORS["text_primary"],
            font=ctk.CTkFont(size=13)
        )
        self.ent_input.pack(side="left", fill="x", expand=True, padx=(12, 8), pady=8)
        self.ent_input.bind("<Return>", lambda e: self._send_message())

        self.btn_send = ctk.CTkButton(
            input_bar,
            text="Send ➔",
            width=90,
            height=36,
            fg_color=COLORS["accent_gold"],
            hover_color="#D9941C",
            text_color="#000000",
            font=ctk.CTkFont(size=13, weight="bold"),
            command=self._send_message
        )
        self.btn_send.pack(side="right", padx=(0, 12), pady=8)

        # Add initial welcome message
        self._add_bot_bubble(
            "Namaste! I am your Astro-Backend AI Astrologer. Ask me anything about your Vedic natal chart, Vimshottari dasha, transits, career, or relationship timing.",
            meta={"intent": "GREETING", "planets": ["Jupiter", "Sun"]}
        )

    def _on_lang_changed(self, choice: str):
        if "Hindi" in choice:
            self.language = "hi"
        elif "Tamil" in choice:
            self.language = "ta"
        elif "Telugu" in choice:
            self.language = "te"
        else:
            self.language = "en"

    def _reset_session(self):
        self.session_id = None
        self.client.active_chat_session_id = None
        self.lbl_session.configure(text="Session: Ready to start new consultation")
        for child in self.chat_scroll.winfo_children():
            child.destroy()
        self.txt_insights.delete("1.0", "end")
        self.txt_insights.insert("1.0", "New session started.")
        self._add_bot_bubble("Session reset. How may I assist your astrological exploration today?")

    def _send_suggestion(self, text: str):
        clean_text = text.lstrip("💍💼💰🌿 ")
        self.ent_input.delete(0, "end")
        self.ent_input.insert(0, clean_text)
        self._send_message()

    def _add_user_bubble(self, message: str):
        bubble_frame = ctk.CTkFrame(self.chat_scroll, fg_color="transparent")
        bubble_frame.pack(fill="x", pady=6)

        bubble = ctk.CTkFrame(bubble_frame, fg_color="#312E81", corner_radius=12)
        bubble.pack(side="right", padx=(60, 4))

        ctk.CTkLabel(
            bubble,
            text=message,
            font=ctk.CTkFont(size=13),
            text_color="#FFFFFF",
            wraplength=480,
            justify="left"
        ).pack(padx=14, pady=10)

    def _add_bot_bubble(self, message: str, meta: Optional[Dict[str, Any]] = None):
        bubble_frame = ctk.CTkFrame(self.chat_scroll, fg_color="transparent")
        bubble_frame.pack(fill="x", pady=6)

        bubble = ctk.CTkFrame(bubble_frame, fg_color="#1E293B", corner_radius=12, border_width=1, border_color="#334155")
        bubble.pack(side="left", padx=(4, 60))

        # Bot header with gold icon
        header = ctk.CTkFrame(bubble, fg_color="transparent")
        header.pack(fill="x", padx=14, pady=(8, 2))
        ctk.CTkLabel(header, text="✨ Astro AI", font=ctk.CTkFont(size=11, weight="bold"), text_color=COLORS["accent_gold"]).pack(side="left")

        ctk.CTkLabel(
            bubble,
            text=message,
            font=ctk.CTkFont(size=13),
            text_color=COLORS["text_primary"],
            wraplength=520,
            justify="left"
        ).pack(padx=14, pady=(2, 10))

    def _send_message(self):
        msg = self.ent_input.get().strip()
        if not msg:
            return

        self.ent_input.delete(0, "end")
        self._add_user_bubble(msg)

        birth_payload = {
            "dob": self.ent_dob.get().strip(),
            "time": self.ent_tob.get().strip(),
            "city": self.ent_city.get().strip()
        }

        self.btn_send.configure(state="disabled", text="Consulting...")

        def task():
            try:
                resp = self.client.send_chat_message(
                    message=msg,
                    birth=birth_payload if not self.session_id else None,
                    session_id=self.session_id,
                    language=self.language
                )
                self.after(0, lambda r=resp: self._on_chat_response(r))
            except Exception as e:
                self.after(0, lambda err=str(e): self._on_chat_error(err))

        threading.Thread(target=task, daemon=True).start()

    def _on_chat_response(self, data: Dict[str, Any]):
        self.btn_send.configure(state="normal", text="Send ➔")
        if not isinstance(data, dict):
            self._add_bot_bubble(str(data))
            return

        if "sessionId" in data and data["sessionId"]:
            self.session_id = data["sessionId"]
            self.lbl_session.configure(text=f"Session: {self.session_id} (Active)")

        bot_reply = data.get("response") or data.get("message") or data.get("answer") or "Analysis computed successfully."
        self._add_bot_bubble(bot_reply, meta=data)

        # Update insight panel
        self.txt_insights.delete("1.0", "end")
        insight_lines = []
        if "intent" in data:
            insight_lines.append(f"🎯 Detected Intent: {data['intent']}")
        if "planets" in data:
            insight_lines.append(f"🪐 Relevant Planets: {', '.join(data['planets'])}")
        if "dashaPeriod" in data:
            insight_lines.append(f"⏳ Active Dasha: {data['dashaPeriod']}")
        if "remedies" in data:
            rems = data["remedies"]
            if isinstance(rems, list):
                insight_lines.append("\n🌿 Recommended Remedies:")
                for r in rems:
                    insight_lines.append(f" • {r}")
            elif isinstance(rems, str):
                insight_lines.append(f"\n🌿 Recommended Remedies:\n{rems}")

        if not insight_lines:
            # Fallback to key values in data
            for k, v in data.items():
                if k not in ("response", "message", "answer", "sessionId"):
                    insight_lines.append(f"{k}: {v}")

        self.txt_insights.insert("1.0", "\n".join(insight_lines) if insight_lines else "Grounded consultation complete.")

    def _on_chat_error(self, err_msg: str):
        self.btn_send.configure(state="normal", text="Send ➔")
        self._add_bot_bubble(f"⚠️ Error reaching Astro AI Service: {err_msg}")
