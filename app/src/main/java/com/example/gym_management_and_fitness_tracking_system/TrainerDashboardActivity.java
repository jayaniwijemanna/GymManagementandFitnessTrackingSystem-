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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

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

    // Tab 1: Bookings / Members elements
    private LinearLayout layoutMembersList, layoutEmptyMembers;
    private TextView tvPendingCountBadge, tvStatTotal, tvStatPending, tvStatApproved;

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
    private FirebaseAuth mAuth;
    private ListenerRegistration bookingsListener;
    private ListenerRegistration membersListener;
    private final List<Booking> trainerBookings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trainer_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Apply Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_header), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Initialize views immediately so Firestore callbacks can update them
        initializeViews();
        setupNavigation();

        // Load trainer from Firestore — never fall back to hardcoded data
        String trainerEmail = getIntent().getStringExtra("TRAINER_EMAIL");
        loadTrainerAndSetup(trainerEmail);
    }

    /**
     * Loads the logged-in trainer's profile from Firestore.
     * Priority: Firebase Auth UID → email query → DataStore cache.
     * Never falls back to hardcoded data.
     */
    private void loadTrainerAndSetup(String trainerEmail) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            // Primary: load by Firebase Auth UID
            db.collection("users").document(firebaseUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Trainer t = doc.toObject(Trainer.class);
                            if (t != null && "trainer".equalsIgnoreCase(t.role)) {
                                t.id = doc.getId();
                                currentTrainer = t;
                                // Sync to DataStore
                                syncTrainerToDataStore(t);
                                onTrainerLoaded();
                                return;
                            }
                        }
                        // UID doc not a trainer — fall back to email query
                        loadTrainerByEmail(trainerEmail);
                    })
                    .addOnFailureListener(e -> loadTrainerByEmail(trainerEmail));
        } else {
            // No Firebase Auth session — query by email directly
            loadTrainerByEmail(trainerEmail);
        }
    }

    /**
     * Fallback: query the 'users' collection by email to find the trainer document.
     */
    private void loadTrainerByEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            // Email not provided — check DataStore cache from a previous session
            if (!DataStore.getInstance().trainers.isEmpty()) {
                currentTrainer = DataStore.getInstance().trainers.get(0);
                onTrainerLoaded();
            } else {
                // Truly nothing available — return to login
                Toast.makeText(this, "Could not load trainer profile. Please log in again.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "trainer")
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    if (!qs.isEmpty()) {
                        DocumentSnapshot doc = qs.getDocuments().get(0);
                        Trainer t = doc.toObject(Trainer.class);
                        if (t != null) {
                            t.id = doc.getId();
                            currentTrainer = t;
                            syncTrainerToDataStore(t);
                            onTrainerLoaded();
                            return;
                        }
                    }
                    // Firestore returned nothing — last resort: DataStore cache
                    Trainer cached = null;
                    for (Trainer t : DataStore.getInstance().trainers) {
                        if (t.email != null && t.email.equalsIgnoreCase(email)) {
                            cached = t;
                            break;
                        }
                    }
                    if (cached != null) {
                        currentTrainer = cached;
                        onTrainerLoaded();
                    } else {
                        Toast.makeText(this, "Trainer account not found. Please contact admin.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Network error — check DataStore cache
                    Trainer cached = null;
                    if (!TextUtils.isEmpty(email)) {
                        for (Trainer t : DataStore.getInstance().trainers) {
                            if (t.email != null && t.email.equalsIgnoreCase(email)) {
                                cached = t;
                                break;
                            }
                        }
                    }
                    if (cached != null) {
                        currentTrainer = cached;
                        Toast.makeText(this, "Loaded in offline mode.", Toast.LENGTH_SHORT).show();
                        onTrainerLoaded();
                    } else {
                        Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    /** Keep the DataStore trainer list in sync. */
    private void syncTrainerToDataStore(Trainer t) {
        List<Trainer> trainers = DataStore.getInstance().trainers;
        for (int i = 0; i < trainers.size(); i++) {
            if (trainers.get(i).email != null && trainers.get(i).email.equalsIgnoreCase(t.email)) {
                trainers.set(i, t);
                return;
            }
        }
        trainers.add(t);
    }

    /**
     * Called once currentTrainer is confirmed loaded from Firestore (or cache).
     * Sets up all UI and starts real-time listeners.
     */
    private void onTrainerLoaded() {
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
                        setupPlansTab();
                        setupProgressTab();
                        setupReviewsChatTab();
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
        tvPendingCountBadge = findViewById(R.id.tv_pending_count_badge);
        tvStatTotal = findViewById(R.id.tv_stat_total);
        tvStatPending = findViewById(R.id.tv_stat_pending);
        tvStatApproved = findViewById(R.id.tv_stat_approved);

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

    // ==================== TAB 1: BOOKING REQUESTS ====================
    private void refreshMembersTab() {
        layoutMembersList.removeAllViews();

        // Compute stats — exclude "Attended" bookings
        int totalCount = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        for (Booking b : trainerBookings) {
            String s = b.status != null ? b.status : "Pending";
            if ("Attended".equalsIgnoreCase(s)) continue;
            totalCount++;
            if ("Pending".equalsIgnoreCase(s)) pendingCount++;
            else if ("Accepted".equalsIgnoreCase(s)) approvedCount++;
        }

        // Update stat views
        if (tvStatTotal != null) tvStatTotal.setText(String.valueOf(totalCount));
        if (tvStatPending != null) tvStatPending.setText(String.valueOf(pendingCount));
        if (tvStatApproved != null) tvStatApproved.setText(String.valueOf(approvedCount));

        // Update pending badge in header
        if (tvPendingCountBadge != null) {
            if (pendingCount > 0) {
                tvPendingCountBadge.setText(pendingCount + " Pending");
                tvPendingCountBadge.setVisibility(View.VISIBLE);
                viewNotificationBadge.setVisibility(View.VISIBLE);
            } else {
                tvPendingCountBadge.setVisibility(View.GONE);
            }
        }

        // Show empty state if no displayable (non-Attended) bookings exist
        if (totalCount == 0) {
            layoutEmptyMembers.setVisibility(View.VISIBLE);
            return;
        }
        layoutEmptyMembers.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Booking b : trainerBookings) {
            // Skip attended — trainer sees only Pending / Accepted / Rejected
            String bStatus = b.status != null ? b.status : "Pending";
            if ("Attended".equalsIgnoreCase(bStatus)) continue;

            View itemView = inflater.inflate(R.layout.item_booking_request, layoutMembersList, false);

            TextView tvInitials = itemView.findViewById(R.id.tv_booking_initials);
            TextView tvMemberName = itemView.findViewById(R.id.tv_booking_member_name);
            TextView tvTime = itemView.findViewById(R.id.tv_booking_time);
            TextView tvPhone = itemView.findViewById(R.id.tv_booking_phone);
            TextView tvStatusBadge = itemView.findViewById(R.id.tv_booking_status_badge);
            LinearLayout btnApprove = itemView.findViewById(R.id.btn_approve_booking);
            LinearLayout btnDecline = itemView.findViewById(R.id.btn_decline_booking);
            LinearLayout layoutActions = itemView.findViewById(R.id.layout_action_buttons);
            TextView tvResolved = itemView.findViewById(R.id.tv_resolved_label);

            // Initials
            String initials = "?";
            if (b.memberName != null && !b.memberName.isEmpty()) {
                String[] parts = b.memberName.trim().split("\\s+");
                if (parts.length == 1) initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
                else initials = (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
            }
            tvInitials.setText(initials);
            tvMemberName.setText(b.memberName != null ? b.memberName : "Member");
            tvTime.setText(b.bookedTime != null ? b.bookedTime : "Time N/A");
            tvPhone.setText(b.memberPhone != null && !b.memberPhone.isEmpty() ? b.memberPhone : "No phone");

            // Configure status badge + action visibility
            switch (bStatus.toLowerCase()) {
                case "pending":
                    tvStatusBadge.setText("PENDING");
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#FF9500"));
                    layoutActions.setVisibility(View.VISIBLE);
                    tvResolved.setVisibility(View.GONE);
                    break;
                case "accepted":
                    tvStatusBadge.setText("APPROVED");
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#34C759"));
                    layoutActions.setVisibility(View.GONE);
                    tvResolved.setVisibility(View.VISIBLE);
                    tvResolved.setText("✓  Approved — Member has been notified");
                    tvResolved.setTextColor(Color.parseColor("#34C759"));
                    break;
                case "rejected":
                    tvStatusBadge.setText("DECLINED");
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#FF3B30"));
                    layoutActions.setVisibility(View.GONE);
                    tvResolved.setVisibility(View.VISIBLE);
                    tvResolved.setText("✗  Declined — Member has been notified");
                    tvResolved.setTextColor(Color.parseColor("#FF3B30"));
                    break;
                default:
                    tvStatusBadge.setText(bStatus.toUpperCase());
                    tvStatusBadge.getBackground().setTint(Color.parseColor("#94A3B8"));
                    layoutActions.setVisibility(View.GONE);
                    tvResolved.setVisibility(View.GONE);
                    break;
            }

            // Approve button
            btnApprove.setOnClickListener(v -> {
                new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                        .setTitle("Approve Booking")
                        .setMessage("Approve session with " + b.memberName + "\nScheduled: " + b.bookedTime + "?")
                        .setPositiveButton("Approve", (dialog, which) -> approveBooking(b))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Decline button
            btnDecline.setOnClickListener(v -> {
                new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                        .setTitle("Decline Booking")
                        .setMessage("Decline session with " + b.memberName + "?\nThe member will be notified.")
                        .setPositiveButton("Decline", (dialog, which) -> declineBooking(b))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            layoutMembersList.addView(itemView);
        }
    }

    /** Approve a booking: update Firestore + push notification to member **/
    private void approveBooking(Booking b) {
        b.status = "Accepted";

        // Update bookings collection
        if (b.id != null && !b.id.isEmpty()) {
            db.collection("bookings").document(b.id).update("status", "Accepted")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✓ Approved: " + b.memberName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        // Update member's bookingStatus in users collection
        if (b.memberId != null && !b.memberId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("bookingStatus", "Accepted");
            updates.put("bookedTrainer", currentTrainer.name);
            db.collection("users").document(b.memberId).update(updates);

            // Push a notification document into Firestore for the member to receive
            pushNotificationToMember(
                    b.memberId,
                    b.memberName,
                    "📅 Booking Approved!",
                    "Your training session with " + currentTrainer.name + " on " + b.bookedTime + " has been APPROVED. See you at the gym!"
            );
        }

        refreshMembersTab();
    }

    /** Decline a booking: update Firestore + push notification to member **/
    private void declineBooking(Booking b) {
        b.status = "Rejected";

        // Update bookings collection
        if (b.id != null && !b.id.isEmpty()) {
            db.collection("bookings").document(b.id).update("status", "Rejected")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Declined booking for " + b.memberName, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        // Update member's bookingStatus in users collection
        if (b.memberId != null && !b.memberId.isEmpty()) {
            db.collection("users").document(b.memberId).update("bookingStatus", "Rejected");

            // Push a notification document into Firestore for the member to receive
            pushNotificationToMember(
                    b.memberId,
                    b.memberName,
                    "❌ Booking Declined",
                    "Your training session request with " + currentTrainer.name + " on " + b.bookedTime + " has been declined. Please choose another time slot."
            );
        }

        refreshMembersTab();
    }

    /**
     * Appends a notification string directly into the member's user document in the 'users' collection.
     * The member's live snapshot listener will receive this automatically and display the bell alert.
     */
    private void pushNotificationToMember(String memberId, String memberName, String title, String message) {
        if (TextUtils.isEmpty(memberId)) return;

        String notifMsg = title + " • " + message;

        db.collection("users").document(memberId)
                .update("notifications", com.google.firebase.firestore.FieldValue.arrayUnion(notifMsg))
                .addOnFailureListener(e -> {
                    // Fallback if notifications field was absent
                    Map<String, Object> map = new HashMap<>();
                    map.put("notifications", java.util.Arrays.asList(notifMsg));
                    db.collection("users").document(memberId)
                            .set(map, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private List<Member> getAssignedMembers() {
        List<Member> list = new ArrayList<>();
        if (currentTrainer == null) return list;

        for (Member m : DataStore.getInstance().members) {
            boolean isAssigned = false;
            if (m.bookedTrainer != null && m.bookedTrainer.equalsIgnoreCase(currentTrainer.name)) {
                isAssigned = true;
            } else {
                for (Booking b : trainerBookings) {
                    if ("Accepted".equalsIgnoreCase(b.status)
                            && ((b.memberId != null && b.memberId.equalsIgnoreCase(m.id))
                            || (b.memberEmail != null && m.email != null && b.memberEmail.equalsIgnoreCase(m.email)))) {
                        isAssigned = true;
                        break;
                    }
                }
            }
            if (isAssigned) {
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
                String notif = "Trainer " + currentTrainer.name + " updated your Workout Plan.";
                if (m.id != null && !m.id.isEmpty()) {
                    db.collection("users").document(m.id)
                            .update("workoutPlan", m.workoutPlan,
                                    "notifications", com.google.firebase.firestore.FieldValue.arrayUnion(notif));
                }
                Toast.makeText(this, "Workout plan updated for " + m.name, Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveDiet.setOnClickListener(v -> {
            int pos = spinnerPlanMember.getSelectedItemPosition();
            if (pos >= 0 && pos < assigned.size()) {
                Member m = assigned.get(pos);
                m.dietPlan = etDietPlan.getText().toString().trim();
                String notif = "Trainer " + currentTrainer.name + " updated your Diet Plan.";
                if (m.id != null && !m.id.isEmpty()) {
                    db.collection("users").document(m.id)
                            .update("dietPlan", m.dietPlan,
                                    "notifications", com.google.firebase.firestore.FieldValue.arrayUnion(notif));
                }
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
            tvProgressWeightHistory.setText("No member selected");
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
                fetchAndDisplayMemberProgress(m);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Fetches live member metrics and 'fitness_logs' from Firestore for the selected member.
     */
    private void fetchAndDisplayMemberProgress(Member m) {
        if (m == null) return;

        // Fetch live user doc first to ensure up-to-date height/weight/water
        if (m.id != null && !m.id.isEmpty()) {
            db.collection("users").document(m.id).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Member liveMember = doc.toObject(Member.class);
                    if (liveMember != null) {
                        liveMember.id = doc.getId();
                        updateProgressMetricsUI(liveMember);
                        loadFitnessLogsFromDb(liveMember);
                        return;
                    }
                }
                updateProgressMetricsUI(m);
                loadFitnessLogsFromDb(m);
            }).addOnFailureListener(e -> {
                updateProgressMetricsUI(m);
                loadFitnessLogsFromDb(m);
            });
        } else {
            updateProgressMetricsUI(m);
            loadFitnessLogsFromDb(m);
        }
    }

    private void updateProgressMetricsUI(Member m) {
        double height = m.height > 0 ? m.height : 175.0;
        double weight = m.weight > 0 ? m.weight : 70.0;
        double hM = height / 100.0;
        double bmi = (hM > 0) ? (weight / (hM * hM)) : 0;

        String cat;
        if (bmi < 18.5) cat = "Underweight";
        else if (bmi < 25.0) cat = "Normal weight";
        else if (bmi < 30.0) cat = "Overweight";
        else cat = "Obese";

        tvProgressBmi.setText(String.format(Locale.getDefault(), "%.1f (%s)", bmi, cat));
        tvProgressWater.setText(m.waterIntake + " / 8 Glasses");
        tvProgressHeightWeight.setText("Height: " + String.format(Locale.getDefault(), "%.0f cm", height) +
                "  |  Weight: " + String.format(Locale.getDefault(), "%.1f kg", weight));
    }

    private void loadFitnessLogsFromDb(Member m) {
        if (tvProgressWeightHistory == null) return;
        tvProgressWeightHistory.setText("Loading live fitness logs from DB...");

        if (m.id != null && !m.id.isEmpty()) {
            db.collection("fitness_logs")
                    .whereEqualTo("memberId", m.id)
                    .get()
                    .addOnSuccessListener(qs -> displayProgressLogs(qs, m))
                    .addOnFailureListener(e -> fetchProgressLogsByEmail(m));
        } else if (m.email != null && !m.email.isEmpty()) {
            fetchProgressLogsByEmail(m);
        } else {
            displayFallbackWeightHistory(m);
        }
    }

    private void fetchProgressLogsByEmail(Member m) {
        if (m.email == null || m.email.isEmpty()) {
            displayFallbackWeightHistory(m);
            return;
        }
        db.collection("fitness_logs")
                .whereEqualTo("memberEmail", m.email)
                .get()
                .addOnSuccessListener(qs -> displayProgressLogs(qs, m))
                .addOnFailureListener(e -> displayFallbackWeightHistory(m));
    }

    private void displayProgressLogs(QuerySnapshot querySnapshot, Member m) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            displayFallbackWeightHistory(m);
            return;
        }

        List<DocumentSnapshot> docs = new ArrayList<>(querySnapshot.getDocuments());
        java.util.Collections.sort(docs, (d1, d2) -> {
            com.google.firebase.Timestamp t1 = d1.getTimestamp("timestamp");
            com.google.firebase.Timestamp t2 = d2.getTimestamp("timestamp");
            if (t1 != null && t2 != null) return t2.compareTo(t1);
            return 0;
        });

        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (DocumentSnapshot doc : docs) {
            String date = doc.getString("date");
            Double weight = doc.getDouble("weight");
            Double bmi = doc.getDouble("bmi");
            String cat = doc.getString("bmiCategory");
            Double height = doc.getDouble("height");

            sb.append("📅 ").append(date != null ? date : "Log #" + count).append("\n");
            sb.append("   • Weight: ").append(weight != null ? String.format(Locale.getDefault(), "%.1f kg", weight) : "N/A");
            if (height != null && height > 0) {
                sb.append(" (").append(String.format(Locale.getDefault(), "%.0f cm", height)).append(")");
            }
            sb.append("\n");
            sb.append("   • BMI: ").append(bmi != null ? String.format(Locale.getDefault(), "%.1f", bmi) : "N/A");
            sb.append(" (").append(cat != null ? cat : "Normal").append(")\n\n");
            count++;
        }

        if (sb.length() > 0) {
            tvProgressWeightHistory.setText(sb.toString().trim());
        } else {
            displayFallbackWeightHistory(m);
        }
    }

    private void displayFallbackWeightHistory(Member m) {
        StringBuilder sb = new StringBuilder();
        if (m.weightHistory == null || m.weightHistory.isEmpty()) {
            sb.append("No weight logs recorded yet.");
        } else {
            for (int i = 0; i < m.weightHistory.size(); i++) {
                sb.append("Log #").append(i + 1).append(": ").append(m.weightHistory.get(i)).append(" kg\n");
            }
        }
        tvProgressWeightHistory.setText(sb.toString().trim());
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
