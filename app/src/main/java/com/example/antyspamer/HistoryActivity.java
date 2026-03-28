package com.example.antyspamer;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private Button backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.historyRecyclerView);
        backBtn = findViewById(R.id.backBtn);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        loadHistory();

        backBtn.setOnClickListener(v -> finish());
    }

    private void loadHistory() {
        List<AlertItem> history = dbHelper.getAllHistory();
        adapter = new HistoryAdapter(history);
        recyclerView.setAdapter(adapter);
    }
}
