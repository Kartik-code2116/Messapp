package com.kartik.messapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.kartik.messapp.adapters.OnboardingAdapter;
import com.kartik.messapp.models.OnboardingItem;
import com.kartik.messapp.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutIndicators;
    private MaterialButton btnNext;
    private TextView btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutIndicators = findViewById(R.id.layout_indicators);
        btnNext = findViewById(R.id.btn_next);
        btnSkip = findViewById(R.id.btn_skip);

        setupOnboardingItems();

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(onboardingAdapter);

        setupIndicators();
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);

                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnNext.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                navigateToRoleSelection();
            }
        });

        btnSkip.setOnClickListener(v -> navigateToRoleSelection());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_restaurant,
                "Smarter Mess Management",
                "Discover the easiest way to organize daily meals, track attendance, and connect students with mess owners."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_calendar,
                "Track Every Meal",
                "Never miss an update. View weekly menus in advance, mark your daily Lunch/Dinner attendance, and manage your subscriptions effortlessly."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_bar_chart,
                "Manage Like a Pro",
                "Keep accurate track of active students, broadcast real-time menu updates, and streamline your revenue through powerful analytics."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_check_circle,
                "Join the Community",
                "Select your role and experience the future of mess management today."
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.bg_onboarding_dot_unselected
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.bg_onboarding_dot_selected
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.bg_onboarding_dot_unselected
                ));
            }
        }
    }

    private void navigateToRoleSelection() {
        // Mark first launch as false
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_first_launch", false).apply();

        Intent intent = new Intent(getApplicationContext(), RoleSelectionActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
