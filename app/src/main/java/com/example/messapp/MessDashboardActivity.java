package com.example.messapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import androidx.appcompat.widget.Toolbar;
import com.example.messapp.databinding.ActivityMessDashboardBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
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

        // Check if guest mode
        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);
        if (isGuestMode) {
            showGuestBanner();
        } else {
            // Fetch mess name only for authenticated users
            fetchMessName();
        }

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_mess_dashboard, R.id.navigation_mess_menu, R.id.navigation_mess_students,
                R.id.navigation_mess_profile, R.id.navigation_mess_offers)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        NavController navController = navHostFragment.getNavController();

        // Pass guest mode to fragments via arguments
        Bundle navArgs = new Bundle();
        navArgs.putBoolean("IS_GUEST", isGuestMode);
        navHostFragment.setArguments(navArgs);

        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    private void showGuestBanner() {
        Snackbar.make(binding.getRoot(),
            "Guest Mode: Sign up to access all features",
            Snackbar.LENGTH_INDEFINITE)
            .setAction("SIGN UP", v -> {
                Intent intent = new Intent(this, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            })
            .show();
    }

    public boolean isGuestMode() {
        return isGuestMode;
    }

    private void fetchMessName() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener(doc -> {
            String messId = doc.getString("messId");
            // Logic to update UI with mess name can be moved to fragments or shared
            // ViewModel
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
            // TODO: Open settings
            return true;
        } else if (id == R.id.action_logout) {
            if (!isGuestMode) {
                FirebaseAuth.getInstance().signOut();
            }
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
