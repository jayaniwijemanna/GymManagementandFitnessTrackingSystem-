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
        trainers.add(new Trainer("Alex Mercer", "Strength & Conditioning", "555-0199"));
        trainers.add(new Trainer("Jessica Chen", "Yoga & Flexibility", "555-0144"));
        trainers.add(new Trainer("Marcus Aurelius", "Bodybuilding & Nutrition", "555-0182"));

        // Populate default test member
        members.add(new Member("John Doe", "member@gmail.com", "555-1234", "Elite Premium", "password"));
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }
}
