package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendNotificationsActivity extends AppCompatActivity {

    // Maps displayed audience label → Firestore role value (null = all)
    private static final String[] AUDIENCE_LABELS = {
            "Everyone (Members & Trainers)",
            "All Members",
            "Trainers"
    };
    private static final String[] AUDIENCE_ROLES = {
            "all",      // query both roles
            "member",
            "trainer"
    };

    private FirebaseFirestore db;
    private Spinner spinnerAudience;
    private EditText etTitle, etMessage;
    private Button btnSend;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_notifications);

        ViewCompat.setOnApplyWindowInsetsListener(
                (View) findViewById(R.id.btn_back).getParent().getParent(),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, systemBars.top, 0, systemBars.bottom);
                    return insets;
                });

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        spinnerAudience = findViewById(R.id.spinner_audience);
        etTitle        = findViewById(R.id.et_notif_title);
        etMessage      = findViewById(R.id.et_notif_message);
        btnSend        = findViewById(R.id.btn_send_notification);
        progressBar    = findViewById(R.id.progress_send);
        tvStatus       = findViewById(R.id.tv_send_status);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, AUDIENCE_LABELS);
        spinnerAudience.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendAnnouncement());
    }

    private void sendAnnouncement() {
        String title   = etTitle.getText().toString().trim();
        String message = etMessage.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }
        if (message.isEmpty()) {
            etMessage.setError("Message is required");
            etMessage.requestFocus();
            return;
        }

        int selectedPos = spinnerAudience.getSelectedItemPosition();
        String audienceRole = AUDIENCE_ROLES[selectedPos];
        String audienceLabel = AUDIENCE_LABELS[selectedPos];

        setLoading(true);
        tvStatus.setVisibility(View.GONE);

        // Build announcement document
        Map<String, Object> announcementData = new HashMap<>();
        announcementData.put("title", title);
        announcementData.put("message", message);
        announcementData.put("targetAudience", audienceRole);
        announcementData.put("audienceLabel", audienceLabel);
        announcementData.put("timestamp", System.currentTimeMillis());

        DocumentReference announcementRef = db.collection("announcements").document();
        announcementData.put("id", announcementRef.getId());

        announcementRef.set(announcementData)
            .addOnSuccessListener(aVoid -> {
                // Push announcement to individual user documents
                pushToTargetUsers(announcementRef.getId(), title, message, audienceRole, audienceLabel);
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Failed to send announcement: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void pushToTargetUsers(String announcementId, String title, String message,
                                   String audienceRole, String audienceLabel) {

        Query query;
        if ("all".equals(audienceRole)) {
            // All users with role member OR trainer
            query = db.collection("users")
                    .whereIn("role", java.util.Arrays.asList("member", "trainer"));
        } else {
            query = db.collection("users").whereEqualTo("role", audienceRole);
        }

        query.get().addOnSuccessListener(querySnapshot -> {
            List<String> userIds = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                userIds.add(doc.getId());
            }

            if (userIds.isEmpty()) {
                setLoading(false);
                showStatus("No " + audienceLabel + " found. Announcement saved to database.", false);
                return;
            }

            // Write announcement entry into each user's announcements sub-collection AND append to notifications array
            final int[] pendingWrites = {userIds.size()};
            String notifItem = title + ": " + message;
            for (String uid : userIds) {
                Map<String, Object> userAnnouncement = new HashMap<>();
                userAnnouncement.put("announcementId", announcementId);
                userAnnouncement.put("title", title);
                userAnnouncement.put("message", message);
                userAnnouncement.put("timestamp", System.currentTimeMillis());
                userAnnouncement.put("read", false);

                // 1. Write to subcollection
                db.collection("users").document(uid)
                    .collection("announcements").document(announcementId)
                    .set(userAnnouncement);

                // 2. Also append notification string to user doc 'notifications' array field
                db.collection("users").document(uid)
                    .update("notifications", com.google.firebase.firestore.FieldValue.arrayUnion(notifItem))
                    .addOnCompleteListener(task -> {
                        pendingWrites[0]--;
                        if (pendingWrites[0] == 0) {
                            setLoading(false);
                            int count = userIds.size();
                            showStatus("✓ Announcement sent to " + count + " " + audienceLabel
                                    + (count == 1 ? "" : "s"), true);
                            etTitle.setText("");
                            etMessage.setText("");
                        }
                    });
            }
        }).addOnFailureListener(e -> {
            setLoading(false);
            Toast.makeText(this, "Failed to fetch users: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void setLoading(boolean loading) {
        btnSend.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String msg, boolean success) {
        tvStatus.setText(msg);
        tvStatus.setTextColor(success ? 0xFF34C759 : 0xFFFF9500); // green or orange
        tvStatus.setVisibility(View.VISIBLE);
    }
}
