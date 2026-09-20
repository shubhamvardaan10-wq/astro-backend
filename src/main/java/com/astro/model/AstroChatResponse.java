package com.astro.model;

import java.util.List;
import java.util.Map;

/**
 * Response payload for conversational AI consultation.
 */
public class AstroChatResponse {

    private String sessionId;
    private String reply;
    private String intent;
    private String language;
    private Map<String, Object> actionCards;
    private Map<String, Object> natalReference;
    private int historyCount;
    private boolean sessionContextRetained;

    public AstroChatResponse() {}

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Map<String, Object> getActionCards() {
        return actionCards;
    }

    public void setActionCards(Map<String, Object> actionCards) {
        this.actionCards = actionCards;
    }

    public Map<String, Object> getNatalReference() {
        return natalReference;
    }

    public void setNatalReference(Map<String, Object> natalReference) {
        this.natalReference = natalReference;
    }

    public int getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(int historyCount) {
        this.historyCount = historyCount;
    }

    public boolean isSessionContextRetained() {
        return sessionContextRetained;
    }

    public void setSessionContextRetained(boolean sessionContextRetained) {
        this.sessionContextRetained = sessionContextRetained;
    }
}
