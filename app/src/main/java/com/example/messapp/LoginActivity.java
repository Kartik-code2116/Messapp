package com.example.messapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messapp.databinding.ActivityLoginBinding;
import com.example.messapp.utils.ThemeManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentRole;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            if (binding == null || binding.getRoot() == null) {
                Log.e("LoginActivity", "View binding failed - binding is null");
                Toast.makeText(this, "UI Loading Error: Layout binding failed", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            setContentView(binding.getRoot());

            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            currentRole = getIntent().getStringExtra("ROLE");
            if (currentRole == null)
                currentRole = "USER"; // default role to avoid NPE

            // Check if guest mode
            boolean isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
            if (isGuest) {
                Log.d("LoginActivity", "Guest mode detected, skipping auth for role: " + currentRole);
                navigateToDashboardAsGuest(currentRole);
                return;
            }

            Log.d("LoginActivity", "User role: " + currentRole);
            updateUI();

            // Set click listeners with null checks
            if (binding.btnMainAction != null) {
                binding.btnMainAction.setOnClickListener(v -> {
                    if (isLoginMode)
                        performLogin();
                    else
                        performSignup();
                });
            } else {
                Log.e("LoginActivity", "btnMainAction is null");
            }

            if (binding.btnSwitchMode != null) {
                binding.btnSwitchMode.setOnClickListener(v -> {
                    isLoginMode = !isLoginMode;
                    updateUI();
                });
            } else {
                Log.e("LoginActivity", "btnSwitchMode is null");
            }

            if (binding.btnForgotPassword != null) {
                binding.btnForgotPassword.setOnClickListener(v -> performForgotPassword());
            } else {
                Log.e("LoginActivity", "btnForgotPassword is null");
            }

            if (binding.btnGoogleLogin != null) {
                binding.btnGoogleLogin.setOnClickListener(v -> handleGoogleSignIn());
            } else {
                Log.e("LoginActivity", "btnGoogleLogin is null");
            }
        } catch (Exception e) {
            Log.e("LoginActivity", "Critical error in onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateUI() {
        try {
            if (binding == null) {
                Log.e("LoginActivity", "updateUI: binding is null");
                return;
            }

            if (isLoginMode) {
                if (binding.textTitle != null) {
                    binding.textTitle.setText("MESS_OWNER".equals(currentRole) ? "Mess Login" : "Student Login");
                }
                if (binding.btnMainAction != null) {
                    binding.btnMainAction.setText("Login");
                }
                if (binding.btnSwitchMode != null) {
                    binding.btnSwitchMode.setText("Don't have an account? Sign Up");
                }
                if (binding.messIdLayout != null) {
                    binding.messIdLayout.setVisibility(View.GONE);
                }
                if (binding.messNameLayout != null) {
                    binding.messNameLayout.setVisibility(View.GONE);
                }
            } else {
                if (binding.textTitle != null) {
                    binding.textTitle.setText("Create Account");
                }
                if (binding.btnMainAction != null) {
                    binding.btnMainAction.setText("Sign Up");
                }
                if (binding.btnSwitchMode != null) {
                    binding.btnSwitchMode.setText("Already have an account? Login");
                }
                if ("MESS_OWNER".equals(currentRole)) {
                    if (binding.messNameLayout != null) {
                        binding.messNameLayout.setVisibility(View.VISIBLE);
                    }
                    if (binding.messIdLayout != null) {
                        binding.messIdLayout.setVisibility(View.GONE);
                    }
                } else {
                    if (binding.messIdLayout != null) {
                        binding.messIdLayout.setVisibility(View.VISIBLE);
                    }
                    if (binding.messNameLayout != null) {
                        binding.messNameLayout.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LoginActivity", "Error in updateUI", e);
        }
    }

    private void performLogin() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        fetchUserRole(mAuth.getCurrentUser()); // Verify role matches
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performSignup() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String extraData = "";

        if ("MESS_OWNER".equals(currentRole)) {
            extraData = binding.messNameEditText.getText().toString().trim();
            if (TextUtils.isEmpty(extraData)) {
                binding.messNameEditText.setError("Required");
                return;
            }
        } else {
            extraData = binding.messIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(extraData)) {
                binding.messIdEditText.setError("Required");
                return;
            }
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password))
            return;

        binding.progressBar.setVisibility(View.VISIBLE);
        String finalExtraData = extraData;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(mAuth.getCurrentUser(), currentRole, finalExtraData);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Signup Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performForgotPassword() {
        String email = binding.emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email to reset password", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error sending reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private void handleGoogleSignIn() {
        CredentialManager credentialManager = CredentialManager.create(this);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("355370937322-q0146f9culvl5d04d2k0eopcbal7sim0.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        binding.progressBar.setVisibility(View.VISIBLE);

        credentialManager.getCredentialAsync(this, request, null, ContextCompat.getMainExecutor(this),
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        binding.progressBar.setVisibility(View.GONE);
                        if (result.getCredential() instanceof GoogleIdTokenCredential) {
                            GoogleIdTokenCredential credential = (GoogleIdTokenCredential) result.getCredential();
                            firebaseAuthWithGoogle(credential.getIdToken());
                        } else {
                            Log.d("Login", "Unknown credential type");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        binding.progressBar.setVisibility(View.GONE);
                        Log.e("Login", "Google Sign In Failed", e);
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserExists(user);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Exception e = task.getException();
                        Log.e("Login", "Firebase Auth Failed", e);
                        Toast.makeText(LoginActivity.this,
                                "Firebase Auth Failed: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserExists(FirebaseUser user) {
        if (user == null)
            return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // User exists, just log them in
                        binding.progressBar.setVisibility(View.GONE);
                        String role = documentSnapshot.getString("role");
                        navigateToDashboard(role);
                    } else {
                        // New user from Google, need to sign them up with current role
                        String extraData = "";
                        if ("MESS_OWNER".equals(currentRole)) {
                            extraData = binding.messNameEditText.getText().toString().trim();
                            if (TextUtils.isEmpty(extraData)) {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.messNameLayout.setVisibility(View.VISIBLE);
                                binding.messNameEditText.setError("Please enter Mess Name first");
                                Toast.makeText(this, "Please enter Mess Name and click Google again", Toast.LENGTH_LONG)
                                        .show();
                                return;
                            }
                        } else {
                            extraData = binding.messIdEditText.getText().toString().trim();
                            if (TextUtils.isEmpty(extraData)) {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.messIdLayout.setVisibility(View.VISIBLE);
                                binding.messIdEditText.setError("Please enter Mess ID first");
                                Toast.makeText(this, "Please enter Mess ID and click Google again", Toast.LENGTH_LONG)
                                        .show();
                                return;
                            }
                        }
                        saveUserToFirestore(user, currentRole, extraData);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Error checking user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String role, String extraData) {
        if (user == null)
            return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", role);

        final String[] messIdHolder = new String[1];
        if ("MESS_OWNER".equals(role)) {
            // Generate a unique Mess ID for the new owner
            messIdHolder[0] = "MESS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            userData.put("messId", messIdHolder[0]);
            userData.put("messName", extraData);
        } else {
            // User is joining an existing mess
            userData.put("messId", extraData);
        }

        // First save user document
        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // If owner, create mess document after user doc succeeds
                    if ("MESS_OWNER".equals(role) && messIdHolder[0] != null) {
                        createMessDocument(user.getUid(), messIdHolder[0], extraData, role);
                    } else {
                        // Not an owner, proceed to dashboard
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                        navigateToDashboard(role);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("Login", "Firestore user save failed", e);
                    Toast.makeText(LoginActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_LONG)
                            .show();
                });
    }

    private void createMessDocument(String ownerId, String messId, String messName, String role) {
        Map<String, Object> messData = new HashMap<>();
        messData.put("ownerId", ownerId);
        messData.put("name", messName);
        messData.put("studentCount", 0);
        messData.put("createdAt", new java.util.Date());

        db.collection("messes").document(messId).set(messData)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Signup Successful", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(role);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("Login", "Firestore mess save failed", e);
                    Toast.makeText(LoginActivity.this, "Error creating mess: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still navigate to dashboard even if mess creation fails
                    // The user can retry later or contact support
                    navigateToDashboard(role);
                });
    }

    private void fetchUserRole(FirebaseUser user) {
        Log.d("Login", "Fetching user role for UID: " + user.getUid());
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        Log.d("Login", "User role found: " + role);
                        navigateToDashboard(role);
                    } else {
                        Log.w("Login", "User document does not exist for UID: " + user.getUid());
                        Toast.makeText(LoginActivity.this, "User data not found. Please sign up first.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e("Login", "Firestore permission error for users/" + user.getUid(), e);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("permission")) {
                        Toast.makeText(LoginActivity.this, "Firebase permission denied. Please check Firestore rules.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard(String role) {
        saveFCMToken(); // Save FCM token after successful login/signup
        if ("MESS_OWNER".equals(role)) {
            startActivity(new Intent(LoginActivity.this, MessDashboardActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, UserDashboardActivity.class));
        }
        finishAffinity();
    }

    private void navigateToDashboardAsGuest(String role) {
        Intent intent;
        if ("MESS_OWNER".equals(role)) {
            intent = new Intent(LoginActivity.this, MessDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
        }
        intent.putExtra("IS_GUEST", true);
        Log.d("LoginActivity", "Navigating to dashboard as guest, role: " + role);
        startActivity(intent);
        finishAffinity();
    }

    private void saveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            Map<String, Object> tokenData = new HashMap<>();
                            tokenData.put("fcmToken", token);
                            db.collection("users").document(currentUser.getUid())
                                    .update(tokenData)
                                    .addOnSuccessListener(aVoid -> {
                                        // Token saved successfully
                                    })
                                    .addOnFailureListener(e -> {
                                        // Log error or handle failure
                                    });
                        }
                    } else {
                        // Handle the error of not getting FCM token
                    }
                });
    }
}
