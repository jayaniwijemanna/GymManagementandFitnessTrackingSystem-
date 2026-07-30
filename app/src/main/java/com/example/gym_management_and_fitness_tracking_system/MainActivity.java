package com.example.gym_management_and_fitness_tracking_system;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private ImageView btnTogglePassword;
    private LinearLayout btnSignIn;
    private TextView tvForgotPassword;
    private boolean isPasswordVisible = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            String email = etEmail.getText().toString().trim();
            if (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                        } else {
                            String err = (task.getException() != null) ? task.getException().getMessage() : "Error sending email";
                            Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                        }
                    });
            } else {
                Toast.makeText(MainActivity.this, "Please enter a valid email to reset password.", Toast.LENGTH_SHORT).show();
            }
        });

        // Sign Up Link Action
        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateProfileActivity.class);
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

        btnSignIn.setEnabled(false);
        Toast.makeText(this, "Connecting to Titan Shield secure database...", Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkRoleAndNavigate(user.getUid(), email);
                    } else {
                        btnSignIn.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Check local DataStore as fallback
                    if (checkLocalDataStoreFallback(email, password)) {
                        return;
                    }
                    btnSignIn.setEnabled(true);
                    String errorMsg = (task.getException() != null) ? task.getException().getMessage() : "Invalid credentials";
                    Toast.makeText(MainActivity.this, "Authentication failed: " + errorMsg, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void checkRoleAndNavigate(String uid, String email) {
        db.collection("users").document(uid).get()
            .addOnCompleteListener(task -> {
                btnSignIn.setEnabled(true);
                String role = "";
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot doc = task.getResult();
                    role = doc.getString("role");
                }

                if (role == null) role = "";

                if ("admin".equalsIgnoreCase(role)) {
                    Toast.makeText(MainActivity.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else if ("trainer".equalsIgnoreCase(role)) {
                    Toast.makeText(MainActivity.this, "Welcome Trainer!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, TrainerDashboardActivity.class);
                    intent.putExtra("TRAINER_EMAIL", email);
                    startActivity(intent);
                    finish();
                } else if ("member".equalsIgnoreCase(role)) {
                    Toast.makeText(MainActivity.this, "Welcome Member!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, MemberDashboardActivity.class);
                    intent.putExtra("MEMBER_EMAIL", email);
                    startActivity(intent);
                    finish();
                } else {
                    // If no role stored in user document, fallback query by email
                    queryRoleByEmail(email);
                }
            });
    }

    private void queryRoleByEmail(String email) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnCompleteListener(task -> {
                String role = "";
                if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                    role = doc.getString("role");
                }

                if (role == null) role = "";

                if ("admin".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else if ("trainer".equalsIgnoreCase(role)) {
                    Intent intent = new Intent(MainActivity.this, TrainerDashboardActivity.class);
                    intent.putExtra("TRAINER_EMAIL", email);
                    startActivity(intent);
                    finish();
                } else {
                    Intent intent = new Intent(MainActivity.this, MemberDashboardActivity.class);
                    intent.putExtra("MEMBER_EMAIL", email);
                    startActivity(intent);
                    finish();
                }
            });
    }

    private boolean checkLocalDataStoreFallback(String email, String password) {
        // Local Member Fallback
        for (Member m : DataStore.getInstance().members) {
            if (m.email.equalsIgnoreCase(email) && m.password.equals(password)) {
                btnSignIn.setEnabled(true);
                Toast.makeText(this, "Logged in via offline mode.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, MemberDashboardActivity.class);
                intent.putExtra("MEMBER_EMAIL", m.email);
                startActivity(intent);
                finish();
                return true;
            }
        }
        // Local Trainer Fallback
        for (Trainer t : DataStore.getInstance().trainers) {
            if (t.email != null && t.email.equalsIgnoreCase(email) && t.password != null && t.password.equals(password)) {
                btnSignIn.setEnabled(true);
                Toast.makeText(this, "Logged in via offline mode.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, TrainerDashboardActivity.class);
                intent.putExtra("TRAINER_EMAIL", t.email);
                startActivity(intent);
                finish();
                return true;
            }
        }
        return false;
    }
}