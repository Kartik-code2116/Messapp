package com.example.messapp.ui.user;

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
import com.example.messapp.databinding.FragmentMessDetailBinding;
import com.example.messapp.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessDetailFragment extends Fragment {

    private FragmentMessDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
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

        setupRecyclerView();
        loadMessDetails();
        loadReviews();

        binding.btnSubmitReview.setOnClickListener(v -> submitReview());
        binding.btnSubscribeMess.setOnClickListener(v -> subscribeToMess());

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
                            binding.textMessDetailLocation.setText("Location: " + mess.getLocation());
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
        db.collection("reviews")
                .whereEqualTo("messId", messId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    float totalRating = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Review review = document.toObject(Review.class);
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
                });
    }

    private void submitReview() {
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

        String userId = mAuth.getCurrentUser().getUid();
        // We might need to fetch user name here, but for now using a placeholder or
        // keeping it empty
        // In a real app, you'd fetch the user's name from his profile
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");
            if (userName == null)
                userName = "Anonymous";

            String reviewId = UUID.randomUUID().toString();
            Review review = new Review(reviewId, messId, userId, userName, rating, comment, new Date());

            db.collection("reviews").document(reviewId).set(review)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Review submitted!", Toast.LENGTH_SHORT).show();
                        binding.reviewCommentEditText.setText("");
                        binding.ratingBarUser.setRating(0);
                        loadReviews();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void subscribeToMess() {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("messId", messId);

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(
                        aVoid -> Toast.makeText(getContext(), "Successfully subscribed!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast
                        .makeText(getContext(), "Subscription failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
