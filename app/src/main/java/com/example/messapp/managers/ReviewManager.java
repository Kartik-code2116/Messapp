package com.example.messapp.managers;

import com.example.messapp.models.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ReviewManager handles rating and review system including:
 * - Creating and managing reviews
 * - Calculating average ratings
 * - Fetching reviews for a mess
 * - Updating/deleting reviews
 */
public class ReviewManager {
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public interface ReviewCallback {
        void onSuccess(Review review);
        void onFailure(String errorMessage);
    }

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);
        void onFailure(String errorMessage);
    }

    public interface RatingCallback {
        void onSuccess(double rating);
        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public ReviewManager() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Create a new review for a mess
     */
    public void createReview(String messId, float rating, String comment, String userName, UpdateCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("User not authenticated");
            return;
        }
        if (messId == null || messId.trim().isEmpty()) {
            callback.onFailure("Mess not found");
            return;
        }
        if (rating <= 0) {
            callback.onFailure("Please provide a rating");
            return;
        }

        getUserReviewForMess(currentUser.getUid(), messId, new ReviewCallback() {
            @Override
            public void onSuccess(Review review) {
                updateReview(review.getReviewId(), rating, comment, callback);
            }

            @Override
            public void onFailure(String errorMessage) {
                createNewReview(messId, rating, comment, userName, callback, currentUser);
            }
        });
    }

    private void createNewReview(String messId, float rating, String comment, String userName, UpdateCallback callback,
            FirebaseUser currentUser) {
        String reviewId = "REVIEW_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("reviewId", reviewId);
        reviewData.put("messId", messId);
        reviewData.put("userId", currentUser.getUid());
        reviewData.put("userName", userName != null && !userName.trim().isEmpty() ? userName : "Student");
        reviewData.put("rating", rating);
        reviewData.put("comment", comment);
        reviewData.put("timestamp", FieldValue.serverTimestamp());
        reviewData.put("likes", 0);

        db.collection("reviews").document(reviewId).set(reviewData)
                .addOnSuccessListener(aVoid -> {
                    updateMessRatingAfterCreate(messId, rating);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all reviews for a mess
     */
    public void getMessReviews(String messId, ReviewListCallback callback) {
        db.collection("reviews")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Review review = querySnapshot.getDocuments().get(i).toObject(Review.class);
                        if (review != null) {
                            if (review.getReviewId() == null) {
                                review.setReviewId(querySnapshot.getDocuments().get(i).getId());
                            }
                            reviews.add(review);
                        }
                    }
                    sortNewestFirst(reviews);
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get all reviews written by a user
     */
    public void getReviewsForUser(String userId, ReviewListCallback callback) {
        db.collection("reviews")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                            Review review = querySnapshot.getDocuments().get(i).toObject(Review.class);
                            if (review != null) {
                                if (review.getReviewId() == null) {
                                    review.setReviewId(querySnapshot.getDocuments().get(i).getId());
                                }
                                reviews.add(review);
                            }
                        }
                    }
                    sortNewestFirst(reviews);
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get average rating for a mess
     */
    public void getAverageRating(String messId, RatingCallback callback) {
        db.collection("reviews")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.getDocuments().size() == 0) {
                        callback.onSuccess(0.0);
                        return;
                    }

                    float totalRating = 0;
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Review review = querySnapshot.getDocuments().get(i).toObject(Review.class);
                        if (review != null) {
                            totalRating += review.getRating();
                        }
                    }
                    double averageRating = totalRating / querySnapshot.getDocuments().size();
                    callback.onSuccess(averageRating);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update mess average rating
     */
    private void updateMessAverageRating(String messId) {
        getAverageRating(messId, new RatingCallback() {
            @Override
            public void onSuccess(double rating) {
                db.collection("messes").document(messId).update("avgRating", rating)
                        .addOnSuccessListener(aVoid -> {
                            // Get total review count
                            db.collection("reviews")
                                    .whereEqualTo("messId", messId)
                                    .get()
                                    .addOnSuccessListener(querySnapshot ->
                                            db.collection("messes").document(messId).update("numReviews", (long) querySnapshot.getDocuments().size())
                                    );
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                // Silent fail
            }
        });
    }

    /**
     * Update a review
     */
    public void updateReview(String reviewId, float rating, String comment, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", rating);
        updates.put("comment", comment);
        updates.put("timestamp", FieldValue.serverTimestamp());

        db.collection("reviews").document(reviewId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String messId = documentSnapshot.getString("messId");
                        Double oldRatingDouble = documentSnapshot.getDouble("rating");
                        double oldRating = oldRatingDouble != null ? oldRatingDouble : 0.0;
                        db.collection("reviews").document(reviewId).update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    if (messId != null) {
                                        updateMessRatingAfterUpdate(messId, oldRating, rating);
                                    }
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        callback.onFailure("Review not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Delete a review
     */
    public void deleteReview(String reviewId, UpdateCallback callback) {
        db.collection("reviews").document(reviewId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String messId = documentSnapshot.getString("messId");
                        Double oldRatingDouble = documentSnapshot.getDouble("rating");
                        double oldRating = oldRatingDouble != null ? oldRatingDouble : 0.0;
                        db.collection("reviews").document(reviewId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (messId != null) {
                                        updateMessRatingAfterDelete(messId, oldRating);
                                    }
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        callback.onFailure("Review not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Like a review
     */
    public void likeReview(String reviewId, UpdateCallback callback) {
        db.collection("reviews").document(reviewId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long currentLikes = documentSnapshot.getLong("likes");
                        long likes = currentLikes != null ? currentLikes : 0;
                        db.collection("reviews").document(reviewId).update("likes", likes + 1)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    } else {
                        callback.onFailure("Review not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get user's review for a mess
     */
    public void getUserReviewForMess(String userId, String messId, ReviewCallback callback) {
        db.collection("reviews")
                .whereEqualTo("userId", userId)
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.getDocuments().size() > 0) {
                        Review review = querySnapshot.getDocuments().get(0).toObject(Review.class);
                        if (review != null) {
                            if (review.getReviewId() == null) {
                                review.setReviewId(querySnapshot.getDocuments().get(0).getId());
                            }
                            callback.onSuccess(review);
                        } else {
                            callback.onFailure("Failed to parse review");
                        }
                    } else {
                        callback.onFailure("No review found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void updateMessRatingAfterCreate(String messId, double newRating) {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference messRef = db.collection("messes").document(messId);
            com.google.firebase.firestore.DocumentSnapshot messDoc = transaction.get(messRef);
            Double avgRatingValue = messDoc.getDouble("avgRating");
            Long reviewCountValue = messDoc.getLong("numReviews");
            double avgRating = avgRatingValue != null ? avgRatingValue : 0.0;
            long reviewCount = reviewCountValue != null ? reviewCountValue : 0;
            long updatedCount = reviewCount + 1;
            double updatedAverage = ((avgRating * reviewCount) + newRating) / updatedCount;
            transaction.update(messRef, "avgRating", updatedAverage, "numReviews", updatedCount);
            return null;
        });
    }

    private void updateMessRatingAfterUpdate(String messId, double oldRating, double newRating) {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference messRef = db.collection("messes").document(messId);
            com.google.firebase.firestore.DocumentSnapshot messDoc = transaction.get(messRef);
            Double avgRatingValue = messDoc.getDouble("avgRating");
            Long reviewCountValue = messDoc.getLong("numReviews");
            double avgRating = avgRatingValue != null ? avgRatingValue : 0.0;
            long reviewCount = reviewCountValue != null ? reviewCountValue : 0;
            if (reviewCount <= 0) {
                transaction.update(messRef, "avgRating", newRating, "numReviews", 1);
                return null;
            }
            double updatedAverage = ((avgRating * reviewCount) - oldRating + newRating) / reviewCount;
            transaction.update(messRef, "avgRating", updatedAverage, "numReviews", reviewCount);
            return null;
        });
    }

    private void updateMessRatingAfterDelete(String messId, double oldRating) {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference messRef = db.collection("messes").document(messId);
            com.google.firebase.firestore.DocumentSnapshot messDoc = transaction.get(messRef);
            Double avgRatingValue = messDoc.getDouble("avgRating");
            Long reviewCountValue = messDoc.getLong("numReviews");
            double avgRating = avgRatingValue != null ? avgRatingValue : 0.0;
            long reviewCount = reviewCountValue != null ? reviewCountValue : 0;
            long updatedCount = Math.max(0, reviewCount - 1);
            double updatedAverage = updatedCount == 0 ? 0.0 : ((avgRating * reviewCount) - oldRating) / updatedCount;
            transaction.update(messRef, "avgRating", updatedAverage, "numReviews", updatedCount);
            return null;
        });
    }

    private void sortNewestFirst(List<Review> reviews) {
        Collections.sort(reviews, (first, second) -> {
            if (first.getTimestamp() == null && second.getTimestamp() == null) return 0;
            if (first.getTimestamp() == null) return 1;
            if (second.getTimestamp() == null) return -1;
            return second.getTimestamp().compareTo(first.getTimestamp());
        });
    }
}
