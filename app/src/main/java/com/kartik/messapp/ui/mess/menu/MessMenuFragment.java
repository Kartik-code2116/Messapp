package com.kartik.messapp.ui.mess.menu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kartik.messapp.R;
import com.kartik.messapp.databinding.FragmentMessMenuBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private Calendar weekStartCalendar;

    // Cutoff times (defaults)
    private int lunchCutoffHour = 10;
    private int lunchCutoffMinute = 30;
    private int dinnerCutoffHour = 16;
    private int dinnerCutoffMinute = 30;

    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] DAY_SHORT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // References to dynamically created views per day
    private final TextInputEditText[] breakfastInputs = new TextInputEditText[7];
    private final TextInputEditText[] lunchInputs = new TextInputEditText[7];
    private final TextInputEditText[] dinnerInputs = new TextInputEditText[7];
    private final View[] dayCardViews = new View[7];
    private final LinearLayout[] mealContentLayouts = new LinearLayout[7];
    private final ImageView[] expandIcons = new ImageView[7];
    private final ImageView[] statusIcons = new ImageView[7];
    private final boolean[] isExpanded = new boolean[7];

    // Track which days have menus set
    private final boolean[] hasMenu = new boolean[7];

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Start from Monday of current week
        weekStartCalendar = Calendar.getInstance();
        weekStartCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        setupWeekNavigation();
        setupDayChips();
        buildDayCards(inflater);
        fetchMessIdAndSetup();

        binding.swipeRefreshMenu.setColorSchemeResources(
                R.color.brand_primary, R.color.state_success, R.color.brand_primary_dark);
        binding.swipeRefreshMenu.setOnRefreshListener(() -> {
            loadMenuForCurrentWeek();
            binding.swipeRefreshMenu.setRefreshing(false);
        });

        return root;
    }

    // ════════════════════════════════════════════════════════
    // Week Navigation
    // ════════════════════════════════════════════════════════

    private void setupWeekNavigation() {
        updateWeekDisplay();

        binding.btnPrevWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekDisplay();
            updateDayChips();
            loadMenuForCurrentWeek();
        });

        binding.btnNextWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekDisplay();
            updateDayChips();
            loadMenuForCurrentWeek();
        });
    }

    private void updateWeekDisplay() {
        if (binding == null) return;

        Calendar weekEnd = (Calendar) weekStartCalendar.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);

        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        String weekRange = displayFormat.format(weekStartCalendar.getTime()) + " – " +
                displayFormat.format(weekEnd.getTime());
        binding.textWeekRange.setText(weekRange);

        // Determine if this is current, past, or future week
        Calendar today = Calendar.getInstance();
        Calendar thisWeekStart = Calendar.getInstance();
        thisWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        thisWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        thisWeekStart.set(Calendar.MINUTE, 0);
        thisWeekStart.set(Calendar.SECOND, 0);
        thisWeekStart.set(Calendar.MILLISECOND, 0);

        Calendar compareStart = (Calendar) weekStartCalendar.clone();
        compareStart.set(Calendar.HOUR_OF_DAY, 0);
        compareStart.set(Calendar.MINUTE, 0);
        compareStart.set(Calendar.SECOND, 0);
        compareStart.set(Calendar.MILLISECOND, 0);

        if (compareStart.equals(thisWeekStart)) {
            binding.textWeekLabel.setText("This Week");
        } else if (compareStart.before(thisWeekStart)) {
            binding.textWeekLabel.setText("Past Week");
        } else {
            binding.textWeekLabel.setText("Upcoming Week");
        }
    }

    // ════════════════════════════════════════════════════════
    // Day Chip Strip
    // ════════════════════════════════════════════════════════

    private void setupDayChips() {
        if (binding == null || getContext() == null) return;

        binding.chipGroupDays.removeAllViews();

        Calendar dayCal = (Calendar) weekStartCalendar.clone();
        int todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 0; i < 7; i++) {
            Chip chip = new Chip(getContext());
            int dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH);
            chip.setText(DAY_SHORT[i] + " " + dayOfMonth);
            chip.setCheckable(true);
            chip.setTextSize(13f);
            chip.setChipCornerRadius(40f);

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                    && dayCal.get(Calendar.YEAR) == todayYear;

            if (isToday) {
                chip.setChecked(true);
                chip.setChipBackgroundColorResource(R.color.brand_primary);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            } else {
                chip.setChipBackgroundColorResource(R.color.neutral_100);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.text_body));
            }

            final int dayIndex = i;
            chip.setOnClickListener(v -> scrollToDayCard(dayIndex));

            binding.chipGroupDays.addView(chip);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateDayChips() {
        if (binding == null || getContext() == null) return;
        binding.chipGroupDays.removeAllViews();

        Calendar dayCal = (Calendar) weekStartCalendar.clone();
        int todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 0; i < 7; i++) {
            Chip chip = new Chip(getContext());
            int dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH);
            chip.setText(DAY_SHORT[i] + " " + dayOfMonth);
            chip.setCheckable(true);
            chip.setTextSize(13f);
            chip.setChipCornerRadius(40f);

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                    && dayCal.get(Calendar.YEAR) == todayYear;

            if (isToday) {
                chip.setChecked(true);
                chip.setChipBackgroundColorResource(R.color.brand_primary);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            } else {
                chip.setChipBackgroundColorResource(R.color.neutral_100);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.text_body));
            }

            final int dayIndex = i;
            chip.setOnClickListener(v -> scrollToDayCard(dayIndex));

            binding.chipGroupDays.addView(chip);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Also update the day card headers (dates)
        updateDayCardHeaders();
    }

    private void scrollToDayCard(int dayIndex) {
        if (binding == null || dayCardViews[dayIndex] == null) return;

        // Expand the target card if collapsed
        if (!isExpanded[dayIndex]) {
            toggleDayCard(dayIndex);
        }

        // Scroll to the card
        binding.scrollView.post(() -> {
            if (binding != null && dayCardViews[dayIndex] != null) {
                binding.scrollView.smoothScrollTo(0, dayCardViews[dayIndex].getTop() - 20);
            }
        });
    }

    // ════════════════════════════════════════════════════════
    // Day Cards
    // ════════════════════════════════════════════════════════

    private void buildDayCards(LayoutInflater inflater) {
        if (binding == null || getContext() == null) return;

        binding.containerDayCards.removeAllViews();

        Calendar dayCal = (Calendar) weekStartCalendar.clone();
        Calendar today = Calendar.getInstance();
        int todayDayOfYear = today.get(Calendar.DAY_OF_YEAR);
        int todayYear = today.get(Calendar.YEAR);

        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        int todayIndex = -1;

        for (int i = 0; i < 7; i++) {
            View cardView = inflater.inflate(R.layout.item_menu_day_card, binding.containerDayCards, false);

            // Find views
            TextView dayNameTv = cardView.findViewById(R.id.text_day_name);
            TextView dayDateTv = cardView.findViewById(R.id.text_day_date);
            TextView badgeToday = cardView.findViewById(R.id.badge_today);
            View accentBar = cardView.findViewById(R.id.view_accent_bar);
            LinearLayout header = cardView.findViewById(R.id.layout_day_header);
            LinearLayout mealContent = cardView.findViewById(R.id.layout_meal_content);
            ImageView expandIcon = cardView.findViewById(R.id.icon_expand);
            ImageView menuStatusIcon = cardView.findViewById(R.id.icon_menu_status);

            // Meal inputs
            TextInputEditText etBreakfast = cardView.findViewById(R.id.et_breakfast);
            TextInputEditText etLunch = cardView.findViewById(R.id.et_lunch);
            TextInputEditText etDinner = cardView.findViewById(R.id.et_dinner);

            // Save buttons
            View btnSaveBreakfast = cardView.findViewById(R.id.btn_save_breakfast);
            View btnSaveLunch = cardView.findViewById(R.id.btn_save_lunch);
            View btnSaveDinner = cardView.findViewById(R.id.btn_save_dinner);

            // Set day info
            dayNameTv.setText(DAY_NAMES[i]);
            dayDateTv.setText(dateFmt.format(dayCal.getTime()));

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                    && dayCal.get(Calendar.YEAR) == todayYear;

            if (isToday) {
                todayIndex = i;
                badgeToday.setVisibility(View.VISIBLE);
                // Highlight card with accent stroke
                if (cardView instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView mcv =
                            (com.google.android.material.card.MaterialCardView) cardView;
                    mcv.setStrokeColor(ContextCompat.getColor(getContext(), R.color.brand_primary));
                    mcv.setStrokeWidth(dpToPx(2));
                }
            }

            // Set accent bar color based on day index (cycling warm palette)
            int[] accentColors = {
                    0xFFF97316, // Orange
                    0xFF10B981, // Emerald
                    0xFF3B82F6, // Blue
                    0xFFF59E0B, // Amber
                    0xFFEC4899, // Pink
                    0xFF8B5CF6, // Purple
                    0xFFEF4444  // Red
            };
            accentBar.setBackgroundColor(accentColors[i]);

            // Store references
            dayCardViews[i] = cardView;
            mealContentLayouts[i] = mealContent;
            expandIcons[i] = expandIcon;
            statusIcons[i] = menuStatusIcon;
            breakfastInputs[i] = etBreakfast;
            lunchInputs[i] = etLunch;
            dinnerInputs[i] = etDinner;
            isExpanded[i] = false;

            // Header click to expand/collapse
            final int dayIndex = i;
            header.setOnClickListener(v -> toggleDayCard(dayIndex));

            // Save button click listeners
            btnSaveBreakfast.setOnClickListener(v -> saveMenu(dayIndex, "breakfast"));
            btnSaveLunch.setOnClickListener(v -> saveMenu(dayIndex, "lunch"));
            btnSaveDinner.setOnClickListener(v -> saveMenu(dayIndex, "dinner"));

            binding.containerDayCards.addView(cardView);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Auto-expand today's card
        if (todayIndex >= 0) {
            final int expandIndex = todayIndex;
            binding.containerDayCards.post(() -> {
                if (binding != null) {
                    toggleDayCard(expandIndex);
                    // Scroll to today's card after a brief delay
                    binding.scrollView.postDelayed(() -> {
                        if (binding != null && dayCardViews[expandIndex] != null) {
                            binding.scrollView.smoothScrollTo(0, dayCardViews[expandIndex].getTop() - 20);
                        }
                    }, 300);
                }
            });
        }
    }

    private void updateDayCardHeaders() {
        if (binding == null || getContext() == null) return;

        Calendar dayCal = (Calendar) weekStartCalendar.clone();
        Calendar today = Calendar.getInstance();
        int todayDayOfYear = today.get(Calendar.DAY_OF_YEAR);
        int todayYear = today.get(Calendar.YEAR);

        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            if (dayCardViews[i] == null) continue;

            TextView dayDateTv = dayCardViews[i].findViewById(R.id.text_day_date);
            TextView badgeToday = dayCardViews[i].findViewById(R.id.badge_today);

            dayDateTv.setText(dateFmt.format(dayCal.getTime()));

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
                    && dayCal.get(Calendar.YEAR) == todayYear;

            badgeToday.setVisibility(isToday ? View.VISIBLE : View.GONE);

            if (dayCardViews[i] instanceof com.google.android.material.card.MaterialCardView) {
                com.google.android.material.card.MaterialCardView mcv =
                        (com.google.android.material.card.MaterialCardView) dayCardViews[i];
                if (isToday) {
                    mcv.setStrokeColor(ContextCompat.getColor(getContext(), R.color.brand_primary));
                    mcv.setStrokeWidth(dpToPx(2));
                } else {
                    mcv.setStrokeColor(ContextCompat.getColor(getContext(), R.color.neutral_200));
                    mcv.setStrokeWidth(dpToPx(1));
                }
            }

            // Collapse all cards on week change
            if (isExpanded[i]) {
                mealContentLayouts[i].setVisibility(View.GONE);
                expandIcons[i].setRotation(90);
                isExpanded[i] = false;
            }

            // Clear inputs
            breakfastInputs[i].setText("");
            lunchInputs[i].setText("");
            dinnerInputs[i].setText("");

            // Reset status icons
            statusIcons[i].setVisibility(View.GONE);
            hasMenu[i] = false;

            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void toggleDayCard(int dayIndex) {
        if (binding == null || mealContentLayouts[dayIndex] == null) return;

        isExpanded[dayIndex] = !isExpanded[dayIndex];

        if (isExpanded[dayIndex]) {
            mealContentLayouts[dayIndex].setVisibility(View.VISIBLE);
            mealContentLayouts[dayIndex].setAlpha(0f);
            mealContentLayouts[dayIndex].animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start();

            // Rotate chevron to point down
            expandIcons[dayIndex].animate()
                    .rotation(270)
                    .setDuration(200)
                    .start();
        } else {
            mealContentLayouts[dayIndex].animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        if (mealContentLayouts[dayIndex] != null) {
                            mealContentLayouts[dayIndex].setVisibility(View.GONE);
                        }
                    })
                    .start();

            // Rotate chevron to point right
            expandIcons[dayIndex].animate()
                    .rotation(90)
                    .setDuration(200)
                    .start();
        }
    }

    // ════════════════════════════════════════════════════════
    // Data Loading
    // ════════════════════════════════════════════════════════

    private void fetchMessIdAndSetup() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Sign in to manage menu", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        if (binding != null) binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            fetchMessSettings();
                            loadMenuForCurrentWeek();
                        } else {
                            Toast.makeText(getContext(), "Mess ID not found for this user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
                        applyAllRestrictions();
                    }
                });
    }

    private void loadMenuForCurrentWeek() {
        if (currentMessId == null || binding == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Compute start and end date strings for the week
        Calendar weekEnd = (Calendar) weekStartCalendar.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        String startDate = dateFormat.format(weekStartCalendar.getTime());
        String endDate   = dateFormat.format(weekEnd.getTime());

        // Reset all day states
        for (int i = 0; i < 7; i++) {
            hasMenu[i] = false;
            if (statusIcons[i] != null) statusIcons[i].setVisibility(View.GONE);
            if (breakfastInputs[i] != null) breakfastInputs[i].setText("");
            if (lunchInputs[i]    != null) lunchInputs[i].setText("");
            if (dinnerInputs[i]   != null) dinnerInputs[i].setText("");
        }

        java.util.List<String> docIds = new java.util.ArrayList<>();
        Calendar dayCalBuilder = (Calendar) weekStartCalendar.clone();
        for (int i = 0; i < 7; i++) {
            docIds.add(currentMessId + "_" + dateFormat.format(dayCalBuilder.getTime()));
            dayCalBuilder.add(Calendar.DAY_OF_YEAR, 1);
        }

        // === SINGLE query for the whole week using document IDs ===
        db.collection("menus")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), docIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (binding == null) return;

                    // Map each returned document to the correct day index
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docDate = doc.getString("date");
                        if (docDate == null) continue;

                        // Find which day index this date corresponds to
                        Calendar dayCal = (Calendar) weekStartCalendar.clone();
                        for (int i = 0; i < 7; i++) {
                            if (docDate.equals(dateFormat.format(dayCal.getTime()))) {
                                String breakfast = doc.getString("breakfast");
                                String lunch     = doc.getString("lunch");
                                String dinner    = doc.getString("dinner");

                                if (breakfast == null) breakfast = "";
                                if (lunch == null)     lunch = "";
                                if (dinner == null)    dinner = "";

                                if (breakfastInputs[i] != null) breakfastInputs[i].setText(breakfast);
                                if (lunchInputs[i]     != null) lunchInputs[i].setText(lunch);
                                if (dinnerInputs[i]    != null) dinnerInputs[i].setText(dinner);

                                boolean hasAny = !breakfast.isEmpty() || !lunch.isEmpty() || !dinner.isEmpty();
                                hasMenu[i] = hasAny;
                                if (statusIcons[i] != null) {
                                    statusIcons[i].setVisibility(hasAny ? View.VISIBLE : View.GONE);
                                }
                                break;
                            }
                            dayCal.add(Calendar.DAY_OF_YEAR, 1);
                        }
                    }

                    updateMenuSummary();
                    applyAllRestrictions();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
            if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading menu: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                    applyAllRestrictions();
                });
    }

    private void updateMenuSummary() {
        if (binding == null) return;

        int setCount = 0;
        for (boolean h : hasMenu) {
            if (h) setCount++;
        }

        if (setCount == 7) {
            binding.textMenuSummary.setText("✓ All 7 days have menus set");
            binding.textMenuSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_success));
        } else if (setCount == 0) {
            binding.textMenuSummary.setText("No menus set for this week yet");
            binding.textMenuSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_caption));
        } else {
            binding.textMenuSummary.setText(setCount + " of 7 days have menus set");
            binding.textMenuSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.state_warning));
        }
    }

    // ════════════════════════════════════════════════════════
    // Time Restrictions
    // ════════════════════════════════════════════════════════

    private void applyAllRestrictions() {
        for (int i = 0; i < 7; i++) {
            applyRestrictions(i);
        }
    }

    private void applyRestrictions(int dayIndex) {
        if (binding == null || dayCardViews[dayIndex] == null) return;

        Calendar now = Calendar.getInstance();
        Calendar dayDate = (Calendar) weekStartCalendar.clone();
        dayDate.add(Calendar.DAY_OF_YEAR, dayIndex);

        // Normalize
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar normalizedDay = (Calendar) dayDate.clone();
        normalizedDay.set(Calendar.HOUR_OF_DAY, 0);
        normalizedDay.set(Calendar.MINUTE, 0);
        normalizedDay.set(Calendar.SECOND, 0);
        normalizedDay.set(Calendar.MILLISECOND, 0);

        boolean isToday = today.equals(normalizedDay);
        boolean isPast = normalizedDay.before(today);

        View btnBreakfast = dayCardViews[dayIndex].findViewById(R.id.btn_save_breakfast);
        View btnLunch = dayCardViews[dayIndex].findViewById(R.id.btn_save_lunch);
        View btnDinner = dayCardViews[dayIndex].findViewById(R.id.btn_save_dinner);

        if (isPast) {
            breakfastInputs[dayIndex].setEnabled(false);
            btnBreakfast.setEnabled(false);
            lunchInputs[dayIndex].setEnabled(false);
            btnLunch.setEnabled(false);
            dinnerInputs[dayIndex].setEnabled(false);
            btnDinner.setEnabled(false);
        } else if (isToday) {
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            boolean lunchCutoffPassed = (currentHour > lunchCutoffHour) ||
                    (currentHour == lunchCutoffHour && currentMinute >= lunchCutoffMinute);
            boolean dinnerCutoffPassed = (currentHour > dinnerCutoffHour) ||
                    (currentHour == dinnerCutoffHour && currentMinute >= dinnerCutoffMinute);

            breakfastInputs[dayIndex].setEnabled(true);
            btnBreakfast.setEnabled(true);

            lunchInputs[dayIndex].setEnabled(!lunchCutoffPassed);
            btnLunch.setEnabled(!lunchCutoffPassed);

            dinnerInputs[dayIndex].setEnabled(!dinnerCutoffPassed);
            btnDinner.setEnabled(!dinnerCutoffPassed);
        } else {
            breakfastInputs[dayIndex].setEnabled(true);
            btnBreakfast.setEnabled(true);
            lunchInputs[dayIndex].setEnabled(true);
            btnLunch.setEnabled(true);
            dinnerInputs[dayIndex].setEnabled(true);
            btnDinner.setEnabled(true);
        }
    }

    // ════════════════════════════════════════════════════════
    // Save Menu
    // ════════════════════════════════════════════════════════

    private void saveMenu(int dayIndex, String mealType) {
        if (binding == null || currentMessId == null) {
            Toast.makeText(getContext(), "Mess ID not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar dayDate = (Calendar) weekStartCalendar.clone();
        dayDate.add(Calendar.DAY_OF_YEAR, dayIndex);

        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(dayDate.getTime());

        String menuText;
        switch (mealType) {
            case "breakfast":
                menuText = breakfastInputs[dayIndex].getText() != null
                        ? breakfastInputs[dayIndex].getText().toString().trim() : "";
                break;
            case "lunch":
                menuText = lunchInputs[dayIndex].getText() != null
                        ? lunchInputs[dayIndex].getText().toString().trim() : "";
                break;
            default:
                menuText = dinnerInputs[dayIndex].getText() != null
                        ? dinnerInputs[dayIndex].getText().toString().trim() : "";
                break;
        }

        // Check time restrictions
        Calendar now = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar normalizedDay = (Calendar) dayDate.clone();
        normalizedDay.set(Calendar.HOUR_OF_DAY, 0);
        normalizedDay.set(Calendar.MINUTE, 0);
        normalizedDay.set(Calendar.SECOND, 0);
        normalizedDay.set(Calendar.MILLISECOND, 0);

        boolean isToday = today.equals(normalizedDay);
        boolean isPast = normalizedDay.before(today);

        if (isPast) {
            Toast.makeText(getContext(), "Cannot save menu for past dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isToday) {
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            if (mealType.equals("lunch")) {
                boolean cutoffPassed = (currentHour > lunchCutoffHour) ||
                        (currentHour == lunchCutoffHour && currentMinute >= lunchCutoffMinute);
                if (cutoffPassed) {
                    String cutoffTime = String.format(Locale.getDefault(), "%02d:%02d", lunchCutoffHour, lunchCutoffMinute);
                    Toast.makeText(getContext(),
                            "Lunch cutoff (" + cutoffTime + ") has passed.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            } else if (mealType.equals("dinner")) {
                boolean cutoffPassed = (currentHour > dinnerCutoffHour) ||
                        (currentHour == dinnerCutoffHour && currentMinute >= dinnerCutoffMinute);
                if (cutoffPassed) {
                    String cutoffTime = String.format(Locale.getDefault(), "%02d:%02d", dinnerCutoffHour, dinnerCutoffMinute);
                    Toast.makeText(getContext(),
                            "Dinner cutoff (" + cutoffTime + ") has passed.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        DocumentReference menuRef = db.collection("menus").document(currentMessId + "_" + formattedDate);

        menuRef.get().addOnSuccessListener(documentSnapshot -> {
            if (binding == null) return;
            Map<String, Object> menuData = new HashMap<>();
            if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                menuData.putAll(documentSnapshot.getData());
            }

            menuData.put("messId", currentMessId);
            menuData.put("date", formattedDate);
            menuData.put(mealType, menuText);

            menuRef.set(menuData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        if (binding == null) return;
                        String label = mealType.substring(0, 1).toUpperCase() + mealType.substring(1);
                        String message = menuText.isEmpty()
                                ? label + " menu removed for " + DAY_NAMES[dayIndex]
                                : label + " menu saved for " + DAY_NAMES[dayIndex];
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                        // Update status
                        checkAndUpdateDayStatus(dayIndex);
                    })
                    .addOnFailureListener(e -> {
                        if (binding == null) return;
                        Toast.makeText(getContext(), "Error saving: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void checkAndUpdateDayStatus(int dayIndex) {
        String breakfast = breakfastInputs[dayIndex].getText() != null
                ? breakfastInputs[dayIndex].getText().toString().trim() : "";
        String lunch = lunchInputs[dayIndex].getText() != null
                ? lunchInputs[dayIndex].getText().toString().trim() : "";
        String dinner = dinnerInputs[dayIndex].getText() != null
                ? dinnerInputs[dayIndex].getText().toString().trim() : "";

        boolean anySet = !breakfast.isEmpty() || !lunch.isEmpty() || !dinner.isEmpty();
        hasMenu[dayIndex] = anySet;

        if (statusIcons[dayIndex] != null) {
            statusIcons[dayIndex].setVisibility(anySet ? View.VISIBLE : View.GONE);
        }
        updateMenuSummary();
    }

    // ════════════════════════════════════════════════════════
    // Utility
    // ════════════════════════════════════════════════════════

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
