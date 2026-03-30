package com.example.messapp.ui.user.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.messapp.LoginActivity;
import com.example.messapp.R;
import com.example.messapp.databinding.FragmentUserProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.messapp.utils.ThemeManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserMessId;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();

        binding.btnChangePassword.setOnClickListener(v -> handleChangePassword());
        binding.btnLogout.setOnClickListener(v -> handleLogout());
        binding.btnHelpSupport.setOnClickListener(v -> handleHelpSupport());
        binding.btnRenewSubscription.setOnClickListener(v -> handleRenewSubscription());
        binding.btnEditProfileImage.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditUserProfileActivity.class));
        });

        binding.btnMyReviews.setOnClickListener(v -> {
            if (currentUserMessId != null) {
                Intent intent = new Intent(getActivity(), com.example.messapp.MessReviewsActivity.class);
                intent.putExtra("messId", currentUserMessId);
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Please wait for profile to load or join a mess.", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        setupThemeToggle();

        return root;
    }

    private void setupThemeToggle() {
        if (binding.switchDarkMode != null) {
            binding.switchDarkMode.setChecked(ThemeManager.isDarkMode(requireContext()));
            binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != ThemeManager.isDarkMode(requireContext())) {
                    ThemeManager.setDarkMode(requireContext(), isChecked);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (binding != null) {
                binding.textProfileEmail.setText("Email: " + currentUser.getEmail());
            }
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (binding == null) return;
                        if (documentSnapshot.exists()) {
                            String messId = documentSnapshot.getString("messId");
                            currentUserMessId = messId;
                            String name = documentSnapshot.getString("name");

                            if (name != null) {
                                binding.textProfileName.setText("Name: " + name);
                            } else {
                                binding.textProfileName.setText("Name: Not Set");
                            }

                            if (messId != null) {
                                binding.textProfileMessId.setText("Mess ID: " + messId);
                                fetchMessName(messId);
                            } else {
                                binding.textProfileMessId.setVisibility(View.GONE);
                                binding.textProfileMessName.setText("Not Joined");
                            }

                            Long lunchExpiry = documentSnapshot.getLong("lunchSubscriptionExpiry");
                            Long dinnerExpiry = documentSnapshot.getLong("dinnerSubscriptionExpiry");
                            Long generalExpiry = documentSnapshot.getLong("subscriptionExpiry");

                            updateSubscriptionStatus(binding.textLunchExpiry, "Lunch",
                                    lunchExpiry != null ? lunchExpiry : (generalExpiry != null ? generalExpiry : 0));
                            updateSubscriptionStatus(binding.textDinnerExpiry, "Dinner",
                                    dinnerExpiry != null ? dinnerExpiry : (generalExpiry != null ? generalExpiry : 0));

                            binding.textSubscriptionExpiry.setVisibility(View.GONE);

                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                com.bumptech.glide.Glide.with(this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.ic_person_black_24dp)
                                        .into(binding.profileImage);
                            }
                        } else {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() != null)
                            Toast.makeText(getContext(), "Error loading user profile", Toast.LENGTH_SHORT).show();
                    });
        } else {
            if (getContext() != null)
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchMessName(String messId) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        String messName = documentSnapshot.getString("name");
                        if (messName != null) {
                            binding.textProfileMessName.setText("Mess Name: " + messName);
                        } else {
                            binding.textProfileMessName.setText("Mess Name: Not Set");
                        }
                    } else {
                        binding.textProfileMessName.setText("Mess Name: Not Found");
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error fetching mess name: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                });
    }

    private void handleChangePassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (getContext() != null)
                                Toast.makeText(getContext(), "Password reset email sent to " + user.getEmail(),
                                        Toast.LENGTH_LONG).show();
                        } else {
                            if (getContext() != null)
                                Toast.makeText(getContext(),
                                        "Failed to send reset email: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "No email associated with this account or user not logged in.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void handleHelpSupport() {
        Toast.makeText(getContext(), "Please contact support@messuncal.com for help.", Toast.LENGTH_LONG).show();
    }

    private void handleRenewSubscription() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;
                    if (documentSnapshot.exists()) {
                        String messId = documentSnapshot.getString("messId");
                        String name = documentSnapshot.getString("name");
                        String email = currentUser.getEmail();

                        if (messId == null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Not joined any mess.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String requestId = db.collection("subscriptionRequests").document().getId();
                        com.example.messapp.models.SubscriptionRequest request = new com.example.messapp.models.SubscriptionRequest(
                                requestId,
                                currentUser.getUid(),
                                name != null ? name : "Student",
                                email,
                                messId,
                                System.currentTimeMillis(),
                                "PENDING",
                                "BOTH");

                        db.collection("subscriptionRequests").document(requestId).set(request)
                                .addOnSuccessListener(aVoid -> {
                                    if (binding == null) return;
                                    binding.progressBar.setVisibility(View.GONE);
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Request Sent")
                                            .setMessage(
                                                    "Your renewal request has been sent to the Mess Owner. Please wait for them to approve it.")
                                            .setPositiveButton("OK", null)
                                            .show();
                                })
                                .addOnFailureListener(e -> {
                                    if (binding == null) return;
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Failed to send request: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateSubscriptionStatus(android.widget.TextView textView, String label, long expiry) {
        if (expiry > 0) {
            long diff = expiry - System.currentTimeMillis();
            long daysLeft = diff / (1000 * 60 * 60 * 24);

            if (daysLeft < 0) {
                textView.setText(label + ": Expired");
                textView.setTextColor(android.graphics.Color.RED);
            } else {
                textView.setText(label + ": Active (" + daysLeft + "d left)");
                textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        } else {
            textView.setText(label + ": No Sub");
            textView.setTextColor(android.graphics.Color.GRAY);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
