package com.example.messapp.managers;

import com.example.messapp.models.Transaction;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PaymentManager handles simulated payment integration including:
 * - Processing subscription payments
 * - Recording transactions
 * - Simulating payment success/failure
 * - Generating payment receipts
 */
public class PaymentManager {
    private final FirebaseFirestore db;
    private final SubscriptionManager subscriptionManager;

    public interface PaymentCallback {
        void onSuccess(String transactionId);

        void onFailure(String errorMessage);
    }

    public interface TransactionListCallback {
        void onSuccess(List<Transaction> transactions);

        void onFailure(String errorMessage);
    }

    public PaymentManager() {
        this.db = FirebaseFirestore.getInstance();
        this.subscriptionManager = new SubscriptionManager();
    }

    /**
     * Simulate payment process for subscription renewal
     * In production, this would integrate with actual payment gateway (Stripe,
     * PayPal, etc.)
     */
    public void processPayment(String userId, String messId, double amount, int subscriptionDays,
            PaymentCallback callback) {
        String transactionId = "TXN_" + UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();

        // Simulate 95% success rate
        boolean paymentSuccessful = Math.random() < 0.95;

        if (paymentSuccessful) {
            // Record transaction
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("id", transactionId);
            transactionData.put("userId", userId);
            transactionData.put("messId", messId);
            transactionData.put("amount", amount);
            transactionData.put("daysGranted", subscriptionDays);
            transactionData.put("timestamp", timestamp);

            db.collection("transactions").document(transactionId).set(transactionData)
                    .addOnSuccessListener(aVoid -> {
                        // Create/update subscription
                        subscriptionManager.createSubscription(userId, messId, (amount / subscriptionDays),
                                subscriptionDays,
                                new SubscriptionManager.UpdateCallback() {
                                    @Override
                                    public void onSuccess() {
                                        callback.onSuccess(transactionId);
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        callback.onFailure(errorMessage);
                                    }
                                });
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("Payment failed. Please try again.");
        }
    }

    /**
     * Get transaction history for a user
     */
    public void getUserTransactions(String userId, TransactionListCallback callback) {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Transaction transaction = querySnapshot.getDocuments().get(i).toObject(Transaction.class);
                        if (transaction != null) {
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get transaction history for a mess (owner view)
     */
    public void getMessTransactions(String messId, TransactionListCallback callback) {
        db.collection("transactions")
                .whereEqualTo("messId", messId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        Transaction transaction = querySnapshot.getDocuments().get(i).toObject(Transaction.class);
                        if (transaction != null) {
                            transactions.add(transaction);
                        }
                    }
                    callback.onSuccess(transactions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Calculate total revenue for a mess
     */
    public void calculateMessRevenue(String messId, TransactionListCallback callback) {
        getMessTransactions(messId, new TransactionListCallback() {
            @Override
            public void onSuccess(java.util.List<com.example.messapp.models.Transaction> transactions) {
                double totalRevenue = 0;
                for (Transaction transaction : transactions) {
                    totalRevenue += transaction.getAmount();
                }
                // Return dummy transaction with total in amount field
                List<Transaction> result = new ArrayList<>();
                result.add(new Transaction(UUID.randomUUID().toString(), messId, "", totalRevenue, 0,
                        System.currentTimeMillis(), "BOTH"));
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    /**
     * Get active subscribers count for a mess
     */
    public void getMessSubscriberCount(String messId, TransactionListCallback callback) {
        db.collection("subscriptions")
                .whereEqualTo("messId", messId)
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Transaction> result = new ArrayList<>();
                    result.add(new Transaction(UUID.randomUUID().toString(), messId, "",
                            querySnapshot.getDocuments().size(), 0, System.currentTimeMillis(), "BOTH"));
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get transaction receipt
     */
    public void getTransactionReceipt(String transactionId, TransactionListCallback callback) {
        db.collection("transactions").document(transactionId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Transaction transaction = documentSnapshot.toObject(Transaction.class);
                        List<Transaction> result = new ArrayList<>();
                        if (transaction != null) {
                            result.add(transaction);
                        }
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure("Transaction not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
