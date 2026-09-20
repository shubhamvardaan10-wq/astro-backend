package com.astro.model;

public class VoiceAgentRequest extends BirthRequest {

    private String userQuery = "What does my future hold?";
    private String voiceName = "Acharya Antigravity";

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        if (userQuery != null && !userQuery.isBlank()) {
            this.userQuery = userQuery;
        }
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        if (voiceName != null && !voiceName.isBlank()) {
            this.voiceName = voiceName;
        }
    }
}
