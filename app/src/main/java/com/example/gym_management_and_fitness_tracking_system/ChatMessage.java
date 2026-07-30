package com.example.gym_management_and_fitness_tracking_system;

import com.google.firebase.Timestamp;

public class ChatMessage {
    public String id = "";
    public String chatId = "";
    public String senderEmail = "";
    public String senderName = "";
    public String senderRole = ""; // "member" or "trainer"
    public String receiverEmail = "";
    public String receiverName = "";
    public String message = "";
    public Timestamp timestamp;

    public ChatMessage() {}

    public ChatMessage(String chatId, String senderEmail, String senderName, String senderRole,
                       String receiverEmail, String receiverName, String message) {
        this.chatId = chatId;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.receiverEmail = receiverEmail;
        this.receiverName = receiverName;
        this.message = message;
        this.timestamp = Timestamp.now();
    }
}
