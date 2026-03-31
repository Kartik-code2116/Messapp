package com.example.messapp.ui.user.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messapp.databinding.FragmentUserHistoryBinding;
import com.example.messapp.models.MealSelection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserHistoryFragment extends Fragment {

    private FragmentUserHistoryBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private HistoryAdapter historyAdapter;
    private List<MealSelection> mealHistoryList;
    private String currentMessId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mealHistoryList = new ArrayList<>();
        historyAdapter = new HistoryAdapter();
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewHistory.setAdapter(historyAdapter);

        setupMonthYearSpinner();

        return root;
    }

    private void setupMonthYearSpinner() {
        List<String> months = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

        for (int i = 0; i < 12; i++) { // Generate last 12 months
            months.add(sdf.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, -1);
        }
        Collections.reverse(months); // Show most recent month last

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMonthYear.setAdapter(adapter);
        binding.spinnerMonthYear.setSelection(months.size() - 1); // Select current month

        binding.spinnerMonthYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonthYear = (String) parent.getItemAtPosition(position);
                loadMealHistory(selectedMonthYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // The change month button is redundant with the spinner, so hide it.
        binding.btnChangeMonth.setVisibility(View.GONE);
    }

    private void loadMealHistory(String selectedMonthYear) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view history", Toast.LENGTH_SHORT).show();
            mealHistoryList.clear();
            historyAdapter.submitList(mealHistoryList);
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        // Parse selectedMonthYear to get start and end date for the month
        Calendar startCal = Calendar.getInstance();
        try {
            startCal.setTime(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(selectedMonthYear));
            startCal.set(Calendar.DAY_OF_MONTH, 1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            return;
        }

        Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.MONTH, 1);
        endCal.add(Calendar.MILLISECOND, -1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        db.collection("meal_selections")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", dateFormat.format(startCal.getTime()))
                .whereLessThanOrEqualTo("date", dateFormat.format(endCal.getTime()))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) {
                        return;
                    }
                    Map<String, MealSelection> dailyMealSelections = new HashMap<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String lunchStatus = document.getString("lunch");
                        String dinnerStatus = document.getString("dinner");

                        if (date != null) {
                            MealSelection mealSelection = new MealSelection(userId, date,
                                lunchStatus != null ? lunchStatus : "Not Selected",
                                dinnerStatus != null ? dinnerStatus : "Not Selected");
                            dailyMealSelections.put(date, mealSelection);
                        }
                    }

                    mealHistoryList.clear();
                    mealHistoryList.addAll(dailyMealSelections.values());
                    Collections.sort(mealHistoryList, (o1, o2) -> o1.getDate().compareTo(o2.getDate())); // Sort by date
                    historyAdapter.submitList(mealHistoryList);

                    updateSummary(mealHistoryList);

                })
                .addOnFailureListener(e -> {
                    if (binding == null) {
                        return;
                    }
                    Toast.makeText(getContext(), "Error loading meal history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSummary(List<MealSelection> mealSelections) {
        if (binding == null) {
            return;
        }
        int totalLunch = 0;
        int totalDinner = 0;
        int daysIn = 0;
        int daysOut = 0;

        for (MealSelection selection : mealSelections) {
            if ("IN".equals(selection.getLunchStatus())) {
                totalLunch++;
            }
            if ("IN".equals(selection.getDinnerStatus())) {
                totalDinner++;
            }
            if ("IN".equals(selection.getLunchStatus()) || "IN".equals(selection.getDinnerStatus())) {
                daysIn++;
            } else {
                daysOut++;
            }
        }

        binding.textTotalLunch.setText("Total Lunch Taken: " + totalLunch);
        binding.textTotalDinner.setText("Total Dinner Taken: " + totalDinner);
        binding.textDaysIn.setText("Days IN: " + daysIn);
        binding.textDaysOut.setText("Days OUT: " + daysOut);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
