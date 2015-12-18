package kilanny.shamarlymushaf;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RecoverySystem;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;
import kilanny.shamarlymushaf.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends FragmentActivity {
    private static abstract class BitmapPool {
        static final Bitmap[] POOL = new Bitmap[3];
        static final int[] idx = new int[3];

        static void reset() {
            Arrays.fill(idx, -1);
            Arrays.fill(POOL, null); // user may change page border display then open activity again
        }

        static int exists(int page) {
            for (int i = 0; i < idx.length; ++i)
                if (idx[i] == page)
                    return i;
            return -1;
        }
    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private final ColorMatrixColorFilter filter = new ColorMatrixColorFilter(
            new ColorMatrix(new float[]
        {
                -1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, -1.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, -1.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        }));

    private static final ColorMatrixColorFilter grayScaleFilter = new ColorMatrixColorFilter(
            new ColorMatrix(new float[] {
                    0.2989f, 0.5870f, 0.1140f, 0, 0,
                    0.2989f, 0.5870f, 0.1140f, 0, 0,
                    0.2989f, 0.5870f, 0.1140f, 0, 0,
                    0, 0, 0, 1, 0
            }));
    private static final int threshold = 128;
    private static final ColorMatrixColorFilter thresholdFilter = new ColorMatrixColorFilter(
            new ColorMatrix(new float[] {
                    85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                    85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                    85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                    0f, 0f, 0f, 1f, 0f
            }));
    private final static int dilationBuffer[][]
            = new int[QuranData.NORMAL_PAGE_HEIGHT][QuranData.NORMAL_PAGE_WIDTH];

    public static final String SHOW_PAGE_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage";
    public static final String SHOW_AYAH_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage#withAyah";
    private FullScreenImageAdapter adapter;
    private ViewPager viewPager;
    private Setting setting;
    SharedPreferences pref;
    private DbManager db;
    private ProgressBar bar;
    private MediaPlayer player;
    private int sura, ayah;
    private boolean allPagePlay = false;
    private boolean autoSwipPage = false;
    private Typeface tradionalArabicFont, tradionalArabicBoldFont;
    private QuranImageView shareImageView;
    private QuranData quranData;
    private int totalDeviceRamMg;
    private int recommendedRamMg;
    private ConcurrentLinkedQueue<Integer> notDownloaded;
    private String initialHighlightAyah; // used for hilighting search result
    private int rotationMode;
    private int currentAyahTafseerIdx; //current Ayah displayed in Tafseerdlg navigation

    //google analytics fields
    private HashSet<Integer> pagesViewed;
    private Date startDate;
    private HashSet<String> listenRecite, viewTafseer;

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
        if (pagesViewed != null && pagesViewed.size() > 1) {
            final long diff = new Date().getTime() - startDate.getTime();
            if (diff >= 60 * 1000) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Context context = getApplicationContext();
                        AnalyticsTrackers.sendPageReadStats(context, pagesViewed, diff);
                        if (listenRecite != null)
                            AnalyticsTrackers.sendListenReciteStats(context, listenRecite);
                        if (viewTafseer != null)
                            AnalyticsTrackers.sendTafseerStats(context, viewTafseer);
                    }
                }).start();
            }
            listenRecite = null;
            pagesViewed = null;
            startDate = null;
        }
        BitmapPool.reset();
        finish(); // prevent re-use the activity after stopping it (causes exceptions)
    }

    private void stopPlayback() {
        if (player != null) {
            if (player.isPlaying()) player.stop();
            player.release();
            player = null;
            allPagePlay = false;
            try {
                bar.setVisibility(View.GONE);
                togglePlayButton(false);
            } catch (Exception ex) {
            }
        }
    }

    private void initViewPagerAdapter() {
        adapter = new FullScreenImageAdapter(this, FullScreenImageAdapter.MAX_PAGE
                - notDownloaded.size());
        final GestureDetector tapGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    super.onLongPress(e);
                    if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
                        downloadAll();
                        return;
                    }
                    QuranImageView imageView = getCurrentPage();
                    int idx = imageView != null ? imageView.getAyahAtPos(e.getX(), e.getY()) : -1;
                    if (idx >= 0) {
                        stopPlayback();
                        imageView.selectedAyahIndex = idx;
                        imageView.invalidate();
                        mSystemUiHider.show();
                    }
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (TOGGLE_ON_CLICK) {
                        mSystemUiHider.toggle();
                    } else {
                        mSystemUiHider.show();
                    }
                    if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE)
                        downloadAll();
                    else {
                        QuranImageView imageView = getCurrentPage();
                        if (imageView != null && imageView.selectedAyahIndex >= QuranImageView.SELECTION_ALL) {
                            imageView.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                            imageView.invalidate();
                        }
                    }
                    return false;
                }
        });
        adapter.setInstantiateQuranImageViewListener(new FullScreenImageAdapter.OnInstantiateQuranImageViewListener() {
            @Override
            public void onInstantiate(WeakReference<QuranImageView> image, View parent) {
                image.get().setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        tapGestureDetector.onTouchEvent(event);
                        return false;
                    }
                });
                // this is not called at onPageSelected
                // when activity starts in landscape, so call here
                configOrientation(image.get());
                initCurrentPageInfo(image.get(), parent);
                if (initialHighlightAyah != null && image.get().currentPage != null
                        && image.get().currentPage.ayahs != null) {
                    String strs[] = initialHighlightAyah.split(",");
                    int ss = Integer.parseInt(strs[0]),
                            aa = Integer.parseInt(strs[1]);
                    for (int i = 0; i < image.get().currentPage.ayahs.size(); ++i) {
                        Ayah a = image.get().currentPage.ayahs.get(i);
                        if (a.sura == ss && a.ayah == aa) {
                            image.get().selectedAyahIndex = i;
                            image.get().invalidate();
                            initialHighlightAyah = null;
                            break;
                        }
                    }
                }
            }
        });
        viewPager.setAdapter(adapter);
        if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE)
            viewPager.setCurrentItem(0);
        else {
            // displaying selected image first
            Intent i = getIntent();
            int page = i.getIntExtra(SHOW_PAGE_MESSAGE, -1);
            initialHighlightAyah = i.getStringExtra(SHOW_AYAH_MESSAGE);
            page = page == -1 ? setting.page : page;
            showPage(page);
        }
    }

    private QuranImageView getCurrentPage() {
        try {
            return (QuranImageView) viewPager.findViewWithTag(setting.page).findViewById(R.id.quranPage);
        } catch (NullPointerException ex){
            return null;
        }
    }

    private void initCurrentPageInfo(QuranImageView image, View parent) {
        if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE ||
                pref.getBoolean("showPageBorders", false) ||
                !pref.getBoolean("showPageInfo", true)) {
            parent.findViewById(R.id.pageInfoLayout).setVisibility(View.GONE);
        } else if (image.currentPage != null && image.currentPage.ayahs.size() > 0) {
            AutoScrollViewPager pager = (AutoScrollViewPager) parent.findViewById(R.id.pageTitleViewPager);
            int page = image.currentPage.page;
            String juz = "", hizb = "";
            for (int i = 1; i < quranData.juzs.length; ++i) {
                int val = (int) quranData.juzs[i].value;
                if (page == val) {
                    juz = quranData.juzs[i].name;
                    break;
                } else if (page < val) {
                    juz = quranData.juzs[i - 1].name;
                    break;
                }
            }
            if (juz.equals("") &&
                    (int) quranData.juzs[quranData.juzs.length - 1].value < page) {
                juz = quranData.juzs[quranData.juzs.length - 1].name;
            }
            for (int i = 1; i < quranData.hizbs.length; ++i) {
                int val = (int) quranData.hizbs[i].value;
                if (val == page) {
                    hizb = quranData.hizbs[i].name;
                    break;
                } else if (val > page) {
                    hizb = quranData.hizbs[i - 1].name;
                    break;
                }
            }
            if (hizb.equals("") &&
                    (int) quranData.hizbs[quranData.hizbs.length - 1].value < page) {
                hizb = quranData.hizbs[quranData.hizbs.length - 1].name;
            }
            PageInfoAdapter adapter = new PageInfoAdapter();
            adapter.setSurahName("سورة " + quranData.surahs[image.currentPage.ayahs.get(0).sura - 1].name);
            adapter.setJuzNumber(juz);
            adapter.setPageNumber("صفحة " + ArabicNumbers.convertDigits(page + ""));
            adapter.setHizbNumber(hizb);
            pager.setAdapter(adapter);
            pager.setDirection(AutoScrollViewPager.LEFT);
            if (!pref.getBoolean("manuallyScrollPageInfo", false)) {
                pager.setCurrentItem(adapter.getCount() - 1);
                pager.setInterval(5000);
                pager.startAutoScroll();
            } else {
                pager.setCurrentItem(Integer.parseInt(pref
                        .getString("defaultPageInfoItem",
                                getString(R.string.defaultPageInfo))));
            }
        }
    }

    private void configOrientation(int orientation) {
        QuranImageView image = getCurrentPage();
        if (image == null) return;
        configOrientation(orientation, image);
    }

    private void configOrientation(QuranImageView image) {
        configOrientation(getResources().getConfiguration().orientation, image);
    }

    private void configOrientation(int orientation, QuranImageView image) {
        if (rotationMode != 0) {
            //force orientation is enabled
            if (rotationMode == 1) {
                image.setScaleType(ImageView.ScaleType.FIT_XY);
            } else {
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setScrollPosition(0, 0);
            }
            return;
        }
        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setScrollPosition(0, 0);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                image.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
            default:
                return;
        }
        image.invalidate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configOrientation(newConfig.orientation);
        if (shareImageView != null)
            configOrientation(newConfig.orientation, shareImageView);
    }

    private Page getPage(int idx) {
        DbManager db = DbManager.getInstance(this);
        Page page = db.getPage(idx);
        if (page.ayahs != null && pref.getBoolean("showPageBorders", false)) {
            //161, 141, 881, 1373
            for (Ayah a : page.ayahs) {
                for (RectF rect : a.rects) {
                    rect.set(rect.left + 161, rect.top + 141,
                            rect.right + 161, rect.bottom + 141);
                }
            }
        }
        return page;
    }

    private void initViewPager() {
        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.animator);
        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
            // Cached values.
            int mControlsHeight;
            int mShortAnimTime;

            @Override
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
            public void onVisibilityChange(boolean visible) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                    // If the ViewPropertyAnimator API is available
                    // (Honeycomb MR2 and later), use it to animate the
                    // in-layout UI controls at the bottom of the
                    // screen.
                    if (mControlsHeight == 0) {
                        mControlsHeight = controlsView.getHeight();
                    }
                    if (mShortAnimTime == 0) {
                        mShortAnimTime = getResources().getInteger(
                                android.R.integer.config_shortAnimTime);
                    }
                    controlsView.animate()
                            .translationY(visible ? 0 : mControlsHeight)
                            .setDuration(mShortAnimTime);
                } else {
                    // If the ViewPropertyAnimator APIs aren't
                    // available, simply show or hide the in-layout UI
                    // controls.
                    controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                }

                if (visible && AUTO_HIDE) {
                    // Schedule a hide().
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        viewPager = (ViewPager) contentView;
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            private WeakReference<QuranImageView> last;
            private AsyncTask current;

            private void config(final int page) {
                current = new AsyncTask<Void, Object, Void>() {

                    int arr[] = { page, page + 1, page - 1 };
                    final int idx[] = {1, 2, 0};
                    OutOfMemoryError outOfMemoryError;
                    Exception exception;

                    @Override
                    protected Void doInBackground(Void... params) {
                        for (int i = 0; i < arr.length; ++i) {
                            if (isCancelled()) return null;
                            if (arr[i] < 1 || arr[i] > FullScreenImageAdapter.MAX_PAGE)
                                continue;
                            try {
                                Page page = getPage(arr[i]);
                                readPage(arr[i], idx[i]);
                                publishProgress(arr[i], idx[i], page);
                            } catch (OutOfMemoryError err) {
                                outOfMemoryError = err;
                                publishProgress(arr[i], idx[i]);
                                err.printStackTrace();
                                AnalyticsTrackers.sendException(MainActivity.this, err);
                                return null;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                exception = ex;
                                publishProgress(arr[i], idx[i]);
                                AnalyticsTrackers.sendException(MainActivity.this, ex);
                                return null;
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPreExecute() {
                        //super.onPreExecute();
                        for (int item : arr) {
                            View v = viewPager.findViewWithTag(item);
                            if (v != null) {
                                QuranImageFragment.showProgress(v, null, null);
                            } else System.out.println("v is null");
                        }
                    }

                    @Override
                    protected void onProgressUpdate(Object... values) {
                        //super.onProgressUpdate(values);
                        if (isFinishing()) return;
                        if (outOfMemoryError != null) {
                            Toast.makeText(MainActivity.this, "خطأ: الذاكرة ممتلئة. أعد تشغيل التطبيق",
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        if (exception != null) {
                            Toast.makeText(MainActivity.this, "حدث خطأ أثناء فتح الصفحة\n" + exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        View v = viewPager.findViewWithTag(values[0]);
                        if (v != null) {
                            QuranImageFragment.showProgress(v, BitmapPool.POOL[(int) values[1]],
                                    (Page) values[2]);
                        } else System.out.println("v is null");
                    }
                }.execute();
            }

            @Override
            public void onPageSelected(int position) {
                if (!autoSwipPage)
                    stopPlayback();
                else autoSwipPage = false;
                setting.page = adapter.getCount() - position;
                if (current != null && !current.isCancelled())
                    current.cancel(false);
                config(setting.page);
                if (pagesViewed == null) {
                    pagesViewed = new HashSet<>();
                    startDate = new Date();
                }
                pagesViewed.add(setting.page);
                setBookmarkMenuItem(setting.isBookmarked(setting.page));
                setting.save(MainActivity.this);
                if (last != null && last.get() != null && last.get().myBitmap != null &&
                        !last.get().myBitmap.isRecycled()) {
                    last.get().selectedAyahIndex = QuranImageView.SELECTION_NONE;
                    last.get().invalidate();
                }
                try {
                    last = new WeakReference<>(getCurrentPage());
                    configOrientation(last.get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (pref.getBoolean("showHizbToast", true)) {
                    // find hizb-juz
                    for (int i = 1; i < quranData.hizbs.length; ++i) {
                        int val = (int) quranData.hizbs[i].value;
                        if (val == setting.page) {
                            String txt;
                            if (i % 2 == 1)
                                txt = quranData.juzs[1 + i / 2].name;
                            else
                                txt = quranData.hizbs[i].name;
                            Toast.makeText(MainActivity.this, txt, Toast.LENGTH_SHORT).show();
                            break;
                        } else if (val > setting.page) break;
                    }
                }
            }
        });
        initViewPagerAdapter();
    }

    public void showPage(int pos) {
        viewPager.setCurrentItem(adapter.getCount() - pos);
    }

    private void showError(String error) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(error);
        dlgAlert.setTitle("خطأ");
        dlgAlert.setPositiveButton("موافق", null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    private void displayGotoDlg() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_goto_dlg);
        TabHost tabHost = (TabHost) dialog.findViewById(R.id.tabHost);
        tabHost.setup();
        TabHost.TabSpec tab1 = tabHost.newTabSpec("tab1");
        TabHost.TabSpec tab2 = tabHost.newTabSpec("tab2");
        TabHost.TabSpec tab3 = tabHost.newTabSpec("tab3");
        tab1.setIndicator("الرقم");
        tab1.setContent(R.id.الرقم);
        tab2.setIndicator("السورة");
        tab2.setContent(R.id.listViewSurah);
        tab3.setIndicator(null,
                getResources().getDrawable(R.drawable.abc_btn_rating_star_on_mtrl_alpha));
        tab3.setContent(R.id.listViewBookmarks);
        /** Add the tabs  to the TabHost to display. */
        tabHost.addTab(tab1);
        tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        //EditText txt = (EditText) dialog.findViewById(R.id.editTextPageNum);
        //txt.setText("" + setting.page);
        dialog.setTitle("ذهاب إلى");
        final ListView l = (ListView) dialog.findViewById(R.id.listViewSurah);
        l.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, quranData.surahs));
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Surah itemValue = (Surah) l.getItemAtPosition(position);
                dialog.dismiss();
                showPage(itemValue.page);
            }
        });
        //some users have bookmarked first page, remove it
        int min = quranData.surahs[0].page,
                max = quranData.surahs[quranData.surahs.length - 1].page;
        for (int i = setting.bookmarks.size() - 1; i >= 0; --i) {
            int num = Integer.parseInt(setting.bookmarks.get(i).name);
            if (num < min || num > max)
                setting.bookmarks.remove(i);
        }
        final ListView l4 = (ListView) dialog.findViewById(R.id.listViewBookmarks);
        String[] book = new String[setting.bookmarks.size()];
        for (int i = 0; i < setting.bookmarks.size(); ++i) {
            String name = setting.bookmarks.get(i).name;
            book[i] = quranData.findSurahAtPage(Integer.parseInt(name)).name + ": " + name;
        }
        l4.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                book));
        l4.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String itemValue = (String) l4.getItemAtPosition(position);
                dialog.dismiss();
                showPage(Integer.parseInt(itemValue.substring(itemValue.indexOf(":") + 2)));
            }
        });
        Spinner spinner = (Spinner) dialog.findViewById(R.id.juzNumber);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.juzs));
        spinner = (Spinner) dialog.findViewById(R.id.hizbNumber);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.hizbs));
        Button b = (Button) dialog.findViewById(R.id.buttonGoto);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    EditText txt = (EditText) dialog.findViewById(R.id.editTextPageNum);
                    Spinner juz = (Spinner) dialog.findViewById(R.id.juzNumber);
                    Spinner hizb = (Spinner) dialog.findViewById(R.id.hizbNumber);
                    EditText sura = (EditText) dialog.findViewById(R.id.editTextSuraNum);
                    EditText ayah = (EditText) dialog.findViewById(R.id.editTextAyahNum);
                    if (!txt.getText().toString().trim().isEmpty()) {
                        int num = Integer.parseInt(txt.getText().toString());
                        if (num > 0 && num <= FullScreenImageAdapter.MAX_PAGE) {
                            dialog.dismiss();
                            showPage(num);
                        } else {
                            showError(String.format("أدخل رقم صفحة صحيح في المدى (1-%d)",
                                    FullScreenImageAdapter.MAX_PAGE));
                        }
                    } else if (juz.getSelectedItemPosition() > 0) {
                        ListItem item = (ListItem) juz.getSelectedItem();
                        dialog.dismiss();
                        showPage((Integer) item.value);
                    } else if (hizb.getSelectedItemPosition() > 0) {
                        ListItem item = (ListItem) hizb.getSelectedItem();
                        dialog.dismiss();
                        showPage((Integer) item.value);
                    } else {
                        String s = sura.getText().toString().trim();
                        String a = ayah.getText().toString().trim();
                        if (!s.isEmpty() && !a.isEmpty()) {
                            dialog.dismiss();
                            int ss = Integer.parseInt(s);
                            int aa = Integer.parseInt(a);
                            if (ss < 1 || ss > QuranData.AYAH_COUNT.length)
                                showError("رقم السورة غير صحيح");
                            else if (aa < 1 || aa > QuranData.AYAH_COUNT[ss - 1])
                                showError("رقم الآية غير صحيح");
                            else
                                showPage(db.getPage(ss, aa));
                        } else if (!s.isEmpty() || !a.isEmpty()) {
                            showError("الحقلان السورة والآية مطلوبان معا");
                        } else {
                            showError("فضلا قم بتعبئة أحد الحقول");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("فضلا أدخل أرقاما فقط، وفي المدى الصحيح");
                }
            }
        });
        dialog.show();
    }

    private void setBookmarkMenuItem(boolean add) {
        if (add) {
            findViewById(R.id.bookmarkBtn)
                    .setBackgroundResource(R.drawable.abc_btn_rating_star_on_mtrl_alpha);
        }
        else {
            findViewById(R.id.bookmarkBtn)
                    .setBackgroundResource(R.drawable.abc_btn_rating_star_off_mtrl_alpha);
        }
    }

    private void togglePlayButton(boolean playing) {
        Button btn = (Button) findViewById(R.id.listen);
        if (playing)
            btn.setBackgroundResource(android.R.drawable.ic_media_pause);
        else
            btn.setBackgroundResource(android.R.drawable.ic_media_play);
    }

    private String getSelectedSound() {
        return pref.getString("listReciters",
                getResources().getString(R.string.defaultReciter));
    }

    private void playRecite(final int fromSurah, final int fromAyah, final int toSurah,
                            final int toAyah) {
        QuranImageView image = getCurrentPage();
        if (image == null || image.currentPage == null) {
            Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتشغيل وإيقاف التلاوة",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (player != null) {
            stopPlayback();
            return;
        }
        final boolean repeat = !(fromSurah == -1 || fromAyah == -1
                || toSurah == -1 || toAyah == -1);
        togglePlayButton(true);
        final Shared currentAyaxIndex = new Shared();
        if (repeat) {
            autoSwipPage = true;
            showPage(db.getPage(fromSurah, fromAyah));
            image = getCurrentPage();
            allPagePlay = true;
            currentAyaxIndex.setData(-1);
            for (int i = 0; i < image.currentPage.ayahs.size(); ++i) {
                Ayah a = image.currentPage.ayahs.get(i);
                if (a.sura == fromSurah && a.ayah == fromAyah) {
                    currentAyaxIndex.setData(image.selectedAyahIndex = i);
                    image.invalidate();
                    break;
                }
            }
            if (currentAyaxIndex.getData() == -1) { // theoretically, this is impossible to happen
                Toast.makeText(this, "نأسف، حدث خطأ. حاول مرة أخرى", Toast.LENGTH_LONG).show();
                return;
            }
        }
        else {
            int sel = image.selectedAyahIndex; // prevent errors caused by other threads modifying this field
            if (sel < 0 ||
                    sel >= image.currentPage.ayahs.size()
                    || pref.getBoolean("playContinues", false)) {
                allPagePlay = true;
                if (sel < 0 || sel >= image.currentPage.ayahs.size()) {
                    currentAyaxIndex.setData(image.selectedAyahIndex = 0);
                    //even for Al Fatihah, let it 0 ayah الاستعاذة
                    image.invalidate();
                } else {// if (pref.getBoolean("playContinues", false)) {
                    currentAyaxIndex.setData(sel);
                }
            } else {
                currentAyaxIndex.setData(sel);
            }
        }
        try {
            bar.setVisibility(View.VISIBLE);
            player = new MediaPlayer();
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (listenRecite == null)
                        listenRecite = new HashSet<>();
                    listenRecite.add(getSelectedSound() + "," + sura + "," + ayah);
                    if (!allPagePlay) {
                        stopPlayback();
                    }
                    else {
                        QuranImageView image = getCurrentPage();
                        int next = QuranData.AYAH_COUNT[sura - 1] >= ayah + 1 ?
                                ayah + 1 : 1;
                        if (repeat) {
                            if (sura == toSurah && ayah == toAyah) {
                                sura = fromSurah; ayah = fromAyah;
                                autoSwipPage = true;
                                showPage(db.getPage(fromSurah, fromAyah));
                                image = getCurrentPage();
                                for (int i = 0; i < image.currentPage.ayahs.size(); ++i) {
                                    Ayah a = image.currentPage.ayahs.get(i);
                                    if (a.sura == fromSurah && a.ayah == fromAyah) {
                                        currentAyaxIndex.setData(i - 1); // will be increased later
                                        break;
                                    }
                                }
                            } else if (next <= ayah) {
                                if (++sura > QuranData.AYAH_COUNT.length) {
                                    if (pref.getBoolean("backToBegin", true)) {
                                        sura = ayah = 1;
                                        autoSwipPage = true;
                                        showPage(db.getPage(1, 1));
                                        image = getCurrentPage();
                                        currentAyaxIndex.setData(0);
                                    } else {
                                        stopPlayback();
                                        return;
                                    }
                                } else ayah = next;
                            } else {
                                ayah = next;
                            }
                        }
                        else {
                            if (next <= ayah) {
                                if (++sura > QuranData.AYAH_COUNT.length) {
                                    if (pref.getBoolean("backToBegin", true)) {
                                        sura = next = 1;
                                        autoSwipPage = true;
                                        showPage(db.getPage(1, 1));
                                        image = getCurrentPage();
                                        currentAyaxIndex.setData(0);
                                    } else {
                                        stopPlayback();
                                        return;
                                    }
                                }
                            }
                            ayah = next;
                        }

                        currentAyaxIndex.setData(image.selectedAyahIndex =
                                currentAyaxIndex.getData() + 1);
                        if (image.selectedAyahIndex == image.currentPage.ayahs.size()) {
                            autoSwipPage = true;
                            showPage(setting.page + 1);
                            image = getCurrentPage();
                            currentAyaxIndex.setData(image.selectedAyahIndex = 0);
                            if (image.currentPage.ayahs.get(0).ayah == 0)
                                currentAyaxIndex.setData(image.selectedAyahIndex =
                                        currentAyaxIndex.getData() + 1);
                        }
                        image.invalidate();
                        bar.setVisibility(View.VISIBLE);
                        try {
                            player.reset();
                            player.setDataSource(Utils.getAyahPath(MainActivity.this,
                                    getSelectedSound(), sura, ayah));
                            player.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                            bar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما لم يعد الملف موجودا",
                                    Toast.LENGTH_SHORT).show();
                            image.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                            image.invalidate();
                            togglePlayButton(false);
                        }
                    }
                }
            });
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (player != null) { //user closed/cancelled before prepare completes
                        player.start();
                    }
                    bar.setVisibility(View.GONE);
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    stopPlayback();
                    Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. تأكد من اتصالك بالانترنت",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Ayah a = image.currentPage.ayahs.get(currentAyaxIndex.getData());
            player.setDataSource(Utils.getAyahPath(MainActivity.this, getSelectedSound(),
                    sura = a.sura, ayah = a.ayah));
            player.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            bar.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما لم يعد الملف موجودا",
                    Toast.LENGTH_SHORT).show();
            player.release();
            player = null;
            togglePlayButton(false);
        }
    }

    private void initButtons() {
        Button btn = (Button) findViewById(R.id.bookmarkBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE ||
                        setting.page == 0) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لإضافة الصفحة الحالية للمفضلة",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                setBookmarkMenuItem(setting.toggleBookmark(setting.page));
                setting.save(MainActivity.this);
            }
        });
        btn = (Button) findViewById(R.id.gotoBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر للذهاب إلى موضع في المصحف",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                displayGotoDlg();
            }
        });
        btn = (Button) findViewById(R.id.listen);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                playRecite(-1, -1, -1, -1);
            }
        });
        btn = (Button) findViewById(R.id.tafseer);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final QuranImageView image = getCurrentPage();
                if (image == null || image.currentPage == null) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لعرض تفسير آية",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                final int sel = image.selectedAyahIndex; // prevent errors caused by other threads modifying this field
                if (sel < 0) {
                    Toast.makeText(MainActivity.this, "حدد آية لتفسيرها، بالضغط عليها مطولا",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("اختر التفسير");
                String tmp[] = null;
                ListItem[] tmp1 = null;
                TafseerDbManager db1 = null;
                File file = Utils.getTafaseerDbFile(MainActivity.this);
                boolean dbExists = file.exists();
                if (dbExists) {
                    try {
                        db1 = TafseerDbManager.getInstance(MainActivity.this);
                        tmp1 = db1.getAvailableTafaseer().toArray(new ListItem[0]);
                        tmp = new String[tmp1.length + 2];
                        tmp[0] = "التفسير الميسر";
                        tmp[tmp.length - 1] = "حذف التفاسير";
                        for (int i = 0; i < tmp1.length; ++i)
                            tmp[i + 1] = tmp1[i].name;
                    } catch (Exception ex) {
                        file.delete();
                        db1 = null;
                        tmp1 = null;
                        dbExists = false;
                    }
                }
                if (!dbExists)
                    tmp = new String[]{"التفسير الميسر", "تحميل 8 تفاسير أخرى (140 ميغا)"};
                final String items[] = tmp;
                final TafseerDbManager db = db1;
                final ListItem[] tafseers = tmp1;
                builder.setItems(items,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    displayTafseer(-1, db, sel, image.currentPage.ayahs, "التفسير الميسر");
                                } else if (items.length == 2) {
                                    final ProgressDialog show = new ProgressDialog(MainActivity.this);
                                    show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    show.setIndeterminate(false);
                                    show.setTitle("تحميل التفاسير: القرطبي وابن كثير والطبري وغيرهم");
                                    show.setMax(100);
                                    show.setProgress(0);
                                    show.show();
                                    final AsyncTask<Void, Integer, Integer> execute = new AsyncTask<Void, Integer, Integer>() {

                                        @Override
                                        protected Integer doInBackground(Void... params) {
                                            return Utils.downloadTafaseerDb(MainActivity.this,
                                                    new RecoverySystem.ProgressListener() {
                                                        @Override
                                                        public void onProgress(int progress) {
                                                            publishProgress(progress);
                                                        }
                                                    }, new CancelOperationListener() {
                                                        @Override
                                                        public boolean canContinue() {
                                                            return !isCancelled();
                                                        }
                                                    });
                                        }

                                        @Override
                                        protected void onProgressUpdate(Integer... values) {
                                            //super.onProgressUpdate(values);
                                            if (show.isShowing() && !isFinishing())
                                                show.setProgress(values[0]);
                                        }

                                        @Override
                                        protected void onPostExecute(Integer integer) {
                                            if (!show.isShowing() || isFinishing())
                                                return;
                                            show.dismiss();
                                            if (integer == Utils.DOWNLOAD_FILE_NOT_FOUND ||
                                                    integer == Utils.DOWNLOAD_IO_EXCEPTION)
                                                showError("فشل التحميل. تأكد من وجود مساحة كافية بالجهاز");
                                            else if (integer != Utils.DOWNLOAD_OK &&
                                                    integer != Utils.DOWNLOAD_USER_CANCEL)
                                                showError("فشل التحميل. تأكد من اتصالك بالشبكة");
                                            else if (integer == Utils.DOWNLOAD_OK)
                                                Utils.showAlert(MainActivity.this, "تحميل التفاسير",
                                                        "تم تحميل التفاسير بنجاح", null);
                                        }
                                    }.execute();
                                    show.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                            execute.cancel(true);
                                        }
                                    });
                                } else if (which - 1 < tafseers.length) {
                                    displayTafseer((int) tafseers[which - 1].value, db,
                                            sel, image.currentPage.ayahs,
                                            tafseers[which - 1].name);
                                } else
                                    Utils.showConfirm(MainActivity.this, "حذف التفاسير",
                                            "حذف التفسير المحملة وتحرير 140 ميغا والإبقاء فقط على التفسير الميسر؟", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Utils.getTafaseerDbFile(MainActivity.this).delete();
                                                }
                                            }, null);
                            }
                        });
                builder.show();
            }
        });
        btn = (Button) findViewById(R.id.repeat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتكرار تلاوة الآيات",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                displayRepeatDlg();
            }
        });
        btn = (Button) findViewById(R.id.shareAyat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لمشاركة الآيات",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                displayShareDlg();
            }
        });
    }

    private void displayAyahTafseerHelper(TafseerDbManager db2, int id, String name,
                                  ArrayList<Ayah> all, int idx,
                                          TextView tafseerTxt, TextView tafseerTitle) {
        Ayah a = all.get(currentAyahTafseerIdx = idx);
        String txt;
        if (viewTafseer == null)
            viewTafseer = new HashSet<>();
        if (db2 == null) {
            txt = db.getTafseer(a.sura, a.ayah);
            viewTafseer.add("ميسر" + a.sura + "," + a.ayah);
        } else {
            viewTafseer.add(name + ": " + a.sura + "," + a.ayah);
            txt = db2.getTafseer(id, a.sura, a.ayah);
        }
        tafseerTxt.setText(txt);
        tafseerTitle.setText("سورة " + quranData.surahs[a.sura - 1].name + ": " + a.ayah);
    }

    private void displayTafseer(final int tafseer, final TafseerDbManager db2,
                                int currnet, final ArrayList<Ayah> all, final String name) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_view_tafseer);
        final TextView textView = (TextView) dialog.findViewById(R.id.tafseerText);
        final TextView titleTextView = (TextView) dialog.findViewById(R.id.txtTafseerDlgTitle);
        textView.setTypeface(pref.getBoolean("fontBold", false) ?
                tradionalArabicFont : tradionalArabicBoldFont);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        displayAyahTafseerHelper(db2, tafseer, name, all, currnet, textView, titleTextView);
        dialog.findViewById(R.id.btnTafseerNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAyahTafseerIdx + 1 < all.size()) {
                    displayAyahTafseerHelper(db2, tafseer, name, all, currentAyahTafseerIdx + 1,
                            textView, titleTextView);
                }
            }
        });
        dialog.findViewById(R.id.btnTafseerPrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAyahTafseerIdx - 1 >= 0) {
                    displayAyahTafseerHelper(db2, tafseer, name, all, currentAyahTafseerIdx - 1,
                            textView, titleTextView);
                }
            }
        });
        dialog.setTitle("عرض تفسير: " + name);
        dialog.show();
    }

    private void displayRepeatDlg() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_repeat_recite);
        final Spinner spinner1 = (Spinner) dialog.findViewById(R.id.fromSurah);
        spinner1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.surahs2));
        final Spinner spinner2 = (Spinner) dialog.findViewById(R.id.toSurah);
        spinner2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.surahs2));
        QuranImageView image = getCurrentPage();
        if (image == null) {
            Toast.makeText(this, "يستخدم هذا الزر لتكرار التلاوة", Toast.LENGTH_LONG).show();
            return;
        }
        if (image.myBitmap == null) {
            AnalyticsTrackers.sendFatalError(MainActivity.this, "MainActivit.displayRepeatDlg",
                    "image.myBitmap == null (recycled ??)");
            Toast.makeText(this, "حدث خطأ ما.", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText from = (EditText) dialog.findViewById(R.id.fromAyah);
        final EditText to = (EditText) dialog.findViewById(R.id.toAyah);
        if (image.currentPage != null && image.currentPage.ayahs.size() >0) {
            spinner1.setSelection(image.currentPage.ayahs.get(0).sura);
            spinner2.setSelection(image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1).sura);
            from.setText(Math.max(1, image.currentPage.ayahs.get(0).ayah) + "");
            to.setText(Math.max(1, image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1).ayah) + "");
        }
        dialog.findViewById(R.id.buttonStartRecite).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String ff = from.getText().toString();
                String tt = to.getText().toString();
                if (spinner1.getSelectedItemPosition() < 1
                        || spinner2.getSelectedItemPosition() < 1
                        || ff.isEmpty() || tt.isEmpty()) {
                    showError("الرجاء تعبئة جميع الحقول");
                    return;
                }
                int f, t;
                try {
                    f = Integer.parseInt(ff);
                    t = Integer.parseInt(tt);
                } catch (Exception ex) {
                    showError("الأرقام غير صحيحة");
                    return;
                }
                if (f <= 0 || t <= 0) {
                    showError("رقم الآية يبدء من 1 فما فوق");
                    return;
                }
                int sf = (int) ((ListItem) spinner1.getSelectedItem()).value;
                int st = (int) ((ListItem) spinner2.getSelectedItem()).value;
                f = Math.min(f, QuranData.AYAH_COUNT[sf - 1]);
                t = Math.min(t, QuranData.AYAH_COUNT[st - 1]);
                if ((sf > st || sf == st && f > t)
                        && !pref.getBoolean("backToBegin", true)) {
                    showError("البداية يجب أن لا تكون أعلى من النهاية. فعل خيار البدء من الفاتحة للاستمرار");
                    return;
                }
                stopPlayback();
                playRecite(sf, f, st, t);
                dialog.dismiss();
            }
        });

        dialog.setTitle("تكرار التلاوة");
        dialog.show();
    }

    private void displayShareDlg() {
        if (!Utils.isExternalStorageWritable()) {
            Toast.makeText(this, "عفوا الذاكرة في جهازك غير قابلة للكتابة. لا يمكنك استخدام هذه الميزة",
                    Toast.LENGTH_LONG).show();
            return;
        }
        QuranImageView image = getCurrentPage();
        if (image == null || image.currentPage == null || image.currentPage.ayahs == null
                || image.currentPage.ayahs.size() == 0) {
            Toast.makeText(this, "يستخدم هذا الزر لمشاركة مجموعة من الآيات", Toast.LENGTH_LONG).show();
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_share_ayat_dlg);
        shareImageView = (QuranImageView) dialog.findViewById(R.id.shareQuranImageView);
        shareImageView.isMultiSelectMode = true;
        shareImageView.setImageBitmap(image.myBitmap);
        shareImageView.pref = pref;
        shareImageView.currentPage = image.currentPage;
        if (image.selectedAyahIndex >= 0)
            shareImageView.mutliSelectList.add(shareImageView.currentPage.ayahs.get(image.selectedAyahIndex));
        configOrientation(shareImageView);
        shareImageView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector detector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    super.onLongPress(e);
                    handle(e);
                }

                private void handle(MotionEvent e) {
                    int idx = shareImageView.getAyahAtPos(e.getX(), e.getY());
                    if (idx >= 0) {
                        Ayah a = shareImageView.currentPage.ayahs.get(idx);
                        if (!shareImageView.mutliSelectList.contains(a))
                            shareImageView.mutliSelectList.add(a);
                        else
                            shareImageView.mutliSelectList.remove(a);
                        shareImageView.invalidate();
                    }
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    handle(e);
                    return super.onSingleTapUp(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return false;
            }
        });
        dialog.findViewById(R.id.buttonShareImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareImageView.mutliSelectList.size() > 0) {
                    dialog.dismiss();
                    File path = new File(Environment.getExternalStorageDirectory(),
                            "shamraly_share.png");
                    shareImageView.saveSelectedAyatAsImage(path, quranData);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/png");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(path));
                    startActivity(Intent.createChooser(share, "مشاركة"));
                } else
                    showError("فضلا حدد آية أو أكثر");
            }
        });
        dialog.findViewById(R.id.buttonShareCopy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareImageView.mutliSelectList.size() > 0) {
                    String text = Utils.getAllAyahText(MainActivity.this,
                            shareImageView.mutliSelectList, quranData);
                    dialog.dismiss();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("مصحف الشمرلي", text));
                    Toast.makeText(MainActivity.this, "تم نسخ النص إلى الحافظة", Toast.LENGTH_LONG).show();
                } else
                    showError("فضلا حدد آية أو أكثر");
            }
        });
        dialog.findViewById(R.id.buttonShareText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shareImageView.mutliSelectList.size() > 0) {
                    String text = Utils.getAllAyahText(MainActivity.this,
                            shareImageView.mutliSelectList, quranData);
                    dialog.dismiss();
                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(sharingIntent, "مشاركة مجموعة من الآيات"));
                } else
                    showError("فضلا حدد آية أو أكثر");
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                shareImageView = null;
            }
        });
        dialog.setTitle("مشاركة آية أو أكثر");
        dialog.show();
    }

    private void calcTotalDeviceRam() {
        RandomAccessFile reader;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            // Get the Number value from the string
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(reader.readLine());
            String value = "";
            while (m.find()) {
                value = m.group(1);
            }
            reader.close();
            totalDeviceRamMg = (int) (Double.parseDouble(value) / 1024.0);
        } catch (Exception ex) {
            ex.printStackTrace();
            totalDeviceRamMg = -1;
        }
        System.out.println("Ram = " + totalDeviceRamMg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.getDatabaseDir(this) == null) {
            Toast.makeText(this,
                    "فشل بدء التطبيق. لا يمكن الكتابة في ذاكرة الجهاز",
                    Toast.LENGTH_LONG).show();
            AnalyticsTrackers.sendFatalError(this, "MainActivity.onCreate", "getDatabaseDir() == null");
            finish();
            return;
        }
        notDownloaded = Utils.getNonExistPagesFromFile(this);
        if (notDownloaded == null) {
            Toast.makeText(this, "فشل بدء التطبيق. أغلق التطبيق ثم افتحه ثانية", Toast.LENGTH_LONG).show();
            AnalyticsTrackers.sendFatalError(this, "MainActivity.onCreate",
                    "serializable == null");
            finish();
            return;
        }
        BitmapPool.reset();
        calcTotalDeviceRam();
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            recommendedRamMg = am.getMemoryClass();
        } catch (Exception ex) {
            ex.printStackTrace();
            recommendedRamMg = -1;
        }
        db = DbManager.getInstance(this);
        deleteAll();
        quranData = QuranData.getInstance(this);
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) this.findViewById(R.id.progressBar);
        tradionalArabicFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
        tradionalArabicBoldFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Bold.ttf");
        try {
            pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            rotationMode = Integer.parseInt(pref.getString("pageRotationMode",
                    getString(R.string.defaultPageRotationMode)));
            if (rotationMode == 1)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (rotationMode == 2)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setting = Setting.getInstance(this);
            initViewPager();
            initButtons();
            ActionBar bar = getActionBar();
            if (bar != null) bar.hide();
        } catch (Exception ex) { //views maybe null?
            ex.printStackTrace();
            AnalyticsTrackers.sendException(this, ex);
            Toast.makeText(this, "فشل بدء التطبيق.\n" + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null && adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
            Utils.showConfirm(this, "تحميل المصحف",
                    "مرحبا بك في تطبيق مصحف الشمرلي.\n نحتاج أولا قبل بدء استخدام التطبيق لتحميل المصحف على جهازك، وذلك حتى يمكنك استخدام التطبيق دون اتصال فيما بعد. البدء بالتحميل الآن؟",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadAll();
                        }
                    }, null);
        }
    }

    /**
     * Used for cleaning up old app version data
     * TODO: remove in later versions
     */
    private void deleteAll() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file;
                try {
                    if (Utils.isExternalStorageWritable()) {
                        file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "quran_Images");
                    } else {
                        file = new File(getFilesDir(), "quran_Images");
                    }
                    if (!file.exists()) return;
                    for (int idx = 1; idx <= FullScreenImageAdapter.MAX_PAGE; ++idx) {
                        File filename = new File(file, idx + "");
                        if (filename.exists()) {
                            System.out.println(filename);
                            filename.delete();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public static int calculateInSampleSize(int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (QuranData.NORMAL_PAGE_HEIGHT > reqHeight || QuranData.NORMAL_PAGE_WIDTH > reqWidth) {

            final int halfHeight = QuranData.NORMAL_PAGE_HEIGHT / 2;
            final int halfWidth = QuranData.NORMAL_PAGE_WIDTH / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static void manhattan(int[][] image) {
        // traverse from top left to bottom right
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                if (image[i][j] == 1) {
                    // first pass and pixel was on, it gets a zero
                    image[i][j] = 0;
                } else {
                    // pixel was off
                    // It is at most the sum of the lengths of the array
                    // away from a pixel that is on
                    image[i][j] = image.length + image[i].length;
                    // or one more than the pixel to the north
                    if (i > 0) {
                        image[i][j] = Math.min(image[i][j], image[i - 1][j] + 1);
                    }
                    // or one more than the pixel to the west
                    if (j > 0) {
                        image[i][j] = Math.min(image[i][j], image[i][j - 1] + 1);
                    }
                }
            }
        }
        // traverse from bottom right to top left
        for (int i = image.length - 1; i >= 0; i--) {
            for (int j = image[i].length - 1; j >= 0; j--) {
                // either what we had on the first pass
                // or one more than the pixel to the south
                if (i + 1 < image.length) {
                    image[i][j] = Math.min(image[i][j], image[i + 1][j] + 1);
                }
                // or one more than the pixel to the east
                if (j + 1 < image[i].length) {
                    image[i][j] = Math.min(image[i][j], image[i][j + 1] + 1);
                }
            }
        }
    }

    private static void dilate(int[][] image, int k) {
        manhattan(image);
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                image[i][j] = image[i][j] <= k ? 1 : 0;
            }
        }
    }

    private synchronized Bitmap readPage(int page, int pos) {
        int current = BitmapPool.exists(page);
        if (current >= 0) {
            int swap = BitmapPool.idx[pos];
            BitmapPool.idx[pos] = page;
            BitmapPool.idx[current] = swap;
            Bitmap swp = BitmapPool.POOL[pos];
            BitmapPool.POOL[pos] = BitmapPool.POOL[current];
            BitmapPool.POOL[current] = swp;
            return BitmapPool.POOL[pos];
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        Bitmap.Config config;
        //if (recommendedRamMg < 64)
            config = Bitmap.Config.RGB_565;
        //else
        //    config = Bitmap.Config.ARGB_8888;
        Display display = getWindowManager().getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        options.inSampleSize = calculateInSampleSize(p.x, p.y);
        options.inPreferredConfig = config;
        options.inMutable = true;
        File file = Utils.getPageFile(this, page);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (OutOfMemoryError e) {
            throw e;
        }
//        if (bitmap == null) {
//            //if not outOfMemory
//            if (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() >
//                    5 * 1024 * 1024)
//                file.delete();
//            return null;
//        }
        bitmap.setHasAlpha(true);
        current = pos;
        BitmapPool.idx[current] = page;
        if (BitmapPool.POOL[current] == null
                || BitmapPool.POOL[current].isRecycled()
                || page <= 3) {
            //causes crashes, since the bitmap is used in imageview
            //if (BitmapPool.POOL[current] != null && !BitmapPool.POOL[current].isRecycled())
            //    BitmapPool.POOL[current].recycle();
            BitmapPool.POOL[current] = page > 3 ?
                    Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config)
                    : bitmap;
        }
        if (page > 3) {
            final Bitmap tmp = BitmapPool.POOL[current];
            final int displayMode = Integer.parseInt(pref.getString("displayPageMode",
                    getString(R.string.defaultDisplayPageMode)));
            final boolean night =  displayMode == 2;
            int boldSize = Integer.parseInt(pref.getString("boldSize",
                    getString(R.string.defaultBoldSize)));
            Canvas c;
            int yellowColor = Color.rgb(255, 255, 225);
            if (boldSize < 0) {
                Paint invertPaint = night ? new Paint() : null;
                if (night) invertPaint.setColorFilter(filter);
                int color;
                if (displayMode == 0)
                    color = yellowColor;
                else if (displayMode == 2)
                    color = Color.BLACK;
                else color = Color.WHITE;
                tmp.eraseColor(color);  // set its background to white, or whatever color you want
                c = new Canvas(tmp);
                c.drawBitmap(bitmap, 0, 0, invertPaint);
            } else {
                tmp.eraseColor(Color.WHITE);
                c = new Canvas(tmp);
                Paint bitmapPaint = new Paint();
                //first convert bitmap to grey scale:
                bitmapPaint.setColorFilter(grayScaleFilter);
                c.drawBitmap(bitmap, 0, 0, bitmapPaint);
                //then convert the resulting bitmap to black and white using threshold matrix
                bitmapPaint.setColorFilter(thresholdFilter);
                c.drawBitmap(tmp, 0, 0, bitmapPaint);
                System.out.printf("%d, %d\n", tmp.getWidth(), tmp.getHeight());
                for (int i = 0; i < tmp.getHeight(); ++i) {
                    tmp.getPixels(dilationBuffer[i], 0, tmp.getWidth(), 0, i, tmp.getWidth(), 1);
                    for (int j = 0; j < dilationBuffer[i].length; ++j) {
                        if (dilationBuffer[i][j] == -1)
                            dilationBuffer[i][j] = 0;
                        else
                            dilationBuffer[i][j] = 1;
                    }
                }
                if (boldSize > 0)
                    dilate(dilationBuffer, boldSize);
                //-1 == 0xFFFFFF (WHITE)
                int background = !night ? displayMode == 0 ? yellowColor : -1 : 0;
                int font = !night ? 0 : -1;
                for (int i = 0; i < tmp.getHeight(); ++i) {
                    for (int j = 0; j < dilationBuffer[i].length; ++j)
                        if (dilationBuffer[i][j] == 1)
                            dilationBuffer[i][j] = font;
                        else
                            dilationBuffer[i][j] = background;
                    tmp.setPixels(dilationBuffer[i], 0, tmp.getWidth(), 0, i, tmp.getWidth(), 1);
                }
            }
            if (pref.getBoolean("showPageLeftRightIndicator", true)) {
                boolean isLeftPage = (page - 1) % 2 == 1;
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(night ? PorterDuff.Mode.LIGHTEN
                        : PorterDuff.Mode.DARKEN));
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                final int offset = 75;
                if (!isLeftPage) {
                    int first = night ? Color.BLACK : Color.WHITE,
                            second = Color.GRAY;
                    paint.setShader(new LinearGradient(offset, 0, 0, 0,
                            first, second, Shader.TileMode.MIRROR));
                    c.drawRect(tmp.getWidth() - offset, 0, tmp.getWidth(), tmp.getHeight(), paint);
                } else {
                    int first = Color.GRAY,
                            second = night ? Color.BLACK : Color.WHITE;
                    paint.setShader(new LinearGradient(0, 0, offset, 0,
                            first, second, Shader.TileMode.MIRROR));
                    c.drawRect(0, 0, offset, tmp.getHeight(), paint);
                }
            }
            if (!bitmap.isRecycled()) bitmap.recycle();
            bitmap = tmp;
            if (pref.getBoolean("showPageBorders", false)) {
                try {
                    InputStream stream = getAssets().open("page_template.png");
                    Bitmap pageBackground = BitmapFactory.decodeStream(stream, null, options);
                    stream.close();
                    c = new Canvas(pageBackground);
                    RectF rect = new RectF();
                    rect.set(161, 141,
                            161 + bitmap.getWidth(),// + 881,
                            141 + bitmap.getHeight()// + 1373
                            );
                    c.drawBitmap(bitmap, null, rect, null);
                    if (!bitmap.isRecycled()) bitmap.recycle();
                    BitmapPool.POOL[current] = bitmap = pageBackground;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    private void downloadAll() {
        final ProgressDialog show = new ProgressDialog(this);
        show.setTitle("تحميل المصحف كاملا");
        show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        show.setIndeterminate(false);
        final int MAX_PAGE = FullScreenImageAdapter.MAX_PAGE;
        show.setMax(MAX_PAGE);
        show.setProgress(0);
        show.show();
        final String downloadUrls[] = {
                getString(R.string.downloadPageUrl),
                getString(R.string.downloadPageUrl2)
        };
        final AsyncTask<Void, Integer, String[]> execute = new AsyncTask<Void, Integer, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                Thread[] threads = new Thread[4];
                final Shared progress = new Shared();
                final Utils.DownloadStatusArray error = new Utils.DownloadStatusArray(threads.length);
                progress.setData(MAX_PAGE - notDownloaded.size());
                for (int th = 0; th < threads.length; ++th) {
                    final int myIdx = th;
                    threads[th] = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            byte[] buf = new byte[1024];
                            while (!isCancelled() && error.isAllOk()) {
                                Integer per = notDownloaded.poll();
                                if (per == null) break;
                                boolean yes = false;
                                for (String url : downloadUrls) {
                                    String path = String.format(Locale.ENGLISH, url, per);
                                    int result = Utils.downloadPage(MainActivity.this,
                                            per, path, buf);
                                    error.status[myIdx].setData(result);
                                    if (result == Utils.DOWNLOAD_OK) {
                                        progress.increment();
                                        publishProgress(progress.getData());
                                        yes = true;
                                        break;
                                    }
                                }
                                if (!yes) notDownloaded.add(per);
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
                Utils.saveNonExistPagesToFile(getApplicationContext(), notDownloaded);
                int err = error.getFirstError();
                if (err == Utils.DOWNLOAD_MALFORMED_URL
                        || err == Utils.DOWNLOAD_SERVER_INVALID_RESPONSE)
                    return new String[]{"خطأ", "لا يمكن الاتصال بخادم التحميل. تأكد من اتصالك بالإنترنت أو حاول لاحقا"};
                else if (err == Utils.DOWNLOAD_IO_EXCEPTION
                        || err == Utils.DOWNLOAD_FILE_NOT_FOUND)
                    return new String[]{"خطأ", "لا يمكن كتابة الملف. تأكد من وجود مساحة كافية"};
                else if (!isCancelled() && err == Utils.DOWNLOAD_OK) {
                    return new String[]{"تحميل المصحف", "جميع الصفحات تم تحميلها بنجاح"};
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(final Integer... values) {
                if (!isFinishing() && show.isShowing())
                    show.setProgress(values[0]);
            }

            @Override
            protected void onCancelled() {
                //super.onCancelled();
                if (!isFinishing() && show.isShowing()) show.dismiss();
            }

            @Override
            protected void onPostExecute(String[] strings) {
                //super.onPostExecute(strings);
                if (!isFinishing() && show.isShowing()) {
                    show.dismiss();
                    Utils.showAlert(MainActivity.this, strings[0], strings[1], null);
                    if (strings[1] != null && strings[1].contains("نجاح")) {
                        initViewPagerAdapter();
                        AnalyticsTrackers.sendDownloadPages(MainActivity.this);
                    }
                }
            }
        }.execute();
        show.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                execute.cancel(true);
            }
        });
    }
}
class ListItem implements Serializable {
    String name;
    Object value;

    public ListItem() {
    }

    public ListItem(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return name;
    }
}
class Setting implements Serializable {
    private static Setting instnace;
    private static final String settingFilename = "myfile";

    int page = 1;
    String saveSoundsDirectory;
    ArrayList<ListItem> bookmarks;

    private Setting() {
    }

    @Nullable
    private ListItem getBookmark(int p) {
        for (ListItem i : bookmarks) {
            if (Integer.parseInt(i.name) == p)
                return i;
        }
        return null;
    }

    public boolean isBookmarked(int p) {
        return getBookmark(p) != null;
    }

    public boolean toggleBookmark(int p) {
        ListItem b = getBookmark(p);
        if (b == null) {
            b = new ListItem();
            b.name = p + "";
            bookmarks.add(b);
            return true;
        } else {
            bookmarks.remove(b);
            return false;
        }
    }

    public void save(Context context) {
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

    public static Setting getInstance(Context context) {
        if (instnace != null)
            return instnace;
        Setting setting = null;
        try {
            FileInputStream fis = context.openFileInput(settingFilename);
            ObjectInputStream is = new ObjectInputStream(fis);
            setting = (Setting) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (setting == null) {
            setting = new Setting();
            setting.bookmarks = new ArrayList<>();
        }
        return instnace = setting;
    }
}
class Surah {
    String name;
    int page;
    int index;

    @Override
    public String toString() {
        return "سورة " + name;
    }
}
class Shared {
    private final Lock lock = new ReentrantLock(true);
    private int _data;

    public void setData(int data) {
        try {
            lock.lock();
            _data = data;
        } finally {
            lock.unlock();
        }
    }

    public int getData() {
        try {
            lock.lock();
            return _data;
        } finally {
            lock.unlock();
        }
    }

    public void increment() {
        try {
            lock.lock();
            _data = _data + 1;
        } finally {
            lock.unlock();
        }
    }
}