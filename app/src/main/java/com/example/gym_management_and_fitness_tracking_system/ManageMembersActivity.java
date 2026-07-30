package com.example.gym_management_and_fitness_tracking_system;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ManageMembersActivity extends AppCompatActivity {

    private final List<Member> memberList = DataStore.getInstance().members;
    private final List<GymPackage> packageList = DataStore.getInstance().packages;
    private MemberAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvMembers;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_members);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        layoutEmpty = findViewById(R.id.layout_empty);
        rvMembers = findViewById(R.id.rv_members);

        adapter = new MemberAdapter(memberList, new MemberAdapter.OnMemberActionListener() {
            @Override
            public void onEdit(int position) {
                showMemberBottomSheet(position);
            }
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= memberList.size()) return;
                Member m = memberList.get(position);
                if (m.id != null && !m.id.isEmpty()) {
                    db.collection("users").document(m.id).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(ManageMembersActivity.this, m.name + " deleted from database", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ManageMembersActivity.this, "Error deleting member: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    memberList.remove(position);
                    adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                    updateEmptyState();
                    Toast.makeText(ManageMembersActivity.this, m.name + " deleted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        rvMembers.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.et_search_members);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        FloatingActionButton fabAdd = findViewById(R.id.fab_add_member);
        fabAdd.setOnClickListener(v -> showMemberBottomSheet(-1));

        listenToPackagesRealtime();
        listenToMembersRealtime();
    }

    private void listenToPackagesRealtime() {
        db.collection("packages").addSnapshotListener((value, error) -> {
            if (value != null) {
                packageList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    GymPackage pkg = doc.toObject(GymPackage.class);
                    if (pkg != null) {
                        pkg.id = doc.getId();
                        packageList.add(pkg);
                    }
                }
            }
        });
    }

    private void listenToMembersRealtime() {
        db.collection("users").whereEqualTo("role", "member").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(ManageMembersActivity.this, "Error loading members: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                memberList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Member m = doc.toObject(Member.class);
                    if (m != null) {
                        m.id = doc.getId();
                        memberList.add(m);
                    }
                }
                adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                updateEmptyState();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (memberList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvMembers.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvMembers.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param editPosition -1 = Add mode, >=0 = Edit mode for that index
     */
    private void showMemberBottomSheet(int editPosition) {
        boolean isEdit = editPosition >= 0;
        Member existing = isEdit ? memberList.get(editPosition) : null;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_member, null);

        TextView tvTitle = sheetView.findViewById(R.id.tv_sheet_title);
        EditText etName = sheetView.findViewById(R.id.et_member_name);
        EditText etEmail = sheetView.findViewById(R.id.et_member_email);
        EditText etPhone = sheetView.findViewById(R.id.et_member_phone);
        EditText etPass = sheetView.findViewById(R.id.et_member_password);
        Button btnSave = sheetView.findViewById(R.id.btn_save_member);
        LinearLayout chipsContainer = sheetView.findViewById(R.id.package_chips_container);
        TextView tvNoPackagesHint = sheetView.findViewById(R.id.tv_no_packages_hint);

        tvTitle.setText(isEdit ? "Edit Member" : "Add Member");
        btnSave.setText(isEdit ? "Update Member" : "Save Member");

        if (isEdit && existing != null) {
            etName.setText(existing.name != null ? existing.name : "");
            etEmail.setText(existing.email != null ? existing.email : "");
            etPhone.setText(existing.phone != null ? existing.phone : "");
            etPass.setText(existing.password != null ? existing.password : "");
        }

        // Track selected package
        final String[] selectedPlan = {(isEdit && existing != null && existing.plan != null) ? existing.plan : "No Package"};

        // "No Package" row is already in XML; wire up its click
        LinearLayout chipNoPackage = sheetView.findViewById(R.id.chip_no_package);
        ImageView checkNoPackage = sheetView.findViewById(R.id.check_no_package);
        // Keep track of all check icons so we can uncheck others
        final ImageView[] allChecks = new ImageView[packageList.size() + 1];
        allChecks[0] = checkNoPackage;

        // Set initial selection for "No Package"
        checkNoPackage.setVisibility("No Package".equals(selectedPlan[0]) ? View.VISIBLE : View.GONE);

        chipNoPackage.setOnClickListener(v -> {
            selectedPlan[0] = "No Package";
            for (ImageView check : allChecks) if (check != null) check.setVisibility(View.GONE);
            checkNoPackage.setVisibility(View.VISIBLE);
        });

        // Dynamically add a row per package
        if (packageList.isEmpty()) {
            tvNoPackagesHint.setVisibility(View.VISIBLE);
        } else {
            tvNoPackagesHint.setVisibility(View.GONE);
            for (int i = 0; i < packageList.size(); i++) {
                GymPackage pkg = packageList.get(i);
                View pkgRow = LayoutInflater.from(this).inflate(R.layout.item_package_chip, chipsContainer, false);

                TextView tvPkgName = pkgRow.findViewById(R.id.tv_chip_name);
                TextView tvPkgPrice = pkgRow.findViewById(R.id.tv_chip_price);
                ImageView checkPkg = pkgRow.findViewById(R.id.check_chip);

                tvPkgName.setText(pkg.name);
                tvPkgPrice.setText("$" + pkg.price + "/mo");

                // Mark selected if this package matches the existing plan
                boolean isSelected = pkg.name.equals(selectedPlan[0]);
                checkPkg.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                if (isSelected) checkNoPackage.setVisibility(View.GONE);

                allChecks[i + 1] = checkPkg;

                final String pkgName = pkg.name;
                pkgRow.setOnClickListener(v -> {
                    selectedPlan[0] = pkgName;
                    for (ImageView check : allChecks) if (check != null) check.setVisibility(View.GONE);
                    checkPkg.setVisibility(View.VISIBLE);
                });

                chipsContainer.addView(pkgRow);
            }
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Name is required"); return; }
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (pass.isEmpty()) pass = "password";
            String plan = selectedPlan[0];

            if (isEdit && existing != null) {
                existing.name = name;
                existing.email = email;
                existing.phone = phone;
                existing.password = pass;
                existing.plan = plan;

                if (existing.id != null && !existing.id.isEmpty()) {
                    db.collection("users").document(existing.id).set(existing)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ManageMembersActivity.this, "Member updated!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ManageMembersActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                    Toast.makeText(this, "Member updated!", Toast.LENGTH_SHORT).show();
                }
            } else {
                DocumentReference newDoc = db.collection("users").document();
                Member newMember = new Member(newDoc.getId(), name, email, phone, plan, pass);

                newDoc.set(newMember)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ManageMembersActivity.this, name + " added successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ManageMembersActivity.this, "Failed to add member: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
