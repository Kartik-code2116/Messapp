package com.kartik.messapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.kartik.messapp.databinding.ActivityCompleteProfileBinding;
import com.kartik.messapp.utils.ThemeManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompleteProfileActivity extends AppCompatActivity {

    private ActivityCompleteProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentRole;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(CompleteProfileActivity.this, "Cancelled QR Scan", Toast.LENGTH_LONG).show();
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
                    
                    binding.messIdEditText.setText(extractedMessId);
                    Toast.makeText(CompleteProfileActivity.this, "Scanned: " + extractedMessId, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityCompleteProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentRole = getIntent().getStringExtra("ROLE");
        if (currentRole == null) {
            currentRole = "USER"; // Default fallback
        }

        setupUIForRole();

        binding.messIdLayout.setEndIconOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan Mess QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            barcodeLauncher.launch(options);
        });

        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void setupUIForRole() {
        binding.nameLayout.setVisibility(View.VISIBLE);
        binding.phoneLayout.setVisibility(View.VISIBLE);

        if ("MESS_OWNER".equals(currentRole)) {
            binding.messNameLayout.setVisibility(View.VISIBLE);
            binding.messIdLayout.setVisibility(View.GONE);
            binding.qrHintText.setVisibility(View.GONE);
        } else {
            binding.messIdLayout.setVisibility(View.VISIBLE);
            binding.qrHintText.setVisibility(View.VISIBLE);
            binding.messNameLayout.setVisibility(View.GONE);
        }
    }

    private void saveProfile() {
        String name = binding.nameEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();
        String extraData = "";

        if (TextUtils.isEmpty(name)) {
            binding.nameEditText.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(phone) || phone.length() < 10) {
            binding.phoneEditText.setError("Enter valid 10-digit number");
            return;
        }

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

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSaveProfile.setEnabled(false);

        saveUserToFirestore(user, currentRole, extraData, name, phone);
    }

    private void saveUserToFirestore(FirebaseUser user, String role, String extraData, String name, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", role);
        userData.put("name", name);
        userData.put("phone", phone);

        final String[] messIdHolder = new String[1];
        if ("MESS_OWNER".equals(role)) {
            messIdHolder[0] = "MESS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            userData.put("messId", messIdHolder[0]);
            userData.put("messName", extraData);
        } else {
            userData.put("messId", extraData);
        }

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    if ("MESS_OWNER".equals(role) && messIdHolder[0] != null) {
                        createMessDocument(user.getUid(), messIdHolder[0], extraData, role);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(CompleteProfileActivity.this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();
                        navigateToDashboard(role);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveProfile.setEnabled(true);
                    Log.e("CompleteProfile", "Firestore user save failed", e);
                    Toast.makeText(CompleteProfileActivity.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(CompleteProfileActivity.this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();
                    navigateToDashboard(role);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnSaveProfile.setEnabled(true);
                    Log.e("CompleteProfile", "Firestore mess save failed", e);
                    Toast.makeText(CompleteProfileActivity.this, "Error creating mess: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard(String role) {
        getSharedPreferences("user_prefs", MODE_PRIVATE).edit().putString("role", role).apply();
        Intent intent = "MESS_OWNER".equals(role)
                ? new Intent(this, MessDashboardActivity.class)
                : new Intent(this, UserDashboardActivity.class);
        startActivity(intent);
        finishAffinity();
    }
}
