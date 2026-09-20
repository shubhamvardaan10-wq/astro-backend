package com.astro.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

public class BirthTimeRectificationRequest extends BirthRequest {

    @Min(value = 5, message = "uncertaintyMinutes must be at least 5")
    @Max(value = 180, message = "uncertaintyMinutes must be at most 180")
    private Integer uncertaintyMinutes = 30;

    @Min(value = 1, message = "stepMinutes must be at least 1")
    @Max(value = 15, message = "stepMinutes must be at most 15")
    private Integer stepMinutes = 2;

    private String gender = "MALE";

    private List<LifeEvent> lifeEvents = new ArrayList<>();

    public Integer getUncertaintyMinutes() {
        return uncertaintyMinutes;
    }

    public void setUncertaintyMinutes(Integer uncertaintyMinutes) {
        this.uncertaintyMinutes = uncertaintyMinutes;
    }

    public Integer getStepMinutes() {
        return stepMinutes;
    }

    public void setStepMinutes(Integer stepMinutes) {
        this.stepMinutes = stepMinutes;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<LifeEvent> getLifeEvents() {
        return lifeEvents;
    }

    public void setLifeEvents(List<LifeEvent> lifeEvents) {
        this.lifeEvents = lifeEvents;
    }

    public static class LifeEvent {
        private String eventType;
        private String eventDate;
        private String description;

        public LifeEvent() {}

        public LifeEvent(String eventType, String eventDate, String description) {
            this.eventType = eventType;
            this.eventDate = eventDate;
            this.description = description;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventDate() {
            return eventDate;
        }

        public void setEventDate(String eventDate) {
            this.eventDate = eventDate;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
