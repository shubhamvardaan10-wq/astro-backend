package com.astro.model;

public class VoiceAuraRequest {

    private String audioBase64;
    private String speakerName = "The Native";
    private String gender = "male";

    public String getAudioBase64() {
        return audioBase64;
    }

    public void setAudioBase64(String audioBase64) {
        this.audioBase64 = audioBase64;
    }

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        if (speakerName != null && !speakerName.isBlank()) {
            this.speakerName = speakerName;
        }
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        if (gender != null && !gender.isBlank()) {
            this.gender = gender;
        }
    }
}
