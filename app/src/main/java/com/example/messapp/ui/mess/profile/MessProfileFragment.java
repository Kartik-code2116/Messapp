package com.example.messapp.ui.mess.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.messapp.EditMessProfileActivity;
import com.example.messapp.LoginActivity;
import com.example.messapp.R;
import com.example.messapp.databinding.FragmentMessProfileBinding;
import com.example.messapp.models.Mess;
import com.example.messapp.ui.mess.settings.MessSettingsActivity;
import com.example.messapp.ui.mess.weeklymenu.WeeklyMenuActivity;
import com.example.messapp.utils.ThemeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MessProfileFragment extends Fragment {

    private FragmentMessProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentMessId;
    private ListenerRegistration studentsListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        binding.btnCopyMessId.setOnClickListener(v -> copyMessIdToClipboard());
        binding.btnNavEditProfile.setOnClickListener(v -> handleEditProfile());
        binding.btnEditProfileImage.setOnClickListener(v -> handleEditProfile());
        binding.btnNavOffers.setOnClickListener(v -> handleOffers());
        binding.btnNavRevenue.setOnClickListener(v -> handleRevenue());
        binding.btnNavWeeklyMenu.setOnClickListener(v -> handleWeeklyMenu());
        binding.btnMessLogout.setOnClickListener(v -> handleLogout());

        binding.btnViewStudents.setOnClickListener(v -> handleViewStudents());

        setupThemeToggle();

        return root;
    }

    private void setupThemeToggle() {
        if (binding.switchDarkMode != null) {
            binding.switchDarkMode.setChecked(ThemeManager.isDarkMode(requireContext()));
            binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != ThemeManager.isDarkMode(requireContext())) {
                    ThemeManager.setDarkMode(requireContext(), isChecked);
                    // No need to call recreate() manually as setDefaultNightMode does it for all
                    // activities
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchMessOwnerData();
    }

    private void fetchMessOwnerData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (binding != null) {
                binding.textOwnerEmail.setText(currentUser.getEmail());
            }
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (binding == null)
                            return;
                        if (documentSnapshot.exists()) {
                            currentMessId = documentSnapshot.getString("messId");
                            if (currentMessId != null) {
                                fetchMessDetails(currentMessId);
                                setupRealtimeStudentsCount(currentMessId);
                            } else {
                                Toast.makeText(requireContext(), "Mess ID not found for this owner.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Mess owner data not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null) {
                            Toast.makeText(requireContext(), "Error fetching mess owner data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            if (getContext() != null) {
                Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchMessDetails(String messId) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null)
                        return;
                    if (documentSnapshot.exists()) {
                        Mess mess = documentSnapshot.toObject(Mess.class);
                        if (mess != null) {
                            binding.textMessProfileName.setText(mess.getName());
                            binding.textMessProfileId.setText(messId);
                            binding.textMessProfileLocation.setText(mess.getLocation());
                            binding.textMessProfileContact.setText(mess.getContact());
                            binding.textMessProfileDescription.setText(mess.getDescription());
                        }
                    } else {
                        Toast.makeText(requireContext(), "Mess details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(requireContext(), "Error fetching mess details: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupRealtimeStudentsCount(String messId) {
        if (studentsListener != null) {
            studentsListener.remove();
        }
        studentsListener = db.collection("users")
                .whereEqualTo("role", "USER")
                .whereEqualTo("messId", messId)
                .addSnapshotListener((snapshots, e) -> {
                    if (binding == null)
                        return;
                    if (e != null) {
                        return;
                    }
                    if (snapshots != null) {
                        int count = snapshots.size();
                        binding.textActiveStudentsCount.setText(String.valueOf(count));
                    }
                });
    }

    private void copyMessIdToClipboard() {
        if (currentMessId != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Mess ID", currentMessId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "Mess ID copied to clipboard", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Mess ID not available to copy.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEditProfile() {
        if (currentMessId != null) {
            Intent intent = new Intent(requireActivity(), EditMessProfileActivity.class);
            intent.putExtra(EditMessProfileActivity.EXTRA_MESS_ID, currentMessId);
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "Mess ID not available for editing.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOffers() {
        NavHostFragment.findNavController(this).navigate(R.id.action_messProfileFragment_to_messOffersFragment);
    }

    private void handleRevenue() {
        NavHostFragment.findNavController(this).navigate(R.id.action_messProfileFragment_to_messRevenueFragment);
    }

    private void handleViewStudents() {
        NavHostFragment.findNavController(this).navigate(R.id.action_messProfileFragment_to_navigation_mess_students);
    }

    private void handleWeeklyMenu() {
        Intent intent = new Intent(requireActivity(), WeeklyMenuActivity.class);
        startActivity(intent);
    }

    private void handleSettings() {
        Intent intent = new Intent(requireActivity(), MessSettingsActivity.class);
        startActivity(intent);
    }

    private void handleLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (studentsListener != null) {
            studentsListener.remove();
        }
        binding = null;
    }
}
