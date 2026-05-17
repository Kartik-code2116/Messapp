package com.example.messapp.ui.user.home;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentUserHomeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserHomeFragment extends Fragment {

    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    private FragmentUserHomeBinding binding;
    private FirebaseFirestore db;
    private String userId;
    private String messId;
    private String todayDate;
    private boolean isLunchSubscribed = false;
    private boolean isDinnerSubscribed = false;
    private boolean isOneTimeSubscribed = false;  // ONE_TIME subscription active
    private boolean oneTimeMealUsedToday = false; // student already used their one meal today
    private boolean allowMultipleChanges = false;
    private CountDownTimer timer;
    private ListenerRegistration plannedOutListener;
    private String currentUserOneTimeAutoSelect;

    private boolean listenersAttached = false;

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

        setupClickListeners();
        animateFoodCards();
        fetchUserDetails();
    }

    private void animateFoodCards() {
        binding.cardBreakfast.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.card_fade_slide_up));

        binding.cardLunch.setTranslationY(18f);
        binding.cardLunch.setAlpha(0f);
        binding.cardLunch.animate().alpha(1f).translationY(0f).setStartDelay(90).setDuration(360).start();

        binding.cardDinner.setTranslationY(18f);
        binding.cardDinner.setAlpha(0f);
        binding.cardDinner.animate().alpha(1f).translationY(0f).setStartDelay(170).setDuration(360).start();
    }

    private void fetchUserDetails() {
        if (userId == null) return;

        db.collection("users").document(userId).addSnapshotListener((documentSnapshot, e) -> {
            if (binding == null) return;
            if (documentSnapshot != null && documentSnapshot.exists()) {
                messId = documentSnapshot.getString("messId");
                Long lunchExpiry   = documentSnapshot.getLong("lunchSubscriptionExpiry");
                Long dinnerExpiry  = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                Long generalExpiry = documentSnapshot.getLong("subscriptionExpiry");
                Long oneTimeExpiry = documentSnapshot.getLong("oneTimeMealExpiry");
                String subscriptionType = documentSnapshot.getString("subscriptionType");
                String oneTimeAutoSelect = documentSnapshot.getString("oneTimeAutoSelect");
                currentUserOneTimeAutoSelect = oneTimeAutoSelect;

                String preference = documentSnapshot.getString("dietaryPreference");
                if (preference != null && !preference.isEmpty()) {
                    binding.textLunchPreference.setText(preference.toUpperCase());
                    binding.textLunchPreference.setVisibility(View.VISIBLE);
                    binding.textDinnerPreference.setText(preference.toUpperCase());
                    binding.textDinnerPreference.setVisibility(View.VISIBLE);
                    int color = preference.equalsIgnoreCase("Veg")
                            ? themeColor(R.color.state_success)
                            : themeColor(R.color.state_error);
                    binding.textLunchPreference.setTextColor(color);
                    binding.textDinnerPreference.setTextColor(color);
                } else {
                    binding.textLunchPreference.setVisibility(View.GONE);
                    binding.textDinnerPreference.setVisibility(View.GONE);
                }

                checkSubscription(lunchExpiry, dinnerExpiry, generalExpiry, oneTimeExpiry, subscriptionType, oneTimeAutoSelect);
                fetchMessSettings();
                loadMenu();
                listenToMySelection();
                listenToPlannedOutDays();
                listenToMessCondition();
            }
        });
    }

    private void checkSubscription(Long lunchExpiry, Long dinnerExpiry, Long generalExpiry,
                                   Long oneTimeExpiry, String subscriptionType, String oneTimeAutoSelect) {
        long now = System.currentTimeMillis();

        // Check ONE_TIME first
        isOneTimeSubscribed = "ONE_TIME".equals(subscriptionType)
                && oneTimeExpiry != null && oneTimeExpiry > now;

        if (isOneTimeSubscribed) {
            // ONE_TIME: both cards render, but the standard lunch/dinner flags stay false
            isLunchSubscribed  = false;
            isDinnerSubscribed = false;
            binding.cardOneTimeAutoSelect.setVisibility(View.VISIBLE);
            
            // Set the radio button without triggering the listener
            binding.radioGroupAutoSelect.setOnCheckedChangeListener(null);
            if ("LUNCH".equals(oneTimeAutoSelect)) {
                binding.radioAutoLunch.setChecked(true);
            } else if ("DINNER".equals(oneTimeAutoSelect)) {
                binding.radioAutoDinner.setChecked(true);
            } else {
                binding.radioAutoNone.setChecked(true);
            }
            binding.radioGroupAutoSelect.setOnCheckedChangeListener((group, checkedId) -> {
                String selection = "NONE";
                if (checkedId == R.id.radio_auto_lunch) selection = "LUNCH";
                else if (checkedId == R.id.radio_auto_dinner) selection = "DINNER";
                updateAutoSelectPreference(selection);
            });
        } else {
            binding.cardOneTimeAutoSelect.setVisibility(View.GONE);
            long lExp = (lunchExpiry  != null && lunchExpiry  > 0) ? lunchExpiry
                    : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);
            long dExp = (dinnerExpiry != null && dinnerExpiry > 0) ? dinnerExpiry
                    : (generalExpiry != null && generalExpiry > 0 ? generalExpiry : 0);
            isLunchSubscribed  = lExp > now;
            isDinnerSubscribed = dExp > now;
        }
    }
    
    private void updateAutoSelectPreference(String selection) {
        if (userId == null) return;
        db.collection("users").document(userId)
                .update("oneTimeAutoSelect", selection)
                .addOnSuccessListener(aVoid -> {
                    if (binding != null) {
                        Toast.makeText(getContext(), "Auto-select preference saved", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchMessSettings() {
        if (messId == null) return;

        db.collection("mess_settings").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        Long lunchHour   = documentSnapshot.getLong("lunchCutoffHour");
                        Long lunchMinute = documentSnapshot.getLong("lunchCutoffMinute");
                        Long dinnerHour  = documentSnapshot.getLong("dinnerCutoffHour");
                        Long dinnerMinute = documentSnapshot.getLong("dinnerCutoffMinute");

                        if (lunchHour   != null) lunchCutoffHour   = lunchHour.intValue();
                        if (lunchMinute != null) lunchCutoffMinute = lunchMinute.intValue();
                        if (dinnerHour  != null) dinnerCutoffHour  = dinnerHour.intValue();
                        if (dinnerMinute != null) dinnerCutoffMinute = dinnerMinute.intValue();

                        Boolean amc = documentSnapshot.getBoolean("allowMultipleChanges");
                        allowMultipleChanges = (amc != null && amc);
                    }
                    startDeadlineTimer();
                });
    }

    private void loadMenu() {
        if (messId == null) return;

        db.collection("menus").document(messId + "_" + todayDate).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        String lunch  = documentSnapshot.getString("lunch");
                        String dinner = documentSnapshot.getString("dinner");
                        binding.textLunchMenuNew.setText(lunch  != null && !lunch.isEmpty()  ? lunch  : "menu is not set");
                        binding.textDinnerMenuNew.setText(dinner != null && !dinner.isEmpty() ? dinner : "menu is not set");
                    } else {
                        binding.textLunchMenuNew.setText("menu is not set");
                        binding.textDinnerMenuNew.setText("menu is not set");
                    }
                });
    }

    private void listenToMySelection() {
        if (messId == null || userId == null) return;

        db.collection("meal_selections").document(messId + "_" + todayDate + "_" + userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (binding == null) return;
                    String lunchStatus  = null;
                    String dinnerStatus = null;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        lunchStatus  = documentSnapshot.getString("lunch");
                        dinnerStatus = documentSnapshot.getString("dinner");
                    }

                    if (isOneTimeSubscribed) {
                        // Track whether any meal is already claimed today
                        oneTimeMealUsedToday = "IN".equals(lunchStatus) || "IN".equals(dinnerStatus);
                        updateOneTimeButtonUI(lunchStatus, dinnerStatus, currentUserOneTimeAutoSelect);
                    } else {
                        updateButtonUI("LUNCH",  lunchStatus);
                        updateButtonUI("DINNER", dinnerStatus);
                    }
                });
    }

    private void setupClickListeners() {
        binding.btnLunchInNew.setOnClickListener(v  -> markAttendance("LUNCH",  "IN"));
        binding.btnLunchOutNew.setOnClickListener(v -> markAttendance("LUNCH",  "OUT"));
        binding.btnDinnerInNew.setOnClickListener(v  -> markAttendance("DINNER", "IN"));
        binding.btnDinnerOutNew.setOnClickListener(v -> markAttendance("DINNER", "OUT"));
        binding.btnPlanOutDays.setOnClickListener(v  -> showOutPlanDialog());
    }

    private void markAttendance(String mealType, String status) {
        if (binding == null) return;

        if (messId == null || userId == null) {
            Toast.makeText(getContext(), "Still loading your profile, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        // ---- ONE_TIME subscription gate ----
        if (isOneTimeSubscribed) {
            if (!"IN".equals(status)) {
                // OUT is not applicable for ONE_TIME — guard anyway
                Toast.makeText(getContext(),
                        "One Time a Day: only mark IN for one meal per day.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (oneTimeMealUsedToday) {
                Toast.makeText(getContext(),
                        "You have already used your one meal for today!",
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // Normal subscription gate
            boolean active = mealType.equals("LUNCH") ? isLunchSubscribed : isDinnerSubscribed;
            if (!active) {
                Toast.makeText(getContext(), mealType + " Subscription Expired! Please renew.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (isCutoffPassed(mealType)) {
            String cutoffTime = mealType.equals("LUNCH")
                    ? String.format(Locale.getDefault(), "%02d:%02d", lunchCutoffHour,  lunchCutoffMinute)
                    : String.format(Locale.getDefault(), "%02d:%02d", dinnerCutoffHour, dinnerCutoffMinute);
            Toast.makeText(getContext(),
                    "Cutoff time (" + cutoffTime + ") has passed for " + mealType + ".",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Calendar today = Calendar.getInstance();
        Calendar selectedDate = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate.setTime(sdf.parse(todayDate));
            today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);       today.set(Calendar.MILLISECOND, 0);
            selectedDate.set(Calendar.HOUR_OF_DAY, 0); selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);       selectedDate.set(Calendar.MILLISECOND, 0);
            if (selectedDate.before(today)) {
                Toast.makeText(getContext(), "Cannot mark attendance for past dates",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception ex) { /* continue */ }

        updateAttendanceAndSubscription(todayDate, mealType, status)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    // No day-credit for OUT on ONE_TIME (the unused meal simply doesn't count)
                    String suffix = !isOneTimeSubscribed && "OUT".equals(status)
                            ? " Subscription day credited." : "";
                    Toast.makeText(getContext(), mealType + " marked as " + status + "!" + suffix,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(ex -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private com.google.android.gms.tasks.Task<Void> updateAttendanceAndSubscription(
            String date, String mealType, String status) {
        String docId  = messId + "_" + date + "_" + userId;
        com.google.firebase.firestore.DocumentReference mealRef =
                db.collection("meal_selections").document(docId);
        com.google.firebase.firestore.DocumentReference userRef =
                db.collection("users").document(userId);
        String statusKey = mealType.equals("LUNCH") ? "lunch" : "dinner";

        return db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot mealSnapshot = transaction.get(mealRef);
            com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
            String previousStatus = mealSnapshot.exists() ? mealSnapshot.getString(statusKey) : null;

            Map<String, Object> mealData = new HashMap<>();
            mealData.put(statusKey, status);
            mealData.put("userId", userId);
            mealData.put("date",   date);
            mealData.put("messId", messId);
            mealData.put("timestamp", System.currentTimeMillis());
            transaction.set(mealRef, mealData, com.google.firebase.firestore.SetOptions.merge());

            // Only adjust subscription expiry for non-ONE_TIME users
            if (!isOneTimeSubscribed) {
                long currentExpiry  = getExpiryForMeal(userSnapshot, mealType);
                long updatedExpiry  = currentExpiry;
                if ("OUT".equals(status) && !"OUT".equals(previousStatus)) {
                    updatedExpiry += MILLIS_PER_DAY;
                } else if ("IN".equals(status) && "OUT".equals(previousStatus)) {
                    updatedExpiry = Math.max(System.currentTimeMillis(), currentExpiry - MILLIS_PER_DAY);
                }
                if (updatedExpiry != currentExpiry) {
                    updateExpiryFields(transaction, userRef, userSnapshot, mealType, updatedExpiry);
                }
            }
            return null;
        });
    }

    // ────────────────────────────────────────────────────────────
    //  ONE_TIME UI: renders both cards, but once one is chosen
    //  the IN button on the other card is locked for the rest of
    //  the day.
    // ────────────────────────────────────────────────────────────
    private void updateOneTimeButtonUI(String lunchStatus, String dinnerStatus, String autoSelect) {
        if (binding == null) return;

        boolean cutoffLunch  = isCutoffPassed("LUNCH");
        boolean cutoffDinner = isCutoffPassed("DINNER");

        int colorPrimary      = themeColor(R.color.brand_primary);
        int colorTextOnPrimary = themeColor(R.color.text_on_brand);
        int colorTextMuted    = themeColor(R.color.text_caption);
        int colorGreen        = themeColor(R.color.state_success);
        int colorDisabled     = themeColor(R.color.text_disabled);
        int colorAmber        = themeColor(R.color.state_warning);

        boolean lunchExplicitIn = "IN".equals(lunchStatus);
        boolean dinnerExplicitIn = "IN".equals(dinnerStatus);
        
        boolean lunchChosen  = lunchExplicitIn || (!dinnerExplicitIn && "LUNCH".equals(autoSelect));
        boolean dinnerChosen = dinnerExplicitIn || (!lunchExplicitIn && "DINNER".equals(autoSelect));

        // ---- Lunch card ----
        if (lunchChosen) {
            // This meal was chosen — show as selected IN, lock both buttons
            binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            binding.btnLunchInNew.setTextColor(colorTextOnPrimary);
            binding.btnLunchInNew.setEnabled(false);
            binding.btnLunchOutNew.setVisibility(View.GONE); // OUT not applicable for ONE_TIME
            binding.textLunchStatus.setText("Today's meal selected");
            binding.textLunchStatusBar.setText(lunchExplicitIn ? "ONE TIME - Used" : "ONE TIME - Auto Selected");
            binding.textLunchStatusBar.setBackgroundColor(colorGreen);
            binding.textLunchStatusBar.setTextColor(Color.WHITE);
        } else if (dinnerChosen) {
            // Dinner was already chosen today — lock lunch card entirely
            binding.btnLunchInNew.setEnabled(false);
            binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorDisabled));
            binding.btnLunchInNew.setTextColor(Color.WHITE);
            binding.btnLunchOutNew.setVisibility(View.GONE);
            binding.textLunchStatus.setText("Dinner already selected today");
            binding.textLunchStatusBar.setText("ONE TIME - Locked");
            binding.textLunchStatusBar.setBackgroundColor(colorDisabled);
            binding.textLunchStatusBar.setTextColor(Color.WHITE);
        } else {
            // Nothing chosen yet — lunch is available if cutoff hasn't passed
            binding.btnLunchOutNew.setVisibility(View.GONE); // OUT hidden for ONE_TIME
            if (cutoffLunch) {
                binding.btnLunchInNew.setEnabled(false);
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorDisabled));
                binding.textLunchStatusBar.setText("ONE TIME - Cutoff Passed");
                binding.textLunchStatusBar.setBackgroundColor(colorDisabled);
            } else {
                binding.btnLunchInNew.setEnabled(true);
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnLunchInNew.setTextColor(colorTextOnPrimary);
                binding.textLunchStatusBar.setText("ONE TIME - Tap IN to select Lunch");
                binding.textLunchStatusBar.setBackgroundColor(colorAmber);
            }
            binding.textLunchStatus.setText("");
            binding.textLunchStatusBar.setTextColor(Color.WHITE);
        }

        // ---- Dinner card ----
        if (dinnerChosen) {
            binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
            binding.btnDinnerInNew.setTextColor(colorTextOnPrimary);
            binding.btnDinnerInNew.setEnabled(false);
            binding.btnDinnerOutNew.setVisibility(View.GONE);
            binding.textDinnerStatus.setText("Today's meal selected");
            binding.textDinnerStatusBar.setText(dinnerExplicitIn ? "ONE TIME - Used" : "ONE TIME - Auto Selected");
            binding.textDinnerStatusBar.setBackgroundColor(colorGreen);
            binding.textDinnerStatusBar.setTextColor(Color.WHITE);
        } else if (lunchChosen) {
            // Lunch was chosen — lock dinner card
            binding.btnDinnerInNew.setEnabled(false);
            binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorDisabled));
            binding.btnDinnerInNew.setTextColor(Color.WHITE);
            binding.btnDinnerOutNew.setVisibility(View.GONE);
            binding.textDinnerStatus.setText("Lunch already selected today");
            binding.textDinnerStatusBar.setText("ONE TIME - Locked");
            binding.textDinnerStatusBar.setBackgroundColor(colorDisabled);
            binding.textDinnerStatusBar.setTextColor(Color.WHITE);
        } else {
            binding.btnDinnerOutNew.setVisibility(View.GONE);
            if (cutoffDinner) {
                binding.btnDinnerInNew.setEnabled(false);
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorDisabled));
                binding.textDinnerStatusBar.setText("ONE TIME - Cutoff Passed");
                binding.textDinnerStatusBar.setBackgroundColor(colorDisabled);
            } else {
                binding.btnDinnerInNew.setEnabled(true);
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnDinnerInNew.setTextColor(colorTextOnPrimary);
                binding.textDinnerStatusBar.setText("ONE TIME - Tap IN to select Dinner");
                binding.textDinnerStatusBar.setBackgroundColor(colorAmber);
            }
            binding.textDinnerStatus.setText("");
            binding.textDinnerStatusBar.setTextColor(Color.WHITE);
        }
    }

    private void showOutPlanDialog() {
        if (binding == null) return;
        if (messId == null || userId == null) {
            Toast.makeText(getContext(), "Still loading your profile, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }
        // ONE_TIME users don't have OUT-day planning (no day-credit system)
        if (isOneTimeSubscribed) {
            Toast.makeText(getContext(),
                    "Plan OUT Days is not available for One Time a Day subscriptions.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, padding, padding, 0);

        DatePicker datePicker = new DatePicker(requireContext());
        datePicker.setMinDate(startOfToday().getTimeInMillis());
        container.addView(datePicker);

        com.google.android.material.textfield.TextInputLayout daysLayout =
                new com.google.android.material.textfield.TextInputLayout(requireContext());
        daysLayout.setHint("How many days?");
        daysLayout.setPadding(0, dp(12), 0, 0);
        com.google.android.material.textfield.TextInputEditText daysInput =
                new com.google.android.material.textfield.TextInputEditText(requireContext());
        daysInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        daysLayout.addView(daysInput);
        container.addView(daysLayout);

        RadioGroup mealGroup = new RadioGroup(requireContext());
        mealGroup.setOrientation(RadioGroup.HORIZONTAL);
        mealGroup.setPadding(0, dp(12), 0, 0);
        int lunchId  = View.generateViewId();
        int dinnerId = View.generateViewId();
        int bothId   = View.generateViewId();
        RadioButton lunch  = createMealRadioButton(lunchId,  "Lunch");
        RadioButton dinner = createMealRadioButton(dinnerId, "Dinner");
        RadioButton both   = createMealRadioButton(bothId,   "Both");
        mealGroup.addView(lunch);
        mealGroup.addView(dinner);
        mealGroup.addView(both);
        mealGroup.check(bothId);
        container.addView(mealGroup);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Plan OUT Days")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String daysText = daysInput.getText() != null ? daysInput.getText().toString().trim() : "";
                    if (daysText.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter number of days", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int days = Integer.parseInt(daysText);
                    if (days <= 0) {
                        Toast.makeText(getContext(), "Days must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar startDate = Calendar.getInstance();
                    startDate.set(datePicker.getYear(), datePicker.getMonth(),
                            datePicker.getDayOfMonth(), 0, 0, 0);
                    startDate.set(Calendar.MILLISECOND, 0);

                    String mealType = "BOTH";
                    int selectedId = mealGroup.getCheckedRadioButtonId();
                    if (selectedId == lunchId)       mealType = "LUNCH";
                    else if (selectedId == dinnerId) mealType = "DINNER";

                    applyOutPlan(startDate, days, mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private RadioButton createMealRadioButton(int id, String label) {
        RadioButton radioButton = new RadioButton(requireContext());
        radioButton.setId(id);
        radioButton.setText(label);
        radioButton.setLayoutParams(
                new RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return radioButton;
    }

    private void applyOutPlan(Calendar startDate, int days, String mealType) {
        if (binding == null) return;
        if ((mealType.equals("LUNCH") || mealType.equals("BOTH")) && !isLunchSubscribed) {
            Toast.makeText(getContext(), "Lunch subscription is expired. Please renew first.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if ((mealType.equals("DINNER") || mealType.equals("BOTH")) && !isDinnerSubscribed) {
            Toast.makeText(getContext(), "Dinner subscription is expired. Please renew first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        binding.btnPlanOutDays.setEnabled(false);
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cursor = (Calendar) startDate.clone();
        for (int i = 0; i < days; i++) {
            dates.add(sdf.format(cursor.getTime()));
            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        com.google.firebase.firestore.DocumentReference userRef =
                db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
            List<com.google.firebase.firestore.DocumentReference> mealRefs = new ArrayList<>();
            List<com.google.firebase.firestore.DocumentSnapshot> mealSnapshots = new ArrayList<>();

            for (String date : dates) {
                com.google.firebase.firestore.DocumentReference ref =
                        db.collection("meal_selections").document(messId + "_" + date + "_" + userId);
                mealRefs.add(ref);
                mealSnapshots.add(transaction.get(ref));
            }

            int lunchCredits  = 0;
            int dinnerCredits = 0;
            long timestamp = System.currentTimeMillis();
            for (int i = 0; i < dates.size(); i++) {
                Map<String, Object> mealData = new HashMap<>();
                mealData.put("userId",  userId);
                mealData.put("date",    dates.get(i));
                mealData.put("messId",  messId);
                mealData.put("timestamp", timestamp);
                mealData.put("plannedOut", true);

                com.google.firebase.firestore.DocumentSnapshot snapshot = mealSnapshots.get(i);
                if (mealType.equals("LUNCH") || mealType.equals("BOTH")) {
                    String previousLunch = snapshot.exists() ? snapshot.getString("lunch") : null;
                    if (!"OUT".equals(previousLunch)) lunchCredits++;
                    mealData.put("lunch", "OUT");
                    mealData.put("plannedOutLunch", true);
                }
                if (mealType.equals("DINNER") || mealType.equals("BOTH")) {
                    String previousDinner = snapshot.exists() ? snapshot.getString("dinner") : null;
                    if (!"OUT".equals(previousDinner)) dinnerCredits++;
                    mealData.put("dinner", "OUT");
                    mealData.put("plannedOutDinner", true);
                }
                transaction.set(mealRefs.get(i), mealData, com.google.firebase.firestore.SetOptions.merge());
            }

            long updatedLunchExpiry  = getExpiryForMeal(userSnapshot, "LUNCH")  + (lunchCredits  * MILLIS_PER_DAY);
            long updatedDinnerExpiry = getExpiryForMeal(userSnapshot, "DINNER") + (dinnerCredits * MILLIS_PER_DAY);
            if (lunchCredits > 0 || dinnerCredits > 0) {
                Map<String, Object> updates = new HashMap<>();
                if (lunchCredits  > 0) updates.put("lunchSubscriptionExpiry",  updatedLunchExpiry);
                if (dinnerCredits > 0) updates.put("dinnerSubscriptionExpiry", updatedDinnerExpiry);
                updates.put("subscriptionExpiry", Math.max(updatedLunchExpiry, updatedDinnerExpiry));
                transaction.update(userRef, updates);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (binding == null) return;
            binding.btnPlanOutDays.setEnabled(true);
            Toast.makeText(getContext(), "OUT days saved. Subscription credited.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            if (binding == null) return;
            binding.btnPlanOutDays.setEnabled(true);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void listenToPlannedOutDays() {
        if (messId == null || userId == null) return;
        if (plannedOutListener != null) plannedOutListener.remove();

        plannedOutListener = db.collection("meal_selections")
                .whereEqualTo("messId", messId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("plannedOut", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null || e != null) return;
                    binding.containerPlannedOutDays.removeAllViews();
                    if (snapshots == null || snapshots.isEmpty()) {
                        binding.cardPlannedOutDays.setVisibility(View.GONE);
                        return;
                    }

                    List<com.google.firebase.firestore.DocumentSnapshot> plannedDocs = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        String date = doc.getString("date");
                        if (date != null && date.compareTo(todayDate) >= 0 && hasPlannedOutMeal(doc)) {
                            plannedDocs.add(doc);
                        }
                    }

                    plannedDocs.sort((left, right) -> {
                        String leftDate  = left.getString("date");
                        String rightDate = right.getString("date");
                        if (leftDate  == null) return 1;
                        if (rightDate == null) return -1;
                        return leftDate.compareTo(rightDate);
                    });

                    if (plannedDocs.isEmpty()) {
                        binding.cardPlannedOutDays.setVisibility(View.GONE);
                        return;
                    }

                    binding.cardPlannedOutDays.setVisibility(View.VISIBLE);
                    for (com.google.firebase.firestore.DocumentSnapshot doc : plannedDocs) {
                        binding.containerPlannedOutDays.addView(createPlannedOutRow(doc));
                    }
                });
    }

    // BUG FIX: only the specific flags, not the catch-all plannedOut==true
    private boolean hasPlannedOutMeal(com.google.firebase.firestore.DocumentSnapshot doc) {
        return (Boolean.TRUE.equals(doc.getBoolean("plannedOutLunch"))  && "OUT".equals(doc.getString("lunch")))
            || (Boolean.TRUE.equals(doc.getBoolean("plannedOutDinner")) && "OUT".equals(doc.getString("dinner")));
    }

    private View createPlannedOutRow(com.google.firebase.firestore.DocumentSnapshot doc) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView text = new TextView(requireContext());
        text.setText(formatPlannedOutLabel(doc));
        text.setTextSize(14);
        text.setTextColor(themeColor(R.color.text_body));
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        com.google.android.material.button.MaterialButton cancelButton =
                new com.google.android.material.button.MaterialButton(requireContext());
        cancelButton.setText("Cancel");
        cancelButton.setTextSize(12);
        cancelButton.setOnClickListener(v -> confirmCancelPlannedOut(doc));

        row.addView(text);
        row.addView(cancelButton);
        return row;
    }

    private String formatPlannedOutLabel(com.google.firebase.firestore.DocumentSnapshot doc) {
        String date = doc.getString("date");
        String meal = getPlannedOutMealLabel(doc);
        try {
            Date parsedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date);
            if (parsedDate != null) {
                date = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(parsedDate);
            }
        } catch (Exception ignored) { }
        return date + " - " + meal;
    }

    private String getPlannedOutMealLabel(com.google.firebase.firestore.DocumentSnapshot doc) {
        boolean lunch  = "OUT".equals(doc.getString("lunch"));
        boolean dinner = "OUT".equals(doc.getString("dinner"));
        if (lunch && dinner) return "Lunch & Dinner";
        if (lunch)  return "Lunch";
        if (dinner) return "Dinner";
        return "OUT";
    }

    private void confirmCancelPlannedOut(com.google.firebase.firestore.DocumentSnapshot doc) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel OUT Plan")
                .setMessage("Cancel planned OUT for " + formatPlannedOutLabel(doc) + "?")
                .setPositiveButton("Cancel Plan", (dialog, which) -> cancelPlannedOut(doc.getId()))
                .setNegativeButton("Keep", null)
                .show();
    }

    private void cancelPlannedOut(String documentId) {
        if (binding == null || userId == null) return;
        binding.btnPlanOutDays.setEnabled(false);

        com.google.firebase.firestore.DocumentReference mealRef =
                db.collection("meal_selections").document(documentId);
        com.google.firebase.firestore.DocumentReference userRef =
                db.collection("users").document(userId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot mealSnapshot = transaction.get(mealRef);
            com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
            if (!mealSnapshot.exists()) return null;

            boolean cancelLunch  = "OUT".equals(mealSnapshot.getString("lunch"))
                    && (Boolean.TRUE.equals(mealSnapshot.getBoolean("plannedOutLunch"))
                     || Boolean.TRUE.equals(mealSnapshot.getBoolean("plannedOut")));
            boolean cancelDinner = "OUT".equals(mealSnapshot.getString("dinner"))
                    && (Boolean.TRUE.equals(mealSnapshot.getBoolean("plannedOutDinner"))
                     || Boolean.TRUE.equals(mealSnapshot.getBoolean("plannedOut")));

            Map<String, Object> mealUpdates = new HashMap<>();
            if (cancelLunch)  { mealUpdates.put("lunch",  "IN"); mealUpdates.put("plannedOutLunch",  false); }
            if (cancelDinner) { mealUpdates.put("dinner", "IN"); mealUpdates.put("plannedOutDinner", false); }
            mealUpdates.put("plannedOut", false);
            mealUpdates.put("timestamp", System.currentTimeMillis());
            transaction.update(mealRef, mealUpdates);

            long lunchExpiry  = getExpiryForMeal(userSnapshot, "LUNCH");
            long dinnerExpiry = getExpiryForMeal(userSnapshot, "DINNER");
            if (cancelLunch)  lunchExpiry  = Math.max(System.currentTimeMillis(), lunchExpiry  - MILLIS_PER_DAY);
            if (cancelDinner) dinnerExpiry = Math.max(System.currentTimeMillis(), dinnerExpiry - MILLIS_PER_DAY);

            Map<String, Object> userUpdates = new HashMap<>();
            if (cancelLunch)  userUpdates.put("lunchSubscriptionExpiry",  lunchExpiry);
            if (cancelDinner) userUpdates.put("dinnerSubscriptionExpiry", dinnerExpiry);
            if (cancelLunch || cancelDinner) {
                userUpdates.put("subscriptionExpiry", Math.max(lunchExpiry, dinnerExpiry));
                transaction.update(userRef, userUpdates);
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (binding == null) return;
            binding.btnPlanOutDays.setEnabled(true);
            Toast.makeText(getContext(), "OUT plan cancelled.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            if (binding == null) return;
            binding.btnPlanOutDays.setEnabled(true);
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private long getExpiryForMeal(com.google.firebase.firestore.DocumentSnapshot userSnapshot, String mealType) {
        String expiryKey = mealType.equals("LUNCH") ? "lunchSubscriptionExpiry" : "dinnerSubscriptionExpiry";
        Long specificExpiry = userSnapshot.getLong(expiryKey);
        Long generalExpiry  = userSnapshot.getLong("subscriptionExpiry");
        if (specificExpiry != null && specificExpiry > 0) return specificExpiry;
        return generalExpiry != null ? generalExpiry : System.currentTimeMillis();
    }

    private void updateExpiryFields(com.google.firebase.firestore.Transaction transaction,
            com.google.firebase.firestore.DocumentReference userRef,
            com.google.firebase.firestore.DocumentSnapshot userSnapshot,
            String mealType, long updatedExpiry) {
        String expiryKey   = mealType.equals("LUNCH") ? "lunchSubscriptionExpiry" : "dinnerSubscriptionExpiry";
        long otherExpiry   = getExpiryForMeal(userSnapshot, mealType.equals("LUNCH") ? "DINNER" : "LUNCH");
        long overallExpiry = Math.max(updatedExpiry, otherExpiry);
        transaction.update(userRef, expiryKey, updatedExpiry, "subscriptionExpiry", overallExpiry);
    }

    private Calendar startOfToday() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int themeColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private void updateButtonUI(String mealType, String status) {
        if (binding == null) return;

        int colorPrimary       = themeColor(R.color.brand_primary);
        int colorTextOnPrimary = themeColor(R.color.text_on_brand);
        int colorTextNormal    = themeColor(R.color.text_heading);
        int colorTextMuted     = themeColor(R.color.text_caption);
        int colorGray          = themeColor(R.color.neutral_300);
        int colorGreen         = themeColor(R.color.state_success);
        int colorYellow        = themeColor(R.color.state_warning);
        int colorDisabled      = themeColor(R.color.text_disabled);

        boolean isSubscribed = mealType.equals("LUNCH") ? isLunchSubscribed : isDinnerSubscribed;
        boolean cutoffPassed = isCutoffPassed(mealType);
        boolean canMark      = isSubscribed && !cutoffPassed;

        if (mealType.equals("LUNCH")) {
            binding.btnLunchOutNew.setVisibility(View.VISIBLE); // restore for normal subs
            if (status == null) {
                binding.textLunchStatusBar.setTextColor(Color.WHITE);
                if (!canMark) {
                    binding.textLunchStatusBar.setText(!isSubscribed
                            ? "Status: Subscription Expired" : "Status: Cutoff Time Passed");
                    binding.textLunchStatusBar.setBackgroundColor(colorDisabled);
                } else {
                    binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                    binding.btnLunchInNew.setTextColor(colorTextOnPrimary);
                    binding.btnLunchOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                    binding.btnLunchOutNew.setTextColor(colorTextMuted);
                    binding.textLunchStatus.setText("");
                    binding.textLunchStatusBar.setText("Status: IN");
                    binding.textLunchStatusBar.setBackgroundColor(colorGreen);
                }
                binding.btnLunchInNew.setEnabled(false);
                binding.btnLunchOutNew.setEnabled(canMark);
                return;
            }
            if ("IN".equals(status)) {
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnLunchInNew.setTextColor(colorTextOnPrimary);
                binding.btnLunchOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnLunchOutNew.setTextColor(colorTextMuted);
                binding.btnLunchInNew.setEnabled(false);
                binding.btnLunchOutNew.setEnabled(canMark && allowMultipleChanges);
                binding.textLunchStatus.setText("");
                binding.textLunchStatusBar.setText("Status: IN");
                binding.textLunchStatusBar.setBackgroundColor(colorGreen);
                binding.textLunchStatusBar.setTextColor(Color.WHITE);
            } else if ("OUT".equals(status)) {
                binding.btnLunchInNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnLunchInNew.setTextColor(colorTextMuted);
                binding.btnLunchOutNew.setBackgroundTintList(ColorStateList.valueOf(colorGray));
                binding.btnLunchOutNew.setTextColor(colorTextNormal);
                binding.btnLunchInNew.setEnabled(canMark && allowMultipleChanges);
                binding.btnLunchOutNew.setEnabled(false);
                binding.textLunchStatus.setText("Marked OUT");
                binding.textLunchStatus.setTextColor(Color.GRAY);
                binding.textLunchStatusBar.setText("Status: OUT");
                binding.textLunchStatusBar.setBackgroundColor(colorYellow);
                binding.textLunchStatusBar.setTextColor(Color.WHITE);
            }
        } else {
            binding.btnDinnerOutNew.setVisibility(View.VISIBLE);
            if (status == null) {
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);
                if (!canMark) {
                    binding.textDinnerStatusBar.setText(!isSubscribed
                            ? "Status: Subscription Expired" : "Status: Cutoff Time Passed");
                    binding.textDinnerStatusBar.setBackgroundColor(colorDisabled);
                } else {
                    binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                    binding.btnDinnerInNew.setTextColor(colorTextOnPrimary);
                    binding.btnDinnerOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                    binding.btnDinnerOutNew.setTextColor(colorTextMuted);
                    binding.textDinnerStatus.setText("");
                    binding.textDinnerStatusBar.setText("Status: IN");
                    binding.textDinnerStatusBar.setBackgroundColor(colorGreen);
                }
                binding.btnDinnerInNew.setEnabled(false);
                binding.btnDinnerOutNew.setEnabled(canMark);
                return;
            }
            if ("IN".equals(status)) {
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                binding.btnDinnerInNew.setTextColor(colorTextOnPrimary);
                binding.btnDinnerOutNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnDinnerOutNew.setTextColor(colorTextMuted);
                binding.btnDinnerInNew.setEnabled(false);
                binding.btnDinnerOutNew.setEnabled(canMark && allowMultipleChanges);
                binding.textDinnerStatus.setText("");
                binding.textDinnerStatusBar.setText("Status: IN");
                binding.textDinnerStatusBar.setBackgroundColor(colorGreen);
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);
            } else if ("OUT".equals(status)) {
                binding.btnDinnerInNew.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                binding.btnDinnerInNew.setTextColor(colorTextMuted);
                binding.btnDinnerOutNew.setBackgroundTintList(ColorStateList.valueOf(colorGray));
                binding.btnDinnerOutNew.setTextColor(colorTextNormal);
                binding.btnDinnerInNew.setEnabled(canMark && allowMultipleChanges);
                binding.btnDinnerOutNew.setEnabled(false);
                binding.textDinnerStatus.setText("Marked OUT");
                binding.textDinnerStatus.setTextColor(Color.GRAY);
                binding.textDinnerStatusBar.setText("Status: OUT");
                binding.textDinnerStatusBar.setBackgroundColor(colorYellow);
                binding.textDinnerStatusBar.setTextColor(Color.WHITE);
            }
        }
    }

    private void startDeadlineTimer() {
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;

                Calendar now  = Calendar.getInstance();
                long nowMs    = now.getTimeInMillis();

                Calendar lunchTarget = (Calendar) now.clone();
                lunchTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                lunchTarget.set(Calendar.MINUTE,      lunchCutoffMinute);
                lunchTarget.set(Calendar.SECOND,      0);
                updateCardTimer(binding.textLunchCardTimer, lunchTarget.getTimeInMillis() - nowMs);

                Calendar dinnerTarget = (Calendar) now.clone();
                dinnerTarget.set(Calendar.HOUR_OF_DAY, dinnerCutoffHour);
                dinnerTarget.set(Calendar.MINUTE,      dinnerCutoffMinute);
                dinnerTarget.set(Calendar.SECOND,      0);
                updateCardTimer(binding.textDinnerCardTimer, dinnerTarget.getTimeInMillis() - nowMs);

                Calendar mainTarget = (Calendar) now.clone();
                String targetLabel  = "LUNCH";
                mainTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                mainTarget.set(Calendar.MINUTE,      lunchCutoffMinute);
                mainTarget.set(Calendar.SECOND,      0);

                if (now.after(mainTarget)) {
                    mainTarget.set(Calendar.HOUR_OF_DAY, dinnerCutoffHour);
                    mainTarget.set(Calendar.MINUTE,      dinnerCutoffMinute);
                    targetLabel = "DINNER";
                    if (now.after(mainTarget)) {
                        mainTarget.add(Calendar.DAY_OF_YEAR, 1);
                        mainTarget.set(Calendar.HOUR_OF_DAY, lunchCutoffHour);
                        mainTarget.set(Calendar.MINUTE,      lunchCutoffMinute);
                        targetLabel = "TOMORROW'S LUNCH";
                    }
                }

                binding.textDeadlineLabel.setText("UPCOMING DEADLINE: " + targetLabel);
                long diff = mainTarget.getTimeInMillis() - nowMs;
                if (diff > 0) {
                    long hours   = TimeUnit.MILLISECONDS.toHours(diff);
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                    if (binding.textTimerHours != null)
                        binding.textTimerHours.setText(String.format(Locale.getDefault(), "%02d", hours));
                    if (binding.textTimerMins  != null)
                        binding.textTimerMins.setText(String.format(Locale.getDefault(), "%02d", minutes));
                    if (binding.textTimerSecs  != null)
                        binding.textTimerSecs.setText(String.format(Locale.getDefault(), "%02d", seconds));
                }
            }
            @Override public void onFinish() {}
        };
        timer.start();
    }

    private void updateCardTimer(TextView timerView, long diff) {
        if (timerView == null) return;
        timerView.setVisibility(View.VISIBLE);
        if (diff > 0) {
            long hours   = TimeUnit.MILLISECONDS.toHours(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
            timerView.setText(hours > 0
                    ? String.format(Locale.getDefault(), "%dh %02dm left", hours, minutes)
                    : String.format(Locale.getDefault(), "%02d:%02d left", minutes, seconds));
            timerView.setTextColor(themeColor(R.color.state_error));
            timerView.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.color.semantic_error_bg)));
        } else {
            timerView.setText("LOCKED");
            timerView.setTextColor(themeColor(R.color.text_disabled));
            timerView.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.color.neutral_100)));
        }
    }

    private boolean isCutoffPassed(String mealType) {
        Calendar now    = Calendar.getInstance();
        int hour        = now.get(Calendar.HOUR_OF_DAY);
        int minute      = now.get(Calendar.MINUTE);
        if (mealType.equals("LUNCH")) {
            return (hour > lunchCutoffHour)  || (hour == lunchCutoffHour  && minute >= lunchCutoffMinute);
        } else {
            return (hour > dinnerCutoffHour) || (hour == dinnerCutoffHour && minute >= dinnerCutoffMinute);
        }
    }

    private void listenToMessCondition() {
        if (messId == null) return;

        db.collection("mess_settings").document(messId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (binding == null) return;
                    if (e != null) return;
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String condition = documentSnapshot.getString("messCondition");
                        if (condition != null) updateMessConditionDisplay(condition);
                        else hideMessConditionCard();
                    } else {
                        hideMessConditionCard();
                    }
                });
    }

    private void updateMessConditionDisplay(String condition) {
        if (binding == null) return;
        binding.cardMessCondition.setVisibility(View.VISIBLE);
        View indicator = binding.indicatorMessCondition;
        switch (condition) {
            case "FULL":
                binding.textMessCondition.setText("Mess is FULL");
                binding.textMessCondition.setTextColor(themeColor(R.color.state_error));
                indicator.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.color.state_error)));
                break;
            case "HALF":
                binding.textMessCondition.setText("Mess is HALF FULL");
                binding.textMessCondition.setTextColor(themeColor(R.color.state_warning));
                indicator.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.color.state_warning)));
                break;
            case "EMPTY":
                binding.textMessCondition.setText("Mess is EMPTY");
                binding.textMessCondition.setTextColor(themeColor(R.color.state_success));
                indicator.setBackgroundTintList(ColorStateList.valueOf(themeColor(R.color.state_success)));
                break;
            default:
                hideMessConditionCard();
        }
    }

    private void hideMessConditionCard() {
        if (binding != null) binding.cardMessCondition.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
        if (plannedOutListener != null) plannedOutListener.remove();
        binding = null;
    }
}
