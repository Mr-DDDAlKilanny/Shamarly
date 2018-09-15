package kilanny.shamarlymushaf.util;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.data.AnalyticData;
import kilanny.shamarlymushaf.data.UserInfo;

public final class AnalyticsTrackers {

    private static AtomicReference<Date> isUpdatingCookie = new AtomicReference<>();
    private static AtomicBoolean sending = new AtomicBoolean(false);

    public static void updateWebCookie(final Context context) {
        Date now = new Date();
        if (isUpdatingCookie.get() != null
                && now.getTime() - isUpdatingCookie.get().getTime() < 2 * 60 * 1000) return;
        isUpdatingCookie.set(now);
        Log.d("updateWebCookie", "Starting");
        if (Utils.isConnected(context) != Utils.CONNECTION_STATUS_CONNECTED) {
            Log.w("updateWebCookie", "No internet connection; skipping operation");
            isUpdatingCookie.set(null);
            return;
        }
        new Handler(context.getMainLooper()).post(new Runnable() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void run() {
                Log.d("updateWebCookie", "Starting in UI");
                final WebView webView = new WebView(context);
                WebViewClient webViewClient = new WebViewClient() {

                    private void onDone() {
                        isUpdatingCookie.set(null);
                        webView.destroy();
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        try {
                            String[] cookies = CookieManager.getInstance().getCookie(url).split(";");
                            String c = "";
                            for (String s : cookies)
                                if (s.startsWith("__test="))
                                    c = s;
                            Log.i("updateWebCookie", String.format("Found cookie: %s", c));
                            AnalyticsTrackersData trackersData = AnalyticsTrackersData
                                    .getInstance(context);
                            trackersData.setCookie(c, context);
                            send(context);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        onDone();
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        Log.e("updateWebCookie", error.toString());
                        super.onReceivedError(view, request, error);
                        onDone();
                    }

                    @Override
                    public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                    WebResourceResponse errorResponse) {
                        Log.e("updateWebCookie", errorResponse.toString());
                        super.onReceivedHttpError(view, request, errorResponse);
                        onDone();
                    }

                    @Override
                    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                        Log.e("updateWebCookie", error.toString());
                        super.onReceivedSslError(view, handler, error);
                        onDone();
                    }
                };
                webView.setWebViewClient(webViewClient);
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webView.loadUrl("http://shmrl.ihostfull.com");
            }
        });
    }

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
                "App [Version: %s, code: %d]\nOS Version: %s\nAPI Level: %d\nDevice: %s\nModel: %s\nProduct: %s\n" +
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

    public static void sendForegroundListenReciteStats(Context context, HashSet<String> strings) {
        try {
            Object[] arr = strings.toArray();
            String payload = Arrays.toString(arr);
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

    private static void addEvent(String evtName, String payload, Context context) {
        AnalyticData event = new AnalyticData();
        event.eventName = evtName;
        event.date = new Date();
        event.payload = payload;
        AnalyticData.Queue.enqueueMany(context, event);
    }

    @Nullable
    private static AnalyticData[] getSendData(Context context) {
        AnalyticsTrackersData trackersData = AnalyticsTrackersData.getInstance(context);
        String userId = UserInfo.getInstance(context).appInstanceId;
        long lastSend = trackersData.getLastSend() != null ? trackersData.getLastSend().getTime()
                : 0;
        long group = Utils.hash(userId) % 10;
        group += 14; // min 2 weeks + (0-9) days
        lastSend += (group * 24 * 60 * 60 * 1000);
        if (new Date().getTime() < lastSend)
            return null;
        final int perSubmitItemCount = 50;
        if (AnalyticData.Queue.size(context) < perSubmitItemCount)
            return null;
        return AnalyticData.Queue.dequeueMany(context, perSubmitItemCount);
    }

    public static void send(final Context context) {
        if (Utils.isConnected(context) != Utils.CONNECTION_STATUS_CONNECTED)
            return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (sending.get())
                    return;
                sending.set(true);
                if (Utils.isConnected(context) != Utils.CONNECTION_STATUS_CONNECTED) {
                    sending.set(false);
                    return;
                }
                AnalyticsTrackersData trackersData = AnalyticsTrackersData.getInstance(context);
                UserInfo userInfo = UserInfo.getInstance(context);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String gender = userInfo.gender == null ? "" : userInfo.gender ? "1" : "2";
                boolean hasSent = false;
                for (int i = 0; i < 5; ++i) {
                    AnalyticData[] sendData = getSendData(context);
                    if (sendData == null) break;
                    String cookie = trackersData.getCookie();
                    Log.d("AnalTrack/send", String.format("Found cookie: %s", cookie));
                    if (cookie == null || cookie.length() == 0) {
                        AnalyticData.Queue.enqueueMany(context, sendData);
                        updateWebCookie(context);
                        break;
                    }
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json; charset=UTF-8");
                    headers.put("Cookie", cookie);
                    try {
                        StringBuilder allData = new StringBuilder();
                        for (AnalyticData data : sendData) {
                            String json = formatJson("{\n" +
                                            "\t\"evtName\": \"%s\", \"app_id\": \"%s\", \"app_code\": \"%s\", \"app_ver\": \"%s\", \"os_ver\": \"%s\", \n" +
                                            "\t\"sdk_ver\":\"%s\", \"dev_name\": \"%s\", \"dev_model\": \"%s\", \"dev_prod\": \"%s\", \"age\": \"%s\", \n" +
                                            "\t\"gender\": \"%s\", \"location\": \"%s\", \"payload\": #####, \"evtDateTime\": \"%s\"\n" +
                                            "}", data.eventName, userInfo.appInstanceId, userInfo.appVersionCode, userInfo.appVersionName, userInfo.deviceOsVersion,
                                    userInfo.deviceSdkVersion, userInfo.deviceName, userInfo.deviceModel, userInfo.deviceProduct, userInfo.ageCategory + "",
                                    gender, userInfo.locationCategory, sdf.format(data.date)
                            );
                            json = json.replace("\t", "")
                                    .replace(", \n", ",")
                                    .replace("\n", "")
                                    .replace("\": \"", "\":\"");
                            json = json.replace("#####", data.payload);
                            if (allData.length() > 0)
                                allData.append(',');
                            allData.append(json);
                        }
                        allData.insert(0, "[");
                        allData.append("]");
                        String res = Utils.postGzipped("http://shmrl.ihostfull.com/stats/create.php",
                                allData.toString(), headers);
                        if (res.contains("stat was created")) {
                            hasSent = true;
                            Log.d("AnalTrack/send", "Successfully sent data: " + allData);
                        } else {
                            AnalyticData.Queue.enqueueMany(context, sendData);
                            Log.d("AnalTrack/send", "Fail sending data: " + allData);
                            if (res.length() > 0)
                                updateWebCookie(context);
                            break;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        AnalyticData.Queue.enqueueMany(context, sendData);
                        break;
                    }
                }
                if (hasSent)
                    trackersData.setLastSend(new Date(), context);
                sending.set(false);
            }
        }).start();
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
}
class AnalyticsTrackersData implements Serializable {

    static final long serialVersionUID = 1L;
    private static AnalyticsTrackersData instance;
    private static final String settingFilename = "AnalyticsTrackersData.dat";

    private String cookie;
    private Date lastSend;
    private Date cookieLastUpdate;

    private AnalyticsTrackersData() {
    }

    public String getCookie() {
        long before12Hours = new Date().getTime() - 12 * 60 * 60 * 1000;
        if (cookieLastUpdate != null && cookieLastUpdate.getTime() > before12Hours)
            return cookie;
        return null;
    }

    public void setCookie(String cookie, Context context) {
        this.cookie = cookie;
        cookieLastUpdate = new Date();
        save(context);
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