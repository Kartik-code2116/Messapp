package com.example.messapp.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.messapp.Mess;
import com.example.messapp.MessReviewsActivity;
import com.example.messapp.R;
import com.example.messapp.managers.PaymentManager;
import com.example.messapp.managers.ReviewManager;
import com.example.messapp.databinding.FragmentMessDetailBinding;
import com.example.messapp.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessDetailFragment extends Fragment {

    private FragmentMessDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ReviewManager reviewManager;
    private String messId;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messId = getArguments().getString("messId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentMessDetailBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        reviewManager = new ReviewManager();

        setupRecyclerView();
        loadMessDetails();
        loadReviews();
        updateReviewFormVisibility();

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
        binding.btnSubscribeMess.setOnClickListener(v -> subscribeToMess());
        binding.btnSeeReviews.setOnClickListener(v -> openReviewsScreen());
        binding.btnWriteReview.setOnClickListener(v -> scrollToReviewForm());

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        binding.recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewReviews.setAdapter(reviewAdapter);
    }

    private void loadMessDetails() {
        if (messId == null)
            return;

        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Mess mess = documentSnapshot.toObject(Mess.class);
                        if (mess != null) {
                            binding.textMessDetailName.setText(mess.getName());
                            binding.textMessDetailLocation.setText(valueOrFallback(mess.getLocation(), "Location not set"));
                            binding.textMessDetailContact.setText("Contact: " + valueOrFallback(mess.getContact(), "Not set"));
                            binding.textMessDetailDescription.setText(valueOrFallback(mess.getDescription(), "No description available"));
                            // Assuming Mess model has these fields or load them from dailyMenu
                            loadDailyMenu(messId);
                        }
                    }
                });
    }

    private void loadDailyMenu(String messId) {
        db.collection("dailyMenu").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lunch = documentSnapshot.getString("lunch");
                        String dinner = documentSnapshot.getString("dinner");
                        binding.textMessDetailLunchMenu.setText(lunch != null ? lunch : "Not set");
                        binding.textMessDetailDinnerMenu.setText(dinner != null ? dinner : "Not set");
                    } else {
                        binding.textMessDetailLunchMenu.setText("Not available");
                        binding.textMessDetailDinnerMenu.setText("Not available");
                    }
                });
    }

    private void loadReviews() {
        if (messId == null) {
            return;
        }

        reviewManager.getMessReviews(messId, new ReviewManager.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                if (binding == null) return;
                    reviewList.clear();
                    float totalRating = 0;
                for (Review review : reviews) {
                        reviewList.add(review);
                        totalRating += review.getRating();
                    }
                    reviewAdapter.notifyDataSetChanged();

                    if (!reviewList.isEmpty()) {
                        float avgRating = totalRating / reviewList.size();
                        binding.ratingBarAvg.setRating(avgRating);
                        binding.textAvgRating.setText(String.format("%.1f (%d Reviews)", avgRating, reviewList.size()));
                    } else {
                        binding.ratingBarAvg.setRating(0);
                        binding.textAvgRating.setText("No Reviews");
                    }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load reviews: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void submitReview() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }

        String comment = binding.reviewCommentEditText.getText().toString().trim();
        float rating = binding.ratingBarUser.getRating();

        if (TextUtils.isEmpty(comment)) {
            Toast.makeText(getContext(), "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating == 0) {
            Toast.makeText(getContext(), "Please provide a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitReview.setEnabled(false);
        String userId = currentUser.getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            if (binding == null) return;
            String userName = userDoc.getString("name");
            if (userName == null)
                userName = "Student";

            reviewManager.createReview(messId, rating, comment, userName, new ReviewManager.UpdateCallback() {
                @Override
                public void onSuccess() {
                    if (binding == null) return;
                        Toast.makeText(getContext(), "Review submitted!", Toast.LENGTH_SHORT).show();
                        binding.reviewCommentEditText.setText("");
                        binding.ratingBarUser.setRating(0);
                    binding.btnSubmitReview.setEnabled(true);

                        // Log Firebase Analytics submit_review event
                        Bundle bundle = new Bundle();
                        bundle.putString("mess_id", messId);
                        bundle.putDouble("rating", rating);
                        FirebaseAnalytics.getInstance(requireContext()).logEvent("submit_review", bundle);

                        loadReviews();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (binding == null) return;
                    binding.btnSubmitReview.setEnabled(true);
                    Toast.makeText(getContext(), "Failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            if (binding == null) return;
            binding.btnSubmitReview.setEnabled(true);
            Toast.makeText(getContext(), "Failed to load user profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void subscribeToMess() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to subscribe", Toast.LENGTH_SHORT).show();
            return;
        }

        if (messId == null) {
            Toast.makeText(getContext(), "Mess details not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading indicator while fetching the mess price
        binding.btnSubscribeMess.setEnabled(false);

        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null || getContext() == null) return;
                    binding.btnSubscribeMess.setEnabled(true);
                    if (documentSnapshot.exists()) {
                        Mess mess = documentSnapshot.toObject(Mess.class);
                        if (mess != null) {
                            showSubscriptionPlansDialog(currentUser.getUid(), mess);
                        } else {
                            Toast.makeText(getContext(), "Failed to load mess information", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Mess details not found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null || getContext() == null) return;
                    binding.btnSubscribeMess.setEnabled(true);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSubscriptionPlansDialog(String userId, Mess mess) {
        if (getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_grant_subscription, null);

        // Update the title and info for student purchase context
        android.widget.TextView textTitle = dialogView.findViewById(android.R.id.title);
        android.view.ViewGroup container = (android.view.ViewGroup) ((android.view.ViewGroup) dialogView).getChildAt(0);
        android.widget.TextView tvTitle = (android.widget.TextView) container.getChildAt(0);
        tvTitle.setText("Subscribe to " + mess.getName());

        android.widget.TextView tvDescription = (android.widget.TextView) container.getChildAt(1);
        tvDescription.setText("Select your preferred meal type and duration to subscribe.");

        com.google.android.material.textfield.TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        com.google.android.material.textfield.TextInputEditText etDays = dialogView.findViewById(R.id.etDays);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group_meal_type);
        android.widget.TextView textOneTimeInfo = dialogView.findViewById(R.id.text_one_time_info);

        // Prepopulate defaults
        double monthlyPrice = mess.getMonthlyPrice() > 0 ? mess.getMonthlyPrice() : 3000.0;
        etDays.setText("30");
        etAmount.setText(String.valueOf((int) monthlyPrice));
        etAmount.setEnabled(false); // Make it read-only for students

        // Listen for changes in days or meal type to update amount
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSubscriptionPrice(etDays, etAmount, radioGroup, monthlyPrice);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
        etDays.addTextChangedListener(watcher);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            textOneTimeInfo.setVisibility(checkedId == R.id.radio_one_time ? View.VISIBLE : View.GONE);
            updateSubscriptionPrice(etDays, etAmount, radioGroup, monthlyPrice);
        });

        // Initialize state
        radioGroup.check(R.id.radio_both); // Set default to BOTH meals

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton("Proceed to Payment", (dialog, which) -> {
                    String daysStr = etDays.getText().toString().trim();
                    if (android.text.TextUtils.isEmpty(daysStr)) {
                        Toast.makeText(getContext(), "Please enter subscription days", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int days = Integer.parseInt(daysStr);
                    if (days <= 0) {
                        Toast.makeText(getContext(), "Days must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String mealType = "BOTH";
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    if (selectedId == R.id.radio_lunch) mealType = "LUNCH";
                    else if (selectedId == R.id.radio_dinner) mealType = "DINNER";
                    else if (selectedId == R.id.radio_one_time) mealType = "ONE_TIME";

                    double finalAmount = calculateSubscriptionAmount(days, mealType, monthlyPrice);
                    processSimulatedPayment(userId, mess.getMessId(), mealType, finalAmount, days);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSubscriptionPrice(com.google.android.material.textfield.TextInputEditText etDays,
                                         com.google.android.material.textfield.TextInputEditText etAmount,
                                         android.widget.RadioGroup radioGroup,
                                         double monthlyPrice) {
        String daysStr = etDays.getText().toString().trim();
        int days = 30;
        if (!android.text.TextUtils.isEmpty(daysStr)) {
            try {
                days = Integer.parseInt(daysStr);
            } catch (NumberFormatException e) {
                days = 30;
            }
        }

        String mealType = "BOTH";
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radio_lunch) mealType = "LUNCH";
        else if (selectedId == R.id.radio_dinner) mealType = "DINNER";
        else if (selectedId == R.id.radio_one_time) mealType = "ONE_TIME";

        double amount = calculateSubscriptionAmount(days, mealType, monthlyPrice);
        etAmount.setText(String.valueOf((int) amount));
    }

    private double calculateSubscriptionAmount(int days, String mealType, double monthlyPrice) {
        double factor = 1.0;
        if ("LUNCH".equals(mealType) || "DINNER".equals(mealType)) {
            factor = 0.55; // 55% of price for single meal
        } else if ("ONE_TIME".equals(mealType)) {
            factor = 0.65; // 65% of price for flexi single meal
        }
        return (monthlyPrice * (days / 30.0)) * factor;
    }

    private void processSimulatedPayment(String userId, String messId, String mealType, double amount, int days) {
        if (getContext() == null || binding == null) return;

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
        progressDialog.setMessage("Processing payment... Please do not close the app.");
        progressDialog.setCancelable(false);
        progressDialog.show();

        PaymentManager paymentManager = new PaymentManager();
        paymentManager.processPayment(userId, messId, mealType, amount, days, new PaymentManager.PaymentCallback() {
            @Override
            public void onSuccess(String transactionId) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (binding == null || getContext() == null) return;
                Toast.makeText(getContext(), "Subscription active! Payment receipt: " + transactionId, Toast.LENGTH_LONG).show();

                // Log Firebase Analytics PURCHASE event
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.TRANSACTION_ID, transactionId);
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, messId);
                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, mealType);
                bundle.putDouble(FirebaseAnalytics.Param.VALUE, amount);
                bundle.putString(FirebaseAnalytics.Param.CURRENCY, "INR");
                FirebaseAnalytics.getInstance(requireContext()).logEvent(FirebaseAnalytics.Event.PURCHASE, bundle);

                loadMessDetails(); // Refresh view
            }

            @Override
            public void onFailure(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if (getContext() == null) return;
                Toast.makeText(getContext(), "Payment Failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateReviewFormVisibility() {
        boolean isLoggedIn = mAuth.getCurrentUser() != null;
        binding.textAddReview.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        binding.cardAddReview.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        binding.btnWriteReview.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
    }

    private void openReviewsScreen() {
        if (messId == null) {
            Toast.makeText(getContext(), "Mess not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), MessReviewsActivity.class);
        intent.putExtra(MessReviewsActivity.EXTRA_MESS_ID, messId);
        startActivity(intent);
    }

    private void scrollToReviewForm() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login to write a review", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.getRoot().smoothScrollTo(0, binding.cardAddReview.getTop());
        binding.reviewCommentEditText.requestFocus();
    }

    private String valueOrFallback(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
