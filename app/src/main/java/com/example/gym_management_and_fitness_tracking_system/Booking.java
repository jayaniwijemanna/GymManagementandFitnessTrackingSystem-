package com.example.gym_management_and_fitness_tracking_system;

public class Booking {
    public String id;
    public String memberId;
    public String memberName;
    public String memberEmail;
    public String memberPhone;
    public String trainerId;
    public String trainerName;
    public String trainerEmail;
    public String bookedTime;
    public String status = "Pending"; // "Pending", "Accepted", "Rejected", "Completed"
    public long timestamp = System.currentTimeMillis();

    // Required empty constructor for Firestore deserialization
    public Booking() {}

    public Booking(String id, String memberId, String memberName, String memberEmail, String memberPhone,
                   String trainerId, String trainerName, String trainerEmail, String bookedTime, String status) {
        this.id = id;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.memberPhone = memberPhone;
        this.trainerId = trainerId;
        this.trainerName = trainerName;
        this.trainerEmail = trainerEmail;
        this.bookedTime = bookedTime;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
