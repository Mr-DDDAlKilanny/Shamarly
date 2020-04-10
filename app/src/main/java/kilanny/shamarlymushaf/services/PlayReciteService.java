package kilanny.shamarlymushaf.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.activities.PlayReciteActivity;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.Shared;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class PlayReciteService extends Service {

    private static final int NOTIFICATION_ID = 6236 + 114;
    private static final int RELEASE_WAKELOCK = 0;
    private static final String ARG_IS_PAUSE_CLICK = "isPauseClick";
    private static final String ACTION_CLICK = "clickAction";
    public static final String ARG_AUTO_STOP_PERIOD_MINUTES_EXTRA = "autoStopPeriodMinutes";
    public static final String ARG_REPEAT_STRING_EXTRA = "repeatReciteDescStr";
    public static final String ACTION_BROADCAST_EVENTS = "playReciteServiceEvent";
    private static final String CHANNEL_ID = "kilanny.shamarlymushaf.services.PlayReciteService";

    private final BroadcastReceiver mNotificationBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Utils.isServiceRunning(context, PlayReciteService.class)
                    && intent.hasExtra(ARG_IS_PAUSE_CLICK)) {
                onClick(intent.getBooleanExtra(ARG_IS_PAUSE_CLICK, true));
            }
        }
    };
    private PhoneStateListener mPhoneStateListener;
    private final IBinder binder = new LocalBinder();
    private MediaPlayer mMediaPlayer;
    private SharedPreferences mPrefs;
    private int mFromSurah, mFromAyah, mToSurah, mToAyah;
    private int mSurah, mAyah;
    private long timerTickMillis;
    private long lastTimerTickMilliLeft;
    private CountDownTimer countDownTimer;
    private RemoteViews mRemoteViews;
    private NotificationManager mNotificationMgr;
    private NotificationCompat.Builder mNotificationBuilder;
    private PowerManager.WakeLock mWakeLock;
    private boolean mPaused, mIsPausing, mHasError, mIsLoading, mIsStopped, mImmediatePause;

    private boolean lastRecitedAyahWasFile = false;
    private String sessionId;

    public PlayReciteService() {
    }

    public int getCurrentSurah() {
        return mSurah;
    }

    public int getCurrentAyah() {
        return mAyah;
    }

    public long getLastTimerTickMilliLeft() {
        return lastTimerTickMilliLeft;
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public boolean isPaused() {
        return mPaused;
    }

    public boolean isStopped() {
        return mIsStopped;
    }

    public boolean hasError() {
        return mHasError;
    }

    public void nextAyah() {
        if (mPaused) {
            QuranData quranData = QuranData.getInstance(this);
            if (++mAyah > quranData.surahs[mSurah - 1].ayahCount) {
                mAyah = 1;
                if (++mSurah > quranData.surahs.length)
                    mSurah = 1;
            }
            updateUi();
        }
    }

    public void prevAyah() {
        if (mPaused) {
            if (--mAyah < 1) {
                QuranData quranData = QuranData.getInstance(this);
                if (--mSurah < 1)
                    mSurah = quranData.surahs.length;
                mAyah = quranData.surahs[mSurah - 1].ayahCount;
            }
            updateUi();
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelName = "Shamarly Mushaf Recite Playing Service";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLightColor(Color.YELLOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationMgr.createNotificationChannel(channel);
        return CHANNEL_ID;
    }

    private Notification initNotification(String surahName, String reciterName) {

        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(getApplicationContext());
        taskStackBuilder.addParentStack(PlayReciteActivity.class);
        taskStackBuilder.addNextIntent(getStartActivityIntent());
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(1,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationBuilder = new NotificationCompat.Builder(this,
                    createNotificationChannel());
        } else {
            mNotificationBuilder = new NotificationCompat.Builder(this);
        }
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.recite_notification_play);
        mRemoteViews.setTextViewText(R.id.txtSurahName, surahName);
        mRemoteViews.setTextViewText(R.id.txtReciteName, reciterName);
        Intent pauseIntent = new Intent(ACTION_CLICK);
        pauseIntent.putExtra(ARG_IS_PAUSE_CLICK, true);
        mRemoteViews.setOnClickPendingIntent(R.id.btnPauseRecite, PendingIntent.getBroadcast(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        Intent stopIntent = new Intent(ACTION_CLICK);
        stopIntent.putExtra(ARG_IS_PAUSE_CLICK, false);
        mRemoteViews.setOnClickPendingIntent(R.id.btnStopRecite, PendingIntent.getBroadcast(
                this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        return mNotificationBuilder
                .setContentIntent(pendingIntent)
                .setContent(mRemoteViews)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_launcher_64)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setFullScreenIntent(pendingIntent, true)
                .build();
    }

    private Intent getStartActivityIntent() {
        Intent intent = new Intent(this, PlayReciteActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_NO_HISTORY);
        return intent;
    }

    private void stopPlayback(boolean finish) {
        releaseWakeLock();
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (finish) {
            mIsStopped = true;
            unregisterReceiver(mNotificationBroadcastReceiver);
            if (mPhoneStateListener != null) {
                TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                if (telephony != null)
                    telephony.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener = null;
            }
            sendEventBroadcast();
            stopSelf();
        }
    }

    public String getSelectedSound() {
        return MainActivity.getSelectedSound(mPrefs, this);
    }

    private String getSelectedSoundName() {
        QuranData quranData = QuranData.getInstance(this);
        int idx = Utils.indexOf(quranData.reciterValues, getSelectedSound());
        if (idx >= 0)
            return quranData.reciterNames[idx];
        return null;
    }

    public void onClick(boolean isPauseClick) {
        if (isPauseClick) {
            pauseRecite(false);
        } else {
            stopPlayback(true);
        }
    }

    private void sendEventBroadcast() {
        sendBroadcast(new Intent(ACTION_BROADCAST_EVENTS));
    }

    private void pauseRecite(boolean immediately) {
        mImmediatePause = immediately;
        if (!mPaused) {
            mPaused = true;
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                if (immediately) {
                    mMediaPlayer.pause();
                } else {
                    mIsPausing = true;
                    Utils.createToast(this,
                            "تم إيقاف التلاوة. انتظر هذه الآية حتى تنتهي",
                            Toast.LENGTH_LONG, Gravity.CENTER).show();
                }
            } else
                stopPlayback(false);
            cancelAutoCloseTimer();
            mRemoteViews.setImageViewResource(R.id.btnPauseRecite, android.R.drawable.ic_media_play);
            sendEventBroadcast();
        } else {
            if (!immediately && mIsPausing)
                return;
            mPaused = false;
            mRemoteViews.setImageViewResource(R.id.btnPauseRecite, android.R.drawable.ic_media_pause);
            initAutoCloseTimer();
            if (immediately && mImmediatePause) {
                mImmediatePause = false;
                mMediaPlayer.start();
            } else
                startPlayback();
            sendEventBroadcast();
        }
        mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void startPlayback() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        final Shared attempt = new Shared();
        attempt.setData(1);
        mMediaPlayer.setOnCompletionListener(mp -> {
            try {
                QuranData quranData = QuranData.getInstance(this);
                AnalyticsTrackers.getInstance(this).sendListenReciteStats(sessionId,
                        getSelectedSound(), mSurah, mAyah, lastRecitedAyahWasFile, true);
                ++mAyah;
                if (mToSurah > 0 && mToAyah > 0) { // repeat choice
                    if (mSurah == mToSurah && mAyah > mToAyah) {
                        mSurah = mFromSurah;
                        mAyah = mFromAyah;
                    } else if (mAyah > quranData.surahs[mSurah - 1].ayahCount) {
                        mAyah = 1;
                        if (++mSurah > quranData.surahs.length)
                            mSurah = 1;
                    }
                } else if (mAyah > quranData.surahs[mSurah - 1].ayahCount) { // no repeat
                    mAyah = 1;
                    if (++mSurah > quranData.surahs.length) {
                        if (mPrefs.getBoolean("backToBegin", true)) {
                            mSurah = 1;
                        } else {
                            stopPlayback(false);
                            return;
                        }
                    }
                }
                mMediaPlayer.reset();
                if (mPaused) {
                    mIsPausing = false;
                    return;
                }
                final Uri path = Utils.getAyahPath(this,
                        getSelectedSound(),
                        mSurah, mAyah, quranData, attempt.getData());
                if (path == null || path.toString().startsWith("http") &&
                        Utils.isConnected(getApplicationContext()) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                    throw new IllegalStateException();
                mMediaPlayer.setDataSource(this, path);
                Runnable tmpRunnable = () -> {
                    if (mMediaPlayer == null) //stopPlayback() was called
                        return;
                    showProgress(true);
                    prepare();
                    lastRecitedAyahWasFile = !path.toString().startsWith("http");
                };
                if (mAyah == 1 && mSurah > 1 && mSurah != 9)
                    MainActivity.playBasmalah(this,
                            getSelectedSound(), quranData, tmpRunnable);
                else tmpRunnable.run();
            } catch (Exception e) {
                e.printStackTrace();
                showProgress(false);
                stopPlayback(false);
                updateUi("جهازك غير متصل / الخادم لا يستجيب");
                Utils.createToast(this,
                        "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                        Toast.LENGTH_SHORT, Gravity.CENTER).show();
            }
        });
        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            attempt.increment();
            if (!mPaused && attempt.getData() == 2) {
                QuranData quranData = QuranData.getInstance(this);
                Uri path = Utils.getAyahPath(this, getSelectedSound(),
                        mSurah, mAyah, quranData, 2);
                if (path != null) {
                    try {
                        if (path.toString().startsWith("http")
                                && Utils.isConnected(getApplicationContext()) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                            throw new IllegalStateException();
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(this, path);
                        mMediaPlayer.prepareAsync();
                        lastRecitedAyahWasFile = !path.toString().startsWith("http");
                        return true;
                    } catch (IOException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            } else if (mPaused)
                mIsPausing = false;
            showProgress(false);
            stopPlayback(false);
            updateUi("جهازك غير متصل / الخادم لا يستجيب");
            Utils.createToast(this,
                    "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                    Toast.LENGTH_SHORT, Gravity.CENTER).show();
            return true;
        });
        mMediaPlayer.setOnPreparedListener(mp -> {
            if (mMediaPlayer != null && !mPaused) { //user closed/cancelled before prepare completes
                updateUi();
                showProgress(false);
                acquireWakeLock(180000);
                mMediaPlayer.start();
            } else if (mPaused)
                mIsPausing = false;
        });
        try {
            QuranData quranData = QuranData.getInstance(this);
            final Uri path = Utils.getAyahPath(this, getSelectedSound(),
                    mSurah, mAyah, quranData, 1);
            if (path == null || path.toString().startsWith("http") &&
                    Utils.isConnected(getApplicationContext()) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                throw new IllegalStateException();
            mMediaPlayer.setDataSource(this, path);
            Runnable tmpRunnable = () -> {
                if (mMediaPlayer == null) //stopPlayback() was called
                    return;
                lastRecitedAyahWasFile = !path.toString().startsWith("http");;
                prepare();
            };
            if (mAyah == 1 && mSurah > 1 && mSurah != 9)
                MainActivity.playBasmalah(this, getSelectedSound(), quranData, tmpRunnable);
            else tmpRunnable.run();
        } catch (Exception e) {
            e.printStackTrace();
            showProgress(false);
            stopPlayback(false);
            updateUi("جهازك غير متصل / الخادم لا يستجيب");
            Utils.createToast(this,
                    "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                    Toast.LENGTH_SHORT, Gravity.CENTER).show();
        }
    }

    private void updateUi() {
        updateUi(null);
    }

    private void updateUi(String errorMessage) {
        if (mSurah == 0 || mAyah == 0)
            return;
        QuranData quranData = QuranData.getInstance(this);
        mRemoteViews.setTextViewText(R.id.txtSurahName,
                "سورة " + quranData.surahs[mSurah - 1].name);

        mHasError = errorMessage != null;
        if (errorMessage != null) {
            mRemoteViews.setTextViewText(R.id.txtError, errorMessage);
            mRemoteViews.setViewVisibility(R.id.txtError, View.VISIBLE);
        } else
            mRemoteViews.setViewVisibility(R.id.txtError, View.GONE);
        mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    private void showProgress(boolean visible) {
        mIsLoading = visible;
        mRemoteViews.setViewVisibility(R.id.progressBarLoadingRecite,
                visible ? View.VISIBLE : View.GONE);
        mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.build());
        sendEventBroadcast();
    }

    private void prepare() {
        acquireWakeLock(60000);
        try {
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException ignored) {
            showProgress(false);
            stopPlayback(false);
            updateUi("جهازك غير متصل / الخادم لا يستجيب");
            Utils.createToast(this,
                    "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                    Toast.LENGTH_SHORT, Gravity.CENTER).show();
        }
    }

    private void cancelAutoCloseTimer() {
        if (countDownTimer != null) {
            try {
                countDownTimer.cancel();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            countDownTimer = null;
        }
    }

    private void initAutoCloseTimer() {
        cancelAutoCloseTimer();
        if (lastTimerTickMilliLeft > 0) {
            countDownTimer = new CountDownTimer(lastTimerTickMilliLeft, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    lastTimerTickMilliLeft = millisUntilFinished;
                    sendEventBroadcast();
                }

                @Override
                public void onFinish() {
                    cancelAutoCloseTimer();
                    pauseRecite(false);
                    lastTimerTickMilliLeft = timerTickMillis;
                }
            }.start();
        }
    }

    public void onGrantedPhoneStatePermission() {
        if (canListenPhoneState() && mPhoneStateListener == null) {
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony == null)
                return;
            telephony.listen(mPhoneStateListener = new PhoneStateListener() {

                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    super.onCallStateChanged(state, phoneNumber);
                    if (!Utils.isServiceRunning(getApplicationContext(), PlayReciteService.class))
                        return;
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE:
                            Log.d("DEBUG", "IDLE");
                            //phoneRinging = false;
                            if (mPaused && mImmediatePause) {
                                pauseRecite(true);
                            }
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            Log.d("DEBUG", "RINGING");
                            //phoneRinging = true;
                            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                                pauseRecite(true);
                            }
                            break;
                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private boolean canListenPhoneState() {
        return getSystemService(Context.TELEPHONY_SERVICE) != null &&
                //(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                        == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        QuranData quranData = QuranData.getInstance(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastTimerTickMilliLeft = timerTickMillis = 60 * 1000 *
                intent.getIntExtra(ARG_AUTO_STOP_PERIOD_MINUTES_EXTRA, 0);
        initAutoCloseTimer();
        String stringExtra = intent.getStringExtra(ARG_REPEAT_STRING_EXTRA);
        if (stringExtra != null) {
            Pattern p = Pattern.compile("(\\d+):(\\d+)-(\\d+):(\\d+)");
            Matcher matcher = p.matcher(stringExtra);
            if (matcher.matches()) {
                mFromSurah = Integer.parseInt(matcher.group(1));
                mFromAyah = Integer.parseInt(matcher.group(2));
                mToSurah = Integer.parseInt(matcher.group(3));
                mToAyah = Integer.parseInt(matcher.group(4));
            }
        }
        if (mFromSurah == 0)
            mFromSurah = 1;
        mSurah = mFromSurah;
        mAyah = mFromAyah;
        sessionId = Utils.newUid();

        mNotificationMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(NOTIFICATION_ID, initNotification(quranData.surahs[mSurah - 1].name,
                getSelectedSoundName()));
        registerReceiver(mNotificationBroadcastReceiver, new IntentFilter(ACTION_CLICK));

        startPlayback();
        onGrantedPhoneStatePermission();
        return START_REDELIVER_INTENT;
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
        return binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        public PlayReciteService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayReciteService.this;
        }
    }
}
