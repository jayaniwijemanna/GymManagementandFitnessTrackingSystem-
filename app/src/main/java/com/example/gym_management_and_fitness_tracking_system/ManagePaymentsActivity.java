package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManagePaymentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvTotalRevenue, tvActiveCount, tvMonthSubtitle;
    private LinearLayout containerPlanBreakdown, containerMemberPayments;

    // Helper map: Package Name (lowercase) -> Price double value
    private final Map<String, Double> packagePrices = new HashMap<>();
    private final Map<String, String> packageFormattedPrices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_payments);

        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTotalRevenue = findViewById(R.id.tv_total_monthly_revenue);
        tvActiveCount = findViewById(R.id.tv_active_subscriptions_count);
        tvMonthSubtitle = findViewById(R.id.tv_month_subtitle);

        containerPlanBreakdown = findViewById(R.id.container_plan_breakdown);
        containerMemberPayments = findViewById(R.id.container_member_payments);

        // Set current month header
        String currentMonthName = new SimpleDateFormat("MMMM yyyy", Locale.US).format(new Date());
        tvMonthSubtitle.setText("Revenue Breakdown for " + currentMonthName);

        loadRevenueData();
    }

    private void loadRevenueData() {
        // Step 1: Load packages to retrieve official pricing per plan
        db.collection("packages").get().addOnSuccessListener(packageSnap -> {
            packagePrices.clear();
            packageFormattedPrices.clear();

            for (DocumentSnapshot doc : packageSnap.getDocuments()) {
                String name = doc.getString("name");
                String priceStr = doc.getString("price");
                if (name != null && priceStr != null) {
                    String cleanName = name.trim();
                    try {
                        String cleanPrice = priceStr.replaceAll("[^0-9.]", "");
                        double priceVal = Double.parseDouble(cleanPrice);
                        packagePrices.put(cleanName.toLowerCase(), priceVal);
                        packageFormattedPrices.put(cleanName.toLowerCase(), priceStr);
                    } catch (Exception ignored) {}
                }
            }

            // Step 2: Fetch members and group revenue by plan
            fetchMembersAndCalculateRevenue();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load plan pricing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            fetchMembersAndCalculateRevenue();
        });
    }

    private void fetchMembersAndCalculateRevenue() {
        db.collection("users").whereEqualTo("role", "member").get().addOnSuccessListener(memberSnap -> {
            containerPlanBreakdown.removeAllViews();
            containerMemberPayments.removeAllViews();

            // Map: Plan Name (original casing) -> List of Members subscribing to that plan
            Map<String, List<Member>> planMembersMap = new HashMap<>();
            double totalMonthlyRevenue = 0;
            int totalActiveSubscribers = 0;

            for (DocumentSnapshot doc : memberSnap.getDocuments()) {
                try {
                    Member m = doc.toObject(Member.class);
                    if (m == null) m = new Member();
                    m.id = doc.getId();
                    if (m.name == null) m.name = doc.getString("name");
                    if (m.email == null) m.email = doc.getString("email");
                    if (m.plan == null) m.plan = doc.getString("plan");

                    String planName = m.plan;
                    if (planName != null && !planName.isEmpty() && !"None".equalsIgnoreCase(planName)) {
                        totalActiveSubscribers++;

                        if (!planMembersMap.containsKey(planName)) {
                            planMembersMap.put(planName, new ArrayList<>());
                        }
                        planMembersMap.get(planName).add(m);

                        Double unitPrice = packagePrices.get(planName.trim().toLowerCase());
                        if (unitPrice != null) {
                            totalMonthlyRevenue += unitPrice;
                        }
                    } else {
                        // Unsubscribed / None
                        if (!planMembersMap.containsKey("No Plan")) {
                            planMembersMap.put("No Plan", new ArrayList<>());
                        }
                        planMembersMap.get("No Plan").add(m);
                    }
                } catch (Exception ignored) {}
            }

            // Update summary UI
            tvTotalRevenue.setText(String.format(Locale.US, "$%.2f", totalMonthlyRevenue));
            tvActiveCount.setText(totalActiveSubscribers + " active member subscription" + (totalActiveSubscribers == 1 ? "" : "s"));

            // Populate Plan Breakdown Cards
            if (planMembersMap.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("No active member plans registered yet.");
                tvEmpty.setTextColor(getColor(R.color.text_gray));
                tvEmpty.setPadding(0, 16, 0, 16);
                containerPlanBreakdown.addView(tvEmpty);
            } else {
                for (Map.Entry<String, List<Member>> entry : planMembersMap.entrySet()) {
                    String planName = entry.getKey();
                    List<Member> members = entry.getValue();
                    int count = members.size();

                    if ("No Plan".equalsIgnoreCase(planName)) continue;

                    Double unitPrice = packagePrices.get(planName.trim().toLowerCase());
                    double planRevenue = (unitPrice != null ? unitPrice : 0.0) * count;
                    String formattedUnitPrice = packageFormattedPrices.containsKey(planName.trim().toLowerCase())
                            ? "$" + packageFormattedPrices.get(planName.trim().toLowerCase()) + "/mo"
                            : (unitPrice != null ? String.format(Locale.US, "$%.2f/mo", unitPrice) : "Custom");

                    View cardView = LayoutInflater.from(this).inflate(R.layout.item_pending_request, containerPlanBreakdown, false);
                    
                    // Hide action buttons in breakdown card
                    cardView.findViewById(R.id.btn_approve).setVisibility(View.GONE);
                    cardView.findViewById(R.id.btn_reject).setVisibility(View.GONE);

                    TextView tvInitials = cardView.findViewById(R.id.tv_member_initials);
                    TextView tvName = cardView.findViewById(R.id.tv_member_name);
                    TextView tvEmail = cardView.findViewById(R.id.tv_member_email);
                    TextView tvStatus = cardView.findViewById(R.id.tv_status_badge);
                    TextView tvPlan = cardView.findViewById(R.id.tv_requested_plan);
                    TextView tvPrice = cardView.findViewById(R.id.tv_plan_price);

                    tvInitials.setText("PL");
                    tvName.setText(planName);
                    tvEmail.setText(count + " Subscriber" + (count == 1 ? "" : "s") + " × " + formattedUnitPrice);
                    
                    tvStatus.setText("Active Plan");
                    tvStatus.setTextColor(0xFF34C759);

                    // Re-label fields for Plan breakdown view
                    tvPlan.setText("Subscribers: " + count);
                    tvPrice.setText(String.format(Locale.US, "$%.2f/mo", planRevenue));

                    containerPlanBreakdown.addView(cardView);
                }
            }

            // Populate Member Payments List
            for (DocumentSnapshot doc : memberSnap.getDocuments()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String plan = doc.getString("plan");
                if (name == null) name = "Member";
                if (plan == null || plan.isEmpty() || "None".equalsIgnoreCase(plan)) continue;

                Double unitPrice = packagePrices.get(plan.trim().toLowerCase());
                String priceText = (unitPrice != null) ? String.format(Locale.US, "$%.2f", unitPrice) : "Active";

                View item = LayoutInflater.from(this).inflate(R.layout.item_pending_request, containerMemberPayments, false);
                item.findViewById(R.id.btn_approve).setVisibility(View.GONE);
                item.findViewById(R.id.btn_reject).setVisibility(View.GONE);

                TextView tvInitials = item.findViewById(R.id.tv_member_initials);
                TextView tvName = item.findViewById(R.id.tv_member_name);
                TextView tvEmail = item.findViewById(R.id.tv_member_email);
                TextView tvStatus = item.findViewById(R.id.tv_status_badge);
                TextView tvPlan = item.findViewById(R.id.tv_requested_plan);
                TextView tvPrice = item.findViewById(R.id.tv_plan_price);

                String initials = "";
                String[] parts = name.trim().split(" ");
                if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].charAt(0);
                if (parts.length > 1 && !parts[parts.length - 1].isEmpty()) initials += parts[parts.length - 1].charAt(0);
                tvInitials.setText(initials.toUpperCase());

                tvName.setText(name);
                tvEmail.setText(email != null ? email : "");
                tvStatus.setText("PAID THIS MONTH");
                tvStatus.setTextColor(0xFF34C759);

                tvPlan.setText(plan);
                tvPrice.setText(priceText);

                containerMemberPayments.addView(item);
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load revenue details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}

