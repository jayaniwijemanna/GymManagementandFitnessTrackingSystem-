package com.example.gym_management_and_fitness_tracking_system;

public class GymPackage {
    public String id;
    public String name;
    public String price;
    public String description;

    // Required empty constructor for Firestore deserialization
    public GymPackage() {}

    public GymPackage(String name, String price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public GymPackage(String id, String name, String price, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
    }
}
