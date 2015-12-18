package kilanny.shamarlymushaf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.RecoverySystem;
import android.view.View;
import android.widget.ListView;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Created by Yasser on 10/11/2015.
 */
public class Utils {

    public static final int DOWNLOAD_SERVER_INVALID_RESPONSE = -1,
            DOWNLOAD_OK = 0,
            DOWNLOAD_MALFORMED_URL = -2,
            DOWNLOAD_FILE_NOT_FOUND = -3,
            DOWNLOAD_IO_EXCEPTION = -4,
            DOWNLOAD_USER_CANCEL = -5;
    public static final String NON_DOWNLOADED_QUEUE_FILE_PATH = "nonDownloaded";

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

    public static File getSurahDir(Context context, String reciter, int surah) {
        Setting s = Setting.getInstance(context);
        File dir = new File(s.saveSoundsDirectory, "recites/" + reciter
                + "/" + surah);
        return dir;
    }

    public static File getAyahFile(Context context, String reciter, int surah, int ayah,
                                   boolean createDirIfNotExists) {
        File dir = getSurahDir(context, reciter, surah);
        if (createDirIfNotExists && !dir.exists())
            dir.mkdirs();
        return new File(dir, "" + ayah);
    }

    /**
     * Used for less memory usage, less object instantiation
     */
    public static File getAyahFile(int ayah, File surahDir) {
        return new File(surahDir, ayah + "");
    }

    public static String getAyahUrl(String reciter, int surah, int ayah) {
        return String.format(Locale.ENGLISH,
                "http://www.everyayah.com/data/%s/%03d%03d.mp3",
                reciter, surah, ayah);
    }

    public static String getAyahPath(Context context, String reciter, int surah, int ayah) {
        File f = getAyahFile(context, reciter, surah, ayah, false);
        if (f.exists())
            return f.getAbsolutePath();
        return getAyahUrl(reciter, surah, ayah);
    }

    public static int downloadTafaseerDb(Context context,
                                         RecoverySystem.ProgressListener progressListener,
                                         CancelOperationListener cancel) {
        File dbFile = getTafaseerDbFile(context);
        boolean error = true;
        if (dbFile.exists()) return DOWNLOAD_OK;
        byte[] buffer = new byte[4096];
        URL url;
        boolean conn = true;
        try {
            url = new URL("http://archive.org/download/shamraly/tafaseer.zip");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("connection", "close");
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
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

    private static int downloadFile(byte[] buffer, String fromUrl, File saveTo) {
        URL url;
        boolean conn = true;
        try {
            url = new URL(fromUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("connection", "close");
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                return DOWNLOAD_SERVER_INVALID_RESPONSE;
            //int fileLength = connection.getContentLength();
            // download the file
            InputStream input = connection.getInputStream();
            conn = false;
            FileOutputStream output = new FileOutputStream(saveTo);
            int count;
            //long total = 0;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                //total += count;
            }
            input.close();
            output.close();
            return DOWNLOAD_OK;
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

    public static int downloadAyah(String reciter, int surah, int ayah,
                                   byte[] buffer, File surahDir) {
        return downloadFile(buffer, getAyahUrl(reciter, surah, ayah),
                getAyahFile(ayah, surahDir));
    }

    private static File[] listAyahs(Context context, String reciter, int surah) {
        File file = getSurahDir(context, reciter, surah);
        if (!file.exists())
            return null;
        return file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                for (int i = 0; i < s.length(); ++i)
                    if (!Character.isDigit(s.charAt(i))) return false;
                return true;
            }
        });
    }

    public static ConcurrentLinkedQueue<Integer> getNotDownloaded(Context context,
                          String reciter, int surah, boolean buffer[]) {
        Arrays.fill(buffer, false);
        File files[] = listAyahs(context, reciter, surah);
        if (files != null) {
            for (File f : files) {
                buffer[Integer.parseInt(f.getName())] = true;
            }
        }
        ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        for (int i = surah == 1 ? 0 : 1; i <= QuranData.AYAH_COUNT[surah - 1]; ++i)
            if (!buffer[i])
                q.add(i);
        return q;
    }

    public static int getNumDownloaded(Context context, String reciter, int surah) {
        File[] arr = listAyahs(context, reciter, surah);
        return arr == null ? 0 : arr.length;
    }

    public static int downloadPage(Context context, int idx, String pageUrl, byte[] buffer) {
        File file = getPageFile(context, idx);
        if (pageExists(context, idx)) return DOWNLOAD_OK;
        int res = downloadFile(buffer, pageUrl, file);
        if (res != DOWNLOAD_OK) return res;
        // make sure bitmap is ok
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inSampleSize = 64;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (bitmap == null) {
            file.delete();
            return DOWNLOAD_SERVER_INVALID_RESPONSE;
        }
        bitmap.recycle();
        return res;
    }

    public static boolean pageExists(Context context, int page) {
        File file = getPageFile(context, page);
        if (!file.exists()) return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return options.outWidth != -1 && options.outHeight != -1;
        } catch (Exception ex) {
            file.delete();
            return false;
        }
    }

    private static File getNonExistPagesFile(Context context) {
        return new File(context.getFilesDir(), NON_DOWNLOADED_QUEUE_FILE_PATH);
    }

    public static void getNonExistPages(final Context context, final int maxPage,
                              final RecoverySystem.ProgressListener listener, int numThreads) {
        File file = getNonExistPagesFile(context);
        if (file.exists()) file.delete();
        final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
        final Shared progress = new Shared();
        progress.setData(0);
        final Thread[] threads = new Thread[numThreads];
        final int work = maxPage / threads.length;
        for (int i = 0; i < threads.length; ++i) {
            final int ii = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    int myStart = ii * work + 1;
                    int myEnd = (ii + 1) * work;
                    if (ii == threads.length - 1) myEnd = maxPage;
                    for (int i = myStart; i <= myEnd; ++i) {
                        if (!pageExists(context, i))
                            q.add(i);
                        progress.increment();
                        listener.onProgress(progress.getData());
                    }
                }
            });
        }
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads)
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                                        final boolean[] buffer2) {
        final ConcurrentLinkedQueue<Integer> q =
                Utils.getNotDownloaded(context, reciter, surah, buffer2);
        final File surahDir = getSurahDir(context, reciter, surah);
        if (!surahDir.exists())
            surahDir.mkdirs();
        Thread[] threads = new Thread[4];
        final Shared progress = new Shared();
        progress.setData(QuranData.AYAH_COUNT[surah - 1] + (surah == 1 ? 1 : 0) - q.size());
        final DownloadStatusArray error = new DownloadStatusArray(threads.length);
        final Shared interrupt = new Shared();
        interrupt.setData(0);
        for (int th = 0; th < threads.length; ++th) {
            final int myIdx = th;
            threads[th] = new Thread(new Runnable() {

                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    while (interrupt.getData() == 0 &&
                            cancel.canContinue() && error.isAllOk()) {
                        Integer per = q.poll();
                        if (per == null) break;
                        int code = downloadAyah(reciter, surah, per, buf, surahDir);
                        error.status[myIdx].setData(code);
                        if (code == DOWNLOAD_OK) {
                            progress.increment();
                            progressListener.onProgress(progress.getData());
                        }
                    }
                }
            });
        }
        for (Thread thread1 : threads) thread1.start();
        for (Thread thread : threads)
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                interrupt.setData(1);
                break;
            }
        listener.taskCompleted(!cancel.canContinue() || interrupt.getData() != 0 ?
                DOWNLOAD_USER_CANCEL : error.getFirstError());
    }

    public static AsyncTask downloadSurah(final Context context,
                              final String reciter, final int surah,
                              final RecoverySystem.ProgressListener progress,
                              final DownloadTaskCompleteListener listener) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared res = new Shared();
                myDownloadSurah(context, reciter, surah, new RecoverySystem.ProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        publishProgress(progress);
                    }
                }, new DownloadTaskCompleteListener() {
                    @Override
                    public void taskCompleted(int result) {
                        res.setData(result);
                    }
                }, new CancelOperationListener() {
                    @Override
                    public boolean canContinue() {
                        return !isCancelled();
                    }
                }, new boolean[290]);
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
        File[] files = listAyahs(context, reciter, surah);
        if (files == null) return true;
        boolean res = true;
        for (File f : files) {
            res &= f.delete();
        }
        return res;
    }

    public static void showConfirm(Context context, String title, String msg,
                             DialogInterface.OnClickListener ok,
                             DialogInterface.OnClickListener cancel) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("نعم", ok)
                .setNegativeButton("لا", cancel)
                .show();
    }

    public static void deleteAll(final Context context, final String reciter,
                                 final RecoverySystem.ProgressListener progress,
                                 final Runnable finish) {
        showConfirm(context, "حذف جميع التلاوات لقارئ",
                "حذف جميع التلاوات التي تم تحميلها لهذا القارئ نهائيا؟",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
                    }
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
                                   final DownloadTaskCompleteListener listener) {
        return new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                final Shared error = new Shared();
                error.setData(DOWNLOAD_OK);
                boolean[] buffer = new boolean[290];
                for (int i = 1; !isCancelled() && error.getData() == DOWNLOAD_OK && i <= 114; ++i) {
                    final int surah = i;
                    myDownloadSurah(context, reciter, i, new RecoverySystem.ProgressListener() {
                        @Override
                        public void onProgress(int progress) {
                            publishProgress(surah, progress);
                        }
                    }, new DownloadTaskCompleteListener() {
                        @Override
                        public void taskCompleted(int result) {
                            error.setData(result);
                        }
                    }, new CancelOperationListener() {
                        @Override
                        public boolean canContinue() {
                            return !isCancelled();
                        }
                    }, buffer);
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
                String res = ((NodeList) xPath.evaluate("/quran/sura[@index=\"" + a.sura
                                + "\"]/aya[@index=\"" + a.ayah + "\"]",
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

            String version = "", whatsNew = "";
            for (Object new_node : new_nodes) {
                TagNode info_node = (TagNode) new_node;
                whatsNew += info_node.getAllChildren().get(0).toString().trim()
                        + "\n";
            }
            if (version_nodes.length > 0) {
                TagNode ver = (TagNode) version_nodes[0];
                version = ver.getAllChildren().get(0).toString().trim();
            }
            return new String[]{version, whatsNew};
        } catch (IOException | XPatherException e) {
            e.printStackTrace();
            return null;
        }
    }
}

interface DownloadTaskCompleteListener {
    void taskCompleted(int result);
}

interface DownloadAllProgressChangeListener {
    void onProgressChange(int surah, int ayah);
}

interface CancelOperationListener {
    boolean canContinue();
}