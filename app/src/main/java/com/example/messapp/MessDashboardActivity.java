package com.example.messapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.example.messapp.databinding.ActivityMessDashboardBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.GravityCompat;
import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessDashboardActivity extends AppCompatActivity {

    private static final String TAG = "MessDashboardActivity";

    private ActivityMessDashboardBinding binding;
    private boolean isGuestMode = false;
    private NavController navController;
    private ListenerRegistration profileListener;
    // Cached values — updated by the real-time listener
    private String cachedName;
    private String cachedProfileImageUrl;
    private String cachedMessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMessDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);
        binding.getRoot().post(this::initNavigation);
    }

    private void initNavigation() {
        if (isFinishing()) return;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
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
     * Override bottom-nav to avoid abrupt fragment replace; uses NavController
     * which applies the enter/exit animations defined in the nav graph.
     */
    private void setupSmoothBottomNav() {
        binding.navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == id) {
                return true;
            }
            navController.navigate(id);
            return true;
        });
        binding.navView.setOnItemReselectedListener(item -> { /* no-op */ });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mess_top_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, com.example.messapp.ui.mess.settings.MessSettingsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        } else if (id == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTopBar() {
        binding.adminTopBar.textDate.setText(
                new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        binding.adminTopBar.profileContainer.setOnClickListener(v -> openProfileDrawer());

        binding.adminTopBar.btnSendMessage.setOnClickListener(v ->
                Toast.makeText(this, "Send Message to Everyone — Coming soon", Toast.LENGTH_SHORT).show());
    }

    /** One real-time listener keeps profile data fresh without extra Firestore reads. */
    private void startProfileListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            binding.adminTopBar.textGreeting.setText(isGuestMode ? "Hello, Guest!" : "Hello Admin!");
            binding.adminTopBar.imgProfile.setImageResource(R.drawable.ic_student_profile);
            return;
        }
        String userId = currentUser.getUid();
        profileListener = FirebaseFirestore.getInstance().collection("users").document(userId)
                .addSnapshotListener((doc, e) -> {
                    if (doc == null) return;
                    cachedName = doc.getString("name");
                    cachedProfileImageUrl = doc.getString("profileImageUrl");
                    cachedMessId = doc.getString("messId");

                    String greeting = (cachedName != null && !cachedName.isEmpty())
                            ? "Hello, " + cachedName.split(" ")[0] + "!"
                            : "Hello Admin!";
                    binding.adminTopBar.textGreeting.setText(greeting);

                    if (cachedProfileImageUrl != null && !cachedProfileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(cachedProfileImageUrl)
                                .placeholder(R.drawable.ic_student_profile)
                                .circleCrop()
                                .into(binding.adminTopBar.imgProfile);
                    } else {
                        binding.adminTopBar.imgProfile.setImageResource(R.drawable.ic_student_profile);
                    }
                });
    }

    public void openProfileDrawer() {
        populateDrawer();
        binding.container.openDrawer(GravityCompat.END);
    }

    private void styleDrawerLogoutItem() {
        try {
            MenuItem logout = binding.profileDrawer.getMenu().findItem(R.id.drawer_admin_logout);
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
            if (id == R.id.drawer_admin_profile) {
                navController.navigate(R.id.navigation_mess_profile);
                binding.navView.setSelectedItemId(R.id.navigation_mess_profile);
            } else if (id == R.id.drawer_admin_settings) {
                startActivity(new Intent(this, com.example.messapp.ui.mess.settings.MessSettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else if (id == R.id.drawer_admin_notifications) {
                Toast.makeText(this, "Notification Settings — Coming soon", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.drawer_admin_logout) {
                performLogout();
            }
            binding.container.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void performLogout() {
        if (!isGuestMode) {
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().remove("role").apply();
        }
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    /** Uses cached data to populate the drawer instantly — no extra Firestore call needed for most fields. */
    private void populateDrawer() {
        if (binding.profileDrawer.getHeaderCount() == 0) return;
        View header = binding.profileDrawer.getHeaderView(0);
        if (header == null) return;

        TextView nameView    = header.findViewById(R.id.text_drawer_name);
        TextView emailView   = header.findViewById(R.id.text_drawer_email);
        TextView membersView = header.findViewById(R.id.text_drawer_members);
        TextView ratingView  = header.findViewById(R.id.text_drawer_rating);
        TextView revenueView = header.findViewById(R.id.text_drawer_revenue);
        TextView messNameView = header.findViewById(R.id.text_drawer_mess_name);
        ImageView profileImage = header.findViewById(R.id.img_drawer_profile);
        if (nameView == null || emailView == null || profileImage == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            nameView.setText(isGuestMode ? "Guest Admin" : "Mess Owner");
            emailView.setText("Not signed in");
            if (membersView != null) membersView.setText("0");
            if (ratingView  != null) ratingView.setText("0.0");
            if (revenueView != null) revenueView.setText("₹0");
            if (messNameView != null) messNameView.setText("My Mess");
            profileImage.setImageResource(R.drawable.ic_student_profile);
            return;
        }

        nameView.setText(cachedName != null && !cachedName.isEmpty() ? cachedName : "Mess Owner");
        emailView.setText(currentUser.getEmail());

        if (cachedProfileImageUrl != null && !cachedProfileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(cachedProfileImageUrl)
                    .placeholder(R.drawable.ic_student_profile)
                    .circleCrop()
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_student_profile);
        }

        if (cachedMessId != null && !cachedMessId.isEmpty()) {
            loadMessDetails(cachedMessId, membersView, ratingView, revenueView, messNameView);
        }
    }

    private void loadMessDetails(String messId, TextView membersView, TextView ratingView,
                                  TextView revenueView, TextView messNameView) {
        FirebaseFirestore.getInstance().collection("messes").document(messId).get()
                .addOnSuccessListener(doc -> {
                    if (membersView != null) {
                        Long memberCount = doc.getLong("studentCount");
                        membersView.setText(String.valueOf(memberCount != null ? memberCount : 0));
                    }
                    if (ratingView != null) {
                        Double rating = doc.getDouble("avgRating");
                        ratingView.setText(String.format("%.1f", rating != null ? rating : 0.0));
                    }
                    if (revenueView != null) {
                        Double revenue = doc.getDouble("totalRevenue");
                        revenueView.setText("₹" + (revenue != null ? revenue.intValue() : 0));
                    }
                    if (messNameView != null) {
                        String messName = doc.getString("name");
                        messNameView.setText(messName != null && !messName.isEmpty() ? messName : "My Mess");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null) profileListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
