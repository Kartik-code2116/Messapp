package com.example.messapp.ui.user.profile;

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

import com.example.messapp.R;
import com.example.messapp.RoleSelectionActivity;
import com.example.messapp.databinding.FragmentUserProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.messapp.utils.ThemeManager;

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
        db    = FirebaseFirestore.getInstance();

        loadUserProfile();

        binding.btnChangePassword.setOnClickListener(v -> handleChangePassword());
        binding.btnLogout.setOnClickListener(v -> handleLogout());
        binding.btnHelpSupport.setOnClickListener(v -> handleHelpSupport());
        binding.btnRenewSubscription.setOnClickListener(v -> handleRenewSubscription());
        binding.btnEditProfileImage.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditUserProfileActivity.class)));
        binding.btnSeeReviews.setOnClickListener(v -> openMessReviews(false));
        binding.btnWriteReview.setOnClickListener(v -> openMessReviews(true));

        binding.btnMyReviews.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.messapp.MyReviewsActivity.class);
            startActivity(intent);
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
                    requireActivity().recreate();
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
                binding.textProfileEmail.setText(currentUser.getEmail());
                binding.textProfileEmailCard.setText(currentUser.getEmail());
            }
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (binding == null) return;
                        if (doc.exists()) {
                            String messId = doc.getString("messId");
                            currentUserMessId = messId;
                            String name = doc.getString("name");

                            String displayName = name != null && !name.isEmpty() ? name : "Student";
                            binding.textProfileName.setText(displayName);
                            binding.textProfileNameCard.setText(displayName);

                            String phone = doc.getString("phone");
                            binding.textProfilePhone.setText(phone != null && !phone.isEmpty() ? phone : "Not set");

                            if (messId != null) {
                                binding.textProfileMessId.setText("Mess ID: " + messId);
                                fetchMessName(messId);
                            } else {
                                binding.textProfileMessId.setVisibility(View.GONE);
                                binding.textProfileMessName.setText("Not Joined");
                            }

                            Long lunchExpiry   = doc.getLong("lunchSubscriptionExpiry");
                            Long dinnerExpiry  = doc.getLong("dinnerSubscriptionExpiry");
                            Long oneTimeExpiry = doc.getLong("oneTimeMealExpiry");
                            Long generalExpiry = doc.getLong("subscriptionExpiry");
                            String subType     = doc.getString("subscriptionType");

                            if ("ONE_TIME".equals(subType)) {
                                binding.textLunchExpiry.setVisibility(View.GONE);
                                binding.textDinnerExpiry.setVisibility(View.GONE);
                                binding.textOneTimeExpiry.setVisibility(View.VISIBLE);
                                updateSubscriptionStatus(binding.textOneTimeExpiry, "One Time a Day",
                                        oneTimeExpiry != null ? oneTimeExpiry : (generalExpiry != null ? generalExpiry : 0));
                            } else {
                                binding.textLunchExpiry.setVisibility(View.VISIBLE);
                                binding.textDinnerExpiry.setVisibility(View.VISIBLE);
                                binding.textOneTimeExpiry.setVisibility(View.GONE);
                                updateSubscriptionStatus(binding.textLunchExpiry,  "Lunch",
                                        lunchExpiry  != null ? lunchExpiry  : (generalExpiry != null ? generalExpiry : 0));
                                updateSubscriptionStatus(binding.textDinnerExpiry, "Dinner",
                                        dinnerExpiry != null ? dinnerExpiry : (generalExpiry != null ? generalExpiry : 0));
                            }

                            binding.textSubscriptionExpiry.setVisibility(View.GONE);

                            String profileImageUrl = doc.getString("profileImageUrl");
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
                            Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                    });
        } else {
            if (getContext() != null)
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchMessName(String messId) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    String messName = doc.exists() ? doc.getString("name") : null;
                    binding.textProfileMessName.setText(
                            messName != null && !messName.isEmpty() ? messName : "Not joined");
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openMessReviews(boolean openReviewDialog) {
        if (currentUserMessId == null || currentUserMessId.isEmpty()) {
            Toast.makeText(getContext(), "Join a mess first to use reviews.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), com.example.messapp.MessReviewsActivity.class);
        intent.putExtra(com.example.messapp.MessReviewsActivity.EXTRA_MESS_ID, currentUserMessId);
        intent.putExtra(com.example.messapp.MessReviewsActivity.EXTRA_OPEN_REVIEW_DIALOG, openReviewDialog);
        startActivity(intent);
    }

    private void handleChangePassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnCompleteListener(task -> {
                        if (getContext() == null) return;
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Password reset email sent to " + user.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    "Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "No email associated with this account.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    // FIX #6: Was navigating to LoginActivity, which skips role selection.
                    // Correctly route to RoleSelectionActivity so the user picks their role fresh.
                    Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void handleHelpSupport() {
        showHelpSupportDialog();
    }

    private void showHelpSupportDialog() {
        if (getContext() == null) return;

        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_help_support, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        android.widget.TextView textPhone = dialogView.findViewById(R.id.support_phone);
        android.widget.TextView textInsta = dialogView.findViewById(R.id.support_instagram);
        android.widget.TextView textEmail = dialogView.findViewById(R.id.support_email);

        android.view.View btnCopyPhone = dialogView.findViewById(R.id.btn_copy_phone);
        android.view.View btnActionPhone = dialogView.findViewById(R.id.btn_action_phone);

        android.view.View btnCopyInsta = dialogView.findViewById(R.id.btn_copy_instagram);
        android.view.View btnActionInsta = dialogView.findViewById(R.id.btn_action_instagram);

        android.view.View btnCopyEmail = dialogView.findViewById(R.id.btn_copy_email);
        android.view.View btnActionEmail = dialogView.findViewById(R.id.btn_action_email);

        android.view.View btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        
        // Default static data for app-level support
        String defaultPhone = "+91 98765 43210";
        String defaultInsta = "messapp_support";
        String defaultEmail = "support@messapp.com";
        
        if (currentUserMessId != null && !currentUserMessId.isEmpty()) {
            db.collection("messes").document(currentUserMessId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String contact = doc.getString("contact");
                    if (contact != null && !contact.isEmpty()) {
                        textPhone.setText(contact);
                    }
                    String messName = doc.getString("name");
                    if (messName != null && !messName.isEmpty()) {
                        textInsta.setText(messName.toLowerCase().replaceAll("\\s+", "_"));
                    }
                    
                    String ownerId = doc.getString("ownerId");
                    if (ownerId != null && !ownerId.isEmpty()) {
                        db.collection("users").document(ownerId).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String email = userDoc.getString("email");
                                if (email != null && !email.isEmpty()) {
                                    textEmail.setText(email);
                                }
                                String phone = userDoc.getString("phone");
                                if ((contact == null || contact.isEmpty()) && phone != null && !phone.isEmpty()) {
                                    textPhone.setText(phone);
                                }
                            }
                        });
                    }
                }
            });
        }

        btnCopyPhone.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Phone Number", textPhone.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Phone number copied!", Toast.LENGTH_SHORT).show();
        });

        btnActionPhone.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + textPhone.getText().toString().replaceAll(" ", "")));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Unable to open dialer", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyInsta.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Instagram Handle", textInsta.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Instagram handle copied!", Toast.LENGTH_SHORT).show();
        });

        btnActionInsta.setOnClickListener(v -> {
            try {
                String handle = textInsta.getText().toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/_u/" + handle));
                intent.setPackage("com.instagram.android");
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/" + handle)));
                }
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Unable to open Instagram", Toast.LENGTH_SHORT).show();
            }
        });

        btnCopyEmail.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Email Address", textEmail.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Email copied!", Toast.LENGTH_SHORT).show();
        });

        btnActionEmail.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{textEmail.getText().toString()});
                intent.putExtra(Intent.EXTRA_SUBJECT, "MessApp Support Request");
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(getContext(), "Unable to open Email client", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleRenewSubscription() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (binding == null) return;
                    if (doc.exists()) {
                        String messId = doc.getString("messId");
                        String name   = doc.getString("name");
                        String email  = currentUser.getEmail();

                        if (messId == null) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Not joined any mess.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String requestId = db.collection("subscriptionRequests").document().getId();
                        com.example.messapp.models.SubscriptionRequest request =
                                new com.example.messapp.models.SubscriptionRequest(
                                        requestId, currentUser.getUid(),
                                        name != null ? name : "Student",
                                        email, messId, System.currentTimeMillis(), "PENDING", "BOTH");

                        db.collection("subscriptionRequests").document(requestId).set(request)
                                .addOnSuccessListener(aVoid -> {
                                    if (binding == null) return;
                                    binding.progressBar.setVisibility(View.GONE);
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("Request Sent")
                                            .setMessage("Your renewal request has been sent to the Mess Owner.")
                                            .setPositiveButton("OK", null)
                                            .show();
                                })
                                .addOnFailureListener(e -> {
                                    if (binding == null) return;
                                    binding.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            long diff     = expiry - System.currentTimeMillis();
            long daysLeft = diff / (1000 * 60 * 60 * 24);
            if (daysLeft < 0) {
                textView.setText(label + " · Expired");
                textView.setTextColor(getResources().getColor(R.color.state_error, requireContext().getTheme()));
            } else {
                textView.setText(label + " · Active (" + daysLeft + "d left)");
                textView.setTextColor(getResources().getColor(R.color.state_success, requireContext().getTheme()));
            }
        } else {
            textView.setText(label + " · No subscription");
            textView.setTextColor(getResources().getColor(R.color.text_caption, requireContext().getTheme()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
