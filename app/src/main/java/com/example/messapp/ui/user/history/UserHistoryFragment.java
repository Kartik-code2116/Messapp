package com.example.messapp.ui.user.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentUserHistoryBinding;
import com.example.messapp.models.MealSelection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserHistoryFragment extends Fragment {

    private static final SimpleDateFormat MONTH_YEAR_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_KEY_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private FragmentUserHistoryBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private HistoryAdapter historyAdapter;
    private final List<String> monthOptions = new ArrayList<>();
    private boolean suppressSpinnerCallback;
    private String selectedMonthYear;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        historyAdapter = new HistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerViewHistory.setLayoutManager(layoutManager);
        binding.recyclerViewHistory.setAdapter(historyAdapter);
        binding.recyclerViewHistory.setHasFixedSize(false);

        binding.swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.brand_primary));
        binding.swipeRefresh.setOnRefreshListener(this::reloadHistory);

        setupMonthYearSpinner();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectedMonthYear != null) {
            loadMealHistory(selectedMonthYear);
        }
    }

    private void setupMonthYearSpinner() {
        monthOptions.clear();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            monthOptions.add(MONTH_YEAR_FORMAT.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, -1);
        }
        Collections.reverse(monthOptions);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                monthOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMonthYear.setAdapter(adapter);

        suppressSpinnerCallback = true;
        binding.spinnerMonthYear.setSelection(monthOptions.size() - 1);
        suppressSpinnerCallback = false;

        selectedMonthYear = monthOptions.get(monthOptions.size() - 1);

        binding.spinnerMonthYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallback) {
                    return;
                }
                selectedMonthYear = monthOptions.get(position);
                loadMealHistory(selectedMonthYear);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        binding.btnChangeMonth.setVisibility(View.GONE);
        loadMealHistory(selectedMonthYear);
    }

    private void reloadHistory() {
        if (selectedMonthYear != null) {
            loadMealHistory(selectedMonthYear);
        } else {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private void loadMealHistory(String monthYear) {
        if (binding == null) {
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            binding.swipeRefresh.setRefreshing(false);
            setLoading(false);
            showEmptyState(true, "Sign in to view your meal history");
            historyAdapter.submitList(Collections.emptyList());
            resetSummary();
            Toast.makeText(getContext(), "Sign in to view history", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar startCal = parseMonthStart(monthYear);
        if (startCal == null) {
            binding.swipeRefresh.setRefreshing(false);
            setLoading(false);
            return;
        }

        Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.MONTH, 1);

        String startDate = DATE_KEY_FORMAT.format(startCal.getTime());
        String endDate = DATE_KEY_FORMAT.format(endCal.getTime());

        setLoading(true);
        showEmptyState(false, null);

        String userId = mAuth.getCurrentUser().getUid();

        // Single-field query avoids Firestore composite index requirements;
        // month range is applied on the client for reliability.
        db.collection("meal_selections")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (binding == null) {
                        return;
                    }

                    Map<String, MealSelection> dailyMealSelections = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        if (date == null || date.compareTo(startDate) < 0 || date.compareTo(endDate) >= 0) {
                            continue;
                        }

                        String lunchStatus = normalizeStatus(document.getString("lunch"));
                        String dinnerStatus = normalizeStatus(document.getString("dinner"));

                        dailyMealSelections.put(date,
                                new MealSelection(userId, date, lunchStatus, dinnerStatus));
                    }

                    List<MealSelection> results = new ArrayList<>(dailyMealSelections.values());
                    Collections.sort(results, (a, b) -> b.getDate().compareTo(a.getDate()));

                    historyAdapter.submitList(results);
                    updateSummary(results);
                    showEmptyState(results.isEmpty(), "No meal records for this month");
                    setLoading(false);
                    binding.swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) {
                        return;
                    }
                    setLoading(false);
                    binding.swipeRefresh.setRefreshing(false);
                    showEmptyState(true, "Could not load history. Pull down to retry.");
                    historyAdapter.submitList(Collections.emptyList());
                    resetSummary();
                    Toast.makeText(getContext(),
                            "Error loading meal history: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Nullable
    private Calendar parseMonthStart(String monthYear) {
        try {
            Date parsed = MONTH_YEAR_FORMAT.parse(monthYear);
            if (parsed == null) {
                return null;
            }
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(parsed);
            startCal.set(Calendar.DAY_OF_MONTH, 1);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            return startCal;
        } catch (ParseException e) {
            return null;
        }
    }

    private String normalizeStatus(@Nullable String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Not marked";
        }
        return status.trim();
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
            boolean lunchIn = "IN".equalsIgnoreCase(selection.getLunchStatus());
            boolean dinnerIn = "IN".equalsIgnoreCase(selection.getDinnerStatus());
            boolean lunchOut = "OUT".equalsIgnoreCase(selection.getLunchStatus());
            boolean dinnerOut = "OUT".equalsIgnoreCase(selection.getDinnerStatus());

            if (lunchIn) {
                totalLunch++;
            }
            if (dinnerIn) {
                totalDinner++;
            }

            if (lunchIn || dinnerIn) {
                daysIn++;
            } else if (lunchOut || dinnerOut) {
                daysOut++;
            }
        }

        binding.textTotalLunch.setText(String.valueOf(totalLunch));
        binding.textTotalDinner.setText(String.valueOf(totalDinner));
        binding.textDaysIn.setText(String.valueOf(daysIn));
        binding.textDaysOut.setText(String.valueOf(daysOut));
    }

    private void resetSummary() {
        if (binding == null) {
            return;
        }
        binding.textTotalLunch.setText("0");
        binding.textTotalDinner.setText("0");
        binding.textDaysIn.setText("0");
        binding.textDaysOut.setText("0");
    }

    private void setLoading(boolean loading) {
        if (binding == null) {
            return;
        }
        binding.progressHistory.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            binding.recyclerViewHistory.setVisibility(View.INVISIBLE);
        } else {
            binding.recyclerViewHistory.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show, @Nullable String message) {
        if (binding == null) {
            return;
        }
        binding.layoutEmptyHistory.setVisibility(show ? View.VISIBLE : View.GONE);
        if (message != null) {
            binding.textEmptyHistory.setText(message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
