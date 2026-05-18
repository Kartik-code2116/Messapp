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
import com.example.messapp.managers.ReviewManager;
import com.example.messapp.ui.user.ReviewAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MessReviewsActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "messId";
    public static final String EXTRA_OPEN_REVIEW_DIALOG = "OPEN_REVIEW_DIALOG";

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddReview;
    private TextView textNoReviews;
    private ReviewAdapter adapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;
    private ReviewManager reviewManager;
    private String currentMessId;
    private String currentUserId;
    private String currentUserName;
    private String currentRole;

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
        reviewManager = new ReviewManager();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }
        currentMessId = getIntent().getStringExtra(EXTRA_MESS_ID);

        recyclerView = findViewById(R.id.recycler_reviews);
        fabAddReview = findViewById(R.id.fab_add_review);
        textNoReviews = findViewById(R.id.text_no_reviews);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(adapter);

        if (currentUserId != null) {
            fetchUserContextAndLoadReviews();
        } else {
            fabAddReview.setVisibility(View.GONE);
            if (currentMessId == null) {
                Toast.makeText(this, "Please login to view reviews", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            loadMessReviews();
        }

        fabAddReview.setOnClickListener(v -> showAddReviewDialog());
    }

    private void fetchUserContextAndLoadReviews() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("name");
                        if (currentUserName == null)
                            currentUserName = "Student";
                        currentRole = documentSnapshot.getString("role");

                        String profileMessId = documentSnapshot.getString("messId");
                        if ("MESS_OWNER".equals(currentRole)) {
                            currentMessId = profileMessId;
                            fabAddReview.setVisibility(View.GONE);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("User Reviews");
                            }
                            loadMessReviews();
                        } else {
                            if (currentMessId == null) {
                                currentMessId = profileMessId;
                            }
                            fabAddReview.setVisibility(currentMessId != null ? View.VISIBLE : View.GONE);
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("My Reviews");
                            }
                            loadMyReviews();
                            if (getIntent().getBooleanExtra(EXTRA_OPEN_REVIEW_DIALOG, false)) {
                                fabAddReview.post(this::showAddReviewDialog);
                            }
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadMessReviews() {
        if (currentMessId == null) {
            Toast.makeText(this, "Mess not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reviewManager.getMessReviews(currentMessId, new ReviewManager.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                reviewList.clear();
                if (!reviews.isEmpty()) {
                    reviewList.addAll(reviews);
                    textNoReviews.setVisibility(View.GONE);
                } else {
                    textNoReviews.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MessReviewsActivity.this, "Error loading reviews: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMyReviews() {
        reviewManager.getReviewsForUser(currentUserId, new ReviewManager.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                reviewList.clear();
                if (!reviews.isEmpty()) {
                    reviewList.addAll(reviews);
                    textNoReviews.setVisibility(View.GONE);
                } else {
                    textNoReviews.setText("You haven't written any reviews yet.");
                    textNoReviews.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MessReviewsActivity.this, "Error loading reviews: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddReviewDialog() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to add a review", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("MESS_OWNER".equals(currentRole)) {
            Toast.makeText(this, "Mess owners can view reviews only", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentMessId == null) {
            Toast.makeText(this, "Join a mess first to add a review", Toast.LENGTH_SHORT).show();
            return;
        }

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
        reviewManager.createReview(currentMessId, rating, comment,
                currentUserName != null ? currentUserName : "Student",
                new ReviewManager.UpdateCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(MessReviewsActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                loadMyReviews();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MessReviewsActivity.this, "Failed to submit review: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
