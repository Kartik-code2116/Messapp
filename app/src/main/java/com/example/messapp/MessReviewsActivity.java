package com.example.messapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.models.Review;
import com.example.messapp.ui.user.ReviewAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MessReviewsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddReview;
    private TextView textNoReviews;
    private ReviewAdapter adapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;
    private String currentMessId;
    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mess_reviews);

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.items_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mess Reviews");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentMessId = getIntent().getStringExtra("messId");

        recyclerView = findViewById(R.id.recycler_reviews);
        fabAddReview = findViewById(R.id.fab_add_review);
        textNoReviews = findViewById(R.id.text_no_reviews);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        if (currentMessId == null) {
            Toast.makeText(this, "Error: Mess not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchUserName();
        loadReviews();

        fabAddReview.setOnClickListener(v -> showAddReviewDialog());
    }

    private void fetchUserName() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name");
                        if (currentUserName == null)
                            currentUserName = "Anonymous";
                    }
                });
    }

    private void loadReviews() {
        db.collection("reviews")
                .whereEqualTo("messId", currentMessId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reviewList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        reviewList.addAll(queryDocumentSnapshots.toObjects(Review.class));
                        textNoReviews.setVisibility(View.GONE);
                    } else {
                        textNoReviews.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading reviews: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddReviewDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_review, null);

        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        TextInputEditText etComment = dialogView.findViewById(R.id.et_comment);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnSubmit = dialogView.findViewById(R.id.btn_submit_review);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(comment)) {
                etComment.setError("Please enter a comment");
                return;
            }

            submitReview(rating, comment);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReview(float rating, String comment) {
        String reviewId = db.collection("reviews").document().getId();
        Review review = new Review(
                reviewId,
                currentMessId,
                currentUserId,
                currentUserName != null ? currentUserName : "Student",
                rating,
                comment,
                null // ServerTimestamp will fill this
        );

        db.collection("reviews").document(reviewId).set(review)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    loadReviews(); // Refresh list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
