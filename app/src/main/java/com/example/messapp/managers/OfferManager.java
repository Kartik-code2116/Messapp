package com.example.messapp.managers;

import com.example.messapp.models.Offer;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OfferManager handles offer/promotion management including:
 * - Creating and managing offers for mess owners
 * - Retrieving active offers for users
 * - Validating offer applicability
 * - Tracking offer usage
 */
public class OfferManager {
    private final FirebaseFirestore db;

    public interface OfferCallback {
        void onSuccess(Offer offer);

        void onFailure(String errorMessage);
    }

    public interface OfferListCallback {
        void onSuccess(List<Offer> offers);

        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();

        void onFailure(String errorMessage);
    }

    public OfferManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new offer for a mess
     */
    public void createOffer(String messId, String title, String description, double discountPercentage, long expiryDate,
            UpdateCallback callback) {
        String offerId = "OFFER_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> offerData = new HashMap<>();
        offerData.put("offerId", offerId);
        offerData.put("messId", messId);
        offerData.put("title", title);
        offerData.put("description", description);
        offerData.put("discountPercentage", discountPercentage);
        offerData.put("expiryDate", expiryDate);
        offerData.put("createdAt", System.currentTimeMillis());
        offerData.put("usageCount", 0);
        offerData.put("active", true);

        db.collection("offers").document(offerId).set(offerData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all active offers for a mess
     */
    public void getMessOffers(String messId, OfferListCallback callback) {
        long currentTime = System.currentTimeMillis();

        db.collection("offers")
                .whereEqualTo("messId", messId)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Offer> offers = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Offer offer = querySnapshot.getDocuments().get(i).toObject(Offer.class);
                        if (offer != null && offer.getExpiryDate().getTime() > currentTime) {
                            offers.add(offer);
                        }
                    }
                    callback.onSuccess(offers);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all active offers (for users to discover)
     */
    public void getAllActiveOffers(OfferListCallback callback) {
        long currentTime = System.currentTimeMillis();

        db.collection("offers")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Offer> offers = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Offer offer = querySnapshot.getDocuments().get(i).toObject(Offer.class);
                        if (offer != null && offer.getExpiryDate().getTime() > currentTime) {
                            offers.add(offer);
                        }
                    }
                    callback.onSuccess(offers);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get offers for a specific mess (user view)
     */
    public void getOffersForMess(String messId, OfferListCallback callback) {
        getMessOffers(messId, callback);
    }

    /**
     * Update an offer
     */
    public void updateOffer(String offerId, String title, String description, double discountPercentage,
            long expiryDate, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("discountPercentage", discountPercentage);
        updates.put("expiryDate", expiryDate);

        db.collection("offers").document(offerId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Deactivate an offer
     */
    public void deactivateOffer(String offerId, UpdateCallback callback) {
        db.collection("offers").document(offerId).update("active", false)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Delete an offer
     */
    public void deleteOffer(String offerId, UpdateCallback callback) {
        db.collection("offers").document(offerId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Track offer usage
     */
    public void trackOfferUsage(String offerId, UpdateCallback callback) {
        db.collection("offers").document(offerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        long usageCount = documentSnapshot.getLong("usageCount");
                        db.collection("offers").document(offerId).update("usageCount", usageCount + 1)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get offer details
     */
    public void getOfferDetails(String offerId, OfferCallback callback) {
        db.collection("offers").document(offerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Offer offer = documentSnapshot.toObject(Offer.class);
                        if (offer != null) {
                            callback.onSuccess(offer);
                        } else {
                            callback.onFailure("Failed to parse offer");
                        }
                    } else {
                        callback.onFailure("Offer not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Check if offer is still valid
     */
    public void isOfferValid(String offerId, com.example.messapp.managers.OfferManager.BooleanCallback callback) {
        db.collection("offers").document(offerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Offer offer = documentSnapshot.toObject(Offer.class);
                        if (offer != null) {
                            long currentTime = System.currentTimeMillis();
                            boolean isValid = offer.getExpiryDate().getTime() > currentTime && offer.isActive();
                            callback.onSuccess(isValid);
                        } else {
                            callback.onSuccess(false);
                        }
                    } else {
                        callback.onSuccess(false);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface BooleanCallback {
        void onSuccess(boolean result);

        void onFailure(String errorMessage);
    }
}
