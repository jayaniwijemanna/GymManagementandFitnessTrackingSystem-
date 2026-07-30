package com.example.gym_management_and_fitness_tracking_system;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {

    public interface OnPackageActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<GymPackage> packages;
    private final OnPackageActionListener listener;

    public PackageAdapter(List<GymPackage> packages, OnPackageActionListener listener) {
        this.packages = packages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        GymPackage pkg = packages.get(position);
        holder.tvName.setText(pkg.name);
        holder.tvPrice.setText("$" + pkg.price + " / Month");
        holder.tvDescription.setText(pkg.description.isEmpty() ? "No description" : pkg.description);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(position));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(position));
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    public static class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvDescription;
        ImageView btnEdit, btnDelete;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_package_name);
            tvPrice = itemView.findViewById(R.id.tv_package_price);
            tvDescription = itemView.findViewById(R.id.tv_package_description);
            btnEdit = itemView.findViewById(R.id.btn_edit_package);
            btnDelete = itemView.findViewById(R.id.btn_delete_package);
        }
    }
}
