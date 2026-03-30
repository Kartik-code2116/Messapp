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
import com.example.messapp.databinding.FragmentMessProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MessProfileFragment extends Fragment {

    private FragmentMessProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            binding.textOwnerEmail.setText(mAuth.getCurrentUser().getEmail());
            loadProfileData(uid);
        }

        binding.btnMessLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.btnCopyMessId.setOnClickListener(v -> {
            String messId = binding.textMessProfileId.getText().toString();
            if (!messId.equals("-") && !messId.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mess ID", messId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Mess ID copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Updated ID to match the layout (btn_nav_edit_profile -> btnNavEditProfile)
        binding.btnNavEditProfile.setOnClickListener(
                v -> Toast.makeText(getContext(), "Feature coming soon!", Toast.LENGTH_SHORT).show());

        binding.btnViewStudents.setOnClickListener(v -> {
            // Logic to navigate to students list or similar
            Toast.makeText(getContext(), "Opening Students List", Toast.LENGTH_SHORT).show();
        });

    }

    private void loadProfileData(String uid) {
        db.collection("messes").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String messId = documentSnapshot.getId();
                        String messName = documentSnapshot.getString("name");
                        String location = documentSnapshot.getString("location");
                        String phone = documentSnapshot.getString("phone");
                        String description = documentSnapshot.getString("description");

                        binding.textMessProfileId.setText(messId);
                        binding.textMessProfileName.setText(messName != null ? messName : "Mess Name");

                        if (location != null)
                            binding.textMessProfileLocation.setText(location);
                        if (phone != null)
                            binding.textMessProfileContact.setText(phone);
                        if (description != null)
                            binding.textMessProfileDescription.setText(description);
                    }
                });
    }
}
