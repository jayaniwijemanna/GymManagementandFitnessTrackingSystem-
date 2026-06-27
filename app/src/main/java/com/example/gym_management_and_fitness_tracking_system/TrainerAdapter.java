package com.example.gym_management_and_fitness_tracking_system;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrainerAdapter extends RecyclerView.Adapter<TrainerAdapter.TrainerViewHolder> {

    public interface OnTrainerActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<Trainer> trainers;
    private final OnTrainerActionListener listener;

    public TrainerAdapter(List<Trainer> trainers, OnTrainerActionListener listener) {
        this.trainers = trainers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrainerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trainer, parent, false);
        return new TrainerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrainerViewHolder holder, int position) {
        Trainer trainer = trainers.get(position);
        holder.tvInitials.setText(trainer.getInitials());
        holder.tvName.setText(trainer.name);
        holder.tvSpec.setText(trainer.specialization.isEmpty() ? "General Trainer" : trainer.specialization);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(position));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(position));
    }

    @Override
    public int getItemCount() {
        return trainers.size();
    }

    public static class TrainerViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvSpec;
        ImageView btnEdit, btnDelete;

        public TrainerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tv_trainer_initials);
            tvName = itemView.findViewById(R.id.tv_trainer_name);
            tvSpec = itemView.findViewById(R.id.tv_trainer_spec);
            btnEdit = itemView.findViewById(R.id.btn_edit_trainer);
            btnDelete = itemView.findViewById(R.id.btn_delete_trainer);
        }
    }
}
