package com.example.messapp.ui.mess.weeklymenu;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messapp.R;
import com.example.messapp.databinding.ActivityWeeklyMenuBinding;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WeeklyMenuActivity extends AppCompatActivity {

    private ActivityWeeklyMenuBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private Calendar weekStartCalendar;
    private SimpleDateFormat dateFormat;

    // Day input fields
    private TextInputEditText[] lunchInputs = new TextInputEditText[7];
    private TextInputEditText[] dinnerInputs = new TextInputEditText[7];
    private String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeeklyMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Set week start to Monday of current week
        weekStartCalendar = Calendar.getInstance();
        weekStartCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        initializeInputFields();
        setupWeekNavigation();
        setupSaveButton();
        
        fetchMessIdAndLoadMenu();
    }

    private void initializeInputFields() {
        // Initialize all lunch and dinner input fields
        lunchInputs[0] = binding.etMondayLunch;
        dinnerInputs[0] = binding.etMondayDinner;
        lunchInputs[1] = binding.etTuesdayLunch;
        dinnerInputs[1] = binding.etTuesdayDinner;
        lunchInputs[2] = binding.etWednesdayLunch;
        dinnerInputs[2] = binding.etWednesdayDinner;
        lunchInputs[3] = binding.etThursdayLunch;
        dinnerInputs[3] = binding.etThursdayDinner;
        lunchInputs[4] = binding.etFridayLunch;
        dinnerInputs[4] = binding.etFridayDinner;
        lunchInputs[5] = binding.etSaturdayLunch;
        dinnerInputs[5] = binding.etSaturdayDinner;
        lunchInputs[6] = binding.etSundayLunch;
        dinnerInputs[6] = binding.etSundayDinner;
    }

    private void setupWeekNavigation() {
        updateWeekDisplay();

        binding.btnPrevWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekDisplay();
            loadMenuForCurrentWeek();
        });

        binding.btnNextWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekDisplay();
            loadMenuForCurrentWeek();
        });
    }

    private void updateWeekDisplay() {
        Calendar weekEnd = (Calendar) weekStartCalendar.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String weekRange = displayFormat.format(weekStartCalendar.getTime()) + " - " + 
                          displayFormat.format(weekEnd.getTime());
        binding.textWeekRange.setText(weekRange);
    }

    private void setupSaveButton() {
        binding.btnSaveWeeklyMenu.setOnClickListener(v -> saveWeeklyMenu());
    }

    private void fetchMessIdAndLoadMenu() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            loadMenuForCurrentWeek();
                        } else {
                            Toast.makeText(this, "Mess ID not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching mess info", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMenuForCurrentWeek() {
        if (currentMessId == null) return;

        // Clear current inputs
        for (int i = 0; i < 7; i++) {
            lunchInputs[i].setText("");
            dinnerInputs[i].setText("");
        }

        // Load menu for each day of the week
        Calendar dayCal = (Calendar) weekStartCalendar.clone();
        
        for (int i = 0; i < 7; i++) {
            String dateStr = dateFormat.format(dayCal.getTime());
            final int dayIndex = i;

            db.collection("menus")
                    .whereEqualTo("messId", currentMessId)
                    .whereEqualTo("date", dateStr)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String lunch = querySnapshot.getDocuments().get(0).getString("lunch");
                            String dinner = querySnapshot.getDocuments().get(0).getString("dinner");
                            
                            if (lunch != null) {
                                lunchInputs[dayIndex].setText(lunch);
                            }
                            if (dinner != null) {
                                dinnerInputs[dayIndex].setText(dinner);
                            }
                        }
                    });

            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void saveWeeklyMenu() {
        if (currentMessId == null) {
            Toast.makeText(this, "Mess ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate inputs
        boolean hasEmptyFields = false;
        for (int i = 0; i < 7; i++) {
            String lunch = lunchInputs[i].getText() != null ? lunchInputs[i].getText().toString().trim() : "";
            String dinner = dinnerInputs[i].getText() != null ? dinnerInputs[i].getText().toString().trim() : "";
            
            if (lunch.isEmpty() || dinner.isEmpty()) {
                hasEmptyFields = true;
                break;
            }
        }

        if (hasEmptyFields) {
            Toast.makeText(this, "Please fill all lunch and dinner menus", Toast.LENGTH_LONG).show();
            return;
        }

        // Save all menus using batch write
        WriteBatch batch = db.batch();
        Calendar dayCal = (Calendar) weekStartCalendar.clone();

        for (int i = 0; i < 7; i++) {
            String dateStr = dateFormat.format(dayCal.getTime());
            String lunch = lunchInputs[i].getText().toString().trim();
            String dinner = dinnerInputs[i].getText().toString().trim();

            Map<String, Object> menuData = new HashMap<>();
            menuData.put("messId", currentMessId);
            menuData.put("date", dateStr);
            menuData.put("lunch", lunch);
            menuData.put("dinner", dinner);
            menuData.put("updatedAt", System.currentTimeMillis());

            String docId = currentMessId + "_" + dateStr;
            batch.set(db.collection("menus").document(docId), menuData, SetOptions.merge());

            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Weekly menu saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
