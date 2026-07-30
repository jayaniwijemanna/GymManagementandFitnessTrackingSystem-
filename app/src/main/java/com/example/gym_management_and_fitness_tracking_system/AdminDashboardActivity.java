package com.example.gym_management_and_fitness_tracking_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminDashboardActivity extends AppCompatActivity {

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
            // Start MainActivity and clear task stack to prevent going back to dashboard
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
