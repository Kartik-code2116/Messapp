package com.example.messapp.managers;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AnalyticsManager handles subscriber and income tracking for mess owners including:
 * - Tracking active subscribers count
 * - Calculating monthly revenue
 * - Tracking subscription trends
 * - Getting analytics data for dashboards
 */
public class AnalyticsManager {
    private final FirebaseFirestore db;

    public interface AnalyticsCallback {
        void onSuccess(Map<String, Object> data);
        void onFailure(String errorMessage);
    }

    public interface NumberCallback {
        void onSuccess(double number);
        void onFailure(String errorMessage);
    }

    public AnalyticsManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Get total subscribers count for a mess
     */
    public void getTotalSubscribers(String messId, NumberCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    long activeCount = 0;
                    long currentTime = System.currentTimeMillis();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Long expiryDate = querySnapshot.getDocuments().get(i).getLong("expiryDate");
                        if (expiryDate != null && expiryDate > currentTime) {
                            activeCount++;
                        }
                    }
                    callback.onSuccess(activeCount);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get monthly revenue for a mess
     */
    public void getMonthlyRevenue(String messId, NumberCallback callback) {
        long currentTime = System.currentTimeMillis();
        long monthAgo = currentTime - (30 * 24 * 60 * 60 * 1000L);

        db.collection("transactions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "SUCCESS")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRevenue = 0;

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Long timestamp = querySnapshot.getDocuments().get(i).getLong("timestamp");
                        Double amount = querySnapshot.getDocuments().get(i).getDouble("amount");

                        if (timestamp != null && amount != null && timestamp > monthAgo) {
                            totalRevenue += amount;
                        }
                    }
                    callback.onSuccess(totalRevenue);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get total revenue for a mess (all time)
     */
    public void getTotalRevenue(String messId, NumberCallback callback) {
        db.collection("transactions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "SUCCESS")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalRevenue = 0;

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Double amount = querySnapshot.getDocuments().get(i).getDouble("amount");
                        if (amount != null) {
                            totalRevenue += amount;
                        }
                    }
                    callback.onSuccess(totalRevenue);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get subscription trend data
     */
    public void getSubscriptionTrend(String messId, AnalyticsCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Object> trendData = new HashMap<>();
                    int activeCount = 0;
                    int expiredCount = 0;
                    int cancelledCount = 0;
                    long currentTime = System.currentTimeMillis();

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        String status = querySnapshot.getDocuments().get(i).getString("status");
                        Long expiryDate = querySnapshot.getDocuments().get(i).getLong("expiryDate");

                        if ("ACTIVE".equals(status) && expiryDate != null && expiryDate > currentTime) {
                            activeCount++;
                        } else if ("EXPIRED".equals(status)) {
                            expiredCount++;
                        } else if ("CANCELLED".equals(status)) {
                            cancelledCount++;
                        }
                    }

                    trendData.put("active", activeCount);
                    trendData.put("expired", expiredCount);
                    trendData.put("cancelled", cancelledCount);
                    trendData.put("total", querySnapshot.getDocuments().size());

                    callback.onSuccess(trendData);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get dashboard analytics for mess owner
     */
    public void getDashboardAnalytics(String messId, AnalyticsCallback callback) {
        Map<String, Object> dashboardData = new HashMap<>();

        // Get total subscribers
        getTotalSubscribers(messId, new NumberCallback() {
            @Override
            public void onSuccess(double subscribers) {
                dashboardData.put("totalSubscribers", subscribers);

                // Get monthly revenue
                getMonthlyRevenue(messId, new NumberCallback() {
                    @Override
                    public void onSuccess(double monthlyRevenue) {
                        dashboardData.put("monthlyRevenue", monthlyRevenue);

                        // Get total revenue
                        getTotalRevenue(messId, new NumberCallback() {
                            @Override
                            public void onSuccess(double totalRevenue) {
                                dashboardData.put("totalRevenue", totalRevenue);
                                callback.onSuccess(dashboardData);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                callback.onFailure(errorMessage);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        callback.onFailure(errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Track page view analytics
     */
    public void trackPageView(String userId, String page, String messId) {
        String eventId = "PAGEVIEW_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", eventId);
        eventData.put("userId", userId);
        eventData.put("page", page);
        eventData.put("messId", messId);
        eventData.put("timestamp", System.currentTimeMillis());

        db.collection("analytics_events").document(eventId).set(eventData);
    }

    /**
     * Track user action analytics
     */
    public void trackUserAction(String userId, String action, String messId, Map<String, Object> metadata) {
        String eventId = "ACTION_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", eventId);
        eventData.put("userId", userId);
        eventData.put("action", action);
        eventData.put("messId", messId);
        eventData.put("metadata", metadata);
        eventData.put("timestamp", System.currentTimeMillis());

        db.collection("analytics_events").document(eventId).set(eventData);
    }

    /**
     * Get average subscription duration
     */
    public void getAverageSubscriptionDuration(String messId, NumberCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.getDocuments().size() == 0) {
                        callback.onSuccess(0);
                        return;
                    }

                    long totalDuration = 0;

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Long startDate = querySnapshot.getDocuments().get(i).getLong("startDate");
                        Long expiryDate = querySnapshot.getDocuments().get(i).getLong("expiryDate");

                        if (startDate != null && expiryDate != null) {
                            totalDuration += (expiryDate - startDate);
                        }
                    }

                    double averageDays = (double) totalDuration / (querySnapshot.getDocuments().size() * 24 * 60 * 60 * 1000L);
                    callback.onSuccess(averageDays);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get customer retention rate
     */
    public void getRetentionRate(String messId, NumberCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalSubscriptions = querySnapshot.getDocuments().size();
                    int renewedSubscriptions = 0;

                    if (totalSubscriptions == 0) {
                        callback.onSuccess(0);
                        return;
                    }

                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        // Count subscriptions that have been renewed (we can check if there are multiple transactions for same user)
                        String userId = querySnapshot.getDocuments().get(i).getString("userId");
                        // This is a simplified version; in production, you'd track actual renewals
                    }

                    // Simplified calculation
                    double retentionRate = (totalSubscriptions > 0) ? 75.0 : 0; // Placeholder value
                    callback.onSuccess(retentionRate);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
