#!/usr/bin/env python3
"""
sound_therapy_engine.py — Planetary Binaural Frequency & Soundscape Generator

Generates high-fidelity, customized therapeutic binaural audio (WAV format)
tuned to Hans Cousto's Cosmic Octave planetary frequencies:
 - Sun: 126.22 Hz (Vitality, Confidence & Solar Radiance)
 - Moon: 210.42 Hz (Emotional Equilibrium & Intuition)
 - Mars: 144.72 Hz (Dynamic Drive & Physical Courage)
 - Mercury: 141.27 Hz (Cognitive Focus, Coding & Mathematical Logic)
 - Jupiter: 183.58 Hz (Expansion, Wealth & Spiritual Wisdom)
 - Venus: 221.23 Hz (Aesthetic Harmony, Love & Sensual Radiance)
 - Saturn: 147.85 Hz (Grounded Discipline, Structure & Transcendence)
"""

import base64
from io import BytesIO
import json
import math
import wave
import numpy as np

PLANETARY_CARRIERS = {
    "Sun": {"hz": 126.22, "chakra": "Manipura (Solar Plexus)", "attribute": "Vitality, Sovereign Clarity & Willpower"},
    "Moon": {"hz": 210.42, "chakra": "Svadhisthana (Sacral)", "attribute": "Emotional Equilibrium, Lucid Intuition & Receptivity"},
    "Mars": {"hz": 144.72, "chakra": "Manipura / Lower", "attribute": "Courage, Strategic Execution & Physical Stamina"},
    "Mercury": {"hz": 141.27, "chakra": "Vishuddha (Throat)", "attribute": "Cognitive Bandwidth, Coding Focus & Business Intellect"},
    "Jupiter": {"hz": 183.58, "chakra": "Ajna / Sahasrara", "attribute": "Wealth Expansion, Sovereign Dignity & Higher Wisdom"},
    "Venus": {"hz": 221.23, "chakra": "Anahata (Heart)", "attribute": "Aesthetic Harmony, Relational Magnetism & Creative Beauty"},
    "Saturn": {"hz": 147.85, "chakra": "Muladhara (Root)", "attribute": "Grounded Discipline, Karmic Endurance & Inner Stillness"}
}

ENTRAINMENT_MODES = {
    "deep_focus": {"beatHz": 10.0, "state": "Alpha (8–12 Hz)", "benefit": "Flow-state coding, strategic synthesis, and high mental retention"},
    "wealth_meditation": {"beatHz": 7.83, "state": "Schumann Resonance / Theta", "benefit": "Dissolving scarcity conditioning and aligning with abundance"},
    "restorative_sleep": {"beatHz": 3.5, "state": "Delta (0.5–4 Hz)", "benefit": "Deep cellular healing, REM cycle stabilization and nervous recovery"},
    "anxiety_relief": {"beatHz": 6.0, "state": "Theta (4–8 Hz)", "benefit": "Rapid Vata grounding, emotional nervous release and mental calm"}
}


def generate_planetary_soundscape(planet="Jupiter", mode="wealth_meditation", duration_sec=10):
    """
    Synthesizes stereo binaural WAV audio based on planetary carrier and brainwave beat.
    """
    duration = min(60, max(5, int(duration_sec)))
    planet_key = planet.capitalize() if planet.capitalize() in PLANETARY_CARRIERS else "Jupiter"
    mode_key = mode.lower() if mode.lower() in ENTRAINMENT_MODES else "wealth_meditation"

    p_info = PLANETARY_CARRIERS[planet_key]
    m_info = ENTRAINMENT_MODES[mode_key]

    sample_rate = 22050
    total_samples = sample_rate * duration
    t = np.linspace(0, duration, total_samples, endpoint=False)

    f_carrier = p_info["hz"]
    f_beat = m_info["beatHz"]

    f_left = f_carrier
    f_right = f_carrier + f_beat

    # Generate stereo channels with soft fade-in/fade-out
    envelope = np.ones(total_samples)
    fade_samples = int(sample_rate * 1.5)  # 1.5 second smooth fade
    envelope[:fade_samples] = np.linspace(0, 1, fade_samples)
    envelope[-fade_samples:] = np.linspace(1, 0, fade_samples)

    # Primary binaural waves
    left_wave = np.sin(2 * np.pi * f_left * t) * 0.45 * envelope
    right_wave = np.sin(2 * np.pi * f_right * t) * 0.45 * envelope

    # Layer warm harmonic Solfeggio undertone (432 Hz subtle background)
    solfeggio_subtle = (np.sin(2 * np.pi * 432.0 * t) * 0.08) * envelope
    left_channel = left_wave + solfeggio_subtle
    right_channel = right_wave + solfeggio_subtle

    # Normalize to 16-bit PCM integer
    max_amp = np.max(np.abs([left_channel, right_channel]))
    if max_amp > 0:
        left_int = (left_channel / max_amp * 30000).astype(np.int16)
        right_int = (right_channel / max_amp * 30000).astype(np.int16)
    else:
        left_int = np.zeros(total_samples, dtype=np.int16)
        right_int = np.zeros(total_samples, dtype=np.int16)

    # Interleave stereo
    stereo = np.empty((total_samples * 2,), dtype=np.int16)
    stereo[0::2] = left_int
    stereo[1::2] = right_int

    # Write WAV to memory buffer
    bio = BytesIO()
    with wave.open(bio, 'wb') as wav_file:
        wav_file.setnchannels(2)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        wav_file.writeframes(stereo.tobytes())

    wav_bytes = bio.getvalue()
    wav_b64 = "data:audio/wav;base64," + base64.b64encode(wav_bytes).decode('utf-8')

    return {
        "targetPlanet": planet_key,
        "carrierFrequencyHz": f_carrier,
        "entrainmentMode": mode_key,
        "binauralBeatFrequencyHz": f_beat,
        "brainwaveTarget": m_info["state"],
        "governingChakra": p_info["chakra"],
        "healingAttributes": p_info["attribute"],
        "modeBenefits": m_info["benefit"],
        "audioSpecs": {
            "durationSeconds": duration,
            "sampleRate": sample_rate,
            "channels": "Stereo (Binaural Headphones Required for Max Effect)",
            "solfeggioHarmonic": "432 Hz Cosmic Base Resonance",
            "audioByteSize": len(wav_bytes)
        },
        "audioDataUrl": wav_b64,
        "listeningProtocol": "Listen using stereo headphones in a relaxed seated or lying position. Close eyes, focus on rhythmic breathing, and allow the binaural beat to synchronize cerebral hemispheres."
    }


if __name__ == "__main__":
    res = generate_planetary_soundscape("Jupiter", "wealth_meditation", 10)
    print("Sound Therapy Engine Test:")
    print("  Planet:", res["targetPlanet"])
    print("  Carrier Hz:", res["carrierFrequencyHz"])
    print("  Beat Hz:", res["binauralBeatFrequencyHz"])
    print("  Audio Size:", res["audioSpecs"]["audioByteSize"], "bytes")
    print("  Audio URL Length:", len(res["audioDataUrl"]), "chars")
