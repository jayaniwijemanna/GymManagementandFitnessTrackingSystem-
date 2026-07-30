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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemberDashboardActivity extends AppCompatActivity {

    private Member currentMember;

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
    private LinearLayout btnHomeBuyPlan, btnHomeCheckin, btnHomeBookTrainer, btnHomeChat;

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

        // Identify logged-in member
        String memberEmail = getIntent().getStringExtra("MEMBER_EMAIL");
        currentMember = null;
        if (!TextUtils.isEmpty(memberEmail)) {
            for (Member m : DataStore.getInstance().members) {
                if (m.email.equalsIgnoreCase(memberEmail)) {
                    currentMember = m;
                    break;
                }
            }
        }
        // Fallback for safety
        if (currentMember == null) {
            if (!DataStore.getInstance().members.isEmpty()) {
                currentMember = DataStore.getInstance().members.get(0);
            } else {
                currentMember = new Member("Default User", "member@gmail.com", "555-0000", "None", "password");
                DataStore.getInstance().members.add(currentMember);
            }
        }

        initializeViews();
        setupNavigation();
        setupHeader();
        refreshHomeTab();
        setupProgramsTab();
        setupFitnessTab();
        setupFeedbackTab();
    }

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
        tvHeaderName.setText(currentMember.name);
        tvHeaderInitials.setText(currentMember.getInitials());

        // Update badge visibility
        updateNotificationBadge();

        btnNotifications.setOnClickListener(v -> showNotificationsDialog());

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Logged out securely.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MemberDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateNotificationBadge() {
        if (currentMember.notifications.isEmpty()) {
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

        if (currentMember.notifications.isEmpty()) {
            builder.setMessage("No new notifications.");
        } else {
            String[] array = currentMember.notifications.toArray(new String[0]);
            builder.setItems(array, null);
        }

        builder.setPositiveButton("Clear All", (dialog, which) -> {
            currentMember.notifications.clear();
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
                break;
            case 2:
                viewFitness.setVisibility(View.VISIBLE);
                highlightTab(imgNavFitness, tvNavFitness);
                refreshFitnessTab();
                break;
            case 3:
                viewFeedback.setVisibility(View.VISIBLE);
                highlightTab(imgNavFeedback, tvNavFeedback);
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
        // Membership Details
        if (currentMember.plan == null || currentMember.plan.isEmpty() || currentMember.plan.equals("None")) {
            tvHomePlanName.setText("No Active Package");
            tvHomePlanStatus.setText("Inactive");
            tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
            tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2A1A0D"))); // stat_orange_bg
            tvHomePlanStatus.setTextColor(Color.parseColor("#FF9500"));
            tvHomePlanDesc.setText("Purchase a training package in the Programs tab to gain access.");
            btnHomeBuyPlan.setVisibility(View.VISIBLE);
        } else {
            tvHomePlanName.setText(currentMember.plan);
            tvHomePlanStatus.setText("Active");
            tvHomePlanStatus.setBackgroundResource(R.drawable.bg_badge);
            tvHomePlanStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#0D2818"))); // stat_green_bg
            tvHomePlanStatus.setTextColor(Color.parseColor("#34C759"));
            
            // Search package details
            String desc = "Your premium package allows access to standard gym areas.";
            for (GymPackage p : DataStore.getInstance().packages) {
                if (p.name.equalsIgnoreCase(currentMember.plan)) {
                    desc = p.description;
                    break;
                }
            }
            tvHomePlanDesc.setText(desc);
            btnHomeBuyPlan.setVisibility(View.GONE);
        }

        // Check in status
        if (currentMember.checkedInTime.equals("Not Checked In")) {
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
        if (currentMember.bookedTrainer.equals("None")) {
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
        btnHomeBookTrainer.setOnClickListener(v -> selectTab(1));
        btnHomeCheckin.setOnClickListener(v -> triggerCheckinFlow());
        btnHomeChat.setOnClickListener(v -> openChatDialog());
    }

    private void triggerCheckinFlow() {
        // Show simulated QR scanner dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_manage_attendance, null); // reuse background setup or structure
        
        // Let's dynamically construct a premium scan dialog
        View scanLayout = inflater.inflate(R.layout.activity_verify_otp, null); // temp view layout
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

        TextView note = scanLayout.findViewById(R.id.tv_cancel).findViewById(R.id.tv_cancel); // wait, it's just cancel
        note.setText("Searching for camera sensor...");
        note.setTextColor(Color.parseColor("#94A3B8"));

        // Hide verify button
        scanLayout.findViewById(R.id.btn_verify).setVisibility(View.GONE);

        builder.setView(scanLayout);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Simulate laser scanner line motion and successful scan in 2 seconds
        new Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
                
                // Set present checked-in
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                currentMember.checkedInTime = sdf.format(new Date());
                currentMember.notifications.add("Checked in successfully at " + currentMember.checkedInTime);
                updateNotificationBadge();
                refreshHomeTab();

                Toast.makeText(this, "Gym Check-in Successful! Welcome to Titan.", Toast.LENGTH_LONG).show();
            }
        }, 2200);

        scanLayout.findViewById(R.id.tv_cancel).setOnClickListener(v -> dialog.dismiss());
    }

    // ==================== TAB 2: PROGRAMS & BOOKING ====================
    private void setupProgramsTab() {
        // Load Packages list
        layoutPackagesList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (GymPackage p : DataStore.getInstance().packages) {
            View pkgView = inflater.inflate(R.layout.item_member_package, layoutPackagesList, false);
            TextView tvName = pkgView.findViewById(R.id.tv_pkg_name);
            TextView tvPrice = pkgView.findViewById(R.id.tv_pkg_price);
            TextView tvDesc = pkgView.findViewById(R.id.tv_pkg_desc);
            TextView btnBuy = pkgView.findViewById(R.id.btn_pkg_buy);

            tvName.setText(p.name);
            tvPrice.setText("$" + p.price + " / month");
            tvDesc.setText(p.description);

            btnBuy.setOnClickListener(v -> showPaymentDialog(p));

            layoutPackagesList.addView(pkgView);
        }

        // Load Trainers list
        layoutTrainersList.removeAllViews();
        for (Trainer t : DataStore.getInstance().trainers) {
            View trnView = inflater.inflate(R.layout.item_member_trainer, layoutTrainersList, false);
            TextView tvInitials = trnView.findViewById(R.id.tv_trn_initials);
            TextView tvName = trnView.findViewById(R.id.tv_trn_name);
            TextView tvSpec = trnView.findViewById(R.id.tv_trn_spec);
            TextView btnBook = trnView.findViewById(R.id.btn_trn_book);

            tvInitials.setText(t.getInitials());
            tvName.setText(t.name);
            tvSpec.setText(t.specialization);

            btnBook.setOnClickListener(v -> showBookingDialog(t));

            layoutTrainersList.addView(trnView);
        }
    }

    private void showPaymentDialog(GymPackage pkg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        builder.setTitle("Select Payment Method");

        // Custom Layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 30, 40, 30);

        TextView info = new TextView(this);
        info.setText("Package: " + pkg.name + "\nPrice: $" + pkg.price + ".00");
        info.setTextColor(Color.WHITE);
        info.setTextSize(16);
        info.setLineSpacing(1.2f, 1.2f);
        root.addView(info);

        // Spacer
        View spacer1 = new View(this);
        spacer1.setMinimumHeight(24);
        root.addView(spacer1);

        RadioGroup group = new RadioGroup(this);
        RadioButton rbCard = new RadioButton(this);
        rbCard.setText("Credit / Debit Card");
        rbCard.setTextColor(Color.WHITE);
        RadioButton rbPaypal = new RadioButton(this);
        rbPaypal.setText("PayPal Secure Account");
        rbPaypal.setTextColor(Color.WHITE);
        RadioButton rbCash = new RadioButton(this);
        rbCash.setText("Cash Desk Payment");
        rbCash.setTextColor(Color.WHITE);

        group.addView(rbCard);
        group.addView(rbPaypal);
        group.addView(rbCash);
        rbCard.setChecked(true);
        root.addView(group);

        // Fields Container
        LinearLayout fieldsLayout = new LinearLayout(this);
        fieldsLayout.setOrientation(LinearLayout.VERTICAL);
        fieldsLayout.setPadding(0, 20, 0, 10);

        // Card Details Input Fields
        EditText etCard = new EditText(this);
        etCard.setHint("Card Number (16-digits)");
        etCard.setTextColor(Color.WHITE);
        etCard.setHintTextColor(Color.GRAY);
        etCard.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        fieldsLayout.addView(etCard);

        root.addView(fieldsLayout);

        // Switch inputs dynamically
        rbCard.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                fieldsLayout.removeAllViews();
                etCard.setHint("Card Number (16-digits)");
                etCard.setText("");
                fieldsLayout.addView(etCard);
            }
        });
        rbPaypal.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                fieldsLayout.removeAllViews();
                EditText etPaypal = new EditText(this);
                etPaypal.setHint("PayPal Email ID");
                etPaypal.setTextColor(Color.WHITE);
                etPaypal.setHintTextColor(Color.GRAY);
                fieldsLayout.addView(etPaypal);
            }
        });
        rbCash.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                fieldsLayout.removeAllViews();
                TextView cashInfo = new TextView(this);
                cashInfo.setText("Please request physical cash activation of " + pkg.name + " package at the receptionist desk.");
                cashInfo.setTextColor(Color.parseColor("#94A3B8"));
                fieldsLayout.addView(cashInfo);
            }
        });

        builder.setView(root);
        builder.setPositiveButton("Complete Purchase", (dialog, which) -> {
            // Check validations if cash is not selected
            if (rbCard.isChecked() && etCard.getText().length() < 12) {
                Toast.makeText(this, "Transaction declined: Invalid card details.", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progress = new ProgressDialog(this, ProgressDialog.THEME_HOLO_DARK);
            progress.setMessage("Securing payment link...");
            progress.setCancelable(false);
            progress.show();

            new Handler().postDelayed(() -> {
                progress.dismiss();
                currentMember.plan = pkg.name;
                currentMember.notifications.add("Purchase successful! Activated plan: " + pkg.name);
                updateNotificationBadge();
                refreshHomeTab();
                Toast.makeText(this, pkg.name + " Activated successfully!", Toast.LENGTH_LONG).show();
            }, 1800);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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

                    Toast.makeText(this, "Trainer booked successfully for " + currentMember.bookedTime, Toast.LENGTH_LONG).show();
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
        tvWaterCount.setText(currentMember.waterIntake + " / 8 glasses");

        // Format and render weight log history
        StringBuilder sb = new StringBuilder();
        if (currentMember.weightHistory.isEmpty()) {
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
                currentMember.weightHistory.add(String.valueOf(w));

                // Display
                layoutBmiResult.setVisibility(View.VISIBLE);
                tvBmiScore.setText(String.format(Locale.getDefault(), "%.1f", bmi));

                String cat;
                int color;
                if (bmi < 18.5) {
                    cat = "Underweight";
                    color = Color.parseColor("#FF9500"); // orange
                } else if (bmi < 24.9) {
                    cat = "Normal Weight";
                    color = Color.parseColor("#34C759"); // green
                } else if (bmi < 29.9) {
                    cat = "Overweight";
                    color = Color.parseColor("#FF9500");
                } else {
                    cat = "Obese";
                    color = Color.parseColor("#FF3B30"); // red
                }
                tvBmiCategory.setText(cat);
                tvBmiCategory.setTextColor(color);

                currentMember.notifications.add("Calculated BMI: " + String.format(Locale.getDefault(), "%.1f", bmi) + " (" + cat + ")");
                updateNotificationBadge();
                refreshFitnessTab();

                Toast.makeText(this, "BMI calculated and Weight logged!", Toast.LENGTH_SHORT).show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number inputs", Toast.LENGTH_SHORT).show();
            }
        });

        // Hydration tracker
        btnWaterMinus.setOnClickListener(v -> {
            if (currentMember.waterIntake > 0) {
                currentMember.waterIntake--;
                refreshFitnessTab();
            }
        });

        btnWaterPlus.setOnClickListener(v -> {
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
        // Populate Spinner
        List<String> list = new ArrayList<>();
        list.add("General Gym Experience");
        for (Trainer t : DataStore.getInstance().trainers) {
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
                currentMember.notifications.add("Submitted " + selectedRating + "-star rating feedback for " + item);
                updateNotificationBadge();

                // Link feedback to trainer
                if (!item.equals("General Gym Experience")) {
                    for (Trainer t : DataStore.getInstance().trainers) {
                        if (item.startsWith(t.name)) {
                            t.addFeedback(selectedRating + " ★ - \"" + feedbackMsg + "\" (by " + currentMember.name + ")");
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
        if (currentMember.bookedTrainer.equals("None")) return;
        
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
