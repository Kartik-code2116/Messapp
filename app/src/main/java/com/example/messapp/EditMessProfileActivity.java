package com.example.messapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messapp.databinding.ActivityEditMessProfileBinding;
import com.example.messapp.models.Mess;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditMessProfileActivity extends AppCompatActivity {

    public static final String EXTRA_MESS_ID = "MESS_ID";
    private static final String COLLECTION_MESSES = "messes";

    private ActivityEditMessProfileBinding binding;
    private FirebaseFirestore db;
    private String messId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditMessProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup back button
        binding.btnBack.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();

        // Get messId from the intent
        messId = getIntent().getStringExtra(EXTRA_MESS_ID);

        if (messId == null) {
            Toast.makeText(this, "Mess ID not provided.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchMessDetails();

        binding.btnSaveProfile.setOnClickListener(v -> saveMessProfile());
    }

    private void fetchMessDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        DocumentReference messRef = db.collection(COLLECTION_MESSES).document(messId);

        messRef.get().addOnSuccessListener(documentSnapshot -> {
            binding.progressBar.setVisibility(View.GONE);
            if (documentSnapshot.exists()) {
                Mess mess = documentSnapshot.toObject(Mess.class);
                if (mess != null) {
                    binding.messNameEditText.setText(mess.getName());
                    binding.messLocationEditText.setText(mess.getLocation());
                    binding.messContactEditText.setText(mess.getContact());
                    binding.messDescriptionEditText.setText(mess.getDescription());
                    binding.messMonthlyPriceEditText.setText(String.valueOf(mess.getMonthlyPrice()));
                }
            } else {
                Toast.makeText(EditMessProfileActivity.this, "Mess details not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(EditMessProfileActivity.this, "Error fetching mess details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void saveMessProfile() {
        String name = binding.messNameEditText.getText().toString().trim();
        String location = binding.messLocationEditText.getText().toString().trim();
        String contact = binding.messContactEditText.getText().toString().trim();
        String description = binding.messDescriptionEditText.getText().toString().trim();
        String monthlyPriceStr = binding.messMonthlyPriceEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) || TextUtils.isEmpty(contact) || TextUtils.isEmpty(description) || TextUtils.isEmpty(monthlyPriceStr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double monthlyPrice = 0.0;
        try {
            monthlyPrice = Double.parseDouble(monthlyPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid monthly price", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> messData = new HashMap<>();
        messData.put(Mess.FIELD_NAME, name);
        messData.put(Mess.FIELD_LOCATION, location);
        messData.put(Mess.FIELD_CONTACT, contact);
        messData.put(Mess.FIELD_DESCRIPTION, description);
        messData.put(Mess.FIELD_MONTHLY_PRICE, monthlyPrice);

        db.collection(COLLECTION_MESSES).document(messId).update(messData)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditMessProfileActivity.this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to MessProfileFragment
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(EditMessProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
