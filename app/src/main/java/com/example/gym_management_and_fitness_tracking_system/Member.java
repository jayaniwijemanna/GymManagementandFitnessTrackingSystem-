package com.example.gym_management_and_fitness_tracking_system;

public class Member {
    public String name;
    public String email;
    public String phone;
    public String plan;

    public Member(String name, String email, String phone, String plan) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.plan = plan;
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
    }
}
