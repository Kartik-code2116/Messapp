package com.example.messapp.managers;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MessNotificationManager {
    public static final String COLLECTION = "mess_notifications";
    public static final String TYPE_ADMIN_MESSAGE = "ADMIN_MESSAGE";
    public static final String TYPE_SUBSCRIPTION_GRANTED = "SUBSCRIPTION_GRANTED";

    private MessNotificationManager() {
    }

    public static void sendAdminMessage(String messId, String senderId, String senderName,
            String title, String message, Runnable onSuccess,
            com.google.android.gms.tasks.OnFailureListener onFailure) {
        Map<String, Object> data = baseNotification(messId, senderId, senderName,
                TYPE_ADMIN_MESSAGE, title, message);
        FirebaseFirestore.getInstance().collection(COLLECTION).add(data)
                .addOnSuccessListener(documentReference -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                })
                .addOnFailureListener(onFailure);
    }

    public static void sendSubscriptionGranted(String messId, String senderId, String targetUserId,
            String mealType, int days, long expiryDate) {
        String label = formatMealType(mealType);
        Map<String, Object> data = baseNotification(messId, senderId, "Mess Admin",
                TYPE_SUBSCRIPTION_GRANTED, "Subscription Granted",
                label + " subscription credited for " + days + " days.");
        data.put("targetUserId", targetUserId);
        data.put("mealType", mealType);
        data.put("expiryDate", expiryDate);
        FirebaseFirestore.getInstance().collection(COLLECTION).add(data);
    }

    private static Map<String, Object> baseNotification(String messId, String senderId,
            String senderName, String type, String title, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("messId", messId);
        data.put("senderId", senderId);
        data.put("senderName", senderName);
        data.put("type", type);
        data.put("title", title);
        data.put("message", message);
        data.put("createdAt", System.currentTimeMillis());
        return data;
    }

    private static String formatMealType(String mealType) {
        if ("LUNCH".equals(mealType)) {
            return "Lunch";
        }
        if ("DINNER".equals(mealType)) {
            return "Dinner";
        }
        if ("ONE_TIME".equals(mealType)) {
            return "One Time a Day";
        }
        return "Lunch and dinner";
    }
}
