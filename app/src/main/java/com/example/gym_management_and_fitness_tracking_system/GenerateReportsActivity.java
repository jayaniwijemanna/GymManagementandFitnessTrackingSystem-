package com.example.gym_management_and_fitness_tracking_system;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GenerateReportsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private View panelReportDisplay;
    private TextView tvReportTitle, tvReportDate;
    private LinearLayout containerReportDetails;
    private Button btnExportReport, btnDownloadReport;

    private String currentReportSummaryText = "";
    private String currentReportTitleText = "Gym_Report";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generate_reports);

        ViewCompat.setOnApplyWindowInsetsListener((View) findViewById(R.id.btn_back).getParent().getParent(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        panelReportDisplay     = findViewById(R.id.panel_report_display);
        tvReportTitle          = findViewById(R.id.tv_report_title);
        tvReportDate           = findViewById(R.id.tv_report_date);
        containerReportDetails = findViewById(R.id.container_report_details);
        btnExportReport        = findViewById(R.id.btn_export_report);
        btnDownloadReport      = findViewById(R.id.btn_download_report);

        findViewById(R.id.btn_report_income).setOnClickListener(v -> generateIncomeReport());
        findViewById(R.id.btn_report_member).setOnClickListener(v -> generateMemberReport());
        findViewById(R.id.btn_report_trainer).setOnClickListener(v -> generateTrainerReport());
        findViewById(R.id.btn_report_attendance).setOnClickListener(v -> generateAttendanceReport());

        btnExportReport.setOnClickListener(v -> exportReportSummary());
        btnDownloadReport.setOnClickListener(v -> downloadReportFile());
    }

    // ── 1. Income Report Generator ──────────────────────────────────────────
    private void generateIncomeReport() {
        currentReportTitleText = "Income_Report";
        showLoadingReport("Income & Revenue Report");

        db.collection("packages").get().addOnSuccessListener(pkgSnap -> {
            Map<String, Double> prices = new HashMap<>();
            for (DocumentSnapshot doc : pkgSnap.getDocuments()) {
                String name = doc.getString("name");
                String priceStr = doc.getString("price");
                if (name != null && priceStr != null) {
                    try {
                        double p = Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));
                        prices.put(name.trim().toLowerCase(), p);
                    } catch (Exception ignored) {}
                }
            }

            db.collection("users").whereEqualTo("role", "member").get().addOnSuccessListener(memberSnap -> {
                containerReportDetails.removeAllViews();
                double totalIncome = 0;
                Map<String, Integer> planCountMap = new HashMap<>();

                for (DocumentSnapshot doc : memberSnap.getDocuments()) {
                    String plan = doc.getString("plan");
                    if (plan != null && !plan.isEmpty() && !"None".equalsIgnoreCase(plan)) {
                        planCountMap.put(plan, planCountMap.getOrDefault(plan, 0) + 1);
                        Double price = prices.get(plan.trim().toLowerCase());
                        if (price != null) totalIncome += price;
                    }
                }

                StringBuilder exportSb = new StringBuilder();
                exportSb.append("=== GYM MANAGEMENT INCOME REPORT ===\n");
                exportSb.append("Date: ").append(tvReportDate.getText()).append("\n");
                exportSb.append("Total Estimated Monthly Revenue: $").append(String.format(Locale.US, "%.2f", totalIncome)).append("\n");
                exportSb.append("Total Members Queried: ").append(memberSnap.size()).append("\n\n");

                addMetricRow("Est. Monthly Revenue", String.format(Locale.US, "$%.2f", totalIncome), "Total from active member plans");
                addMetricRow("Active Subscribers", String.valueOf(planCountMap.values().stream().mapToInt(Integer::intValue).sum()), "Members with an active gym plan");

                for (Map.Entry<String, Integer> entry : planCountMap.entrySet()) {
                    String planName = entry.getKey();
                    int count = entry.getValue();
                    Double price = prices.get(planName.trim().toLowerCase());
                    double subtotal = (price != null ? price : 0.0) * count;

                    String detail = count + " member(s) × " + (price != null ? String.format(Locale.US, "$%.2f", price) : "N/A");
                    addMetricRow(planName + " Revenue", String.format(Locale.US, "$%.2f", subtotal), detail);

                    exportSb.append("• ").append(planName).append(": ").append(detail).append(" = $").append(String.format(Locale.US, "%.2f", subtotal)).append("\n");
                }

                currentReportSummaryText = exportSb.toString();
                panelReportDisplay.setVisibility(View.VISIBLE);
            });
        });
    }

    // ── 2. Member Report Generator ──────────────────────────────────────────
    private void generateMemberReport() {
        currentReportTitleText = "Member_Report";
        showLoadingReport("Member Statistics Report");

        db.collection("users").whereEqualTo("role", "member").get().addOnSuccessListener(snap -> {
            containerReportDetails.removeAllViews();
            int totalMembers = snap.size();
            int subscribedCount = 0;
            int pendingCount = 0;
            Map<String, Integer> planDist = new HashMap<>();

            for (DocumentSnapshot doc : snap.getDocuments()) {
                String plan = doc.getString("plan");
                String status = doc.getString("planStatus");
                if ("Pending".equalsIgnoreCase(status)) {
                    pendingCount++;
                }
                if (plan != null && !plan.isEmpty() && !"None".equalsIgnoreCase(plan)) {
                    subscribedCount++;
                    planDist.put(plan, planDist.getOrDefault(plan, 0) + 1);
                }
            }

            StringBuilder exportSb = new StringBuilder();
            exportSb.append("=== GYM MEMBER REPORT ===\n");
            exportSb.append("Date: ").append(tvReportDate.getText()).append("\n");
            exportSb.append("Total Registered Members: ").append(totalMembers).append("\n");
            exportSb.append("Active Plan Subscribers: ").append(subscribedCount).append("\n");
            exportSb.append("Pending Plan Requests: ").append(pendingCount).append("\n\n");

            addMetricRow("Total Registered Members", String.valueOf(totalMembers), "All time registered member accounts");
            addMetricRow("Active Subscriptions", String.valueOf(subscribedCount), "Members currently enrolled in a plan");
            addMetricRow("Pending Plan Requests", String.valueOf(pendingCount), "Requests awaiting admin review");

            for (Map.Entry<String, Integer> entry : planDist.entrySet()) {
                addMetricRow(entry.getKey() + " Distribution", entry.getValue() + " Members", "Enrolled subscribers");
                exportSb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" members\n");
            }

            currentReportSummaryText = exportSb.toString();
            panelReportDisplay.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── 3. Trainer Report Generator ─────────────────────────────────────────
    private void generateTrainerReport() {
        currentReportTitleText = "Trainer_Report";
        showLoadingReport("Trainer Performance & Status Report");

        db.collection("users").whereEqualTo("role", "trainer").get().addOnSuccessListener(snap -> {
            containerReportDetails.removeAllViews();
            int totalTrainers = snap.size();

            StringBuilder exportSb = new StringBuilder();
            exportSb.append("=== GYM TRAINER REPORT ===\n");
            exportSb.append("Date: ").append(tvReportDate.getText()).append("\n");
            exportSb.append("Total Active Trainers: ").append(totalTrainers).append("\n\n");

            addMetricRow("Active Trainers", String.valueOf(totalTrainers), "Total registered gym trainers");

            for (DocumentSnapshot doc : snap.getDocuments()) {
                String name = doc.getString("name");
                String spec = doc.getString("specialization");
                String phone = doc.getString("phone");
                String exp = doc.getString("experience");

                if (name == null) name = "Trainer";
                if (spec == null || spec.isEmpty()) spec = "General Fitness";
                if (exp == null) exp = "1 Year";

                String subtext = "Spec: " + spec + " | Exp: " + exp + (phone != null ? " | Phone: " + phone : "");
                addMetricRow(name, spec, subtext);

                exportSb.append("• ").append(name).append(" (").append(spec).append(") - ").append(exp).append(" exp\n");
            }

            currentReportSummaryText = exportSb.toString();
            panelReportDisplay.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── 4. Attendance Report Generator ──────────────────────────────────────
    private void generateAttendanceReport() {
        currentReportTitleText = "Attendance_Report";
        showLoadingReport("Daily Attendance Report");

        db.collection("users").whereEqualTo("role", "member").get().addOnSuccessListener(snap -> {
            containerReportDetails.removeAllViews();
            int totalMembers = snap.size();
            int checkedInCount = 0;

            List<DocumentSnapshot> checkedInDocs = new ArrayList<>();

            for (DocumentSnapshot doc : snap.getDocuments()) {
                String checkedIn = doc.getString("checkedInTime");
                if (checkedIn != null && !checkedIn.isEmpty() && !"Not Checked In".equalsIgnoreCase(checkedIn)) {
                    checkedInCount++;
                    checkedInDocs.add(doc);
                }
            }

            StringBuilder exportSb = new StringBuilder();
            exportSb.append("=== GYM ATTENDANCE REPORT ===\n");
            exportSb.append("Date: ").append(tvReportDate.getText()).append("\n");
            exportSb.append("Total Checked-In Members Today: ").append(checkedInCount).append(" / ").append(totalMembers).append("\n\n");

            addMetricRow("Today's Checked-In Members", String.valueOf(checkedInCount), "Currently present in gym facility");
            addMetricRow("Facility Capacity Usage", (totalMembers > 0 ? (checkedInCount * 100 / totalMembers) : 0) + "%", checkedInCount + " out of " + totalMembers + " members");

            for (DocumentSnapshot doc : checkedInDocs) {
                String name = doc.getString("name");
                String plan = doc.getString("plan");
                String time = doc.getString("checkedInTime");
                if (name == null) name = "Member";

                addMetricRow(name, time != null ? time : "Checked In", "Plan: " + (plan != null ? plan : "General"));
                exportSb.append("• ").append(name).append(" - Checked in at ").append(time).append("\n");
            }

            currentReportSummaryText = exportSb.toString();
            panelReportDisplay.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Helper class to hold report metric rows for PDF rendering ─────────
    private static class ReportMetric {
        String title, value, subtext;
        ReportMetric(String title, String value, String subtext) {
            this.title = title;
            this.value = value;
            this.subtext = subtext;
        }
    }

    private final List<ReportMetric> currentReportMetrics = new ArrayList<>();

    // ── Native PDF Document Generator ────────────────────────────────────────
    private void downloadReportFile() {
        if (currentReportSummaryText.isEmpty()) {
            Toast.makeText(this, "No report data available to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = currentReportTitleText + "_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = null;

        try {
            // 1. Create PDF Document (A4 size: 595 x 842 pt)
            android.graphics.pdf.PdfDocument pdfDocument = new android.graphics.pdf.PdfDocument();
            android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                    new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
            android.graphics.pdf.PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            android.graphics.Canvas canvas = page.getCanvas();
            android.graphics.Paint paint = new android.graphics.Paint();

            // Background canvas
            canvas.drawColor(android.graphics.Color.parseColor("#121212"));

            // Header Banner Background
            paint.setColor(android.graphics.Color.parseColor("#1C1C1E"));
            canvas.drawRect(0, 0, 595, 100, paint);

            // Title Header Text
            paint.setColor(android.graphics.Color.WHITE);
            paint.setTextSize(20);
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
            canvas.drawText("GYM MANAGEMENT SYSTEM", 30, 42, paint);

            // Subtitle Report Type
            paint.setColor(android.graphics.Color.parseColor("#FF9500"));
            paint.setTextSize(15);
            canvas.drawText(tvReportTitle.getText().toString().toUpperCase(), 30, 68, paint);

            // Date String Right Aligned
            paint.setColor(android.graphics.Color.parseColor("#8E8E93"));
            paint.setTextSize(11);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            canvas.drawText(tvReportDate.getText().toString(), 30, 86, paint);

            // Divider line
            paint.setColor(android.graphics.Color.parseColor("#3A3A3C"));
            paint.setStrokeWidth(2);
            canvas.drawLine(30, 115, 565, 115, paint);

            // Render Metrics Rows
            int yPos = 145;
            paint.setStrokeWidth(0);

            for (ReportMetric metric : currentReportMetrics) {
                if (yPos > 760) break; // stay within single page bounds

                // Metric Card Box
                paint.setColor(android.graphics.Color.parseColor("#1C1C1E"));
                canvas.drawRoundRect(new android.graphics.RectF(30, yPos - 15, 565, yPos + 40), 10, 10, paint);

                // Metric Title
                paint.setColor(android.graphics.Color.WHITE);
                paint.setTextSize(13);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                canvas.drawText(metric.title, 45, yPos + 5, paint);

                // Metric Subtext
                paint.setColor(android.graphics.Color.parseColor("#8E8E93"));
                paint.setTextSize(11);
                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                canvas.drawText(metric.subtext, 45, yPos + 25, paint);

                // Metric Value Right-Aligned
                paint.setColor(android.graphics.Color.parseColor("#34C759"));
                paint.setTextSize(14);
                paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                paint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                canvas.drawText(metric.value, 550, yPos + 15, paint);
                paint.setTextAlign(android.graphics.Paint.Align.LEFT);

                yPos += 68;
            }

            // Footer Text
            paint.setColor(android.graphics.Color.parseColor("#636366"));
            paint.setTextSize(10);
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);
            canvas.drawText("Generated by Gym Management & Fitness Tracking System", 595 / 2f, 810, paint);

            pdfDocument.finishPage(page);

            // 2. Save PDF file to App External Files Directory
            File appDownloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (appDownloadsDir == null) appDownloadsDir = getFilesDir();
            pdfFile = new File(appDownloadsDir, fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.flush();
            fos.close();

            // 3. Save to Public MediaStore Downloads (Android Q+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            java.io.FileInputStream fis = new java.io.FileInputStream(pdfFile);
                            byte[] buffer = new byte[4096];
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                os.write(buffer, 0, length);
                            }
                            os.flush();
                            os.close();
                            fis.close();
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Close pdfDocument ONLY after all writing operations are finished
            pdfDocument.close();

            // Update UI with PDF path
            TextView tvPath = findViewById(R.id.tv_download_path);
            if (tvPath != null) {
                tvPath.setText("📄 PDF Saved: " + pdfFile.getAbsolutePath());
                tvPath.setVisibility(View.VISIBLE);
            }

            Toast.makeText(this, "✅ PDF Report Generated & Saved!", Toast.LENGTH_LONG).show();

            // Prompt user to open/share PDF
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("📄 PDF Generated Successfully")
                    .setMessage("File saved to:\n" + pdfFile.getAbsolutePath())
                    .setPositiveButton("Share PDF", (dialog, which) -> exportReportSummary())
                    .setNegativeButton("OK", null)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ── Share Report Logic ───────────────────────────────────────────────────
    private void exportReportSummary() {
        if (currentReportSummaryText.isEmpty()) {
            Toast.makeText(this, "No report data available to share.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, tvReportTitle.getText().toString());
        shareIntent.putExtra(Intent.EXTRA_TEXT, currentReportSummaryText);

        startActivity(Intent.createChooser(shareIntent, "Share Gym Report Summary"));
    }

    // ── Helper UI Methods ───────────────────────────────────────────────────
    private void showLoadingReport(String title) {
        tvReportTitle.setText(title);
        tvReportDate.setText(new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US).format(new Date()));
        containerReportDetails.removeAllViews();
        currentReportMetrics.clear();

        TextView tvLoading = new TextView(this);
        tvLoading.setText("Generating report data from Firestore...");
        tvLoading.setTextColor(getColor(R.color.text_gray));
        tvLoading.setPadding(0, 16, 0, 16);
        containerReportDetails.addView(tvLoading);

        panelReportDisplay.setVisibility(View.VISIBLE);
    }

    private void addMetricRow(String title, String value, String subtext) {
        currentReportMetrics.add(new ReportMetric(title, value, subtext));

        View v = LayoutInflater.from(this).inflate(R.layout.item_pending_request, containerReportDetails, false);
        v.findViewById(R.id.btn_approve).setVisibility(View.GONE);
        v.findViewById(R.id.btn_reject).setVisibility(View.GONE);

        TextView tvInitials = v.findViewById(R.id.tv_member_initials);
        TextView tvName     = v.findViewById(R.id.tv_member_name);
        TextView tvEmail    = v.findViewById(R.id.tv_member_email);
        TextView tvStatus   = v.findViewById(R.id.tv_status_badge);
        TextView tvPlan     = v.findViewById(R.id.tv_requested_plan);
        TextView tvPrice    = v.findViewById(R.id.tv_plan_price);

        tvInitials.setText("RP");
        tvName.setText(title);
        tvEmail.setText(subtext);

        tvStatus.setText("REPORT");
        tvStatus.setTextColor(0xFF007AFF);

        tvPlan.setText("Value");
        tvPrice.setText(value);

        containerReportDetails.addView(v);
    }
}


