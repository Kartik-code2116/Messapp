package com.example.messapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.example.messapp.databinding.ActivityMessDashboardBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.view.Menu;
import android.view.MenuItem;

public class MessDashboardActivity extends AppCompatActivity {

    private ActivityMessDashboardBinding binding;
    private boolean isGuestMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMessDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);
        if (isGuestMode) {
            showGuestBanner();
        } else {
            fetchMessName();
        }

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_mess_dashboard, R.id.navigation_mess_menu, R.id.navigation_mess_students,
                R.id.navigation_mess_profile, R.id.navigation_mess_offers)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        NavController navController = navHostFragment.getNavController();

        Bundle navArgs = new Bundle();
        navArgs.putBoolean("IS_GUEST", isGuestMode);
        navHostFragment.setArguments(navArgs);

        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void showGuestBanner() {
        Snackbar.make(binding.getRoot(),
            "Guest Mode - Sign up for full access",
            Snackbar.LENGTH_LONG)
            .setAction("SIGN UP", v -> {
                Intent intent = new Intent(this, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .setActionTextColor(getResources().getColor(android.R.color.holo_blue_light, getTheme()))
            .show();
    }

    public boolean isGuestMode() {
        return isGuestMode;
    }

    private void fetchMessName() {
        // FIX #2: getCurrentUser() was called without a null check — crashes if auth
        // state hasn't resolved yet (e.g. cold start or token refresh in progress).
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    // messId available here if needed; UI updates are handled in fragments
                    String messId = doc.getString("messId");
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mess_top_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, com.example.messapp.ui.mess.settings.MessSettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            if (!isGuestMode) {
                FirebaseAuth.getInstance().signOut();
            }
            // FIX #7 (partial): navigate to RoleSelectionActivity on logout so role
            // context is always fresh — mirrors the correct behavior in UserProfileFragment.
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
