package com.astro.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for conversational AI consultation with multi-turn memory.
 */
public class AstroChatRequest {

    private String sessionId;

    @NotBlank(message = "message must not be blank")
    private String message;

    private BirthRequest birth;

    private String language = "en";

    public AstroChatRequest() {}

    public AstroChatRequest(String sessionId, String message, BirthRequest birth, String language) {
        this.sessionId = sessionId;
        this.message = message;
        this.birth = birth;
        this.language = language != null ? language : "en";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BirthRequest getBirth() {
        return birth;
    }

    public void setBirth(BirthRequest birth) {
        this.birth = birth;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
