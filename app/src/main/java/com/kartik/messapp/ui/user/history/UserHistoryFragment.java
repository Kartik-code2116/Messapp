package com.kartik.messapp.ui.user.history;

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

import com.kartik.messapp.R;
import com.kartik.messapp.databinding.FragmentUserHistoryBinding;
import com.kartik.messapp.models.MealSelection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        binding.recyclerViewHistory.setLayoutManager(layoutManager);
        binding.recyclerViewHistory.setNestedScrollingEnabled(false);
        binding.recyclerViewHistory.setAdapter(historyAdapter);
        historyAdapter.registerAdapterDataObserver(new androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                if (binding != null) {
                    binding.recyclerViewHistory.requestLayout();
                    if (binding.recyclerViewHistory.getParent() instanceof View) {
                        ((View) binding.recyclerViewHistory.getParent()).requestLayout();
                    }
                }
            }
        });

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

        // Load profile (messId + auto-select prefs + expiries), then fetch meal_selections for this student
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (binding == null) return;

                    String subscriptionType = userDoc.exists() ? userDoc.getString("subscriptionType") : "BOTH";
                    boolean autoSelectLunch = userDoc.exists() && Boolean.TRUE.equals(userDoc.getBoolean("autoSelectLunch"));
                    boolean autoSelectDinner = userDoc.exists() && Boolean.TRUE.equals(userDoc.getBoolean("autoSelectDinner"));
                    String oneTimeAutoSelect = userDoc.exists() ? userDoc.getString("oneTimeAutoSelect") : "NONE";
                    String messId = userDoc.exists() ? userDoc.getString("messId") : null;

                    Long lunchExpiry = userDoc.exists() ? userDoc.getLong("lunchSubscriptionExpiry") : null;
                    Long dinnerExpiry = userDoc.exists() ? userDoc.getLong("dinnerSubscriptionExpiry") : null;
                    Long generalExpiry = userDoc.exists() ? userDoc.getLong("subscriptionExpiry") : null;
                    Long oneTimeExpiry = userDoc.exists() ? userDoc.getLong("oneTimeMealExpiry") : null;

                    long lExp = (lunchExpiry != null && lunchExpiry > 0) ? lunchExpiry
                            : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);
                    long dExp = (dinnerExpiry != null && dinnerExpiry > 0) ? dinnerExpiry
                            : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);
                    long oExp = (oneTimeExpiry != null && oneTimeExpiry > 0) ? oneTimeExpiry
                            : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);

                    fetchMealSelectionsForMonth(userId, messId, startDate, endDate,
                            subscriptionType, autoSelectLunch, autoSelectDinner, oneTimeAutoSelect,
                            lExp, dExp, oExp);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    finishLoadingWithError("Could not load user profile. Pull down to retry.",
                            "Error loading user profile: " + e.getMessage());
                });
    }

    /**
     * Loads selections by userId, and by messId when available (covers legacy docs whose id ends with _userId).
     */
    private void fetchMealSelectionsForMonth(String userId, @Nullable String messId,
                                             String startDate, String endDate,
                                             String subscriptionType,
                                             boolean autoSelectLunch, boolean autoSelectDinner,
                                             String oneTimeAutoSelect,
                                             long lExp, long dExp, long oExp) {
        List<com.google.android.gms.tasks.Task<?>> allTasks = new ArrayList<>();

        // Task 1: Fetch by userId (covers all modern documents)
        com.google.android.gms.tasks.Task<QuerySnapshot> queryTask = db.collection("meal_selections")
                .whereEqualTo("userId", userId)
                .get();
        allTasks.add(queryTask);

        // Tasks 2..N: Fetch by exact docIds for the month (covers legacy documents missing userId field)
        Calendar cal = parseMonthStart(selectedMonthYear);
        if (cal != null && messId != null && !messId.isEmpty()) {
            Calendar endCal = (Calendar) cal.clone();
            endCal.add(Calendar.MONTH, 1);
            while (cal.before(endCal)) {
                String dateStr = DATE_KEY_FORMAT.format(cal.getTime());
                String docId = messId + "_" + dateStr + "_" + userId;
                allTasks.add(db.collection("meal_selections").document(docId).get());
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(allTasks)
                .addOnSuccessListener(taskList -> {
                    if (binding == null) return;

                    QuerySnapshot byUserSnapshot = null;
                    List<DocumentSnapshot> directDocs = new ArrayList<>();

                    // Task 0 is the query
                    if (taskList.get(0).isSuccessful()) {
                        byUserSnapshot = (QuerySnapshot) taskList.get(0).getResult();
                    }

                    for (int i = 1; i < taskList.size(); i++) {
                        com.google.android.gms.tasks.Task<?> t = taskList.get(i);
                        if (t.isSuccessful() && t.getResult() instanceof DocumentSnapshot) {
                            DocumentSnapshot snap = (DocumentSnapshot) t.getResult();
                            if (snap.exists()) {
                                directDocs.add(snap);
                            }
                        }
                    }

                    applyMealHistoryResults(userId, startDate, endDate, subscriptionType,
                            autoSelectLunch, autoSelectDinner, oneTimeAutoSelect,
                            lExp, dExp, oExp,
                            byUserSnapshot, directDocs);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    finishLoadingWithError("Could not load history. Pull down to retry.",
                            "Error loading meal history: " + e.getMessage());
                });
    }

    private void applyMealHistoryResults(String userId, String startDate, String endDate,
                                         String subscriptionType,
                                         boolean autoSelectLunch, boolean autoSelectDinner,
                                         String oneTimeAutoSelect,
                                         long lExp, long dExp, long oExp,
                                         @Nullable QuerySnapshot byUserSnapshot,
                                         @Nullable List<DocumentSnapshot> directDocs) {
        if (binding == null) return;

        Map<String, MealSelection> dailyMealSelections = new HashMap<>();
        String todayStr = DATE_KEY_FORMAT.format(new Date());

        try {
            Date start = DATE_KEY_FORMAT.parse(startDate);
            Date end = DATE_KEY_FORMAT.parse(endDate);
            if (start != null && end != null) {
                Calendar current = Calendar.getInstance();
                current.setTime(start);
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(end);

                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 23);
                today.set(Calendar.MINUTE, 59);
                today.set(Calendar.SECOND, 59);
                today.set(Calendar.MILLISECOND, 999);

                while (current.before(endCal)) {
                    if (current.after(today)) {
                        current.add(Calendar.DAY_OF_MONTH, 1);
                        continue;
                    }
                    String dateStr = DATE_KEY_FORMAT.format(current.getTime());
                    long dayTime = current.getTimeInMillis();

                    boolean isLunchSubscribed = false;
                    boolean isDinnerSubscribed = false;

                    if ("ONE_TIME".equals(subscriptionType)) {
                        isLunchSubscribed = oExp >= dayTime;
                        isDinnerSubscribed = oExp >= dayTime;
                    } else {
                        isLunchSubscribed = lExp >= dayTime;
                        isDinnerSubscribed = dExp >= dayTime;
                    }

                    boolean isPastDate = dateStr.compareTo(todayStr) < 0;
                    String defaultLunch = normalizeStatus(null, "LUNCH", subscriptionType, autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, isLunchSubscribed, isPastDate);
                    String defaultDinner = normalizeStatus(null, "DINNER", subscriptionType, autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, isDinnerSubscribed, isPastDate);
                    dailyMealSelections.put(dateStr, new MealSelection(userId, dateStr, defaultLunch, defaultDinner));
                    
                    current.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        } catch (ParseException ignored) {}

        if (byUserSnapshot != null) {
            mergeMealDocuments(byUserSnapshot.getDocuments(), userId, startDate, endDate, subscriptionType,
                    autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, lExp, dExp, oExp, dailyMealSelections, todayStr);
        }
        if (directDocs != null) {
            mergeMealDocuments(directDocs, userId, startDate, endDate, subscriptionType,
                    autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, lExp, dExp, oExp, dailyMealSelections, todayStr);
        }

        List<MealSelection> results = new ArrayList<>(dailyMealSelections.values());
        Collections.sort(results, (a, b) -> b.getDate().compareTo(a.getDate()));

        historyAdapter.submitList(results);
        updateSummary(results);
        showEmptyState(results.isEmpty(), "No meal records for this month");
        setLoading(false);
        binding.swipeRefresh.setRefreshing(false);
    }

    private void mergeMealDocuments(List<? extends DocumentSnapshot> snapshot, String userId,
                                    String startDate, String endDate,
                                    String subscriptionType,
                                    boolean autoSelectLunch, boolean autoSelectDinner,
                                    String oneTimeAutoSelect,
                                    long lExp, long dExp, long oExp,
                                    Map<String, MealSelection> dailyMealSelections, String todayStr) {
        for (DocumentSnapshot document : snapshot) {
            if (!belongsToUser(document, userId)) {
                continue;
            }

            String date = resolveSelectionDate(document, userId);
            if (date == null || date.compareTo(startDate) < 0 || date.compareTo(endDate) >= 0) {
                continue;
            }

            String lunch = document.getString("lunch");
            String dinner = document.getString("dinner");
            if (lunch == null && dinner == null) {
                continue;
            }

            boolean isLunchSubscribed = false;
            boolean isDinnerSubscribed = false;
            try {
                Date parsedDate = DATE_KEY_FORMAT.parse(date);
                if (parsedDate != null) {
                    long dayTime = parsedDate.getTime();
                    if ("ONE_TIME".equals(subscriptionType)) {
                        isLunchSubscribed = oExp >= dayTime;
                        isDinnerSubscribed = oExp >= dayTime;
                    } else {
                        isLunchSubscribed = lExp >= dayTime;
                        isDinnerSubscribed = dExp >= dayTime;
                    }
                }
            } catch (ParseException ignored) {}

            boolean isPastDate = date.compareTo(todayStr) < 0;
            String lunchStatus = normalizeStatus(lunch, "LUNCH", subscriptionType,
                    autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, isLunchSubscribed, isPastDate);
            String dinnerStatus = normalizeStatus(dinner, "DINNER", subscriptionType,
                    autoSelectLunch, autoSelectDinner, oneTimeAutoSelect, isDinnerSubscribed, isPastDate);

            MealSelection existing = dailyMealSelections.get(date);
            if (existing == null) {
                dailyMealSelections.put(date, new MealSelection(userId, date, lunchStatus, dinnerStatus));
            } else {
                if (!"Not marked".equals(lunchStatus)) {
                    existing.setLunchStatus(lunchStatus);
                }
                if (!"Not marked".equals(dinnerStatus)) {
                    existing.setDinnerStatus(dinnerStatus);
                }
            }
        }
    }

    private boolean belongsToUser(DocumentSnapshot document, String userId) {
        String docUserId = document.getString("userId");
        if (userId.equals(docUserId)) {
            return true;
        }
        return document.getId().endsWith("_" + userId);
    }

    @Nullable
    private String resolveSelectionDate(DocumentSnapshot document, String userId) {
        String date = document.getString("date");
        if (date != null && !date.isEmpty()) {
            return date;
        }
        String docId = document.getId();
        if (!docId.endsWith("_" + userId)) {
            return null;
        }
        String prefix = docId.substring(0, docId.length() - userId.length() - 1);
        int lastSep = prefix.lastIndexOf('_');
        if (lastSep < 0 || lastSep >= prefix.length() - 1) {
            return null;
        }
        String candidate = prefix.substring(lastSep + 1);
        if (candidate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return candidate;
        }
        return null;
    }

    private void finishLoadingWithError(String emptyMessage, String toastMessage) {
        if (binding == null) return;
        setLoading(false);
        binding.swipeRefresh.setRefreshing(false);
        showEmptyState(true, emptyMessage);
        historyAdapter.submitList(Collections.emptyList());
        resetSummary();
        if (getContext() != null) {
            Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
        }
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

    private String normalizeStatus(@Nullable String status, String mealType, String subscriptionType,
                                   boolean autoSelectLunch, boolean autoSelectDinner, String oneTimeAutoSelect,
                                   boolean isMealSubscribed, boolean isPastDate) {
        if (!isMealSubscribed) {
            return "No Subscription";
        }
        if (status == null || status.trim().isEmpty() || "RESET".equalsIgnoreCase(status.trim())) {
            if (isPastDate) {
                return "Not marked";
            }
            boolean isOneTime = "ONE_TIME".equals(subscriptionType);
            if (isOneTime) {
                if (mealType.equals("LUNCH") && "LUNCH".equals(oneTimeAutoSelect)) {
                    return "Auto-IN";
                } else if (mealType.equals("DINNER") && "DINNER".equals(oneTimeAutoSelect)) {
                    return "Auto-IN";
                }
            } else {
                if (mealType.equals("LUNCH") && autoSelectLunch) {
                    return "Auto-IN";
                } else if (mealType.equals("DINNER") && autoSelectDinner) {
                    return "Auto-IN";
                }
            }
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
            String lStatus = selection.getLunchStatus();
            String dStatus = selection.getDinnerStatus();
            
            boolean lunchIn = "IN".equalsIgnoreCase(lStatus) 
                    || "Auto-IN".equalsIgnoreCase(lStatus)
                    || "Auto-selected IN".equalsIgnoreCase(lStatus)
                    || "Auto IN".equalsIgnoreCase(lStatus);
            
            boolean dinnerIn = "IN".equalsIgnoreCase(dStatus) 
                    || "Auto-IN".equalsIgnoreCase(dStatus)
                    || "Auto-selected IN".equalsIgnoreCase(dStatus)
                    || "Auto IN".equalsIgnoreCase(dStatus);
            
            boolean lunchOut = "OUT".equalsIgnoreCase(lStatus);
            boolean dinnerOut = "OUT".equalsIgnoreCase(dStatus);

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
        binding.recyclerViewHistory.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void showEmptyState(boolean show, @Nullable String message) {
        if (binding == null) {
            return;
        }
        binding.layoutEmptyHistory.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewHistory.setVisibility(show ? View.GONE : View.VISIBLE);
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
