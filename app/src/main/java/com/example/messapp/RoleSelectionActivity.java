package com.example.messapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.messapp.databinding.ActivityRoleSelectionBinding;

public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelectionActivity";
    private ActivityRoleSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Set click listeners with null-safety checks
            View messOwnerBtn = findViewById(R.id.btn_mess_uncal);
            View userBtn = findViewById(R.id.btn_user);
            View guestBtn = findViewById(R.id.btn_guest);

            if (messOwnerBtn == null) {
                Log.e(TAG, "btn_mess_uncal button not found in layout");
                showErrorAndExit("UI Layout Error: Mess Owner button missing");
                return;
            }
            if (userBtn == null) {
                Log.e(TAG, "btn_user button not found in layout");
                showErrorAndExit("UI Layout Error: User button missing");
                return;
            }
            if (guestBtn == null) {
                Log.e(TAG, "btn_guest button not found in layout");
                showErrorAndExit("UI Layout Error: Guest button missing");
                return;
            }

            messOwnerBtn.setOnClickListener(v -> {
                try {
                    navigateToLoginSignup("MESS_OWNER");
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating for MESS_OWNER role", e);
                    showError("Navigation Error: " + e.getMessage());
                }
            });

            userBtn.setOnClickListener(v -> {
                try {
                    navigateToLoginSignup("USER");
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating for USER role", e);
                    showError("Navigation Error: " + e.getMessage());
                }
            });

            guestBtn.setOnClickListener(v -> {
                try {
                    showGuestModeDialog();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing guest mode dialog", e);
                    showError("Error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showErrorAndExit("Initialization Error: " + e.getMessage());
        }
    }

    private void navigateToLoginSignup(String role) {
        try {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("ROLE", role);
            Log.d(TAG, "Starting LoginActivity with role: " + role);
            startActivity(intent);
            // Delay finish to allow activity to start properly
            // finish(); // Keep usage of back button to role selection possible
        } catch (Exception e) {
            Log.e(TAG, "Error starting LoginActivity", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showErrorAndExit(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void showGuestModeDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Preview Mode")
            .setMessage("Which side would you like to preview?")
            .setPositiveButton("User View", (dialog, which) -> navigateAsGuest("USER"))
            .setNegativeButton("Mess Owner View", (dialog, which) -> navigateAsGuest("MESS_OWNER"))
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void navigateAsGuest(String role) {
        try {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            intent.putExtra("ROLE", role);
            intent.putExtra("IS_GUEST", true);
            Log.d(TAG, "Starting LoginActivity in guest mode with role: " + role);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting LoginActivity in guest mode", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
