package com.astro.model;

import java.util.ArrayList;
import java.util.List;

public class SoulGraphRequest {

    public static class Profile {
        private String id;
        private String name;
        private String dob = "1990-05-15";
        private String time = "14:30:00";
        private String city = "New Delhi";
        private String role = "Founder / Partner";

        public Profile() {}

        public Profile(String id, String name, String dob, String role) {
            this.id = id;
            this.name = name;
            this.dob = dob;
            this.role = role;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    private List<Profile> profiles = new ArrayList<>();

    public List<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<Profile> profiles) {
        this.profiles = profiles;
    }
}
