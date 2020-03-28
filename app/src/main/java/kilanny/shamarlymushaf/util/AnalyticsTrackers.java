package kilanny.shamarlymushaf.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

//import com.amplitude.api.Amplitude;
//import com.amplitude.api.AmplitudeClient;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import kilanny.shamarlymushaf.App;
import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.data.AnalyticData;
import kilanny.shamarlymushaf.data.UserInfo;

public final class AnalyticsTrackers {

    //private static AmplitudeClient instance;
//    private static AtomicBoolean sending = new AtomicBoolean(false);
    private static Executor executor = new ThreadPoolExecutor(1, // core size
            2, // max size
            10*60, // idle timeout
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3)); // queue with a size;

//    private static AmplitudeClient getInstance(Context context) {
//        if (instance == null) {
//            instance = Amplitude.getInstance();
//            UserInfo userInfo = UserInfo.getInstance(context);
//            JSONObject jsonObject = new JSONObject();
//            try {
//                jsonObject.put("appInstanceId", userInfo.appInstanceId);
//                jsonObject.put("deviceName", userInfo.deviceName);
//                jsonObject.put("deviceModel", userInfo.deviceModel);
//                jsonObject.put("deviceOsVersion", userInfo.deviceOsVersion);
//                jsonObject.put("deviceProduct", userInfo.deviceProduct);
//                jsonObject.put("deviceSdkVersion", userInfo.deviceSdkVersion);
//                jsonObject.put("appVersionName", userInfo.appVersionName);
//                jsonObject.put("appVersionCode", userInfo.appVersionCode);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            instance.initialize(context, "2dc4b3f0d0704ece0e44623c1bfc5bce")
//                    .setUserId(userInfo.appInstanceId)
//                    .enableForegroundTracking(App.get(context))
//                    .setUserProperties(jsonObject);
//        }
//        return instance;
//    }

    public static String getDeviceInfo(Context context) {
        long vmHead = -1, recomendHeap = -1, totalMem = -1,
                freeMemory = -1, processors = -1;
        try {
            freeMemory = Runtime.getRuntime().freeMemory();
            processors = Runtime.getRuntime().availableProcessors();
            vmHead = Runtime.getRuntime().maxMemory();
            totalMem = Runtime.getRuntime().totalMemory();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                recomendHeap = am.getMemoryClass();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return String.format(Locale.ENGLISH,
                "kilanny.shamarlymushaf.App [Version: %s, code: %d]\nOS Version: %s\nAPI Level: %d\nDevice: %s\nModel: %s\nProduct: %s\n" +
                        "Recommended Heap: %d\nFree Memory: %d\nProcessor Count: %d\nVM Heap size: %d\n" +
                        "Total Memory: %d",
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                System.getProperty("os.version"), // OS version
                Build.VERSION.SDK_INT,      // API Level
                Build.DEVICE,           // Device
                Build.MODEL,            // Model
                Build.PRODUCT,          // Product,
                recomendHeap, freeMemory, processors, vmHead, totalMem
        );
    }

    public static void sendException(Context context, String title, Throwable throwable) {
        try {
            String payload = String.format(
                    "{\"th\":\"%s\",\"t\":\"%s\",\"e\":\"%s\"}",
                    Thread.currentThread().getName(), title, throwable.getMessage());
            addEvent("exception", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendFatalError(Context context, String title, String message) {
        try {
            String payload = String.format(
                    "{\"th\":\"%s\",\"t\":\"%s\",\"m\":\"%s\"}",
                    Thread.currentThread().getName(), title, message);
            addEvent("exception", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendPageReadStats(Context context, String sessionId,
                                         HashSet<Integer> pages, long timeMs) {
        try {
            Object[] arr = pages.toArray();
            String payload = String.format(Locale.US,
                    "{\"sId\":\"%s\",\"pages\":%s,\"timeMs\":%d}",
                    sessionId, Arrays.toString(arr), timeMs);
            addEvent("readPages", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendTafseerStats(Context context, String sessionId,
                                        HashSet<String> strings) {
        try {
            Object[] arr = strings.toArray();
            String payload = String.format("{\"sId\":\"%s\",\"dtl\":%s}",
                    sessionId, Arrays.toString(arr));
            addEvent("viewTafseer", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendListenReciteStats(Context context, String sessionId,
                                             HashSet<String> strings) {
        try {
            Object[] arr = strings.toArray();
            String payload = String.format("{\"sId\":\"%s\",\"dtl\":%s}",
                    sessionId, Arrays.toString(arr));
            addEvent("listenRecites", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendForegroundListenReciteStats(Context context, String reciter, HashSet<String> strings) {
        try {
            Object[] arr = strings.toArray();
            String payload = String.format("{\"r\":\"%s\",\"s\":%s}",
                    reciter, Arrays.toString(arr));
            addEvent("listenForegroundRecites", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    public static boolean sendComment(Activity context, String comment) {
////        if (isPlayServicesUnavailable(context)) {
////            showUpdatePlayServicesNotification(context);
////            return false;
////        }
////        try {
////            if (sInstance == null) initialize(context);
////            getInstance().get(Target.APP)
////                    .send(new HitBuilders.EventBuilder("User Interactions", "Comment")
////                            .setLabel(comment + "\n" + getDeviceInfo())
////                            .build());
////            return true;
////        } catch (Exception ex) {
////            ex.printStackTrace();
////            return false;
////        }
//        context.startActivity(new Intent(context, ReportIssueActivity.class));
//        return true;
//    }

    public static void sendDownloadPages(Context context) {
        try {
            String payload = "{\"e\":0}";
            addEvent("downloadPages", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendDownloadRecites(Context context,
                                           String reciter, HashSet<Integer> surahs) {
        try {
            String payload = String.format("{\"r\":\"%s\",\"s\":%s}",
                    reciter, Arrays.toString(surahs.toArray()));
            addEvent("downloadRecites", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendDownloadRecites(Context context,
                                           String reciter, int surah) {
        try {
            String payload = String.format(Locale.US,
                    "{\"r\":\"%s\",\"s\":%d}", reciter, surah);
            addEvent("downloadSuraRecites", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void addEvent(final String evtName, final String payload, final Context context) {
        executor.execute(() -> {
            AnalyticData event = new AnalyticData();
            event.eventName = evtName;
            event.date = new Date();
            event.payload = payload;
            AnalyticData.Queue.enqueueMany(context, event);
        });
    }

    private static boolean sendUserInfo(Context context) {
        UserInfo userInfo = UserInfo.getInstance(context);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.US);
        String gender = userInfo.gender == null ? "" : userInfo.gender ? "1" : "2";
        TimeZone timeZone = TimeZone.getDefault();
        String json = formatJson("{\n" +
                        "\t\"app_code\": \"%s\", \"app_ver\": \"%s\", \"os_ver\": \"%s\", \n" +
                        "\t\"sdk_ver\":\"%s\", \"dev_name\": \"%s\", \"dev_model\": \"%s\", \"dev_prod\": \"%s\", \"age\": \"%s\", \n" +
                        "\t\"gender\": \"%s\", \"location\": \"%s\", \"lastUpdate\":\"%s\",\"tz\":\"%s\"\n" +
                        "}",
                userInfo.appVersionCode, userInfo.appVersionName, userInfo.deviceOsVersion,
                userInfo.deviceSdkVersion, userInfo.deviceName, userInfo.deviceModel,
                userInfo.deviceProduct, userInfo.ageCategory + "", gender,
                userInfo.locationCategory, sdf.format(new Date()),
                timeZone.getDisplayName(false, TimeZone.SHORT) + "_" + timeZone.getID()
        );
        json = json.replace("\t", "")
                .replace(", \n", ",")
                .replace("\n", "")
                .replace("\": \"", "\":\"");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        String url = String.format("https://shamarlymushaf.firebaseio.com/analytics/%s/info.json",
                userInfo.appInstanceId);
        String put = Utils.sendHttpRequest(url, "PUT", json, headers);
        try {
            JSONObject jsonObject = new JSONObject(put);
            return jsonObject.getString("app_code").equals(userInfo.appVersionCode);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static void send(final Context context) {
        Constraints constraints = new Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest saveRequest = new PeriodicWorkRequest
                .Builder(UploadLogsWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UploadLogsWorker.class.getName(),
                ExistingPeriodicWorkPolicy.REPLACE, saveRequest);
    }

    private static String formatJson(String format, Object ...args) {
        for (int i = 0; i < args.length; ++i) {
            if (args[i] instanceof String) {
                String s = (String) args[i];
                args[i] = s.replace("null", "")
                        .replace("\"", "'");
            }
        }
        return String.format(Locale.US, format, args);
    }

    public static void sendWidgetEnabled(Context context, boolean enabled) {
        try {
            String payload = "{\"e\":" + enabled + "}";
            addEvent("widgetEnabled", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendWidgetRefresh(Context context, int sura, int ayah) {
        try {
            String payload = String.format(Locale.US,
                    "{\"s\":%d,\"a\":%d}", sura, ayah);
            addEvent("widgetRefresh", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendMaqraahResponse(Context context, int value) {
        try {
            String payload = String.format(Locale.US, "{\"v\":%d}", value);
            addEvent("maqraahResponse", payload, context);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class UploadLogsWorker extends Worker {

        public UploadLogsWorker(
                @NonNull Context context,
                @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Integer tmpSz = AnalyticData.Queue.cachedSize();
            if (tmpSz != null && tmpSz == 0)
                return Result.success();
            if (Utils.isConnected(getApplicationContext()) != Utils.CONNECTION_STATUS_CONNECTED)
                return Result.retry();
            int sz = AnalyticData.Queue.size(getApplicationContext());
            UserInfo userInfo = UserInfo.getInstance(getApplicationContext());
            boolean hasChanged = userInfo.hasChanged != null && userInfo.hasChanged;
            AnalyticsTrackersData trackersData = AnalyticsTrackersData.getInstance(getApplicationContext());
            if (trackersData.getLastSend() == null || hasChanged) {
                if (sendUserInfo(getApplicationContext())) {
                    trackersData.setLastSend(new Date(), getApplicationContext());
                    userInfo.hasChanged = false;
                    userInfo.save(getApplicationContext());
                } else {
                    return Result.retry();
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US);
            for (int i = 0; i < 10 && sz > 0; ++i, --sz) {
                AnalyticData sendData = AnalyticData.Queue.dequeueMany(getApplicationContext(), 1)[0];
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=UTF-8");
                try {
                    String json = formatJson("{\n" +
                            "\t\"av\": \"%s\", \"evt\": \"%s\", \"time\": \"%s\", #####\n" +
                            "}", userInfo.appVersionCode, sendData.eventName, sdf.format(sendData.date));
                    json = json.replace("\t", "")
                            .replace(", \n", ",")
                            .replace("\n", "")
                            .replace("\": \"", "\":\"");
                    String payload = sendData.payload;
                    payload = payload.substring(payload.indexOf('{') + 1);
                    payload = payload.substring(0, payload.lastIndexOf('}'));
                    json = json.replace("#####", payload);
                    String url = String.format(
                            "https://shamarlymushaf.firebaseio.com/analytics/%s/events/%s.json",
                            userInfo.appInstanceId, sdf.format(new Date()));
                    String res = Utils.sendHttpRequest(url, "PUT", json, headers);
                    if (res.contains("{")) {
                        trackersData.setLastSend(new Date(), getApplicationContext());
                        Log.d("AnalTrack/send", "Successfully sent data: " + json);
                    } else {
                        AnalyticData.Queue.enqueueMany(getApplicationContext(), sendData);
                        Log.d("AnalTrack/send", "Fail sending data: " + json);
                        break;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AnalyticData.Queue.enqueueMany(getApplicationContext(), sendData);
                    break;
                }
            }
            return Result.success();
        }
    }
}

class AnalyticsTrackersData implements Serializable {

    static final long serialVersionUID = 2L;
    private static AnalyticsTrackersData instance;
    private static final String settingFilename = "AnalyticsTrackersData.dat";

    private Date lastSend;

    private AnalyticsTrackersData() {
    }

    public Date getLastSend() {
        return lastSend;
    }

    public void setLastSend(Date lastSend, Context context) {
        this.lastSend = lastSend;
        save(context);
    }

    private void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(settingFilename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AnalyticsTrackersData getInstance(Context context) {
        if (instance != null)
            return instance;
        try {
            FileInputStream fis = context.openFileInput(settingFilename);
            ObjectInputStream is = new ObjectInputStream(fis);
            instance = (AnalyticsTrackersData) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (instance == null)
            instance = new AnalyticsTrackersData();
        return instance;
    }
}