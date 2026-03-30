package com.example.messapp.managers;

import com.example.messapp.Mess;
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
                                mess.getName().toLowerCase().contains(lowerQuery) ||
                                mess.getLocation().toLowerCase().contains(lowerQuery) ||
                                mess.getDescription().toLowerCase().contains(lowerQuery)
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
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();
                    String lowerLocation = location.toLowerCase();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null && mess.getLocation().toLowerCase().contains(lowerLocation)) {
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
                .orderBy("avgRating")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();

                    // Reverse to get highest first
                    for (int i = querySnapshot.getDocuments().size() - 1; i >= 0 && i >= querySnapshot.getDocuments().size() - 10; i--) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null && mess.getAvgRating() > 0) {
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
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null && mess.getMonthlyPrice() >= minPrice && mess.getMonthlyPrice() <= maxPrice) {
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
                .orderBy("studentCount")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();

                    // Reverse to get highest first
                    for (int i = querySnapshot.getDocuments().size() - 1; i >= 0 && i >= querySnapshot.getDocuments().size() - 10; i--) {
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
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Mess> messes = new ArrayList<>();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Mess mess = querySnapshot.getDocuments().get(i).toObject(Mess.class);
                        if (mess != null && mess.getAvgRating() >= minRating) {
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
        getAllMesses(new MessListCallback() {
            @Override
            public void onSuccess(List<Mess> messes) {
                List<Mess> filtered = new ArrayList<>();
                String lowerQuery = searchQuery != null ? searchQuery.toLowerCase() : "";

                for (Mess mess : messes) {
                    boolean matchesQuery = searchQuery == null || searchQuery.isEmpty() ||
                            mess.getName().toLowerCase().contains(lowerQuery) ||
                            mess.getLocation().toLowerCase().contains(lowerQuery);

                    boolean matchesLocation = location == null || location.isEmpty() ||
                            mess.getLocation().toLowerCase().contains(location.toLowerCase());

                    boolean matchesRating = mess.getAvgRating() >= minRating;

                    boolean matchesPrice = mess.getMonthlyPrice() >= minPrice && mess.getMonthlyPrice() <= maxPrice;

                    if (matchesQuery && matchesLocation && matchesRating && matchesPrice) {
                        filtered.add(mess);
                    }
                }
                callback.onSuccess(filtered);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }
}
