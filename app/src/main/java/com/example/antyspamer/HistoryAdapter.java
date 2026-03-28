package com.example.antyspamer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<AlertItem> historyList;

    public HistoryAdapter(List<AlertItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertItem item = historyList.get(position);
        holder.keyword.setText("Słowo: " + item.getKeyword());
        holder.timestamp.setText(item.getTimestamp());
        holder.context.setText(item.getContext());
        holder.status.setText(item.getStatus());

        switch (item.getStatus()) {
            case "CONFIRMED":
                holder.status.setBackgroundColor(Color.parseColor("#FFCDD2")); // Jasny czerwony
                holder.status.setTextColor(Color.RED);
                break;
            case "REJECTED":
                holder.status.setBackgroundColor(Color.parseColor("#C8E6C9")); // Jasny zielony
                holder.status.setTextColor(Color.parseColor("#2E7D32"));
                break;
            default:
                holder.status.setBackgroundColor(Color.parseColor("#FFF9C4")); // Jasny żółty
                holder.status.setTextColor(Color.parseColor("#FBC02D"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView keyword, timestamp, context, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            keyword = itemView.findViewById(R.id.historyKeyword);
            timestamp = itemView.findViewById(R.id.historyTimestamp);
            context = itemView.findViewById(R.id.historyContext);
            status = itemView.findViewById(R.id.historyStatus);
        }
    }
}
