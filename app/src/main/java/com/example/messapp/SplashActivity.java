package com.example.messapp;

import com.example.messapp.utils.ThemeManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Animation Code
        android.view.View logoCard = findViewById(R.id.logoCard);
        android.view.View textTitle = findViewById(R.id.textTitle);

        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.5f);
        logoCard.setScaleY(0.5f);

        textTitle.setAlpha(0f);
        textTitle.setTranslationY(50f);

        logoCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();

        textTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(300)
                .setDuration(1000)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // User is logged in, fetch role and navigate to dashboard
                fetchUserRole(currentUser);
            } else {
                // No user logged in, navigate to Role Selection
                startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                finish();
            }
        }, 2500); // Increased delay slightly to let animation finish
    }

    private void fetchUserRole(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        navigateToDashboard(role);
                    } else {
                        // User data not found, navigate to Role Selection
                        startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Error fetching user data, navigate to Role Selection
                    startActivity(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                    finish();
                });
    }

    private void navigateToDashboard(String role) {
        if ("MESS_OWNER".equals(role)) {
            startActivity(new Intent(SplashActivity.this, MessDashboardActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, UserDashboardActivity.class));
        }
        finish();
    }
}
