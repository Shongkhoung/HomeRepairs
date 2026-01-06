package com.example.homerepairs.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.homerepairs.ChatActivity;
import com.example.homerepairs.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Service to handle Firebase Cloud Messaging (FCM) push notifications
 */
public class NotificationService extends FirebaseMessagingService {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "homerepairs_messages";
    private static final String CHANNEL_NAME = "Messages";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleMessageData(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(remoteMessage.getNotification(), remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Send token to your server if needed
        // saveTokenToServer(token);
    }

    private void handleMessageData(Map<String, String> data) {
        String type = data.get("type");
        if ("new_message".equals(type)) {
            String conversationId = data.get("conversationId");
            String senderName = data.get("senderName");
            String messageText = data.get("messageText");

            if (conversationId != null && messageText != null) {
                showMessageNotification(conversationId, senderName, messageText);
            }
        }
    }

    private void handleNotification(RemoteMessage.Notification notification, Map<String, String> data) {
        String conversationId = data.get("conversationId");
        String title = notification.getTitle() != null ? notification.getTitle() : "New Message";
        String body = notification.getBody() != null ? notification.getBody() : "";

        if (conversationId != null) {
            showMessageNotification(conversationId, title, body);
        } else {
            // Generic notification
            showGenericNotification(title, body);
        }
    }

    private void showMessageNotification(String conversationId, String senderName, String messageText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create intent to open ChatActivity
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.no_profile_image) // Use no_profile_image fallback
                .setContentTitle(senderName != null ? senderName : "New Message")
                .setContentText(messageText)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageText));

        // Generate unique notification ID based on conversationId
        int notificationId = conversationId.hashCode();

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "Notification shown for conversation: " + conversationId);
        }
    }

    private void showGenericNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.no_profile_image)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for new messages");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
}
