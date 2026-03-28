package com.example.antyspamer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GuardianAdapter extends RecyclerView.Adapter<GuardianAdapter.ViewHolder> {

    private List<Guardian> guardianList;
    private OnGuardianDeleteListener deleteListener;

    public interface OnGuardianDeleteListener {
        void onDelete(Guardian guardian);
    }

    public GuardianAdapter(List<Guardian> guardianList, OnGuardianDeleteListener deleteListener) {
        this.guardianList = guardianList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guardian, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Guardian guardian = guardianList.get(position);
        holder.name.setText(guardian.getName());
        holder.phone.setText(guardian.getPhone());
        holder.deleteBtn.setOnClickListener(v -> deleteListener.onDelete(guardian));
    }

    @Override
    public int getItemCount() {
        return guardianList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, phone;
        ImageButton deleteBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.guardianName);
            phone = itemView.findViewById(R.id.guardianPhone);
            deleteBtn = itemView.findViewById(R.id.deleteGuardianBtn);
        }
    }
}
