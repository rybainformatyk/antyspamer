package com.example.antyspamer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistory();
    }

    private void loadHistory() {
        List<DatabaseHelper.AlertItem> history = dbHelper.getAllHistory();
        recyclerView.setAdapter(new HistoryAdapter(history));
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<DatabaseHelper.AlertItem> list;

        public HistoryAdapter(List<DatabaseHelper.AlertItem> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.AlertItem item = list.get(position);
            holder.keyword.setText("Słowo: " + item.keyword);
            holder.timestamp.setText(item.timestamp);
            holder.context.setText(item.context);
            holder.status.setText(item.status);

            int bg = R.color.status_pending_bg;
            int txt = R.color.status_pending_text;

            if ("CONFIRMED".equals(item.status)) {
                bg = R.color.status_confirmed_bg; txt = R.color.status_confirmed_text;
            } else if ("REJECTED".equals(item.status)) {
                bg = R.color.status_rejected_bg; txt = R.color.status_rejected_text;
            }

            holder.status.setBackgroundResource(R.drawable.status_bg_pending);
            holder.status.setBackgroundTintList(ContextCompat.getColorStateList(HistoryActivity.this, bg));
            holder.status.setTextColor(ContextCompat.getColor(HistoryActivity.this, txt));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView keyword, timestamp, context, status;
            public ViewHolder(@NonNull View v) {
                super(v);
                keyword = v.findViewById(R.id.historyKeyword);
                timestamp = v.findViewById(R.id.historyTimestamp);
                context = v.findViewById(R.id.historyContext);
                status = v.findViewById(R.id.historyStatus);
            }
        }
    }
}
