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

public class ManagePackagesActivity extends AppCompatActivity {

    private final List<GymPackage> packageList = DataStore.getInstance().packages;
    private PackageAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvPackages;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_packages);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        layoutEmpty = findViewById(R.id.layout_empty);
        rvPackages = findViewById(R.id.rv_packages);

        adapter = new PackageAdapter(packageList, new PackageAdapter.OnPackageActionListener() {
            @Override
            public void onEdit(int position) {
                showEditPackageBottomSheet(position);
            }
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= packageList.size()) return;
                GymPackage pkg = packageList.get(position);
                if (pkg.id != null && !pkg.id.isEmpty()) {
                    db.collection("packages").document(pkg.id).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(ManagePackagesActivity.this, pkg.name + " deleted from database", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ManagePackagesActivity.this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    packageList.remove(position);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            }
        });

        rvPackages.setLayoutManager(new LinearLayoutManager(this));
        rvPackages.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_package);
        fabAdd.setOnClickListener(v -> showAddPackageBottomSheet());

        listenToPackagesRealtime();
    }

    private void listenToPackagesRealtime() {
        db.collection("packages").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(ManagePackagesActivity.this, "Error loading packages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                packageList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    GymPackage pkg = doc.toObject(GymPackage.class);
                    if (pkg != null) {
                        pkg.id = doc.getId();
                        packageList.add(pkg);
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
        if (packageList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvPackages.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvPackages.setVisibility(View.VISIBLE);
        }
    }

    private void showAddPackageBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_package, null);

        EditText etName = sheetView.findViewById(R.id.et_package_name);
        EditText etPrice = sheetView.findViewById(R.id.et_package_price);
        EditText etDesc = sheetView.findViewById(R.id.et_package_description);

        sheetView.findViewById(R.id.btn_save_package).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }
            String price = etPrice.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            DocumentReference newDocRef = db.collection("packages").document();
            GymPackage newPkg = new GymPackage(newDocRef.getId(), name, price.isEmpty() ? "0" : price, desc);

            newDocRef.set(newPkg)
                .addOnSuccessListener(aVoid -> Toast.makeText(ManagePackagesActivity.this, name + " added successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ManagePackagesActivity.this, "Failed to add package: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showEditPackageBottomSheet(int position) {
        if (position < 0 || position >= packageList.size()) return;
        GymPackage pkg = packageList.get(position);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_package, null);

        ((android.widget.TextView) sheetView.findViewById(R.id.tv_sheet_title)).setText("Edit Package");

        EditText etName = sheetView.findViewById(R.id.et_package_name);
        EditText etPrice = sheetView.findViewById(R.id.et_package_price);
        EditText etDesc = sheetView.findViewById(R.id.et_package_description);
        etName.setText(pkg.name);
        etPrice.setText(pkg.price);
        etDesc.setText(pkg.description);

        ((android.widget.Button) sheetView.findViewById(R.id.btn_save_package)).setText("Update Package");

        sheetView.findViewById(R.id.btn_save_package).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }
            pkg.name = name;
            pkg.price = etPrice.getText().toString().trim();
            pkg.description = etDesc.getText().toString().trim();

            if (pkg.id != null && !pkg.id.isEmpty()) {
                db.collection("packages").document(pkg.id).set(pkg)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ManagePackagesActivity.this, "Package updated!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ManagePackagesActivity.this, "Failed to update package: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                adapter.notifyItemChanged(position);
            }

            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
