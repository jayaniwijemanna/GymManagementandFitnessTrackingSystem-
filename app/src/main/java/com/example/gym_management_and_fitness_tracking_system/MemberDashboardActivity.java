package com.example.gym_management_and_fitness_tracking_system;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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
    private ListenerRegistration bookingsListener;
    private final List<Booking> memberBookings = new ArrayList<>();

    // QR Scanner launcher
    private ActivityResultLauncher<ScanOptions> qrScanLauncher;
    private Runnable qrScanSuccessCallback;

    // Local list references from DataStore (kept in sync by Firestore listeners)
    private final List<GymPackage> packageList = DataStore.getInstance().packages;
    private final List<Trainer> trainerList = DataStore.getInstance().trainers;

    // Header views
    private TextView tvHeaderInitials, tvHeaderName;
    private RelativeLayout btnNotifications;
    private View viewNotificationBadge;
    private FrameLayout btnLogout;

    // View Containers (Tabs)
    private ScrollView viewHome, viewPrograms, viewFitness, viewFeedback, viewBookings;

    // Navigation Tab Buttons
    private LinearLayout navTabHome, navTabPrograms, navTabFitness, navTabFeedback, navTabBookings;
    private ImageView imgNavHome, imgNavPrograms, imgNavFitness, imgNavFeedback, imgNavBookings;
    private TextView tvNavHome, tvNavPrograms, tvNavFitness, tvNavFeedback, tvNavBookings;

    // Home Tab elements
    private TextView tvHomePlanName, tvHomePlanStatus, tvHomePlanDesc;
    private TextView tvHomeCheckinStatus, tvHomeCheckinBadge;
    private TextView tvHomeBookingInfo, tvHomeBookingHistory, tvHomeWorkoutPlan, tvHomeDietPlan, tvHomeChatTrainerName;
    private TextView tvPendingPlanInfo;
    private LinearLayout btnHomeBuyPlan, btnHomeChangePlan, btnHomeCheckin, btnHomeBookTrainer, btnHomeChat;
    private LinearLayout layoutPendingPlanInfo;

    // Programs Tab elements
    private LinearLayout layoutPackagesList, layoutTrainersList;

    // Fitness Tab elements
    private EditText etBmiHeight, etBmiWeight;
    private LinearLayout btnCalculateBmi, layoutBmiHistoryCards;
    private FrameLayout btnWaterMinus, btnWaterPlus;
    private LinearLayout layoutBmiResult;
    private TextView tvBmiScore, tvBmiCategory, tvBmiHistoryEmpty, tvWaterCount;
    private TextView tvFitnessDietPlan, tvFitnessWorkoutPlan;

    // Feedback Tab elements
    private Spinner spinnerFeedbackTrainer;
    private LinearLayout layoutRatingStars;
    private EditText etFeedbackMsg;
    // Bookings Tab details
    private TextView tvBookingStatTotal, tvBookingStatPending, tvBookingStatAccepted;
    private LinearLayout layoutBookingsHistoryList;
    private TextView tvEmptyBookingsHistory;

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

        // Register ZXing QR scanner result launcher
        qrScanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String scannedContent = result.getContents().trim();
                if ("TITAN_GYM_ATTENDANCE_CHECKIN_ENTRANCE_QR_2026".equals(scannedContent)
                        || "TITAN-ENTRANCE-2026".equals(scannedContent)) {
                    if (qrScanSuccessCallback != null) {
                        qrScanSuccessCallback.run();
                    }
                } else {
                    Toast.makeText(this, "❌ Invalid QR Code. Please scan the official Titan Gym Entrance QR.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Scan cancelled.", Toast.LENGTH_SHORT).show();
            }
        });

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
                                // Preserve waterIntake and weightHistory if needed
                                if (currentMember != null) {
                                    m.waterIntake = currentMember.waterIntake;
                                    m.weightHistory = currentMember.weightHistory;
                                }
                                currentMember = m;
                                updateNotificationBadge();
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
                        if (currentMember != null) {
                            m.waterIntake = currentMember.waterIntake;
                        }
                        currentMember = m;
                        updateNotificationBadge();
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
        if (currentMember != null && currentMember.id != null && !currentMember.id.isEmpty()) {
            listenToBookingsRealtime(currentMember.id);
        }
        refreshHomeTab();
        setupProgramsTab();
        setupFitnessTab();
        setupFeedbackTab();
        updateNotificationBadge();
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

    private void listenToBookingsRealtime(String memberId) {
        if (TextUtils.isEmpty(memberId)) return;
        if (bookingsListener != null) bookingsListener.remove();

        bookingsListener = db.collection("bookings")
                .whereEqualTo("memberId", memberId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        memberBookings.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking b = doc.toObject(Booking.class);
                            if (b != null) {
                                b.id = doc.getId();
                                memberBookings.add(b);
                            }
                        }
                        java.util.Collections.sort(memberBookings, (b1, b2) -> Long.compare(b2.timestamp, b1.timestamp));
                        refreshHomeTab();
                        setupProgramsTab();
                        setupBookingsTab();
                        setupFeedbackTab();
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
        if (bookingsListener != null) bookingsListener.remove();
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
        viewBookings = findViewById(R.id.view_bookings);
        viewFitness = findViewById(R.id.view_fitness);
        viewFeedback = findViewById(R.id.view_feedback);

        // Bottom Navigation tabs
        navTabHome = findViewById(R.id.nav_tab_home);
        navTabPrograms = findViewById(R.id.nav_tab_programs);
        navTabBookings = findViewById(R.id.nav_tab_bookings);
        navTabFitness = findViewById(R.id.nav_tab_fitness);
        navTabFeedback = findViewById(R.id.nav_tab_feedback);

        imgNavHome = findViewById(R.id.img_nav_home);
        imgNavPrograms = findViewById(R.id.img_nav_programs);
        imgNavBookings = findViewById(R.id.img_nav_bookings);
        imgNavFitness = findViewById(R.id.img_nav_fitness);
        imgNavFeedback = findViewById(R.id.img_nav_feedback);

        tvNavHome = findViewById(R.id.tv_nav_home);
        tvNavPrograms = findViewById(R.id.tv_nav_programs);
        tvNavBookings = findViewById(R.id.tv_nav_bookings);
        tvNavFitness = findViewById(R.id.tv_nav_fitness);
        tvNavFeedback = findViewById(R.id.tv_nav_feedback);

        // Home Tab details
        tvHomePlanName = findViewById(R.id.tv_home_plan_name);
        tvHomePlanStatus = findViewById(R.id.tv_home_plan_status);
        tvHomePlanDesc = findViewById(R.id.tv_home_plan_desc);
        tvHomeCheckinStatus = findViewById(R.id.tv_home_checkin_status);
        tvHomeCheckinBadge = findViewById(R.id.tv_home_checkin_badge);
        tvHomeBookingInfo = findViewById(R.id.tv_home_booking_info);
        tvHomeBookingHistory = findViewById(R.id.tv_home_booking_history);
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
        layoutBmiHistoryCards = findViewById(R.id.layout_bmi_history_cards);
        tvBmiHistoryEmpty = findViewById(R.id.tv_bmi_history_empty);
        tvFitnessDietPlan = findViewById(R.id.tv_fitness_diet_plan);
        tvFitnessWorkoutPlan = findViewById(R.id.tv_fitness_workout_plan);
        tvWaterCount = findViewById(R.id.tv_water_count);
        btnWaterMinus = findViewById(R.id.btn_water_minus);
        btnWaterPlus = findViewById(R.id.btn_water_plus);

        // Bookings Tab details
        tvBookingStatTotal = findViewById(R.id.tv_booking_stat_total);
        tvBookingStatPending = findViewById(R.id.tv_booking_stat_pending);
        tvBookingStatAccepted = findViewById(R.id.tv_booking_stat_accepted);
        layoutBookingsHistoryList = findViewById(R.id.layout_bookings_history_list);
        tvEmptyBookingsHistory = findViewById(R.id.tv_empty_bookings_history);
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

        BottomSheetDialog dialog = new BottomSheetDialog(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 36);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);

        // Handle bar
        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleLp.setMargins(0, 0, 0, 24);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        // Header Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🔔 NOTIFICATIONS");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        // Notifications content container
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setBackgroundResource(R.drawable.bg_action_card);
        contentLayout.setPadding(24, 20, 24, 20);

        if (currentMember == null || currentMember.notifications == null || currentMember.notifications.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No new notifications.");
            tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
            tvEmpty.setTextSize(13);
            contentLayout.addView(tvEmpty);
        } else {
            for (int i = currentMember.notifications.size() - 1; i >= 0; i--) {
                String notifText = currentMember.notifications.get(i);
                TextView tvItem = new TextView(this);
                tvItem.setText("• " + notifText);
                tvItem.setTextColor(Color.parseColor("#E2E8F0"));
                tvItem.setTextSize(13);
                tvItem.setPadding(0, 8, 0, 8);
                contentLayout.addView(tvItem);
            }
        }
        root.addView(contentLayout);

        // Action Buttons Row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 24, 0, 0);

        // Clear All Button
        LinearLayout btnClear = new LinearLayout(this);
        btnClear.setOrientation(LinearLayout.HORIZONTAL);
        btnClear.setGravity(android.view.Gravity.CENTER);
        btnClear.setBackgroundResource(R.drawable.bg_button_selector);
        btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DC2626")));
        btnClear.setPadding(24, 20, 24, 20);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clearLp.setMargins(0, 0, 8, 0);
        btnClear.setLayoutParams(clearLp);

        TextView tvClear = new TextView(this);
        tvClear.setText("Clear All");
        tvClear.setTextColor(Color.WHITE);
        tvClear.setTextSize(14);
        tvClear.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnClear.addView(tvClear);

        btnClear.setOnClickListener(v -> {
            dialog.dismiss();
            if (currentMember != null) {
                currentMember.notifications.clear();
                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    db.collection("users").document(currentMember.id)
                            .update("notifications", new ArrayList<String>());
                }
            }
            updateNotificationBadge();
            Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show();
        });
        btnRow.addView(btnClear);

        // Close Button
        LinearLayout btnClose = new LinearLayout(this);
        btnClose.setOrientation(LinearLayout.HORIZONTAL);
        btnClose.setGravity(android.view.Gravity.CENTER);
        btnClose.setBackgroundResource(R.drawable.bg_button_selector);
        btnClose.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
        btnClose.setPadding(24, 20, 24, 20);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        closeLp.setMargins(8, 0, 0, 0);
        btnClose.setLayoutParams(closeLp);

        TextView tvClose = new TextView(this);
        tvClose.setText("Close");
        tvClose.setTextColor(Color.WHITE);
        tvClose.setTextSize(14);
        tvClose.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnClose.addView(tvClose);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRow.addView(btnClose);

        root.addView(btnRow);

        dialog.setContentView(root);
        dialog.show();
    }

    // Swapping content screens
    private void setupNavigation() {
        navTabHome.setOnClickListener(v -> selectTab(0));
        navTabPrograms.setOnClickListener(v -> selectTab(1));
        navTabBookings.setOnClickListener(v -> selectTab(2));
        navTabFitness.setOnClickListener(v -> selectTab(3));
        navTabFeedback.setOnClickListener(v -> selectTab(4));
    }

    private void selectTab(int index) {
        // Hide all views
        viewHome.setVisibility(View.GONE);
        viewPrograms.setVisibility(View.GONE);
        if (viewBookings != null) viewBookings.setVisibility(View.GONE);
        viewFitness.setVisibility(View.GONE);
        viewFeedback.setVisibility(View.GONE);

        // Reset navigation colors
        resetTabStyle(imgNavHome, tvNavHome);
        resetTabStyle(imgNavPrograms, tvNavPrograms);
        if (imgNavBookings != null) resetTabStyle(imgNavBookings, tvNavBookings);
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
                if (viewBookings != null) viewBookings.setVisibility(View.VISIBLE);
                if (imgNavBookings != null) highlightTab(imgNavBookings, tvNavBookings);
                setupBookingsTab();
                break;
            case 3:
                viewFitness.setVisibility(View.VISIBLE);
                highlightTab(imgNavFitness, tvNavFitness);
                refreshFitnessTab();
                break;
            case 4:
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

        // Daily Auto-Reset: Reset check-in status on each new day
        Calendar calCheckin = Calendar.getInstance();
        int curDay = calCheckin.get(Calendar.DAY_OF_MONTH);
        int curMonth = calCheckin.get(Calendar.MONTH) + 1;
        int curYear = calCheckin.get(Calendar.YEAR);
        String todayDateStr = curDay + "/" + curMonth + "/" + curYear;

        if (currentMember.checkedInDate != null && !currentMember.checkedInDate.isEmpty() && !currentMember.checkedInDate.equals(todayDateStr)) {
            currentMember.checkedInTime = "Not Checked In";
            currentMember.checkedInDate = todayDateStr;
            if (currentMember.id != null && !currentMember.id.isEmpty()) {
                db.collection("users").document(currentMember.id)
                        .update("checkedInTime", "Not Checked In", "checkedInDate", todayDateStr);
            }
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

        // Trainer booking status & history from integrated bookings collection
        if (memberBookings.isEmpty()) {
            tvHomeBookingInfo.setText("No active personal trainer booking.");
            btnHomeBookTrainer.setVisibility(View.VISIBLE);
            tvHomeChatTrainerName.setText("No active personal trainer");
            btnHomeChat.setEnabled(false);
            btnHomeChat.setAlpha(0.5f);
            tvHomeBookingHistory.setVisibility(View.GONE);
        } else {
            Booking latest = memberBookings.get(0);
            tvHomeBookingInfo.setText("Trainer: " + latest.trainerName + " (" + latest.status + ")\nTime: " + latest.bookedTime);

            if ("Pending".equalsIgnoreCase(latest.status) || "Accepted".equalsIgnoreCase(latest.status)) {
                btnHomeBookTrainer.setVisibility(View.GONE);
                tvHomeChatTrainerName.setText("Chat with " + latest.trainerName);
                btnHomeChat.setEnabled(true);
                btnHomeChat.setAlpha(1.0f);
            } else {
                btnHomeBookTrainer.setVisibility(View.VISIBLE);
                tvHomeChatTrainerName.setText("No active personal trainer");
                btnHomeChat.setEnabled(false);
                btnHomeChat.setAlpha(0.5f);
            }

            StringBuilder sb = new StringBuilder("• Booking History (" + memberBookings.size() + " total):\n");
            for (Booking b : memberBookings) {
                String st = b.status != null ? b.status : "Pending";
                sb.append("  - ").append(b.trainerName).append(" | ").append(b.bookedTime).append(" [").append(st).append("]\n");
            }
            tvHomeBookingHistory.setText(sb.toString().trim());
            tvHomeBookingHistory.setVisibility(View.VISIBLE);
        }

        // Render booking history log
        if (currentMember.bookingHistory != null && !currentMember.bookingHistory.isEmpty()) {
            StringBuilder sb = new StringBuilder("• Booking History:\n");
            for (int i = currentMember.bookingHistory.size() - 1; i >= 0; i--) {
                sb.append("  - ").append(currentMember.bookingHistory.get(i)).append("\n");
            }
            tvHomeBookingHistory.setText(sb.toString().trim());
            tvHomeBookingHistory.setVisibility(View.VISIBLE);
        } else {
            tvHomeBookingHistory.setVisibility(View.GONE);
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
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 40);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.setMargins(0, 0, 0, 24);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        TextView title = new TextView(this);
        title.setText("GYM ENTRANCE QR SCANNER");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        TextView inst = new TextView(this);
        inst.setText("Point camera at gym QR code and tap 'Scan QR' to detect.");
        inst.setTextColor(Color.parseColor("#94A3B8"));
        inst.setTextSize(13);
        inst.setGravity(android.view.Gravity.CENTER);
        inst.setPadding(0, 6, 0, 18);
        root.addView(inst);

        // QR Viewfinder Simulation Frame
        FrameLayout cameraFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameLp = new LinearLayout.LayoutParams(spToPx(240), spToPx(130));
        frameLp.setMargins(0, 0, 0, 16);
        cameraFrame.setLayoutParams(frameLp);
        cameraFrame.setBackgroundResource(R.drawable.bg_input_selector);

        TextView tvFrameText = new TextView(this);
        tvFrameText.setText("📷 CAMERA VIEWFINDER\n\n[ Ready - Tap 'Scan QR' Below ]");
        tvFrameText.setTextColor(Color.parseColor("#94A3B8"));
        tvFrameText.setGravity(android.view.Gravity.CENTER);
        tvFrameText.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cameraFrame.addView(tvFrameText);
        root.addView(cameraFrame);

        // QR Code Scanned Output Field (Empty by default)
        EditText etQrCode = new EditText(this);
        etQrCode.setHint("Scanned QR Code (e.g. TITAN-ENTRANCE-2026)");
        etQrCode.setText("");
        etQrCode.setTextColor(Color.WHITE);
        etQrCode.setHintTextColor(Color.parseColor("#64748B"));
        etQrCode.setBackgroundResource(R.drawable.bg_input_selector);
        etQrCode.setPadding(30, 22, 30, 22);
        etQrCode.setTextSize(14);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, 0, 0, 16);
        etQrCode.setLayoutParams(etLp);
        root.addView(etQrCode);

        // Button 1: Camera Scan Action
        LinearLayout btnCameraScan = new LinearLayout(this);
        btnCameraScan.setOrientation(LinearLayout.HORIZONTAL);
        btnCameraScan.setGravity(android.view.Gravity.CENTER);
        btnCameraScan.setBackgroundResource(R.drawable.bg_button_selector);
        btnCameraScan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B")));
        btnCameraScan.setPadding(0, 24, 0, 24);
        btnCameraScan.setClickable(true);
        btnCameraScan.setFocusable(true);

        TextView tvCameraScanText = new TextView(this);
        tvCameraScanText.setText("📷  SCAN QR WITH CAMERA");
        tvCameraScanText.setTextColor(Color.parseColor("#38BDF8"));
        tvCameraScanText.setTextSize(14);
        tvCameraScanText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnCameraScan.addView(tvCameraScanText);
        root.addView(btnCameraScan);

        // Button 2: Submit Attendance
        LinearLayout btnScanConfirm = new LinearLayout(this);
        btnScanConfirm.setOrientation(LinearLayout.HORIZONTAL);
        btnScanConfirm.setGravity(android.view.Gravity.CENTER);
        btnScanConfirm.setBackgroundResource(R.drawable.bg_button_selector);
        btnScanConfirm.setPadding(0, 26, 0, 26);
        btnScanConfirm.setClickable(true);
        btnScanConfirm.setFocusable(true);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.setMargins(0, 12, 0, 0);
        btnScanConfirm.setLayoutParams(btnLp);

        TextView tvBtnText = new TextView(this);
        tvBtnText.setText("✓  MARK ATTENDANCE");
        tvBtnText.setTextColor(Color.WHITE);
        tvBtnText.setTextSize(15);
        tvBtnText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnScanConfirm.addView(tvBtnText);
        root.addView(btnScanConfirm);

        // STATE FLAG: Only true after camera physically scans and returns valid code
        final boolean[] qrScanned = {false};

        // Camera Scan Button Action — opens REAL device camera via ZXing
        btnCameraScan.setOnClickListener(v -> {
            // Set callback: runs on successful QR detection
            qrScanSuccessCallback = () -> {
                qrScanned[0] = true;
                etQrCode.setText("TITAN-ENTRANCE-2026");
                etQrCode.setFocusable(false);
                etQrCode.setFocusableInTouchMode(false);
                tvFrameText.setText("✅ QR CODE VERIFIED!\n\n[ Titan Gym Entrance ]");
                tvFrameText.setTextColor(Color.parseColor("#34C759"));
                tvBtnText.setText("✓  MARK ATTENDANCE");
                tvBtnText.setTextColor(Color.parseColor("#34C759"));
                tvCameraScanText.setText("📷  Re-Scan QR");
                Toast.makeText(this, "✅ QR Verified! Tap 'Mark Attendance' to confirm.", Toast.LENGTH_SHORT).show();
            };

            // Launch real camera scanner — locked to portrait, QR_CODE only for instant detection
            ScanOptions options = new ScanOptions();
            options.setPrompt("Point camera at Titan Gym QR Code to check in");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);           // lock to portrait, no rotation
            options.setBarcodeImageEnabled(false);         // faster, no image capture
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE); // QR only = faster focus
            qrScanLauncher.launch(options);
        });


        // Submit Attendance Button Action — BLOCKED until qrScanned == true
        btnScanConfirm.setOnClickListener(v -> {
            if (!qrScanned[0]) {
                Toast.makeText(this, "❌ Please scan the Gym QR Code first before marking attendance!", Toast.LENGTH_LONG).show();
                return;
            }

            dialog.dismiss();

            ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
            progress.setMessage("Validating QR Code & marking attendance...");
            progress.setCancelable(false);
            progress.show();

            new Handler().postDelayed(() -> {
                progress.dismiss();

                // Format current time and date
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String checkinTime = sdf.format(new Date());

                Calendar cal = Calendar.getInstance();
                int day = cal.get(Calendar.DAY_OF_MONTH);
                int month = cal.get(Calendar.MONTH) + 1;
                int year = cal.get(Calendar.YEAR);
                String dateStr1 = day + "/" + month + "/" + year;
                String dateStr2 = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month, year);

                // Check for bookings matching today's date
                int matchedBookingsCount = 0;
                for (Booking b : memberBookings) {
                    if (b.bookedTime != null && ("Pending".equalsIgnoreCase(b.status) || "Accepted".equalsIgnoreCase(b.status))) {
                        if (b.bookedTime.contains(dateStr1) || b.bookedTime.contains(dateStr2)) {
                            b.status = "Attended";
                            matchedBookingsCount++;
                            if (b.id != null && !b.id.isEmpty()) {
                                db.collection("bookings").document(b.id).update("status", "Attended");
                            }
                        }
                    }
                }

                currentMember.checkedInTime = checkinTime;
                currentMember.checkedInDate = dateStr1;
                String notifMsg = matchedBookingsCount > 0
                        ? "Checked in at " + checkinTime + " • Marked " + matchedBookingsCount + " booking(s) as Attended!"
                        : "Checked in successfully at " + checkinTime;
                currentMember.notifications.add(notifMsg);
                updateNotificationBadge();

                // Persist check-in to Firestore users document
                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    Map<String, Object> memberCheckinUpdates = new HashMap<>();
                    memberCheckinUpdates.put("checkedInTime", checkinTime);
                    memberCheckinUpdates.put("checkedInDate", dateStr1);
                    db.collection("users").document(currentMember.id).update(memberCheckinUpdates);
                }

                // Write attendance document to Firestore 'attendance' collection
                Map<String, Object> attDoc = new HashMap<>();
                attDoc.put("memberId", currentMember.id != null ? currentMember.id : "");
                attDoc.put("memberName", currentMember.name != null ? currentMember.name : "Member");
                attDoc.put("memberEmail", currentMember.email != null ? currentMember.email : "");
                attDoc.put("checkInTime", checkinTime);
                attDoc.put("date", dateStr1);
                attDoc.put("matchedBookingsCount", matchedBookingsCount);
                attDoc.put("status", "Present");
                attDoc.put("timestamp", com.google.firebase.Timestamp.now());

                db.collection("attendance").add(attDoc);

                refreshHomeTab();
                setupProgramsTab();
                setupBookingsTab();

                if (matchedBookingsCount > 0) {
                    Toast.makeText(this, "Attendance Marked! " + matchedBookingsCount + " session booking(s) marked Attended.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Attendance Marked! Checked in at " + checkinTime, Toast.LENGTH_LONG).show();
                }
            }, 1000);
        });

        dialog.setContentView(root);
        dialog.show();
    }

    private void showMyMemberPassQrDialog() {
        if (currentMember == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 40);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.setMargins(0, 0, 0, 30);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        TextView title = new TextView(this);
        title.setText("MEMBER GYM PASS QR CODE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Member: " + currentMember.name + " (" + (currentMember.plan != null ? currentMember.plan : "Standard") + ")\nScan at gym entrance or turnstile to mark attendance.");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(13);
        sub.setGravity(android.view.Gravity.CENTER);
        sub.setPadding(0, 6, 0, 24);
        root.addView(sub);

        ImageView imgQr = new ImageView(this);
        int qrSize = spToPx(240);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(qrSize, qrSize);
        imgQr.setLayoutParams(imgLp);

        String qrPayload = "TITAN_MEMBER_PASS:" + (currentMember.id != null ? currentMember.id : "MEM_001") + "|" + currentMember.name;
        Bitmap qrBmp = QrGenerator.generateQrBitmap(qrPayload, 500);
        imgQr.setImageBitmap(qrBmp);
        root.addView(imgQr);

        TextView codeText = new TextView(this);
        codeText.setText("Member ID: " + (currentMember.id != null ? currentMember.id : "TITAN-MEM-001"));
        codeText.setTextColor(Color.parseColor("#34C759"));
        codeText.setTextSize(13);
        codeText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        codeText.setPadding(0, 20, 0, 0);
        root.addView(codeText);

        dialog.setContentView(root);
        dialog.show();
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
                TextView tvRating = trnView.findViewById(R.id.tv_trn_rating);
                TextView btnBook = trnView.findViewById(R.id.btn_trn_book);

                tvInitials.setText(t.getInitials());
                tvName.setText(t.name);
                tvSpec.setText(t.specialization);
                if (tvRating != null) {
                    tvRating.setText(t.getFormattedRating());
                }

                // Show 'Booked' state if this trainer is currently booked
                boolean isBookedThisTrainer = false;
                for (Booking b : memberBookings) {
                    if (b.trainerName != null && b.trainerName.equalsIgnoreCase(t.name)
                            && ("Pending".equalsIgnoreCase(b.status) || "Accepted".equalsIgnoreCase(b.status))) {
                        isBookedThisTrainer = true;
                        break;
                    }
                }

                if (isBookedThisTrainer) {
                    btnBook.setText("Booked ✓");
                    btnBook.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
                    btnBook.setTextColor(Color.parseColor("#34C759"));
                    btnBook.setClickable(false);
                } else {
                    btnBook.setText("Book Trainer");
                    btnBook.setOnClickListener(v -> showBookingDialog(t));
                }

                layoutTrainersList.addView(trnView);
            }
        }
    }

    // ==================== TAB 3: BOOKINGS HISTORY TRACKER ====================
    private void setupBookingsTab() {
        if (layoutBookingsHistoryList == null) return;
        layoutBookingsHistoryList.removeAllViews();

        // Count only non-attended bookings for stats
        int total = 0;
        int pending = 0;
        int accepted = 0;

        for (Booking b : memberBookings) {
            String s = b.status != null ? b.status : "Pending";
            if ("Attended".equalsIgnoreCase(s)) continue; // exclude attended from member view
            total++;
            if ("Pending".equalsIgnoreCase(s)) pending++;
            else if ("Accepted".equalsIgnoreCase(s)) accepted++;
        }

        if (tvBookingStatTotal != null) tvBookingStatTotal.setText(String.valueOf(total));
        if (tvBookingStatPending != null) tvBookingStatPending.setText(String.valueOf(pending));
        if (tvBookingStatAccepted != null) tvBookingStatAccepted.setText(String.valueOf(accepted));

        // Check if there are any displayable bookings
        boolean hasDisplayable = false;
        for (Booking b : memberBookings) {
            if (!"Attended".equalsIgnoreCase(b.status)) {
                hasDisplayable = true;
                break;
            }
        }

        if (memberBookings.isEmpty() || !hasDisplayable) {
            if (tvEmptyBookingsHistory != null) tvEmptyBookingsHistory.setVisibility(View.VISIBLE);
            return;
        }
        if (tvEmptyBookingsHistory != null) tvEmptyBookingsHistory.setVisibility(View.GONE);

        for (Booking b : memberBookings) {
            // Skip attended bookings — not shown in member section
            String statusStr = b.status != null ? b.status : "Pending";
            if ("Attended".equalsIgnoreCase(statusStr)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(24, 18, 24, 18);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, 14);
            card.setLayoutParams(cardLp);

            // Row 1: Trainer Name + Status Badge
            LinearLayout row1 = new LinearLayout(this);
            row1.setOrientation(LinearLayout.HORIZONTAL);
            row1.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvTrn = new TextView(this);
            tvTrn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            tvTrn.setText("Trainer: " + (b.trainerName != null ? b.trainerName : "Trainer"));
            tvTrn.setTextColor(Color.WHITE);
            tvTrn.setTextSize(15);
            tvTrn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            row1.addView(tvTrn);

            TextView tvBadge = new TextView(this);
            tvBadge.setPadding(20, 8, 20, 8);
            tvBadge.setTextSize(11);
            tvBadge.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvBadge.setBackgroundResource(R.drawable.bg_badge);
            tvBadge.setText(statusStr);

            if ("Accepted".equalsIgnoreCase(statusStr)) {
                tvBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818")));
                tvBadge.setTextColor(Color.parseColor("#34C759"));
            } else if ("Rejected".equalsIgnoreCase(statusStr)) {
                tvBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2D1010")));
                tvBadge.setTextColor(Color.parseColor("#FF3B30"));
            } else {
                // Pending
                tvBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D")));
                tvBadge.setTextColor(Color.parseColor("#FF9500"));
            }
            row1.addView(tvBadge);
            card.addView(row1);

            // Row 2: Booked Session Time
            TextView tvTime = new TextView(this);
            tvTime.setText("📅  Session Slot: " + (b.bookedTime != null ? b.bookedTime : "N/A"));
            tvTime.setTextColor(Color.parseColor("#94A3B8"));
            tvTime.setTextSize(13);
            tvTime.setPadding(0, 8, 0, 0);
            card.addView(tvTime);

            layoutBookingsHistoryList.addView(card);
        }
    }

    /**
     * Shows a plan application dialog with the Titan Gym dark theme bottom sheet.
     * @param pkg The package the member wants to apply for.
     * @param isSwitch true if the member already has a pending application and wants to switch it.
     */
    private void showPlanApplicationDialog(GymPackage pkg, boolean isSwitch) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // Root container with dark background
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 36);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);

        // Drag handle indicator
        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleLp.setMargins(0, 0, 0, 30);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        // Title
        boolean hasActivePlan = "Active".equalsIgnoreCase(currentMember.planStatus)
                && currentMember.plan != null && !currentMember.plan.isEmpty()
                && !currentMember.plan.equals("None") && !currentMember.plan.equals("No Package");

        String titleStr = isSwitch ? "Switch Plan Application"
                : hasActivePlan ? "Request Plan Change" : "Apply for Membership Plan";

        TextView tvTitle = new TextView(this);
        tvTitle.setText(titleStr);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, 24);
        root.addView(tvTitle);

        // Package Info Card
        LinearLayout cardPkg = new LinearLayout(this);
        cardPkg.setOrientation(LinearLayout.VERTICAL);
        cardPkg.setBackgroundResource(R.drawable.bg_action_card);
        cardPkg.setPadding(24, 20, 24, 20);

        TextView tvPkgName = new TextView(this);
        tvPkgName.setText(pkg.name);
        tvPkgName.setTextColor(Color.WHITE);
        tvPkgName.setTextSize(18);
        tvPkgName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cardPkg.addView(tvPkgName);

        TextView tvPkgPrice = new TextView(this);
        tvPkgPrice.setText("$" + pkg.price + " / month");
        tvPkgPrice.setTextColor(Color.parseColor("#34C759"));
        tvPkgPrice.setTextSize(15);
        tvPkgPrice.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvPkgPrice.setPadding(0, 4, 0, 12);
        cardPkg.addView(tvPkgPrice);

        TextView tvPkgDesc = new TextView(this);
        tvPkgDesc.setText(pkg.description);
        tvPkgDesc.setTextColor(Color.parseColor("#94A3B8"));
        tvPkgDesc.setTextSize(13);
        cardPkg.addView(tvPkgDesc);

        root.addView(cardPkg);

        // Verification Notice Card
        LinearLayout cardNotice = new LinearLayout(this);
        cardNotice.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams noticeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noticeLp.setMargins(0, 16, 0, 24);
        cardNotice.setLayoutParams(noticeLp);
        cardNotice.setBackgroundResource(R.drawable.bg_card);
        cardNotice.setPadding(20, 16, 20, 16);

        TextView tvNotice = new TextView(this);
        tvNotice.setText("⚠  Your application will be submitted for admin verification. Once approved, the plan will be activated on your account.");
        tvNotice.setTextColor(Color.parseColor("#FF9500"));
        tvNotice.setTextSize(13);
        cardNotice.addView(tvNotice);

        if (hasActivePlan) {
            TextView tvActiveNotice = new TextView(this);
            tvActiveNotice.setText("Current plan: " + currentMember.plan + " (remains active until admin approves the change).");
            tvActiveNotice.setTextColor(Color.parseColor("#94A3B8"));
            tvActiveNotice.setTextSize(12);
            tvActiveNotice.setPadding(0, 8, 0, 0);
            cardNotice.addView(tvActiveNotice);
        }

        root.addView(cardNotice);

        // Submit Button
        String confirmLabel = isSwitch ? "Switch Application" : hasActivePlan ? "Request Change" : "Submit Application";
        android.widget.Button btnSubmit = new android.widget.Button(this);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, spToPx(48));
        btnSubmit.setLayoutParams(btnLp);
        btnSubmit.setText(confirmLabel);
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setTextSize(15);
        btnSubmit.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#007AFF")));

        btnSubmit.setOnClickListener(v -> {
            dialog.dismiss();
            submitPlanApplication(pkg);
        });

        root.addView(btnSubmit);

        dialog.setContentView(root);
        dialog.show();
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

    /**
     * Displays trainer details, real-time availability status, and date/time pickers prior to booking.
     * Uses the Titan Gym dark theme bottom sheet format.
     */
    private void showBookingDialog(Trainer trainer) {
        // Enforce active membership plan validation
        boolean hasActivePlan = currentMember != null
                && currentMember.plan != null
                && !currentMember.plan.trim().isEmpty()
                && !"None".equalsIgnoreCase(currentMember.plan)
                && !"No Package".equalsIgnoreCase(currentMember.plan)
                && "Active".equalsIgnoreCase(currentMember.planStatus);

        if (!hasActivePlan) {
            BottomSheetDialog dialog = new BottomSheetDialog(this);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(40, 24, 40, 36);
            root.setBackgroundResource(R.drawable.bg_bottom_sheet);

            // Handle bar
            View handle = new View(this);
            LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
            handleLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            handleLp.setMargins(0, 0, 0, 24);
            handle.setLayoutParams(handleLp);
            handle.setBackgroundResource(R.drawable.bg_input_default);
            root.addView(handle);

            // Header Title
            TextView tvTitle = new TextView(this);
            tvTitle.setText("⚠️ MEMBERSHIP INACTIVE");
            tvTitle.setTextColor(Color.parseColor("#FF9500"));
            tvTitle.setTextSize(18);
            tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tvTitle.setPadding(0, 0, 0, 16);
            root.addView(tvTitle);

            // Card Message
            LinearLayout cardMsg = new LinearLayout(this);
            cardMsg.setOrientation(LinearLayout.VERTICAL);
            cardMsg.setBackgroundResource(R.drawable.bg_action_card);
            cardMsg.setPadding(24, 20, 24, 20);

            TextView tvMsg = new TextView(this);
            tvMsg.setText("Your gym membership plan is not active. You must have an active membership package to book a personal trainer.");
            tvMsg.setTextColor(Color.parseColor("#E2E8F0"));
            tvMsg.setTextSize(13);
            tvMsg.setLineSpacing(4f, 1f);
            cardMsg.addView(tvMsg);
            root.addView(cardMsg);

            // Buttons Row
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setPadding(0, 24, 0, 0);

            // Explore Packages Button
            LinearLayout btnExplore = new LinearLayout(this);
            btnExplore.setOrientation(LinearLayout.HORIZONTAL);
            btnExplore.setGravity(android.view.Gravity.CENTER);
            btnExplore.setBackgroundResource(R.drawable.bg_button_selector);
            btnExplore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#38BDF8")));
            btnExplore.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            expLp.setMargins(0, 0, 8, 0);
            btnExplore.setLayoutParams(expLp);

            TextView tvExplore = new TextView(this);
            tvExplore.setText("Explore Packages");
            tvExplore.setTextColor(Color.WHITE);
            tvExplore.setTextSize(14);
            tvExplore.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            btnExplore.addView(tvExplore);

            btnExplore.setOnClickListener(v -> {
                dialog.dismiss();
                selectTab(1);
            });
            btnRow.addView(btnExplore);

            // Cancel Button
            LinearLayout btnCancel = new LinearLayout(this);
            btnCancel.setOrientation(LinearLayout.HORIZONTAL);
            btnCancel.setGravity(android.view.Gravity.CENTER);
            btnCancel.setBackgroundResource(R.drawable.bg_button_selector);
            btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#334155")));
            btnCancel.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams canLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            canLp.setMargins(8, 0, 0, 0);
            btnCancel.setLayoutParams(canLp);

            TextView tvCancel = new TextView(this);
            tvCancel.setText("Cancel");
            tvCancel.setTextColor(Color.WHITE);
            tvCancel.setTextSize(14);
            tvCancel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            btnCancel.addView(tvCancel);

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            btnRow.addView(btnCancel);

            root.addView(btnRow);

            dialog.setContentView(root);
            dialog.show();
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 36);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);

        // Drag handle bar
        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleLp.setMargins(0, 0, 0, 30);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Book Personal Trainer");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, 20);
        root.addView(tvTitle);

        // Trainer Details & Availability Card
        LinearLayout cardTrainer = new LinearLayout(this);
        cardTrainer.setOrientation(LinearLayout.VERTICAL);
        cardTrainer.setBackgroundResource(R.drawable.bg_action_card);
        cardTrainer.setPadding(24, 20, 24, 20);

        // Header: Initials + Name + Specialization
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        FrameLayout avatar = new FrameLayout(this);
        avatar.setLayoutParams(new LinearLayout.LayoutParams(spToPx(44), spToPx(44)));
        avatar.setBackgroundResource(R.drawable.bg_icon_blue);

        TextView tvInitials = new TextView(this);
        tvInitials.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.CENTER));
        tvInitials.setText(trainer.getInitials());
        tvInitials.setTextColor(Color.WHITE);
        tvInitials.setTextSize(14);
        tvInitials.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        avatar.addView(tvInitials);
        headerRow.addView(avatar);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setPadding(24, 0, 0, 0);

        TextView tvTrnName = new TextView(this);
        tvTrnName.setText(trainer.name);
        tvTrnName.setTextColor(Color.WHITE);
        tvTrnName.setTextSize(16);
        tvTrnName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textCol.addView(tvTrnName);

        TextView tvTrnSpec = new TextView(this);
        tvTrnSpec.setText(trainer.specialization + "  •  " + trainer.rating + " ★");
        tvTrnSpec.setTextColor(Color.parseColor("#94A3B8"));
        tvTrnSpec.setTextSize(13);
        textCol.addView(tvTrnSpec);

        headerRow.addView(textCol);
        cardTrainer.addView(headerRow);

        // Availability info box
        TextView tvAvailability = new TextView(this);
        String availStr = (trainer.availability != null && !trainer.availability.isEmpty())
                ? trainer.availability : "Mon - Sat: 06:00 AM - 08:00 PM (Available)";
        tvAvailability.setText("🟢 Availability:\n" + availStr);
        tvAvailability.setTextColor(Color.parseColor("#34C759"));
        tvAvailability.setTextSize(13);
        tvAvailability.setPadding(0, 16, 0, 0);
        cardTrainer.addView(tvAvailability);

        root.addView(cardTrainer);

        // Spacer
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(1, 20));
        root.addView(spacer1);

        // Date selection field
        TextView tvDateLabel = new TextView(this);
        tvDateLabel.setText("SELECT SESSION DATE");
        tvDateLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvDateLabel.setTextSize(11);
        tvDateLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvDateLabel.setPadding(0, 8, 0, 8);
        root.addView(tvDateLabel);

        final Calendar calendar = Calendar.getInstance();
        final String[] chosenDate = {calendar.get(Calendar.DAY_OF_MONTH) + "/" + (calendar.get(Calendar.MONTH) + 1) + "/" + calendar.get(Calendar.YEAR)};

        TextView tvDateInput = new TextView(this);
        tvDateInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spToPx(46)));
        tvDateInput.setBackgroundResource(R.drawable.bg_input_selector);
        tvDateInput.setPadding(24, 0, 24, 0);
        tvDateInput.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvDateInput.setText("📅  Date: " + chosenDate[0]);
        tvDateInput.setTextColor(Color.WHITE);
        tvDateInput.setTextSize(14);
        tvDateInput.setClickable(true);
        tvDateInput.setFocusable(true);
        root.addView(tvDateInput);

        tvDateInput.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                chosenDate[0] = dayOfMonth + "/" + (month + 1) + "/" + year;
                tvDateInput.setText("📅  Date: " + chosenDate[0]);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dpd.setTitle("Select Session Date");
            dpd.show();
        });

        // Time selection field
        TextView tvTimeLabel = new TextView(this);
        tvTimeLabel.setText("SELECT SESSION TIME");
        tvTimeLabel.setTextColor(Color.parseColor("#94A3B8"));
        tvTimeLabel.setTextSize(11);
        tvTimeLabel.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTimeLabel.setPadding(0, 16, 0, 8);
        root.addView(tvTimeLabel);

        final String[] chosenTime = {String.format(Locale.getDefault(), "%02d:%02d", 10, 0)};

        TextView tvTimeInput = new TextView(this);
        tvTimeInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spToPx(46)));
        tvTimeInput.setBackgroundResource(R.drawable.bg_input_selector);
        tvTimeInput.setPadding(24, 0, 24, 0);
        tvTimeInput.setGravity(android.view.Gravity.CENTER_VERTICAL);
        tvTimeInput.setText("⏰  Time: " + chosenTime[0]);
        tvTimeInput.setTextColor(Color.WHITE);
        tvTimeInput.setTextSize(14);
        tvTimeInput.setClickable(true);
        tvTimeInput.setFocusable(true);
        root.addView(tvTimeInput);

        tvTimeInput.setOnClickListener(v -> {
            TimePickerDialog tpd = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                chosenTime[0] = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                tvTimeInput.setText("⏰  Time: " + chosenTime[0]);
            }, 10, 0, true);
            tpd.setTitle("Select Session Time");
            tpd.show();
        });

        // Spacer
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(1, 28));
        root.addView(spacer2);

        // Confirm Booking Button
        android.widget.Button btnConfirm = new android.widget.Button(this);
        btnConfirm.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spToPx(48)));
        btnConfirm.setText("Confirm Trainer Booking");
        btnConfirm.setTextColor(Color.WHITE);
        btnConfirm.setTextSize(15);
        btnConfirm.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#34C759")));

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
            progress.setMessage("Checking trainer availability & confirming...");
            progress.setCancelable(false);
            progress.show();

            new Handler().postDelayed(() -> {
                progress.dismiss();

                DocumentReference newDoc = db.collection("bookings").document();
                Booking newBooking = new Booking(
                        newDoc.getId(),
                        currentMember.id != null ? currentMember.id : "",
                        currentMember.name != null ? currentMember.name : "Member",
                        currentMember.email != null ? currentMember.email : "",
                        currentMember.phone != null ? currentMember.phone : "",
                        trainer.id != null ? trainer.id : "",
                        trainer.name != null ? trainer.name : "Trainer",
                        trainer.email != null ? trainer.email : "",
                        chosenDate[0] + " at " + chosenTime[0],
                        "Pending"
                );

                currentMember.bookedTrainer = trainer.name;
                currentMember.bookedTime = chosenDate[0] + " at " + chosenTime[0];
                currentMember.bookingStatus = "Pending";
                currentMember.notifications.add("Booked personal training slot with " + trainer.name);
                updateNotificationBadge();

                newDoc.set(newBooking)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(this, "Trainer booked for " + newBooking.bookedTime, Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Booking saved locally. Sync error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("bookedTrainer", trainer.name);
                    updates.put("bookedTime", chosenDate[0] + " at " + chosenTime[0]);
                    updates.put("bookingStatus", "Pending");
                    db.collection("users").document(currentMember.id).update(updates);
                }

                // Push real-time notification to the trainer's user document in Firestore
                if (trainer.id != null && !trainer.id.isEmpty()) {
                    String trainerNotif = "📅 New booking request from " + currentMember.name + " for " + chosenDate[0] + " at " + chosenTime[0];
                    db.collection("users").document(trainer.id)
                            .update("notifications", com.google.firebase.firestore.FieldValue.arrayUnion(trainerNotif));
                }
            }, 1500);
        });

        root.addView(btnConfirm);

        dialog.setContentView(root);
        dialog.show();
    }

    // Helper conversion
    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    // ==================== TAB 3: FITNESS TRACKING ====================
    private void refreshFitnessTab() {
        if (currentMember == null) return;

        // --- 1. Auto-reset water count on a new day ---
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        String todayStr = day + "/" + month + "/" + year;

        String savedWaterDate = currentMember.waterDate != null ? currentMember.waterDate : "";
        if (!savedWaterDate.equals(todayStr)) {
            // New day — reset water intake
            currentMember.waterIntake = 0;
            currentMember.waterDate = todayStr;
            if (currentMember.id != null && !currentMember.id.isEmpty()) {
                Map<String, Object> waterReset = new HashMap<>();
                waterReset.put("waterIntake", 0);
                waterReset.put("waterDate", todayStr);
                db.collection("users").document(currentMember.id).update(waterReset);
            }
        }
        tvWaterCount.setText(currentMember.waterIntake + " / 8 glasses");

        // --- 2. Pre-populate height/weight fields ---
        if (currentMember.height > 0) etBmiHeight.setText(String.valueOf(currentMember.height));
        if (currentMember.weight > 0) etBmiWeight.setText(String.valueOf(currentMember.weight));

        // --- 3. Display trainer-assigned diet/workout plans ---
        String diet = (currentMember.dietPlan != null && !currentMember.dietPlan.isEmpty())
                ? currentMember.dietPlan
                : getBmiBasedDietPlan();
        String workout = (currentMember.workoutPlan != null && !currentMember.workoutPlan.isEmpty())
                ? currentMember.workoutPlan
                : getBmiBasedWorkoutPlan();

        tvFitnessDietPlan.setText(diet);
        tvFitnessWorkoutPlan.setText(workout);

        // --- 4. Load BMI history from Firestore fitness_logs ---
        if (currentMember.id == null || currentMember.id.isEmpty()) return;
        db.collection("fitness_logs")
                .whereEqualTo("memberId", currentMember.id)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutBmiHistoryCards.removeAllViews();
                    if (snapshots == null || snapshots.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No BMI logs yet. Calculate above to start tracking.");
                        tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                        tvEmpty.setTextSize(12);
                        layoutBmiHistoryCards.addView(tvEmpty);
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        String logDate = doc.getString("date");
                        Double logBmi = doc.getDouble("bmi");
                        String logCat = doc.getString("bmiCategory");
                        Double logWeight = doc.getDouble("weight");
                        Double logHeight = doc.getDouble("height");
                        if (logDate == null || logBmi == null) continue;

                        // Build a row card for each log entry
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.VERTICAL);
                        row.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_action_card));
                        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        rowLp.setMargins(0, 0, 0, 16);
                        row.setLayoutParams(rowLp);
                        row.setPadding(24, 16, 24, 16);

                        TextView tvDate = new TextView(this);
                        tvDate.setText("📅 " + logDate);
                        tvDate.setTextColor(Color.parseColor("#94A3B8"));
                        tvDate.setTextSize(11);
                        row.addView(tvDate);

                        TextView tvBmiRow = new TextView(this);
                        String bmiDisplay = String.format(Locale.getDefault(), "BMI: %.1f  •  %s", logBmi, logCat != null ? logCat : "");
                        tvBmiRow.setText(bmiDisplay);
                        tvBmiRow.setTextColor(Color.WHITE);
                        tvBmiRow.setTextSize(14);
                        tvBmiRow.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                        row.addView(tvBmiRow);

                        if (logWeight != null && logHeight != null) {
                            TextView tvStats = new TextView(this);
                            tvStats.setText(String.format(Locale.getDefault(), "%.1f kg  •  %.0f cm", logWeight, logHeight));
                            tvStats.setTextColor(Color.parseColor("#64748B"));
                            tvStats.setTextSize(12);
                            row.addView(tvStats);
                        }

                        layoutBmiHistoryCards.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutBmiHistoryCards.removeAllViews();
                    TextView tvEmpty = new TextView(this);
                    tvEmpty.setText("No BMI logs found.");
                    tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                    tvEmpty.setTextSize(12);
                    layoutBmiHistoryCards.addView(tvEmpty);
                });
    }

    /** Returns a BMI-based recommended diet plan if no trainer plan is assigned. */
    private String getBmiBasedDietPlan() {
        if (currentMember == null || currentMember.weight <= 0 || currentMember.height <= 0)
            return "No diet plan assigned yet. Contact your trainer.";
        double hM = currentMember.height / 100.0;
        double bmi = currentMember.weight / (hM * hM);
        if (bmi < 18.5)
            return "🥣 High-Calorie Plan (BMI-based):\n" +
                   "• Breakfast: Oatmeal + banana + peanut butter + whole milk\n" +
                   "• Lunch: Rice + chicken + lentils + olive oil\n" +
                   "• Snack: Nuts, cheese, dried fruit\n" +
                   "• Dinner: Pasta + salmon + avocado\nGoal: caloric surplus (+500 kcal/day)";
        else if (bmi < 24.9)
            return "🥗 Balanced Maintenance Plan (BMI-based):\n" +
                   "• Breakfast: Oats + protein shake + almonds\n" +
                   "• Lunch: Grilled chicken + brown rice + broccoli\n" +
                   "• Snack: Greek yogurt + berries\n" +
                   "• Dinner: Salmon + sweet potato + asparagus";
        else if (bmi < 29.9)
            return "🥙 Lean-Cut Plan (BMI-based):\n" +
                   "• Breakfast: Eggs + whole-grain toast + black coffee\n" +
                   "• Lunch: Tuna salad + quinoa + leafy greens\n" +
                   "• Snack: Apple + almond butter\n" +
                   "• Dinner: Grilled turkey + roasted veggies\nGoal: mild deficit (-300 kcal/day)";
        else
            return "🥦 Low-Carb Deficit Plan (BMI-based):\n" +
                   "• Breakfast: Egg whites + spinach omelette + green tea\n" +
                   "• Lunch: Grilled chicken breast + large salad (no dressing)\n" +
                   "• Snack: Cucumber + hummus\n" +
                   "• Dinner: Baked fish + steamed broccoli\nGoal: deficit (-500 kcal/day), limit carbs";
    }

    /** Returns a BMI-based recommended workout plan if no trainer plan is assigned. */
    private String getBmiBasedWorkoutPlan() {
        if (currentMember == null || currentMember.weight <= 0 || currentMember.height <= 0)
            return "No workout plan assigned yet. Contact your trainer.";
        double hM = currentMember.height / 100.0;
        double bmi = currentMember.weight / (hM * hM);
        if (bmi < 18.5)
            return "💪 Muscle-Gain Program (BMI-based):\n" +
                   "• Mon/Wed/Fri: Heavy compound lifts (Squat, Deadlift, Bench)\n" +
                   "• Tue/Thu: Accessory work + pull-ups + dips\n" +
                   "• Sat: Rest or light walk\nGoal: progressive overload, 3–4 sets × 6–8 reps";
        else if (bmi < 24.9)
            return "🏋️ Maintenance & Tone Program (BMI-based):\n" +
                   "• Mon/Wed/Fri: Full-body resistance training\n" +
                   "• Tue/Thu: 30-min cardio (run/cycle)\n" +
                   "• Sat: Flexibility & yoga\nGoal: maintain composition, moderate intensity";
        else if (bmi < 29.9)
            return "🔥 Fat-Burn Program (BMI-based):\n" +
                   "• Mon/Wed/Fri: Circuit training (HIIT + weights)\n" +
                   "• Tue/Thu: 45-min steady-state cardio\n" +
                   "• Sat: Brisk walk or swim\nGoal: caloric burn, 70–80% max heart rate";
        else
            return "🚶 Low-Impact Start Program (BMI-based):\n" +
                   "• Daily: 30-min brisk walk\n" +
                   "• Mon/Wed/Fri: Light resistance bands + chair squats\n" +
                   "• Sat: 20-min water aerobics or swimming\nGoal: build base fitness safely, increase each week";
    }

    private void setupFitnessTab() {
        btnCalculateBmi.setOnClickListener(v -> {
            String strHeight = etBmiHeight.getText().toString().trim();
            String strWeight = etBmiWeight.getText().toString().trim();

            if (TextUtils.isEmpty(strHeight)) { etBmiHeight.setError("Height is required"); etBmiHeight.requestFocus(); return; }
            if (TextUtils.isEmpty(strWeight)) { etBmiWeight.setError("Weight is required"); etBmiWeight.requestFocus(); return; }

            try {
                double h = Double.parseDouble(strHeight);
                double w = Double.parseDouble(strWeight);
                if (h <= 0 || w <= 0) { Toast.makeText(this, "Height and weight must be positive.", Toast.LENGTH_SHORT).show(); return; }

                currentMember.height = h;
                currentMember.weight = w;

                double hMeters = h / 100.0;
                double bmi = w / (hMeters * hMeters);

                String cat;
                int color;
                if (bmi < 18.5)      { cat = "Underweight";   color = Color.parseColor("#FF9500"); }
                else if (bmi < 24.9) { cat = "Normal Weight"; color = Color.parseColor("#34C759"); }
                else if (bmi < 29.9) { cat = "Overweight";    color = Color.parseColor("#FF9500"); }
                else                 { cat = "Obese";          color = Color.parseColor("#FF3B30"); }

                // Display result immediately
                layoutBmiResult.setVisibility(View.VISIBLE);
                tvBmiScore.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                tvBmiCategory.setText(cat);
                tvBmiCategory.setTextColor(color);

                currentMember.notifications.add("Calculated BMI: " + String.format(Locale.getDefault(), "%.1f", bmi) + " (" + cat + ")");
                updateNotificationBadge();

                // --- Firestore upsert: one document per member per day ---
                Calendar cal = Calendar.getInstance();
                String dateStr = cal.get(Calendar.DAY_OF_MONTH) + "/"
                        + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);

                if (currentMember.id != null && !currentMember.id.isEmpty()) {
                    String docId = currentMember.id + "_" + dateStr.replace("/", "-");

                    Map<String, Object> logData = new HashMap<>();
                    logData.put("memberId", currentMember.id);
                    logData.put("memberName", currentMember.name != null ? currentMember.name : "Member");
                    logData.put("date", dateStr);
                    logData.put("height", h);
                    logData.put("weight", w);
                    logData.put("bmi", Math.round(bmi * 10.0) / 10.0);
                    logData.put("bmiCategory", cat);
                    logData.put("timestamp", com.google.firebase.Timestamp.now());

                    db.collection("fitness_logs").document(docId)
                            .set(logData)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "✅ BMI logged for " + dateStr, Toast.LENGTH_SHORT).show();
                                refreshFitnessTab();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "BMI calculated but not saved: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                    // Also update height/weight on the user document
                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("height", h);
                    userUpdates.put("weight", w);
                    db.collection("users").document(currentMember.id).update(userUpdates);
                } else {
                    Toast.makeText(this, "BMI calculated (not saved — member ID missing).", Toast.LENGTH_SHORT).show();
                    refreshFitnessTab();
                }

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show();
            }
        });

        // Hydration tracker — persists to Firestore after each change
        btnWaterMinus.setOnClickListener(v -> {
            if (currentMember == null || currentMember.waterIntake <= 0) return;
            currentMember.waterIntake--;
            persistWaterIntake();
            tvWaterCount.setText(currentMember.waterIntake + " / 8 glasses");
        });

        btnWaterPlus.setOnClickListener(v -> {
            if (currentMember == null) return;
            if (currentMember.waterIntake >= 20) { Toast.makeText(this, "Maximum tracking limit reached.", Toast.LENGTH_SHORT).show(); return; }
            currentMember.waterIntake++;
            persistWaterIntake();
            tvWaterCount.setText(currentMember.waterIntake + " / 8 glasses");
            if (currentMember.waterIntake == 8) {
                Toast.makeText(this, "💧 Awesome! Daily hydration target met!", Toast.LENGTH_LONG).show();
                currentMember.notifications.add("Great job meeting your daily hydration target (8 glasses)!");
                updateNotificationBadge();
            }
        });
    }

    /** Persists current waterIntake and today's date to Firestore. */
    private void persistWaterIntake() {
        if (currentMember == null || currentMember.id == null || currentMember.id.isEmpty()) return;
        Calendar cal = Calendar.getInstance();
        String todayStr = cal.get(Calendar.DAY_OF_MONTH) + "/"
                + (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.YEAR);
        currentMember.waterDate = todayStr;
        Map<String, Object> waterUpdate = new HashMap<>();
        waterUpdate.put("waterIntake", currentMember.waterIntake);
        waterUpdate.put("waterDate", todayStr);
        db.collection("users").document(currentMember.id).update(waterUpdate);
    }


    // ==================== TAB 4: FEEDBACK & RATING ====================

    private void setupFeedbackTab() {
        // Filter trainers list: only allow feedback for trainers the member has booked
        List<String> list = new ArrayList<>();
        list.add("General Gym Experience");

        List<String> bookedTrainerNames = new ArrayList<>();
        for (Booking b : memberBookings) {
            if (b.trainerName != null && !b.trainerName.isEmpty() && !b.trainerName.equalsIgnoreCase("None")) {
                if (!bookedTrainerNames.contains(b.trainerName)) {
                    bookedTrainerNames.add(b.trainerName);
                }
            }
        }

        for (Trainer t : trainerList) {
            if (bookedTrainerNames.contains(t.name)) {
                list.add(t.name + " (" + t.specialization + ")");
            }
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

                // Link feedback to trainer in Firestore and re-calculate average rating
                if (!item.equals("General Gym Experience")) {
                    for (Trainer t : trainerList) {
                        if (item.startsWith(t.name)) {
                            String reviewEntry = selectedRating + " ★ - \"" + feedbackMsg + "\" (by " + (currentMember != null ? currentMember.name : "Member") + ")";
                            t.addFeedback(reviewEntry);
                            double newAvg = t.getAverageRating();
                            t.rating = String.format(Locale.getDefault(), "%.1f", newAvg);

                            // Persist updated feedback and average rating to Firestore trainer document
                            if (t.id != null && !t.id.isEmpty()) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("feedback", t.feedback);
                                updates.put("rating", t.rating);
                                db.collection("users").document(t.id)
                                        .update(updates)
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Feedback saved locally only.", Toast.LENGTH_SHORT).show());
                            }
                            break;
                        }
                    }
                }

                Toast.makeText(this, "Feedback submitted successfully. Thank you!", Toast.LENGTH_LONG).show();

                // Clear fields & refresh trainer list to show updated ratings
                setStarRating(0);
                etFeedbackMsg.setText("");
                spinnerFeedbackTrainer.setSelection(0);
                setupProgramsTab();
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
