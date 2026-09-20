package com.astro.model;

import java.util.List;

public class TeamSynergyRequest {

    private List<TeamMember> members;

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }

    public static class TeamMember {
        private String name;
        private String role;
        private String dob;

        public TeamMember() {}

        public TeamMember(String name, String role, String dob) {
            this.name = name;
            this.role = role;
            this.dob = dob;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDob() { return dob; }
        public void setDob(String dob) { this.dob = dob; }
    }
}
