package com.kartik.messapp.managers;

import com.kartik.messapp.models.Mess;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * DiscoveryManager handles mess discovery features including:
 * - Searching for messes by name/location
 * - Filtering messes by rating/price
 * - Getting nearby messes
 * - Getting trending/popular messes
 * - Getting recommended messes
 */
public class DiscoveryManager {
    private final FirebaseFirestore db;

    public interface MessListCallback {
        void onSuccess(List<Mess> messes);
        void onFailure(String errorMessage);
    }

    public DiscoveryManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get all available messes
     */
    public void getAllMesses(MessListCallback callback) {
        db.collection("messes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Search messes by name
     */
    public void searchMessesByName(String searchQuery, MessListCallback callback) {
        db.collection("messes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    String lowerQuery = searchQuery.toLowerCase();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null && (
                                (mess.getName() != null && mess.getName().toLowerCase().contains(lowerQuery)) ||
                                (mess.getLocation() != null && mess.getLocation().toLowerCase().contains(lowerQuery)) ||
                                (mess.getDescription() != null && mess.getDescription().toLowerCase().contains(lowerQuery))
                        )) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get messes by location
     */
    public void getMessesByLocation(String location, MessListCallback callback) {
        db.collection("messes")
                .whereEqualTo("location", location)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get top-rated messes
     */
    public void getTopRatedMesses(MessListCallback callback) {
        db.collection("messes")
                .orderBy("avgRating", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get messes within price range
     */
    public void getMessesByPriceRange(double minPrice, double maxPrice, MessListCallback callback) {
        db.collection("messes")
                .whereGreaterThanOrEqualTo("monthlyPrice", minPrice)
                .whereLessThanOrEqualTo("monthlyPrice", maxPrice)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get messes with high student count (popular)
     */
    public void getPopularMesses(MessListCallback callback) {
        db.collection("messes")
                .orderBy("studentCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get messes with minimum rating threshold
     */
    public void getHighRatedMesses(double minRating, MessListCallback callback) {
        db.collection("messes")
                .whereGreaterThanOrEqualTo("avgRating", minRating)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get mess details
     */
    public void getMessDetails(String messId, MessListCallback callback) {
        db.collection("messes").document(messId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    if (documentSnapshot.exists()) {
                        Mess mess = documentSnapshot.toObject(Mess.class);
                        if (mess != null) {
                            messes.add(mess);
                        }
                    }
                    callback.onSuccess(messes);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Advanced filter: Search by multiple criteria
     */
    public void advancedSearch(String searchQuery, String location, double minRating, double minPrice, double maxPrice, MessListCallback callback) {
        db.collection("messes")
                .whereGreaterThanOrEqualTo("monthlyPrice", minPrice)
                .whereLessThanOrEqualTo("monthlyPrice", maxPrice)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> filtered = new ArrayList<>();
                    String lowerQuery = searchQuery != null ? searchQuery.toLowerCase() : "";
                    String lowerLocation = location != null ? location.toLowerCase() : "";

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null) {
                            boolean matchesQuery = searchQuery == null || searchQuery.isEmpty() ||
                                    (mess.getName() != null && mess.getName().toLowerCase().contains(lowerQuery)) ||
                                    (mess.getLocation() != null && mess.getLocation().toLowerCase().contains(lowerQuery));

                            boolean matchesLocation = location == null || location.isEmpty() ||
                                    (mess.getLocation() != null && mess.getLocation().toLowerCase().contains(lowerLocation));

                            boolean matchesRating = mess.getAvgRating() >= minRating;

                            if (matchesQuery && matchesLocation && matchesRating) {
                                filtered.add(mess);
                            }
                        }
                    }
                    callback.onSuccess(filtered);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
