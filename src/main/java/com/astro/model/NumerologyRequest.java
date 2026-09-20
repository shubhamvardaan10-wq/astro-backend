package com.astro.model;

import jakarta.validation.constraints.NotBlank;

public class NumerologyRequest {

    @NotBlank(message = "fullName or name is required")
    private String fullName;

    @NotBlank(message = "dob is required (YYYY-MM-DD)")
    private String dob;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    // Alias setter for name
    public void setName(String name) {
        if (this.fullName == null || this.fullName.isBlank()) {
            this.fullName = name;
        }
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    // Alias setter for dateOfBirth
    public void setDateOfBirth(String dateOfBirth) {
        if (this.dob == null || this.dob.isBlank()) {
            this.dob = dateOfBirth;
        }
    }
}
