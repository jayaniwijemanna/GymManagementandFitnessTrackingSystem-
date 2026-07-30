package com.example.gym_management_and_fitness_tracking_system;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MemberDashboardActivity extends AppCompatActivity {

    private Member currentMember;

    // Firestore
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration memberListener;
    private ListenerRegistration packagesListener;
    private ListenerRegistration trainersListener;

    // Local list references from DataStore (kept in sync by Firestore listeners)
    private final List<GymPackage> packageList = DataStore.getInstance().packages;
    private final List<Trainer> trainerList = DataStore.getInstance().trainers;

    // Header views
    private TextView tvHeaderInitials, tvHeaderName;
    private RelativeLayout btnNotifications;
    private View viewNotificationBadge;
    private FrameLayout btnLogout;

    // View Containers (Tabs)
    private ScrollView viewHome, viewPrograms, viewFitness, viewFeedback;

    // Navigation Tab Buttons
    private LinearLayout navTabHome, navTabPrograms, navTabFitness, navTabFeedback;
    private ImageView imgNavHome, imgNavPrograms, imgNavFitness, imgNavFeedback;
    private TextView tvNavHome, tvNavPrograms, tvNavFitness, tvNavFeedback;

    // Home Tab elements
    private TextView tvHomePlanName, tvHomePlanStatus, tvHomePlanDesc;
    private TextView tvHomeCheckinStatus, tvHomeCheckinBadge;
    private TextView tvHomeBookingInfo, tvHomeWorkoutPlan, tvHomeDietPlan, tvHomeChatTrainerName;
    private TextView tvPendingPlanInfo;
    private LinearLayout btnHomeBuyPlan, btnHomeChangePlan, btnHomeCheckin, btnHomeBookTrainer, btnHomeChat;
    private LinearLayout layoutPendingPlanInfo;

    // Programs Tab elements
    private LinearLayout layoutPackagesList, layoutTrainersList;

    // Fitness Tab elements
    private EditText etBmiHeight, etBmiWeight;
    private LinearLayout btnCalculateBmi;
    private FrameLayout btnWaterMinus, btnWaterPlus;
    private LinearLayout layoutBmiResult;
    private TextView tvBmiScore, tvBmiCategory, tvWeightHistoryList, tvWaterCount;

    // Feedback Tab elements
    private Spinner spinnerFeedbackTrainer;
    private LinearLayout layoutRatingStars;
    private EditText etFeedbackMsg;
    private LinearLayout btnSubmitFeedback;
    private int selectedRating = 0;
    private ImageView[] starViews = new ImageView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_member_dashboard);

        // Apply Insets
        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.layout_header).getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        setupNavigation();

        // Load data from Firestore
        String memberEmail = getIntent().getStringExtra("MEMBER_EMAIL");
        loadMemberFromFirestore(memberEmail);
        listenToPackagesRealtime();
        listenToTrainersRealtime();
    }

    // ==================== FIRESTORE DATA LOADING ====================

    /**
     * Loads the current member's document from Firestore in real-time.
     * First tries to match by Firebase Auth UID, then falls back to email query.
     */
    private void loadMemberFromFirestore(String memberEmail) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            // Primary: listen to authenticated user's doc by UID
            String uid = firebaseUser.getUid();
            memberListener = db.collection("users").document(uid)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Toast.makeText(this, "Error loading profile: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            fallbackLoadByEmail(memberEmail);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            Member m = snapshot.toObject(Member.class);
                            if (m != null) {
                                m.id = snapshot.getId();
                                // Preserve runtime-only fields (notifications, waterIntake) if member already loaded
                                if (currentMember != null) {
                                    m.notifications = currentMember.notifications;
                                    m.waterIntake = currentMember.waterIntake;
                                    m.weightHistory = currentMember.weightHistory;
                                }
                                currentMember = m;
                                // Update DataStore reference
                                syncMemberToDataStore(m);
                                onMemberLoaded();
                            }
                        } else {
                            fallbackLoadByEmail(memberEmail);
                        }
                    });
        } else {
            fallbackLoadByEmail(memberEmail);
        }
    }

    /**
     * Fallback: query by email if no Firebase Auth session (e.g. offline login).
     */
    private void fallbackLoadByEmail(String memberEmail) {
        if (TextUtils.isEmpty(memberEmail)) {
            // Last resort: use DataStore or create a temporary member
            if (!DataStore.getInstance().members.isEmpty()) {
                currentMember = DataStore.getInstance().members.get(0);
            } else {
                currentMember = new Member("Guest User", "guest@gmail.com", "N/A", "None");
            }
            onMemberLoaded();
            return;
        }

        db.collection("users").whereEqualTo("email", memberEmail)
                .whereEqualTo("role", "member")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Member m = doc.toObject(Member.class);
                        if (m != null) {
                            m.id = doc.getId();
                            currentMember = m;
                            syncMemberToDataStore(m);
                            // Now attach a real-time listener on this doc for live updates
                            attachMemberDocumentListener(doc.getId());
                        } else {
                            currentMember = new Member("Unknown", memberEmail, "N/A", "None");
                        }
                    } else {
                        // Not found in Firestore — check DataStore in-memory
                        currentMember = null;
                        for (Member m : DataStore.getInstance().members) {
                            if (m.email.equalsIgnoreCase(memberEmail)) {
                                currentMember = m;
                                break;
                            }
                        }
                        if (currentMember == null) {
                            currentMember = new Member("Unknown", memberEmail, "N/A", "None");
                        }
                    }
                    onMemberLoaded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Use DataStore fallback
                    for (Member m : DataStore.getInstance().members) {
                        if (m.email.equalsIgnoreCase(memberEmail)) {
                            currentMember = m;
                            break;
                        }
                    }
                    if (currentMember == null) {
                        currentMember = new Member("Unknown", memberEmail, "N/A", "None");
                    }
                    onMemberLoaded();
                });
    }

    /**
     * Attach a real-time snapshot listener to a member document (by doc ID).
     * Used after the fallback email query to keep data live.
     */
    private void attachMemberDocumentListener(String docId) {
        if (memberListener != null) {
            memberListener.remove();
        }
        memberListener = db.collection("users").document(docId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    Member m = snapshot.toObject(Member.class);
                    if (m != null) {
                        m.id = snapshot.getId();
                        // Preserve runtime-only transient fields
                        if (currentMember != null) {
                            m.notifications = currentMember.notifications;
                            m.waterIntake = currentMember.waterIntake;
                        }
                        currentMember = m;
                        syncMemberToDataStore(m);
                        // Refresh UI if already initialized
                        refreshHomeTab();
                        refreshFitnessTab();
                    }
                });
    }

    /**
     * Keep the DataStore member list in sync (replace or add the current member).
     */
    private void syncMemberToDataStore(Member m) {
        List<Member> members = DataStore.getInstance().members;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).email.equalsIgnoreCase(m.email)) {
                members.set(i, m);
                return;
            }
        }
        members.add(m);
    }

    /**
     * Called once the current member object is populated.
     */
    private void onMemberLoaded() {
        setupHeader();
        refreshHomeTab();
        setupProgramsTab();
        setupFitnessTab();
        setupFeedbackTab();
    }

    /**
     * Real-time listener for the 'packages' Firestore collection.
     */
    private void listenToPackagesRealtime() {
        packagesListener = db.collection("packages").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading packages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                packageList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    GymPackage pkg = doc.toObject(GymPackage.class);
                    if (pkg != null) {
                        pkg.id = doc.getId();
                        packageList.add(pkg);
                    }
                }
                // Refresh programs tab if member is already loaded
                if (currentMember != null) {
                    setupProgramsTab();
                    refreshHomeTab(); // Package description might have changed
                }
            }
        });
    }

    /**
     * Real-time listener for all trainers in the 'users' collection.
     */
    private void listenToTrainersRealtime() {
        trainersListener = db.collection("users").whereEqualTo("role", "trainer")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading trainers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        trainerList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Trainer t = doc.toObject(Trainer.class);
                            if (t != null) {
                                t.id = doc.getId();
                                trainerList.add(t);
                            }
                        }
                        // Refresh programs & feedback tabs if member is loaded
                        if (currentMember != null) {
                            setupProgramsTab();
                            setupFeedbackTab();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firestore listeners to avoid memory leaks
        if (memberListener != null) memberListener.remove();
        if (packagesListener != null) packagesListener.remove();
        if (trainersListener != null) trainersListener.remove();
    }

    // ==================== VIEW INITIALIZATION ====================

    private void initializeViews() {
        // Header
        tvHeaderInitials = findViewById(R.id.tv_header_initials);
        tvHeaderName = findViewById(R.id.tv_header_name);
        btnNotifications = findViewById(R.id.btn_notifications);
        viewNotificationBadge = findViewById(R.id.view_notification_badge);
        btnLogout = findViewById(R.id.btn_logout);

        // Main Tabs
        viewHome = findViewById(R.id.view_home);
        viewPrograms = findViewById(R.id.view_programs);
        viewFitness = findViewById(R.id.view_fitness);
        viewFeedback = findViewById(R.id.view_feedback);

        // Bottom Navigation tabs
        navTabHome = findViewById(R.id.nav_tab_home);
        navTabPrograms = findViewById(R.id.nav_tab_programs);
        navTabFitness = findViewById(R.id.nav_tab_fitness);
        navTabFeedback = findViewById(R.id.nav_tab_feedback);

        imgNavHome = findViewById(R.id.img_nav_home);
        imgNavPrograms = findViewById(R.id.img_nav_programs);
        imgNavFitness = findViewById(R.id.img_nav_fitness);
        imgNavFeedback = findViewById(R.id.img_nav_feedback);

        tvNavHome = findViewById(R.id.tv_nav_home);
        tvNavPrograms = findViewById(R.id.tv_nav_programs);
        tvNavFitness = findViewById(R.id.tv_nav_fitness);
        tvNavFeedback = findViewById(R.id.tv_nav_feedback);

        // Home Tab details
        tvHomePlanName = findViewById(R.id.tv_home_plan_name);
        tvHomePlanStatus = findViewById(R.id.tv_home_plan_status);
        tvHomePlanDesc = findViewById(R.id.tv_home_plan_desc);
        tvHomeCheckinStatus = findViewById(R.id.tv_home_checkin_status);
        tvHomeCheckinBadge = findViewById(R.id.tv_home_checkin_badge);
        tvHomeBookingInfo = findViewById(R.id.tv_home_booking_info);
        tvHomeWorkoutPlan = findViewById(R.id.tv_home_workout_plan);
        tvHomeDietPlan = findViewById(R.id.tv_home_diet_plan);
        tvHomeChatTrainerName = findViewById(R.id.tv_home_chat_trainer_name);
        btnHomeBuyPlan = findViewById(R.id.btn_home_buy_plan);
        btnHomeChangePlan = findViewById(R.id.btn_home_change_plan);
        layoutPendingPlanInfo = findViewById(R.id.layout_pending_plan_info);
        tvPendingPlanInfo = findViewById(R.id.tv_pending_plan_info);
        btnHomeCheckin = findViewById(R.id.btn_home_checkin);
        btnHomeBookTrainer = findViewById(R.id.btn_home_book_trainer);
        btnHomeChat = findViewById(R.id.btn_home_chat);

        // Programs Tab details
        layoutPackagesList = findViewById(R.id.layout_packages_list);
        layoutTrainersList = findViewById(R.id.layout_trainers_list);

        // Fitness Tab details
        etBmiHeight = findViewById(R.id.et_bmi_height);
        etBmiWeight = findViewById(R.id.et_bmi_weight);
        btnCalculateBmi = findViewById(R.id.btn_calculate_bmi);
        layoutBmiResult = findViewById(R.id.layout_bmi_result);
        tvBmiScore = findViewById(R.id.tv_bmi_score);
        tvBmiCategory = findViewById(R.id.tv_bmi_category);
        tvWeightHistoryList = findViewById(R.id.tv_weight_history_list);
        tvWaterCount = findViewById(R.id.tv_water_count);
        btnWaterMinus = findViewById(R.id.btn_water_minus);
        btnWaterPlus = findViewById(R.id.btn_water_plus);

        // Feedback Tab details
        spinnerFeedbackTrainer = findViewById(R.id.spinner_feedback_trainer);
        layoutRatingStars = findViewById(R.id.layout_rating_stars);
        etFeedbackMsg = findViewById(R.id.et_feedback_msg);
        btnSubmitFeedback = findViewById(R.id.btn_submit_feedback);

        starViews[0] = findViewById(R.id.img_star_1);
        starViews[1] = findViewById(R.id.img_star_2);
        starViews[2] = findViewById(R.id.img_star_3);
        starViews[3] = findViewById(R.id.img_star_4);
        starViews[4] = findViewById(R.id.img_star_5);
    }

    private void setupHeader() {
        if (currentMember == null) return;
        tvHeaderName.setText(currentMember.name);
        tvHeaderInitials.setText(currentMember.getInitials());

        // Update badge visibility
        updateNotificationBadge();

        btnNotifications.setOnClickListener(v -> showNotificationsDialog());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out securely.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MemberDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateNotificationBadge() {
        if (currentMember == null || currentMember.notifications.isEmpty()) {
            viewNotificationBadge.setVisibility(View.GONE);
        } else {
            viewNotificationBadge.setVisibility(View.VISIBLE);
        }
    }

    private void showNotificationsDialog() {
        // Clear badge
        viewNotificationBadge.setVisibility(View.GONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        builder.setTitle("Notifications Alert");

        if (currentMember == null || currentMember.notifications.isEmpty()) {
            builder.setMessage("No new notifications.");
        } else {
            String[] array = currentMember.notifications.toArray(new String[0]);
            builder.setItems(array, null);
        }

        builder.setPositiveButton("Clear All", (dialog, which) -> {
            if (currentMember != null) currentMember.notifications.clear();
            updateNotificationBadge();
            Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    // Swapping content screens
    private void setupNavigation() {
        navTabHome.setOnClickListener(v -> selectTab(0));
        navTabPrograms.setOnClickListener(v -> selectTab(1));
        navTabFitness.setOnClickListener(v -> selectTab(2));
        navTabFeedback.setOnClickListener(v -> selectTab(3));
    }

    private void selectTab(int index) {
        // Hide all views
        viewHome.setVisibility(View.GONE);
        viewPrograms.setVisibility(View.GONE);
        viewFitness.setVisibility(View.GONE);
        viewFeedback.setVisibility(View.GONE);

        // Reset navigation colors
        resetTabStyle(imgNavHome, tvNavHome);
        resetTabStyle(imgNavPrograms, tvNavPrograms);
        resetTabStyle(imgNavFitness, tvNavFitness);
        resetTabStyle(imgNavFeedback, tvNavFeedback);

        // Highlight selected
        switch (index) {
            case 0:
                viewHome.setVisibility(View.VISIBLE);
                highlightTab(imgNavHome, tvNavHome);
                refreshHomeTab();
                break;
            case 1:
                viewPrograms.setVisibility(View.VISIBLE);
                highlightTab(imgNavPrograms, tvNavPrograms);
                setupProgramsTab();
                break;
            case 2:
                viewFitness.setVisibility(View.VISIBLE);
                highlightTab(imgNavFitness, tvNavFitness);
                refreshFitnessTab();
                break;
            case 3:
                viewFeedback.setVisibility(View.VISIBLE);
                highlightTab(imgNavFeedback, tvNavFeedback);
                setupFeedbackTab();
                break;
        }
    }

    private void resetTabStyle(ImageView img, TextView txt) {
        img.setColorFilter(Color.parseColor("#4A5568")); // nav_inactive
        txt.setTextColor(Color.parseColor("#4A5568"));
    }

    private void highlightTab(ImageView img, TextView txt) {
        img.setColorFilter(Color.parseColor("#007AFF")); // nav_active / accent_blue
        txt.setTextColor(Color.parseColor("#007AFF"));
    }

    // ==================== TAB 1: HOME ====================
    private void refreshHomeTab() {
        if (currentMember == null) return;

        boolean hasActivePlan = currentMember.plan != null && !currentMember.plan.trim().isEmpty()
                && !currentMember.plan.equalsIgnoreCase("None") && !currentMember.plan.equalsIgnoreCase("No Package");

        String status = currentMember.planStatus;
        if (status == null || status.trim().isEmpty() || "None".equalsIgnoreCase(status)) {
            if (hasActivePlan) {
                status = "Active";
                currentMember.planStatus = "Active";
            } else {
                status = "None";
            }
        }

        // Hide all plan action buttons by default
        btnHomeBuyPlan.setVisibility(View.GONE);
        btnHomeChangePlan.setVisibility(View.GONE);
        layoutPendingPlanInfo.setVisibility(View.GONE);

        // --- ACTIVE plan ---
        if ("Active".equalsIgnoreCase(status) && hasActivePlan) {
            tvHomePlanName.setText(currentMember.plan);
            tvHomePlanStatus.setText("Active");
            tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
            tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
            tvHomePlanStatus.setTextColor(Color.parseColor("#34C759"));

            // Fetch package description from the live list
            String desc = "Your premium package allows access to standard gym areas.";
            for (GymPackage p : packageList) {
                if (p.name != null && p.name.equalsIgnoreCase(currentMember.plan)) {
                    if (p.description != null && !p.description.isEmpty()) desc = p.description;
                    break;
                }
            }
            tvHomePlanDesc.setText(desc);
            btnHomeChangePlan.setVisibility(View.VISIBLE);

        // --- PENDING approval ---
        } else if ("Pending".equalsIgnoreCase(status)) {
            String pendingName = (currentMember.pendingPlan != null && !currentMember.pendingPlan.isEmpty())
                    ? currentMember.pendingPlan : "Requested Plan";

            if (hasActivePlan) {
                tvHomePlanName.setText(currentMember.plan);
                tvHomePlanStatus.setText("Pending Change");
                tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
                tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D")));
                tvHomePlanStatus.setTextColor(Color.parseColor("#FF9500"));

                String desc = "Your active package remains usable while your request is processed.";
                for (GymPackage p : packageList) {
                    if (p.name != null && p.name.equalsIgnoreCase(currentMember.plan)) {
                        if (p.description != null && !p.description.isEmpty()) desc = p.description;
                        break;
                    }
                }
                tvHomePlanDesc.setText(desc);
                layoutPendingPlanInfo.setVisibility(View.VISIBLE);
                tvPendingPlanInfo.setText("Request to change to \"" + pendingName + "\" is pending admin approval.");
            } else {
                tvHomePlanName.setText("No Active Plan");
                tvHomePlanStatus.setText("Pending");
                tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
                tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D")));
                tvHomePlanStatus.setTextColor(Color.parseColor("#FF9500"));
                tvHomePlanDesc.setText("Your plan application is awaiting admin review and approval.");
                layoutPendingPlanInfo.setVisibility(View.VISIBLE);
                tvPendingPlanInfo.setText("Application for \"" + pendingName + "\" is pending admin approval.");
            }

        // --- REJECTED ---
        } else if ("Rejected".equalsIgnoreCase(status)) {
            String planDisplay = hasActivePlan ? currentMember.plan : "No Active Plan";
            tvHomePlanName.setText(planDisplay);
            tvHomePlanStatus.setText("Rejected");
            tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
            tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2D1010")));
            tvHomePlanStatus.setTextColor(Color.parseColor("#FF3B30"));
            tvHomePlanDesc.setText("Your plan application was rejected. You may apply for a different plan.");
            layoutPendingPlanInfo.setVisibility(View.VISIBLE);
            tvPendingPlanInfo.setText("Application rejected. Please apply again from the Programs tab.");
            tvPendingPlanInfo.setTextColor(Color.parseColor("#FF3B30"));
            btnHomeBuyPlan.setVisibility(View.VISIBLE);

        // --- NONE / No Plan ---
        } else {
            tvHomePlanName.setText("No Active Package");
            tvHomePlanStatus.setText("Inactive");
            tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
            tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D")));
            tvHomePlanStatus.setTextColor(Color.parseColor("#FF9500"));
            tvHomePlanDesc.setText("Apply for a training package in the Programs tab to gain access.");
            btnHomeBuyPlan.setVisibility(View.VISIBLE);
        }

        // Check in status
        if (currentMember.checkedInTime == null || currentMember.checkedInTime.equals("Not Checked In")) {
            tvHomeCheckinStatus.setText("Not Checked In Today");
            tvHomeCheckinBadge.setText("Absent");
            tvHomeCheckinBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2D1010")));
            tvHomeCheckinBadge.setTextColor(Color.parseColor("#FF3B30"));
            btnHomeCheckin.setVisibility(View.VISIBLE);
        } else {
            tvHomeCheckinStatus.setText("Checked In at " + currentMember.checkedInTime);
            tvHomeCheckinBadge.setText("Present");
            tvHomeCheckinBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
            tvHomeCheckinBadge.setTextColor(Color.parseColor("#34C759"));
            btnHomeCheckin.setVisibility(View.GONE);
        }

        // Trainer booking
        if (currentMember.bookedTrainer == null || currentMember.bookedTrainer.equals("None")) {
            tvHomeBookingInfo.setText("No active personal trainer booking.");
            btnHomeBookTrainer.setVisibility(View.VISIBLE);
            tvHomeChatTrainerName.setText("No active personal trainer");
            btnHomeChat.setEnabled(false);
            btnHomeChat.setAlpha(0.5f);
        } else {
            tvHomeBookingInfo.setText("Booked: " + currentMember.bookedTrainer + " (" + currentMember.bookingStatus + ")\nTime: " + currentMember.bookedTime);
            btnHomeBookTrainer.setVisibility(View.GONE);
            tvHomeChatTrainerName.setText("Chat with " + currentMember.bookedTrainer);
            btnHomeChat.setEnabled(true);
            btnHomeChat.setAlpha(1.0f);
        }

        // Workout and Diet Plans binding
        if (currentMember.workoutPlan == null || currentMember.workoutPlan.trim().isEmpty()) {
            tvHomeWorkoutPlan.setText("No workout plan assigned yet. Ask your trainer!");
        } else {
            tvHomeWorkoutPlan.setText(currentMember.workoutPlan);
        }

        if (currentMember.dietPlan == null || currentMember.dietPlan.trim().isEmpty()) {
            tvHomeDietPlan.setText("No diet plan assigned yet. Ask your trainer!");
        } else {
            tvHomeDietPlan.setText(currentMember.dietPlan);
        }

        btnHomeBuyPlan.setOnClickListener(v -> selectTab(1));
        btnHomeChangePlan.setOnClickListener(v -> selectTab(1));
        btnHomeBookTrainer.setOnClickListener(v -> selectTab(1));
        btnHomeCheckin.setOnClickListener(v -> triggerCheckinFlow());
        btnHomeChat.setOnClickListener(v -> openChatDialog());
    }

    private void triggerCheckinFlow() {
        // Show simulated QR scanner dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        LayoutInflater inflater = getLayoutInflater();

        // Dynamically construct a premium scan dialog
        View scanLayout = inflater.inflate(R.layout.activity_verify_otp, null);
        TextView header = scanLayout.findViewById(R.id.title_titan_gym);
        header.setText("QR SCANNER");
        TextView inst = scanLayout.findViewById(R.id.tv_otp_instructions);
        inst.setText("Align gym QR code within the frame to check in.");

        // Replace input box with scan line simulation
        LinearLayout inputGroup = (LinearLayout) scanLayout.findViewById(R.id.et_otp).getParent();
        inputGroup.removeAllViews();

        TextView scanAnim = new TextView(this);
        scanAnim.setText("[ SCANNING CAMERA VIEW ]\n\n════════════════════");
        scanAnim.setTextColor(Color.parseColor("#34C759"));
        scanAnim.setGravity(android.view.Gravity.CENTER);
        scanAnim.setPadding(0, 30, 0, 30);
        inputGroup.addView(scanAnim);

        TextView note = scanLayout.findViewById(R.id.tv_cancel);
        note.setText("Searching for camera sensor...");
        note.setTextColor(Color.parseColor("#94A3B8"));

        // Hide verify button
        scanLayout.findViewById(R.id.btn_verify).setVisibility(View.GONE);

        builder.setView(scanLayout);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Simulate laser scanner and successful scan in 2 seconds
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();

                // Set present checked-in time
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String checkinTime = sdf.format(new Date());
                currentMember.checkedInTime = checkinTime;
                currentMember.notifications.add("Checked in successfully at " + checkinTime);
                updateNotificationBadge();
                refreshHomeTab();

                // Persist check-in to Firestore
                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    db.collection("users").document(currentMember.id)
                            .update("checkedInTime", checkinTime)
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Check-in saved locally only.", Toast.LENGTH_SHORT).show());
                }

                Toast.makeText(this, "Gym Check-in Successful! Welcome to Titan.", Toast.LENGTH_LONG).show();
            }
        }, 2200);

        scanLayout.findViewById(R.id.tv_cancel).setOnClickListener(v -> dialog.dismiss());
    }

    // ==================== TAB 2: PROGRAMS & BOOKING ====================
    private void setupProgramsTab() {
        if (currentMember == null) return;

        // Load Packages list from Firestore-synced packageList
        layoutPackagesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        boolean hasActivePlan = currentMember.plan != null && !currentMember.plan.trim().isEmpty()
                && !currentMember.plan.equalsIgnoreCase("None") && !currentMember.plan.equalsIgnoreCase("No Package");

        String memberPlanStatus = currentMember.planStatus;
        if (memberPlanStatus == null || memberPlanStatus.trim().isEmpty() || "None".equalsIgnoreCase(memberPlanStatus)) {
            if (hasActivePlan) {
                memberPlanStatus = "Active";
                currentMember.planStatus = "Active";
            } else {
                memberPlanStatus = "None";
            }
        }
        String memberPlan = currentMember.plan != null ? currentMember.plan : "";
        String memberPendingPlan = currentMember.pendingPlan != null ? currentMember.pendingPlan : "";

        if (packageList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No packages available. Check back later.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(0, 16, 0, 16);
            layoutPackagesList.addView(empty);
        } else {
            for (GymPackage p : packageList) {
                View pkgView = inflater.inflate(R.layout.item_member_package, layoutPackagesList, false);
                TextView tvName = pkgView.findViewById(R.id.tv_pkg_name);
                TextView tvPrice = pkgView.findViewById(R.id.tv_pkg_price);
                TextView tvDesc = pkgView.findViewById(R.id.tv_pkg_desc);
                TextView btnBuy = pkgView.findViewById(R.id.btn_pkg_buy);

                tvName.setText(p.name);
                tvPrice.setText("$" + p.price + " / month");
                tvDesc.setText(p.description);

                boolean isActivePlan = "Active".equalsIgnoreCase(memberPlanStatus)
                        && p.name != null && p.name.equalsIgnoreCase(memberPlan);
                boolean isPendingThisPlan = "Pending".equalsIgnoreCase(memberPlanStatus)
                        && p.name != null && p.name.equalsIgnoreCase(memberPendingPlan);

                if (isActivePlan) {
                    // This is the current active plan
                    btnBuy.setText("Active ✓");
                    btnBuy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
                    btnBuy.setTextColor(Color.parseColor("#34C759"));
                    btnBuy.setClickable(false);
                } else if (isPendingThisPlan) {
                    // Application pending for this plan
                    btnBuy.setText("Pending...");
                    btnBuy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D")));
                    btnBuy.setTextColor(Color.parseColor("#FF9500"));
                    btnBuy.setClickable(false);
                } else if ("Pending".equalsIgnoreCase(memberPlanStatus)) {
                    // Another plan is pending — allow changing the pending application
                    btnBuy.setText("Switch Apply");
                    btnBuy.setOnClickListener(v -> showPlanApplicationDialog(p, true));
                } else if ("Active".equalsIgnoreCase(memberPlanStatus)) {
                    // Member has an active plan — allow requesting change
                    btnBuy.setText("Change to This");
                    btnBuy.setOnClickListener(v -> showPlanApplicationDialog(p, false));
                } else {
                    // No plan or rejected — allow fresh application
                    btnBuy.setText("Apply");
                    btnBuy.setOnClickListener(v -> showPlanApplicationDialog(p, false));
                }

                layoutPackagesList.addView(pkgView);
            }
        }

        // Load Trainers list from Firestore-synced trainerList
        layoutTrainersList.removeAllViews();
        if (trainerList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No trainers available at the moment.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(0, 16, 0, 16);
            layoutTrainersList.addView(empty);
        } else {
            for (Trainer t : trainerList) {
                View trnView = inflater.inflate(R.layout.item_member_trainer, layoutTrainersList, false);
                TextView tvInitials = trnView.findViewById(R.id.tv_trn_initials);
                TextView tvName = trnView.findViewById(R.id.tv_trn_name);
                TextView tvSpec = trnView.findViewById(R.id.tv_trn_spec);
                TextView btnBook = trnView.findViewById(R.id.btn_trn_book);

                tvInitials.setText(t.getInitials());
                tvName.setText(t.name);
                tvSpec.setText(t.specialization);

                // Show 'Booked' state if this trainer is already booked
                if (t.name != null && t.name.equalsIgnoreCase(currentMember.bookedTrainer)) {
                    btnBook.setText("Booked ✓");
                    btnBook.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
                    btnBook.setTextColor(Color.parseColor("#34C759"));
                    btnBook.setClickable(false);
                } else {
                    btnBook.setOnClickListener(v -> showBookingDialog(t));
                }

                layoutTrainersList.addView(trnView);
            }
        }
    }

    /**
     * Shows a plan application dialog.
     * @param pkg The package the member wants to apply for.
     * @param isSwitch true if the member already has a pending application and wants to switch it.
     */
    private void showPlanApplicationDialog(GymPackage pkg, boolean isSwitch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);

        boolean hasActivePlan = "Active".equalsIgnoreCase(currentMember.planStatus)
                && currentMember.plan != null && !currentMember.plan.isEmpty()
                && !currentMember.plan.equals("None");

        String title = isSwitch ? "Switch Plan Application"
                : hasActivePlan ? "Request Plan Change" : "Apply for Membership Plan";
        builder.setTitle(title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 30, 40, 30);

        // Package info
        TextView info = new TextView(this);
        info.setText("Package: " + pkg.name + "\nPrice: $" + pkg.price + " / month\n\n" + pkg.description);
        info.setTextColor(Color.WHITE);
        info.setTextSize(15);
        info.setLineSpacing(1.2f, 1.2f);
        root.addView(info);

        // Admin approval notice
        View divider = new View(this);
        divider.setMinimumHeight(20);
        root.addView(divider);

        TextView notice = new TextView(this);
        notice.setText("⚠  Your application will be submitted for admin verification. Once approved, the plan will be activated on your account.");
        notice.setTextColor(Color.parseColor("#FF9500"));
        notice.setTextSize(13);
        notice.setLineSpacing(1.2f, 1.2f);
        root.addView(notice);

        if (hasActivePlan) {
            View divider2 = new View(this);
            divider2.setMinimumHeight(12);
            root.addView(divider2);

            TextView changeNote = new TextView(this);
            changeNote.setText("Current plan: " + currentMember.plan + " (remains active until admin approves the change).");
            changeNote.setTextColor(Color.parseColor("#94A3B8"));
            changeNote.setTextSize(12);
            root.addView(changeNote);
        }

        builder.setView(root);
        String confirmLabel = isSwitch ? "Switch Application" : hasActivePlan ? "Request Change" : "Submit Application";
        builder.setPositiveButton(confirmLabel, (dialog, which) -> submitPlanApplication(pkg));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Submits a plan application to Firestore with status = "Pending".
     * The current active plan (if any) is preserved until admin approves the change.
     */
    private void submitPlanApplication(GymPackage pkg) {
        ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
        progress.setMessage("Submitting plan application...");
        progress.setCancelable(false);
        progress.show();

        new Handler().postDelayed(() -> {
            progress.dismiss();

            // Update in-memory state
            currentMember.pendingPlan = pkg.name;
            currentMember.planStatus = "Pending";
            currentMember.notifications.add("Plan application submitted for: " + pkg.name + ". Awaiting admin approval.");
            updateNotificationBadge();
            refreshHomeTab();
            setupProgramsTab();

            // Persist to Firestore
            if (currentMember.id != null && !currentMember.id.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("pendingPlan", pkg.name);
                updates.put("planStatus", "Pending");
                db.collection("users").document(currentMember.id)
                        .update(updates)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Application submitted! Awaiting admin approval.", Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Saved locally. Sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Application submitted! Awaiting admin approval.", Toast.LENGTH_LONG).show();
            }
        }, 1200);
    }

    private void showBookingDialog(Trainer trainer) {
        Calendar calendar = Calendar.getInstance();

        // 1. Date Picker
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;

            // 2. Time Picker
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

                ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
                progress.setMessage("Checking trainer availability...");
                progress.setCancelable(false);
                progress.show();

                new Handler().postDelayed(() -> {
                    progress.dismiss();
                    currentMember.bookedTrainer = trainer.name;
                    currentMember.bookedTime = selectedDate + " at " + selectedTime;
                    currentMember.bookingStatus = "Pending";
                    currentMember.notifications.add("Booked personal training slot with " + trainer.name);
                    updateNotificationBadge();
                    refreshHomeTab();
                    setupProgramsTab(); // Refresh to show "Booked ✓"

                    // Persist booking to Firestore
                    if (currentMember.id != null && !currentMember.id.isEmpty()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("bookedTrainer", trainer.name);
                        updates.put("bookedTime", selectedDate + " at " + selectedTime);
                        updates.put("bookingStatus", "Pending");

                        db.collection("users").document(currentMember.id)
                                .update(updates)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(this, "Trainer booked for " + currentMember.bookedTime, Toast.LENGTH_LONG).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Booking saved locally. Sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Trainer booked for " + currentMember.bookedTime, Toast.LENGTH_LONG).show();
                    }

                }, 1500);

            }, 10, 0, true);
            timePickerDialog.show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.setTitle("Select Booking Date");
        datePickerDialog.show();
    }

    // Helper conversion
    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    // ==================== TAB 3: FITNESS TRACKING ====================
    private void refreshFitnessTab() {
        if (currentMember == null) return;
        tvWaterCount.setText(currentMember.waterIntake + " / 8 glasses");

        // Format and render weight log history
        StringBuilder sb = new StringBuilder();
        if (currentMember.weightHistory == null || currentMember.weightHistory.isEmpty()) {
            sb.append("• No weights logged yet. Input values above to log progress.");
        } else {
            for (int i = currentMember.weightHistory.size() - 1; i >= 0; i--) {
                sb.append("• Log #").append(i + 1).append(": ").append(currentMember.weightHistory.get(i)).append(" kg\n");
            }
        }
        tvWeightHistoryList.setText(sb.toString().trim());

        // Prepopulate current fields if saved
        if (currentMember.height > 0) {
            etBmiHeight.setText(String.valueOf(currentMember.height));
        }
        if (currentMember.weight > 0) {
            etBmiWeight.setText(String.valueOf(currentMember.weight));
        }
    }

    private void setupFitnessTab() {
        btnCalculateBmi.setOnClickListener(v -> {
            String strHeight = etBmiHeight.getText().toString().trim();
            String strWeight = etBmiWeight.getText().toString().trim();

            if (TextUtils.isEmpty(strHeight)) {
                etBmiHeight.setError("Height is required");
                etBmiHeight.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(strWeight)) {
                etBmiWeight.setError("Weight is required");
                etBmiWeight.requestFocus();
                return;
            }

            try {
                double h = Double.parseDouble(strHeight);
                double w = Double.parseDouble(strWeight);

                if (h <= 0 || w <= 0) {
                    Toast.makeText(this, "Height and weight must be positive numbers.", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentMember.height = h;
                currentMember.weight = w;

                double hMeters = h / 100.0;
                double bmi = w / (hMeters * hMeters);

                // Add to log
                if (currentMember.weightHistory == null) currentMember.weightHistory = new ArrayList<>();
                currentMember.weightHistory.add(String.valueOf(w));

                // Display
                layoutBmiResult.setVisibility(View.VISIBLE);
                tvBmiScore.setText(String.format(Locale.getDefault(), "%.1f", bmi));

                String cat;
                int color;
                if (bmi < 18.5) {
                    cat = "Underweight";
                    color = Color.parseColor("#FF9500");
                } else if (bmi < 24.9) {
                    cat = "Normal Weight";
                    color = Color.parseColor("#34C759");
                } else if (bmi < 29.9) {
                    cat = "Overweight";
                    color = Color.parseColor("#FF9500");
                } else {
                    cat = "Obese";
                    color = Color.parseColor("#FF3B30");
                }
                tvBmiCategory.setText(cat);
                tvBmiCategory.setTextColor(color);

                currentMember.notifications.add("Calculated BMI: " + String.format(Locale.getDefault(), "%.1f", bmi) + " (" + cat + ")");
                updateNotificationBadge();
                refreshFitnessTab();

                // Persist height, weight, and weight history to Firestore
                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("height", h);
                    updates.put("weight", w);
                    updates.put("weightHistory", currentMember.weightHistory);
                    db.collection("users").document(currentMember.id)
                            .update(updates)
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "BMI saved locally only.", Toast.LENGTH_SHORT).show());
                }

                Toast.makeText(this, "BMI calculated and Weight logged!", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show();
            }
        });

        // Hydration tracker
        btnWaterMinus.setOnClickListener(v -> {
            if (currentMember != null && currentMember.waterIntake > 0) {
                currentMember.waterIntake--;
                refreshFitnessTab();
            }
        });

        btnWaterPlus.setOnClickListener(v -> {
            if (currentMember == null) return;
            currentMember.waterIntake++;
            refreshFitnessTab();
            if (currentMember.waterIntake == 8) {
                Toast.makeText(this, "💧 Awesome work! Hydration target met today! 💧", Toast.LENGTH_LONG).show();
                currentMember.notifications.add("Great job meeting your daily hydration target (8 glasses)!");
                updateNotificationBadge();
            }
        });
    }

    // ==================== TAB 4: FEEDBACK & RATING ====================
    private void setupFeedbackTab() {
        // Populate Spinner with real trainers from Firestore
        List<String> list = new ArrayList<>();
        list.add("General Gym Experience");
        for (Trainer t : trainerList) {
            list.add(t.name + " (" + t.specialization + ")");
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeedbackTrainer.setAdapter(dataAdapter);

        // Click stars logic
        for (int i = 0; i < 5; i++) {
            final int starIndex = i;
            starViews[i].setOnClickListener(v -> setStarRating(starIndex + 1));
        }

        // Submit button action
        btnSubmitFeedback.setOnClickListener(v -> {
            String feedbackMsg = etFeedbackMsg.getText().toString().trim();
            String item = spinnerFeedbackTrainer.getSelectedItem().toString();

            if (selectedRating == 0) {
                Toast.makeText(this, "Please tap the stars to rate.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(feedbackMsg)) {
                etFeedbackMsg.setError("Comments are required");
                etFeedbackMsg.requestFocus();
                return;
            }

            ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
            progress.setMessage("Submitting feedback securely...");
            progress.setCancelable(false);
            progress.show();

            new Handler().postDelayed(() -> {
                progress.dismiss();
                if (currentMember != null) {
                    currentMember.notifications.add("Submitted " + selectedRating + "-star rating feedback for " + item);
                    updateNotificationBadge();
                }

                // Link feedback to trainer in Firestore
                if (!item.equals("General Gym Experience")) {
                    for (Trainer t : trainerList) {
                        if (item.startsWith(t.name)) {
                            String reviewEntry = selectedRating + " ★ - \"" + feedbackMsg + "\" (by " + (currentMember != null ? currentMember.name : "Member") + ")";
                            t.addFeedback(reviewEntry);

                            // Persist feedback to Firestore trainer document
                            if (t.id != null && !t.id.isEmpty()) {
                                db.collection("users").document(t.id)
                                        .update("feedback", t.feedback)
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Feedback saved locally only.", Toast.LENGTH_SHORT).show());
                            }
                            break;
                        }
                    }
                }

                Toast.makeText(this, "Feedback submitted successfully. Thank you!", Toast.LENGTH_LONG).show();

                // Clear fields
                setStarRating(0);
                etFeedbackMsg.setText("");
                spinnerFeedbackTrainer.setSelection(0);
            }, 1200);
        });
    }

    private void setStarRating(int rating) {
        selectedRating = rating;
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                starViews[i].setColorFilter(Color.parseColor("#FF9500")); // active rating orange
            } else {
                starViews[i].setColorFilter(Color.parseColor("#4A5568")); // inactive tint
            }
        }
    }

    private void openChatDialog() {
        if (currentMember == null || currentMember.bookedTrainer == null || currentMember.bookedTrainer.equals("None")) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_verify_otp, null);

        TextView title = dialogView.findViewById(R.id.title_titan_gym);
        title.setText("CHAT PORTAL");

        TextView instructions = dialogView.findViewById(R.id.tv_otp_instructions);
        instructions.setText("Secure channel with " + currentMember.bookedTrainer);

        EditText etMsg = dialogView.findViewById(R.id.et_otp);
        etMsg.setHint("Type your message...");
        etMsg.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        TextView tvCancel = dialogView.findViewById(R.id.tv_cancel);
        tvCancel.setText("Close Chat");

        LinearLayout btnSend = (LinearLayout) dialogView.findViewById(R.id.btn_verify);
        TextView btnSendText = (TextView) btnSend.getChildAt(0);
        btnSendText.setText("Send Message");

        AlertDialog dialog = builder.setView(dialogView).create();

        btnSend.setOnClickListener(v -> {
            String msg = etMsg.getText().toString().trim();
            if (msg.isEmpty()) return;
            Toast.makeText(this, "Message sent to " + currentMember.bookedTrainer, Toast.LENGTH_SHORT).show();
            etMsg.setText("");

            // Auto response after 1.5 seconds
            new Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    Toast.makeText(this, currentMember.bookedTrainer + ": Got your message! Let's discuss it in our next session.", Toast.LENGTH_LONG).show();
                }
            }, 1500);
        });

        tvCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
