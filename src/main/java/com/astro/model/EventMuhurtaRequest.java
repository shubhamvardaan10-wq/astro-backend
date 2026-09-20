package com.astro.model;

public class EventMuhurtaRequest {

    private String eventType = "marriage"; // marriage, property, business, vehicle, medical
    private Integer daysAhead = 90;
    private Integer maxResults = 6;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        if (eventType != null && !eventType.isBlank()) {
            this.eventType = eventType;
        }
    }

    public Integer getDaysAhead() {
        return daysAhead;
    }

    public void setDaysAhead(Integer daysAhead) {
        if (daysAhead != null && daysAhead > 0) {
            this.daysAhead = daysAhead;
        }
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        if (maxResults != null && maxResults > 0) {
            this.maxResults = maxResults;
        }
    }
}
