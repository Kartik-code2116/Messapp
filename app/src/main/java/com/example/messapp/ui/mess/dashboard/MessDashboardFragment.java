package com.example.messapp.ui.mess.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentMessDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.messapp.adapters.SubscriptionRequestAdapter;
import com.example.messapp.managers.MessNotificationManager;
import com.example.messapp.models.SubscriptionRequest;
import com.example.messapp.models.Transaction;
import com.google.android.material.textfield.TextInputEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MessDashboardFragment extends Fragment {

    private FragmentMessDashboardBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;

    // FIX #4: Replaced two identical Firestore listeners (lunchListener /
    // dinnerListener)
    // with a single mealListener that reads both fields in one callback.
    // This halves the number of Firestore reads and eliminates duplicate
    // subscriptions.
    private ListenerRegistration mealListener;

    private ListenerRegistration studentsCountListener;
    private SubscriptionRequestAdapter requestAdapter;
    private List<SubscriptionRequest> pendingRequests = new ArrayList<>();
    private ListenerRegistration requestsListener;
    private android.os.CountDownTimer dashboardTimer;
    private Map<String, DocumentSnapshot> userDocs = new HashMap<>();
    private Map<String, DocumentSnapshot> mealDocs = new HashMap<>();
    private int lunchCutoffHour = 10;
    private int lunchCutoffMinute = 30;
    private int dinnerCutoffHour = 16;
    private int dinnerCutoffMinute = 30;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupRequestRecyclerView();
        displayCurrentDate();
        setupMessConditionButtons();
        binding.btnResetAllAttendance.setOnClickListener(v -> showResetConfirmation());
        fetchMessOwnerData();
        
        binding.swipeRefreshDashboard.setOnRefreshListener(() -> {
            fetchMessOwnerData();
            binding.swipeRefreshDashboard.setRefreshing(false);
        });

        return root;
    }

    private void setupRequestRecyclerView() {
        requestAdapter = new SubscriptionRequestAdapter();
        binding.recyclerPending.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPending.setAdapter(requestAdapter);
        requestAdapter.setOnConfirmClickListener(this::showGrantSubscriptionDialog);
        requestAdapter.setOnDeleteClickListener(this::deleteSubscriptionRequest);
    }

    private void showGrantSubscriptionDialog(SubscriptionRequest request) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_grant_subscription, null);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDays = dialogView.findViewById(R.id.etDays);
        android.widget.RadioGroup radioGroupMealType = dialogView.findViewById(R.id.radio_group_meal_type);
        android.widget.TextView textOneTimeInfo = dialogView.findViewById(R.id.text_one_time_info);

        radioGroupMealType.setOnCheckedChangeListener((group, checkedId) -> {
            textOneTimeInfo.setVisibility(checkedId == R.id.radio_one_time ? View.VISIBLE : View.GONE);
        });

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Grant", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String daysStr = etDays.getText().toString().trim();
                    if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(daysStr)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String mealType = "ONE_TIME";
                    int selectedId = radioGroupMealType.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch)
                        mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner)
                        mealType = "DINNER";
                    else if (selectedId == R.id.radio_both)
                        mealType = "BOTH";
                    grantSubscription(request, Double.parseDouble(amountStr), Integer.parseInt(daysStr), mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void grantSubscription(SubscriptionRequest request, double amount, int days, String mealType) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(request.getStudentId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    long lunchExpiry = System.currentTimeMillis();
                    long dinnerExpiry = System.currentTimeMillis();
                    long oneTimeExpiry = System.currentTimeMillis();
                    long generalExpiry = System.currentTimeMillis();

                    if (documentSnapshot.exists()) {
                        Long existingL = documentSnapshot.getLong("lunchSubscriptionExpiry");
                        Long existingD = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                        Long existingO = documentSnapshot.getLong("oneTimeMealExpiry");
                        Long existingG = documentSnapshot.getLong("subscriptionExpiry");
                        if (existingL != null && existingL > lunchExpiry)
                            lunchExpiry = existingL;
                        if (existingD != null && existingD > dinnerExpiry)
                            dinnerExpiry = existingD;
                        if (existingO != null && existingO > oneTimeExpiry)
                            oneTimeExpiry = existingO;
                        if (existingG != null && existingG > generalExpiry)
                            generalExpiry = existingG;
                    }

                    if (mealType.equals("ONE_TIME")) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(oneTimeExpiry);
                        cal.add(Calendar.DAY_OF_YEAR, days);
                        oneTimeExpiry = cal.getTimeInMillis();
                        lunchExpiry = 0;
                        dinnerExpiry = 0;
                    } else {
                        if (mealType.equals("LUNCH") || mealType.equals("BOTH")) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(lunchExpiry);
                            cal.add(Calendar.DAY_OF_YEAR, days);
                            lunchExpiry = cal.getTimeInMillis();
                        }
                        if (mealType.equals("DINNER") || mealType.equals("BOTH")) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(dinnerExpiry);
                            cal.add(Calendar.DAY_OF_YEAR, days);
                            dinnerExpiry = cal.getTimeInMillis();
                        }
                        oneTimeExpiry = 0;
                    }
                    generalExpiry = Math.max(oneTimeExpiry, Math.max(lunchExpiry, dinnerExpiry));

                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("subscriptionExpiry", generalExpiry);
                    userUpdate.put("lunchSubscriptionExpiry", lunchExpiry);
                    userUpdate.put("dinnerSubscriptionExpiry", dinnerExpiry);
                    userUpdate.put("oneTimeMealExpiry", oneTimeExpiry);
                    userUpdate.put("subscriptionType", mealType);

                    String transId = db.collection("transactions").document().getId();
                    Transaction transaction = new Transaction(
                            transId,
                            currentMessId,
                            request.getStudentId(),
                            amount,
                            days,
                            System.currentTimeMillis(),
                            mealType);

                    // Use a batch so user-update, status-update, and transaction are atomic
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    batch.update(db.collection("users").document(request.getStudentId()), userUpdate);
                    batch.update(db.collection("subscriptionRequests").document(request.getId()), "status", "GRANTED");
                    batch.set(db.collection("transactions").document(transId), transaction);
                    final long notificationExpiry = generalExpiry;

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                String senderId = mAuth.getCurrentUser() != null
                                        ? mAuth.getCurrentUser().getUid() : "";
                                MessNotificationManager.sendSubscriptionGranted(
                                        request.getMessId(),
                                        senderId,
                                        request.getStudentId(),
                                        mealType,
                                        days,
                                        notificationExpiry);
                                Toast.makeText(getContext(), "Subscription granted!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching student: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void deleteSubscriptionRequest(SubscriptionRequest request) {
        if (binding == null)
            return;
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Request")
                .setMessage("Delete this subscription request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    db.collection("subscriptionRequests").document(request.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Request deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void displayCurrentDate() {
        binding.textMessDashboardDate.setText(
                new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(new Date()));
    }

    private void setupMessConditionButtons() {
        binding.btnConditionFull.setOnClickListener(v -> updateMessCondition("FULL"));
        binding.btnConditionHalf.setOnClickListener(v -> updateMessCondition("HALF"));
        binding.btnConditionEmpty.setOnClickListener(v -> updateMessCondition("EMPTY"));
        binding.btnConditionNotConform.setOnClickListener(v -> updateMessCondition("NOT_CONFORM"));
    }

    private void updateMessCondition(String condition) {
        if (currentMessId == null) {
            Toast.makeText(getContext(), "Mess ID not available", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("messCondition", condition);
        data.put("lastUpdated", System.currentTimeMillis());
        db.collection("mess_settings").document(currentMessId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Condition updated: " + condition, Toast.LENGTH_SHORT).show();
                    updateConditionUI(condition);
                })
                .addOnFailureListener(
                        e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadMessCondition() {
        if (currentMessId == null || binding == null)
            return;
        db.collection("mess_settings").document(currentMessId)
                .addSnapshotListener((snap, e) -> {
                    if (binding == null || e != null)
                        return;
                    if (snap != null && snap.exists()) {
                        String c = snap.getString("messCondition");
                        if (c != null)
                            updateConditionUI(c);
                        else {
                            binding.textCurrentCondition.setText("Current: Not Set");
                            resetConditionButtons();
                        }
                    } else {
                        binding.textCurrentCondition.setText("Current: Not Set");
                        resetConditionButtons();
                    }
                });
    }

    private void updateConditionUI(String condition) {
        if (binding == null)
            return;
        binding.textCurrentCondition.setText("Current: " + condition);
        resetConditionButtons();
        switch (condition) {
            case "FULL":
                binding.btnConditionFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444));
                binding.btnConditionFull.setTextColor(0xFFFFFFFF);
                binding.btnConditionFull.setStrokeWidth(0);
                break;
            case "HALF":
                binding.btnConditionHalf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF59E0B));
                binding.btnConditionHalf.setTextColor(0xFFFFFFFF);
                binding.btnConditionHalf.setStrokeWidth(0);
                break;
            case "EMPTY":
                binding.btnConditionEmpty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                binding.btnConditionEmpty.setTextColor(0xFFFFFFFF);
                binding.btnConditionEmpty.setStrokeWidth(0);
                break;
            case "NOT_CONFORM":
                binding.btnConditionNotConform
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6B7280));
                binding.btnConditionNotConform.setTextColor(0xFFFFFFFF);
                binding.btnConditionNotConform.setStrokeWidth(0);
                binding.textCurrentCondition.setText("Current: Not Conform");
                break;
        }
    }

    private void resetConditionButtons() {
        if (binding == null)
            return;
        binding.btnConditionFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionFull.setTextColor(0xFFEF4444);
        binding.btnConditionFull.setStrokeWidth(2);
        binding.btnConditionHalf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionHalf.setTextColor(0xFFF59E0B);
        binding.btnConditionHalf.setStrokeWidth(2);
        binding.btnConditionEmpty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionEmpty.setTextColor(0xFF10B981);
        binding.btnConditionEmpty.setStrokeWidth(2);
        binding.btnConditionNotConform.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionNotConform.setTextColor(0xFF6B7280);
        binding.btnConditionNotConform.setStrokeWidth(2);
    }

    private void fetchMessOwnerData() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to view dashboard", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null)
                        return;
                    if (doc.exists()) {
                        currentMessId = doc.getString("messId");
                        if (currentMessId != null) {
                            fetchMessDetails(currentMessId);
                            loadMessCondition();
                            fetchMessSettings();
                            setupRealtimeMealListener(currentMessId); // FIX #4: single listener
                            setupRealtimeRequestListeners(currentMessId);
                            setupRealtimeStudentsCountListener(currentMessId);
                        } else {
                            Toast.makeText(getContext(), "Mess ID not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRealtimeRequestListeners(String messId) {
        requestsListener = db.collection("subscriptionRequests")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || e != null)
                        return;
                    if (snapshots != null) {
                        pendingRequests.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            SubscriptionRequest req = doc.toObject(SubscriptionRequest.class);
                            if (req != null)
                                pendingRequests.add(req);
                        }
                        requestAdapter.setRequests(pendingRequests);
                    }
                });
    }

    private void fetchMessDetails(String messId) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null)
                        return;
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null)
                            binding.textMessDashboardName.setText(name);
                    } else {
                        Toast.makeText(getContext(), "Mess details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRealtimeStudentsCountListener(String messId) {
        studentsCountListener = db.collection("users")
                .whereEqualTo("messId", messId)
                .whereEqualTo("role", "USER")
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || e != null)
                        return;
                    if (snapshots != null) {
                        userDocs.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            userDocs.put(doc.getId(), doc);
                        }
                        updateDefaultInMealCounts();
                    }
                });
    }

    // FIX #4: Single Firestore listener reads both lunch and dinner from the same
    // snapshot. Previously two identical queries were registered, doubling read
    // costs.
    private void setupRealtimeMealListener(String messId) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        mealListener = db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("date", todayDate)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                            @Nullable FirebaseFirestoreException e) {
                        if (binding == null || e != null)
                            return;

                        mealDocs.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String uId = doc.getString("userId");
                                if (uId != null)
                                    mealDocs.put(uId, doc);
                            }
                        }
                        updateDefaultInMealCounts();
                    }
                });
    }

    private void updateDefaultInMealCounts() {
        if (binding == null)
            return;

        int totalActive = 0;
        int lunchInCount = 0;
        int lunchOutCount = 0;
        int dinnerInCount = 0;
        int dinnerOutCount = 0;
        long now = System.currentTimeMillis();

        for (DocumentSnapshot userDoc : userDocs.values()) {
            Long expiry = userDoc.getLong("subscriptionExpiry");
            Long lunchExpiry = userDoc.getLong("lunchSubscriptionExpiry");
            Long dinnerExpiry = userDoc.getLong("dinnerSubscriptionExpiry");
            Long oneTimeExpiry = userDoc.getLong("oneTimeMealExpiry");
            String subType = userDoc.getString("subscriptionType");
            String autoSelect = userDoc.getString("oneTimeAutoSelect");

            if (expiry != null && expiry > now)
                totalActive++;

            boolean isOneTime = "ONE_TIME".equals(subType) && oneTimeExpiry != null && oneTimeExpiry > now;

            long lunchExp = lunchExpiry != null && lunchExpiry > 0 ? lunchExpiry : (expiry != null ? expiry : 0);
            long dinnerExp = dinnerExpiry != null && dinnerExpiry > 0 ? dinnerExpiry : (expiry != null ? expiry : 0);

            boolean isRegLunchActive = !isOneTime && lunchExp > now;
            boolean isRegDinnerActive = !isOneTime && dinnerExp > now;

            DocumentSnapshot mealDoc = mealDocs.get(userDoc.getId());
            String explicitLunch = mealDoc != null ? mealDoc.getString("lunch") : null;
            String explicitDinner = mealDoc != null ? mealDoc.getString("dinner") : null;

            // Lunch logic
            if (isOneTime) {
                if ("IN".equals(explicitLunch)) {
                    lunchInCount++;
                } else if ("LUNCH".equals(autoSelect)) {
                    if (!"OUT".equals(explicitLunch))
                        lunchInCount++;
                    else
                        lunchOutCount++;
                }
            } else if (isRegLunchActive) {
                boolean autoLunch = Boolean.TRUE.equals(userDoc.getBoolean("autoSelectLunch"));
                if (autoLunch) {
                    if ("OUT".equals(explicitLunch))
                        lunchOutCount++;
                    else
                        lunchInCount++;
                } else {
                    if ("IN".equals(explicitLunch))
                        lunchInCount++;
                    else
                        lunchOutCount++;
                }
            }

            // Dinner logic
            if (isOneTime) {
                if ("IN".equals(explicitDinner)) {
                    dinnerInCount++;
                } else if ("DINNER".equals(autoSelect)) {
                    if (!"OUT".equals(explicitDinner))
                        dinnerInCount++;
                    else
                        dinnerOutCount++;
                }
            } else if (isRegDinnerActive) {
                boolean autoDinner = Boolean.TRUE.equals(userDoc.getBoolean("autoSelectDinner"));
                if (autoDinner) {
                    if ("OUT".equals(explicitDinner))
                        dinnerOutCount++;
                    else
                        dinnerInCount++;
                } else {
                    if ("IN".equals(explicitDinner))
                        dinnerInCount++;
                    else
                        dinnerOutCount++;
                }
            }
        }

        binding.textTotalStudents.setText(String.valueOf(totalActive));
        binding.textLunchInCount.setText(String.valueOf(lunchInCount));
        binding.textLunchOutCount.setText(String.valueOf(lunchOutCount));
        binding.textDinnerInCount.setText(String.valueOf(dinnerInCount));
        binding.textDinnerOutCount.setText(String.valueOf(dinnerOutCount));

        // Compute breakfast selections count breakdown
        Map<String, Integer> breakfastCounts = new HashMap<>();
        int breakfastInTotal = 0;

        for (DocumentSnapshot mealDoc : mealDocs.values()) {
            String breakfastStatus = mealDoc.getString("breakfast");
            if ("IN".equals(breakfastStatus)) {
                String item = mealDoc.getString("breakfast_item");
                if (item == null || item.trim().isEmpty()) {
                    item = "Standard/Default";
                } else {
                    item = item.trim();
                }
                breakfastCounts.put(item, breakfastCounts.getOrDefault(item, 0) + 1);
                breakfastInTotal++;
            }
        }

        if (breakfastInTotal == 0) {
            binding.textBreakfastCountsBreakdown.setText("No student selections today yet.");
        } else {
            StringBuilder breakdown = new StringBuilder();
            breakdown.append("Total Selections: ").append(breakfastInTotal).append("\n\n");
            for (Map.Entry<String, Integer> entry : breakfastCounts.entrySet()) {
                breakdown.append("• ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append(" student(s)\n");
            }
            binding.textBreakfastCountsBreakdown.setText(breakdown.toString().trim());
        }
    }

    private void fetchMessSettings() {
        if (currentMessId == null)
            return;
        db.collection("mess_settings").document(currentMessId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists())
                        return;
                    Long lh = snap.getLong("lunchCutoffHour");
                    Long lm = snap.getLong("lunchCutoffMinute");
                    Long dh = snap.getLong("dinnerCutoffHour");
                    Long dm = snap.getLong("dinnerCutoffMinute");
                    if (lh != null)
                        lunchCutoffHour = lh.intValue();
                    if (lm != null)
                        lunchCutoffMinute = lm.intValue();
                    if (dh != null)
                        dinnerCutoffHour = dh.intValue();
                    if (dm != null)
                        dinnerCutoffMinute = dm.intValue();
                    startDashboardTimer();
                });
    }

    private void startDashboardTimer() {
        if (dashboardTimer != null)
            dashboardTimer.cancel();
        dashboardTimer = new android.os.CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long ms) {
                if (binding == null)
                    return;
                Calendar now = Calendar.getInstance();
                Calendar target = (Calendar) now.clone();
                String label = "Lunch";
                target.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                target.set(Calendar.MINUTE, lunchCutoffMinute);
                target.set(Calendar.SECOND, 0);

                if (now.after(target)) {
                    target.set(Calendar.HOUR_OF_DAY, dinnerCutoffHour);
                    target.set(Calendar.MINUTE, dinnerCutoffMinute);
                    label = "Dinner";
                    if (now.after(target)) {
                        target.add(Calendar.DAY_OF_YEAR, 1);
                        target.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                        target.set(Calendar.MINUTE, lunchCutoffMinute);
                        label = "Tomorrow's Lunch";
                    }
                }

                long diff = target.getTimeInMillis() - now.getTimeInMillis();
                if (diff > 0) {
                    binding.layoutDeadlineContainer.setVisibility(View.VISIBLE);
                    long h = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff);
                    long m = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long s = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    binding.textDashboardTimer.setText(
                            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
                }
            }

            @Override
            public void onFinish() {
            }
        };
        dashboardTimer.start();
    }

    private void showResetConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reset All Attendance")
                .setMessage("This will clear ALL student IN/OUT selections for today. Continue?")
                .setPositiveButton("Reset All", (d, w) -> resetAttendance())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetAttendance() {
        if (currentMessId == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.collection("meal_selections")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No records for today.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : query) {
                        String uId = doc.getString("userId");
                        if (uId != null) {
                            DocumentSnapshot userDoc = userDocs.get(uId);
                            if (userDoc != null) {
                                boolean isOneTime = "ONE_TIME".equals(userDoc.getString("subscriptionType"));
                                if (!isOneTime) {
                                    String lunch = doc.getString("lunch");
                                    String dinner = doc.getString("dinner");

                                    long lunchExpiry = userDoc.getLong("lunchSubscriptionExpiry") != null
                                            ? userDoc.getLong("lunchSubscriptionExpiry")
                                            : 0;
                                    long dinnerExpiry = userDoc.getLong("dinnerSubscriptionExpiry") != null
                                            ? userDoc.getLong("dinnerSubscriptionExpiry")
                                            : 0;

                                    boolean updated = false;
                                    if ("OUT".equals(lunch) && lunchExpiry > 0) {
                                        lunchExpiry = Math.max(System.currentTimeMillis(),
                                                lunchExpiry - (24 * 60 * 60 * 1000L));
                                        updated = true;
                                    }
                                    if ("OUT".equals(dinner) && dinnerExpiry > 0) {
                                        dinnerExpiry = Math.max(System.currentTimeMillis(),
                                                dinnerExpiry - (24 * 60 * 60 * 1000L));
                                        updated = true;
                                    }

                                    if (updated) {
                                        long generalExpiry = Math.max(lunchExpiry, dinnerExpiry);
                                        batch.update(userDoc.getReference(), "lunchSubscriptionExpiry", lunchExpiry,
                                                "dinnerSubscriptionExpiry", dinnerExpiry,
                                                "subscriptionExpiry", generalExpiry);
                                    }
                                }
                            }
                        }
                        Map<String, Object> resetData = new HashMap<>();
                        resetData.put("lunch", "RESET");
                        resetData.put("dinner", "RESET");
                        resetData.put("timestamp", System.currentTimeMillis());
                        batch.set(doc.getReference(), resetData, com.google.firebase.firestore.SetOptions.merge());
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Attendance cleared!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(ex -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mealListener != null)
            mealListener.remove();
        if (requestsListener != null)
            requestsListener.remove();
        if (studentsCountListener != null)
            studentsCountListener.remove();
        if (dashboardTimer != null)
            dashboardTimer.cancel();
        binding = null;
    }
}
