package com.example.messapp;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;
import androidx.navigation.fragment.NavHostFragment;

import com.example.messapp.databinding.ActivityMessDashboardBinding;
import com.example.messapp.managers.MessNotificationManager;
import com.example.messapp.utils.ThemeManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        isGuestMode = getIntent().getBooleanExtra("IS_GUEST", false);
        binding.getRoot().post(this::initNavigation);
    }

    private void initNavigation() {
        if (isFinishing())
            return;

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
        binding.navView.setOnItemReselectedListener(item -> {
            /* no-op */ });
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

        binding.adminTopBar.btnSendMessage.setOnClickListener(v -> showSendMessageDialog());
    }

    private void showSendMessageDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (isGuestMode || currentUser == null) {
            Toast.makeText(this, "Sign in as mess admin to send messages.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cachedMessId == null || cachedMessId.isEmpty()) {
            Toast.makeText(this, "Mess ID not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_message, null);
        TextInputEditText titleInput = dialogView.findViewById(R.id.input_message_title);
        TextInputEditText bodyInput = dialogView.findViewById(R.id.input_message_body);
        View btnViewSentMessages = dialogView.findViewById(R.id.btn_view_sent_messages);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (btnViewSentMessages != null) {
            btnViewSentMessages.setOnClickListener(v -> {
                dialog.dismiss();
                showSentMessagesDialog();
            });
        }

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = titleInput.getText() != null
                            ? titleInput.getText().toString().trim() : "";
                    String message = bodyInput.getText() != null
                            ? bodyInput.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(title)) {
                        titleInput.setError("Required");
                        return;
                    }
                    if (TextUtils.isEmpty(message)) {
                        bodyInput.setError("Required");
                        return;
                    }

                    String senderName = cachedName != null && !cachedName.isEmpty()
                            ? cachedName : "Mess Admin";
                    MessNotificationManager.sendAdminMessage(
                            cachedMessId,
                            currentUser.getUid(),
                            senderName,
                            title,
                            message,
                            () -> {
                                Toast.makeText(this, "Message sent to students.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            },
                            e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }));
        dialog.show();
    }

    private void showSentMessagesDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (isGuestMode || currentUser == null) {
            Toast.makeText(this, "Sign in as mess admin to view sent messages.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cachedMessId == null || cachedMessId.isEmpty()) {
            Toast.makeText(this, "Mess ID not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sent_messages, null);
        LinearLayout listContainer = dialogView.findViewById(R.id.layout_sent_messages_list);
        TextView emptyText = dialogView.findViewById(R.id.text_empty_sent_messages);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        FirebaseFirestore.getInstance().collection(MessNotificationManager.COLLECTION)
                .whereEqualTo("messId", cachedMessId)
                .whereEqualTo("type", MessNotificationManager.TYPE_ADMIN_MESSAGE)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing()) return;
                    List<DocumentSnapshot> notifications = new ArrayList<>(snapshot.getDocuments());
                    Collections.sort(notifications, (left, right) -> {
                        Long leftCreated = left.getLong("createdAt");
                        Long rightCreated = right.getLong("createdAt");
                        long leftVal = leftCreated != null ? leftCreated : 0L;
                        long rightVal = rightCreated != null ? rightCreated : 0L;
                        return Long.compare(rightVal, leftVal);
                    });

                    emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    listContainer.removeAllViews();
                    for (DocumentSnapshot doc : notifications) {
                        View row = getLayoutInflater().inflate(R.layout.item_sent_message, null);
                        TextView textTitle = row.findViewById(R.id.text_message_title);
                        TextView textBody = row.findViewById(R.id.text_message_body);
                        TextView textDate = row.findViewById(R.id.text_message_date);
                        View btnDelete = row.findViewById(R.id.btn_delete_message);

                        textTitle.setText(doc.getString("title"));
                        textBody.setText(doc.getString("message"));

                        Long createdAt = doc.getLong("createdAt");
                        if (createdAt != null) {
                            textDate.setText(java.text.DateFormat.getDateTimeInstance(
                                    java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                                    .format(new Date(createdAt)));
                        } else {
                            textDate.setText("");
                        }

                        btnDelete.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("Delete Message")
                                    .setMessage("Are you sure you want to delete this message? Students will no longer see it.")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        doc.getReference().delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show();
                                                    listContainer.removeView(row);
                                                    if (listContainer.getChildCount() == 0) {
                                                        emptyText.setVisibility(View.VISIBLE);
                                                    }
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });

                        listContainer.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    Toast.makeText(this, "Failed to load messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        dialog.show();
    }

    /**
     * One real-time listener keeps profile data fresh without extra Firestore
     * reads.
     */
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
                    if (doc == null)
                        return;
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

    /**
     * Uses cached data to populate the drawer instantly — no extra Firestore call
     * needed for most fields.
     */
    private void populateDrawer() {
        if (binding.profileDrawer.getHeaderCount() == 0)
            return;
        View header = binding.profileDrawer.getHeaderView(0);
        if (header == null)
            return;

        TextView nameView = header.findViewById(R.id.text_drawer_name);
        TextView emailView = header.findViewById(R.id.text_drawer_email);
        TextView membersView = header.findViewById(R.id.text_drawer_members);
        TextView ratingView = header.findViewById(R.id.text_drawer_rating);
        TextView revenueView = header.findViewById(R.id.text_drawer_revenue);
        TextView messNameView = header.findViewById(R.id.text_drawer_mess_name);
        ImageView profileImage = header.findViewById(R.id.img_drawer_profile);
        if (nameView == null || emailView == null || profileImage == null)
            return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            nameView.setText(isGuestMode ? "Guest Admin" : "Mess Owner");
            emailView.setText("Not signed in");
            if (membersView != null)
                membersView.setText("0");
            if (ratingView != null)
                ratingView.setText("0.0");
            if (revenueView != null)
                revenueView.setText("₹0");
            if (messNameView != null)
                messNameView.setText("My Mess");
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
        // 1. Fetch rating and mess name from mess document
        FirebaseFirestore.getInstance().collection("messes").document(messId).get()
                .addOnSuccessListener(doc -> {
                    if (ratingView != null) {
                        Double rating = doc.getDouble("avgRating");
                        ratingView.setText(String.format(Locale.getDefault(), "%.1f", rating != null ? rating : 0.0));
                    }
                    if (messNameView != null) {
                        String messName = doc.getString("name");
                        messNameView.setText(messName != null && !messName.isEmpty() ? messName : "My Mess");
                    }
                });

        // 2. Query dynamically joined student members (role = USER)
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("messId", messId)
                .whereEqualTo("role", "USER")
                .get()
                .addOnSuccessListener(snapshots -> {
                    int joinedCount = snapshots != null ? snapshots.size() : 0;
                    if (membersView != null) {
                        membersView.setText(String.valueOf(joinedCount));
                    }
                })
                .addOnFailureListener(e -> {
                    if (membersView != null) membersView.setText("0");
                });

        // 3. Query transactions dynamically to calculate current month's revenue
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    double monthlySum = 0;
                    if (snapshots != null) {
                        for (DocumentSnapshot transDoc : snapshots.getDocuments()) {
                            Long timestamp = transDoc.getLong("timestamp");
                            Double amount = transDoc.getDouble("amount");
                            if (timestamp != null && timestamp >= startOfMonth && amount != null) {
                                monthlySum += amount;
                            }
                        }
                    }
                    if (revenueView != null) {
                        revenueView.setText("₹" + (int) monthlySum);
                    }
                })
                .addOnFailureListener(e -> {
                    if (revenueView != null) revenueView.setText("₹0");
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (profileListener != null)
            profileListener.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_mess_dashboard);
        if (navHostFragment == null)
            return super.onSupportNavigateUp();
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
