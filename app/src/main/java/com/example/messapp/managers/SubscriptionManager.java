package com.example.messapp.managers;

import com.example.messapp.models.Subscription;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SubscriptionManager handles user subscription management including:
 * - Creating and managing subscriptions
 * - Checking subscription status and expiry
 * - Renewing subscriptions
 * - Fetching user and mess subscriptions
 */
public class SubscriptionManager {
    private final FirebaseFirestore db;

    public interface SubscriptionCallback {
        void onSuccess(Subscription subscription);
        void onFailure(String errorMessage);
    }

    public interface SubscriptionListCallback {
        void onSuccess(List<Subscription> subscriptions);
        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface BooleanCallback {
        void onSuccess(boolean result);
        void onFailure(String errorMessage);
    }

    public SubscriptionManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new subscription for user to a mess
     */
    public void createSubscription(String userId, String messId, double monthlyPrice, int durationDays, UpdateCallback callback) {
        String subscriptionId = "SUB_" + UUID.randomUUID().toString().substring(0, 8);
        long startDate = System.currentTimeMillis();
        long expiryDate = startDate + (durationDays * 24 * 60 * 60 * 1000L);

        Map<String, Object> subscriptionData = new HashMap<>();
        subscriptionData.put("subscriptionId", subscriptionId);
        subscriptionData.put("userId", userId);
        subscriptionData.put("messId", messId);
        subscriptionData.put("startDate", startDate);
        subscriptionData.put("expiryDate", expiryDate);
        subscriptionData.put("status", "ACTIVE");
        subscriptionData.put("monthlyPrice", monthlyPrice);

        db.collection("subscriptions").document(subscriptionId).set(subscriptionData)
                .addOnSuccessListener(aVoid -> {
                    // Also update user's subscribed messes
                    updateUserSubscriptions(userId, messId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get user's active subscriptions
     */
    public void getUserSubscriptions(String userId, SubscriptionListCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Subscription> subscriptions = new ArrayList<>();
                    long currentTime = System.currentTimeMillis();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Subscription subscription = querySnapshot.getDocuments().get(i).toObject(Subscription.class);
                        if (subscription != null && subscription.getExpiryDate() > currentTime) {
                            subscriptions.add(subscription);
                        }
                    }
                    callback.onSuccess(subscriptions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all subscriptions for a mess
     */
    public void getMessSubscriptions(String messId, SubscriptionListCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Subscription> subscriptions = new ArrayList<>();
                    long currentTime = System.currentTimeMillis();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Subscription subscription = querySnapshot.getDocuments().get(i).toObject(Subscription.class);
                        if (subscription != null && subscription.getExpiryDate() > currentTime) {
                            subscriptions.add(subscription);
                        }
                    }
                    callback.onSuccess(subscriptions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Check if user has active subscription to a mess
     */
    public void hasActiveSubscription(String userId, String messId, BooleanCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long currentTime = System.currentTimeMillis();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Subscription subscription = querySnapshot.getDocuments().get(i).toObject(Subscription.class);
                        if (subscription != null && subscription.getExpiryDate() > currentTime) {
                            callback.onSuccess(true);
                            return;
                        }
                    }
                    callback.onSuccess(false);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Renew subscription
     */
    public void renewSubscription(String subscriptionId, int durationDays, UpdateCallback callback) {
        long expiryDate = System.currentTimeMillis() + (durationDays * 24 * 60 * 60 * 1000L);

        db.collection("subscriptions").document(subscriptionId).update("expiryDate", expiryDate)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Cancel subscription
     */
    public void cancelSubscription(String subscriptionId, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED");
        updates.put("expiryDate", System.currentTimeMillis());

        db.collection("subscriptions").document(subscriptionId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update user's subscribed messes list
     */
    @SuppressWarnings("unchecked")
    private void updateUserSubscriptions(String userId, String messId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> subscribedMesses = (List<String>) documentSnapshot.get("subscribedMesses");
                        if (subscribedMesses == null) {
                            subscribedMesses = new ArrayList<>();
                        }
                        if (!subscribedMesses.contains(messId)) {
                            subscribedMesses.add(messId);
                            db.collection("users").document(userId).update("subscribedMesses", subscribedMesses);
                        }
                    }
                });
    }

    /**
     * Get subscription details
     */
    public void getSubscriptionDetails(String subscriptionId, SubscriptionCallback callback) {
        db.collection("subscriptions").document(subscriptionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Subscription subscription = documentSnapshot.toObject(Subscription.class);
                        if (subscription != null) {
                            callback.onSuccess(subscription);
                        } else {
                            callback.onFailure("Failed to parse subscription");
                        }
                    } else {
                        callback.onFailure("Subscription not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
