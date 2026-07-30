package com.example.gym_management_and_fitness_tracking_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvStatMembers, tvStatTrainers, tvStatRevenue, tvStatAttendance;
    private final Map<String, Double> packagePrices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        tvStatMembers    = findViewById(R.id.tv_stat_members);
        tvStatTrainers   = findViewById(R.id.tv_stat_trainers);
        tvStatRevenue    = findViewById(R.id.tv_stat_revenue);
        tvStatAttendance = findViewById(R.id.tv_stat_attendance);

        // Fetch dynamic stats
        loadDashboardStats();

        // Set up click listeners for all quick actions
        findViewById(R.id.action_members).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageMembersActivity.class));
        });

        findViewById(R.id.action_trainers).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageTrainersActivity.class));
        });

        findViewById(R.id.action_packages).setOnClickListener(v -> {
            startActivity(new Intent(this, ManagePackagesActivity.class));
        });

        findViewById(R.id.action_payments).setOnClickListener(v -> {
            startActivity(new Intent(this, ManagePaymentsActivity.class));
        });

        findViewById(R.id.action_attendance).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageAttendanceActivity.class));
        });

        findViewById(R.id.action_notifications).setOnClickListener(v -> {
            startActivity(new Intent(this, SendNotificationsActivity.class));
        });

        findViewById(R.id.action_pending_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, PendingPlanRequestsActivity.class));
        });

        findViewById(R.id.action_reports).setOnClickListener(v -> {
            startActivity(new Intent(this, GenerateReportsActivity.class));
        });
        
        // Logout Button
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        // Step 1: Cache package prices to accurately calculate monthly revenue
        db.collection("packages").get().addOnSuccessListener(packageSnap -> {
            packagePrices.clear();
            for (DocumentSnapshot doc : packageSnap.getDocuments()) {
                String name = doc.getString("name");
                String priceStr = doc.getString("price");
                if (name != null && priceStr != null) {
                    try {
                        // Strip currency symbols if any
                        String cleanedPrice = priceStr.replaceAll("[^0-9.]", "");
                        double priceVal = Double.parseDouble(cleanedPrice);
                        packagePrices.put(name.trim().toLowerCase(), priceVal);
                    } catch (Exception ignored) {}
                }
            }

            // Step 2: Fetch members & compute active plan revenue
            db.collection("users").whereEqualTo("role", "member").get().addOnSuccessListener(memberSnap -> {
                int memberCount = memberSnap.size();
                tvStatMembers.setText(String.valueOf(memberCount));

                double totalMonthlyRevenue = 0;
                int todayAttendanceCount = 0;

                for (DocumentSnapshot doc : memberSnap.getDocuments()) {
                    String plan = doc.getString("plan");
                    if (plan != null && !plan.isEmpty() && !"None".equalsIgnoreCase(plan)) {
                        Double price = packagePrices.get(plan.trim().toLowerCase());
                        if (price != null) {
                            totalMonthlyRevenue += price;
                        }
                    }

                    String checkedIn = doc.getString("checkedInTime");
                    if (checkedIn != null && !checkedIn.isEmpty() && !"Not Checked In".equalsIgnoreCase(checkedIn)) {
                        todayAttendanceCount++;
                    }
                }

                tvStatRevenue.setText(String.format(Locale.US, "$%.0f", totalMonthlyRevenue));
                tvStatAttendance.setText(String.valueOf(todayAttendanceCount));
            });
        });

        // Step 3: Fetch active trainers count
        db.collection("users").whereEqualTo("role", "trainer").get().addOnSuccessListener(trainerSnap -> {
            tvStatTrainers.setText(String.valueOf(trainerSnap.size()));
        });
    }
}

