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
import com.example.messapp.managers.ReviewManager;
import com.example.messapp.databinding.FragmentMessDetailBinding;
import com.example.messapp.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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
        String userId = currentUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("messId", messId);

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(
                        aVoid -> Toast.makeText(getContext(), "Successfully subscribed!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "Subscription failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
