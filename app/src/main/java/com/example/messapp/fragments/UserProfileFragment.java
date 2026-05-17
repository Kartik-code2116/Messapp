package com.example.messapp.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.messapp.RoleSelectionActivity;
import com.example.messapp.databinding.FragmentUserProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String email = mAuth.getCurrentUser().getEmail();
            binding.textProfileEmail.setText(email);
            loadProfileData(uid);
        }

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.btnChangePassword.setOnClickListener(v -> {
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(a -> Toast
                                .makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(
                                e -> Toast.makeText(getContext(), "Error sending email", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadProfileData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String messId = documentSnapshot.getString("messId");
                        binding.textProfileMessId.setText(messId);

                        // Load name (from signup) or fallback to "Student"
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            binding.textProfileName.setText(name);
                            binding.textProfilePhone.setText(documentSnapshot.getString("phone") != null ? 
                                documentSnapshot.getString("phone") : "Not set");
                        } else {
                            binding.textProfileName.setText("Student");
                            binding.textProfilePhone.setText("Not set");
                        }

                        fetchMessName(messId);
                    }
                });
    }

    private void fetchMessName(String messId) {
        if (messId == null)
            return;
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        binding.textProfileMessName.setText(documentSnapshot.getString("name"));
                    }
                });
    }
}
