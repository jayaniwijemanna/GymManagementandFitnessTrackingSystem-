package com.example.gym_management_and_fitness_tracking_system;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple in-memory singleton store shared across activities.
 * Replace with Firebase/SQLite calls when the backend is ready.
 */
public class DataStore {

    private static DataStore instance;

    public final List<Member> members = new ArrayList<>();
    public final List<Trainer> trainers = new ArrayList<>();
    public final List<GymPackage> packages = new ArrayList<>();

    private DataStore() {
        // Packages are dynamically loaded from Cloud Firestore collection 'packages'

        // Populate dummy trainers
        Trainer t1 = new Trainer("Alex Mercer", "Strength & Conditioning", "555-0199", "alex@gmail.com", "password");
        t1.addFeedback("5 ★ - Excellent workout routines! Very technical and helpful.");
        t1.addFeedback("4 ★ - Great conditioning training, highly recommended!");
        t1.rating = "4.5";
        trainers.add(t1);

        Trainer t2 = new Trainer("Jessica Chen", "Yoga & Flexibility", "555-0144", "jessica@gmail.com", "password");
        t2.addFeedback("5 ★ - Amazing yoga sessions! I feel so refreshed.");
        t2.addFeedback("5 ★ - Helped me recover from my back pain. A miracle worker.");
        t2.rating = "5.0";
        trainers.add(t2);

        Trainer t3 = new Trainer("Marcus Aurelius", "Bodybuilding & Nutrition", "555-0182", "marcus@gmail.com", "password");
        t3.addFeedback("5 ★ - Best bodybuilding tips in the gym. Form is perfect.");
        t3.addFeedback("5 ★ - Nutrition plans are super easy to follow and very effective!");
        t3.rating = "5.0";
        trainers.add(t3);

        // Members are dynamically loaded from Cloud Firestore collection 'users' / 'members'
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }
}
