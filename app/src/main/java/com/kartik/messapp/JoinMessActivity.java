package com.kartik.messapp;

import android.content.Intent;
import android.net.Uri;
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
import com.kartik.messapp.databinding.ActivityJoinMessBinding;
import com.kartik.messapp.utils.ThemeManager;

import java.util.HashMap;
import java.util.Map;

public class JoinMessActivity extends AppCompatActivity {

    private ActivityJoinMessBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(JoinMessActivity.this, "Cancelled QR Scan", Toast.LENGTH_LONG).show();
                } else {
                    String scannedData = result.getContents();
                    String extractedMessId = scannedData;
                    try {
                        Uri uri = Uri.parse(scannedData);
                        if (uri.getQueryParameter("code") != null) {
                            extractedMessId = uri.getQueryParameter("code");
                        } else if (uri.getQueryParameter("messId") != null) {
                            extractedMessId = uri.getQueryParameter("messId");
                        } else if (uri.getLastPathSegment() != null && !uri.getLastPathSegment().endsWith(".html")) {
                            extractedMessId = uri.getLastPathSegment();
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors and fallback to raw data
                    }
                    if (binding != null && binding.messIdEditText != null) {
                        binding.messIdEditText.setText(extractedMessId);
                    }
                    Toast.makeText(JoinMessActivity.this, "Scanned: " + extractedMessId, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityJoinMessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.messIdLayout.setEndIconOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan Mess Owner's QR Code");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(true);
            options.setCaptureActivity(CustomCaptureActivity.class);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
        });

        binding.btnJoinMess.setOnClickListener(v -> joinMess());
    }

    private void joinMess() {
        String messId = binding.messIdEditText.getText().toString().trim();

        if (TextUtils.isEmpty(messId)) {
            binding.messIdEditText.setError("Required");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnJoinMess.setEnabled(false);

        // Verify if the mess exists first
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // First, delete from mess_leavers if it exists
                        db.collection("mess_leavers")
                                .whereEqualTo("userId", user.getUid())
                                .whereEqualTo("messId", messId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                        batch.delete(doc.getReference());
                                    }
                                    
                                    // Mess exists, update user
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("messId", messId);
                                    
                                    batch.update(db.collection("users").document(user.getUid()), updates);
                                    
                                    batch.commit().addOnSuccessListener(aVoid -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        Toast.makeText(this, "Successfully joined mess!", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }).addOnFailureListener(e -> {
                                        binding.progressBar.setVisibility(View.GONE);
                                        binding.btnJoinMess.setEnabled(true);
                                        Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // If deletion fails, we still want to join the mess. 
                                    // Just fallback to old logic.
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("messId", messId);
                                    db.collection("users").document(user.getUid())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            binding.progressBar.setVisibility(View.GONE);
                                            Toast.makeText(this, "Successfully joined mess!", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                                });
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnJoinMess.setEnabled(true);
                        binding.messIdEditText.setError("Invalid Mess ID");
                        Toast.makeText(this, "Mess not found. Please check the ID.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnJoinMess.setEnabled(true);
                    Toast.makeText(this, "Error checking mess: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
