package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingPlanRequestsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvRequests;
    private View layoutEmpty;
    private TextView tvCount;

    // List of members with planStatus == "Pending"
    private final List<Member> pendingMembers = new ArrayList<>();
    // Map of package name -> price (fetched from "packages" collection)
    private final Map<String, String> packagePriceMap = new HashMap<>();

    private PendingRequestAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pending_plan_requests);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        rvRequests  = findViewById(R.id.rv_pending_requests);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvCount     = findViewById(R.id.tv_pending_count);

        adapter = new PendingRequestAdapter();
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);

        // First load packages so we have prices, then load pending members
        loadPackagePrices();
    }

    // ── Load packages from Firestore ─────────────────────────────────────────
    private void loadPackagePrices() {
        db.collection("packages").get().addOnSuccessListener(snap -> {
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String name  = doc.getString("name");
                String price = doc.getString("price");
                if (name != null) packagePriceMap.put(name, price != null ? "$" + price + "/mo" : "N/A");
            }
            listenToPendingMembers();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load packages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            listenToPendingMembers(); // still try to show members
        });
    }

    // ── Real-time listener for members with planStatus == "Pending" ──────────
    private void listenToPendingMembers() {
        db.collection("users")
            .whereEqualTo("role", "member")
            .whereEqualTo("planStatus", "Pending")
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                pendingMembers.clear();
                if (value != null) {
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            Member m = doc.toObject(Member.class);
                            if (m == null) m = new Member();
                            m.id = doc.getId();
                            if (m.name == null) m.name = doc.getString("name");
                            if (m.email == null) m.email = doc.getString("email");
                            if (m.pendingPlan == null) m.pendingPlan = doc.getString("pendingPlan");
                            if (m.name == null) m.name = "Member";
                            if (m.email == null) m.email = "";
                            if (m.pendingPlan == null) m.pendingPlan = "";
                            pendingMembers.add(m);
                        } catch (Exception ignored) {}
                    }
                }
                updateUI();
            });
    }

    private void updateUI() {
        int count = pendingMembers.size();
        tvCount.setText(count == 0 ? "No pending requests" : count + " request" + (count == 1 ? "" : "s") + " awaiting review");
        if (count == 0) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    // ── Approve a plan request ────────────────────────────────────────────────
    private void approvePlanRequest(Member member) {
        String planName = member.pendingPlan;
        Map<String, Object> updates = new HashMap<>();
        updates.put("plan", planName);
        updates.put("planStatus", "Active");
        updates.put("pendingPlan", "");

        db.collection("users").document(member.id).update(updates)
            .addOnSuccessListener(aVoid -> {
                // Send approval announcement to member's subcollection
                sendMemberAnnouncement(member.id, member.name, planName, true);
                Toast.makeText(this, member.name + "'s plan approved!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to approve: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Reject a plan request ─────────────────────────────────────────────────
    private void rejectPlanRequest(Member member) {
        String planName = member.pendingPlan;
        Map<String, Object> updates = new HashMap<>();
        updates.put("planStatus", "Rejected");
        updates.put("pendingPlan", "");

        db.collection("users").document(member.id).update(updates)
            .addOnSuccessListener(aVoid -> {
                sendMemberAnnouncement(member.id, member.name, planName, false);
                Toast.makeText(this, member.name + "'s request rejected.", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to reject: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Write an announcement into the member's announcements subcollection ───
    private void sendMemberAnnouncement(String uid, String memberName, String planName, boolean approved) {
        String title   = approved ? "🎉 Plan Request Approved!" : "Plan Request Update";
        String message = approved
                ? "Congratulations " + memberName + "! Your request for the \"" + planName + "\" plan has been approved. Enjoy your membership!"
                : "Hi " + memberName + ", your request for the \"" + planName + "\" plan has been reviewed and was not approved at this time. Please contact admin for more information.";

        String announcementId = db.collection("announcements").document().getId();

        // Write to global announcements collection
        Map<String, Object> globalDoc = new HashMap<>();
        globalDoc.put("id", announcementId);
        globalDoc.put("title", title);
        globalDoc.put("message", message);
        globalDoc.put("targetAudience", "member");
        globalDoc.put("audienceLabel", "Individual Member");
        globalDoc.put("timestamp", System.currentTimeMillis());
        db.collection("announcements").document(announcementId).set(globalDoc);

        // Write to member's personal announcements subcollection
        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("announcementId", announcementId);
        userDoc.put("title", title);
        userDoc.put("message", message);
        userDoc.put("timestamp", System.currentTimeMillis());
        userDoc.put("read", false);
        db.collection("users").document(uid)
          .collection("announcements").document(announcementId)
          .set(userDoc);
    }

    // ── RecyclerView Adapter ──────────────────────────────────────────────────
    private class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_request, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int position) {
            Member m = pendingMembers.get(position);

            // Initials
            String initials = "";
            if (m.name != null && !m.name.isEmpty()) {
                String[] parts = m.name.trim().split(" ");
                initials += parts[0].charAt(0);
                if (parts.length > 1) initials += parts[parts.length - 1].charAt(0);
            }
            h.tvInitials.setText(initials.toUpperCase());
            h.tvName.setText(m.name);
            h.tvEmail.setText(m.email != null ? m.email : "");

            // Requested plan
            String plan = (m.pendingPlan != null && !m.pendingPlan.isEmpty()) ? m.pendingPlan : "Unknown Plan";
            h.tvPlan.setText(plan);

            // Price lookup
            String price = packagePriceMap.get(plan);
            h.tvPrice.setText(price != null ? price : "N/A");

            h.btnApprove.setOnClickListener(v -> approvePlanRequest(m));
            h.btnReject.setOnClickListener(v -> rejectPlanRequest(m));
        }

        @Override
        public int getItemCount() { return pendingMembers.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvInitials, tvName, tvEmail, tvPlan, tvPrice;
            Button btnApprove, btnReject;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInitials = itemView.findViewById(R.id.tv_member_initials);
                tvName     = itemView.findViewById(R.id.tv_member_name);
                tvEmail    = itemView.findViewById(R.id.tv_member_email);
                tvPlan     = itemView.findViewById(R.id.tv_requested_plan);
                tvPrice    = itemView.findViewById(R.id.tv_plan_price);
                btnApprove = itemView.findViewById(R.id.btn_approve);
                btnReject  = itemView.findViewById(R.id.btn_reject);
            }
        }
    }
}
