package com.example.messapp.managers;

import androidx.annotation.NonNull;
import com.example.messapp.Mess;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * ProfileManager handles mess owner profile management including:
 * - Fetching and updating mess information
 * - Managing profile pictures and descriptions
 * - Updating contact information
 * - Profile completion status
 */
public class ProfileManager {
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public interface ProfileCallback {
        void onSuccess(Mess mess);
        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public ProfileManager() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Fetch mess profile from Firestore
     */
    public void fetchMessProfile(String messId, ProfileCallback callback) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Mess mess = documentSnapshot.toObject(Mess.class);
                        if (mess != null) {
                            callback.onSuccess(mess);
                        } else {
                            callback.onFailure("Failed to parse mess data");
                        }
                    } else {
                        callback.onFailure("Mess not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update mess profile information
     */
    public void updateMessProfile(String messId, String name, String location, String description, String contact, double monthlyPrice, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("location", location);
        updates.put("description", description);
        updates.put("contact", contact);
        updates.put("monthlyPrice", monthlyPrice);

        db.collection("messes").document(messId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update profile picture URL
     */
    public void updateProfilePicture(String messId, String picturePath, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePictureUrl", picturePath);

        db.collection("messes").document(messId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get current user's mess ID
     */
    public void getCurrentUserMessId(ProfileCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String messId = documentSnapshot.getString("messId");
                        if (messId != null) {
                            fetchMessProfile(messId, callback);
                        } else {
                            callback.onFailure("Mess ID not found");
                        }
                    } else {
                        callback.onFailure("User data not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Check if profile is complete
     */
    public void isProfileComplete(String messId, ProfileCallback callback) {
        db.collection("messes").document(messId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String location = documentSnapshot.getString("location");
                        String description = documentSnapshot.getString("description");
                        String contact = documentSnapshot.getString("contact");
                        String profilePic = documentSnapshot.getString("profilePictureUrl");

                        boolean isComplete = location != null && !location.isEmpty() &&
                                description != null && !description.isEmpty() &&
                                contact != null && !contact.isEmpty() &&
                                profilePic != null && !profilePic.isEmpty();

                        if (isComplete) {
                            callback.onSuccess(documentSnapshot.toObject(Mess.class));
                        } else {
                            callback.onFailure("Profile incomplete");
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
