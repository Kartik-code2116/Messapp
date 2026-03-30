package com.example.messapp.managers;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FirebaseNotificationManager handles FCM (Firebase Cloud Messaging) notifications including:
 * - Setting up notification channels
 * - Sending notifications to users
 * - Handling notification subscriptions/topics
 * - Managing notification preferences
 */
public class FirebaseNotificationManager {
    private final Context context;
    private final FirebaseFirestore db;
    private static final String NOTIFICATION_CHANNEL_ID = "MessApp_Notifications";
    private static final String NOTIFICATION_CHANNEL_NAME = "Mess App Notifications";

    public interface TokenCallback {
        void onSuccess(String token);
        void onFailure(String errorMessage);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public FirebaseNotificationManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    /**
     * Initialize and create notification channels
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = android.app.NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
            channel.setDescription("Notifications for Mess App activities");

            android.app.NotificationManager notificationManager = context.getSystemService(android.app.NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Get FCM token for the current device
     */
    public void getFCMToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    callback.onSuccess(token);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Subscribe user to mess notifications topic
     */
    public void subscribeToMessNotifications(String messId, UpdateCallback callback) {
        String topic = "mess_" + messId;
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Unsubscribe user from mess notifications topic
     */
    public void unsubscribeFromMessNotifications(String messId, UpdateCallback callback) {
        String topic = "mess_" + messId;
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Subscribe user to mess owner notifications topic
     */
    public void subscribeToMessOwnerNotifications(String messId, UpdateCallback callback) {
        String topic = "owner_" + messId;
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Save device token to Firestore
     */
    public void saveDeviceToken(String userId, String token, UpdateCallback callback) {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("fcmToken", token);
        tokenData.put("lastUpdated", System.currentTimeMillis());

        db.collection("users").document(userId).update(tokenData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Send test notification to user
     */
    public void sendTestNotification(String title, String message) {
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Log notification event for analytics
     */
    public void logNotificationEvent(String userId, String messId, String eventType, UpdateCallback callback) {
        String eventId = "EVENT_" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", eventId);
        eventData.put("userId", userId);
        eventData.put("messId", messId);
        eventData.put("eventType", eventType);
        eventData.put("timestamp", System.currentTimeMillis());

        db.collection("notification_events").document(eventId).set(eventData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Get notification preferences for user
     */
    public void getNotificationPreferences(String userId, PreferencesCallback callback) {
        db.collection("notification_preferences").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> preferences = documentSnapshot.getData();
                        callback.onSuccess(preferences);
                    } else {
                        callback.onSuccess(new HashMap<>()); // Return empty preferences
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Update notification preferences
     */
    public void updateNotificationPreferences(String userId, Map<String, Object> preferences, UpdateCallback callback) {
        db.collection("notification_preferences").document(userId).set(preferences)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface PreferencesCallback {
        void onSuccess(Map<String, Object> preferences);
        void onFailure(String errorMessage);
    }
}
