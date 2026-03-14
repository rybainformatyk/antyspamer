package com.example.antyspamer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private TextInputEditText editName, editSurname;
    private CheckBox checkFinancial, checkSecurity, checkUrgent;
    private Button backBtn;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editName = findViewById(R.id.editName);
        editSurname = findViewById(R.id.editSurname);
        checkFinancial = findViewById(R.id.checkFinancial);
        checkSecurity = findViewById(R.id.checkSecurity);
        checkUrgent = findViewById(R.id.checkUrgent);
        backBtn = findViewById(R.id.backBtn);

        prefs = getSharedPreferences("SpamPrefs", MODE_PRIVATE);

        // Load current settings
        editName.setText(prefs.getString("user_name", ""));
        editSurname.setText(prefs.getString("user_surname", ""));
        checkFinancial.setChecked(prefs.getBoolean("detect_financial", true));
        checkSecurity.setChecked(prefs.getBoolean("detect_security", true));
        checkUrgent.setChecked(prefs.getBoolean("detect_urgent", true));

        backBtn.setOnClickListener(v -> {
            prefs.edit()
                    .putString("user_name", editName.getText().toString())
                    .putString("user_surname", editSurname.getText().toString())
                    .putBoolean("detect_financial", checkFinancial.isChecked())
                    .putBoolean("detect_security", checkSecurity.isChecked())
                    .putBoolean("detect_urgent", checkUrgent.isChecked())
                    .apply();
            finish();
        });
    }
}
