package com.example.messapp.ui.user.menu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentCalendar = Calendar.getInstance();

        fetchUserMessId();

        return root;
    }

    private void fetchUserMessId() {
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

    private void setupDaySelector() {
        LinearLayout daySelectorContainer = binding.daySelectorContainer;
        daySelectorContainer.removeAllViews(); // Clear existing views

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()); // Start from Monday

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            Button dayButton = new Button(getContext());
            dayButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            dayButton.setText(dayFormat.format(calendar.getTime()));
            dayButton.setTag(dateFormat.format(calendar.getTime())); // Store date as tag
            dayButton.setOnClickListener(this::onDaySelected);
            dayButton.setBackgroundColor(Color.TRANSPARENT);
            dayButton.setTextColor(getResources().getColor(R.color.black));
            dayButton.setPadding(16, 8, 16, 8);

            daySelectorContainer.addView(dayButton);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void onDaySelected(View view) {
        String selectedDate = (String) view.getTag();
        // Update currentCalendar to the selected date
        try {
            currentCalendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate));
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        loadMenuForDate(currentCalendar);
        highlightSelectedDay(selectedDate);
    }

    private void highlightSelectedDay(String selectedDate) {
        LinearLayout daySelectorContainer = binding.daySelectorContainer;
        for (int i = 0; i < daySelectorContainer.getChildCount(); i++) {
            Button dayButton = (Button) daySelectorContainer.getChildAt(i);
            if (selectedDate.equals(dayButton.getTag())) {
                dayButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                dayButton.setTextColor(getResources().getColor(R.color.white));
            } else {
                dayButton.setBackgroundColor(Color.TRANSPARENT);
                dayButton.setTextColor(getResources().getColor(R.color.black));
            }
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
