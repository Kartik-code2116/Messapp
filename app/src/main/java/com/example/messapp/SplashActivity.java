package com.example.messapp;

import com.example.messapp.utils.ThemeManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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

        // --- Entrance animations ---
        android.view.View logoCard  = findViewById(R.id.logoCard);
        android.view.View textTitle = findViewById(R.id.textTitle);

        logoCard.setAlpha(0f);
        logoCard.setScaleX(0.6f);
        logoCard.setScaleY(0.6f);

        textTitle.setAlpha(0f);
        textTitle.setTranslationY(40f);

        logoCard.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

        textTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(250)
                .setDuration(700)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Navigate after animation settles
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                fetchUserRole(currentUser);
            } else {
                navigateTo(new Intent(SplashActivity.this, RoleSelectionActivity.class));
            }
        }, 1800);
    }

    private void fetchUserRole(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        navigateToDashboard(role);
                    } else {
                        navigateTo(new Intent(SplashActivity.this, RoleSelectionActivity.class));
                    }
                })
                .addOnFailureListener(e ->
                        navigateTo(new Intent(SplashActivity.this, RoleSelectionActivity.class)));
    }

    private void navigateToDashboard(String role) {
        Intent intent = "MESS_OWNER".equals(role)
                ? new Intent(SplashActivity.this, MessDashboardActivity.class)
                : new Intent(SplashActivity.this, UserDashboardActivity.class);
        navigateTo(intent);
    }

    /** Centralised navigation with a consistent fade-out transition from splash. */
    private void navigateTo(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
