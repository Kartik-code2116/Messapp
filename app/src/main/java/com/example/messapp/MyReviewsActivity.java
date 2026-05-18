package com.example.messapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.messapp.managers.ReviewManager;
import com.example.messapp.models.Review;
import com.example.messapp.ui.user.ReviewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MyReviewsActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private final List<Review> reviews = new ArrayList<>();
    private TextView emptyView;
    private ReviewManager reviewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reviews);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Reviews");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.reviews_recycler_view);
        emptyView = findViewById(R.id.text_no_reviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(reviews);
        recyclerView.setAdapter(adapter);

        reviewManager = new ReviewManager();
        loadMyReviews();
    }

    private void loadMyReviews() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view your reviews", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        reviewManager.getReviewsForUser(currentUser.getUid(), new ReviewManager.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> loadedReviews) {
                reviews.clear();
                reviews.addAll(loadedReviews);
                adapter.notifyDataSetChanged();
                emptyView.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MyReviewsActivity.this, "Error loading reviews: " + errorMessage, Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }
}
