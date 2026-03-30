package com.example.messapp.ui.user.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.messapp.R;
import com.example.messapp.databinding.ActivityEditUserProfileBinding;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditUserProfileActivity extends AppCompatActivity {

    private ActivityEditUserProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;
    private Uri selectedImageUri;
    private String currentProfileImageUrl;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    binding.imgProfileEdit.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance("gs://messapp-5275d.firebasestorage.app");
        userId = mAuth.getCurrentUser().getUid();

        setupToolbar();
        loadCurrentProfile();

        binding.btnChangePhoto.setOnClickListener(v -> pickImage());
        binding.btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadCurrentProfile() {
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String gender = documentSnapshot.getString("gender");
                        String diet = documentSnapshot.getString("dietaryPreference");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        if (name != null)
                            binding.etName.setText(name);
                        if (phone != null)
                            binding.etPhone.setText(phone);

                        if (gender != null) {
                            if ("Male".equals(gender))
                                binding.chipMale.setChecked(true);
                            else if ("Female".equals(gender))
                                binding.chipFemale.setChecked(true);
                            else if ("Other".equals(gender))
                                binding.chipOther.setChecked(true);
                        }

                        if (diet != null) {
                            if ("Veg".equals(diet))
                                binding.chipVeg.setChecked(true);
                            else if ("Non-Veg".equals(diet))
                                binding.chipNonVeg.setChecked(true);
                            else if ("Both".equals(diet))
                                binding.chipBoth.setChecked(true);
                        }

                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.ic_person_black_24dp)
                                    .into(binding.imgProfileEdit);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        if (selectedImageUri != null) {
            uploadImageAndSaveProfile(name, phone);
        } else {
            updateFirestore(name, phone, currentProfileImageUrl);
        }
    }

    private void uploadImageAndSaveProfile(String name, String phone) {
        StorageReference ref = storage.getReference()
                .child("profile_images/" + userId + "/" + UUID.randomUUID().toString() + ".jpg");

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        if (task.getException() != null) {
                            throw task.getException();
                        }
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    updateFirestore(name, phone, uri.toString());
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed()) {
                        binding.progressBar.setVisibility(View.GONE);
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(this, "Image upload failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        android.util.Log.e("EditProfile", "Image upload failed", e);

                        if (errorMsg.contains("does not exist")) {
                            Toast.makeText(this, "Note: Please check if Storage is enabled in Firebase Console.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void updateFirestore(String name, String phone, String imageUrl) {
        String gender = "";
        int genderChipId = binding.chipGroupGender.getCheckedChipId();
        if (genderChipId == R.id.chip_male)
            gender = "Male";
        else if (genderChipId == R.id.chip_female)
            gender = "Female";
        else if (genderChipId == R.id.chip_other)
            gender = "Other";

        String diet = "";
        int dietChipId = binding.chipGroupDiet.getCheckedChipId();
        if (dietChipId == R.id.chip_veg)
            diet = "Veg";
        else if (dietChipId == R.id.chip_non_veg)
            diet = "Non-Veg";
        else if (dietChipId == R.id.chip_both)
            diet = "Both";

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("dietaryPreference", diet);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isDestroyed()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isDestroyed()) {
                        binding.progressBar.setVisibility(View.GONE);
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(this, "Update failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        android.util.Log.e("EditProfile", "Firestore update failed", e);
                    }
                });
    }
}
