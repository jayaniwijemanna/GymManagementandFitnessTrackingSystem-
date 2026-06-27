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

    private DataStore() {}

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }
}
