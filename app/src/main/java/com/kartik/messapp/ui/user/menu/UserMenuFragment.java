package com.kartik.messapp.ui.user.menu;

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
import com.kartik.messapp.databinding.FragmentUserMenuBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UserMenuFragment extends Fragment {

    private FragmentUserMenuBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private Calendar weekStartCalendar;

    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final String[] DAY_SHORT  = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // Per-day view references
    private final View[]        dayCardViews     = new View[7];
    private final LinearLayout[] mealLayouts     = new LinearLayout[7];
    private final ImageView[]   expandIcons      = new ImageView[7];
    private final TextView[]    breakfastTvs     = new TextView[7];
    private final TextView[]    lunchTvs         = new TextView[7];
    private final TextView[]    dinnerTvs        = new TextView[7];
    private final LinearLayout[] breakfastSections = new LinearLayout[7];
    private final boolean[]     isExpanded       = new boolean[7];

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserMenuBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Start from Monday of the current week
        weekStartCalendar = Calendar.getInstance();
        weekStartCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        setupWeekNavigation();
        setupDayChips();
        buildDayCards(inflater);
        fetchUserMessId();

        binding.swipeRefreshUserMenu.setColorSchemeResources(
                R.color.brand_primary, R.color.state_success, R.color.brand_primary_dark);
        binding.swipeRefreshUserMenu.setOnRefreshListener(() -> {
            loadMenuForCurrentWeek();
            binding.swipeRefreshUserMenu.setRefreshing(false);
        });

        return binding.getRoot();
    }

    // ────────────────────────────────────────────────────────
    // Week navigation
    // ────────────────────────────────────────────────────────

    private void setupWeekNavigation() {
        updateWeekDisplay();

        binding.btnPrevWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            updateWeekDisplay();
            refreshChipsAndCards();
        });

        binding.btnNextWeek.setOnClickListener(v -> {
            weekStartCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            updateWeekDisplay();
            refreshChipsAndCards();
        });
    }

    private void updateWeekDisplay() {
        if (binding == null) return;

        Calendar weekEnd = (Calendar) weekStartCalendar.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);

        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd", Locale.getDefault());
        binding.textWeekRange.setText(
                fmt.format(weekStartCalendar.getTime()) + " – " + fmt.format(weekEnd.getTime()));

        // Label: This Week / Past Week / Upcoming Week
        Calendar thisMonday = Calendar.getInstance();
        thisMonday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        thisMonday.set(Calendar.HOUR_OF_DAY, 0); thisMonday.set(Calendar.MINUTE, 0);
        thisMonday.set(Calendar.SECOND, 0);      thisMonday.set(Calendar.MILLISECOND, 0);

        Calendar compareStart = (Calendar) weekStartCalendar.clone();
        compareStart.set(Calendar.HOUR_OF_DAY, 0); compareStart.set(Calendar.MINUTE, 0);
        compareStart.set(Calendar.SECOND, 0);       compareStart.set(Calendar.MILLISECOND, 0);

        if (compareStart.equals(thisMonday))       binding.textWeekLabel.setText("This Week");
        else if (compareStart.before(thisMonday))  binding.textWeekLabel.setText("Past Week");
        else                                        binding.textWeekLabel.setText("Upcoming Week");
    }

    // ────────────────────────────────────────────────────────
    // Day chip strip
    // ────────────────────────────────────────────────────────

    private void setupDayChips() {
        buildChips();
    }

    private void buildChips() {
        if (binding == null || getContext() == null) return;
        binding.chipGroupDays.removeAllViews();

        Calendar dayCal   = (Calendar) weekStartCalendar.clone();
        int todayDOY  = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);

        for (int i = 0; i < 7; i++) {
            Chip chip = new Chip(getContext());
            chip.setText(DAY_SHORT[i] + " " + dayCal.get(Calendar.DAY_OF_MONTH));
            chip.setCheckable(true);
            chip.setTextSize(12f);
            chip.setChipCornerRadius(40f);

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDOY
                    && dayCal.get(Calendar.YEAR) == todayYear;

            if (isToday) {
                chip.setChecked(true);
                chip.setChipBackgroundColorResource(R.color.brand_primary);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            } else {
                chip.setChipBackgroundColorResource(R.color.neutral_100);
                chip.setTextColor(ContextCompat.getColor(getContext(), R.color.text_body));
            }

            final int idx = i;
            chip.setOnClickListener(v -> scrollToDayCard(idx));

            binding.chipGroupDays.addView(chip);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void scrollToDayCard(int dayIndex) {
        if (binding == null || dayCardViews[dayIndex] == null) return;

        if (!isExpanded[dayIndex]) toggleDayCard(dayIndex);

        binding.scrollView.post(() -> {
            if (binding != null && dayCardViews[dayIndex] != null) {
                binding.scrollView.smoothScrollTo(0, dayCardViews[dayIndex].getTop() - 24);
            }
        });
    }

    // ────────────────────────────────────────────────────────
    // Build day cards
    // ────────────────────────────────────────────────────────

    private void buildDayCards(LayoutInflater inflater) {
        if (binding == null || getContext() == null) return;
        binding.containerDayCards.removeAllViews();

        Calendar dayCal   = (Calendar) weekStartCalendar.clone();
        int todayDOY  = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);
        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd", Locale.getDefault());

        int todayIndex = -1;

        // Accent colours cycling through a warm/vibrant palette
        int[] accentColors = {
                0xFFF97316, // Orange
                0xFF10B981, // Emerald
                0xFF3B82F6, // Blue
                0xFFF59E0B, // Amber
                0xFFEC4899, // Pink
                0xFF8B5CF6, // Purple
                0xFFEF4444  // Red
        };

        for (int i = 0; i < 7; i++) {
            View card = inflater.inflate(R.layout.item_user_menu_day_card,
                    binding.containerDayCards, false);

            TextView dayNameTv   = card.findViewById(R.id.text_day_name);
            TextView dayDateTv   = card.findViewById(R.id.text_day_date);
            TextView badgeToday  = card.findViewById(R.id.badge_today);
            View     accentBar   = card.findViewById(R.id.view_accent_bar);
            LinearLayout header  = card.findViewById(R.id.layout_day_header);
            LinearLayout content = card.findViewById(R.id.layout_meal_content);
            ImageView expandIcon = card.findViewById(R.id.icon_expand);

            TextView breakfastTv      = card.findViewById(R.id.text_breakfast_display);
            TextView lunchTv          = card.findViewById(R.id.text_lunch_display);
            TextView dinnerTv         = card.findViewById(R.id.text_dinner_display);
            LinearLayout bfSection    = card.findViewById(R.id.section_breakfast);

            dayNameTv.setText(DAY_NAMES[i]);
            dayDateTv.setText(dateFmt.format(dayCal.getTime()));
            accentBar.setBackgroundColor(accentColors[i]);

            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDOY
                    && dayCal.get(Calendar.YEAR) == todayYear;

            if (isToday) {
                todayIndex = i;
                badgeToday.setVisibility(View.VISIBLE);
                if (card instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView mcv =
                            (com.google.android.material.card.MaterialCardView) card;
                    mcv.setStrokeColor(ContextCompat.getColor(getContext(), R.color.brand_primary));
                    mcv.setStrokeWidth(dpToPx(2));
                }
            }

            // Store references
            dayCardViews[i]      = card;
            mealLayouts[i]       = content;
            expandIcons[i]       = expandIcon;
            breakfastTvs[i]      = breakfastTv;
            lunchTvs[i]          = lunchTv;
            dinnerTvs[i]         = dinnerTv;
            breakfastSections[i] = bfSection;
            isExpanded[i]        = false;

            final int idx = i;
            header.setOnClickListener(v -> toggleDayCard(idx));

            binding.containerDayCards.addView(card);
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Auto-expand today
        if (todayIndex >= 0) {
            final int ti = todayIndex;
            binding.containerDayCards.post(() -> {
                if (binding != null) {
                    toggleDayCard(ti);
                    binding.scrollView.postDelayed(() -> {
                        if (binding != null && dayCardViews[ti] != null) {
                            binding.scrollView.smoothScrollTo(0, dayCardViews[ti].getTop() - 24);
                        }
                    }, 300);
                }
            });
        }
    }

    private void refreshChipsAndCards() {
        if (binding == null) return;

        // Reset all cards
        for (int i = 0; i < 7; i++) {
            isExpanded[i] = false;
            if (mealLayouts[i] != null) {
                mealLayouts[i].setAlpha(0f);
                mealLayouts[i].setVisibility(View.GONE);
            }
            if (expandIcons[i] != null) expandIcons[i].setRotation(90);
            if (breakfastTvs[i] != null) breakfastTvs[i].setText("—");
            if (lunchTvs[i]     != null) lunchTvs[i].setText("—");
            if (dinnerTvs[i]    != null) dinnerTvs[i].setText("—");
        }

        buildChips();
        updateDayCardDates();
        if (currentMessId != null) loadMenuForCurrentWeek();
    }

    private void updateDayCardDates() {
        if (getContext() == null) return;
        Calendar dayCal   = (Calendar) weekStartCalendar.clone();
        int todayDOY  = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int todayYear = Calendar.getInstance().get(Calendar.YEAR);
        SimpleDateFormat dateFmt = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            if (dayCardViews[i] == null) continue;

            TextView dayDateTv  = dayCardViews[i].findViewById(R.id.text_day_date);
            TextView badgeToday = dayCardViews[i].findViewById(R.id.badge_today);

            dayDateTv.setText(dateFmt.format(dayCal.getTime()));
            boolean isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayDOY
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

            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void toggleDayCard(int dayIndex) {
        if (binding == null || mealLayouts[dayIndex] == null) return;
        isExpanded[dayIndex] = !isExpanded[dayIndex];

        if (isExpanded[dayIndex]) {
            mealLayouts[dayIndex].setVisibility(View.VISIBLE);
            mealLayouts[dayIndex].setAlpha(0f);
            mealLayouts[dayIndex].animate().alpha(1f).setDuration(220).start();
            expandIcons[dayIndex].animate().rotation(270).setDuration(200).start();
        } else {
            mealLayouts[dayIndex].animate().alpha(0f).setDuration(180)
                    .withEndAction(() -> {
                        if (mealLayouts[dayIndex] != null)
                            mealLayouts[dayIndex].setVisibility(View.GONE);
                    }).start();
            expandIcons[dayIndex].animate().rotation(90).setDuration(200).start();
        }
    }

    // ────────────────────────────────────────────────────────
    // Data loading
    // ────────────────────────────────────────────────────────

    private void fetchUserMessId() {
        if (mAuth.getCurrentUser() == null) {
            setAllMenuText("Sign in to view menu");
            return;
        }

        if (binding != null) binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (doc.exists()) {
                        currentMessId = doc.getString("messId");
                        if (currentMessId != null) {
                            loadMenuForCurrentWeek();
                        } else {
                            setAllMenuText("No mess linked to your account");
                        }
                    } else {
                        setAllMenuText("User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMenuForCurrentWeek() {
        if (currentMessId == null || binding == null) return;

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Calendar weekEnd = (Calendar) weekStartCalendar.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        String startDate = dateFmt.format(weekStartCalendar.getTime());
        String endDate   = dateFmt.format(weekEnd.getTime());

        // Reset display text while loading
        for (int i = 0; i < 7; i++) {
            if (lunchTvs[i]  != null) lunchTvs[i].setText("Loading…");
            if (dinnerTvs[i] != null) dinnerTvs[i].setText("Loading…");
            if (breakfastSections[i] != null) breakfastSections[i].setVisibility(View.GONE);
        }

        java.util.List<String> docIds = new java.util.ArrayList<>();
        Calendar dayCalBuilder = (Calendar) weekStartCalendar.clone();
        for (int i = 0; i < 7; i++) {
            docIds.add(currentMessId + "_" + dateFmt.format(dayCalBuilder.getTime()));
            dayCalBuilder.add(Calendar.DAY_OF_YEAR, 1);
        }

        // === SINGLE query for all 7 days using document IDs ===
        db.collection("menus")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), docIds)
                .get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;

                    // Default all days to "no menu set"
                    for (int i = 0; i < 7; i++) {
                        if (lunchTvs[i]  != null) lunchTvs[i].setText("No lunch menu set");
                        if (dinnerTvs[i] != null) dinnerTvs[i].setText("No dinner menu set");
                    }

                    // Map returned documents to day indices
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String docDate = doc.getString("date");
                        if (docDate == null) continue;

                        Calendar dayCal = (Calendar) weekStartCalendar.clone();
                        for (int i = 0; i < 7; i++) {
                            if (docDate.equals(dateFmt.format(dayCal.getTime()))) {
                                String b = doc.getString("breakfast");
                                String l = doc.getString("lunch");
                                String d = doc.getString("dinner");

                                String breakfast = (b != null) ? b.trim() : "";
                                String lunch     = (l != null) ? l.trim() : "";
                                String dinner    = (d != null) ? d.trim() : "";

                                boolean hasBreakfast = !breakfast.isEmpty();
                                if (breakfastSections[i] != null)
                                    breakfastSections[i].setVisibility(hasBreakfast ? View.VISIBLE : View.GONE);
                                if (breakfastTvs[i] != null)
                                    breakfastTvs[i].setText(breakfast);

                                if (lunchTvs[i] != null)
                                    lunchTvs[i].setText(lunch.isEmpty() ? "No lunch menu set" : lunch);
                                if (dinnerTvs[i] != null)
                                    dinnerTvs[i].setText(dinner.isEmpty() ? "No dinner menu set" : dinner);
                                break;
                            }
                            dayCal.add(Calendar.DAY_OF_YEAR, 1);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    for (int i = 0; i < 7; i++) {
                        if (lunchTvs[i]  != null) lunchTvs[i].setText("Error loading menu");
                        if (dinnerTvs[i] != null) dinnerTvs[i].setText("Error loading menu");
                    }
                });
    }


    private void setAllMenuText(String msg) {
        for (int i = 0; i < 7; i++) {
            if (lunchTvs[i]  != null) lunchTvs[i].setText(msg);
            if (dinnerTvs[i] != null) dinnerTvs[i].setText(msg);
        }
    }

    // ────────────────────────────────────────────────────────
    // Utility
    // ────────────────────────────────────────────────────────

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
