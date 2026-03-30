package com.example.messapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.messapp.databinding.FragmentMessStudentsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MessStudentsFragment extends Fragment {

    private FragmentMessStudentsBinding binding;
    private FirebaseFirestore db;
    private String messId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessStudentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        fetchMessIdAndStudents();
    }

    private void fetchMessIdAndStudents() {
        String ownerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("messes").whereEqualTo("ownerId", ownerId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        messId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        loadStudents();
                    } else {
                        Toast.makeText(getContext(), "Error: Mess ID not found for this user.", Toast.LENGTH_SHORT)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching mess: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadStudents() {
        db.collection("users").whereEqualTo("messId", messId).addSnapshotListener((value, error) -> {
            if (value != null && !value.isEmpty()) {
                int studentCount = 0;
                for (DocumentSnapshot doc : value) {
                    String role = doc.getString("role");
                    if ("USER".equals(role)) {
                        studentCount++;
                    }
                }
                binding.textTotalStudentsCount.setText(String.valueOf(studentCount));
            } else {
                 binding.textTotalStudentsCount.setText("0");
            }
        });
    }
}
