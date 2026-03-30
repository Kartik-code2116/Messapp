package com.example.messapp.models;

public class Subscription {
    private String subscriptionId;
    private String userId;
    private String messId;
    private long startDate;
    private long expiryDate;
    private String status; // "ACTIVE", "EXPIRED", "CANCELLED"
    private double monthlyPrice;
    private String type; // "LUNCH", "DINNER", "BOTH"

    public Subscription() {
        // Required no-argument constructor for Firebase
    }

    public Subscription(String subscriptionId, String userId, String messId, long startDate, long expiryDate,
            String status, double monthlyPrice, String type) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.messId = messId;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.monthlyPrice = monthlyPrice;
        this.type = type;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(double monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status) && System.currentTimeMillis() < expiryDate;
    }
}
