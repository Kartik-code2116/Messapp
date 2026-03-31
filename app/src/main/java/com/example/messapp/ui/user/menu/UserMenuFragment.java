package com.example.messapp.ui.user.menu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.messapp.R;
import com.example.messapp.databinding.FragmentUserMenuBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class UserMenuFragment extends Fragment {

    private FragmentUserMenuBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private Calendar currentCalendar;
    private boolean isDaySelectorExpanded = false;
    private SimpleDateFormat dayFormat;
    private SimpleDateFormat dateFormat;
    private String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private int selectedDayIndex = -1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentCalendar = Calendar.getInstance();

        dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Set default day to today
        selectedDayIndex = getDayOfWeekIndex(currentCalendar);

        fetchUserMessId();

        // Setup FAB click listener for expand/collapse animation
        binding.fabDaySelector.setOnClickListener(v -> toggleDaySelector());

        return root;
    }

    private void fetchUserMessId() {
        if (mAuth.getCurrentUser() == null) {
            binding.textLunchMenuDisplay.setText("Sign in to view menu");
            binding.textDinnerMenuDisplay.setText("Sign in to view menu");
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentMessId = documentSnapshot.getString("messId");
                        if (currentMessId != null) {
                            setupDaySelector();
                            loadMenuForDate(currentCalendar);
                        }
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
                });
    }

    private int getDayOfWeekIndex(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        // Convert to 0-based index where Monday = 0
        return (dayOfWeek + 5) % 7;
    }

    private void toggleDaySelector() {
        isDaySelectorExpanded = !isDaySelectorExpanded;

        // Rotate FAB icon 180 degrees
        RotateAnimation rotate = new RotateAnimation(
                isDaySelectorExpanded ? 0 : 180,
                isDaySelectorExpanded ? 180 : 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(300);
        rotate.setFillAfter(true);
        binding.fabDaySelector.startAnimation(rotate);

        // Animate day selector container visibility
        if (isDaySelectorExpanded) {
            expandDaySelector();
        } else {
            collapseDaySelector();
        }
    }

    private void expandDaySelector() {
        binding.daySelectorExpandableContainer.setVisibility(View.VISIBLE);
        binding.daySelectorExpandableContainer.setAlpha(0f);
        binding.daySelectorExpandableContainer.setTranslationY(-50f);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(binding.daySelectorExpandableContainer, "alpha", 0f, 1f);
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(binding.daySelectorExpandableContainer, "translationY", -50f, 0f);

        alphaAnim.setDuration(300);
        translateAnim.setDuration(300);

        alphaAnim.start();
        translateAnim.start();
    }

    private void collapseDaySelector() {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(binding.daySelectorExpandableContainer, "alpha", 1f, 0f);
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(binding.daySelectorExpandableContainer, "translationY", 0f, -50f);

        alphaAnim.setDuration(300);
        translateAnim.setDuration(300);

        alphaAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.daySelectorExpandableContainer.setVisibility(View.GONE);
            }
        });

        alphaAnim.start();
        translateAnim.start();
    }

    private void setupDaySelector() {
        LinearLayout daySelectorContainer = binding.daySelectorContainer;
        daySelectorContainer.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start from Monday

        for (int i = 0; i < 7; i++) {
            Button dayButton = createCircularDayButton(i, calendar);
            daySelectorContainer.addView(dayButton);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Highlight today
        updateDayButtonStyles();
    }

    private Button createCircularDayButton(int index, Calendar calendar) {
        Button button = new Button(getContext());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(4, 4, 4, 4);
        button.setLayoutParams(params);

        String dayName = dayNames[index];
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        button.setText(dayName + "\n" + dayOfMonth);
        button.setTextSize(10f);
        button.setTag(index);

        // Create circular background
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.TRANSPARENT);
        button.setBackground(drawable);
        button.setTextColor(getResources().getColor(R.color.text_body));

        button.setOnClickListener(v -> onCircularDaySelected(index, calendar));

        return button;
    }

    private void onCircularDaySelected(int index, Calendar baseCalendar) {
        selectedDayIndex = index;

        // Calculate the actual date based on index
        Calendar selectedCal = (Calendar) baseCalendar.clone();
        selectedCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        selectedCal.add(Calendar.DAY_OF_YEAR, index);

        currentCalendar = selectedCal;
        loadMenuForDate(currentCalendar);
        updateDayButtonStyles();

        // Auto collapse after selection with delay
        binding.daySelectorExpandableContainer.postDelayed(() -> {
            if (isDaySelectorExpanded) {
                toggleDaySelector();
            }
        }, 500);
    }

    private void updateDayButtonStyles() {
        LinearLayout daySelectorContainer = binding.daySelectorContainer;
        for (int i = 0; i < daySelectorContainer.getChildCount(); i++) {
            Button button = (Button) daySelectorContainer.getChildAt(i);
            int index = (int) button.getTag();

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);

            if (index == selectedDayIndex) {
                drawable.setColor(getResources().getColor(R.color.brand_primary));
                button.setTextColor(Color.WHITE);
                // Add scale animation for selected
                button.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                drawable.setColor(getResources().getColor(R.color.neutral_100));
                button.setTextColor(getResources().getColor(R.color.text_body));
                button.setScaleX(1f);
                button.setScaleY(1f);
            }

            button.setBackground(drawable);
        }
    }

    private void loadMenuForDate(Calendar calendar) {
        if (currentMessId == null) {
            return;
        }
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        db.collection("menus")
                .whereEqualTo("messId", currentMessId)
                .whereEqualTo("date", formattedDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String lunchMenu = queryDocumentSnapshots.getDocuments().get(0).getString("lunch");
                        String dinnerMenu = queryDocumentSnapshots.getDocuments().get(0).getString("dinner");

                        binding.textLunchMenuDisplay.setText(lunchMenu != null ? lunchMenu : "No lunch menu available.");
                        binding.textDinnerMenuDisplay.setText(dinnerMenu != null ? dinnerMenu : "No dinner menu available.");
                    } else {
                        binding.textLunchMenuDisplay.setText("No menu set for this day.");
                        binding.textDinnerMenuDisplay.setText("No menu set for this day.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading menu for " + formattedDate, Toast.LENGTH_SHORT).show();
                    binding.textLunchMenuDisplay.setText("Error loading menu.");
                    binding.textDinnerMenuDisplay.setText("Error loading menu.");
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
