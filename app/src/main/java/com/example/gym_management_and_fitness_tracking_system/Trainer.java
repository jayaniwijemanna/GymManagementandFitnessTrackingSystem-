package com.example.gym_management_and_fitness_tracking_system;

public class Trainer {
    public String name;
    public String specialization;
    public String phone;
    public String email;
    public String password;
    public java.util.List<String> feedback = new java.util.ArrayList<>();
    public String rating = "5.0";

    public Trainer(String name, String specialization, String phone) {
        this.name = name;
        this.specialization = specialization;
        this.phone = phone;
        // Generate a fallback/default email and password
        this.email = name.toLowerCase().replace(" ", "") + "@gmail.com";
        this.password = "password";
    }

    public Trainer(String name, String specialization, String phone, String email, String password) {
        this.name = name;
        this.specialization = specialization;
        this.phone = phone;
        this.email = email;
        this.password = password;
    }

    public void addFeedback(String review) {
        if (feedback == null) {
            feedback = new java.util.ArrayList<>();
        }
        feedback.add(review);
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
    }
}
