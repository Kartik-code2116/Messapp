package com.example.messapp.ui.mess.settings;

import android.os.Bundle;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messapp.databinding.ActivityMessSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MessSettingsActivity extends AppCompatActivity {

    private ActivityMessSettingsBinding binding;
    private FirebaseFirestore db;
    private String messId;

    // Default values
    private static final int DEFAULT_LUNCH_HOUR = 10;
    private static final int DEFAULT_LUNCH_MINUTE = 30;
    private static final int DEFAULT_DINNER_HOUR = 16;
    private static final int DEFAULT_DINNER_MINUTE = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupNumberPickers();
        fetchMessId();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupNumberPickers() {
        // Lunch Hour Picker (0-23)
        binding.lunchHourPicker.setMinValue(0);
        binding.lunchHourPicker.setMaxValue(23);
        binding.lunchHourPicker.setValue(DEFAULT_LUNCH_HOUR);
        binding.lunchHourPicker.setWrapSelectorWheel(true);

        // Lunch Minute Picker (0-59)
        binding.lunchMinutePicker.setMinValue(0);
        binding.lunchMinutePicker.setMaxValue(59);
        binding.lunchMinutePicker.setValue(DEFAULT_LUNCH_MINUTE);
        binding.lunchMinutePicker.setWrapSelectorWheel(true);

        // Dinner Hour Picker (0-23)
        binding.dinnerHourPicker.setMinValue(0);
        binding.dinnerHourPicker.setMaxValue(23);
        binding.dinnerHourPicker.setValue(DEFAULT_DINNER_HOUR);
        binding.dinnerHourPicker.setWrapSelectorWheel(true);

        // Dinner Minute Picker (0-59)
        binding.dinnerMinutePicker.setMinValue(0);
        binding.dinnerMinutePicker.setMaxValue(59);
        binding.dinnerMinutePicker.setValue(DEFAULT_DINNER_MINUTE);
        binding.dinnerMinutePicker.setWrapSelectorWheel(true);

        // Format display to show leading zeros
        binding.lunchHourPicker.setFormatter(value -> String.format("%02d", value));
        binding.lunchMinutePicker.setFormatter(value -> String.format("%02d", value));
        binding.dinnerHourPicker.setFormatter(value -> String.format("%02d", value));
        binding.dinnerMinutePicker.setFormatter(value -> String.format("%02d", value));
    }

    private void fetchMessId() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        messId = documentSnapshot.getString("messId");
                        if (messId != null) {
                            loadExistingSettings();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading mess data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingSettings() {
        db.collection("mess_settings").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long lunchHour = documentSnapshot.getLong("lunchCutoffHour");
                        Long lunchMinute = documentSnapshot.getLong("lunchCutoffMinute");
                        Long dinnerHour = documentSnapshot.getLong("dinnerCutoffHour");
                        Long dinnerMinute = documentSnapshot.getLong("dinnerCutoffMinute");

                        if (lunchHour != null)
                            binding.lunchHourPicker.setValue(lunchHour.intValue());
                        if (lunchMinute != null)
                            binding.lunchMinutePicker.setValue(lunchMinute.intValue());
                        if (dinnerHour != null)
                            binding.dinnerHourPicker.setValue(dinnerHour.intValue());
                        if (dinnerMinute != null)
                            binding.dinnerMinutePicker.setValue(dinnerMinute.intValue());

                        Boolean allowMultiple = documentSnapshot.getBoolean("allowMultipleChanges");
                        if (allowMultiple != null) {
                            binding.switchAllowMultipleChanges.setChecked(allowMultiple);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        if (messId == null) {
            Toast.makeText(this, "Mess ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        int lunchHour = binding.lunchHourPicker.getValue();
        int lunchMinute = binding.lunchMinutePicker.getValue();
        int dinnerHour = binding.dinnerHourPicker.getValue();
        int dinnerMinute = binding.dinnerMinutePicker.getValue();

        // Validation: Dinner should be after lunch
        int lunchTotalMinutes = lunchHour * 60 + lunchMinute;
        int dinnerTotalMinutes = dinnerHour * 60 + dinnerMinute;

        if (dinnerTotalMinutes <= lunchTotalMinutes) {
            Toast.makeText(this, "Dinner cutoff must be after lunch cutoff", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("messId", messId);
        settings.put("lunchCutoffHour", lunchHour);
        settings.put("lunchCutoffMinute", lunchMinute);
        settings.put("dinnerCutoffHour", dinnerHour);
        settings.put("dinnerCutoffMinute", dinnerMinute);
        settings.put("allowMultipleChanges", binding.switchAllowMultipleChanges.isChecked());
        settings.put("updatedAt", System.currentTimeMillis());
        settings.put("updatedBy", FirebaseAuth.getInstance().getCurrentUser().getUid());

        binding.btnSaveSettings.setEnabled(false);
        binding.btnSaveSettings.setText("Saving...");

        db.collection("mess_settings").document(messId)
                .set(settings)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show();
                    binding.btnSaveSettings.setEnabled(true);
                    binding.btnSaveSettings.setText("Save Settings");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
