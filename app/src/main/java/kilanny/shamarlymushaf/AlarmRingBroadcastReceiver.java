package kilanny.shamarlymushaf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import kilanny.shamarlymushaf.activities.WelcomeActivity;
import kilanny.shamarlymushaf.data.alarms.AlarmDb;
import kilanny.shamarlymushaf.util.Utils;

public class AlarmRingBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent = new Intent(context, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String channelId = "kilanny.shamarlymushaf.AlarmRingBroadcastReceiver";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Utils.createNotificationChannel(context, channelId,
                    "إشعارات الورد - مصحف الشمرلي");
        }
        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("قراءة الورد")
                .setContentText("حان الوقت لقراءة ورد القرآن الكريم!")
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLights(context.getResources().getColor(R.color.color_preloader_center), 500, 1000)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        NotificationManagerCompat.from(context).notify(1, notification);

        Utils.scheduleAndDeletePrevious(context,
                AlarmDb.getInstance(context).alarmDao().getAllEnabled());
    }
}
