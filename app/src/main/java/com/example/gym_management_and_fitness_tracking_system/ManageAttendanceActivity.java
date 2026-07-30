package com.example.gym_management_and_fitness_tracking_system;

import android.graphics.Bitmap;
import android.graphics.Color;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ManageAttendanceActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration attendanceListener;
    private LinearLayout layoutAttendanceList;
    private TextView tvAttendanceDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_attendance);

        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView btnShowGymQr = findViewById(R.id.btn_show_gym_qr);
        if (btnShowGymQr != null) {
            btnShowGymQr.setOnClickListener(v -> showGymQrDialog());
        }

        db = FirebaseFirestore.getInstance();
        layoutAttendanceList = findViewById(R.id.layout_attendance_list);
        tvAttendanceDate = findViewById(R.id.tv_attendance_date);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        if (tvAttendanceDate != null) {
            tvAttendanceDate.setText("Attendance Report - " + sdf.format(new Date()));
        }

        listenToAttendanceRealtime();
    }

    private void listenToAttendanceRealtime() {
        if (attendanceListener != null) attendanceListener.remove();

        attendanceListener = db.collection("attendance")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading attendance: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null && layoutAttendanceList != null) {
                        // Clear existing item views except header
                        if (layoutAttendanceList.getChildCount() > 1) {
                            layoutAttendanceList.removeViews(1, layoutAttendanceList.getChildCount() - 1);
                        }

                        if (value.getDocuments().isEmpty()) {
                            TextView emptyTv = new TextView(this);
                            emptyTv.setText("No check-in attendance records found for today.");
                            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
                            emptyTv.setTextSize(14);
                            emptyTv.setPadding(0, 20, 0, 0);
                            layoutAttendanceList.addView(emptyTv);
                            return;
                        }

                        LayoutInflater inflater = LayoutInflater.from(this);
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            View itemView = inflater.inflate(R.layout.item_member, layoutAttendanceList, false);

                            TextView tvInitials = itemView.findViewById(R.id.tv_member_initials);
                            TextView tvName = itemView.findViewById(R.id.tv_member_name);
                            TextView tvPlan = itemView.findViewById(R.id.tv_member_plan);
                            ImageView btnEdit = itemView.findViewById(R.id.btn_edit_member);
                            ImageView btnDelete = itemView.findViewById(R.id.btn_delete_member);

                            btnEdit.setVisibility(View.GONE);
                            btnDelete.setVisibility(View.GONE);

                            String mName = doc.getString("memberName");
                            String cTime = doc.getString("checkInTime");
                            String dStr = doc.getString("date");
                            Long bCount = doc.getLong("matchedBookingsCount");
                            int bCountInt = bCount != null ? bCount.intValue() : 0;

                            String initials = "?";
                            if (mName != null && !mName.isEmpty()) {
                                String[] parts = mName.trim().split("\\s+");
                                if (parts.length == 1) initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
                                else initials = (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
                            }

                            tvInitials.setText(initials);
                            tvName.setText((mName != null ? mName : "Member") + "  |  Check-in: " + (cTime != null ? cTime : "N/A"));

                            String bInfo = bCountInt > 0 ? " • " + bCountInt + " booking(s) marked Attended" : "";
                            tvPlan.setText("Status: Present (" + (dStr != null ? dStr : "") + ")" + bInfo);
                            tvPlan.setTextColor(Color.parseColor("#34C759"));

                            layoutAttendanceList.addView(itemView);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceListener != null) attendanceListener.remove();
    }

    private void showGymQrDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 24, 40, 40);
        root.setBackgroundResource(R.drawable.bg_bottom_sheet);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        View handle = new View(this);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(100, 10);
        handleLp.setMargins(0, 0, 0, 30);
        handle.setLayoutParams(handleLp);
        handle.setBackgroundResource(R.drawable.bg_input_default);
        root.addView(handle);

        TextView title = new TextView(this);
        title.setText("TITAN GYM ENTRANCE QR CODE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Display on desk or screen. Members scan this QR to check in.");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(13);
        sub.setGravity(android.view.Gravity.CENTER);
        sub.setPadding(0, 6, 0, 24);
        root.addView(sub);

        ImageView imgQr = new ImageView(this);
        int qrSize = spToPx(240);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(qrSize, qrSize);
        imgQr.setLayoutParams(imgLp);

        Bitmap qrBmp = QrGenerator.generateQrBitmap("TITAN_GYM_ATTENDANCE_CHECKIN_ENTRANCE_QR_2026", 500);
        imgQr.setImageBitmap(qrBmp);
        root.addView(imgQr);

        TextView codeText = new TextView(this);
        codeText.setText("Code: TITAN-ENTRANCE-2026");
        codeText.setTextColor(Color.parseColor("#34C759"));
        codeText.setTextSize(13);
        codeText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        codeText.setPadding(0, 20, 0, 0);
        root.addView(codeText);

        dialog.setContentView(root);
        dialog.show();
    }

    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
