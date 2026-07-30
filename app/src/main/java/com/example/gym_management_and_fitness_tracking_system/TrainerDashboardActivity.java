package com.example.gym_management_and_fitness_tracking_system;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrainerDashboardActivity extends AppCompatActivity {

    private Trainer currentTrainer;

    // Header elements
    private TextView tvHeaderInitials, tvHeaderName;
    private FrameLayout btnLogout;
    private RelativeLayout btnNotifications;
    private View viewNotificationBadge;

    // View Containers (Tabs)
    private ScrollView viewMembers, viewPlans, viewProgress, viewReviewsChat;

    // Navigation Tab Buttons
    private LinearLayout navTabMembers, navTabPlans, navTabProgress, navTabReviews;
    private ImageView imgNavMembers, imgNavPlans, imgNavProgress, imgNavReviews;
    private TextView tvNavMembers, tvNavPlans, tvNavProgress, tvNavReviews;

    // Tab 1: Members elements
    private LinearLayout layoutMembersList, layoutEmptyMembers;

    // Tab 2: Plans elements
    private Spinner spinnerPlanMember;
    private EditText etWorkoutPlan, etDietPlan;
    private LinearLayout btnSaveWorkout, btnSaveDiet;

    // Tab 3: Progress elements
    private Spinner spinnerProgressMember;
    private TextView tvProgressBmi, tvProgressWater, tvProgressHeightWeight, tvProgressWeightHistory;

    // Tab 4: Reviews & Chat elements
    private TextView tvReviewsRatingScore;
    private LinearLayout layoutReviewsContainer;
    private Spinner spinnerChatMember;
    private TextView tvChatHistory;
    private EditText etChatMsg;
    private LinearLayout btnChatSend;

    // In-memory chat storage for simulation
    private final Map<String, List<String>> memberChatLogs = new HashMap<>();

    private FirebaseFirestore db;
    private ListenerRegistration bookingsListener;
    private ListenerRegistration membersListener;
    private final List<Booking> trainerBookings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trainer_dashboard);

        db = FirebaseFirestore.getInstance();

        // Apply Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_header), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Retrieve current trainer
        String trainerEmail = getIntent().getStringExtra("TRAINER_EMAIL");
        currentTrainer = null;
        if (!TextUtils.isEmpty(trainerEmail)) {
            for (Trainer t : DataStore.getInstance().trainers) {
                if (t.email != null && t.email.equalsIgnoreCase(trainerEmail)) {
                    currentTrainer = t;
                    break;
                }
            }
        }

        // Fallback for safety
        if (currentTrainer == null) {
            if (!DataStore.getInstance().trainers.isEmpty()) {
                currentTrainer = DataStore.getInstance().trainers.get(0);
            } else {
                currentTrainer = new Trainer("Alex Mercer", "Strength & Conditioning", "555-0199", "alex@gmail.com", "password");
                DataStore.getInstance().trainers.add(currentTrainer);
            }
        }

        initializeViews();
        setupNavigation();
        setupHeader();
        listenToMembersRealtime();
        listenToTrainerBookingsRealtime();
        refreshMembersTab();
        setupPlansTab();
        setupProgressTab();
        setupReviewsChatTab();
    }

    private void listenToTrainerBookingsRealtime() {
        if (currentTrainer == null) return;
        if (bookingsListener != null) bookingsListener.remove();

        bookingsListener = db.collection("bookings")
                .whereEqualTo("trainerName", currentTrainer.name)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        trainerBookings.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Booking b = doc.toObject(Booking.class);
                            if (b != null) {
                                b.id = doc.getId();
                                trainerBookings.add(b);
                            }
                        }
                        java.util.Collections.sort(trainerBookings, (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp));
                        refreshMembersTab();
                    }
                });
    }

    private void listenToMembersRealtime() {
        if (membersListener != null) membersListener.remove();
        membersListener = db.collection("users").whereEqualTo("role", "member")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Member> members = DataStore.getInstance().members;
                        members.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Member m = doc.toObject(Member.class);
                            if (m != null) {
                                m.id = doc.getId();
                                members.add(m);
                            }
                        }
                        refreshMembersTab();
                    }
                });
    }

    private void initializeViews() {
        // Header views
        tvHeaderInitials = findViewById(R.id.tv_header_initials);
        tvHeaderName = findViewById(R.id.tv_header_name);
        btnLogout = findViewById(R.id.btn_logout);
        btnNotifications = findViewById(R.id.btn_notifications);
        viewNotificationBadge = findViewById(R.id.view_notification_badge);

        // Content Tabs
        viewMembers = findViewById(R.id.view_members);
        viewPlans = findViewById(R.id.view_plans);
        viewProgress = findViewById(R.id.view_progress);
        viewReviewsChat = findViewById(R.id.view_reviews_chat);

        // Navigation elements
        navTabMembers = findViewById(R.id.nav_tab_members);
        navTabPlans = findViewById(R.id.nav_tab_plans);
        navTabProgress = findViewById(R.id.nav_tab_progress);
        navTabReviews = findViewById(R.id.nav_tab_reviews);

        imgNavMembers = findViewById(R.id.img_nav_members);
        imgNavPlans = findViewById(R.id.img_nav_plans);
        imgNavProgress = findViewById(R.id.img_nav_progress);
        imgNavReviews = findViewById(R.id.img_nav_reviews);

        tvNavMembers = findViewById(R.id.tv_nav_members);
        tvNavPlans = findViewById(R.id.tv_nav_plans);
        tvNavProgress = findViewById(R.id.tv_nav_progress);
        tvNavReviews = findViewById(R.id.tv_nav_reviews);

        // Tab 1
        layoutMembersList = findViewById(R.id.layout_members_list);
        layoutEmptyMembers = findViewById(R.id.layout_empty_members);

        // Tab 2
        spinnerPlanMember = findViewById(R.id.spinner_plan_member);
        etWorkoutPlan = findViewById(R.id.et_workout_plan);
        etDietPlan = findViewById(R.id.et_diet_plan);
        btnSaveWorkout = findViewById(R.id.btn_save_workout);
        btnSaveDiet = findViewById(R.id.btn_save_diet);

        // Tab 3
        spinnerProgressMember = findViewById(R.id.spinner_progress_member);
        tvProgressBmi = findViewById(R.id.tv_progress_bmi);
        tvProgressWater = findViewById(R.id.tv_progress_water);
        tvProgressHeightWeight = findViewById(R.id.tv_progress_height_weight);
        tvProgressWeightHistory = findViewById(R.id.tv_progress_weight_history);

        // Tab 4
        tvReviewsRatingScore = findViewById(R.id.tv_reviews_rating_score);
        layoutReviewsContainer = findViewById(R.id.layout_reviews_container);
        spinnerChatMember = findViewById(R.id.spinner_chat_member);
        tvChatHistory = findViewById(R.id.tv_chat_history);
        etChatMsg = findViewById(R.id.et_chat_msg);
        btnChatSend = findViewById(R.id.btn_chat_send);
    }

    private void setupHeader() {
        tvHeaderName.setText(currentTrainer.name);
        tvHeaderInitials.setText(currentTrainer.getInitials());

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Logged out securely.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TrainerDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "No new trainer portal notifications.", Toast.LENGTH_SHORT).show();
            viewNotificationBadge.setVisibility(View.GONE);
        });
    }

    private void setupNavigation() {
        navTabMembers.setOnClickListener(v -> selectTab(0));
        navTabPlans.setOnClickListener(v -> selectTab(1));
        navTabProgress.setOnClickListener(v -> selectTab(2));
        navTabReviews.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int index) {
        viewMembers.setVisibility(View.GONE);
        viewPlans.setVisibility(View.GONE);
        viewProgress.setVisibility(View.GONE);
        viewReviewsChat.setVisibility(View.GONE);

        resetTabStyle(imgNavMembers, tvNavMembers);
        resetTabStyle(imgNavPlans, tvNavPlans);
        resetTabStyle(imgNavProgress, tvNavProgress);
        resetTabStyle(imgNavReviews, tvNavReviews);

        switch (index) {
            case 0:
                viewMembers.setVisibility(View.VISIBLE);
                highlightTab(imgNavMembers, tvNavMembers);
                refreshMembersTab();
                break;
            case 1:
                viewPlans.setVisibility(View.VISIBLE);
                highlightTab(imgNavPlans, tvNavPlans);
                setupPlansTab();
                break;
            case 2:
                viewProgress.setVisibility(View.VISIBLE);
                highlightTab(imgNavProgress, tvNavProgress);
                setupProgressTab();
                break;
            case 3:
                viewReviewsChat.setVisibility(View.VISIBLE);
                highlightTab(imgNavReviews, tvNavReviews);
                setupReviewsChatTab();
                break;
        }
    }

    private void resetTabStyle(ImageView img, TextView txt) {
        img.setColorFilter(Color.parseColor("#4A5568"));
        txt.setTextColor(Color.parseColor("#4A5568"));
    }

    private void highlightTab(ImageView img, TextView txt) {
        img.setColorFilter(Color.parseColor("#34C759")); // Green highlight for Trainer
        txt.setTextColor(Color.parseColor("#34C759"));
    }

    // ==================== TAB 1: MEMBERS LIST / BOOKINGS ====================
    private void refreshMembersTab() {
        layoutMembersList.removeAllViews();

        if (trainerBookings.isEmpty()) {
            layoutEmptyMembers.setVisibility(View.VISIBLE);
            return;
        }
        layoutEmptyMembers.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Booking b : trainerBookings) {
            View itemView = inflater.inflate(R.layout.item_member, layoutMembersList, false);

            TextView tvInitials = itemView.findViewById(R.id.tv_member_initials);
            TextView tvName = itemView.findViewById(R.id.tv_member_name);
            TextView tvPlan = itemView.findViewById(R.id.tv_member_plan);
            ImageView btnEdit = itemView.findViewById(R.id.btn_edit_member);
            ImageView btnDelete = itemView.findViewById(R.id.btn_delete_member);

            // Member initials helper
            String initials = "?";
            if (b.memberName != null && !b.memberName.isEmpty()) {
                String[] parts = b.memberName.trim().split("\\s+");
                if (parts.length == 1) initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
                else initials = (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
            }

            tvInitials.setText(initials);
            tvName.setText((b.memberName != null ? b.memberName : "Member") + "  |  " + (b.bookedTime != null ? b.bookedTime : "Time N/A"));

            String bStatus = b.status != null ? b.status : "Pending";
            tvPlan.setText("Status: " + bStatus + "  (" + (b.memberPhone != null ? b.memberPhone : "") + ")");

            if ("Pending".equalsIgnoreCase(bStatus)) {
                tvPlan.setTextColor(Color.parseColor("#FF9500"));
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
            } else if ("Accepted".equalsIgnoreCase(bStatus)) {
                tvPlan.setTextColor(Color.parseColor("#34C759"));
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE); // Allow cancelling/rejecting
            } else if ("Rejected".equalsIgnoreCase(bStatus)) {
                tvPlan.setTextColor(Color.parseColor("#FF3B30"));
                btnEdit.setVisibility(View.VISIBLE); // Allow accepting again
                btnDelete.setVisibility(View.GONE);
            } else {
                tvPlan.setTextColor(Color.parseColor("#94A3B8"));
            }

            // Accept Button Action
            btnEdit.setImageResource(R.drawable.ic_check);
            btnEdit.setColorFilter(Color.parseColor("#34C759"));
            btnEdit.setOnClickListener(v -> {
                b.status = "Accepted";
                if (b.id != null && !b.id.isEmpty()) {
                    db.collection("bookings").document(b.id).update("status", "Accepted")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Accepted booking for " + b.memberName, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                if (b.memberId != null && !b.memberId.isEmpty()) {
                    db.collection("users").document(b.memberId).update("bookingStatus", "Accepted");
                }
                refreshMembersTab();
            });

            // Reject Button Action
            btnDelete.setImageResource(R.drawable.ic_delete);
            btnDelete.setColorFilter(Color.parseColor("#FF3B30"));
            btnDelete.setOnClickListener(v -> {
                b.status = "Rejected";
                if (b.id != null && !b.id.isEmpty()) {
                    db.collection("bookings").document(b.id).update("status", "Rejected")
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Rejected booking for " + b.memberName, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
                if (b.memberId != null && !b.memberId.isEmpty()) {
                    db.collection("users").document(b.memberId).update("bookingStatus", "Rejected");
                }
                refreshMembersTab();
            });

            layoutMembersList.addView(itemView);
        }
    }

    private List<Member> getAssignedMembers() {
        List<Member> list = new ArrayList<>();
        for (Member m : DataStore.getInstance().members) {
            if (m.bookedTrainer != null && m.bookedTrainer.equalsIgnoreCase(currentTrainer.name)) {
                list.add(m);
            }
        }
        return list;
    }

    private void showMemberDetailsDialog(Member m) {
        double bmi = m.weight / ((m.height / 100.0) * (m.height / 100.0));
        String cat;
        if (bmi < 18.5) cat = "Underweight";
        else if (bmi < 25.0) cat = "Normal weight";
        else if (bmi < 30.0) cat = "Overweight";
        else cat = "Obese";

        new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setTitle(m.name + " Profile")
                .setMessage("Email: " + m.email + "\n" +
                        "Phone: " + m.phone + "\n" +
                        "Registered Plan: " + m.plan + "\n" +
                        "Height: " + m.height + " cm\n" +
                        "Weight: " + m.weight + " kg\n" +
                        "Calculated BMI: " + String.format(Locale.getDefault(), "%.1f", bmi) + " (" + cat + ")\n" +
                        "Daily Hydration: " + m.waterIntake + " glasses\n" +
                        "Check-in Status: " + m.checkedInTime)
                .setPositiveButton("Close", null)
                .show();
    }

    // ==================== TAB 2: PLANS CREATOR ====================
    private void setupPlansTab() {
        List<Member> assigned = getAssignedMembers();
        if (assigned.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("No members assigned");
            spinnerPlanMember.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, empty));
            btnSaveWorkout.setEnabled(false);
            btnSaveDiet.setEnabled(false);
            return;
        }

        btnSaveWorkout.setEnabled(true);
        btnSaveDiet.setEnabled(true);

        List<String> names = new ArrayList<>();
        for (Member m : assigned) {
            names.add(m.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerPlanMember.setAdapter(adapter);

        spinnerPlanMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Member m = assigned.get(position);
                etWorkoutPlan.setText(m.workoutPlan);
                etDietPlan.setText(m.dietPlan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSaveWorkout.setOnClickListener(v -> {
            int pos = spinnerPlanMember.getSelectedItemPosition();
            if (pos >= 0 && pos < assigned.size()) {
                Member m = assigned.get(pos);
                m.workoutPlan = etWorkoutPlan.getText().toString().trim();
                m.notifications.add("Trainer " + currentTrainer.name + " updated your Workout Plan.");
                Toast.makeText(this, "Workout plan updated for " + m.name, Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveDiet.setOnClickListener(v -> {
            int pos = spinnerPlanMember.getSelectedItemPosition();
            if (pos >= 0 && pos < assigned.size()) {
                Member m = assigned.get(pos);
                m.dietPlan = etDietPlan.getText().toString().trim();
                m.notifications.add("Trainer " + currentTrainer.name + " updated your Diet Plan.");
                Toast.makeText(this, "Diet plan updated for " + m.name, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== TAB 3: MEMBER PROGRESS ====================
    private void setupProgressTab() {
        List<Member> assigned = getAssignedMembers();
        if (assigned.isEmpty()) {
            List<String> empty = new ArrayList<>();
            empty.add("No members assigned");
            spinnerProgressMember.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, empty));
            tvProgressBmi.setText("-");
            tvProgressWater.setText("-");
            tvProgressHeightWeight.setText("Select an assigned member to track progress");
            tvProgressWeightHistory.setText("");
            return;
        }

        List<String> names = new ArrayList<>();
        for (Member m : assigned) {
            names.add(m.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerProgressMember.setAdapter(adapter);

        spinnerProgressMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Member m = assigned.get(position);
                double bmi = m.weight / ((m.height / 100.0) * (m.height / 100.0));
                String cat;
                if (bmi < 18.5) cat = "Underweight";
                else if (bmi < 25.0) cat = "Normal";
                else if (bmi < 30.0) cat = "Overweight";
                else cat = "Obese";

                tvProgressBmi.setText(String.format(Locale.getDefault(), "%.1f (%s)", bmi, cat));
                tvProgressWater.setText(m.waterIntake + " Glasses");
                tvProgressHeightWeight.setText("Height: " + m.height + " cm  |  Weight: " + m.weight + " kg");

                StringBuilder historyBuilder = new StringBuilder();
                if (m.weightHistory == null || m.weightHistory.isEmpty()) {
                    historyBuilder.append("No weight logs recorded.");
                } else {
                    for (int i = 0; i < m.weightHistory.size(); i++) {
                        historyBuilder.append("Log #").append(i + 1).append(": ").append(m.weightHistory.get(i)).append(" kg\n");
                    }
                }
                tvProgressWeightHistory.setText(historyBuilder.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ==================== TAB 4: REVIEWS & CHAT ====================
    private void setupReviewsChatTab() {
        tvReviewsRatingScore.setText(currentTrainer.rating + " ★");

        layoutReviewsContainer.removeAllViews();
        if (currentTrainer.feedback == null || currentTrainer.feedback.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No reviews recorded yet.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setTextSize(13);
            layoutReviewsContainer.addView(empty);
        } else {
            for (String f : currentTrainer.feedback) {
                LinearLayout card = new LinearLayout(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                lp.setMargins(0, 0, 0, 10);
                card.setLayoutParams(lp);
                card.setBackgroundResource(R.drawable.bg_card);
                card.setPadding(14, 14, 14, 14);
                card.setOrientation(LinearLayout.VERTICAL);

                TextView text = new TextView(this);
                text.setText(f);
                text.setTextColor(Color.parseColor("#E5E7EB"));
                text.setTextSize(13);

                card.addView(text);
                layoutReviewsContainer.addView(card);
            }
        }

        // Messenger Setup
        List<Member> assigned = getAssignedMembers();
        if (assigned.isEmpty()) {
            List<String> emptyList = new ArrayList<>();
            emptyList.add("No members assigned");
            spinnerChatMember.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, emptyList));
            tvChatHistory.setText("[System] Select an assigned member to open secure channel.");
            btnChatSend.setEnabled(false);
            return;
        }

        btnChatSend.setEnabled(true);
        List<String> names = new ArrayList<>();
        for (Member m : assigned) {
            names.add(m.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerChatMember.setAdapter(adapter);

        spinnerChatMember.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Member m = assigned.get(position);
                updateChatHistory(m);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnChatSend.setOnClickListener(v -> {
            int pos = spinnerChatMember.getSelectedItemPosition();
            if (pos >= 0 && pos < assigned.size()) {
                Member m = assigned.get(pos);
                String msgText = etChatMsg.getText().toString().trim();
                if (!msgText.isEmpty()) {
                    List<String> chat = getChatLogForMember(m.email);
                    chat.add("Trainer: " + msgText);
                    etChatMsg.setText("");
                    updateChatHistory(m);

                    // Simulate reply
                    new Handler().postDelayed(() -> {
                        if (!isFinishing()) {
                            chat.add(m.name + ": Thanks, coach! I will follow these instructions.");
                            updateChatHistory(m);
                        }
                    }, 1500);
                }
            }
        });
    }

    private List<String> getChatLogForMember(String email) {
        if (!memberChatLogs.containsKey(email)) {
            List<String> list = new ArrayList<>();
            list.add("[System] Secure encryption active.");
            list.add("Member: Hi trainer, what is my schedule today?");
            list.add("Trainer: Hello! Your routine and plans have been updated. Check them on your dashboard.");
            memberChatLogs.put(email, list);
        }
        return memberChatLogs.get(email);
    }

    private void updateChatHistory(Member m) {
        List<String> chat = getChatLogForMember(m.email);
        StringBuilder sb = new StringBuilder();
        for (String s : chat) {
            sb.append(s).append("\n\n");
        }
        tvChatHistory.setText(sb.toString());
    }

    // Helper conversion
    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
