package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SendNotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_notifications);

        ViewCompat.setOnApplyWindowInsetsListener((android.view.View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());
        
        Spinner spinnerAudience = findViewById(R.id.spinner_audience);
        String[] audiences = new String[]{"All Members", "Active Members", "Expired Members", "Trainers"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, audiences);
        spinnerAudience.setAdapter(adapter);
        
        Button btnSend = findViewById(R.id.btn_send_notification);
        btnSend.setOnClickListener(v -> Toast.makeText(this, "Notification Sent Successfully", Toast.LENGTH_SHORT).show());
    }
}
