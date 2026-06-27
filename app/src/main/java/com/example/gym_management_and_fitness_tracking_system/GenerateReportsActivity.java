package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GenerateReportsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generate_reports);

        ViewCompat.setOnApplyWindowInsetsListener((android.view.View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_report_income).setOnClickListener(v -> showToast("Downloading Income Report PDF..."));
        findViewById(R.id.btn_report_member).setOnClickListener(v -> showToast("Downloading Member Report PDF..."));
        findViewById(R.id.btn_report_trainer).setOnClickListener(v -> showToast("Downloading Trainer Report PDF..."));
        findViewById(R.id.btn_report_attendance).setOnClickListener(v -> showToast("Downloading Attendance Report PDF..."));
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
