package com.example.gym_management_and_fitness_tracking_system;

public class Trainer {
    public String id = "";
    public String name = "Trainer";
    public String specialization = "General Fitness";
    public String phone = "";
    public String email = "";
    public String password = "password";
    public String role = "trainer";
    public java.util.List<String> feedback = new java.util.ArrayList<>();
    public String rating = "5.0";
    public String availability = "Mon - Sat: 06:00 AM - 08:00 PM (Available)";

    // Required empty constructor for Firestore deserialization
    public Trainer() {}

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

    public Trainer(String id, String name, String specialization, String phone, String email, String password) {
        this.id = id;
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

    public double getAverageRating() {
        if (feedback == null || feedback.isEmpty()) return 5.0;
        double sum = 0;
        int count = 0;
        for (String f : feedback) {
            if (f != null && f.contains("★")) {
                try {
                    String ratingPart = f.split("★")[0].trim();
                    double r = Double.parseDouble(ratingPart);
                    sum += r;
                    count++;
                } catch (Exception ignored) {}
            }
        }
        if (count == 0) return 5.0;
        return Math.round((sum / count) * 10.0) / 10.0;
    }

    public int getRatingCount() {
        if (feedback == null || feedback.isEmpty()) return 0;
        int count = 0;
        for (String f : feedback) {
            if (f != null && f.contains("★")) {
                count++;
            }
        }
        return count;
    }

    public String getFormattedRating() {
        int count = getRatingCount();
        if (count == 0) {
            return "★ " + rating + " (New)";
        }
        return String.format(java.util.Locale.getDefault(), "★ %.1f (%d rating%s)", getAverageRating(), count, count > 1 ? "s" : "");
    }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
    }
}
