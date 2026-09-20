package com.astro.model;

public class SoundTherapyRequest {

    private String targetPlanet = "Jupiter"; // Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn
    private String purpose = "wealth_meditation"; // deep_focus, wealth_meditation, restorative_sleep, anxiety_relief
    private Integer durationSeconds = 10;

    public String getTargetPlanet() {
        return targetPlanet;
    }

    public void setTargetPlanet(String targetPlanet) {
        if (targetPlanet != null && !targetPlanet.isBlank()) {
            this.targetPlanet = targetPlanet;
        }
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        if (purpose != null && !purpose.isBlank()) {
            this.purpose = purpose;
        }
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        if (durationSeconds != null && durationSeconds > 0) {
            this.durationSeconds = Math.min(60, durationSeconds);
        }
    }
}
