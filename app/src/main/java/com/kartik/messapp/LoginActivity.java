package com.kartik.messapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.kartik.messapp.R;
import com.kartik.messapp.databinding.ActivityLoginBinding;
import com.kartik.messapp.utils.ThemeManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

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
    private boolean isLoginMode = false;
    
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(LoginActivity.this, "Cancelled QR Scan", Toast.LENGTH_LONG).show();
                } else {
                    String scannedData = result.getContents();
                    String extractedMessId = scannedData;
                    try {
                        android.net.Uri uri = android.net.Uri.parse(scannedData);
                        if (uri.getQueryParameter("code") != null) {
                            extractedMessId = uri.getQueryParameter("code");
                        } else if (uri.getQueryParameter("messId") != null) {
                            extractedMessId = uri.getQueryParameter("messId");
                        } else if (uri.getLastPathSegment() != null && !uri.getLastPathSegment().endsWith(".html")) {
                            extractedMessId = uri.getLastPathSegment();
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors and fallback to the raw scanned data
                    }
                    if (binding != null && binding.messIdEditText != null) {
                        binding.messIdEditText.setText(extractedMessId);
                    }
                    Toast.makeText(LoginActivity.this, "Scanned: " + extractedMessId, Toast.LENGTH_SHORT).show();
                }
            });

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

            if (getIntent().hasExtra("IS_SIGNUP_MODE") && getIntent().getBooleanExtra("IS_SIGNUP_MODE", false)) {
                isLoginMode = false;
            }

            // Check if guest mode
            boolean isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
            if (isGuest) {
                Log.d("LoginActivity", "Guest mode detected, skipping auth for role: " + currentRole);
                navigateToDashboardAsGuest(currentRole);
                return;
            }

            Log.d("LoginActivity", "User role: " + currentRole);
            updateUI();
            
            // Prefill mess ID if it came from a deep link
            String prefillMessId = getIntent().getStringExtra("PREFILL_MESS_ID");
            if (prefillMessId != null && !prefillMessId.isEmpty() && binding.messIdEditText != null) {
                binding.messIdEditText.setText(prefillMessId);
            }
            
            if (binding.messIdLayout != null) {
                binding.messIdLayout.setEndIconOnClickListener(v -> {
                    ScanOptions options = new ScanOptions();
                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
                    options.setPrompt("Scan Mess Owner's QR Code");
                    options.setCameraId(0); 
                    options.setBeepEnabled(true);
                    options.setBarcodeImageEnabled(true);
                    barcodeLauncher.launch(options);
                });
            }

            setupBackNavigation();

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

            // Student Name & Phone click listeners (already in layout)
            if (binding.studentNameEditText != null) {
                binding.studentNameEditText.setOnClickListener(v -> {});
            }
            if (binding.studentPhoneEditText != null) {
                binding.studentPhoneEditText.setOnClickListener(v -> {});
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

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(LoginActivity.this, RoleSelectionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                // overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        });
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
                // Hide student fields during login
                if (binding.studentNameLayout != null) {
                    binding.studentNameLayout.setVisibility(View.GONE);
                }
                if (binding.studentPhoneLayout != null) {
                    binding.studentPhoneLayout.setVisibility(View.GONE);
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
                    // Hide student fields for mess owner
                    if (binding.studentNameLayout != null) {
                        binding.studentNameLayout.setVisibility(View.GONE);
                    }
                    if (binding.studentPhoneLayout != null) {
                        binding.studentPhoneLayout.setVisibility(View.GONE);
                    }
                } else {
                    if (binding.messIdLayout != null) {
                        binding.messIdLayout.setVisibility(View.VISIBLE);
                    }
                    if (binding.messNameLayout != null) {
                        binding.messNameLayout.setVisibility(View.GONE);
                    }
                    // Show student name & phone fields for student signup
                    if (binding.studentNameLayout != null) {
                        binding.studentNameLayout.setVisibility(View.VISIBLE);
                    }
                    if (binding.studentPhoneLayout != null) {
                        binding.studentPhoneLayout.setVisibility(View.VISIBLE);
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
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Login Failed: " + errMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performSignup() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String extraData = "";
        String studentName = "";
        String studentPhone = "";

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
            // Get student name and phone
            studentName = binding.studentNameEditText.getText().toString().trim();
            studentPhone = binding.studentPhoneEditText.getText().toString().trim();
            if (TextUtils.isEmpty(studentName)) {
                binding.studentNameEditText.setError("Required");
                return;
            }
            if (TextUtils.isEmpty(studentPhone)) {
                binding.studentPhoneEditText.setError("Required");
                return;
            }
            if (studentPhone.length() < 10) {
                binding.studentPhoneEditText.setError("Invalid phone number");
                return;
            }
            if (!studentPhone.matches("[6-9]\\d{9}")) {
                binding.studentPhoneEditText.setError("Enter a valid 10-digit mobile number");
                return;
            }
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password))
            return;

        binding.progressBar.setVisibility(View.VISIBLE);
        String finalExtraData = extraData;
        String finalStudentName = studentName;
        String finalStudentPhone = studentPhone;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(mAuth.getCurrentUser(), currentRole, finalExtraData, finalStudentName, finalStudentPhone);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Signup Failed: " + errMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performForgotPassword() {
        String currentEmail = binding.emailEditText.getText().toString().trim();
        
        android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if (!TextUtils.isEmpty(currentEmail)) {
            emailInput.setText(currentEmail);
        }

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        emailInput.setPadding(padding, padding, padding, padding);

        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_Messapp)
                .setTitle("Reset Password")
                .setMessage("Enter the email address associated with your account. We will send you a password reset link.")
                .setView(emailInput)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    binding.progressBar.setVisibility(View.VISIBLE);
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                binding.progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Error sending reset email: " + task.getException().getMessage(), Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleGoogleSignIn() {
        CredentialManager credentialManager = CredentialManager.create(this);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
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
                        boolean isNewUser = false;
                        if (task.getResult() != null && task.getResult().getAdditionalUserInfo() != null) {
                            isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();
                        }
                        
                        if (isNewUser) {
                            binding.progressBar.setVisibility(View.GONE);
                            Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                            intent.putExtra("ROLE", currentRole);
                            startActivity(intent);
                        } else {
                            checkUserExists(user);
                        }
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
                        // Edge case: if user document doesn't exist but isNewUser was false
                        binding.progressBar.setVisibility(View.GONE);
                        Intent intent = new Intent(LoginActivity.this, CompleteProfileActivity.class);
                        intent.putExtra("ROLE", currentRole);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Error checking user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String role, String extraData, String studentName, String studentPhone) {
        if (user == null)
            return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", role);

        // Add student name and phone for student users
        if (!TextUtils.isEmpty(studentName)) {
            userData.put("name", studentName);
        }
        if (!TextUtils.isEmpty(studentPhone)) {
            userData.put("phone", studentPhone);
        }

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
                    // Do not navigate to dashboard on failure, to prevent broken state
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
        // Cache role in shared prefs so FCM service can route notifications correctly
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().putString("role", role).apply();
        saveFCMToken();

        // Log Firebase Analytics LOGIN event
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "firebase_auth");
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.LOGIN, bundle);

        Intent intent = "MESS_OWNER".equals(role)
                ? new Intent(LoginActivity.this, MessDashboardActivity.class)
                : new Intent(LoginActivity.this, UserDashboardActivity.class);
        startActivity(intent);
        // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
        // overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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
                                        Log.e("LoginActivity", "Failed to save FCM token to Firestore", e);
                                    });
                        }
                    } else {
                        Log.e("LoginActivity", "Failed to get FCM token", task.getException());
                    }
                });
    }
}
