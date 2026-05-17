package com.example.messapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.messapp.databinding.ActivityUserDashboardBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "UserDashboardActivity";

    private ActivityUserDashboardBinding binding;
    private boolean isGuestMode = false;
    private NavController navController;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration profileListener;
    // Cache profile data to avoid re-fetching on every drawer open
    private String cachedName;
    private String cachedProfileImageUrl;
    private String cachedMessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);

        // NavHost from FragmentContainerView is ready after the first layout pass
        binding.getRoot().post(this::initNavigation);
    }

    private void initNavigation() {
        if (isFinishing()) return;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_user_dashboard);
        if (navHostFragment == null) {
            Log.e(TAG, "NavHostFragment missing — returning to role selection");
            redirectToRoleSelection();
            return;
        }

        try {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);
            setupSmoothBottomNav();
            setupProfileDrawer();
            binding.profileDrawer.post(this::styleDrawerLogoutItem);
            setupTopBar();
            startProfileListener();
            if (isGuestMode) {
                showGuestBanner();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize navigation", e);
            redirectToRoleSelection();
        }
    }

    private void redirectToRoleSelection() {
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Override bottom-nav tab switches to use a smooth fade animation
     * instead of the default abrupt fragment replace.
     */
    private void setupSmoothBottomNav() {
        binding.navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Don't re-navigate to the currently selected destination
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == id) {
                return true;
            }
            // Navigate using NavController (honours the back stack correctly)
            navController.navigate(id);
            return true;
        });
        // Prevent re-selecting the same tab from triggering navigation
        binding.navView.setOnItemReselectedListener(item -> { /* no-op */ });
    }

    private void setupTopBar() {
        binding.userTopBar.textDate.setText(
                new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        binding.userTopBar.profileContainer.setOnClickListener(v -> openProfileDrawer());

        binding.userTopBar.btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show());
    }

    /**
     * One real-time listener instead of one-shot get() on every drawer open.
     * Cached values update the top bar and drawer immediately.
     */
    private void startProfileListener() {
        if (auth.getCurrentUser() == null) {
            applyProfileToTopBar(isGuestMode ? "Hello, Guest!" : "Hello!", null);
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        profileListener = db.collection("users").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc == null) return;
                    cachedName = doc.getString("name");
                    cachedProfileImageUrl = doc.getString("profileImageUrl");
                    cachedMessId = doc.getString("messId");

                    String greeting = (cachedName != null && !cachedName.isEmpty())
                            ? "Hello, " + cachedName.split(" ")[0] + "!"
                            : "Hello!";
                    applyProfileToTopBar(greeting, cachedProfileImageUrl);
                });
    }

    private void applyProfileToTopBar(String greeting, String imageUrl) {
        binding.userTopBar.textGreeting.setText(greeting);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_student_profile)
                    .circleCrop()
                    .into(binding.userTopBar.imgProfile);
        } else {
            binding.userTopBar.imgProfile.setImageResource(R.drawable.ic_student_profile);
        }
    }

    public void openProfileDrawer() {
        populateDrawer();
        binding.container.openDrawer(GravityCompat.END);
    }

    private void styleDrawerLogoutItem() {
        try {
            MenuItem logout = binding.profileDrawer.getMenu().findItem(R.id.drawer_user_logout);
            if (logout == null) return;
            Drawable icon = logout.getIcon();
            if (icon != null) {
                Drawable tinted = DrawableCompat.wrap(icon.mutate());
                DrawableCompat.setTint(tinted, ContextCompat.getColor(this, R.color.state_error));
                logout.setIcon(tinted);
            }
            CharSequence title = logout.getTitle();
            if (title != null) {
                SpannableString styled = new SpannableString(title);
                styled.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(this, R.color.state_error)),
                        0, styled.length(), 0);
                logout.setTitle(styled);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not style logout drawer item", e);
        }
    }

    private void setupProfileDrawer() {
        binding.profileDrawer.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_user_profile) {
                navController.navigate(R.id.navigation_user_profile);
                binding.navView.setSelectedItemId(R.id.navigation_user_profile);
            } else if (id == R.id.drawer_user_menu) {
                navController.navigate(R.id.navigation_user_menu);
                binding.navView.setSelectedItemId(R.id.navigation_user_menu);
            } else if (id == R.id.drawer_user_history) {
                navController.navigate(R.id.navigation_user_history);
                binding.navView.setSelectedItemId(R.id.navigation_user_history);
            } else if (id == R.id.drawer_user_home) {
                navController.navigate(R.id.navigation_user_home);
                binding.navView.setSelectedItemId(R.id.navigation_user_home);
            } else if (id == R.id.drawer_user_logout) {
                binding.container.closeDrawer(androidx.core.view.GravityCompat.END);
                performLogout();
                return true;
            }
            binding.container.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void performLogout() {
        if (!isGuestMode) {
            auth.signOut();
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().remove("role").apply();
        }
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    /** Populate drawer using cached data — avoids a Firestore round-trip on every open. */
    private void populateDrawer() {
        if (binding.profileDrawer.getHeaderCount() == 0) return;
        View header = binding.profileDrawer.getHeaderView(0);
        if (header == null) return;

        TextView nameView         = header.findViewById(R.id.text_drawer_name);
        TextView emailView        = header.findViewById(R.id.text_drawer_email);
        TextView messView         = header.findViewById(R.id.text_drawer_mess);
        TextView subscriptionView = header.findViewById(R.id.text_drawer_subscription);
        ImageView profileImage    = header.findViewById(R.id.img_drawer_profile);
        if (nameView == null || emailView == null || messView == null
                || subscriptionView == null || profileImage == null) {
            return;
        }

        if (auth.getCurrentUser() == null) {
            nameView.setText(isGuestMode ? "Guest Student" : "Student");
            emailView.setText("Not signed in");
            messView.setText("Not Joined");
            subscriptionView.setText("Not Active");
            profileImage.setImageResource(R.drawable.ic_student_profile);
            return;
        }

        emailView.setText(auth.getCurrentUser().getEmail());
        nameView.setText(cachedName != null && !cachedName.isEmpty() ? cachedName : "Student");
        messView.setText(cachedMessId != null && !cachedMessId.isEmpty()
                ? cachedMessId : "Not Joined");

        if (cachedProfileImageUrl != null && !cachedProfileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(cachedProfileImageUrl)
                    .placeholder(R.drawable.ic_student_profile)
                    .circleCrop()
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_student_profile);
        }

        // Load subscription info separately (less critical, fine to fetch once)
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null) return;
                    Long lunchExpiry   = doc.getLong("lunchSubscriptionExpiry");
                    Long dinnerExpiry  = doc.getLong("dinnerSubscriptionExpiry");
                    Long generalExpiry = doc.getLong("subscriptionExpiry");
                    subscriptionView.setText(
                            buildDrawerSubscriptionText(lunchExpiry, dinnerExpiry, generalExpiry));
                });
    }

    /** Compact label for the drawer stat pill (max 2 lines). */
    private String buildDrawerSubscriptionText(Long lunchExpiry, Long dinnerExpiry, Long generalExpiry) {
        long lunch = lunchExpiry != null && lunchExpiry > 0 ? lunchExpiry
                : (generalExpiry != null ? generalExpiry : 0);
        long dinner = dinnerExpiry != null && dinnerExpiry > 0 ? dinnerExpiry
                : (generalExpiry != null ? generalExpiry : 0);
        if (lunch <= 0 && dinner <= 0) return "Not Active";
        boolean lunchActive = lunch > System.currentTimeMillis();
        boolean dinnerActive = dinner > System.currentTimeMillis();
        if (lunchActive && dinnerActive) return "Lunch & Dinner";
        if (lunchActive) return "Lunch active";
        if (dinnerActive) return "Dinner active";
        return "Expired";
    }

    private void showGuestBanner() {
        Snackbar.make(binding.getRoot(),
            "Guest Mode — Sign up for full access",
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
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) profileListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_user_dashboard);
        if (navHostFragment == null) return super.onSupportNavigateUp();
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
