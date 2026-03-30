package com.example.messapp.models;

public class Transaction {
    private String id;
    private String messId;
    private String userId;
    private double amount;
    private int daysGranted;
    private long timestamp;
    private String subscriptionType; // "LUNCH", "DINNER", "BOTH"

    public Transaction() {
    }

    public Transaction(String id, String messId, String userId, double amount, int daysGranted, long timestamp,
            String subscriptionType) {
        this.id = id;
        this.messId = messId;
        this.userId = userId;
        this.amount = amount;
        this.daysGranted = daysGranted;
        this.timestamp = timestamp;
        this.subscriptionType = subscriptionType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Compatibility for old field name "transactionId"
    public String getTransactionId() {
        return id;
    }

    public void setTransactionId(String transactionId) {
        this.id = transactionId;
    }

    public String getMessId() {
        return messId;
    }

    public void setMessId(String messId) {
        this.messId = messId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getDaysGranted() {
        return daysGranted;
    }

    public void setDaysGranted(int daysGranted) {
        this.daysGranted = daysGranted;
    }

    // Compatibility for old field name "subscriptionDays"
    public int getSubscriptionDays() {
        return daysGranted;
    }

    public void setSubscriptionDays(int subscriptionDays) {
        this.daysGranted = subscriptionDays;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }
}
