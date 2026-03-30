package com.example.messapp.ui.user.home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.messapp.databinding.FragmentUserHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserHomeFragment extends Fragment {

    private FragmentUserHomeBinding binding;
    private FirebaseFirestore db;
    private String userId;
    private String messId;
    private String todayDate;
    private boolean isLunchSubscribed = false;
    private boolean isDinnerSubscribed = false;
    private boolean allowMultipleChanges = false;
    private CountDownTimer timer;

    // Cutoff times (defaults)
    private int lunchCutoffHour = 10;
    private int lunchCutoffMinute = 30;
    private int dinnerCutoffHour = 16;
    private int dinnerCutoffMinute = 30;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Set Date
        binding.textDate.setText(new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        fetchUserDetails();
    }

    private void fetchUserDetails() {
        if (userId == null)
            return;

        db.collection("users").document(userId).addSnapshotListener((documentSnapshot, e) -> {
            if (binding == null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                messId = documentSnapshot.getString("messId");
                Long lunchExpiry = documentSnapshot.getLong("lunchSubscriptionExpiry");
                Long dinnerExpiry = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                Long generalExpiry = documentSnapshot.getLong("subscriptionExpiry"); // Fallback

                String name = documentSnapshot.getString("name");
                if (name != null) {
                    binding.textGreeting.setText("Hello, " + name.split(" ")[0] + "!");
                }

                String preference = documentSnapshot.getString("dietaryPreference");
                if (preference != null && !preference.isEmpty()) {
                    binding.textLunchPreference.setText(preference.toUpperCase());
                    binding.textLunchPreference.setVisibility(View.VISIBLE);
                    binding.textDinnerPreference.setText(preference.toUpperCase());
                    binding.textDinnerPreference.setVisibility(View.VISIBLE);

                    // Set color based on preference
                    int color = preference.equalsIgnoreCase("Veg") ? Color.parseColor("#4CAF50")
                            : Color.parseColor("#F44336");
                    binding.textLunchPreference.setTextColor(color);
                    binding.textDinnerPreference.setTextColor(color);
                } else {
                    binding.textLunchPreference.setVisibility(View.GONE);
                    binding.textDinnerPreference.setVisibility(View.GONE);
                }

                checkSubscription(lunchExpiry, dinnerExpiry, generalExpiry);
                fetchMessSettings(); // Fetch cutoff times
                loadMenu();
                listenToMySelection();
                listenToMessCondition(); // Listen to mess condition updates
            }
        });
    }

    private void checkSubscription(Long lunchExpiry, Long dinnerExpiry, Long generalExpiry) {
        long now = System.currentTimeMillis();

        // Use specific expiry if available, otherwise fallback to general
        long lExp = (lunchExpiry != null && lunchExpiry > 0) ? lunchExpiry
                : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);
        long dExp = (dinnerExpiry != null && dinnerExpiry > 0) ? dinnerExpiry
                : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);

        isLunchSubscribed = lExp > now;
        isDinnerSubscribed = dExp > now;

        // Update UI to reflect subscription status
        if (binding != null) {
            updateButtonStatesBasedOnSubscription();
        }
    }

    private void updateButtonStatesBasedOnSubscription() {
        if (binding == null)
            return;

        // This will be called after subscription check to update button states
        // The actual state update happens in updateButtonUI which is called from
        // listenToMySelection
        // But we can also update here if needed for immediate feedback
    }

    private void fetchMessSettings() {
        if (messId == null)
            return;

        db.collection("mess_settings").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        Long lunchHour = documentSnapshot.getLong("lunchCutoffHour");
                        Long lunchMinute = documentSnapshot.getLong("lunchCutoffMinute");
                        Long dinnerHour = documentSnapshot.getLong("dinnerCutoffHour");
                        Long dinnerMinute = documentSnapshot.getLong("dinnerCutoffMinute");

                        if (lunchHour != null)
                            lunchCutoffHour = lunchHour.intValue();
                        if (lunchMinute != null)
                            lunchCutoffMinute = lunchMinute.intValue();
                        if (dinnerHour != null)
                            dinnerCutoffHour = dinnerHour.intValue();
                        if (dinnerMinute != null)
                            dinnerCutoffMinute = dinnerMinute.intValue();

                        Boolean amc = documentSnapshot.getBoolean("allowMultipleChanges");
                        allowMultipleChanges = (amc != null && amc);
                    }
                    // After fetching settings, setup UI
                    setupClickListeners();
                    startDeadlineTimer();
                });
    }

    private void loadMenu() {
        if (messId == null)
            return;

        db.collection("menus").document(messId + "_" + todayDate).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        String lunch = documentSnapshot.getString("lunch");
                        String dinner = documentSnapshot.getString("dinner");

                        binding.textLunchMenuNew
                                .setText(lunch != null && !lunch.isEmpty() ? lunch : "menu is not set");
                        binding.textDinnerMenuNew
                                .setText(dinner != null && !dinner.isEmpty() ? dinner : "menu is not set");
                    } else {
                        binding.textLunchMenuNew.setText("menu is not set");
                        binding.textDinnerMenuNew.setText("menu is not set");
                    }
                });
    }

    private void listenToMySelection() {
        if (messId == null || userId == null)
            return;

        db.collection("meal_selections").document(messId + "_" + todayDate + "_" + userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String lunchStatus = documentSnapshot.getString("lunch");
                        String dinnerStatus = documentSnapshot.getString("dinner");

                        updateButtonUI("LUNCH", lunchStatus);
                        updateButtonUI("DINNER", dinnerStatus);
                    } else {
                        // No selection made yet - show red status
                        updateButtonUI("LUNCH", null);
                        updateButtonUI("DINNER", null);
                    }
                });
    }

    private void setupClickListeners() {
        binding.btnLunchInNew.setOnClickListener(v -> markAttendance("LUNCH", "IN"));
        binding.btnLunchOutNew.setOnClickListener(v -> markAttendance("LUNCH", "OUT"));
        binding.btnDinnerInNew.setOnClickListener(v -> markAttendance("DINNER", "IN"));
        binding.btnDinnerOutNew.setOnClickListener(v -> markAttendance("DINNER", "OUT"));
    }

    private void markAttendance(String mealType, String status) {
        if (binding == null || messId == null || userId == null) {
            Toast.makeText(getContext(), "Error: Missing required information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check subscription status
        boolean active = mealType.equals("LUNCH") ? isLunchSubscribed : isDinnerSubscribed;
        if (!active) {
            Toast.makeText(getContext(), mealType + " Subscription Expired! Please renew your subscription.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check if cutoff time has passed
        if (isCutoffPassed(mealType)) {
            String cutoffTime = mealType.equals("LUNCH")
                    ? String.format(Locale.getDefault(), "%02d:%02d", lunchCutoffHour, lunchCutoffMinute)
                    : String.format(Locale.getDefault(), "%02d:%02d", dinnerCutoffHour, dinnerCutoffMinute);
            Toast.makeText(getContext(),
                    "Cutoff time (" + cutoffTime + ") has passed for " + mealType + ". Cannot mark attendance now.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Validate date - can't mark for past dates
        Calendar today = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate.setTime(sdf.parse(todayDate));
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            selectedDate.set(Calendar.HOUR_OF_DAY, 0);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            if (selectedDate.before(today)) {
                Toast.makeText(getContext(), "Cannot mark attendance for past dates", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            // Date parsing error - continue anyway
        }

        Map<String, Object> data = new HashMap<>();
        String typeKey = mealType.equals("LUNCH") ? "lunch" : "dinner";

        data.put(typeKey, status);
        data.put("userId", userId);
        data.put("date", todayDate);
        data.put("messId", messId);
        data.put("timestamp", System.currentTimeMillis()); // Add timestamp for tracking

        db.collection("meal_selections").document(messId + "_" + todayDate + "_" + userId)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), mealType + " marked as " + status + " successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null)
                        return;
                    Toast.makeText(getContext(), "Error marking attendance: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void updateButtonUI(String mealType, String status) {
        if (binding == null)
            return;

        int colorPrimary = Color.parseColor("#13ec13"); // dash_primary (green)
        int colorTextNormal = Color.parseColor("#111811");
        int colorTextMuted = Color.parseColor("#618961");
        int colorGray = Color.LTGRAY;

        // Status bar colors
        int colorGreen = Color.parseColor("#4CAF50"); // Green for IN
        int colorYellow = Color.parseColor("#FF9800"); // Orange/Yellow for OUT
        int colorRed = Color.parseColor("#F44336"); // Red for not marked
        int colorDisabled = Color.parseColor("#9CA3AF"); // Gray for disabled

        // Check subscription and cutoff for button state
        boolean isSubscribed = mealType.equals("LUNCH") ? isLunchSubscribed : isDinnerSubscribed;
        boolean cutoffPassed = isCutoffPassed(mealType);
        boolean canMark = isSubscribed && !cutoffPassed;

        if (mealType.equals("LUNCH")) {
            if (status == null) {
                // Not marked - show red status
                binding.textLunchStatusBar.setText("Status: Not Marked");
                binding.textLunchStatusBar.setBackgroundColor(colorRed);
                binding.textLunchStatusBar.setTextColor(Color.WHITE);

                // Enable/disable buttons based on subscription and cutoff
                binding.btnLunchInNew.setEnabled(canMark);
                binding.btnLunchOutNew.setEnabled(canMark);

                // Visual feedback for disabled state
                if (!canMark) {
                    if (!isSubscribed) {
                        binding.textLunchStatusBar.setText("Status: Subscription Expired");
                        binding.textLunchStatusBar.setBackgroundColor(colorDisabled);
                    } else if (cutoffPassed) {
                        binding.textLunchStatusBar.setText("Status: Cutoff Time Passed");
                        binding.textLunchStatusBar.setBackgroundColor(colorDisabled);
                    }
                }
                return;
            }
            if ("IN".equals(status)) {
                // IN Selected
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnLunchInNew.setTextColor(colorTextNormal);

                binding.btnLunchOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnLunchOutNew.setTextColor(colorTextMuted);

                // Disable buttons after marking (Unless multiple changes allowed and deadline
                // not passed)
                boolean canToggle = canMark && allowMultipleChanges;
                binding.btnLunchInNew.setEnabled(false); // Current selection always disabled
                binding.btnLunchOutNew.setEnabled(canToggle);

                // Update status bar - GREEN for IN
                binding.textLunchStatus.setText("");
                binding.textLunchStatusBar.setText("Status: IN");
                binding.textLunchStatusBar.setBackgroundColor(colorGreen);
                binding.textLunchStatusBar.setTextColor(Color.WHITE);
            } else if ("OUT".equals(status)) {
                // OUT Selected
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnLunchInNew.setTextColor(colorTextMuted);

                binding.btnLunchOutNew.setBackgroundTintList(ColorStateList.valueOf(colorGray));
                binding.btnLunchOutNew.setTextColor(colorTextNormal);

                // Disable buttons after marking (Unless multiple changes allowed and deadline
                // not passed)
                boolean canToggle = canMark && allowMultipleChanges;
                binding.btnLunchInNew.setEnabled(canToggle);
                binding.btnLunchOutNew.setEnabled(false); // Current selection always disabled

                binding.textLunchStatus.setText("Marked OUT");
                binding.textLunchStatus.setTextColor(Color.GRAY);
                // Update status bar - YELLOW/ORANGE for OUT
                binding.textLunchStatusBar.setText("Status: OUT");
                binding.textLunchStatusBar.setBackgroundColor(colorYellow);
                binding.textLunchStatusBar.setTextColor(Color.WHITE);
            }
        } else {
            // DINNER Logic
            // Note: isSubscribed, cutoffPassed, and canMark are already declared at method
            // level
            // They are correctly set for DINNER based on the mealType check above

            if (status == null) {
                // Not marked - show red status
                binding.textDinnerStatusBar.setText("Status: Not Marked");
                binding.textDinnerStatusBar.setBackgroundColor(colorRed);
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);

                // Enable/disable buttons based on subscription and cutoff
                binding.btnDinnerInNew.setEnabled(canMark);
                binding.btnDinnerOutNew.setEnabled(canMark);

                // Visual feedback for disabled state
                if (!canMark) {
                    if (!isSubscribed) {
                        binding.textDinnerStatusBar.setText("Status: Subscription Expired");
                        binding.textDinnerStatusBar.setBackgroundColor(colorDisabled);
                    } else if (cutoffPassed) {
                        binding.textDinnerStatusBar.setText("Status: Cutoff Time Passed");
                        binding.textDinnerStatusBar.setBackgroundColor(colorDisabled);
                    }
                }
                return;
            }
            if ("IN".equals(status)) {
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnDinnerInNew.setTextColor(colorTextNormal);

                binding.btnDinnerOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnDinnerOutNew.setTextColor(colorTextMuted);

                // Disable buttons after marking (Unless multiple changes allowed and deadline
                // not passed)
                boolean canToggle = canMark && allowMultipleChanges;
                binding.btnDinnerInNew.setEnabled(false); // Current selection always disabled
                binding.btnDinnerOutNew.setEnabled(canToggle);

                // Update status bar - GREEN for IN
                binding.textDinnerStatus.setText("");
                binding.textDinnerStatusBar.setText("Status: IN");
                binding.textDinnerStatusBar.setBackgroundColor(colorGreen);
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);
            } else if ("OUT".equals(status)) {
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnDinnerInNew.setTextColor(colorTextMuted);

                binding.btnDinnerOutNew.setBackgroundTintList(ColorStateList.valueOf(colorGray));
                binding.btnDinnerOutNew.setTextColor(colorTextNormal);

                // Disable buttons after marking (Unless multiple changes allowed and deadline
                // not passed)
                boolean canToggle = canMark && allowMultipleChanges;
                binding.btnDinnerInNew.setEnabled(canToggle);
                binding.btnDinnerOutNew.setEnabled(false); // Current selection always disabled

                binding.textDinnerStatus.setText("Marked OUT");
                binding.textDinnerStatus.setTextColor(Color.GRAY);
                // Update status bar - YELLOW/ORANGE for OUT
                binding.textDinnerStatusBar.setText("Status: OUT");
                binding.textDinnerStatusBar.setBackgroundColor(colorYellow);
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);
            }
        }
    }

    private void startDeadlineTimer() {
        if (timer != null)
            timer.cancel();

        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null)
                    return;

                Calendar now = Calendar.getInstance();
                long nowMs = now.getTimeInMillis();

                // Lunch Deadline
                Calendar lunchTarget = (Calendar) now.clone();
                lunchTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                lunchTarget.set(Calendar.MINUTE, lunchCutoffMinute);
                lunchTarget.set(Calendar.SECOND, 0);

                long lunchDiff = lunchTarget.getTimeInMillis() - nowMs;
                updateCardTimer(binding.textLunchCardTimer, lunchDiff, "LUNCH");

                // Dinner Deadline
                Calendar dinnerTarget = (Calendar) now.clone();
                dinnerTarget.set(Calendar.HOUR_OF_DAY, dinnerCutoffHour);
                dinnerTarget.set(Calendar.MINUTE, dinnerCutoffMinute);
                dinnerTarget.set(Calendar.SECOND, 0);

                long dinnerDiff = dinnerTarget.getTimeInMillis() - nowMs;
                updateCardTimer(binding.textDinnerCardTimer, dinnerDiff, "DINNER");

                // Top (Main) Timer Logic - Same as before but integrated
                Calendar mainTarget = (Calendar) now.clone();
                String targetLabel = "LUNCH";
                mainTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                mainTarget.set(Calendar.MINUTE, lunchCutoffMinute);
                mainTarget.set(Calendar.SECOND, 0);

                if (now.after(mainTarget)) {
                    mainTarget.set(Calendar.HOUR_OF_DAY, dinnerCutoffHour);
                    mainTarget.set(Calendar.MINUTE, dinnerCutoffMinute);
                    targetLabel = "DINNER";
                    if (now.after(mainTarget)) {
                        mainTarget.add(Calendar.DAY_OF_YEAR, 1);
                        mainTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                        mainTarget.set(Calendar.MINUTE, lunchCutoffMinute);
                        targetLabel = "TOMORROW'S LUNCH";
                    }
                }

                binding.textDeadlineLabel.setText("UPCOMING DEADLINE: " + targetLabel);
                long diff = mainTarget.getTimeInMillis() - nowMs;

                if (diff > 0) {
                    long hours = TimeUnit.MILLISECONDS.toHours(diff);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

                    if (binding.textTimerHours != null)
                        binding.textTimerHours.setText(String.format(Locale.getDefault(), "%02d", hours));
                    if (binding.textTimerMins != null)
                        binding.textTimerMins.setText(String.format(Locale.getDefault(), "%02d", minutes));
                    if (binding.textTimerSecs != null)
                        binding.textTimerSecs.setText(String.format(Locale.getDefault(), "%02d", seconds));
                }
            }

            @Override
            public void onFinish() {
            }
        };
        timer.start();
    }

    private void updateCardTimer(TextView timerView, long diff, String type) {
        if (timerView == null)
            return;
        if (diff > 0) {
            timerView.setVisibility(View.VISIBLE);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;

            if (hours > 0) {
                timerView.setText(String.format(Locale.getDefault(), "%dh %02dm left", hours, minutes));
            } else {
                timerView.setText(String.format(Locale.getDefault(), "%02d:%02d left", minutes, seconds));
            }
            timerView.setTextColor(Color.parseColor("#EF4444")); // Red-ish
            timerView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        } else {
            timerView.setVisibility(View.VISIBLE);
            timerView.setText("LOCKED");
            timerView.setTextColor(Color.GRAY);
            timerView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
        }
    }

    private boolean isCutoffPassed(String mealType) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        if (mealType.equals("LUNCH")) {
            return (hour > lunchCutoffHour) || (hour == lunchCutoffHour && minute > lunchCutoffMinute);
        } else {
            return (hour > dinnerCutoffHour) || (hour == dinnerCutoffHour && minute > dinnerCutoffMinute);
        }
    }

    private void listenToMessCondition() {
        if (messId == null)
            return;

        db.collection("mess_settings").document(messId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (binding == null)
                        return;
                    if (e != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String condition = documentSnapshot.getString("messCondition");
                        if (condition != null) {
                            updateMessConditionDisplay(condition);
                        } else {
                            hideMessConditionCard();
                        }
                    } else {
                        hideMessConditionCard();
                    }
                });
    }

    private void updateMessConditionDisplay(String condition) {
        if (binding == null)
            return;

        binding.cardMessCondition.setVisibility(View.VISIBLE);
        View indicator = binding.indicatorMessCondition;

        switch (condition) {
            case "FULL":
                binding.textMessCondition.setText("Mess is FULL");
                binding.textMessCondition.setTextColor(Color.parseColor("#EF4444"));
                indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444));
                break;
            case "HALF":
                binding.textMessCondition.setText("Mess is HALF FULL");
                binding.textMessCondition.setTextColor(Color.parseColor("#F59E0B"));
                indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF59E0B));
                break;
            case "EMPTY":
                binding.textMessCondition.setText("Mess is EMPTY");
                binding.textMessCondition.setTextColor(Color.parseColor("#10B981"));
                indicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                break;
            default:
                hideMessConditionCard();
                return;
        }
    }

    private void hideMessConditionCard() {
        if (binding != null) {
            binding.cardMessCondition.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null)
            timer.cancel();
        binding = null;
    }
}
