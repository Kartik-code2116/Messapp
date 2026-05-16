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

    private ActivityMessDashboardBinding binding;
    private boolean isGuestMode = false;
    private NavController navController;

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
        navController = navHostFragment.getNavController();

        Bundle navArgs = new Bundle();
        navArgs.putBoolean("IS_GUEST", isGuestMode);
        navHostFragment.setArguments(navArgs);

        NavigationUI.setupWithNavController(binding.navView, navController);

        setupProfileDrawer();
        setupTopBar();
        loadTopBarProfile();
        loadDrawerProfile();
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
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTopBar() {
        binding.adminTopBar.textDate.setText(
                new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        binding.adminTopBar.profileContainer.setOnClickListener(v -> openProfileDrawer());

        binding.adminTopBar.btnSendMessage.setOnClickListener(v ->
                Toast.makeText(this, "Send Message to Everyone - Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void loadTopBarProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            binding.adminTopBar.textGreeting.setText(isGuestMode ? "Hello, Guest!" : "Hello Admin!");
            binding.adminTopBar.imgProfile.setImageResource(R.drawable.ic_student_profile);
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        binding.adminTopBar.textGreeting.setText("Hello, " + name.split(" ")[0] + "!");
                    } else {
                        binding.adminTopBar.textGreeting.setText("Hello Admin!");
                    }

                    String profileImageUrl = doc.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_student_profile)
                                .circleCrop()
                                .into(binding.adminTopBar.imgProfile);
                    } else {
                        binding.adminTopBar.imgProfile.setImageResource(R.drawable.ic_student_profile);
                    }
                });
    }

    public void openProfileDrawer() {
        loadTopBarProfile();
        loadDrawerProfile();
        binding.container.openDrawer(GravityCompat.END);
    }

    private void setupProfileDrawer() {
        binding.profileDrawer.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_admin_profile) {
                navController.navigate(R.id.navigation_mess_profile);
                binding.navView.setSelectedItemId(R.id.navigation_mess_profile);
            } else if (id == R.id.drawer_admin_settings) {
                startActivity(new Intent(this, com.example.messapp.ui.mess.settings.MessSettingsActivity.class));
            } else if (id == R.id.drawer_admin_logout) {
                if (!isGuestMode) {
                    FirebaseAuth.getInstance().signOut();
                }
                Intent intent = new Intent(this, RoleSelectionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            binding.container.closeDrawer(GravityCompat.END);
            return true;
        });
    }

    private void loadDrawerProfile() {
        View header = binding.profileDrawer.getHeaderView(0);
        TextView nameView = header.findViewById(R.id.text_drawer_name);
        TextView emailView = header.findViewById(R.id.text_drawer_email);
        TextView membersView = header.findViewById(R.id.text_drawer_members);
        ImageView profileImage = header.findViewById(R.id.img_drawer_profile);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            nameView.setText(isGuestMode ? "Guest Admin" : "Mess Owner");
            emailView.setText("Not signed in");
            if (membersView != null) membersView.setText("Members: 0");
            profileImage.setImageResource(R.drawable.ic_student_profile);
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    nameView.setText(name != null && !name.isEmpty() ? name : "Mess Owner");

                    String email = doc.getString("email");
                    emailView.setText(email != null && !email.isEmpty() ? email : currentUser.getEmail());

                    String profileImageUrl = doc.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_student_profile)
                                .circleCrop()
                                .into(profileImage);
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
