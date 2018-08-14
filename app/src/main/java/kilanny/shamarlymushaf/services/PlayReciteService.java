package kilanny.shamarlymushaf.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MainActivity;

public class PlayReciteService extends Service {
    public static final int MESSAGE_EXIT = -1234567;
    static final int NOTIFICATION_ID = 19920513;
    private static PlayReciteService _lastInstance;
    private static final Class<?>[] mSetForegroundSignature = new Class[] {
            boolean.class};
    private static final Class<?>[] mStartForegroundSignature = new Class[] {
            int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {
            boolean.class};

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_EXIT:
                    myStopSelf();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private NotificationManager mNM;
    private Notification.Builder notificationBuilder;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Handler runOnUiThreadHandler;
    private int lastServiceStartId = -1;
    private RemoteViews notificationRemoteViews;

    public PlayReciteService() {
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {

            lastServiceStartId = msg.arg1;
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private void myStopSelf() {
        if (lastServiceStartId != -1) {
            int tmp = lastServiceStartId;
            lastServiceStartId = -1;
            stopSelf(tmp);
        }
    }

    private void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        } catch (IllegalAccessException e) {
            // Should not happen.
            Log.w("ApiDemos", "Unable to invoke method", e);
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    private void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    private void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }

    public static class StopButtonListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (_lastInstance != null) {
                //_lastInstance.stopAutoCall(true);
            }
            Toast.makeText(context, "Stopping...", Toast.LENGTH_SHORT).show();
        }
    }

    private void runOnUiThread(Runnable runnable) {
        runOnUiThreadHandler.post(runnable);
    }

    private void initNotification() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notificationRemoteViews = new RemoteViews(getPackageName(), R.layout.inbackground_notification);

        //this is the intent that is supposed to be called when the button is clicked
        Intent stopIntent = new Intent(this, StopButtonListener.class);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this,
                0, stopIntent, 0);
        //notificationRemoteViews.setOnClickPendingIntent(R.id.btnStopCalls, pendingStopIntent);
        notificationBuilder = new Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setContent(notificationRemoteViews)
                .setSmallIcon(R.mipmap.ic_launcher);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
            return;
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = getClass().getMethod("setForeground",
                    mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "OS doesn't have Service.startForeground OR Service.setForeground!");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        _lastInstance = this;
        runOnUiThreadHandler = new Handler();

        initNotification();
        startForegroundCompat(NOTIFICATION_ID, notificationBuilder.getNotification());

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

    }

    private void updateNotificationTextView(int txtView, String text) {
        notificationRemoteViews.setTextViewText(txtView, text);
        mNM.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        int callListId = intent.getIntExtra("callListId", -1);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure our notification is gone.
        stopForegroundCompat(NOTIFICATION_ID);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
