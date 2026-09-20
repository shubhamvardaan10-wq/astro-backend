package com.astro.model;

public class MessagingWebhookRequest {
    private String platform = "WHATSAPP";
    private String senderId = "user-108";
    private String messageText = "How is my career looking for the next 6 months?";
    private String dob = "1990-05-15";
    private String time = "14:30:00";
    private String city = "New Delhi";

    public MessagingWebhookRequest() {}

    public MessagingWebhookRequest(String platform, String senderId, String messageText) {
        this.platform = platform;
        this.senderId = senderId;
        this.messageText = messageText;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
