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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.messapp.adapters.SubscriptionRequestAdapter;
import com.example.messapp.models.SubscriptionRequest;
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
    private ListenerRegistration lunchListener;
    private ListenerRegistration dinnerListener;
    private ListenerRegistration studentsCountListener; // Added
    private SubscriptionRequestAdapter requestAdapter;
    private List<SubscriptionRequest> pendingRequests = new ArrayList<>();
    private ListenerRegistration requestsListener;
    private android.os.CountDownTimer dashboardTimer;
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

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Grant", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String daysStr = etDays.getText().toString().trim();

                    if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(daysStr)) {
                        Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String mealType = "BOTH";
                    int selectedId = radioGroupMealType.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch)
                        mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner)
                        mealType = "DINNER";

                    grantSubscription(request, Double.parseDouble(amountStr), Integer.parseInt(daysStr), mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void grantSubscription(SubscriptionRequest request, double amount, int days, String mealType) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(View.VISIBLE);

        // 1. Get current student data to find current expiry
        db.collection("users").document(request.getStudentId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    long lunchExpiry = System.currentTimeMillis();
                    long dinnerExpiry = System.currentTimeMillis();
                    long generalExpiry = System.currentTimeMillis();

                    if (documentSnapshot.exists()) {
                        Long existingL = documentSnapshot.getLong("lunchSubscriptionExpiry");
                        Long existingD = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                        Long existingG = documentSnapshot.getLong("subscriptionExpiry");

                        if (existingL != null && existingL > lunchExpiry)
                            lunchExpiry = existingL;
                        if (existingD != null && existingD > dinnerExpiry)
                            dinnerExpiry = existingD;
                        if (existingG != null && existingG > generalExpiry)
                            generalExpiry = existingG;
                    }

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

                    generalExpiry = Math.max(lunchExpiry, dinnerExpiry);

                    // 3. Update user doc
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("subscriptionExpiry", generalExpiry);
                    userUpdate.put("lunchSubscriptionExpiry", lunchExpiry);
                    userUpdate.put("dinnerSubscriptionExpiry", dinnerExpiry);

                    db.collection("users").document(request.getStudentId()).update(userUpdate)
                            .addOnSuccessListener(aVoid -> {
                                // 4. Mark request as GRANTED
                                db.collection("subscriptionRequests").document(request.getId())
                                        .update("status", "GRANTED")
                                        .addOnSuccessListener(aVoid2 -> {
                                            if (binding == null)
                                                return;
                                            binding.progressBar.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Subscription granted successfully!",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to grant: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching student data: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void deleteSubscriptionRequest(SubscriptionRequest request) {
        if (binding == null)
            return;

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to delete this subscription request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    db.collection("subscriptionRequests").document(request.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Request deleted successfully!", Toast.LENGTH_SHORT)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null)
                                    return;
                                binding.progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void displayCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        binding.textMessDashboardDate.setText(currentDate);
        // Removed textTodayDateMess - date is now displayed in header only
    }

    private void setupMessConditionButtons() {
        binding.btnConditionFull.setOnClickListener(v -> updateMessCondition("FULL"));
        binding.btnConditionHalf.setOnClickListener(v -> updateMessCondition("HALF"));
        binding.btnConditionEmpty.setOnClickListener(v -> updateMessCondition("EMPTY"));
    }

    private void updateMessCondition(String condition) {
        if (currentMessId == null) {
            Toast.makeText(getContext(), "Mess ID not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> conditionData = new HashMap<>();
        conditionData.put("messCondition", condition);
        conditionData.put("lastUpdated", System.currentTimeMillis());

        db.collection("mess_settings").document(currentMessId)
                .set(conditionData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mess condition updated to " + condition, Toast.LENGTH_SHORT).show();
                    updateConditionUI(condition);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error updating condition: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void loadMessCondition() {
        if (currentMessId == null || binding == null)
            return;

        db.collection("mess_settings").document(currentMessId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (binding == null)
                        return;
                    if (e != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String condition = documentSnapshot.getString("messCondition");
                        if (condition != null) {
                            updateConditionUI(condition);
                        } else {
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
        }
    }

    private void resetConditionButtons() {
        if (binding == null)
            return;
        // Reset all buttons to outlined style
        binding.btnConditionFull.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionFull.setTextColor(0xFFEF4444);
        binding.btnConditionFull.setStrokeWidth(2);

        binding.btnConditionHalf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionHalf.setTextColor(0xFFF59E0B);
        binding.btnConditionHalf.setStrokeWidth(2);

        binding.btnConditionEmpty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        binding.btnConditionEmpty.setTextColor(0xFF10B981);
        binding.btnConditionEmpty.setStrokeWidth(2);
    }

    private void fetchMessOwnerData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMessDetails(currentMessId);
                            loadMessCondition(); // Ensure conditions are loaded
                            fetchMessSettings(); // New
                            setupRealtimeMealListeners(currentMessId);
                            setupRealtimeRequestListeners(currentMessId);
                            setupRealtimeStudentsCountListener(currentMessId);
                        } else {
                            Toast.makeText(getContext(), "Mess ID not found for this user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), "Error fetching mess owner data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRealtimeRequestListeners(String messId) {
        requestsListener = db.collection("subscriptionRequests")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null)
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
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        String messName = documentSnapshot.getString("name");

                        if (messName != null) {
                            binding.textMessDashboardName.setText(messName);
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), "Error fetching mess details: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void setupRealtimeStudentsCountListener(String messId) {
        studentsCountListener = db.collection("users")
                .whereEqualTo("messId", messId)
                .whereEqualTo("role", "USER")
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null)
                        return;
                    if (snapshots != null) {
                        int activeCount = 0;
                        long currentTime = System.currentTimeMillis();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Long expiry = doc.getLong("subscriptionExpiry");
                            if (expiry != null && expiry > currentTime) {
                                activeCount++;
                            }
                        }
                        binding.textTotalStudents.setText(String.valueOf(activeCount));
                    }
                });
    }

    private void setupRealtimeMealListeners(String messId) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Lunch Listener - Query meal_selections collection
        lunchListener = db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("date", todayDate)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (binding == null)
                            return;
                        if (e != null) {
                            return;
                        }

                        int lunchInCount = 0;
                        int lunchOutCount = 0;

                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String lunchStatus = doc.getString("lunch");
                                if ("IN".equals(lunchStatus)) {
                                    lunchInCount++;
                                } else if ("OUT".equals(lunchStatus)) {
                                    lunchOutCount++;
                                }
                            }
                        }

                        binding.textLunchInCount.setText(String.valueOf(lunchInCount));
                        binding.textLunchOutCount.setText(String.valueOf(lunchOutCount));
                    }
                });

        // Dinner Listener - Same collection, check dinner field
        dinnerListener = db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("date", todayDate)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (binding == null)
                            return;
                        if (e != null) {
                            return;
                        }

                        int dinnerInCount = 0;
                        int dinnerOutCount = 0;

                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                String dinnerStatus = doc.getString("dinner");
                                if ("IN".equals(dinnerStatus)) {
                                    dinnerInCount++;
                                } else if ("OUT".equals(dinnerStatus)) {
                                    dinnerOutCount++;
                                }
                            }
                        }

                        binding.textDinnerInCount.setText(String.valueOf(dinnerInCount));
                        binding.textDinnerOutCount.setText(String.valueOf(dinnerOutCount));
                    }
                });
    }

    private void fetchMessSettings() {
        if (currentMessId == null)
            return;
        db.collection("mess_settings").document(currentMessId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null || documentSnapshot == null || !documentSnapshot.exists())
                        return;

                    Long lh = documentSnapshot.getLong("lunchCutoffHour");
                    Long lm = documentSnapshot.getLong("lunchCutoffMinute");
                    Long dh = documentSnapshot.getLong("dinnerCutoffHour");
                    Long dm = documentSnapshot.getLong("dinnerCutoffMinute");

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
            public void onTick(long millisUntilFinished) {
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
                    binding.textDashboardTimer.setVisibility(View.VISIBLE);
                    long h = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff);
                    long m = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long s = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    binding.textDashboardTimer
                            .setText(String.format(Locale.getDefault(), "%s Deadline: %02d:%02d:%02d", label, h, m, s));
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
                .setMessage(
                        "This will clear ALL student IN/OUT selections for today. Students will be able to re-mark their status if the deadline hasn't passed. Continue?")
                .setPositiveButton("Reset All", (dialog, which) -> resetAttendance())
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
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "No attendance records found for today.", Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        if (binding == null)
                            return;
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "All attendance records cleared for today!", Toast.LENGTH_SHORT)
                                .show();
                    }).addOnFailureListener(e -> {
                        if (binding == null)
                            return;
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to reset: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (lunchListener != null) {
            lunchListener.remove();
        }
        if (dinnerListener != null) {
            dinnerListener.remove();
        }
        if (requestsListener != null) {
            requestsListener.remove();
        }
        if (studentsCountListener != null) {
            studentsCountListener.remove();
        }
        if (dashboardTimer != null) {
            dashboardTimer.cancel();
        }
        binding = null;
    }
}
