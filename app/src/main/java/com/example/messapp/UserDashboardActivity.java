package com.example.messapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.messapp.databinding.ActivityUserDashboardBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private ActivityUserDashboardBinding binding;
    private boolean isGuestMode = false;
    private NavController navController;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check if guest mode
        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);
        if (isGuestMode) {
            showGuestBanner();
        }

        // Setup Toolbar
        // Toolbar removed in favor of fragment-specific headers

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_user_home, R.id.navigation_user_menu, R.id.navigation_user_history,
                R.id.navigation_user_profile)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_user_dashboard);
        navController = navHostFragment.getNavController();

        // Pass guest mode to fragments via arguments
        Bundle navArgs = new Bundle();
        navArgs.putBoolean("IS_GUEST", isGuestMode);
        navHostFragment.setArguments(navArgs);

        NavigationUI.setupWithNavController(binding.navView, navController);
        setupProfileDrawer();
        loadDrawerProfile();
    }

    public void openProfileDrawer() {
        loadDrawerProfile();
        binding.container.openDrawer(GravityCompat.END);
    }

    private void setupProfileDrawer() {
        binding.profileDrawer.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_user_profile) {
                navController.navigate(R.id.navigation_user_profile);
            } else if (id == R.id.drawer_user_menu) {
                navController.navigate(R.id.navigation_user_menu);
            } else if (id == R.id.drawer_user_history) {
                navController.navigate(R.id.navigation_user_history);
            } else if (id == R.id.drawer_user_home) {
                navController.navigate(R.id.navigation_user_home);
            }
            binding.navView.setSelectedItemId(mapDrawerItemToBottomItem(id));
            binding.container.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private int mapDrawerItemToBottomItem(int drawerItemId) {
        if (drawerItemId == R.id.drawer_user_profile) return R.id.navigation_user_profile;
        if (drawerItemId == R.id.drawer_user_menu) return R.id.navigation_user_menu;
        if (drawerItemId == R.id.drawer_user_history) return R.id.navigation_user_history;
        return R.id.navigation_user_home;
    }

    private void loadDrawerProfile() {
        View header = binding.profileDrawer.getHeaderView(0);
        TextView nameView = header.findViewById(R.id.text_drawer_name);
        TextView emailView = header.findViewById(R.id.text_drawer_email);
        TextView messView = header.findViewById(R.id.text_drawer_mess);
        TextView subscriptionView = header.findViewById(R.id.text_drawer_subscription);
        ImageView profileImage = header.findViewById(R.id.img_drawer_profile);

        if (auth.getCurrentUser() == null) {
            nameView.setText(isGuestMode ? "Guest Student" : "Student");
            emailView.setText("Not signed in");
            messView.setText("Mess: Not Joined");
            subscriptionView.setText("Subscription: Not Active");
            profileImage.setImageResource(R.drawable.ic_student_profile);
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        emailView.setText(auth.getCurrentUser().getEmail());
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String messId = doc.getString("messId");
                    String profileImageUrl = doc.getString("profileImageUrl");
                    Long lunchExpiry = doc.getLong("lunchSubscriptionExpiry");
                    Long dinnerExpiry = doc.getLong("dinnerSubscriptionExpiry");
                    Long generalExpiry = doc.getLong("subscriptionExpiry");

                    nameView.setText(name != null && !name.isEmpty() ? name : "Student");
                    messView.setText(messId != null && !messId.isEmpty() ? "Mess ID: " + messId : "Mess: Not Joined");
                    subscriptionView.setText(buildSubscriptionText(lunchExpiry, dinnerExpiry, generalExpiry));

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_student_profile)
                                .circleCrop()
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.ic_student_profile);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Unable to load profile details", Toast.LENGTH_SHORT).show());
    }

    private String buildSubscriptionText(Long lunchExpiry, Long dinnerExpiry, Long generalExpiry) {
        long lunch = lunchExpiry != null && lunchExpiry > 0 ? lunchExpiry
                : (generalExpiry != null ? generalExpiry : 0);
        long dinner = dinnerExpiry != null && dinnerExpiry > 0 ? dinnerExpiry
                : (generalExpiry != null ? generalExpiry : 0);
        if (lunch <= 0 && dinner <= 0) return "Subscription: Not Active";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return "Lunch: " + (lunch > 0 ? sdf.format(new Date(lunch)) : "Not Active")
                + "\nDinner: " + (dinner > 0 ? sdf.format(new Date(dinner)) : "Not Active");
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

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_user_dashboard);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
