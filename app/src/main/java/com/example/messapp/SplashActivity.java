package com.example.messapp;

import com.example.messapp.utils.ThemeManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String PREFS_USER = "user_prefs";
    private static final String KEY_ROLE = "role";
    private static final int SPLASH_DELAY_MS = 1500;
    /** Never block on Firestore longer than this after the splash delay. */
    private static final int ROLE_FETCH_TIMEOUT_MS = 2500;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;

    private final Runnable roleFetchTimeoutRunnable = () -> {
        if (hasNavigated) return;
        Log.w(TAG, "Role fetch timed out — using cached role or role selection");
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToRoleSelection();
            return;
        }
        String cachedRole = getCachedRole();
        if (cachedRole != null) {
            goToDashboard(cachedRole);
        } else {
            // Signed in locally but offline / Firestore unavailable
            goToRoleSelection();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        android.view.View logoCard = findViewById(R.id.logoCard);
        android.view.View textTitle = findViewById(R.id.textTitle);

        if (logoCard != null && textTitle != null) {
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
        }

        handler.postDelayed(this::decideDestination, SPLASH_DELAY_MS);
    }

    private void decideDestination() {
        if (hasNavigated || isFinishing()) return;

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToRoleSelection();
            return;
        }

        // Fast path: role saved at last successful login — no Firestore wait
        String cachedRole = getCachedRole();
        if (cachedRole != null) {
            Log.d(TAG, "Using cached role: " + cachedRole);
            goToDashboard(cachedRole);
            return;
        }

        // Slow path: fetch role with a hard timeout so splash never hangs
        handler.postDelayed(roleFetchTimeoutRunnable, ROLE_FETCH_TIMEOUT_MS);

        db.collection("users").document(currentUser.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (hasNavigated || isFinishing()) return;
                    handler.removeCallbacks(roleFetchTimeoutRunnable);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String role = task.getResult().getString("role");
                        if (role != null && !role.isEmpty()) {
                            cacheRole(role);
                            goToDashboard(role);
                        } else {
                            goToRoleSelection();
                        }
                    } else {
                        Log.w(TAG, "Role fetch failed", task.getException());
                        goToRoleSelection();
                    }
                });
    }

    private String getCachedRole() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        String role = prefs.getString(KEY_ROLE, null);
        return (role != null && !role.isEmpty()) ? role : null;
    }

    private void cacheRole(String role) {
        getSharedPreferences(PREFS_USER, MODE_PRIVATE).edit().putString(KEY_ROLE, role).apply();
    }

    private void goToRoleSelection() {
        navigateOnce(new Intent(SplashActivity.this, RoleSelectionActivity.class));
    }

    private void goToDashboard(String role) {
        Intent intent = "MESS_OWNER".equals(role)
                ? new Intent(SplashActivity.this, MessDashboardActivity.class)
                : new Intent(SplashActivity.this, UserDashboardActivity.class);
        navigateOnce(intent);
    }

    private void navigateOnce(Intent intent) {
        if (hasNavigated || isFinishing()) return;
        hasNavigated = true;
        handler.removeCallbacksAndMessages(null);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
