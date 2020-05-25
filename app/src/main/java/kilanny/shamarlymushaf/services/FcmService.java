package kilanny.shamarlymushaf.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MessageTopicListActivity;
import kilanny.shamarlymushaf.data.User;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.data.msgs.ReceivedTopicMessage;
import kilanny.shamarlymushaf.tasks.UploadUserWorker;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class FcmService extends FirebaseMessagingService {

    private static final String TAG = "FcmService";

    public FcmService() {
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        String text = remoteMessage.getData().get("text");
        String from = remoteMessage.getFrom();
        if (from != null && text != null) {
            String topic = null, topicDisplay = null;
            if (from.endsWith(getString(R.string.dayAyahTopic))) {
                topic = getString(R.string.dayAyahTopic);
                topicDisplay = "آية اليوم";
            } else {
                String[] topics = getResources().getStringArray(R.array.topic_names);
                String[] names = getResources().getStringArray(R.array.topic_display_names);
                for (int i = 0; i < topics.length; ++i) {
                    if (from.endsWith(topics[i])) {
                        topic = topics[i];
                        topicDisplay = names[i];
                        break;
                    }
                }
            }
            if (topic != null) {
                Log.d(TAG, "Storing a message from topic " + topic + ": " + text);
                try {
                    FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(this);
                    db.receivedTopicMessageDao().insert(
                            new ReceivedTopicMessage(topic, text, new Date()));
                    if (db.topicDao().getNotify(topic))
                        notifyMessageReceived(topicDisplay, text);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    AnalyticsTrackers.getInstance(this).sendException("topic msg insertDb", ex);
                }
            }
        }
    }

    private void notifyMessageReceived(String topic, String text) {
        String channelId = "kilanny.shamarlymushaf.services.FcmService";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.createNotificationChannel(this, channelId,
                    "إشعارات مصحف الشمرلي");
        }

        Intent intent = new Intent(this, MessageTopicListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final int code = 2;
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                code, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String line = text.length() > 10 ? text.substring(0, 100) : text;
        String large = text.length() > 500 ? text.substring(0, 500) : text;
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("واحة الشمرلي - " + topic)
                .setContentText(line)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(large))
                .setContentIntent(pendingIntent)
                .build();
        NotificationManagerCompat.from(this).notify(code, notification);
    }

//    @Override
//    public void onDeletedMessages() {
//        super.onDeletedMessages();
//        Log.d(TAG, "onDeletedMessages");
//    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.i("FCM/onNewToken", "User token: " + s);

        User user = User.getInstance(this);
        user.fcmToken = s;
        user.isSent = false;
        user.save(this);

        WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(UploadUserWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build());
    }
}
