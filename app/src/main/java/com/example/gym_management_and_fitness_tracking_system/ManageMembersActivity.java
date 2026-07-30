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

import java.util.List;

public class ManageMembersActivity extends AppCompatActivity {

    private final List<Member> memberList = DataStore.getInstance().members;
    private final List<GymPackage> packageList = DataStore.getInstance().packages;
    private MemberAdapter adapter;
    private LinearLayout layoutEmpty;
    private RecyclerView rvMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_members);

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
                String name = memberList.get(position).name;
                memberList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, memberList.size());
                adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                updateEmptyState();
                Toast.makeText(ManageMembersActivity.this, name + " deleted", Toast.LENGTH_SHORT).show();
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

        updateEmptyState();
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
        Button btnSave = sheetView.findViewById(R.id.btn_save_member);
        LinearLayout chipsContainer = sheetView.findViewById(R.id.package_chips_container);
        TextView tvNoPackagesHint = sheetView.findViewById(R.id.tv_no_packages_hint);

        tvTitle.setText(isEdit ? "Edit Member" : "Add Member");
        btnSave.setText(isEdit ? "Update Member" : "Save Member");

        if (isEdit) {
            etName.setText(existing.name);
            etEmail.setText(existing.email);
            etPhone.setText(existing.phone);
        }

        // Track selected package
        final String[] selectedPlan = {isEdit ? existing.plan : "No Package"};

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
            String plan = selectedPlan[0];

            if (isEdit) {
                existing.name = name;
                existing.email = email;
                existing.phone = phone;
                existing.plan = plan;
                adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                Toast.makeText(this, "Member updated!", Toast.LENGTH_SHORT).show();
            } else {
                memberList.add(new Member(name, email, phone, plan));
                adapter.filter(((EditText) findViewById(R.id.et_search_members)).getText().toString());
                updateEmptyState();
                Toast.makeText(this, name + " added successfully!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }
}
