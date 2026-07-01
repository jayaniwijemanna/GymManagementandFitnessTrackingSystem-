package com.example.gym_management_and_fitness_tracking_system;

import java.util.ArrayList;
import java.util.List;

public class Member {
    public String name;
    public String email;
    public String phone;
    public String plan;
    public String password;
    public double height = 175.0; // cm
    public double weight = 70.0; // kg
    public String bookedTrainer = "None";
    public String bookedTime = "None";
    public int waterIntake = 0;
    public List<String> weightHistory = new ArrayList<>();
    public List<String> notifications = new ArrayList<>();
    public String checkedInTime = "Not Checked In";

    public Member(String name, String email, String phone, String plan) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.plan = plan;
        this.password = "password"; // default fallback password
        this.notifications.add("Welcome to Titan Gym! Complete your profile.");
        this.notifications.add("Explore our training packages to get started.");
        this.weightHistory.add("70.0");
    }

    public Member(String name, String email, String phone, String plan, String password) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.plan = plan;
        this.password = password;
        this.notifications.add("Welcome to Titan Gym! Complete your profile.");
        this.notifications.add("Explore our training packages to get started.");
        this.weightHistory.add("70.0");
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
    }
}
