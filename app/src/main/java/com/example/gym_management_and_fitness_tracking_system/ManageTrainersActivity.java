package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.view.View;
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

import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class ManageTrainersActivity extends AppCompatActivity {

    private final List<Trainer> trainerList = DataStore.getInstance().trainers;
    private TrainerAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvTrainers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_trainers);

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
                String name = trainerList.get(position).name;
                trainerList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, trainerList.size());
                updateEmptyState();
                Toast.makeText(ManageTrainersActivity.this, name + " removed", Toast.LENGTH_SHORT).show();
            }
        });

        rvTrainers.setLayoutManager(new LinearLayoutManager(this));
        rvTrainers.setAdapter(adapter);

        FloatingActionButton fabAddTrainer = findViewById(R.id.fab_add_trainer);
        fabAddTrainer.setOnClickListener(v -> showAddTrainerBottomSheet());

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

        sheetView.findViewById(R.id.btn_save_trainer).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            String spec = etSpec.getText().toString().trim();
            trainerList.add(new Trainer(name, spec, ""));
            adapter.notifyItemInserted(trainerList.size() - 1);
            updateEmptyState();
            Toast.makeText(this, name + " added successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showEditTrainerBottomSheet(int position) {
        Trainer trainer = trainerList.get(position);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_trainer, null);

        ((android.widget.TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("Edit Trainer");

        EditText etName = sheetView.findViewById(R.id.et_trainer_name);
        EditText etSpec = sheetView.findViewById(R.id.et_trainer_specialization);
        etName.setText(trainer.name);
        etSpec.setText(trainer.specialization);

        ((android.widget.Button) sheetView.findViewById(R.id.btn_save_trainer)).setText("Update Trainer");

        sheetView.findViewById(R.id.btn_save_trainer).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }
            trainer.name = name;
            trainer.specialization = etSpec.getText().toString().trim();
            adapter.notifyItemChanged(position);
            Toast.makeText(this, "Trainer updated!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
