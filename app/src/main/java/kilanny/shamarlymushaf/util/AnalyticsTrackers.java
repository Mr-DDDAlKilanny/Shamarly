package kilanny.shamarlymushaf.util;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import kilanny.shamarlymushaf.data.alarms.Alarm;

public final class AnalyticsTrackers {

    private static AnalyticsTrackers instance;

    public static AnalyticsTrackers getInstance(Context context) {
        if (instance == null)
            instance = new AnalyticsTrackers(context);
        return instance;
    }

    private FirebaseAnalytics mFirebaseAnalytics;

    private AnalyticsTrackers(Context context) {
        if (Utils.isGooglePlayServicesAvailable(context)) {
            try {
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean canMakeAnalytics() {
        return mFirebaseAnalytics != null;
    }

    public void sendException(String title, Throwable throwable) {
        if (!canMakeAnalytics()) return;
        try {
            FirebaseCrashlytics.getInstance().log("Non-Fatal: " + title + " - "
                    + Thread.currentThread().getName());
            FirebaseCrashlytics.getInstance().recordException(throwable);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendFatalError(String title, String message) {
        if (!canMakeAnalytics()) return;
        try {
            FirebaseCrashlytics.getInstance().log("Fatal: " + title + " - "
                    + Thread.currentThread().getName() + " - " + message);
            FirebaseCrashlytics.getInstance().recordException(new Exception(message));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendRefreshDownloads(boolean isUri) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isUri", isUri);
            mFirebaseAnalytics.logEvent("RefreshDownloads", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendChooceDir(boolean isUri) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isUri", isUri);
            mFirebaseAnalytics.logEvent("ChooceDir", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendImportRecite(String recite, boolean isUri) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("recite", recite);
            bundle.putBoolean("isUri", isUri);
            mFirebaseAnalytics.logEvent("ImportRecite", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendImportReciteFail(String recite, boolean isUri, String exMessage) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("recite", recite);
            bundle.putString("exMessage", exMessage);
            bundle.putBoolean("isUri", isUri);
            mFirebaseAnalytics.logEvent("ImportReciteFail", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendPageReadStats(String sessionId, int page, int timeMs) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("page", page);
            bundle.putInt("timeMs", timeMs);
            bundle.putString("sessionId", sessionId);
            mFirebaseAnalytics.logEvent("PageRead", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendTafseerStats(String sessionId, int tafseerId, int sura, int ayah) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("tafseerId", tafseerId);
            bundle.putInt("sura", sura);
            bundle.putInt("ayah", ayah);
            bundle.putString("sessionId", sessionId);
            mFirebaseAnalytics.logEvent("TafseerRead", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDownloadTafaseerStart() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DownloadTafaseerStart", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDownloadTafaseerSuccess() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DownloadTafaseerSuccess", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDeleteTafaseer() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DeleteTafaseer", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendListenReciteStats(String sessionId, String reciter, int sura, int ayah,
                                      boolean isLocalFile, boolean isBackground) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("reciter", reciter);
            bundle.putInt("sura", sura);
            bundle.putInt("ayah", ayah);
            bundle.putString("sessionId", sessionId);
            bundle.putBoolean("isLocalFile", isLocalFile);
            bundle.putBoolean("isBackground", isBackground);
            mFirebaseAnalytics.logEvent("ListenRecite", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDownloadPages() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DownloadPages", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDownloadRecites(String reciter, int sura) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("reciter", reciter);
            bundle.putInt("sura", sura);
            mFirebaseAnalytics.logEvent("DownloadRecites", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendDeleteRecites(String reciter) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("reciter", reciter);
            mFirebaseAnalytics.logEvent("DeleteRecites", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendWidgetEnabled(boolean enabled) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent(enabled ? "WidgetEnabled" : "WidgetDisabled", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendWidgetRefresh(int sura, int ayah) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("sura", sura);
            bundle.putInt("ayah", ayah);
            mFirebaseAnalytics.logEvent("WidgetRefresh", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMaqraahResponse(int value) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("value", value);
            mFirebaseAnalytics.logEvent("MaqraahResponse", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logVideoOpened(String videoId, int source) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("id", videoId);
            bundle.putInt("source", source);
            mFirebaseAnalytics.logEvent("VideoOpened", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logTopicSubscribed(String topic) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("topic", topic);
            mFirebaseAnalytics.logEvent("TopicSubscribed", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logMessagesRead(String topic, int readCount) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("topic", topic);
            bundle.putInt("readCount", readCount);
            mFirebaseAnalytics.logEvent("MessagesRead", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logTopicUnsubscribed(String topic) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putString("topic", topic);
            mFirebaseAnalytics.logEvent("TopicUnsubscribed", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logAlarmDeleted(Alarm alarm) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            putAlarm(alarm, bundle);
            mFirebaseAnalytics.logEvent("AlarmDeleted", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void putAlarm(Alarm alarm, Bundle bundle) {
        bundle.putBoolean("alarm_enabled", alarm.enabled);
        bundle.putInt("alarm_id", alarm.id);
        bundle.putInt("alarm_weekDayFlags", alarm.weekDayFlags);
    }

    public void logModifyAlarm(Alarm alarm, boolean isNew) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            putAlarm(alarm, bundle);
            bundle.putBoolean("isNew", isNew);
            mFirebaseAnalytics.logEvent("AlarmModified", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}