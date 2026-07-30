package com.example.gym_management_and_fitness_tracking_system;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    public interface OnMemberActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<Member> masterList;
    private List<Member> filteredList;
    private final OnMemberActionListener listener;

    public MemberAdapter(List<Member> members, OnMemberActionListener listener) {
        this.masterList = members;
        this.filteredList = new ArrayList<>(members);
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = filteredList.get(position);
        holder.tvInitials.setText(member.getInitials());
        holder.tvName.setText(member.name);

        // Show plan name and status (Pending / Active / No Package)
        String planLabel;
        int planColor;

        if ("Pending".equalsIgnoreCase(member.planStatus)) {
            String requested = (member.pendingPlan != null && !member.pendingPlan.isEmpty()) ? member.pendingPlan : "Plan";
            if (member.plan != null && !member.plan.isEmpty() && !member.plan.equals("No Package") && !member.plan.equals("None")) {
                planLabel = "Active: " + member.plan + "  •  Pending: " + requested;
            } else {
                planLabel = "Pending Application: " + requested;
            }
            planColor = 0xFFFF9500; // orange
        } else if (member.plan != null && !member.plan.isEmpty() && !member.plan.equals("No Package") && !member.plan.equals("None")) {
            planLabel = "Active Plan: " + member.plan;
            planColor = 0xFF34C759; // accent_green
        } else {
            planLabel = "No Package Assigned";
            planColor = 0xFFAAAAAA; // gray
        }

        holder.tvPlan.setText(planLabel);
        holder.tvPlan.setTextColor(planColor);

        holder.btnEdit.setOnClickListener(v -> {
            int realIndex = masterList.indexOf(member);
            if (realIndex >= 0) listener.onEdit(realIndex);
        });
        holder.btnDelete.setOnClickListener(v -> {
            int realIndex = masterList.indexOf(member);
            if (realIndex >= 0) listener.onDelete(realIndex);
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(masterList);
        } else {
            String lower = query.toLowerCase();
            for (Member m : masterList) {
                if (m == null) continue;
                String name = m.name != null ? m.name.toLowerCase() : "";
                String email = m.email != null ? m.email.toLowerCase() : "";
                String plan = m.plan != null ? m.plan.toLowerCase() : "";
                String pendingPlan = m.pendingPlan != null ? m.pendingPlan.toLowerCase() : "";

                if (name.contains(lower) || email.contains(lower) || plan.contains(lower) || pendingPlan.contains(lower)) {
                    filteredList.add(m);
                }
            }
        }
        notifyDataSetChanged();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvPlan;
        ImageView btnEdit, btnDelete;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tv_member_initials);
            tvName = itemView.findViewById(R.id.tv_member_name);
            tvPlan = itemView.findViewById(R.id.tv_member_plan);
            btnEdit = itemView.findViewById(R.id.btn_edit_member);
            btnDelete = itemView.findViewById(R.id.btn_delete_member);
        }
    }
}
