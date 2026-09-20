package com.astro.model;

public class AuraChakraRequest {

    private String personImageBase64;
    private String subjectName = "The Native";
    private BirthRequest birthDetails;

    public String getPersonImageBase64() {
        return personImageBase64;
    }

    public void setPersonImageBase64(String personImageBase64) {
        this.personImageBase64 = personImageBase64;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        if (subjectName != null && !subjectName.isBlank()) {
            this.subjectName = subjectName;
        }
    }

    public BirthRequest getBirthDetails() {
        return birthDetails;
    }

    public void setBirthDetails(BirthRequest birthDetails) {
        this.birthDetails = birthDetails;
    }
}
