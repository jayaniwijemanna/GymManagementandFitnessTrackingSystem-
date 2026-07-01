package com.example.gym_management_and_fitness_tracking_system;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp;
    private LinearLayout btnVerify;
    private TextView tvCancel;
    private TextView tvInstructions;

    private String regName;
    private String regEmail;
    private String regPhone;
    private String regPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Retrieve registration data from intent
        Intent incoming = getIntent();
        if (incoming != null) {
            regName = incoming.getStringExtra("REG_NAME");
            regEmail = incoming.getStringExtra("REG_EMAIL");
            regPhone = incoming.getStringExtra("REG_PHONE");
            regPassword = incoming.getStringExtra("REG_PASSWORD");
        }

        // Initialize UI Elements
        etOtp = findViewById(R.id.et_otp);
        btnVerify = findViewById(R.id.btn_verify);
        tvCancel = findViewById(R.id.tv_cancel);
        tvInstructions = findViewById(R.id.tv_otp_instructions);

        if (!TextUtils.isEmpty(regEmail)) {
            tvInstructions.setText("We sent a security OTP to " + regEmail + ". Enter the 4-digit code below.");
        }

        // Verify button action
        btnVerify.setOnClickListener(v -> verifyOtp());

        // Cancel action
        tvCancel.setOnClickListener(v -> finish());
    }

    private void verifyOtp() {
        String enteredCode = etOtp.getText().toString().trim();

        if (TextUtils.isEmpty(enteredCode)) {
            etOtp.setError("Please enter the 4-digit OTP");
            etOtp.requestFocus();
            return;
        }

        if (enteredCode.equals("1234")) {
            // Save the member
            Member newMember = new Member(regName, regEmail, regPhone, "None", regPassword);
            DataStore.getInstance().members.add(newMember);

            Toast.makeText(this, "Profile verified! Welcome to Titan Gym.", Toast.LENGTH_LONG).show();

            // Navigate to member dashboard (clear back stack)
            Intent intent = new Intent(VerifyOtpActivity.this, MemberDashboardActivity.class);
            intent.putExtra("MEMBER_EMAIL", regEmail);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            etOtp.setError("Invalid code. Please use 1234 for testing.");
            etOtp.requestFocus();
        }
    }
}
