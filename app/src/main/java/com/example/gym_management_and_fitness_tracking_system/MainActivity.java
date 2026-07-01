package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private LinearLayout btnSignIn;
    private TextView tvForgotPassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply Window Insets for Edge-to-Edge display compatibility
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI Elements
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        TextView tvSignUp = findViewById(R.id.tv_sign_up);

        // Password Visibility Toggle
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Sign In Action
        btnSignIn.setOnClickListener(v -> performLogin());

        // Forgot Password Action
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Password recovery feature coming soon.", Toast.LENGTH_SHORT).show();
        });

        // Sign Up Link Action
        tvSignUp.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(MainActivity.this, CreateProfileActivity.class);
            startActivity(intent);
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye);
            isPasswordVisible = false;
        } else {
            // Show password
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            isPasswordVisible = true;
        }
        // Retain cursor position at the end of input text
        etPassword.setSelection(etPassword.getText().length());
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (email.equalsIgnoreCase("admin")) {
            Toast.makeText(this, "Connecting to Titan Shield secure database...", Toast.LENGTH_SHORT).show();
            android.content.Intent intent = new android.content.Intent(MainActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Verify if it is a registered member
        Member foundMember = null;
        for (Member m : DataStore.getInstance().members) {
            if (m.email.equalsIgnoreCase(email)) {
                foundMember = m;
                break;
            }
        }

        if (foundMember != null) {
            if (foundMember.password.equals(password)) {
                Toast.makeText(this, "Connecting to Titan Shield secure database...", Toast.LENGTH_SHORT).show();
                android.content.Intent intent = new android.content.Intent(MainActivity.this, MemberDashboardActivity.class);
                intent.putExtra("MEMBER_EMAIL", foundMember.email);
                startActivity(intent);
                finish();
            } else {
                etPassword.setError("Incorrect password");
                etPassword.requestFocus();
            }
            return;
        }

        Toast.makeText(this, "No account found. Please sign up first.", Toast.LENGTH_LONG).show();
    }
}