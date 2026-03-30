package com.example.messapp.utils;

import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.messapp.RoleSelectionActivity;
import com.google.android.material.snackbar.Snackbar;

/**
 * Utility class to manage guest mode functionality across the app.
 * Provides helper methods to check guest status and handle restricted actions.
 */
public class GuestModeManager {

    private static final String PREFS_NAME = "GuestModePrefs";
    private static final String KEY_IS_GUEST = "is_guest";

    private final Context context;
    private final boolean isGuestMode;

    public GuestModeManager(Context context, boolean isGuestMode) {
        this.context = context;
        this.isGuestMode = isGuestMode;
    }

    /**
     * Check if the current session is in guest mode
     */
    public boolean isGuestMode() {
        return isGuestMode;
    }

    /**
     * Check if an action is allowed. If in guest mode, shows a dialog prompting signup.
     * @return true if action is allowed (not in guest mode), false otherwise
     */
    public boolean checkActionAllowed(String actionName, Runnable onAllowed) {
        if (isGuestMode) {
            showSignupDialog(actionName);
            return false;
        }
        if (onAllowed != null) {
            onAllowed.run();
        }
        return true;
    }

    /**
     * Shows a signup dialog when a guest tries to perform a restricted action
     */
    public void showSignupDialog(String featureName) {
        new AlertDialog.Builder(context)
            .setTitle("Sign Up Required")
            .setMessage("You need to create an account to " + featureName + ". Would you like to sign up now?")
            .setPositiveButton("Sign Up", (dialog, which) -> {
                navigateToSignup();
            })
            .setNegativeButton("Continue as Guest", (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }

    /**
     * Shows a snackbar with signup prompt
     */
    public void showSignupSnackbar(android.view.View view, String message) {
        Snackbar.make(view, message + " - Sign up to access", Snackbar.LENGTH_LONG)
            .setAction("SIGN UP", v -> navigateToSignup())
            .show();
    }

    /**
     * Navigate to the role selection/signup screen
     */
    public void navigateToSignup() {
        Intent intent = new Intent(context, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }

    /**
     * Get a display string indicating guest status
     */
    public String getGuestStatusText() {
        return isGuestMode ? "Guest Mode" : "";
    }

    /**
     * Helper method to extract guest mode status from fragment arguments
     */
    public static boolean isGuestModeFromArguments(Fragment fragment) {
        if (fragment.getArguments() != null) {
            return fragment.getArguments().getBoolean("IS_GUEST", false);
        }
        return false;
    }

    /**
     * Helper method to extract guest mode status from parent activity
     */
    public static boolean isGuestModeFromActivity(Fragment fragment) {
        if (fragment.getActivity() instanceof com.example.messapp.UserDashboardActivity) {
            return ((com.example.messapp.UserDashboardActivity) fragment.getActivity()).isGuestMode();
        }
        if (fragment.getActivity() instanceof com.example.messapp.MessDashboardActivity) {
            return ((com.example.messapp.MessDashboardActivity) fragment.getActivity()).isGuestMode();
        }
        return false;
    }
}
