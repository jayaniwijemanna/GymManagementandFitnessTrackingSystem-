package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ManageTrainersActivity extends AppCompatActivity {

    private final List<Trainer> trainerList = DataStore.getInstance().trainers;
    private TrainerAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvTrainers;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_trainers);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        layoutEmpty = findViewById(R.id.layout_empty);
        rvTrainers = findViewById(R.id.rv_trainers);

        adapter = new TrainerAdapter(trainerList, new TrainerAdapter.OnTrainerActionListener() {
            @Override
            public void onEdit(int position) {
                showEditTrainerBottomSheet(position);
            }
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= trainerList.size()) return;
                Trainer t = trainerList.get(position);
                if (t.id != null && !t.id.isEmpty()) {
                    db.collection("users").document(t.id).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(ManageTrainersActivity.this, t.name + " removed from database", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ManageTrainersActivity.this, "Error deleting trainer: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    trainerList.remove(position);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                    Toast.makeText(ManageTrainersActivity.this, t.name + " removed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rvTrainers.setLayoutManager(new LinearLayoutManager(this));
        rvTrainers.setAdapter(adapter);

        FloatingActionButton fabAddTrainer = findViewById(R.id.fab_add_trainer);
        fabAddTrainer.setOnClickListener(v -> showAddTrainerBottomSheet());

        listenToTrainersRealtime();
    }

    private void listenToTrainersRealtime() {
        db.collection("users").whereEqualTo("role", "trainer").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(ManageTrainersActivity.this, "Error loading trainers: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                trainerList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    try {
                        Trainer t = doc.toObject(Trainer.class);
                        if (t == null) {
                            t = new Trainer();
                        }
                        t.id = doc.getId();
                        if (t.name == null || t.name.isEmpty()) {
                            t.name = doc.getString("name");
                        }
                        if (t.specialization == null || t.specialization.isEmpty()) {
                            t.specialization = doc.getString("specialization");
                        }
                        if (t.name == null) t.name = "Trainer";
                        if (t.specialization == null) t.specialization = "General Fitness";

                        trainerList.add(t);
                    } catch (Exception ex) {
                        String name = doc.getString("name");
                        String spec = doc.getString("specialization");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        Trainer t = new Trainer(doc.getId(),
                                name != null ? name : "Trainer",
                                spec != null ? spec : "General Fitness",
                                phone != null ? phone : "",
                                email != null ? email : "",
                                "password");
                        trainerList.add(t);
                    }
                }
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (trainerList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTrainers.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTrainers.setVisibility(View.VISIBLE);
        }
    }

    private void showAddTrainerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_trainer, null);

        EditText etName = sheetView.findViewById(R.id.et_trainer_name);
        EditText etSpec = sheetView.findViewById(R.id.et_trainer_specialization);
        EditText etEmail = sheetView.findViewById(R.id.et_trainer_email);
        EditText etPhone = sheetView.findViewById(R.id.et_trainer_phone);
        EditText etPass = sheetView.findViewById(R.id.et_trainer_password);

        sheetView.findViewById(R.id.btn_save_trainer).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }
            String spec = etSpec.getText().toString().trim();
            if (spec.isEmpty()) spec = "General Fitness";

            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) email = name.toLowerCase().replace(" ", "") + "@gmail.com";

            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) phone = "555-0100";

            String pass = etPass.getText().toString().trim();
            if (pass.isEmpty()) pass = "password";

            DocumentReference newDoc = db.collection("users").document();
            Trainer newTrainer = new Trainer(newDoc.getId(), name, spec, phone, email, pass);

            newDoc.set(newTrainer)
                .addOnSuccessListener(aVoid -> Toast.makeText(ManageTrainersActivity.this, name + " added successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ManageTrainersActivity.this, "Failed to add trainer: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showEditTrainerBottomSheet(int position) {
        if (position < 0 || position >= trainerList.size()) return;
        Trainer trainer = trainerList.get(position);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_trainer, null);

        ((android.widget.TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("Edit Trainer");

        EditText etName = sheetView.findViewById(R.id.et_trainer_name);
        EditText etSpec = sheetView.findViewById(R.id.et_trainer_specialization);
        EditText etEmail = sheetView.findViewById(R.id.et_trainer_email);
        EditText etPhone = sheetView.findViewById(R.id.et_trainer_phone);
        EditText etPass = sheetView.findViewById(R.id.et_trainer_password);

        etName.setText(trainer.name != null ? trainer.name : "");
        etSpec.setText(trainer.specialization != null ? trainer.specialization : "");
        etEmail.setText(trainer.email != null ? trainer.email : "");
        etPhone.setText(trainer.phone != null ? trainer.phone : "");
        etPass.setText(trainer.password != null ? trainer.password : "");

        ((android.widget.Button) sheetView.findViewById(R.id.btn_save_trainer)).setText("Update Trainer");

        sheetView.findViewById(R.id.btn_save_trainer).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }

            trainer.name = name;
            trainer.specialization = etSpec.getText().toString().trim();
            trainer.email = etEmail.getText().toString().trim();
            trainer.phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (!pass.isEmpty()) trainer.password = pass;

            if (trainer.id != null && !trainer.id.isEmpty()) {
                db.collection("users").document(trainer.id).set(trainer)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ManageTrainersActivity.this, "Trainer updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ManageTrainersActivity.this, "Failed to update trainer: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                adapter.notifyItemChanged(position);
            }

            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
