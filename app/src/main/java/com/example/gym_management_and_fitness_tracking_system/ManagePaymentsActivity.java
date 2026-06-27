package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ManagePaymentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_payments);

        ViewCompat.setOnApplyWindowInsetsListener((android.view.View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        Button btnVerify = findViewById(R.id.btn_verify_payment);
        btnVerify.setOnClickListener(v -> Toast.makeText(this, "Payment Verified", Toast.LENGTH_SHORT).show());
        
        Button btnReceipt = findViewById(R.id.btn_receipt);
        btnReceipt.setOnClickListener(v -> Toast.makeText(this, "Generating Receipt...", Toast.LENGTH_SHORT).show());
    }
}
