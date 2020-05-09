package kilanny.shamarlymushaf.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.AppExecutors;

public class WorkerService extends Service {

    public static final String ACTION_BROADCAST_EVENTS = "workerServiceEvent";
    public static final String ARG_WORK_TYPE = "workType";
    public static final String ARG_IMPORT_NAME = "importName";
    public static final String ARG_IMPORT_FILENAME = "importFilename";
    public static final String ARG_IMPORT_FILE = "importFile";
    public static final int WORK_TYPE_IMPORT_RECITE = 1;

    private static final int NOTIFICATION_ID = 4109;
    private static final String CHANNEL_ID = "kilanny.shamarlymushaf.services.WorkerService";

    private final IBinder mBinder = new LocalBinder();
    private NotificationManager mNotificationMgr;
    private NotificationCompat.Builder mNotificationBuilder;
    private PowerManager.WakeLock mWakeLock;
    private int mProgress = 0;
    private String mMessage;
    private Handler mHandler;

    public int getProgress() {
        return mProgress;
    }

    public String getMessage() {
        return mMessage;
    }

    public WorkerService() {
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelName = "Shamarly Mushaf Worker Service";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLightColor(Color.YELLOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationMgr.createNotificationChannel(channel);
        return CHANNEL_ID;
    }

    private Notification initNotification(String title, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationBuilder = new NotificationCompat.Builder(this,
                    createNotificationChannel());
        } else {
            mNotificationBuilder = new NotificationCompat.Builder(this);
        }
        mMessage = message;
        return mNotificationBuilder
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(title)
                .setContentText(message)
                .setProgress(100, mProgress = 0, false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_launcher_64)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private synchronized void updateUi(int progress, @Nullable String message) {
        if (mProgress != progress || message != null && !mMessage.equals(message)) {
            mHandler.post(() -> {
                mNotificationBuilder.setProgress(100, progress, false);
                if (message != null) mNotificationBuilder.setContentText(message);
                mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.build());
                sendEventBroadcast();
            });
            mProgress = progress;
        }
    }

    private void sendEventBroadcast() {
        sendBroadcast(new Intent(ACTION_BROADCAST_EVENTS));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int workType = intent.getIntExtra(ARG_WORK_TYPE, 0);
        Runnable work = null;
        String title = null, message = null;
        boolean valid = false;
        switch (workType) {
            case WORK_TYPE_IMPORT_RECITE: {
                String name = intent.getStringExtra(ARG_IMPORT_NAME);
                String fileName = intent.getStringExtra(ARG_IMPORT_FILENAME);
                String file = intent.getStringExtra(ARG_IMPORT_FILE);
                if (name != null && fileName != null && file != null) {
                    valid = true;
                    title = "استيراد التلاوات";
                    message = "يتم استيراد: " + name;
                    work = () -> {
                    };
                }
            }
            break;
        }
        if (!valid) {
            Log.w(getClass().getName(), "Service is stopping since it was not given valid work arguments");
            releaseWakeLock();
            stopSelf();
            return START_NOT_STICKY;
        }

        mHandler = new Handler();
        mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(NOTIFICATION_ID, initNotification(title, message));

        AppExecutors.getInstance().executeOnCachedExecutor(work);

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        super.onDestroy();
    }

    private void releaseWakeLock() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void acquireWakeLock(long milli) {
        mWakeLock.acquire(milli);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        public WorkerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WorkerService.this;
        }
    }
}
