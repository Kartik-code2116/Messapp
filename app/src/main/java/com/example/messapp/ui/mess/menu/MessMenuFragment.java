package com.example.messapp.ui.mess.menu;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.messapp.databinding.FragmentMessMenuBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessMenuFragment extends Fragment {

    private FragmentMessMenuBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private Calendar selectedDateCalendar;
    
    // Cutoff times (defaults)
    private int lunchCutoffHour = 10;
    private int lunchCutoffMinute = 30;
    private int dinnerCutoffHour = 16;
    private int dinnerCutoffMinute = 30;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedDateCalendar = Calendar.getInstance(); // Default to today

        fetchMessIdAndSetup();

        binding.btnSelectDate.setOnClickListener(v -> showDatePickerDialog());
        binding.btnSaveLunch.setOnClickListener(v -> saveMenu("lunch"));
        binding.btnSaveDinner.setOnClickListener(v -> saveMenu("dinner"));

        return root;
    }

    private void fetchMessIdAndSetup() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to manage menu", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMessSettings(); // Fetch cutoff times first
                            displaySelectedDate();
                            loadMenuForSelectedDate();
                        } else {
                            Toast.makeText(getContext(), "Mess ID not found for this user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), "Error fetching mess owner data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displaySelectedDate() {
        if (binding == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        binding.textSelectedDate.setText(sdf.format(selectedDateCalendar.getTime()));
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateCalendar.set(year, month, dayOfMonth);
                    displaySelectedDate();
                    loadMenuForSelectedDate();
                },
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void loadMenuForSelectedDate() {
        if (binding == null) return;
        if (currentMessId == null) {
            return;
        }
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedDateCalendar.getTime());

        db.collection("menus")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("date", formattedDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) return;
                    String lunchMenu = "";
                    String dinnerMenu = "";
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lunchMenu = queryDocumentSnapshots.getDocuments().get(0).getString("lunch");
                        dinnerMenu = queryDocumentSnapshots.getDocuments().get(0).getString("dinner");
                    }
                    binding.lunchEditText.setText(lunchMenu);
                    binding.dinnerEditText.setText(dinnerMenu);

                    // Apply time-based editing restrictions
                    applyMenuEditingRestrictions();

                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), "Error loading menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.lunchEditText.setText("");
                    binding.dinnerEditText.setText("");
                    applyMenuEditingRestrictions(); // Still apply restrictions even on error
                });
    }

    private void fetchMessSettings() {
        if (currentMessId == null) return;
        
        db.collection("mess_settings").document(currentMessId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long lunchHour = documentSnapshot.getLong("lunchCutoffHour");
                        Long lunchMinute = documentSnapshot.getLong("lunchCutoffMinute");
                        Long dinnerHour = documentSnapshot.getLong("dinnerCutoffHour");
                        Long dinnerMinute = documentSnapshot.getLong("dinnerCutoffMinute");

                        if (lunchHour != null) lunchCutoffHour = lunchHour.intValue();
                        if (lunchMinute != null) lunchCutoffMinute = lunchMinute.intValue();
                        if (dinnerHour != null) dinnerCutoffHour = dinnerHour.intValue();
                        if (dinnerMinute != null) dinnerCutoffMinute = dinnerMinute.intValue();
                    }
                    // Apply restrictions after fetching settings
                    if (binding != null) {
                        applyMenuEditingRestrictions();
                    }
                });
    }

    private void applyMenuEditingRestrictions() {
        if (binding == null) return;
        Calendar now = Calendar.getInstance();
        Calendar selectedDate = (Calendar) selectedDateCalendar.clone();

        // Normalize dates for comparison (remove time component)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);

        boolean isToday = today.equals(selectedDate);
        boolean isPast = selectedDate.before(today);

        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        if (isPast) {
            // Past dates - disable editing
            binding.lunchEditText.setEnabled(false);
            binding.btnSaveLunch.setEnabled(false);
            binding.dinnerEditText.setEnabled(false);
            binding.btnSaveDinner.setEnabled(false);
        } else if (isToday) {
            // Today - check cutoff times
            boolean lunchCutoffPassed = (currentHour > lunchCutoffHour) || 
                                        (currentHour == lunchCutoffHour && currentMinute >= lunchCutoffMinute);
            boolean dinnerCutoffPassed = (currentHour > dinnerCutoffHour) || 
                                        (currentHour == dinnerCutoffHour && currentMinute >= dinnerCutoffMinute);

            binding.lunchEditText.setEnabled(!lunchCutoffPassed);
            binding.btnSaveLunch.setEnabled(!lunchCutoffPassed);
            
            binding.dinnerEditText.setEnabled(!dinnerCutoffPassed);
            binding.btnSaveDinner.setEnabled(!dinnerCutoffPassed);
        } else {
            // Future dates - allow editing
            binding.lunchEditText.setEnabled(true);
            binding.btnSaveLunch.setEnabled(true);
            binding.dinnerEditText.setEnabled(true);
            binding.btnSaveDinner.setEnabled(true);
        }
    }

    private void saveMenu(String mealType) {
        if (binding == null) return;
        if (currentMessId == null) {
            Toast.makeText(getContext(), "Mess ID not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(selectedDateCalendar.getTime());
        String menuText;

        if (mealType.equals("lunch")) {
            menuText = binding.lunchEditText.getText().toString().trim();
        } else {
            menuText = binding.dinnerEditText.getText().toString().trim();
        }

        if (menuText.isEmpty()) {
            Toast.makeText(getContext(), "Menu cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar selectedDate = (Calendar) selectedDateCalendar.clone();
        
        // Normalize dates for comparison
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
        selectedDate.set(Calendar.MINUTE, 0);
        selectedDate.set(Calendar.SECOND, 0);
        selectedDate.set(Calendar.MILLISECOND, 0);

        boolean isToday = today.equals(selectedDate);
        boolean isPast = selectedDate.before(today);

        if (isPast) {
            Toast.makeText(getContext(), "Cannot save menu for past dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Time restrictions only apply for today's menu
        if (isToday) {
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            
            if (mealType.equals("lunch")) {
                boolean cutoffPassed = (currentHour > lunchCutoffHour) || 
                                     (currentHour == lunchCutoffHour && currentMinute >= lunchCutoffMinute);
                if (cutoffPassed) {
                    String cutoffTime = String.format(Locale.getDefault(), "%02d:%02d", lunchCutoffHour, lunchCutoffMinute);
                    Toast.makeText(getContext(), 
                        "Lunch menu cutoff time (" + cutoffTime + ") has passed. Cannot save menu now.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (mealType.equals("dinner")) {
                boolean cutoffPassed = (currentHour > dinnerCutoffHour) || 
                                     (currentHour == dinnerCutoffHour && currentMinute >= dinnerCutoffMinute);
                if (cutoffPassed) {
                    String cutoffTime = String.format(Locale.getDefault(), "%02d:%02d", dinnerCutoffHour, dinnerCutoffMinute);
                    Toast.makeText(getContext(), 
                        "Dinner menu cutoff time (" + cutoffTime + ") has passed. Cannot save menu now.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        DocumentReference menuRef = db.collection("menus").document(currentMessId + "_" + formattedDate);

        menuRef.get().addOnSuccessListener(documentSnapshot -> {
            if (binding == null) return;
            Map<String, Object> menuData = new HashMap<>();
            if (documentSnapshot.exists()) {
                menuData = (Map<String, Object>) documentSnapshot.getData();
            }
            if (menuData == null)
                menuData = new HashMap<>();

            menuData.put("messId", currentMessId);
            menuData.put("date", formattedDate);
            menuData.put(mealType, menuText);

            menuRef.set(menuData)
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;
                        Toast.makeText(getContext(), mealType + " menu saved successfully!", Toast.LENGTH_SHORT).show();
                        loadMenuForSelectedDate(); // Refresh menu display and button states
                    })
                    .addOnFailureListener(e -> {
                        if (binding == null) return;
                        Toast.makeText(getContext(), "Error saving " + mealType + " menu: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
