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
        // Populate dummy packages
        packages.add(new GymPackage("Elite Premium", "85", "Access to all gym areas, personal locker, 1 VIP trainer session/week, group classes & spa."));
        packages.add(new GymPackage("Fitness Pro", "55", "Standard gym floor access, locker access, and 1 group fitness class/week."));
        packages.add(new GymPackage("Basic Strength", "35", "Cardio and strength area access during off-peak hours (9 AM - 4 PM)."));

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

        // Populate default test member
        Member defaultMember = new Member("John Doe", "member@gmail.com", "555-1234", "Elite Premium", "password");
        members.add(defaultMember);

        // ── Hardcoded demo members assigned to Alex Mercer ──────────────────

        // Member 1 — Accepted, active training
        Member m1 = new Member("Ryan Carter", "ryan.carter@gmail.com", "555-2201", "Elite Premium");
        m1.height = 182.0;
        m1.weight = 85.0;
        m1.waterIntake = 8;
        m1.bookedTrainer = "Alex Mercer";
        m1.bookedTime = "Mon / Wed / Fri  07:00 AM";
        m1.bookingStatus = "Accepted";
        m1.checkedInTime = "Today, 07:14 AM";
        m1.workoutPlan = "Day 1: Chest & Triceps — Bench Press 4×8, Incline DB Press 3×10, Tricep Pushdown 3×12\n" +
                         "Day 2: Back & Biceps — Deadlift 4×5, Pull-ups 3×8, Barbell Curl 3×12\n" +
                         "Day 3: Legs — Squat 4×8, Leg Press 3×12, Calf Raises 4×15";
        m1.dietPlan = "Protein: 180g/day  |  Carbs: 250g/day  |  Fats: 60g/day\n" +
                      "Pre-workout: banana + oats  |  Post-workout: whey shake + rice";
        m1.weightHistory.add("87.5");
        m1.weightHistory.add("86.0");
        m1.weightHistory.add("85.5");
        m1.weightHistory.add("85.0");
        members.add(m1);

        // Member 2 — Pending approval
        Member m2 = new Member("Priya Sharma", "priya.sharma@gmail.com", "555-3387", "Fitness Pro");
        m2.height = 165.0;
        m2.weight = 58.0;
        m2.waterIntake = 6;
        m2.bookedTrainer = "Alex Mercer";
        m2.bookedTime = "Tue / Thu  06:00 PM";
        m2.bookingStatus = "Pending";
        m2.checkedInTime = "Not Checked In";
        m2.workoutPlan = "";
        m2.dietPlan = "";
        m2.weightHistory.add("58.0");
        members.add(m2);

        // Member 3 — Accepted, weight-loss goal
        Member m3 = new Member("Tom Nguyen", "tom.nguyen@gmail.com", "555-4412", "Basic Strength");
        m3.height = 175.0;
        m3.weight = 92.0;
        m3.waterIntake = 10;
        m3.bookedTrainer = "Alex Mercer";
        m3.bookedTime = "Mon / Wed  08:00 AM";
        m3.bookingStatus = "Accepted";
        m3.checkedInTime = "Yesterday, 08:05 AM";
        m3.workoutPlan = "Focus: Weight Loss HIIT\n" +
                         "Mon: 20 min treadmill intervals + core circuit\n" +
                         "Wed: Battle ropes, rowing machine, kettlebell swings 3×15";
        m3.dietPlan = "Caloric deficit: 500 kcal/day\n" +
                      "Avoid processed sugar. High fibre veggies with every meal.\n" +
                      "Hydration goal: 3L water/day";
        m3.weightHistory.add("98.0");
        m3.weightHistory.add("96.5");
        m3.weightHistory.add("94.0");
        m3.weightHistory.add("92.5");
        m3.weightHistory.add("92.0");
        members.add(m3);
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }
}
