package com.example.messapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.example.messapp.databinding.ActivityUserDashboardBinding;
import com.example.messapp.managers.MessNotificationManager;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    private static final String TAG = "UserDashboardActivity";

    private ActivityUserDashboardBinding binding;
    private boolean isGuestMode = false;
    private NavController navController;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration profileListener;
    private ListenerRegistration notificationListener;
    // Cache profile data to avoid re-fetching on every drawer open
    private String cachedName;
    private String cachedProfileImageUrl;
    private String cachedMessId;
    private long cachedNotificationSeenAt;
    private String activeNotificationMessId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);

        // NavHost from FragmentContainerView is ready after the first layout pass
        binding.getRoot().post(this::initNavigation);
    }

    private void initNavigation() {
        if (isFinishing())
            return;

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
        binding.navView.setOnItemReselectedListener(item -> {
            /* no-op */ });
    }

    private void setupTopBar() {
        binding.userTopBar.textDate.setText(
                new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(new Date()));

        binding.userTopBar.profileContainer.setOnClickListener(v -> openProfileDrawer());

        binding.userTopBar.btnNotification.setOnClickListener(v -> showNotificationsDialog());
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
                    if (doc == null)
                        return;
                    cachedName = doc.getString("name");
                    cachedProfileImageUrl = doc.getString("profileImageUrl");
                    cachedMessId = doc.getString("messId");
                    Long seenAt = doc.getLong("lastNotificationSeenAt");
                    cachedNotificationSeenAt = seenAt != null ? seenAt : 0L;

                    String greeting = (cachedName != null && !cachedName.isEmpty())
                            ? "Hello, " + cachedName.split(" ")[0] + "!"
                            : "Hello!";
                    applyProfileToTopBar(greeting, cachedProfileImageUrl);
                    startNotificationListenerIfReady();
                });
    }

    private void startNotificationListenerIfReady() {
        if (auth.getCurrentUser() == null || cachedMessId == null || cachedMessId.isEmpty()) {
            updateNotificationBadge(0);
            return;
        }
        if (cachedMessId.equals(activeNotificationMessId) && notificationListener != null) {
            return;
        }
        if (notificationListener != null) {
            notificationListener.remove();
        }
        activeNotificationMessId = cachedMessId;
        String userId = auth.getCurrentUser().getUid();
        notificationListener = db.collection(MessNotificationManager.COLLECTION)
                .whereEqualTo("messId", cachedMessId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null) {
                        updateNotificationBadge(0);
                        return;
                    }
                    int unread = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!isNotificationVisibleForUser(doc, userId)) {
                            continue;
                        }
                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null && createdAt > cachedNotificationSeenAt) {
                            unread++;
                        }
                    }
                    updateNotificationBadge(unread);
                });
    }

    private boolean isNotificationVisibleForUser(DocumentSnapshot doc, String userId) {
        String targetUserId = doc.getString("targetUserId");
        return targetUserId == null || targetUserId.isEmpty() || targetUserId.equals(userId);
    }

    private void updateNotificationBadge(int unreadCount) {
        if (binding == null || binding.userTopBar == null) {
            return;
        }
        if (unreadCount <= 0) {
            binding.userTopBar.textNotificationBadge.setVisibility(View.GONE);
            return;
        }
        binding.userTopBar.textNotificationBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
        binding.userTopBar.textNotificationBadge.setVisibility(View.VISIBLE);
    }

    private void showNotificationsDialog() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Sign in to view notifications.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cachedMessId == null || cachedMessId.isEmpty()) {
            Toast.makeText(this, "Join a mess to receive notifications.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        LinearLayout listContainer = dialogView.findViewById(R.id.layout_notification_list);
        TextView emptyText = dialogView.findViewById(R.id.text_empty_notifications);
        String userId = auth.getCurrentUser().getUid();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        db.collection(MessNotificationManager.COLLECTION)
                .whereEqualTo("messId", cachedMessId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<DocumentSnapshot> notifications = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (isNotificationVisibleForUser(doc, userId)) {
                            notifications.add(doc);
                        }
                    }
                    Collections.sort(notifications, (left, right) -> Long.compare(
                            valueOrZero(right.getLong("createdAt")),
                            valueOrZero(left.getLong("createdAt"))));

                    emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    listContainer.removeAllViews();
                    for (DocumentSnapshot doc : notifications) {
                        listContainer.addView(createNotificationRow(doc));
                    }

                    db.collection("users").document(userId)
                            .update("lastNotificationSeenAt", System.currentTimeMillis());
                    cachedNotificationSeenAt = System.currentTimeMillis();
                    updateNotificationBadge(0);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        dialog.show();
    }

    private View createNotificationRow(DocumentSnapshot doc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 14, 0, 14);

        TextView title = new TextView(this);
        title.setText(doc.getString("title") != null ? doc.getString("title") : "Notification");
        title.setTextColor(ContextCompat.getColor(this, R.color.text_heading));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(title);

        TextView message = new TextView(this);
        message.setText(doc.getString("message") != null ? doc.getString("message") : "");
        message.setTextColor(ContextCompat.getColor(this, R.color.text_body));
        message.setTextSize(14);
        message.setPadding(0, 4, 0, 0);
        row.addView(message);

        Long createdAt = doc.getLong("createdAt");
        if (createdAt != null) {
            TextView date = new TextView(this);
            date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(createdAt)));
            date.setTextColor(ContextCompat.getColor(this, R.color.text_caption));
            date.setTextSize(12);
            date.setPadding(0, 6, 0, 0);
            row.addView(date);
        }
        return row;
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
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
            if (logout == null)
                return;
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

    /**
     * Populate drawer using cached data — avoids a Firestore round-trip on every
     * open.
     */
    private void populateDrawer() {
        if (binding.profileDrawer.getHeaderCount() == 0)
            return;
        View header = binding.profileDrawer.getHeaderView(0);
        if (header == null)
            return;

        TextView nameView = header.findViewById(R.id.text_drawer_name);
        TextView emailView = header.findViewById(R.id.text_drawer_email);
        TextView messView = header.findViewById(R.id.text_drawer_mess);
        TextView subscriptionView = header.findViewById(R.id.text_drawer_subscription);
        ImageView profileImage = header.findViewById(R.id.img_drawer_profile);
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
                ? cachedMessId
                : "Not Joined");

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
                    if (doc == null)
                        return;
                    Long lunchExpiry = doc.getLong("lunchSubscriptionExpiry");
                    Long dinnerExpiry = doc.getLong("dinnerSubscriptionExpiry");
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
        if (lunch <= 0 && dinner <= 0)
            return "Not Active";
        boolean lunchActive = lunch > System.currentTimeMillis();
        boolean dinnerActive = dinner > System.currentTimeMillis();
        if (lunchActive && dinnerActive)
            return "Lunch & Dinner";
        if (lunchActive)
            return "Lunch active";
        if (dinnerActive)
            return "Dinner active";
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
        if (profileListener != null)
            profileListener.remove();
        if (notificationListener != null)
            notificationListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_user_dashboard);
        if (navHostFragment == null)
            return super.onSupportNavigateUp();
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
