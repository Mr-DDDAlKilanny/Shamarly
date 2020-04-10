package kilanny.shamarlymushaf.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.RecoverySystem;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Ayah;
import kilanny.shamarlymushaf.data.AyahFile;
import kilanny.shamarlymushaf.data.DownloadedAyat;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.data.Shared;
import kilanny.shamarlymushaf.views.QuranImageView;

/**
 * Created by Yasser on 10/11/2015.
 */
public class Utils {

    public static final int DOWNLOAD_SERVER_INVALID_RESPONSE = -1,
            DOWNLOAD_OK = 0,
            DOWNLOAD_MALFORMED_URL = -2,
            DOWNLOAD_FILE_NOT_FOUND = -3,
            DOWNLOAD_IO_EXCEPTION = -4,
            DOWNLOAD_USER_CANCEL = -5,
            DOWNLOAD_QUOTA_EXCEEDED = -6;
    public static final byte CONNECTION_STATUS_CONNECTED = 1,
            CONNECTION_STATUS_NOT_CONNECTED = 2,
            CONNECTION_STATUS_UNKNOWN_STATUS = 3;
    //https://developer.android.com/topic/performance/threads
    public static final int MIN_THREAD_MEMORY_ALLOCATION = 64 * 1024;
    public static final String NON_DOWNLOADED_QUEUE_FILE_PATH = "nonDownloaded";

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static int getCpuCoreCount(boolean forceReadFromSysFile) {
        if(!forceReadFromSysFile && Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            //Private Class to display only CPU devices in the directory listing
            try {
                //Get directory containing CPU info
                File dir = new File("/sys/devices/system/cpu/");
                //Filter to only list the devices we care about
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        //Check if filename is "cpu", followed by a single digit number
                        if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
                            return true;
                        }
                        return false;
                    }
                });
                //Return the number of cores (virtual CPU devices)
                Log.d("getCpuCoreCount", "Found " + files.length + " cores");
                return files.length;
            } catch(Exception e) {
                if (Build.VERSION.SDK_INT >= 17)
                    return Runtime.getRuntime().availableProcessors();
                //Default to return 1 core
                return 1;
            }
        }
    }

    public static class DownloadStatusArray {

        public final Shared[] status;

        public DownloadStatusArray(int size) {
            status = new Shared[size];
            for (int i = 0; i < size; ++i) {
                status[i] = new Shared();
                status[i].setData(DOWNLOAD_OK);
            }
        }

        public boolean isAllOk() {
            for (Shared s : status)
                if (s.getData() != DOWNLOAD_OK)
                    return false;
            return true;
        }

        public boolean isAnyOk() {
            for (Shared s : status)
                if (s.getData() == DOWNLOAD_OK)
                    return true;
            return false;
        }

        public int getFirstError() {
            for (Shared s : status)
                if (s.getData() != DOWNLOAD_OK)
                    return s.getData();
            return DOWNLOAD_OK;
        }
    }

    public static File getDatabaseDir(Context context) {
        File filesDir;
        // Make sure it's available
        if (!isExternalStorageWritable() || (filesDir = context.getExternalFilesDir(null)) == null) {
            // Load another directory, probably local memory
            filesDir = context.getFilesDir();
        }
        return filesDir;
    }

    public static File getTafaseerDbFile(Context context) {
        return new File(getDatabaseDir(context), "tafaseer.db");
    }

    public static File getQuranDir(Context context) {
        File file = new File(getDatabaseDir(context), "pages");
        if (!file.exists())
            file.mkdirs();
        return file;
    }

    public static File getPageFile(Context context, int idx) {
        return new File(Utils.getQuranDir(context), String.format(Locale.ENGLISH, "%d", idx));
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    @Nullable
    public static DocumentFile findOrCreateDir(DocumentFile parent, String name, boolean createIfNotExists) {
        DocumentFile file = parent.findFile(name);
        if (file == null && createIfNotExists)
            file = parent.createDirectory(name);
        return file;
    }

    /**
     * @return Either File or DocumentFile (or null)
     */
    @Nullable
    public static Object getSurahDir(Context context, String reciter, int surah,
                                     boolean createDirsIfNotExists) {
        Setting s = Setting.getInstance(context);
        Boolean isUri = isSaveSoundsUri(context);
        if (isUri == null) return null;
        if (!isUri) {
            return new File(s.saveSoundsDirectory, "recites/" + reciter
                    + "/" + surah);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri uri = Uri.parse(s.saveSoundsDirectory);
            DocumentFile root = DocumentFile.fromTreeUri(context, uri);
            if (root == null) return null; // moved or deleted ?
            DocumentFile recites = findOrCreateDir(root, "recites", createDirsIfNotExists);
            if (recites == null) return null; // could not create dir at selected path
            DocumentFile reciterFile = findOrCreateDir(recites, reciter, createDirsIfNotExists);
            if (reciterFile == null) return null;
            String _sura = String.format(Locale.ENGLISH, "%d", surah);
            return findOrCreateDir(reciterFile, _sura, createDirsIfNotExists);
        } else {
            throw new RuntimeException("how did code reach this?");
        }
    }

    @Nullable
    public static Boolean isSaveSoundsUri(Context context) {
        Setting s = Setting.getInstance(context);
        if (s.saveSoundsDirectory == null) return null;
        //return DocumentFile.isDocumentUri(context, Uri.parse(s.saveSoundsDirectory));
        boolean res = s.saveSoundsDirectory.startsWith("content://");
        if (!res) {
            File file = new File(s.saveSoundsDirectory);
            if (!file.exists() || !file.isDirectory())
                return null;
        } else {
            DocumentFile documentFile = DocumentFile.fromTreeUri(context, Uri.parse(s.saveSoundsDirectory));
            if (documentFile == null || !documentFile.exists() || !documentFile.isDirectory())
                return null;
        }
        return res;
    }

    public static Object getAyahFile(Context context, String reciter, int surah, int ayah,
                                   boolean createDirIfNotExists) {
        Object dir = getSurahDir(context, reciter, surah, true);
        if (dir == null) return null;
        if (dir instanceof File) {
            File file = (File) dir;
            if (createDirIfNotExists && !file.exists())
                file.mkdirs();
            return new File(file, "" + ayah);
        } else {
            DocumentFile documentFile = (DocumentFile) dir;
            String _ayah = String.format(Locale.ENGLISH, "%d", ayah);
            return new AyahFile(documentFile, _ayah);
        }
    }

    /**
     * Used for less memory usage, less object instantiation
     */
    public static Object getAyahFile(int ayah, Object surahDir) {
        if (surahDir instanceof File)
            return new File((File) surahDir, ayah + "");
        String _ayah = String.format(Locale.ENGLISH, "%d", ayah);
        return new AyahFile((DocumentFile) surahDir, _ayah);
    }

    public static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; ++i)
            if (array[i].equals(value))
                return i;
        return -1;
    }

    public static Uri getAyahUrl(String reciter, int surah, int ayah, QuranData data, int numAttempt) {
        if (reciter == null)
            return null;
        if (numAttempt == 1) {
            if (!reciter.startsWith("null"))
                return Uri.parse(String.format(Locale.ENGLISH,
                        "http://www.everyayah.com/data/%s/%03d%03d.mp3",
                        reciter, surah, ayah));
        }
        int idx = indexOf(data.reciterValues, reciter); //should never be == -1
        String alt = data.reciterValues_alt[idx];
        if (alt.startsWith("null"))
            return null;
        return Uri.parse(String.format(Locale.ENGLISH, alt.replace("%%", "%"),
                surah, ayah));
    }

    public static Uri getAyahPath(Context context, String reciter, int surah, int ayah,
                                     QuranData data, int numAttempt) {
        DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
        boolean needSave = false;
        try {
            Object f = getAyahFile(context, reciter, surah, ayah, false);
            if (f instanceof File) {
                File file = (File) f;
                if (file.exists()) {
                    if (!downloadedAyat.get(reciter, surah, ayah)) {
                        downloadedAyat.set(reciter, surah, ayah, true);
                        needSave = true;
                    }
                    return Uri.parse(file.toURI().toString());
                }
            } else {
                AyahFile ayahFile = (AyahFile) f;
                if (ayahFile != null) {
                    DocumentFile documentFile = ayahFile.get();
                    if (documentFile != null) {
                        if (!downloadedAyat.get(reciter, surah, ayah)) {
                            downloadedAyat.set(reciter, surah, ayah, true);
                            needSave = true;
                        }
                        return documentFile.getUri();
                    }
                }
            }
            downloadedAyat.set(reciter, surah, ayah, false);
            return getAyahUrl(reciter, surah, ayah, data, numAttempt);
        } finally {
            if (needSave)
                downloadedAyat.save(context);
        }
    }

    private static int downloadTafaseerDbHelper(RecoverySystem.ProgressListener progressListener,
                                                CancelOperationListener cancel,
                                                File dbFile, String downloadUrl,
                                                long expectedZipLength, byte[] buffer) {
        boolean error = true;
        URL url;
        boolean conn = true;
        try {
            url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("connection", "close");
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
            long zipLength;
            //final long ZIP_LENGTH = 36486134L;
            if (Build.VERSION.SDK_INT >= 24)
                zipLength = connection.getContentLengthLong();
            else
                zipLength = connection.getContentLength();
            if (zipLength != expectedZipLength) return DOWNLOAD_SERVER_INVALID_RESPONSE;
            // download the file
            ZipInputStream zipIs = new ZipInputStream(connection.getInputStream());
            ZipEntry entry = zipIs.getNextEntry();
            if (entry == null) return DOWNLOAD_SERVER_INVALID_RESPONSE;
            long fileLength = entry.getSize();
            conn = false;
            FileOutputStream output = new FileOutputStream(dbFile);
            int count, tmpProgress, progress = -1;
            long total = 0;
            while ((count = zipIs.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                total += count;
                tmpProgress = (int) (total * 100 / fileLength);
                if (progressListener != null && tmpProgress != progress) // only if total length is known
                    progressListener.onProgress(progress = tmpProgress);
                if (cancel != null && !cancel.canContinue())
                    break;
            }
            zipIs.closeEntry();
            zipIs.close();
            output.close();
            error = false;
            if (total != fileLength)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
            return cancel != null && !cancel.canContinue() ? DOWNLOAD_USER_CANCEL : DOWNLOAD_OK;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return DOWNLOAD_MALFORMED_URL;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return DOWNLOAD_FILE_NOT_FOUND;
        } catch (SocketException ex) {
            ex.printStackTrace();
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        } catch (IOException e) {
            e.printStackTrace();
            return conn ? DOWNLOAD_SERVER_INVALID_RESPONSE : DOWNLOAD_IO_EXCEPTION;
        } finally {
            if ((error || cancel != null && !cancel.canContinue()) && dbFile.exists())
                dbFile.delete();
        }
    }

    public static byte isConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting() ?
                    CONNECTION_STATUS_CONNECTED : CONNECTION_STATUS_NOT_CONNECTED;
        } catch (Exception ignored) {
            return CONNECTION_STATUS_UNKNOWN_STATUS;
        }
    }

    public static int downloadTafaseerDb(Context context,
                                         RecoverySystem.ProgressListener progressListener,
                                         CancelOperationListener cancel) {
        File dbFile = getTafaseerDbFile(context);
        if (dbFile.exists()) return DOWNLOAD_OK;
        long space = 0;
        try {
            space = getDatabaseDir(context).getUsableSpace();
        } catch (SecurityException ignored) {
        }
        if (space > 0 && space < 140 * 1024 * 1024)
            return DOWNLOAD_IO_EXCEPTION;
        int urls[] = {R.string.donwloadTafaseerDbUrl, R.string.donwloadTafaseerDbUrl2};
        int res = -1;
        byte[] buffer = new byte[4096];
        for (int id : urls) {
            res = downloadTafaseerDbHelper(progressListener, cancel, dbFile,
                    context.getResources().getString(id),
                    Long.parseLong(context.getResources().getString(R.string.tafaseerDbZipLength)),
                    buffer);
            if (res == DOWNLOAD_OK || res == DOWNLOAD_USER_CANCEL)
                break;
            else if (dbFile.exists())
                dbFile.delete();
        }
        return res;
    }

    public static void extractZippedFile(InputStream zip, File output) throws IOException {
        byte[] buffer = new byte[4096];
        ZipEntry ze = null;
        int length;
        FileOutputStream myOutput = new FileOutputStream(output);
        ZipInputStream zipIs = new ZipInputStream(zip);
        if ((ze = zipIs.getNextEntry()) != null) {
            while ((length = zipIs.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            zipIs.closeEntry();
            myOutput.flush();
            myOutput.close();
        }
        zipIs.close();
        zip.close();
    }

    public static int findReciteZipItemByFileName(@NonNull Context context, @NonNull String fileName) {
        String name;
        if (fileName.contains("muhammad_siddiq_al-minshawi_teacher"))
            name = "null_1";
        else
            name = fileName.substring(0, fileName.lastIndexOf('.'));
        String[] r = context.getResources().getStringArray(R.array.reciter_values);
        for (int i = 0; i < r.length; ++i) if (name.contains(r[i])) return i;
        return -1;
    }

    public static boolean extractReciteZipFile(@NonNull Context context, @NonNull File zipFile,
                                               RecoverySystem.ProgressListener listener) throws IOException {
        FileInputStream fis = new FileInputStream(zipFile);
        boolean res = extractReciteZipFile(context, zipFile.getName(), fis, listener);
        fis.close();
        return res;
    }

    public static boolean extractReciteZipFile(@NonNull Context context,
                                               String fileName,
                                               @NonNull InputStream inputStream,
                                               RecoverySystem.ProgressListener listener) throws IOException {
        int reciteIdx = findReciteZipItemByFileName(context, fileName);
        if (reciteIdx == -1) return false;
        byte[] buffer = new byte[1024];
        String[] r = context.getResources().getStringArray(R.array.reciter_values);
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry ze = zis.getNextEntry();
        Pattern pattern = Pattern.compile("^(\\d{3})(\\d{3}).mp3$");
        Object ayahFile = null;
        DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
        int curSura = -1;
        try {
            while (ze != null) {
                Matcher matcher = pattern.matcher(ze.getName());
                if (matcher.matches()) {
                    int surah = Integer.parseInt(matcher.group(1));
                    if (surah != curSura) {
                        curSura = surah;
                        listener.onProgress(curSura);
                    }
                    int ayah = Integer.parseInt(matcher.group(2));
                    Object surahDir = getSurahDir(context, r[reciteIdx], surah, true);
                    if (surahDir instanceof File) {
                        File file = (File) surahDir;
                        if (!file.exists()) file.mkdirs();
                    }
                    ayahFile = getAyahFile(ayah, surahDir);
                    boolean exists;
                    if (ayahFile instanceof File) {
                        File file = (File) ayahFile;
                        exists = file.exists();
                    } else {
                        AyahFile ayahFile1 = (AyahFile) ayahFile;
                        exists = ayahFile1.get() != null;
                    }
                    if (!exists) {
                        //Log.d("extractSurahZip", "Unzipping to " + ayahFile.getAbsolutePath());
                        OutputStream fos;
                        if (ayahFile instanceof File) {
                            fos = new FileOutputStream((File) ayahFile);
                        } else {
                            fos = context.getContentResolver().openOutputStream(
                                    ((AyahFile) ayahFile).getOrCreate().getUri());
                        }
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    downloadedAyat.set(r[reciteIdx], surah, ayah, true);
                }
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            downloadedAyat.save(context);
        } catch (Exception ex) {
            downloadedAyat.save(context);
            if (ayahFile != null) {
                try {
                    if (ayahFile instanceof File) {
                        ((File) ayahFile).delete();
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        DocumentsContract.deleteDocument(context.getContentResolver(),
                                ((AyahFile) ayahFile).getOrCreate().getUri());
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            throw ex;
        }
        return true;
    }

    public static androidx.appcompat.app.AlertDialog showIndeterminateProgressDialog(Context context, String title) {
        int llPadding = 30;
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(context);
        tvText.setText(title);
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(ll);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
        return dialog;
    }

    private static int downloadFile(byte[] buffer, Uri fromUrl, OutputStream output) {
        if (fromUrl == null)
            return DOWNLOAD_MALFORMED_URL;
        URL url;
        boolean conn = true;
        try {
            url = new URL(fromUrl.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("connection", "close");
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
            long fileLength;
            if (Build.VERSION.SDK_INT >= 24)
                fileLength = connection.getContentLengthLong();
            else
                fileLength = connection.getContentLength();
            // download the file
            InputStream input = connection.getInputStream();
            conn = false;
            int count;
            long total = 0;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                total += count;
            }
            input.close();
            connection.disconnect();
            return fileLength <= 0 || total == fileLength ?
                    DOWNLOAD_OK : DOWNLOAD_SERVER_INVALID_RESPONSE;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return DOWNLOAD_MALFORMED_URL;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return DOWNLOAD_FILE_NOT_FOUND;
        } catch (SocketException ex) {
            ex.printStackTrace();
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        } catch (IOException e) {
            e.printStackTrace();
            return conn ? DOWNLOAD_SERVER_INVALID_RESPONSE : DOWNLOAD_IO_EXCEPTION;
        }
    }

    private static int _downloadAyah(Context context, Uri ayahUri,
                                     byte[] buffer, Object ayahFile) throws IOException {
        DownloadQuota q = DownloadQuota.getInstance(context);
        if (!q.canDownloadNow()) return DOWNLOAD_QUOTA_EXCEEDED;
        int res = -1;
        boolean exists;
        if (ayahFile instanceof File) {
            File file = (File) ayahFile;
            exists = file.exists();
        } else {
            AyahFile ayahFile1 = (AyahFile) ayahFile;
            exists = ayahFile1.get() != null;
        }
        if (exists)
            return DOWNLOAD_OK;
        try {
            OutputStream fos;
            if (ayahFile instanceof File) {
                fos = new FileOutputStream((File) ayahFile);
            } else {
                fos = context.getContentResolver().openOutputStream(
                        ((AyahFile) ayahFile).getOrCreate().getUri());
            }
            res = downloadFile(buffer, ayahUri, fos);
            fos.close();
            return res;
        } finally {
            if (res == DOWNLOAD_OK) {
                long length;
                if (ayahFile instanceof File) {
                    length = ((File) ayahFile).length();
                } else {
                    length = ((AyahFile) ayahFile).get().length();
                }
                q.incrementBytes(length);
                q.save(context);
            }
        }
    }

    private static void deleteIfFailed(Context context, int status, Object ayahFile) {
        if (status != DOWNLOAD_OK) { // download failed
            if (ayahFile instanceof File) {
                File file = (File) ayahFile;
                if (file.exists()) file.delete();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AyahFile ayahFile1 = (AyahFile) ayahFile;
                DocumentFile documentFile = ayahFile1.get();
                if (documentFile != null) {
                    try {
                        DocumentsContract.deleteDocument(context.getContentResolver(),
                                documentFile.getUri());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static int downloadAyah(Context context, String reciter, int surah, int ayah,
                                   byte[] buffer, Object surahDir, QuranData data) {
        DownloadQuota q = DownloadQuota.getInstance(context);
        if (!q.canDownloadNow()) return DOWNLOAD_QUOTA_EXCEEDED;
        int res;
        Object ayahFile = getAyahFile(ayah, surahDir);
        boolean exists;
        if (ayahFile instanceof File) {
            File file = (File) ayahFile;
            exists = file.exists();
        } else {
            AyahFile ayahFile1 = (AyahFile) ayahFile;
            exists = ayahFile1.get() != null;
        }
        DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
        if (exists) {
            if (!downloadedAyat.get(reciter, surah, ayah)) {
                downloadedAyat.set(reciter, surah, ayah, true);
                downloadedAyat.save(context);
            }
            return DOWNLOAD_OK;
        }
        try {
            res = _downloadAyah(context, getAyahUrl(reciter, surah, ayah, data, 1),
                    buffer, ayahFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            res = DOWNLOAD_IO_EXCEPTION;
        }
        if (res == DOWNLOAD_OK) {
            downloadedAyat.set(reciter, surah, ayah, true);
            downloadedAyat.save(context);
            return res;
        } else deleteIfFailed(context, res, ayahFile);
        Uri next = getAyahUrl(reciter, surah, ayah, data, 2);
        if (next == null) return res;
        try {
            res = _downloadAyah(context, getAyahUrl(reciter, surah, ayah, data, 1),
                    buffer, ayahFile);
        } catch (IOException ex) {
            ex.printStackTrace();
            res = DOWNLOAD_IO_EXCEPTION;
        }
        downloadedAyat.set(reciter, surah, ayah, res == DOWNLOAD_OK);
        downloadedAyat.save(context);
        if (res == DOWNLOAD_OK) return res;
        else deleteIfFailed(context, res, ayahFile);
        return res;
    }

    private static Object[] listAyahs(Context context, String reciter, int surah) {
        Object surahDir = getSurahDir(context, reciter, surah, false);
        if (surahDir == null) return null;
        if (surahDir instanceof File) {
            File file = (File) surahDir;
            if (!file.exists())
                return null;
            return file.listFiles((file1, s) -> {
                for (int i = 0; i < s.length(); ++i)
                    if (!Character.isDigit(s.charAt(i))) return false;
                return true;
            });
        } else {
            DocumentFile documentFile = (DocumentFile) surahDir;
            return documentFile.listFiles();
        }
    }

    public static ConcurrentLinkedQueue<Integer> getNotDownloaded(Context context,
                          String reciter, int surah, boolean buffer[], QuranData data) {
        Arrays.fill(buffer, false);
        Object files[] = listAyahs(context, reciter, surah);
        if (files != null) {
            for (Object f : files) {
                String name;
                if (f instanceof File)
                    name = ((File) f).getName();
                else
                    name = ((DocumentFile) f).getName();
                buffer[Integer.parseInt(name)] = true;
            }
        }
        ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        for (int i = surah == 1 ? 0 : 1; i <= data.surahs[surah - 1].ayahCount; ++i)
            if (!buffer[i])
                q.add(i);
        return q;
    }

    public static int getNumDownloaded(Context context, String reciter, int surah) {
        Boolean saveSoundsUri = isSaveSoundsUri(context);
        if (saveSoundsUri == null) return 0;
        if (!saveSoundsUri) {
            Object[] arr = listAyahs(context, reciter, surah);
            return arr == null ? 0 : arr.length;
        } else {
            int count = 0;
            DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
            for (int j = 0; j < downloadedAyat.getSuraLength(reciter, surah); ++j) {
                if (downloadedAyat.get(reciter, surah, j + (surah == 1 ? 0 : 1)))
                    ++count;
            }
            return count;
        }
    }

    public static void refreshNumDownloaded(Context context, String reciter,
                                            RecoverySystem.ProgressListener listener,
                                            Runnable onDone) {
        new AsyncTask<Void, Integer, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                Boolean saveSoundsUri = isSaveSoundsUri(context);
                if (saveSoundsUri == null) return null;
                DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
                for (int surah = 1; surah <= 114; ++surah) {
                    Object[] arr = listAyahs(context, reciter, surah);
                    for (int ayah = 0; ayah < downloadedAyat.getSuraLength(reciter, surah); ++ayah) {
                        downloadedAyat.set(reciter, surah, ayah + (surah == 1 ? 0 : 1), false);
                    }
                    if (arr != null) {
                        for (Object ayahFile : arr) {
                            int ayah = -1;
                            if (ayahFile instanceof File) {
                                File file = (File) ayahFile;
                                ayah = Integer.parseInt(file.getName());
                            } else if (ayahFile != null) {
                                DocumentFile documentFile = (DocumentFile) ayahFile;
                                ayah = Integer.parseInt(documentFile.getName());
                            }
                            if (ayah >= 0) {
                                downloadedAyat.set(reciter, surah, ayah, true);
                            }
                        }
                    }
                    publishProgress(surah);
                }
                downloadedAyat.save(context);
                AnalyticsTrackers.getInstance(context).sendRefreshDownloads(saveSoundsUri);
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                listener.onProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                onDone.run();
            }
        }.execute();
    }

    public static void refreshNumDownloaded(Context context, String reciter, Runnable onDone) {
        showConfirm(context, "تحديث الملفات",
                "متأكد أنك تريد تحديث الملفات التي تم تحميلها؟ قد تستغرق هذه العملية وقتا"
                , (dialog, which) -> {
                    final ProgressDialog show = new ProgressDialog(context);
                    show.setTitle("تحديث الملفات");
                    show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    show.setIndeterminate(false);
                    show.setCancelable(false);
                    show.setMax(114);
                    show.setProgress(0);
                    show.show();
                    refreshNumDownloaded(context, reciter, show::setProgress, () -> {
                        show.dismiss();
                        onDone.run();
                    });
                }, (dialog, which) -> {});
    }

    public static int downloadPage(Context context, int idx, String pageUrl, byte[] buffer) {
        File file = getPageFile(context, idx);
        if (pageExists(context, idx)) return DOWNLOAD_OK;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return DOWNLOAD_IO_EXCEPTION;
        }
        int res = downloadFile(buffer, Uri.parse(pageUrl), fos);
        try {
            fos.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            res = DOWNLOAD_IO_EXCEPTION;
        }
        if (res != DOWNLOAD_OK) {
            if (file.exists())
                file.delete();
            return res;
        }
        // make sure bitmap is ok
        if (!pageExists(context, idx)) {
            Log.d("downloadPage", "Fail downloading " + pageUrl);
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        }
        return res;
    }

    public static boolean pageExists(Context context, int page) {
        File file = getPageFile(context, page);
        if (!file.exists()) return false;
        if (file.length() != QuranData.PAGE_FILE_SIZE[page - 1]) {
            Log.w("pageExists", "page " + page + " size is not equal to the recorded size");
            file.delete();
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inSampleSize = 8;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return bitmap != null && !bitmap.isMutable(); //cause nullPointerException
        } catch (Exception ex) { //nullpointerexception, ioexception, etc
            file.delete();
            return false;
        } finally {
            try {
                if (bitmap != null) bitmap.recycle();
            } catch (Exception ignore) {
            }
        }
    }

    private static File getNonExistPagesFile(Context context) {
        return new File(context.getFilesDir(), NON_DOWNLOADED_QUEUE_FILE_PATH);
    }

    public static boolean canWarmStart(Context context) {
        return getNonExistPagesFile(context).exists();
    }

    public static void getNonExistPages(final Context context, final int maxPage,
                                        final RecoverySystem.ProgressListener listener,
                                        final int numThreads) {
        File file = getNonExistPagesFile(context);
        if (file.exists()) file.delete();
        final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        final Shared progress = new Shared();
        progress.setData(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final int work = maxPage / numThreads;
        ArrayList<Callable<Void>> callables = new ArrayList<>();
        for (int i = 0; i < numThreads; ++i) {
            final int ii = i;
            callables.add(() -> {
                int myStart = ii * work + 1;
                int myEnd = (ii + 1) * work;
                if (ii == numThreads - 1) myEnd = maxPage;
                for (int i1 = myStart; i1 <= myEnd; ++i1) {
                    if (!pageExists(context, i1)) {
                        q.add(i1);
                        Log.d("getNotExitPages", "Page " + i1 + " does not exist");
                    }
                    progress.increment();
                    listener.onProgress(progress.getData());
                }
                return null;
            });
        }
        try {
            executor.invokeAll(callables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executor.shutdownNow();
        saveNonExistPagesToFile(context, q);
    }

    public static ConcurrentLinkedQueue<Integer> getNonExistPagesFromFile(Context context) {
        File file = getNonExistPagesFile(context);
        if (!file.exists()) return null;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            ConcurrentLinkedQueue<Integer> q = (ConcurrentLinkedQueue<Integer>) objectInputStream.readObject();
            objectInputStream.close();
            return q;
        } catch (IOException | ClassNotFoundException e) {
            file.delete();
            e.printStackTrace();
            return null;
        }
    }

    public static void saveNonExistPagesToFile(Context context, ConcurrentLinkedQueue<Integer> q) {
        File file = getNonExistPagesFile(context);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            ObjectOutputStream stream = new ObjectOutputStream(outputStream);
            stream.writeObject(q);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void myDownloadSurah(final Context context,
                                        final String reciter, final int surah,
                                        final RecoverySystem.ProgressListener progressListener,
                                        final DownloadTaskCompleteListener listener,
                                        final CancelOperationListener cancel,
                                        final boolean[] buffer2,
                                        final QuranData data) {
        final ConcurrentLinkedQueue<Integer> q =
                Utils.getNotDownloaded(context, reciter, surah, buffer2, data);
        final Object surahDir = getSurahDir(context, reciter, surah, true);
        if (surahDir == null)
            throw new IllegalStateException("User has not selected download dir");
        if (surahDir instanceof File) {
            File file = (File) surahDir;
            if (!file.exists())
                file.mkdirs();
        }
        final Shared progress = new Shared();
        progress.setData(data.surahs[surah - 1].ayahCount + (surah == 1 ? 1 : 0) - q.size());
        byte[] buf = new byte[1024];
        //Basmalah if not downloaded
        int err = DOWNLOAD_OK;
        downloadAyah(context, reciter, 1, 1, buf,
                getSurahDir(context, reciter, 1, true), data);
        while (err == DOWNLOAD_OK && cancel.canContinue()) {
            Integer per = q.poll();
            if (per == null) break;
            err = downloadAyah(context, reciter, surah, per,
                    buf, surahDir, data);
            if (err == DOWNLOAD_OK) {
                progress.increment();
                progressListener.onProgress(progress.getData());
            }
        }
        listener.taskCompleted(!cancel.canContinue() ? DOWNLOAD_USER_CANCEL : err);
    }

    public static AsyncTask downloadSurah(final Context context,
                              final String reciter, final int surah,
                              final RecoverySystem.ProgressListener progress,
                              final DownloadTaskCompleteListener listener,
                              final QuranData data) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared res = new Shared();
                myDownloadSurah(context, reciter, surah,
                        this::publishProgress,
                        res::setData,
                        () -> !isCancelled(),
                        new boolean[290], data
                );
                return res.getData();
            }

            @Override
            protected void onProgressUpdate(final Integer... values) {
                progress.onProgress(values[0]);
            }

            @Override
            protected void onCancelled() {
                listener.taskCompleted(DOWNLOAD_USER_CANCEL);
            }

            @Override
            protected void onPostExecute(Integer result) {
                listener.taskCompleted(result);
            }
        }.execute();
    }

    public static boolean deleteSurah(Context context, String reciter, int surah) {
        Object[] files = listAyahs(context, reciter, surah);
        if (files == null) return true;
        boolean res = true;
        DownloadedAyat downloadedAyat = DownloadedAyat.getInstance(context);
        for (Object f : files) {
            int ayah;
            if (f instanceof File) {
                File file = (File) f;
                ayah = Integer.parseInt(file.getName());
                res &= file.delete();
            } else {
                DocumentFile file = (DocumentFile) f;
                ayah = Integer.parseInt(file.getName());
                res &= file.delete();
            }
            downloadedAyat.set(reciter, surah, ayah, false);
        }
        downloadedAyat.save(context);
        return res;
    }

    public static void showConfirm(Context context, String title, String msg,
                             DialogInterface.OnClickListener ok,
                             DialogInterface.OnClickListener cancel) {
        showConfirm(context, title, msg, "نعم", "لا", ok, cancel);
    }

    public static void showConfirm(Context context, String title, String msg,
                                   String okText, String cancelText,
                                   DialogInterface.OnClickListener ok,
                                   DialogInterface.OnClickListener cancel) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(okText, ok)
                .setNegativeButton(cancelText, cancel)
                .show();
    }

    public static void showSelectionDlg(Context context, String title, String[] options,
                                        boolean cancelabe,
                                        DialogInterface.OnClickListener onClickListener,
                                        DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setItems(options, onClickListener);
        builder.setOnCancelListener(cancelListener);
        builder.setCancelable(cancelabe);
        builder.show();
    }

    public static void deleteAll(final Context context, final String reciter,
                                 final RecoverySystem.ProgressListener progress,
                                 final Runnable finish) {
        showConfirm(context, "حذف جميع التلاوات لقارئ",
                "حذف جميع التلاوات التي تم تحميلها لهذا القارئ نهائيا؟",
                (dialog, which) -> {
                    final ProgressDialog show = new ProgressDialog(context);
                    show.setTitle("حذف جميع تلاوات قارئ");
                    show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    show.setIndeterminate(false);
                    show.setCancelable(false);
                    show.setMax(114);
                    show.setProgress(0);
                    show.show();
                    new AsyncTask<Void, Integer, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            for (int i = 1; i <= 114; ++i) {
                                deleteSurah(context, reciter, i);
                                publishProgress(i);
                            }
                            return null;
                        }

                        @Override
                        protected void onProgressUpdate(final Integer... values) {
                            show.setProgress(values[0]);
                            progress.onProgress(values[0]);
                        }

                        @Override
                        protected void onPostExecute(Void v) {
                            finish.run();
                            show.dismiss();
                        }
                    }.execute();
                }, null);
    }

    public static View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public static AsyncTask downloadAll(final Activity context, final String reciter,
                                   final DownloadAllProgressChangeListener progress,
                                   final DownloadTaskCompleteListener listener,
                                    final QuranData data) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared error = new Shared();
                error.setData(DOWNLOAD_OK);
                boolean[] buffer = new boolean[290];
                for (int i = 1; !isCancelled() && error.getData() == DOWNLOAD_OK && i <= 114; ++i) {
                    final int surah = i;
                    myDownloadSurah(context, reciter, i,
                            progress1 -> publishProgress(surah, progress1),
                            error::setData,
                            () -> !isCancelled(), buffer, data);
                }
                return isCancelled() ? DOWNLOAD_USER_CANCEL : error.getData();
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progress.onProgressChange(values[0], values[1]);
            }

            @Override
            protected void onCancelled() {
                listener.taskCompleted(DOWNLOAD_USER_CANCEL);
            }

            @Override
            protected void onPostExecute(Integer result) {
                listener.taskCompleted(result);
            }
        }.execute();
    }

    public static void showAlert(Context context, String title, String msg, DialogInterface.OnClickListener ok) {
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(context);
        dlgAlert.setMessage(msg);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton("موافق", ok);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    public static String getAyahText(Context context, int sura, int ayah) {
        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            InputStream stream = context.getAssets().open("quran-uthmani.xml");
            is.setCharacterStream(new InputStreamReader(stream));
            doc = db.parse(is);
            stream.close();
        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            String expr = String.format(Locale.ENGLISH,
                    "/quran/sura[@index=\"%d\"]/aya[@index=\"%d\"]",
                    sura, ayah);
            String res = ((NodeList) xPath.evaluate(expr,
                    doc.getDocumentElement(), XPathConstants.NODESET))
                    .item(0).getAttributes().getNamedItem("text").getTextContent();
            return res;
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getAllAyahText(Context context, ArrayList<Ayah> list, QuranData quranData) {
        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            InputStream stream = context.getAssets().open("quran-uthmani.xml");
            is.setCharacterStream(new InputStreamReader(stream));
            doc = db.parse(is);
            stream.close();
        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            StringBuilder all = new StringBuilder();
            QuranImageView.sortMutliSelectList(list);
            int prevSurah = -1;
            for (Ayah a : list) {
                if (a.ayah < 1 || a.sura < 1 || a.sura > 114
                        || a.ayah > quranData.surahs[a.sura - 1].ayahCount) continue;
                String expr = String.format(Locale.ENGLISH,
                        "/quran/sura[@index=\"%d\"]/aya[@index=\"%d\"]",
                        a.sura, a.ayah);
                String res = ((NodeList) xPath.evaluate(expr,
                        doc.getDocumentElement(), XPathConstants.NODESET))
                        .item(0).getAttributes().getNamedItem("text").getTextContent();
                if (prevSurah != a.sura) {
                    prevSurah = a.sura;
                    if (all.length() > 0)
                        all.append("\n");
                    all.append("قال تعالى في سورة ")
                            .append(quranData.surahs[a.sura - 1].name.trim())
                            .append(":\n");
                }
                all.append("{").append(res).append(" (")
                        .append(ArabicNumbers.convertDigits(a.ayah + "")).append(")}\n");
            }
            all.append("\n")
                    .append("مصحف الشمرلي على أندرويد\n")
                    .append("https://play.google.com/store/apps/details?id=kilanny.shamarlymushaf");
            return all.toString();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[] getAppVersionInfo(String playPackage) {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();
        props.setAllowHtmlInsideAttributes(true);
        props.setAllowMultiWordAttributes(true);
        props.setRecognizeUnicodeChars(true);
        props.setOmitComments(true);
        try {
            URL url = new URL("https://play.google.com/store/apps/details?id=" + playPackage);
            URLConnection conn = url.openConnection();
            TagNode node = cleaner.clean(new InputStreamReader(conn.getInputStream()));
            Object[] new_nodes = node.evaluateXPath("//*[@class='recent-change']");
            Object[] version_nodes = node.evaluateXPath("//*[@itemprop='softwareVersion']");

            String version = "";
            StringBuilder whatsNew = new StringBuilder();
            for (Object new_node : new_nodes) {
                TagNode info_node = (TagNode) new_node;
                whatsNew.append(info_node.getAllChildren().get(0).toString().trim()).append("\n");
            }
            if (version_nodes.length > 0) {
                TagNode ver = (TagNode) version_nodes[0];
                version = ver.getAllChildren().get(0).toString().trim();
            }
            return new String[]{version, whatsNew.toString()};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    public static String getTimeAgoString(Date date) {
        if (date == null)
            return "";
        long time = date.getTime();
        if (time < 1000000000000L) {
            time *= 1000;
        }
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        long now = currentDate.getTime();
        if (time > now || time <= 0) {
            return "in the future !!?";
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "منذ لحظات";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "منذ دقيقة";
        } else if (diff < 60 * MINUTE_MILLIS) {
            return "منذ " + diff / MINUTE_MILLIS + " دقيقة";
        } else if (diff < 2 * HOUR_MILLIS) {
            return "منذ ساعة";
        } else if (diff < 24 * HOUR_MILLIS) {
            return "منذ " + diff / HOUR_MILLIS + " ساعات";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "أمس";
        } else {
            return "منذ " + diff / DAY_MILLIS + " يوم";
        }
    }

    public static String newUid() {
        return UUID.randomUUID().toString();
    }

    public static long hash(String s) {
        long hash = 7;
        for (int i = 0; i < s.length(); i++) {
            hash = hash * 31 + s.charAt(i);
        }
        return hash;
    }

    public static android.widget.Toast createToast(Context ctx, String text, int duration, int gravity) {
        @SuppressLint("ShowToast")
        android.widget.Toast toast = android.widget.Toast.makeText(ctx, text, duration);
        toast.setGravity(gravity, 0, 0);
        return toast;
    }

    public static android.widget.Toast createToast(Context ctx, int textResId, int duration, int gravity) {
        @SuppressLint("ShowToast")
        android.widget.Toast toast = android.widget.Toast.makeText(ctx, textResId, duration);
        toast.setGravity(gravity, 0, 0);
        return toast;
    }

    public static String postGzipped(String url, String postData, Map<String, String> headers) {
        StringBuilder data = new StringBuilder();

        HttpURLConnection httpURLConnection = null;
        try {

            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            httpURLConnection.setRequestProperty("Content-encoding", "gzip");

            httpURLConnection.setDoOutput(true);

            GZIPOutputStream dos1 = new GZIPOutputStream(httpURLConnection.getOutputStream());
            dos1.write(postData.getBytes("UTF-8"));
            dos1.flush();
            dos1.close();

            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                data.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return data.toString();
    }

    public static String sendHttpRequest(String url, String method, String body, Map<String, String> headers) {
        StringBuilder data = new StringBuilder();

        HttpURLConnection httpURLConnection = null;
        try {

            httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod(method);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            httpURLConnection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            InputStream in = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                data.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }

        return data.toString();
    }

    public static boolean haveAvailableMemory(int sz) {
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.maxMemory(),
                used = runtime.totalMemory();
        return mem - used >= sz;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void openUrlInChromeOrDefault(Context context, String urlString) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // Chrome browser presumably not installed so allow user to choose instead
            intent.setPackage(null);
            context.startActivity(intent);
        }
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
                switch (errorCode) {
                    case ConnectionResult.SUCCESS:
                        Log.d("isGmsAvailable", "SUCCESS");
                        // Google Play Services installed and up to date
                        return true;
                    case ConnectionResult.SERVICE_MISSING:
                        Log.d("isGmsAvailable", "MISSING");
                        // Google Play services is missing on this device.
                        break;
                    case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                        Log.d("isGmsAvailable", "VERSION_UPDATE_REQUIRED");
                        // The installed version of Google Play services is out of date.
                        break;
                    case ConnectionResult.SERVICE_DISABLED:
                        Log.d("isGmsAvailable", "DISABLED");
                        // The installed version of Google Play services has been disabled on this device.
                        break;
                    case ConnectionResult.SERVICE_INVALID:
                        Log.d("isGmsAvailable", "INVALID");
                        // The version of the Google Play services installed on this device is not authentic.
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
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
}

class DownloadQuota implements Serializable {
    public static final long DAILY_DOWNLOAD_QUOTA_BYTES = 30 * 1024 * 1024;
    private static final String settingFileName = "downloadQuota";
    private static DownloadQuota instance;

    private final Date beginDate;
    private long usedQuota;

    public Date getBeginDate() {
        return beginDate;
    }

    public long getUsedQuota() {
        return usedQuota;
    }

    public boolean canDownloadNow() {
        Date now = new Date();
        int days = (int) Math.ceil((now.getTime() - getBeginDate().getTime())
                / (1000.0 * 60 * 60 * 24));
        return getUsedQuota() <= days * DAILY_DOWNLOAD_QUOTA_BYTES;
    }

    private DownloadQuota() {
        beginDate = new Date();
    }

    public boolean save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(settingFileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static DownloadQuota getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(settingFileName);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (DownloadQuota) is.readObject();
                is.close();
                fis.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (instance == null)
                instance = new DownloadQuota();
        }
        return instance;
    }

    public void incrementBytes(long bytes) {
        usedQuota += bytes;
    }
}