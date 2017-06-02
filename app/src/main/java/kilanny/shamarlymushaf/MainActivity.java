package kilanny.shamarlymushaf;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.graphics.Rect;
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
import android.support.annotation.NonNull;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;
import kilanny.shamarlymushaf.util.MyOutOfMemoryException;
import kilanny.shamarlymushaf.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends FragmentActivity {

    private class FindPageSelectionResult {
        QuranImageView image;
        int selectionIndex;
        boolean isRight;
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        WeakReference<QuranImageView> currentImageView;
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

    private static final ColorMatrixColorFilter filter = new ColorMatrixColorFilter(
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
    private static final int yellowColor = Color.rgb(255, 255, 225);
    public static final String SHOW_PAGE_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage";
    public static final String SHOW_AYAH_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage#withAyah";
    FullScreenImageAdapter adapter;
    private ViewPager viewPager;
    private Setting setting;
    SharedPreferences pref;
    private DbManager db;
    private ProgressBar bar;
    private MediaPlayer player;
    private int sura, ayah;
    private boolean allPagePlay = false;
    private boolean autoSwipPage = false;
    private boolean autoHidePageInfo;
    private Typeface tradionalArabicFont, tradionalArabicBoldFont;
    private QuranImageView shareImageView;
    private QuranData quranData;
    private int totalDeviceRamMg;
    private int recommendedRamMg;
    private ConcurrentLinkedQueue<Integer> notDownloaded;
    private String initialHighlightAyah; // used for hilighting search result
    private int rotationMode;
    private int currentAyahTafseerIdx; //current Ayah displayed in Tafseerdlg navigation
    private int currentSelectedTafseer; //current tafseer selected in Tafseerdlg
    private boolean playRecitePageIsRight = false; // always false in single page mode
    private final Lock readPageLock = new ReentrantLock(true);
    private final Lock readBordersLock = new ReentrantLock(true);

    //google analytics fields
    private HashSet<Integer> pagesViewed;
    private Date startDate;
    private HashSet<String> listenRecite, viewTafseer;
    private final Timer autoCloseTimer = new Timer("autoCloseScreen");
    private final Shared idleUseCounter = new Shared();

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
                        listenRecite = null;
                        pagesViewed = null;
                        startDate = null;
                    }
                }).start();
            }
        }
        DbManager.dispose();
        autoCloseTimer.cancel();
        finish(); // prevent re-use the activity after stopping it (causes exceptions)
    }

    @Override
    protected void onDestroy() {
        try {
            autoCloseTimer.cancel();
            if (adapter != null)
                adapter.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        super.onDestroy();
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

    private boolean isDualPage() {
        return rotationMode == 2 && pref.getBoolean("displayDualPages", false);
    }

    private void initViewPagerAdapter() {
        adapter = new FullScreenImageAdapter(this, FullScreenImageAdapter.MAX_PAGE
                - notDownloaded.size(), isDualPage());
        if (notDownloaded.isEmpty())
            initAutoCloseTimer();
        final MyGestureDetector listener = new MyGestureDetector() {

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                if (adapter.isNotAllDownloaded()) {
                    downloadAll();
                    return;
                }
                idleUseCounter.setData(0);
                if (autoHidePageInfo) {
                    View pageInfo = getCurrentPageInfo();
                    if (pageInfo != null)
                        showAndSchedulePageInfoHide(pageInfo);
                }
                QuranImageView imageView = currentImageView.get();
                int idx = imageView != null ? imageView.getAyahAtPos(e.getX(), e.getY()) : -1;
                if (idx >= 0) {
                    stopPlayback();
                    imageView.selectedAyahIndex = idx;
                    imageView.invalidate();
                    if (isDualPage()) {
                        QuranImageView other = getCurrentPage(!isRightPage(imageView.currentPage.page));
                        if (other != null) {
                            other.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                            other.invalidate();
                        }
                    }
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
                if (adapter.isNotAllDownloaded())
                    downloadAll();
                else {
                    idleUseCounter.setData(0);
                    QuranImageView imageView = getCurrentPage(true);
                    if (imageView != null && imageView.selectedAyahIndex >= QuranImageView.SELECTION_ALL) {
                        imageView.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                        imageView.invalidate();
                    }
                    imageView = getCurrentPage(false);
                    if (imageView != null && imageView.selectedAyahIndex >= QuranImageView.SELECTION_ALL) {
                        imageView.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                        imageView.invalidate();
                    }
                    if (autoHidePageInfo) {
                        View pageInfo = getCurrentPageInfo();
                        if (pageInfo != null)
                            showAndSchedulePageInfoHide(pageInfo);
                    }
                }
                return false;
            }
        };
        final GestureDetector tapGestureDetector = new GestureDetector(this, listener);
        adapter.setInstantiateQuranImageViewListener(new FullScreenImageAdapter.OnInstantiateQuranImageViewListener() {
            @Override
            public void onInstantiate(final WeakReference<QuranImageView> image, View parent) {
                image.get().setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        listener.currentImageView = image;
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
        if (adapter.isNotAllDownloaded())
            viewPager.setCurrentItem(0);
        else {
            // displaying selected image first
            Intent i = getIntent();
            int page = i.getIntExtra(SHOW_PAGE_MESSAGE, -1);
            initialHighlightAyah = i.getStringExtra(SHOW_AYAH_MESSAGE);
            int max = FullScreenImageAdapter.MAX_PAGE / (isDualPage() ? 2 : 1);
            page = page == -1 ? setting.page : page;
            page = page < 1 || page > max ? 1 : page;
            showPage(page);
        }
    }

    @Nullable
    private QuranImageView getCurrentPage(boolean right) {
        try {
            return (QuranImageView) viewPager.findViewWithTag(setting.page)
                    .findViewById(!isDualPage() ? R.id.quranPage
                            : right ? R.id.quranPage_right : R.id.quranPage_left);
        } catch (NullPointerException ex){
            return null;
        }
    }

    @Nullable
    private View getCurrentPageInfo() {
        try {
            return viewPager.findViewWithTag(setting.page)
                    .findViewById(R.id.pageInfoLayout);
        } catch (NullPointerException ex){
            return null;
        }
    }

    private void initCurrentPageInfo(QuranImageView image, View parent) {
        View pageInfo = parent.findViewById(R.id.pageInfoLayout);
        if (adapter.isNotAllDownloaded() ||
                isShowPageBorders(pref) ||
                !pref.getBoolean("showPageInfo", true)) {
            pageInfo.setVisibility(View.GONE);
        } else if (image.currentPage != null && image.currentPage.ayahs != null
                && image.currentPage.ayahs.size() > 0) {
            pageInfo.setVisibility(View.VISIBLE);
            AutoScrollViewPager pager = (AutoScrollViewPager) parent.findViewById(R.id.pageTitleViewPager);
            int page = image.currentPage.page;
            String juz = "", hizb = "";
            ListItem j = quranData.findJuzAtPage(page);
            if (j != null)
                juz = j.name;
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

            if (autoHidePageInfo) {
                showAndSchedulePageInfoHide(pageInfo);
            }
        }
    }

    private void showAndSchedulePageInfoHide(@NonNull final View pageInfo) {
        final int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pageInfo.setVisibility(View.VISIBLE);
                pageInfo.animate().translationY(0).setDuration(duration)
                        .setListener(null);
            }
        });
        Runnable delayDone = new Runnable() {
            @Override
            public void run() {
                //no need to check HONEYCOMB_MR2 (3.2), app min is android 4.0
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pageInfo.animate()
                                .translationY(-pageInfo.getHeight())
                                .setDuration(duration)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        pageInfo.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
            }
        };
        mHideHandler.postDelayed(delayDone, AUTO_HIDE_DELAY_MILLIS * 2);
    }

    private void configOrientation(int orientation, boolean right) {
        QuranImageView image = getCurrentPage(right);
        if (image == null) return;
        configOrientation(orientation, image);
    }

    private void configOrientation(QuranImageView image) {
        configOrientation(getResources().getConfiguration().orientation, image);
    }

    private void configOrientation(int orientation, QuranImageView image) {
        if (isDualPage()) {
            image.setScaleType(ImageView.ScaleType.FIT_XY);
            return;
        }
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
        configOrientation(newConfig.orientation, false);
        configOrientation(newConfig.orientation, true);
        if (shareImageView != null)
            configOrientation(newConfig.orientation, shareImageView);
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
            private WeakReference<QuranImageView> last, last2;

            @Override
            public void onPageSelected(int position) {
                if (!autoSwipPage)
                    stopPlayback();
                else autoSwipPage = false;
                setting.page = adapter.getCount() - position; // correct also for dual mode
                if (pagesViewed == null) {
                    pagesViewed = new HashSet<>();
                    startDate = new Date();
                }
                int p = setting.page * (setting.lastWasDualPage ? 2 : 1);
                pagesViewed.add(p);
                setBookmarkMenuItem(setting.isBookmarked(p));
                setting.save(MainActivity.this);
                if (last != null && last.get() != null && last.get().myBitmap != null &&
                        !last.get().myBitmap.isRecycled()) {
                    last.get().selectedAyahIndex = QuranImageView.SELECTION_NONE;
                    last.get().invalidate();
                }
                try {
                    last = new WeakReference<>(getCurrentPage(false));
                    configOrientation(last.get());
                    if (isDualPage()) {
                        last2 = new WeakReference<>(getCurrentPage(true));
                        configOrientation(last2.get());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (pref.getBoolean("showHizbToast", true)) {
                    if (pref.getBoolean("showRob3Toast", false)) {
                        int low = 1, high = quranData.arba3.length - 1;
                        while (low <= high) {
                            int mid = (low + high) / 2;
                            int val = (int) quranData.arba3[mid].value;
                            if (val == p) {
                                String txt;
                                if ((mid - 1) % 8 == 0)
                                    txt = quranData.juzs[(mid - 1) / 8 + 1].name;
                                else if ((mid - 1) % 4 == 0)
                                    txt = quranData.hizbs[(mid - 1) / 4 + 1].name;
                                else
                                    txt = quranData.arba3[mid].name;
                                Toast.makeText(MainActivity.this, txt, Toast.LENGTH_SHORT).show();
                                break;
                            } else if (val < p)
                                low = mid + 1;
                            else
                                high = mid - 1;
                        }
                    } else {
                        // find hizb-juz
                        for (int i = 1; i < quranData.hizbs.length; ++i) {
                            int val = (int) quranData.hizbs[i].value;
                            if (val == p) {
                                String txt;
                                if (i % 2 == 1)
                                    txt = quranData.juzs[1 + i / 2].name;
                                else
                                    txt = quranData.hizbs[i].name;
                                Toast.makeText(MainActivity.this, txt, Toast.LENGTH_SHORT).show();
                                break;
                            } else if (val > p) break;
                        }
                    }
                }
                if (autoHidePageInfo) {
                    View pageInfo = getCurrentPageInfo();
                    if (pageInfo != null)
                        showAndSchedulePageInfoHide(pageInfo);
                }
            }
        });
        initViewPagerAdapter();
    }

    public void showPage(int pos) {
        viewPager.setCurrentItem(adapter.getCount() - pos);
    }

    private void showPageForRecite(int pos) {
        if (!isDualPage())
            showPage(pos);
        else {
            playRecitePageIsRight = isRightPage(pos);
            showPage(pos / 2);
        }
    }

    private void showError(String error) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(error);
        dlgAlert.setTitle("خطأ");
        dlgAlert.setPositiveButton("موافق", null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    private void displayReciteInBackgroundDlg() {
        final FindPageSelectionResult result = getCurrentPageSelected();
        if (result == null || result.image == null) {
            Toast.makeText(this, "يستخدم هذا الزر لتشغيل التلاوة في الخلفية", Toast.LENGTH_LONG).show();
            return;
        }
        final QuranImageView image = result.image;
        if (image.myBitmap == null) {
            AnalyticsTrackers.sendFatalError(MainActivity.this, "MainActivit.displayRepeatDlg",
                    "image.myBitmap == null (recycled ??)");
            Toast.makeText(this, "حدث خطأ ما.", Toast.LENGTH_LONG).show();
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_recite_inbackground);
        final Spinner stopPeriod = (Spinner) dialog.findViewById(R.id.spinnerAutostopBackgroundRecite);
        stopPeriod.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1,
                new ListItem[] {
                        new ListItem("خمس دقائق", 5),
                        new ListItem("عشر دقائق", 10),
                        new ListItem("ربع ساعة", 15),
                        new ListItem("نصف ساعة", 30),
                        new ListItem("ثلاثة أرباع الساعة", 45),
                        new ListItem("ساعة واحدة", 60),
                        new ListItem("ساعة ونصف", 90),
                        new ListItem("ساعتان", 120),
                        new ListItem("ثلاث ساعات", 180),
                        new ListItem("خمس ساعات", 300)
        }));
        final Spinner spinner1 = (Spinner) dialog.findViewById(R.id.fromSurahR);
        spinner1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.surahs2));
        final Spinner spinner2 = (Spinner) dialog.findViewById(R.id.toSurahR);
        spinner2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, quranData.surahs2));
        final EditText from = (EditText) dialog.findViewById(R.id.fromAyahR);
        final EditText to = (EditText) dialog.findViewById(R.id.toAyahR);
        if (image.currentPage != null && image.currentPage.ayahs.size() >0) {
            spinner1.setSelection(image.currentPage.ayahs.get(0).sura);
            spinner2.setSelection(image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1)
                    .sura);
            from.setText(Math.max(1, image.currentPage.ayahs.get(0).ayah) + "");
            to.setText(Math.max(1, image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1)
                    .ayah) + "");
        }
        final CheckBox checkBoxAutoStop = (CheckBox) dialog.findViewById(R.id.checkboxAutoStop);
        checkBoxAutoStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                stopPeriod.setEnabled(isChecked);
            }
        });
        final CheckBox checkBoxRepeat = (CheckBox) dialog.findViewById(R.id.checkBoxRepeat);
        checkBoxRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                spinner1.setClickable(isChecked);
                spinner2.setClickable(isChecked);
                from.setEnabled(isChecked);
                to.setEnabled(isChecked);
            }
        });
        dialog.findViewById(R.id.buttonStartReciteInBackground).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int autoStop;
                if (checkBoxAutoStop.isChecked()) {
                    autoStop = (int) ((ListItem) stopPeriod.getSelectedItem()).value;
                } else autoStop = 0;
                int sf, f, st = 0, t = 0;
                if (checkBoxRepeat.isChecked()) {
                    String ff = from.getText().toString();
                    String tt = to.getText().toString();
                    if (spinner1.getSelectedItemPosition() < 1
                            || spinner2.getSelectedItemPosition() < 1
                            || ff.isEmpty() || tt.isEmpty()) {
                        showError("الرجاء تعبئة جميع الحقول");
                        return;
                    }
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
                    sf = (int) ((ListItem) spinner1.getSelectedItem()).value;
                    st = (int) ((ListItem) spinner2.getSelectedItem()).value;
                    f = Math.min(f, quranData.surahs[sf - 1].ayahCount);
                    t = Math.min(t, quranData.surahs[st - 1].ayahCount);
                    if ((sf > st || sf == st && f > t)
                            && !pref.getBoolean("backToBegin", true)) {
                        showError("البداية يجب أن لا تكون أعلى من النهاية. فعل خيار البدء من الفاتحة للاستمرار");
                        return;
                    }
                } else {
                    Ayah a = image.currentPage.ayahs.get(result.selectionIndex >= 0 ?
                            result.selectionIndex : 0);
                    sf = a.sura; f = a.ayah;
                }
                Intent intent = new Intent(MainActivity.this, PlayReciteActivity.class);
                intent.putExtra(PlayReciteActivity.AUTO_STOP_PERIOD_MINUTES_EXTRA, autoStop);
                intent.putExtra(PlayReciteActivity.REPEAT_STRING_EXTRA, String.format("%d:%d-%d:%d",
                        sf, f, st, t));
                startActivity(intent);
                dialog.dismiss();
            }
        });
        dialog.setTitle("التلاوة بدون الشاشة");
        dialog.show();
    }

    private void setBookmarkMenuItem(boolean add) {
        if (add) {
            findViewById(R.id.bookmarkBtn)
                    .setBackgroundResource(android.R.drawable.star_big_on);
        }
        else {
            findViewById(R.id.bookmarkBtn)
                    .setBackgroundResource(android.R.drawable.star_big_off);
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
        return getSelectedSound(pref, this);
    }

    public static String getSelectedSound(SharedPreferences pref, Context context) {
        return pref.getString("listReciters",
                context.getString(R.string.defaultReciter));
    }

    public static void playBasmalah(final Context context, final String selectedSound,
                                    final QuranData quranData,
                                    final Runnable finishCallback) {
        final MediaPlayer player = new MediaPlayer();
        final Shared attempt = new Shared();
        attempt.setData(1);
        try {
            player.setDataSource(Utils.getAyahPath(context, selectedSound, 1, 1, quranData,
                    attempt.getData()));
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    player.start();
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    player.release();
                    finishCallback.run();
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    attempt.increment();
                    int num = attempt.getData();
                    if (num == 2) {
                        String path = Utils.getAyahPath(context, selectedSound, 1, 1,
                                quranData, num);
                        if (path == null) {
                            finishCallback.run();
                            return true;
                        }
                        try {
                            player.reset();
                            player.setDataSource(path);
                            player.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                            finishCallback.run();
                        }
                    } else finishCallback.run();
                    return true;
                }
            });
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            finishCallback.run();
        }
    }

    private boolean isRightPage(int page) {
        return page % 2 == 0;
    }

    private void playRecite(final int fromSurah, final int fromAyah, final int toSurah,
                            final int toAyah) {
        FindPageSelectionResult result = getCurrentPageSelected();
        if (result == null) {
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
        QuranImageView image;

        final Shared currentAyaxIndex = new Shared();
        if (repeat) {
            autoSwipPage = true;
            showPageForRecite(db.getPage(fromSurah, fromAyah));
            image = getCurrentPage(playRecitePageIsRight);
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
            FindPageSelectionResult selectionResult = getCurrentPageSelected();
            int sel = selectionResult.selectionIndex;
            image = selectionResult.image;
            if (image == null || image.currentPage == null) {
                //user clicks btn before loading completes
                Toast.makeText(this, "الرجاء انتظار تحميل الصفحة ثم حاول ثانية",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (isDualPage())
                playRecitePageIsRight = result.isRight;
            if (sel < 0 ||
                    sel >= image.currentPage.ayahs.size()
                    || pref.getBoolean("playContinues", false)) {
                allPagePlay = true;
                if (sel < 0 || sel >= image.currentPage.ayahs.size()) {
                    if (isDualPage())
                        playRecitePageIsRight = true;
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
        final Shared attempt = new Shared();
        attempt.setData(1);
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
                    } else {
                        QuranImageView image = getCurrentPage(playRecitePageIsRight);
                        int next = quranData.surahs[sura - 1].ayahCount >= ayah + 1 ?
                                ayah + 1 : 1;
                        if (repeat) {
                            if (sura == toSurah && ayah == toAyah) {
                                sura = fromSurah;
                                ayah = fromAyah;
                                autoSwipPage = true;
                                showPageForRecite(db.getPage(fromSurah, fromAyah));
                                image = getCurrentPage(playRecitePageIsRight);
                                for (int i = 0; i < image.currentPage.ayahs.size(); ++i) {
                                    Ayah a = image.currentPage.ayahs.get(i);
                                    if (a.sura == fromSurah && a.ayah == fromAyah) {
                                        currentAyaxIndex.setData(i - 1); // will be increased later
                                        break;
                                    }
                                }
                            } else if (next <= ayah) {
                                if (++sura > quranData.surahs.length) {
                                    if (pref.getBoolean("backToBegin", true)) {
                                        sura = ayah = 1;
                                        autoSwipPage = true;
                                        showPageForRecite(db.getPage(1, 1));
                                        image = getCurrentPage(playRecitePageIsRight);
                                        currentAyaxIndex.setData(0);
                                    } else {
                                        stopPlayback();
                                        return;
                                    }
                                } else ayah = next;
                            } else {
                                ayah = next;
                            }
                        } else {
                            if (next <= ayah) {
                                if (++sura > quranData.surahs.length) {
                                    if (pref.getBoolean("backToBegin", true)) {
                                        sura = next = 1;
                                        autoSwipPage = true;
                                        showPageForRecite(db.getPage(1, 1));
                                        image = getCurrentPage(playRecitePageIsRight);
                                        currentAyaxIndex.setData(0);
                                    } else {
                                        stopPlayback();
                                        return;
                                    }
                                }
                            }
                            ayah = next;
                        }
                        if (image == null || image.currentPage == null) {
                            stopPlayback();
                            return;
                        }
                        currentAyaxIndex.setData(image.selectedAyahIndex =
                                currentAyaxIndex.getData() + 1);
                        if (image.selectedAyahIndex == image.currentPage.ayahs.size()) {
                            autoSwipPage = true;
                            if (isDualPage() && playRecitePageIsRight) {
                                image = getCurrentPage(playRecitePageIsRight);
                                if (image != null) {
                                    image.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                                    image.invalidate();
                                }
                                image = getCurrentPage(playRecitePageIsRight = false);
                            } else {
                                showPageForRecite(isDualPage() ? setting.page * 2 + 2 :
                                        setting.page + 1);
                                image = getCurrentPage(playRecitePageIsRight);
                            }
                            currentAyaxIndex.setData(image.selectedAyahIndex = 0);
                            if (image.currentPage.ayahs.get(0).ayah == 0)
                                currentAyaxIndex.setData(image.selectedAyahIndex =
                                        currentAyaxIndex.getData() + 1);
                        }
                        image.invalidate();
                        bar.setVisibility(View.VISIBLE);
                        final QuranImageView image1 = image;
                        //attempt.setData(1);
                        //instead remember last working choice
                        Runnable tmpRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (player == null) //stopPlayback() was called
                                    return;
                                try {
                                    player.reset();
                                    player.setDataSource(Utils.getAyahPath(MainActivity.this,
                                            getSelectedSound(), sura, ayah, quranData, attempt.getData()));
                                    player.prepareAsync();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    bar.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما لم يعد الملف موجودا",
                                            Toast.LENGTH_SHORT).show();
                                    image1.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                                    image1.invalidate();
                                    togglePlayButton(false);
                                }
                            }
                        };
                        if (ayah == 1 && sura > 1 && sura != 9)
                            playBasmalah(MainActivity.this, getSelectedSound(), quranData,
                                    tmpRunnable);
                        else tmpRunnable.run();
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
                    attempt.increment();
                    if (attempt.getData() == 2) {
                        String path = Utils.getAyahPath(MainActivity.this, getSelectedSound(),
                                sura, ayah, quranData, 2);
                        if (path != null) {
                            try {
                                player.reset();
                                player.setDataSource(path);
                                player.prepareAsync();
                                return true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    stopPlayback();
                    Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Ayah a = image.currentPage.ayahs.get(currentAyaxIndex.getData());
            player.setDataSource(Utils.getAyahPath(MainActivity.this, getSelectedSound(),
                    sura = a.sura, ayah = a.ayah, quranData, 1));
            Runnable tmpRunnable = new Runnable() {
                @Override
                public void run() {
                    if (player == null) //stopPlayback() was called
                        return;
                    player.prepareAsync();
                }
            };
            if (ayah == 1 && sura > 1 && sura != 9)
                playBasmalah(this, getSelectedSound(), quranData, tmpRunnable);
            else tmpRunnable.run();
        } catch (Exception e) {
            e.printStackTrace();
            stopPlayback();
            Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما لم يعد الملف موجودا",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private FindPageSelectionResult getCurrentPageSelected() {
        QuranImageView img1 = getCurrentPage(true);
        QuranImageView img2 = getCurrentPage(false);
        if (img1 == null || img1.currentPage == null
                || img2 == null || img2.currentPage == null) {
            return null;
        }
        // prevent errors caused by other threads modifying this field
        int sel1 = img1.selectedAyahIndex;
        int sel2 = img2.selectedAyahIndex;
        if (sel1 < 0 && sel2 < 0) {
            FindPageSelectionResult res = new FindPageSelectionResult();
            res.image = img1;
            res.selectionIndex = -1;
            return res;
        }
        FindPageSelectionResult res = new FindPageSelectionResult();
        res.isRight = sel1 >= 0;
        res.image = res.isRight ? img1 : img2;
        res.selectionIndex = res.isRight ? sel1 : sel2;
        return res;
    }

    private void initButtons() {
        Button btn = (Button) findViewById(R.id.bookmarkBtn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.isNotAllDownloaded() || setting.page <= 1) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لإضافة الصفحة الحالية للمفضلة",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                setBookmarkMenuItem(setting.toggleBookmark(setting.page * (setting.lastWasDualPage ? 2 : 1)));
                setting.save(MainActivity.this);
            }
        });
        findViewById(R.id.listenBackground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.isNotAllDownloaded() || setting.page <= 1) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لاستماع التلاوة مع توفير البطارية",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                displayReciteInBackgroundDlg();
                //Toast.makeText(MainActivity.this, "سيتم توفير هذه الميزة قريبا إن شاء الله",
                //        Toast.LENGTH_LONG).show();
            }
        });
        btn = (Button) findViewById(R.id.listen);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (adapter.isNotAllDownloaded() || setting.page <= 1) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتشغيل التلاوة",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                playRecite(-1, -1, -1, -1);
            }
        });
        btn = (Button) findViewById(R.id.tafseer);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FindPageSelectionResult result = getCurrentPageSelected();
                if (result == null) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لعرض تفسير آية",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                final int sel = result.selectionIndex;
                if (sel < 0) {
                    Toast.makeText(MainActivity.this, "حدد آية لتفسيرها، بالضغط عليها مطولا",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                final QuranImageView image = result.image;
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
                                    displayTafseer(-1, db, tafseers, sel,
                                            image.currentPage.ayahs);
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
                                    displayTafseer(which, db, tafseers,
                                            sel, image.currentPage.ayahs);
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
                if (adapter.isNotAllDownloaded()) {
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
                if (adapter.isNotAllDownloaded() || setting.page <= 1) {
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
        if (id == -1) {
            txt = db.getTafseer(a.sura, a.ayah);
            viewTafseer.add("ميسر" + a.sura + "," + a.ayah);
        } else {
            viewTafseer.add(name + ": " + a.sura + "," + a.ayah);
            txt = db2.getTafseer(id, a.sura, a.ayah);
        }
        tafseerTxt.setText(txt);
        tafseerTitle.setText("سورة " + quranData.surahs[a.sura - 1].name + ": " + a.ayah);
    }

    private void displayTafseer(int tafseer, final TafseerDbManager db2,
                                final ListItem[] allTafaseer, int currnet,
                                final ArrayList<Ayah> all) {
        final boolean hasDownloadedTafaseer = allTafaseer != null && allTafaseer.length > 1;
        currentSelectedTafseer = hasDownloadedTafaseer ? Math.max(0, tafseer) : 0;
        ListItem[] items_tmp;
        if (hasDownloadedTafaseer) {
            items_tmp = new ListItem[allTafaseer.length + 1];
            System.arraycopy(allTafaseer, 0, items_tmp, 1, allTafaseer.length);
        } else {
            items_tmp = new ListItem[1];
        }
        items_tmp[0] = new ListItem();
        items_tmp[0].name = "التفسير الميسر";
        items_tmp[0].value = -1;
        final ListItem[] items = items_tmp;
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_view_tafseer);
        String theme = pref.getString("tafseerTheme",
                getString(R.string.defaultTafseerTheme));
        final TextView textView = (TextView) dialog.findViewById(R.id.tafseerText);
        final TextView titleTextView = (TextView) dialog.findViewById(R.id.txtTafseerDlgTitle);
        textView.setTypeface(pref.getBoolean("fontBold", false) ?
                tradionalArabicFont : tradionalArabicBoldFont);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        displayAyahTafseerHelper(db2,
                (Integer) items[currentSelectedTafseer].value,
                items[currentSelectedTafseer].name, all, currnet, textView, titleTextView);
        if (!pref.getBoolean("showTafseerNavigation", true)) {
            titleTextView.setVisibility(View.GONE);
            dialog.findViewById(R.id.btnTafseerNext).setVisibility(View.GONE);
            dialog.findViewById(R.id.btnTafseerPrev).setVisibility(View.GONE);
            dialog.findViewById(R.id.spinnerTafseer).setVisibility(View.GONE);
        }
        dialog.findViewById(R.id.btnTafseerNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAyahTafseerIdx + 1 < all.size()) {
                    displayAyahTafseerHelper(db2,
                            (Integer) items[currentSelectedTafseer].value,
                            items[currentSelectedTafseer].name, all, currentAyahTafseerIdx + 1,
                            textView, titleTextView);
                }
            }
        });
        dialog.findViewById(R.id.btnTafseerPrev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentAyahTafseerIdx - 1 >= 0) {
                    displayAyahTafseerHelper(db2, (Integer) items[currentSelectedTafseer].value,
                            items[currentSelectedTafseer].name, all, currentAyahTafseerIdx - 1,
                            textView, titleTextView);
                }
            }
        });
        Spinner s = (Spinner) dialog.findViewById(R.id.spinnerTafseer);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, items));
        s.setSelection(hasDownloadedTafaseer ? tafseer : 0);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                if (hasDownloadedTafaseer) {
                    currentSelectedTafseer = position;
                    displayAyahTafseerHelper(db2, (Integer) items[currentSelectedTafseer].value,
                            items[currentSelectedTafseer].name, all, currentAyahTafseerIdx,
                            textView, titleTextView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        dialog.setTitle("عرض التفسير");
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
        FindPageSelectionResult result = getCurrentPageSelected();
        if (result == null) {
            Toast.makeText(this, "يستخدم هذا الزر لتكرار التلاوة", Toast.LENGTH_LONG).show();
            return;
        }
        QuranImageView image = result.image;
        if (image.myBitmap == null) {
            AnalyticsTrackers.sendFatalError(MainActivity.this, "MainActivit.displayRepeatDlg",
                    "image.myBitmap == null (recycled ??)");
            Toast.makeText(this, "حدث خطأ ما.", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText from = (EditText) dialog.findViewById(R.id.fromAyah);
        final EditText to = (EditText) dialog.findViewById(R.id.toAyah);
        if (image.currentPage != null && image.currentPage.ayahs != null
                && image.currentPage.ayahs.size() > 0) {
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
                f = Math.min(f, quranData.surahs[sf - 1].ayahCount);
                t = Math.min(t, quranData.surahs[st - 1].ayahCount);
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

    private void displayShareDlg(QuranImageView image) {
        if (image.currentPage == null || image.currentPage.ayahs == null ||
                image.currentPage.ayahs.size() == 0) {
            Toast.makeText(this, "الصفحة المختارة لا تحتوي على آيات",
                    Toast.LENGTH_LONG).show();
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
                if (shareImageView != null) {
                    shareImageView.setImageBitmap(null);
                    shareImageView = null;
                }
            }
        });
        dialog.setTitle("مشاركة آية أو أكثر");
        dialog.show();
    }

    private void displayShareDlg() {
        if (!Utils.isExternalStorageWritable()) {
            Toast.makeText(this, "عفوا الذاكرة في جهازك غير قابلة للكتابة. لا يمكنك استخدام هذه الميزة",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!isDualPage()) {
            FindPageSelectionResult result = getCurrentPageSelected();
            if (result == null || result.image.currentPage == null) {
                Toast.makeText(this, "يستخدم هذا الزر لمشاركة مجموعة من الآيات",
                        Toast.LENGTH_LONG).show();
                return;
            }
            displayShareDlg(result.image);
        }
        else {
            Utils.showSelectionDlg(this, "مشاركة آية أو أكثر",
                    new String[] {"من الصفحة اليمنى", "من الصفحة اليسرى"}, true,
                    new DialogInterface.OnClickListener() {
                    @Override
                public void onClick(DialogInterface dialog, int which) {
                        displayShareDlg(getCurrentPage(which == 0));
                }
            }, null);
        }
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

    private void initAutoCloseTimer() {
        autoCloseTimer.scheduleAtFixedRate(new TimerTask() {

            private long period = Long.parseLong(pref.getString("autoCloseScreenMinutes",
                    getString(R.string.defaultAutoCloseScreenValue)));

            @Override
            public void run() {
                idleUseCounter.increment();
                if (period <= idleUseCounter.getData()) {
                    autoCloseTimer.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "انتهت مهلة عدم الاستخدام. تم إغلاق الشاشة آليا لتوفير البطارية",
                                    Toast.LENGTH_LONG).show();
                            if (!isFinishing()) finish();
                        }
                    });
                }
            }
        }, 60000, 60000);
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
        try {
            calcTotalDeviceRam();
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            recommendedRamMg = am.getMemoryClass();
        } catch (Exception ex) {
            ex.printStackTrace();
            recommendedRamMg = -1;
        }
        try {
            db = DbManager.getInstanceWithTest(this);
            deleteAll();
            quranData = QuranData.getInstance(this);
            setContentView(R.layout.activity_main);
            bar = (ProgressBar) this.findViewById(R.id.progressBar);
            tradionalArabicFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
            tradionalArabicBoldFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Bold.ttf");
            pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            autoHidePageInfo = pref.getBoolean("showPageInfo", true) &&
                    pref.getBoolean("autoHidePageInfo", false);
            rotationMode = Integer.parseInt(pref.getString("pageRotationMode",
                    getString(R.string.defaultPageRotationMode)));
            if (rotationMode == 1)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else if (rotationMode == 2)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setting = Setting.getInstance(this);
            boolean current = isDualPage();
            if (current != setting.lastWasDualPage) {
                if (current) setting.page /= 2;
                else setting.page *= 2;
                if (setting.page < 1 || setting.page > FullScreenImageAdapter.MAX_PAGE)
                    setting.page = 1;
                setting.lastWasDualPage = current;
            }
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
        if (adapter != null && adapter.isNotAllDownloaded()) {
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

    /**
     * Helper method for image processing (dilation)
     */
    private static void manhattan(int width, int height) {
        // traverse from top left to bottom right
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (dilationBuffer[i][j] == 1) {
                    // first pass and pixel was on, it gets a zero
                    dilationBuffer[i][j] = 0;
                } else {
                    // pixel was off
                    // It is at most the sum of the lengths of the array
                    // away from a pixel that is on
                    dilationBuffer[i][j] = dilationBuffer.length + dilationBuffer[i].length;
                    // or one more than the pixel to the north
                    if (i > 0) {
                        dilationBuffer[i][j] = Math.min(dilationBuffer[i][j], dilationBuffer[i - 1][j] + 1);
                    }
                    // or one more than the pixel to the west
                    if (j > 0) {
                        dilationBuffer[i][j] = Math.min(dilationBuffer[i][j], dilationBuffer[i][j - 1] + 1);
                    }
                }
            }
        }
        // traverse from bottom right to top left
        for (int i = height - 1; i >= 0; i--) {
            for (int j = width - 1; j >= 0; j--) {
                // either what we had on the first pass
                // or one more than the pixel to the south
                if (i + 1 < height) {
                    dilationBuffer[i][j] = Math.min(dilationBuffer[i][j], dilationBuffer[i + 1][j] + 1);
                }
                // or one more than the pixel to the east
                if (j + 1 < width) {
                    dilationBuffer[i][j] = Math.min(dilationBuffer[i][j], dilationBuffer[i][j + 1] + 1);
                }
            }
        }
    }

    private static void imageProcessing(final int k, final Bitmap tmp,
                                 int displayMode, boolean night) {
        Thread[] threads = new Thread[4];
        final Shared shared = new Shared();
        final Lock lock = new ReentrantLock(true);
        final Lock lock2 = new ReentrantLock(true);
        final Condition condition = lock.newCondition();
        shared.setData(0);
        final int width = tmp.getWidth(), height = tmp.getHeight();
        int threadWork = height / threads.length;
        if (k == 0) { //no dilation, just set the font/background colors of the thresholded image
            final int background = !night ? displayMode == 0 ? yellowColor : -1 : 0;
            final int font = !night ? 0 : -1;
            for (int idx = 0; idx < threads.length; ++idx) {
                final int myStart = threadWork * idx,
                        myEnd = idx == threads.length - 1 ? height : (idx + 1) * threadWork;
                threads[idx] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = myStart; i < myEnd; ++i) {
                            lock2.lock();
                            tmp.getPixels(dilationBuffer[i], 0, width, 0, i, width, 1);
                            lock2.unlock();
                            for (int j = 0; j < width; ++j) {
                                if (dilationBuffer[i][j] == -1)
                                    dilationBuffer[i][j] = background;
                                else
                                    dilationBuffer[i][j] = font;
                            }
                            lock2.lock();
                            tmp.setPixels(dilationBuffer[i], 0, width, 0, i, width, 1);
                            lock2.unlock();
                        }
                        lock.lock();
                        shared.increment();
                        condition.signal();
                        lock.unlock();
                    }
                });
            }
            for (Thread thread : threads)
                thread.start();
            lock.lock();
            while (shared.getData() < threads.length) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            lock.unlock();
            return;
        }

        //else, prepare for dilation (set all 0/1)
        //http://blog.ostermiller.org/dilate-and-erode
        for (int idx = 0; idx < threads.length; ++idx) {
            final int myStart = threadWork * idx,
                    myEnd = idx == threads.length - 1 ? height : (idx + 1) * threadWork;
            threads[idx] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = myStart; i < myEnd; ++i) {
                        lock2.lock();
                        tmp.getPixels(dilationBuffer[i], 0, width, 0, i, width, 1);
                        lock2.unlock();
                        for (int j = 0; j < width; ++j) {
                            if (dilationBuffer[i][j] == -1 || dilationBuffer[i][j] == 1)
                                dilationBuffer[i][j] = 0;
                            else
                                dilationBuffer[i][j] = 1;
                        }
                    }
                    lock.lock();
                    shared.increment();
                    condition.signal();
                    lock.unlock();
                }
            });
        }
        for (Thread thread : threads)
            thread.start();
        lock.lock();
        while (shared.getData() < threads.length) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        lock.unlock();
        //dilate
        manhattan(width, height);
        //restore colors of font/background
        final int background = !night ? displayMode == 0 ? yellowColor : -1 : 0;
        final int font = !night ? 0 : -1;
        shared.setData(0);
        for (int idx = 0; idx < threads.length; ++idx) {
            final int myStart = threadWork * idx,
                    myEnd = idx == threads.length - 1 ? height : (idx + 1) * threadWork;
            threads[idx] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = myStart; i < myEnd; ++i) {
                        for (int j = 0; j < width; ++j)
                            if (dilationBuffer[i][j] <= k)
                                dilationBuffer[i][j] = font;
                            else
                                dilationBuffer[i][j] = background;
                        lock2.lock();
                        tmp.setPixels(dilationBuffer[i], 0, width, 0, i, width, 1);
                        lock2.unlock();
                    }
                    lock.lock();
                    shared.increment();
                    condition.signal();
                    lock.unlock();
                }
            });
        }
        for (Thread thread : threads)
            thread.start();
        lock.lock();
        while (shared.getData() < threads.length) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        lock.unlock();
    }

    public Bitmap readBorders(int page) {
                       /*
Rect bounds (x, y, w, h)
Main page (161, 141, 881, 1373)
Title (509, 73, 183, 58)
Page number (561, 1528, 75, 38)
                 */
        readBordersLock.lock();
        checkOutOfMemory();
        try {
            InputStream stream = getAssets().open("page_template.png");
            //FileInputStream fs = (FileInputStream) stream;
            //FileChannel channel = stream.getChannel();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inMutable = true;
            options.inSampleSize = calculateInSampleSize(QuranData.BORDERED_PAGE_WIDTH,
                    QuranData.BORDERED_PAGE_HEIGHT);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            //FileLock lock = channel.lock();
            Bitmap pageBackground = BitmapFactory.decodeStream(stream, null, options);
            //lock.release();
            stream.close();
            Canvas c = new Canvas(pageBackground);
//            RectF rect = new RectF();
//            rect.set(161, 141,
//                    161 + bitmap.getWidth(),// + 881,
//                    141 + bitmap.getHeight()// + 1373
//            );
//            c.drawBitmap(bitmap, null, rect, null);
//            bitmap.recycle();
            int textColor = Color.rgb(162, 86, 65);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(textColor);
            paint.setAntiAlias(true);
            paint.setTypeface(tradionalArabicBoldFont);
            paint.setTextSize(25);
            Rect textBounds = new Rect();
            String text = isRightPage(page) ?
                    quranData.findJuzAtPage(page).name :
                    "سورة " + quranData.findSurahAtPage(page).name;
            paint.getTextBounds(text, 0, text.length(), textBounds);
            Rect rect = new Rect(509, 73, 509 + 183, 73 + 58);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(text, rect.exactCenterX(), rect.bottom - 10, paint);
            text = ArabicNumbers.convertDigits(page + "");
            paint.getTextBounds(text, 0, text.length(), textBounds);
            paint.setTextSize(35);
            rect.set(561, 1528, 561 + 75, 1528 + 38);
            c.drawText(text, rect.exactCenterX(), rect.exactCenterY() + 10, paint);
            if (Integer.parseInt(pref.getString("displayPageMode",
                    getString(R.string.defaultDisplayPageMode))) == 2) {
                checkOutOfMemory();
                Bitmap tmp = Bitmap.createBitmap(pageBackground.getWidth(),
                        pageBackground.getHeight(), options.inPreferredConfig);
                c = new Canvas(tmp);
                paint = new Paint();
                paint.setColorFilter(filter);
                c.drawBitmap(pageBackground, 0,0 , paint);
                pageBackground.recycle();
                pageBackground = tmp;
            }
            return pageBackground;
        } catch (Exception e) {
            e.printStackTrace();
            AnalyticsTrackers.sendException(this, e);
            return null;
        } finally {
            readBordersLock.unlock();
        }
    }

    public static boolean isShowPageBorders(SharedPreferences pref) {
        return pref.getBoolean("showPageBorders", false);
    }

    private static void checkOutOfMemory() {
        Runtime runtime = Runtime.getRuntime();
        long mem = runtime.maxMemory(),
                used = runtime.totalMemory();
        if (mem - used < 5 * 1024 * 1024)
            throw new MyOutOfMemoryException(mem, used);
    }

    public Bitmap readPage(int page)
            throws OutOfMemoryError, MyOutOfMemoryException {
        try {
            readPageLock.lock(); //it uses a single one dilation buffer,
            // so only one thread can use at time
            checkOutOfMemory();
            if (page == FullScreenImageAdapter.MAX_PAGE + 1) // can happen in dual mode
                page = 1;
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
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap == null) {
                //try again
                try {
                    options.inDither = false;
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                } catch (OutOfMemoryError e) {
                    throw e;
                }
                if (bitmap == null) {
                    //file.delete(); //corrupt file
                    return null;
                }
            }
            bitmap.setHasAlpha(true);
            if (page > 3)
                try {
                    checkOutOfMemory(); //check first before allocating another bitmap
                } catch (MyOutOfMemoryException ex) {
                    bitmap.recycle();
                    throw ex;
                }
            Bitmap work = page > 3 ?
                    Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config)
                    : bitmap;
            if (page > 3) {
                final int displayMode = Integer.parseInt(pref.getString("displayPageMode",
                        getString(R.string.defaultDisplayPageMode)));
                final boolean night = displayMode == 2;
                int boldSize = Integer.parseInt(pref.getString("boldSize",
                        getString(R.string.defaultBoldSize)));
                Canvas c;
                if (boldSize < 0) {
                    Paint invertPaint = night ? new Paint() : null;
                    if (night) invertPaint.setColorFilter(filter);
                    int color;
                    if (displayMode == 0)
                        color = yellowColor;
                    else if (displayMode == 2)
                        color = Color.BLACK;
                    else color = Color.WHITE;
                    work.eraseColor(color);  // set its background to white, or whatever color you want
                    c = new Canvas(work);
                    c.drawBitmap(bitmap, 0, 0, invertPaint);
                } else {
                    work.eraseColor(Color.WHITE);
                    c = new Canvas(work);
                    Paint bitmapPaint = new Paint();
                    //first convert bitmap to grey scale:
                    bitmapPaint.setColorFilter(grayScaleFilter);
                    c.drawBitmap(bitmap, 0, 0, bitmapPaint);
                    //then convert the resulting bitmap to black and white using threshold matrix
                    bitmapPaint.setColorFilter(thresholdFilter);
                    c.drawBitmap(work, 0, 0, bitmapPaint);
                    imageProcessing(boldSize, work, displayMode, night);
                }
                boolean isLeftPage = (page - 1) % 2 == 1;
                bitmap.recycle();
                bitmap = work;
                if (!isShowPageBorders(pref) && pref.getBoolean("showPageLeftRightIndicator", true)) {
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setXfermode(new PorterDuffXfermode(night ? PorterDuff.Mode.LIGHTEN
                            : PorterDuff.Mode.DARKEN));
                    paint.setStyle(Paint.Style.FILL_AND_STROKE);
                    final int offset = 75;
                    if (!isLeftPage) {
                        int first = night ? Color.BLACK :
                                displayMode == 0 ? yellowColor : Color.WHITE,
                                second = Color.GRAY;
                        paint.setShader(new LinearGradient(offset, 0, 0, 0,
                                first, second, Shader.TileMode.MIRROR));
                        c.drawRect(bitmap.getWidth() - offset, 0,
                                bitmap.getWidth(), bitmap.getHeight(), paint);
                    } else {
                        int first = Color.GRAY,
                                second = night ? Color.BLACK :
                                        displayMode == 0 ? yellowColor : Color.WHITE;
                        paint.setShader(new LinearGradient(0, 0, offset, 0,
                                first, second, Shader.TileMode.MIRROR));
                        c.drawRect(0, 0, offset, bitmap.getHeight(), paint);
                    }
                }
            }
            return bitmap;
        } finally {
            readPageLock.unlock();
        }
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
                try {
                    if (!isFinishing() && show.isShowing()) show.dismiss();
                } catch (Exception ex) {
                }
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
        //user close activity?
        show.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!execute.isCancelled())
                    execute.cancel(true);
            }
        });
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