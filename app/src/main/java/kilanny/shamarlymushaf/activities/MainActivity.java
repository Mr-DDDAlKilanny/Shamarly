package kilanny.shamarlymushaf.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
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
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.FullScreenImageAdapter;
import kilanny.shamarlymushaf.adapters.PageInfoAdapter;
import kilanny.shamarlymushaf.data.Ayah;
import kilanny.shamarlymushaf.data.DbManager;
import kilanny.shamarlymushaf.data.Khatmah;
import kilanny.shamarlymushaf.data.ListItem;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.data.Shared;
import kilanny.shamarlymushaf.data.TafseerDbManager;
import kilanny.shamarlymushaf.fragments.QuickSettingsFragment;
import kilanny.shamarlymushaf.services.PlayReciteService;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.ArabicNumbers;
import kilanny.shamarlymushaf.util.MyOutOfMemoryException;
import kilanny.shamarlymushaf.util.SystemUiHider;
import kilanny.shamarlymushaf.util.Utils;
import kilanny.shamarlymushaf.views.QuranImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends FragmentActivity implements QuickSettingsFragment.Callbacks {

    private static class FindPageSelectionResult {
        QuranImageView image;
        int selectionIndex;
        boolean isRight;
    }

    private static class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
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

    public static final String EXTRA_KHATMAH_NAME = "EXTRA_KHATMAH_NAME";

    private static final ColorMatrixColorFilter nightFilter = new ColorMatrixColorFilter(
            new ColorMatrix(new float[] {
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
    public static final String SHOW_PAGE_MESSAGE = "kilanny.shamarlymushaf.activities.MainActivity.showPage";
    public static final String SHOW_AYAH_MESSAGE = "kilanny.shamarlymushaf.activities.MainActivity.showPage#withAyah";


    public FullScreenImageAdapter adapter;
    private ImageButton mQuickSettingsButton;
    private boolean mQuickSettingsButtonVisible;
    private ViewPager viewPager;
    private Setting setting;
    public SharedPreferences pref;
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
    private String currentKhatmahName;
    private final Lock readPageLock = new ReentrantLock(true);
    private final Lock readBordersLock = new ReentrantLock(true);

    //google analytics fields
    private Date tafseerPageStartDate, quranPageStartDate;
    private int lastTafId, lastTafSura, lastTafAyah, lastPage;
    private String sessionId;
    private static boolean lastRecitedAyahWasFile = false;
    private final Timer autoCloseTimer = new Timer("autoCloseScreen");
    private final Shared idleUseCounter = new Shared();
    private Dialog tafseerDialog;

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();

        DbManager.dispose();
        autoCloseTimer.cancel();
        if (adapter != null) {
            adapter.recycle();
            adapter = null;
        }
        finish(); // prevent re-use the activity after stopping it (causes exceptions)
    }

    private void sendTafseer() {
        if (tafseerPageStartDate != null) {
            final long diff = new Date().getTime() - tafseerPageStartDate.getTime();
            if (diff >= 5 * 1000) {
                AnalyticsTrackers.getInstance(this).sendTafseerStats(sessionId,
                        lastTafId, lastTafSura, lastTafAyah);
            }
            tafseerPageStartDate = null;
        }
    }

    @Override
    protected void onDestroy() {
        autoCloseTimer.cancel();
        if (adapter != null) {
            adapter.recycle();
            adapter = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (tafseerDialog != null && tafseerDialog.isShowing()) {
            sendTafseer();
            tafseerDialog.dismiss();
            tafseerDialog = null;
        }
        super.onPause();
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
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isDualPage() {
        return rotationMode == 2 && pref.getBoolean("displayDualPages", false);
    }

    private void initViewPagerAdapter() {
        adapter = new FullScreenImageAdapter(this, FullScreenImageAdapter.MAX_PAGE
                - notDownloaded.size(), isDualPage(), autoHidePageInfo);
        if (notDownloaded.isEmpty())
            initAutoCloseTimer();
        final MyGestureDetector listener = new MyGestureDetector() {

            @Override
            public void onLongPress(MotionEvent e) {
                if (adapter == null) // recycled
                    return;
                super.onLongPress(e);
                if (adapter.isNotAllDownloaded()) {
                    downloadAll();
                    return;
                }
                idleUseCounter.setData(0);
                if (autoHidePageInfo &&
                        !adapter.isNotAllDownloaded() &&
                        !isShowPageBorders(pref) &&
                        pref.getBoolean("showPageInfo", true)) {
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
                if (adapter == null) // recycled
                    return false;
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
                    if (autoHidePageInfo &&
                            !adapter.isNotAllDownloaded() &&
                            !isShowPageBorders(pref) &&
                            pref.getBoolean("showPageInfo", true)) {
                        View pageInfo = getCurrentPageInfo();
                        if (pageInfo != null)
                            showAndSchedulePageInfoHide(pageInfo);
                    }
                    if (mQuickSettingsButton != null && !adapter.isNotAllDownloaded())
                        showAndScheduleSettingButtonHide(true);
                }
                return false;
            }
        };
        final GestureDetector tapGestureDetector = new GestureDetector(this, listener);
        adapter.setInstantiateQuranImageViewListener((image, parent) -> {
            image.get().setOnTouchListener((v, event) -> {
                listener.currentImageView = image;
                tapGestureDetector.onTouchEvent(event);
                return false;
            });
            if (image.get().currentPage != null)
                Log.d("onInstantiate", "at page " + image.get().currentPage.page);
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
            if (page == FullScreenImageAdapter.MAX_PAGE)
                onPageSelected(adapter.getCount() - page);
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
            AutoScrollViewPager pager = parent.findViewById(R.id.pageTitleViewPager);
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
            if (currentKhatmahName != null)
                adapter.setKhatmahName(currentKhatmahName);
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
        runOnUiThread(() -> {
            pageInfo.setVisibility(View.VISIBLE);
            pageInfo.animate().translationY(0).setDuration(duration)
                    .setListener(null);
        });
        mHideHandler.postDelayed(() -> runOnUiThread(() ->
                pageInfo.animate().translationY(-pageInfo.getHeight()).setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        pageInfo.setVisibility(View.GONE);
                    }
                })), AUTO_HIDE_DELAY_MILLIS * 2);
    }

    private void showAndScheduleSettingButtonHide(boolean show) {
        if (show && mQuickSettingsButtonVisible) {
            delayedHide(mHideSettingButtonRunnable, 1);
            return;
        }
        if (show) {
            runOnUiThread(() -> {
                mQuickSettingsButton.setVisibility(View.VISIBLE);
                mQuickSettingsButton.setRotation(0);
                ViewCompat.animate(mQuickSettingsButton)
                        .rotation(360)
                        .translationX(0)
                        .alpha(1)
                        .withLayer()
                        .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(View view) {
                                super.onAnimationEnd(view);
                                mQuickSettingsButtonVisible = true;
                            }
                        })
                        .start();
            });
        }
        delayedHide(mHideSettingButtonRunnable, AUTO_HIDE_DELAY_MILLIS);
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

            @Override
            public void onPageSelected(int position) {
                MainActivity.this.onPageSelected(position);
            }
        });
        initViewPagerAdapter();
    }

    private WeakReference<QuranImageView> last, last2;

    private void onPageSelected(int position) {
        Log.d("onPageSelect", "page " + position);
        if (adapter == null) // recycled
            return;
        idleUseCounter.setData(0);
        if (!autoSwipPage)
            stopPlayback();
        else autoSwipPage = false;
        setting.page = adapter.getCount() - position; // correct also for dual mode
        if (sessionId == null)
            sessionId = Utils.newUid();

        int p = setting.page * (setting.lastWasDualPage ? 2 : 1);
        if (quranPageStartDate != null) {
            final long diff = new Date().getTime() - quranPageStartDate.getTime();
            if (diff >= 60 * 1000)
                AnalyticsTrackers.getInstance(this).sendPageReadStats(sessionId, lastPage, (int) diff);
        }
        lastPage = p;
        quranPageStartDate = new Date();
        if (currentKhatmahName != null) {
            Khatmah khatmah = setting.getKhatmahByName(currentKhatmahName);
            khatmah.page = p;
            khatmah.lastReadDate = new Date();
        }
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
                            txt = quranData.arba3[mid].name + " " + ((mid - 1) / 4 + 1);
                        Utils.createToast(MainActivity.this, txt, Toast.LENGTH_LONG, Gravity.CENTER).show();
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
                        Utils.createToast(MainActivity.this, txt, Toast.LENGTH_SHORT, Gravity.CENTER).show();
                        break;
                    } else if (val > p) break;
                }
            }
        }
        if (autoHidePageInfo &&
                !adapter.isNotAllDownloaded() &&
                !isShowPageBorders(pref) &&
                pref.getBoolean("showPageInfo", true)) {
            View pageInfo = getCurrentPageInfo();
            if (pageInfo != null)
                showAndSchedulePageInfoHide(pageInfo);
        }
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
            AnalyticsTrackers.getInstance(this).sendFatalError("displayReciteInBackgroundDlg",
                    "image.myBitmap == null (recycled ??)");
            Toast.makeText(this, "حدث خطأ ما.", Toast.LENGTH_LONG).show();
            return;
        }
        final Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.fragment_recite_inbackground);
        final Spinner stopPeriod = dialog.findViewById(R.id.spinnerAutostopBackgroundRecite);
        stopPeriod.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
                R.id.text1,
                new ListItem[] {
                        new ListItem("عدم الإيقاف تلقائيا", 0),
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
        final Spinner spinner1 = dialog.findViewById(R.id.fromSurahR);
        spinner1.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
                R.id.text1, quranData.surahs2));
        final Spinner spinner2 = dialog.findViewById(R.id.toSurahR);
        spinner2.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
                R.id.text1, quranData.surahs2));
        final EditText from = dialog.findViewById(R.id.fromAyahR);
        final EditText to = dialog.findViewById(R.id.toAyahR);
        if (image.currentPage != null && image.currentPage.ayahs != null
                && image.currentPage.ayahs.size() > 0) {
            spinner1.setSelection(image.currentPage.ayahs.get(0).sura);
            spinner2.setSelection(image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1)
                    .sura);
            from.setText(Math.max(1, image.currentPage.ayahs.get(0).ayah) + "");
            to.setText(Math.max(1, image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1)
                    .ayah) + "");
        }
        final CheckBox checkBoxRepeat = dialog.findViewById(R.id.checkBoxRepeat);
        final LinearLayout repeatReciteLayout = dialog.findViewById(R.id.repeatReciteLayout);
        checkBoxRepeat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repeatReciteLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            spinner1.setClickable(isChecked);
            spinner2.setClickable(isChecked);
            from.setEnabled(isChecked);
            to.setEnabled(isChecked);
        });
        dialog.findViewById(R.id.buttonStartReciteInBackground).setOnClickListener(v -> {
            int autoStop = (int) ((ListItem) stopPeriod.getSelectedItem()).value;
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
            } else if (image.currentPage != null && image.currentPage.ayahs != null) {
                Ayah a = image.currentPage.ayahs.get(Math.max(result.selectionIndex, 0));
                sf = a.sura; f = a.ayah;
            } else {
                sf = 1;
                f = 1;
            }
            Intent intent = new Intent(MainActivity.this, PlayReciteService.class);
            intent.putExtra(PlayReciteService.ARG_AUTO_STOP_PERIOD_MINUTES_EXTRA, autoStop);
            intent.putExtra(PlayReciteService.ARG_REPEAT_STRING_EXTRA,
                    String.format(Locale.ENGLISH, "%d:%d-%d:%d", sf, f, st, t));
            Utils.startForegroundService(this, intent);
            dialog.dismiss();
            finish();
        });
        dialog.setTitle("سماع التلاوة");
        dialog.show();
    }

    @SuppressLint("RestrictedApi")
    private void setButtonBackground(Button btn, @DrawableRes int resId) {
        btn.setBackground(AppCompatDrawableManager.get().getDrawable(this, resId));
    }

    private void setBookmarkMenuItem(boolean add) {
        Button btn = findViewById(R.id.bookmarkBtn);
        setButtonBackground(btn, add ? R.drawable.baseline_star_48
                : R.drawable.baseline_star_outline_48);
    }

    private void togglePlayButton(boolean playing) {
        Button btn = findViewById(R.id.listen);
        setButtonBackground(btn, playing ? R.drawable.baseline_pause_circle_outline_48 :
                R.drawable.baseline_play_circle_outline_48);
    }

    private String getSelectedSound() {
        return getSelectedSound(pref, this);
    }

    public static String getSelectedSound(SharedPreferences pref, Context context) {
        return pref.getString("listReciters", context.getString(R.string.defaultReciter));
    }

    public static void playBasmalah(final Context context, final String selectedSound,
                                    final QuranData quranData,
                                    final Runnable finishCallback) {
        final Shared attempt = new Shared();
        attempt.setData(1);
        MediaPlayer player1 = null;
        try {
            Uri path = Utils.getAyahPath(context, selectedSound, 1, 1, quranData,
                    attempt.getData());
            if (path == null || path.toString().startsWith("http") && Utils.isConnected(context)
                    == Utils.CONNECTION_STATUS_NOT_CONNECTED) {
                finishCallback.run();
                return;
            }
            final MediaPlayer player = new MediaPlayer();
            player1 = player;
            Utils.setDataSource(player, context, path);
            player.setOnPreparedListener(mp -> player.start());
            player.setOnCompletionListener(mp -> {
                player.release();
                finishCallback.run();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                attempt.increment();
                int num = attempt.getData();
                if (num == 2 || num == 3) {
                    Uri path1 = Utils.getAyahPath(context, selectedSound, 1, 1,
                            quranData, num);
                    if (path1 == null || path1.toString().startsWith("http")
                            && Utils.isConnected(context) == Utils.CONNECTION_STATUS_NOT_CONNECTED) {
                        player.release();
                        finishCallback.run();
                        return true;
                    }
                    try {
                        player.reset();
                        Utils.setDataSource(player, context, path1);
                        player.prepareAsync();
                        lastRecitedAyahWasFile = !path1.toString().startsWith("http");
                    } catch (IOException | IllegalStateException e) {
                        e.printStackTrace();
                        player.release();
                        finishCallback.run();
                    }
                } else finishCallback.run();
                return true;
            });
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.prepareAsync();
            lastRecitedAyahWasFile = !path.toString().startsWith("http");
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            if (player1 != null)
                player1.release();
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

        displayPlayTutorial();

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
            if (image != null && image.currentPage != null && image.currentPage.ayahs != null) {
                for (int i = 0; i < image.currentPage.ayahs.size(); ++i) {
                    Ayah a = image.currentPage.ayahs.get(i);
                    if (a.sura == fromSurah && a.ayah == fromAyah) {
                        currentAyaxIndex.setData(image.selectedAyahIndex = i);
                        image.invalidate();
                        break;
                    }
                }
            } else {
                stopPlayback();
                return;
            }
            if (currentAyaxIndex.getData() == -1) {
                //user clicks btn before loading completes
                Toast.makeText(this, "الرجاء انتظار تحميل الصفحة ثم حاول ثانية",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        else {
            FindPageSelectionResult selectionResult = getCurrentPageSelected();
            int sel = selectionResult.selectionIndex;
            image = selectionResult.image;
            if (image == null || image.currentPage == null || image.currentPage.ayahs == null) {
                //user clicks btn before loading completes
                Toast.makeText(this, "الرجاء انتظار تحميل الصفحة ثم حاول ثانية",
                        Toast.LENGTH_LONG).show();
                stopPlayback();
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
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
            player = new MediaPlayer();
            player.setOnCompletionListener(mp -> {
                AnalyticsTrackers.getInstance(this).sendListenReciteStats(sessionId,
                        getSelectedSound(), sura, ayah, lastRecitedAyahWasFile, false);
                if (!allPagePlay) {
                    stopPlayback();
                } else {
                    QuranImageView image12 = getCurrentPage(playRecitePageIsRight);
                    int next = quranData.surahs[sura - 1].ayahCount >= ayah + 1 ?
                            ayah + 1 : 1;
                    if (repeat) {
                        if (sura == toSurah && ayah == toAyah) {
                            sura = fromSurah;
                            ayah = fromAyah;
                            autoSwipPage = true;
                            showPageForRecite(db.getPage(fromSurah, fromAyah));
                            image12 = getCurrentPage(playRecitePageIsRight);
                            if (image12 != null && image12.currentPage != null && image12.currentPage.ayahs != null)
                                for (int i = 0; i < image12.currentPage.ayahs.size(); ++i) {
                                    Ayah a = image12.currentPage.ayahs.get(i);
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
                                    image12 = getCurrentPage(playRecitePageIsRight);
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
                                    image12 = getCurrentPage(playRecitePageIsRight);
                                    currentAyaxIndex.setData(0);
                                } else {
                                    stopPlayback();
                                    return;
                                }
                            }
                        }
                        ayah = next;
                    }
                    if (image12 == null || image12.currentPage == null || image12.currentPage.ayahs == null) {
                        stopPlayback();
                        return;
                    }
                    currentAyaxIndex.setData(image12.selectedAyahIndex =
                            currentAyaxIndex.getData() + 1);
                    if (image12.selectedAyahIndex == image12.currentPage.ayahs.size()) {
                        autoSwipPage = true;
                        if (isDualPage() && playRecitePageIsRight) {
                            image12 = getCurrentPage(playRecitePageIsRight);
                            if (image12 != null) {
                                image12.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                                image12.invalidate();
                            }
                            image12 = getCurrentPage(playRecitePageIsRight = false);
                        } else {
                            showPageForRecite(isDualPage() ? setting.page * 2 + 2 :
                                    setting.page + 1);
                            image12 = getCurrentPage(playRecitePageIsRight);
                        }
                        if (image12 == null || image12.currentPage == null
                                || image12.currentPage.ayahs == null) {
                            stopPlayback();
                            return;
                        }
                        currentAyaxIndex.setData(image12.selectedAyahIndex = 0);
                        if (image12.currentPage.ayahs.get(0).ayah == 0)
                            currentAyaxIndex.setData(image12.selectedAyahIndex =
                                    currentAyaxIndex.getData() + 1);
                    }
                    image12.invalidate();
                    bar.setVisibility(View.VISIBLE);
                    final QuranImageView image1 = image12;
                    //attempt.setData(1);
                    //instead remember last working choice
                    Runnable tmpRunnable = () -> {
                        if (player == null) //stopPlayback() was called
                            return;
                        try {
                            player.reset();
                            Uri path = Utils.getAyahPath(MainActivity.this,
                                    getSelectedSound(), sura, ayah, quranData, attempt.getData());
                            if (path == null || path.toString().startsWith("http") &&
                                    Utils.isConnected(getApplicationContext()) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                                throw new IllegalStateException();
                            Utils.setDataSource(player, this, path);
                            player.prepareAsync();
                            lastRecitedAyahWasFile = !path.toString().startsWith("http");
                        } catch (IOException | IllegalStateException e) {
                            e.printStackTrace();
                            bar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this,
                                    "لا يمكن تشغيل التلاوة. ربما هناك مشكلة في اتصال الشبكة",
                                    Toast.LENGTH_SHORT).show();
                            image1.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                            image1.invalidate();
                            togglePlayButton(false);
                        }
                    };
                    if (ayah == 1 && sura > 1 && sura != 9)
                        playBasmalah(MainActivity.this, getSelectedSound(), quranData,
                                tmpRunnable);
                    else tmpRunnable.run();
                }
            });
            player.setOnPreparedListener(mp -> {
                if (player != null) { //user closed/cancelled before prepare completes
                    player.start();
                }
                bar.setVisibility(View.GONE);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                attempt.increment();
                if (attempt.getData() == 2 || attempt.getData() == 3) {
                    Uri path = Utils.getAyahPath(MainActivity.this, getSelectedSound(),
                            sura, ayah, quranData, attempt.getData());
                    if (path != null) {
                        try {
                            if (path.toString().startsWith("http") &&
                                    Utils.isConnected(getApplicationContext())
                                            == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                                throw new IllegalStateException();
                            player.reset();
                            Utils.setDataSource(player, this, path);
                            player.prepareAsync();
                            lastRecitedAyahWasFile = !path.toString().startsWith("http");
                            return true;
                        } catch (IOException | IllegalStateException e) {
                            e.printStackTrace();
                        }
                    }
                }
                stopPlayback();
                Toast.makeText(MainActivity.this,
                        "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                        Toast.LENGTH_LONG).show();
                return true;
            });
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            final Ayah a = image.currentPage.ayahs.get(currentAyaxIndex.getData());
            Runnable tmpRunnable = () -> {
                if (player == null) //stopPlayback() was called
                    return;
                try {
                    Uri path = Utils.getAyahPath(MainActivity.this, getSelectedSound(),
                            sura = a.sura, ayah = a.ayah, quranData, 1);
                    if (path == null || path.toString().startsWith("http") &&
                            Utils.isConnected(getApplicationContext()) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
                        throw new IllegalStateException();
                    Utils.setDataSource(player, this, path);
                    player.prepareAsync();
                    lastRecitedAyahWasFile = !path.toString().startsWith("http");
                } catch (IOException | IllegalStateException ignored) {
                    stopPlayback();
                    Toast.makeText(MainActivity.this,
                            "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                            Toast.LENGTH_LONG).show();
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

    private void displayPlayTutorial() {
        final SerializableInFile<Integer> maqraahResponse = new SerializableInFile<>(
                getApplicationContext(), "playTutorial__st", 0);
        if (maqraahResponse.getData() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("هل تعلم؟");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage("يمكنك\n- تشغيل التلاوة بلا توقف من خلال تفعيل ذلك من الإعدادات\n- تشغيل في الخلفية لتوفير البطارية من الزر المجاور للتلاوة بالأسفل\n- تحميل لا محدود للتلاوات بدون اتصال من شاشة التحميلات");
            builder.setPositiveButton("لا تخبرني مرة أخرى", (dialog, id) -> {
                dialog.cancel();
                maqraahResponse.setData(1, MainActivity.this);
            });
            builder.setNegativeButton("حسنا", (dialog, id) -> {
                dialog.cancel();
            });
            builder.show();
        }
    }

    private void initButtons() {
        Button btn = findViewById(R.id.bookmarkBtn);
        setBookmarkMenuItem(false);
        btn.setOnClickListener(v -> {
            if (adapter.isNotAllDownloaded() || setting.page <= 1) {
                Toast.makeText(MainActivity.this, "يستخدم هذا الزر لإضافة الصفحة الحالية للمفضلة",
                        Toast.LENGTH_LONG).show();
                return;
            }
            setBookmarkMenuItem(setting.toggleBookmark(setting.page * (setting.lastWasDualPage ? 2 : 1)));
            setting.save(MainActivity.this);
        });
        final int minPage = isDualPage() ? 1 : 2;
        btn = findViewById(R.id.listenBackground);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setButtonBackground(btn, R.drawable.baseline_radio_48);
        btn.setOnClickListener(v -> {
            if (adapter.isNotAllDownloaded() || setting.page < minPage) {
                Toast.makeText(MainActivity.this, "يستخدم هذا الزر لاستماع التلاوة مع توفير البطارية",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (Utils.isServiceRunning(this, PlayReciteService.class)) {
                Utils.createToast(this, "تم تشغيل التلاوات في الخلفية. يمكن الوصول إليها عبر شريط الحالة بجوار الساعة",
                        Toast.LENGTH_LONG, Gravity.CENTER).show();
                return;
            }
            displayReciteInBackgroundDlg();
        });
        btn = findViewById(R.id.listen);
        togglePlayButton(false);
        btn.setOnClickListener(v -> {
            if (adapter.isNotAllDownloaded() || setting.page < minPage) {
                Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتشغيل التلاوة",
                        Toast.LENGTH_LONG).show();
                return;
            }
            if (Utils.isServiceRunning(this, PlayReciteService.class)) {
                Utils.createToast(this, "تم تشغيل التلاوات في الخلفية. يمكن الوصول إليها عبر شريط الحالة بجوار الساعة",
                        Toast.LENGTH_LONG, Gravity.CENTER).show();
                return;
            }
            playRecite(-1, -1, -1, -1);
        });
        btn = findViewById(R.id.tafseer);
        setButtonBackground(btn, R.drawable.baseline_menu_book_48);
        btn.setOnClickListener(v -> {
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
            final File file = Utils.getTafaseerDbFile(MainActivity.this);
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
                    Utils.showConfirm(MainActivity.this, "خطأ",
                            "حدث خطأ أثناء محاولة الوصول لملف التفاسير. أعد تشغيل الجهاز",
                            "حذف التفاسير", "لا تحذف الآن",
                            (dialog, which) -> {
                                file.delete();
                                Utils.showAlert(MainActivity.this,
                                        "خطأ في الملف",
                                        "تم حذف ملف التفاسير. أعد تحميله مرة أخرى",
                                        null);
                            }, null);
                    return;
                }
            }
            if (!dbExists) {
                tmp = new String[]{"التفسير الميسر", "تحميل 8 تفاسير أخرى (140 ميغا)"};
            }
            final String items[] = tmp;
            final TafseerDbManager db = db1;
            final ListItem[] tafseers = tmp1;
            builder.setItems(items,
                    (dialog, which) -> {
                        if (which == 0) {
                            displayTafseer(-1, db, tafseers, sel,
                                    image.currentPage.ayahs);
                        } else if (items.length == 2) {
                            final ProgressDialog show = new ProgressDialog(MainActivity.this);
                            show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            show.setIndeterminate(false);
                            show.setTitle("تحميل التفاسير الإضافية");
                            show.setMessage("يتم تحميل 8 تفاسير إضافية: الطبرى والقرطبى وابن كثير والتحرير لابن عاشور والسعدى والبغوى والإعراب لدعاس والوسيط");
                            show.setMax(100);
                            show.setProgress(0);
                            show.show();
                            AnalyticsTrackers.getInstance(this).sendDownloadTafaseerStart();
                            final AsyncTask<Void, Integer, Integer> execute = new AsyncTask<Void, Integer, Integer>() {

                                @Override
                                protected Integer doInBackground(Void... params) {
                                    return Utils.downloadTafaseerDb(MainActivity.this,
                                            this::publishProgress, () -> !isCancelled());
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
                                    else if (integer == Utils.DOWNLOAD_OK) {
                                        Utils.showAlert(MainActivity.this, "تحميل التفاسير",
                                                "تم تحميل التفاسير بنجاح", null);
                                        AnalyticsTrackers.getInstance(MainActivity.this)
                                                .sendDownloadTafaseerSuccess();
                                    }
                                }
                            }.execute();
                            show.setOnCancelListener(dialog1 -> execute.cancel(true));
                        } else if (which - 1 < tafseers.length) {
                            displayTafseer(which, db, tafseers,
                                    sel, image.currentPage.ayahs);
                        } else
                            Utils.showConfirm(this, "حذف التفاسير",
                                    "حذف التفسير المحملة وتحرير 140 ميغا والإبقاء فقط على التفسير الميسر؟", (dialog12, which1) -> {
                                Utils.getTafaseerDbFile(this).delete();
                                AnalyticsTrackers.getInstance(this).sendDeleteTafaseer();
                                }, null);
                    });
            builder.show();
        });
        btn = findViewById(R.id.repeat);
        setButtonBackground(btn, R.drawable.baseline_repeat_48);
        btn.setOnClickListener(v -> {
            if (adapter.isNotAllDownloaded()) {
                Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتكرار تلاوة الآيات",
                        Toast.LENGTH_LONG).show();
                return;
            }
            displayRepeatDlg();
        });
        btn = findViewById(R.id.shareAyat);
        setButtonBackground(btn, R.drawable.baseline_share_48);
        btn.setOnClickListener(v -> {
            if (adapter.isNotAllDownloaded() || setting.page <= 1) {
                Toast.makeText(MainActivity.this, "يستخدم هذا الزر لمشاركة الآيات",
                        Toast.LENGTH_LONG).show();
                return;
            }
            displayShareDlg();
        });

        ImageButton btn2 = findViewById(R.id.fabQuickSettings);
        boolean enabled = pref.getBoolean("showSettingButton", false);
        btn2.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mQuickSettingsButton = enabled ? btn2 : null;
        mQuickSettingsButtonVisible = enabled;
        if (enabled) {
            btn2.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.PreferenceThemeOverlay)
                        .setTitle("")
                        .setView(LayoutInflater.from(this).inflate(R.layout.dlg_quick_settings, null))
                        .show();
            });
            showAndScheduleSettingButtonHide(false);
        }
    }

    private void displayAyahTafseerHelper(TafseerDbManager db2, int id, String name,
                                  ArrayList<Ayah> all, int idx,
                                          TextView tafseerTxt, TextView tafseerTitle) {
        Ayah a = all.get(currentAyahTafseerIdx = idx);
        String txt;
        if (id == -1)
            txt = db.getTafseer(a.sura, a.ayah);
        else
            txt = db2.getTafseer(id, a.sura, a.ayah);
        sendTafseer();
        tafseerPageStartDate = new Date();
        lastTafId = id;
        lastTafSura = a.sura;
        lastTafAyah = a.ayah;
        tafseerTxt.setText(txt);
        tafseerTitle.setText("سورة " + quranData.surahs[a.sura - 1].name + ": " + a.ayah);
    }

    private void displayTafseer(int tafseer, final TafseerDbManager db2,
                                final ListItem[] allTafaseer, int currnet,
                                final ArrayList<Ayah> all) {
        if (all == null)
            return;
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
        boolean themeBlack = pref.getString("tafseerTheme",
                getString(R.string.defaultTafseerTheme))
                .equals(getString(R.string.defaultTafseerTheme));
        tafseerDialog = new Dialog(this,
                themeBlack ? android.R.style.Theme_DeviceDefault_Dialog
                        : android.R.style.Theme_DeviceDefault_Light_Dialog);
        tafseerDialog.setContentView(R.layout.fragment_view_tafseer);
        final TextView textView = (TextView) tafseerDialog.findViewById(R.id.tafseerText);
        final TextView titleTextView = (TextView) tafseerDialog.findViewById(R.id.txtTafseerDlgTitle);
        textView.setTypeface(pref.getBoolean("fontBold", false) ?
                tradionalArabicFont : tradionalArabicBoldFont);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        displayAyahTafseerHelper(db2,
                (Integer) items[currentSelectedTafseer].value,
                items[currentSelectedTafseer].name, all, currnet, textView, titleTextView);
        if (!pref.getBoolean("showTafseerNavigation", true)) {
            titleTextView.setVisibility(View.GONE);
            tafseerDialog.findViewById(R.id.btnTafseerNext).setVisibility(View.GONE);
            tafseerDialog.findViewById(R.id.btnTafseerPrev).setVisibility(View.GONE);
            tafseerDialog.findViewById(R.id.spinnerTafseer).setVisibility(View.GONE);
        }
        tafseerDialog.findViewById(R.id.btnTafseerNext).setOnClickListener(v -> {
            idleUseCounter.setData(0);
            if (currentAyahTafseerIdx + 1 < all.size()) {
                displayAyahTafseerHelper(db2,
                        (Integer) items[currentSelectedTafseer].value,
                        items[currentSelectedTafseer].name, all, currentAyahTafseerIdx + 1,
                        textView, titleTextView);
            }
        });
        tafseerDialog.findViewById(R.id.btnTafseerPrev).setOnClickListener(v -> {
            idleUseCounter.setData(0);
            if (currentAyahTafseerIdx - 1 >= 0) {
                displayAyahTafseerHelper(db2, (Integer) items[currentSelectedTafseer].value,
                        items[currentSelectedTafseer].name, all, currentAyahTafseerIdx - 1,
                        textView, titleTextView);
            }
        });
        Spinner s = tafseerDialog.findViewById(R.id.spinnerTafseer);
        s.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, R.id.text1, items));
        s.setSelection(hasDownloadedTafaseer ? tafseer : 0);
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
                                       int position, long id) {
                idleUseCounter.setData(0);
                ((TextView) parentView.getChildAt(0)).setTextColor(
                        ResourcesCompat.getColor(getResources(), android.R.color.holo_blue_bright, null));
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
        tafseerDialog.setTitle("عرض التفسير");
        tafseerDialog.setOnDismissListener(dialog -> sendTafseer());
        tafseerDialog.show();
    }

    private void displayRepeatDlg() {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.fragment_repeat_recite);
        final Spinner spinner1 = dialog.findViewById(R.id.fromSurah);
        spinner1.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
                R.id.text1, quranData.surahs2));
        final Spinner spinner2 = dialog.findViewById(R.id.toSurah);
        spinner2.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item,
                R.id.text1, quranData.surahs2));
        FindPageSelectionResult result = getCurrentPageSelected();
        if (result == null) {
            Toast.makeText(this, "يستخدم هذا الزر لتكرار التلاوة", Toast.LENGTH_LONG).show();
            return;
        }
        QuranImageView image = result.image;
        if (image.myBitmap == null) {
            AnalyticsTrackers.getInstance(this).sendFatalError("displayRepeatDlg",
                    "image.myBitmap == null (recycled ??)");
            Toast.makeText(this, "حدث خطأ ما.", Toast.LENGTH_LONG).show();
            return;
        }
        final EditText from = dialog.findViewById(R.id.fromAyah);
        final EditText to = dialog.findViewById(R.id.toAyah);
        if (image.currentPage != null && image.currentPage.ayahs != null
                && image.currentPage.ayahs.size() > 0) {
            spinner1.setSelection(image.currentPage.ayahs.get(0).sura);
            spinner2.setSelection(image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1).sura);
            from.setText(Math.max(1, image.currentPage.ayahs.get(0).ayah) + "");
            to.setText(Math.max(1, image.currentPage.ayahs.get(image.currentPage.ayahs.size() - 1).ayah) + "");
        }
        dialog.findViewById(R.id.buttonStartRecite).setOnClickListener(v -> {
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
        });

        dialog.setTitle("تكرار التلاوة");
        dialog.show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void displayShareDlg(QuranImageView image) {
        if (image.currentPage == null || image.currentPage.ayahs == null ||
                image.currentPage.ayahs.size() == 0) {
            Toast.makeText(this, "الصفحة المختارة لا تحتوي على آيات",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_Dialog);
        dialog.setContentView(R.layout.fragment_share_ayat_dlg);
        shareImageView = dialog.findViewById(R.id.shareQuranImageView);
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
                    if (shareImageView == null || e == null) {
                        return; // dlg close
                    }
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
        dialog.findViewById(R.id.buttonShareImage).setOnClickListener(v -> {
            dialog.dismiss();
            File path = new File(getExternalFilesDir(null),
                    "shamraly_share.png");
            try {
                shareImageView.saveSelectedAyatAsImage(MainActivity.this,
                        path, quranData);
            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, "حدث خطأ أثناء محاولة حفظ الصورة",
                        Toast.LENGTH_LONG).show();
                AnalyticsTrackers.getInstance(this).sendException("shareDlgSaveImg", ex);
                return;
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(MainActivity.this,
                    getApplicationContext().getPackageName() + ".kilanny.shamarlymushaf.provider",
                    path));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "مشاركة"));
        });
        dialog.findViewById(R.id.buttonShareCopy).setOnClickListener(v -> {
            if (shareImageView.mutliSelectList.size() > 0) {
                String text = Utils.getAllAyahText(MainActivity.this,
                        shareImageView.mutliSelectList, quranData);
                dialog.dismiss();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("مصحف الشمرلي", text));
                Toast.makeText(MainActivity.this, "تم نسخ النص إلى الحافظة", Toast.LENGTH_LONG).show();
            } else
                showError("فضلا حدد آية أو أكثر");
        });
        dialog.findViewById(R.id.buttonShareText).setOnClickListener(v -> {
            if (shareImageView != null && shareImageView.mutliSelectList.size() > 0) {
                String text = Utils.getAllAyahText(MainActivity.this,
                        shareImageView.mutliSelectList, quranData);
                dialog.dismiss();
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(sharingIntent, "مشاركة مجموعة من الآيات"));
            } else
                showError("فضلا حدد آية أو أكثر");
        });
        dialog.setOnDismissListener(dialog1 -> {
            if (shareImageView != null) {
                shareImageView.setImageBitmap(null);
                shareImageView = null;
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
            if (result == null || result.image.currentPage == null || result.image.currentPage.ayahs == null) {
                Toast.makeText(this, "يستخدم هذا الزر لمشاركة مجموعة من الآيات",
                        Toast.LENGTH_LONG).show();
                return;
            }
            displayShareDlg(result.image);
        }
        else {
            Utils.showSelectionDlg(this, "مشاركة آية أو أكثر",
                    new String[] {"من الصفحة اليمنى", "من الصفحة اليسرى"}, true,
                    (dialog, which) -> {
                        QuranImageView page = getCurrentPage(which == 0);
                        if (page == null || page.currentPage == null || page.currentPage.ayahs == null)
                            return;
                        displayShareDlg(page);
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
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "انتهت مهلة عدم الاستخدام. تم إغلاق الشاشة آليا لتوفير البطارية",
                                Toast.LENGTH_LONG).show();
                        if (!isFinishing()) finish();
                    });
                }
            }
        }, 60000, 60000);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.getDatabaseDir(this) == null) {
            Toast.makeText(this,
                    "فشل بدء التطبيق. لا يمكن الكتابة في ذاكرة الجهاز",
                    Toast.LENGTH_LONG).show();
            AnalyticsTrackers.getInstance(this).sendFatalError("MainActivity.onCreate",
                    "getDatabaseDir() == null");
            finish();
            return;
        }
        notDownloaded = Utils.getNonExistPagesFromFile(this);
        if (notDownloaded == null) {
            Toast.makeText(this, "فشل بدء التطبيق. أغلق التطبيق ثم افتحه ثانية", Toast.LENGTH_LONG).show();
            AnalyticsTrackers.getInstance(this).sendFatalError("MainActivity.onCreate",
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
            pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            quranData = QuranData.getInstance(this);
            // Hiding the title bar has to happen before the view is created
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_main);
            currentKhatmahName = getIntent().getStringExtra(EXTRA_KHATMAH_NAME);
            bar = findViewById(R.id.progressBar);
            tradionalArabicFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
            tradionalArabicBoldFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Bold.ttf");
            autoHidePageInfo = pref.getBoolean("showPageInfo", true) &&
                    pref.getBoolean("autoHidePageInfo", true);
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
        } catch (final Exception ex) { //views maybe null?
            ex.printStackTrace();
            AnalyticsTrackers.getInstance(this).sendException("Main.onCreate()", ex);
            Toast.makeText(this, "فشل بدء التطبيق.\n" + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && hasFocus) {
            Log.d("onWindowFocusChanged", "hiding system ui");
            hideSystemUI();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null && adapter.isNotAllDownloaded()) {
            Utils.showConfirm(this, "تحميل المصحف",
                    "مرحبا بك في تطبيق مصحف الشمرلي.\n نحتاج أولا قبل بدء استخدام التطبيق لتحميل المصحف على جهازك، وذلك حتى يمكنك استخدام التطبيق دون اتصال فيما بعد. البدء بالتحميل الآن؟",
                    (dialog, which) -> downloadAll(), null);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };
    private final Runnable mHideSettingButtonRunnable = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(() -> {
                mQuickSettingsButton.setRotation(360);
                ViewCompat.animate(mQuickSettingsButton)
                        .rotation(-360)
                        .translationX(-mQuickSettingsButton.getWidth())
                        .withLayer()
                        .alpha(0)
                        .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new ViewPropertyAnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(View view) {
                                super.onAnimationEnd(view);
                                mQuickSettingsButtonVisible = false;
                                mQuickSettingsButton.setVisibility(View.GONE);
                            }
                        })
                        .start();
            });
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

    private void delayedHide(Runnable mHideRunnable, int delayMillis) {
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
        int nThreads = Utils.getCpuCoreCount(true);
        final Shared shared = new Shared();
        final Lock lock = new ReentrantLock(true);
        final Lock lock2 = new ReentrantLock(true);
        final Condition condition = lock.newCondition();
        shared.setData(0);
        final int width = tmp.getWidth(), height = tmp.getHeight();
        int threadWork = height / nThreads;
        if (k == 0) { //no dilation, just set the font/background colors of the thresholded image
            final int background = !night ? displayMode == 0 ? yellowColor : -1 : 0;
            final int font = !night ? 0 : -1;
            for (int idx = 0; idx < nThreads; ++idx) {
                final int myStart = threadWork * idx,
                        myEnd = idx == nThreads - 1 ? height : (idx + 1) * threadWork;
                AppExecutors.getInstance().executeOnCachedExecutor(() -> {
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
                });
            }
            lock.lock();
            while (shared.getData() < nThreads) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            lock.unlock();
            Log.d("imageProc", "Finished thresholding as k == 0");
            return;
        }

        //else, prepare for dilation (set all 0/1)
        //http://blog.ostermiller.org/dilate-and-erode
        Log.d("imageProc", "Started dilation, k = " + k);
        Date startDate = new Date();
        for (int idx = 0; idx < nThreads; ++idx) {
            final int myStart = threadWork * idx,
                    myEnd = idx == nThreads - 1 ? height : (idx + 1) * threadWork;
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
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
            });
        }
        lock.lock();
        while (shared.getData() < nThreads) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        lock.unlock();
        Log.d("imageProc", "Phase #1 took ms: " + (new Date().getTime() - startDate.getTime()));
        Date phase2 = new Date();
        //dilate
        manhattan(width, height);
        Log.d("imageProc", "Phase #2 took ms: " + (new Date().getTime() - phase2.getTime()));
        //restore colors of font/background
        final int background = !night ? displayMode == 0 ? yellowColor : -1 : 0;
        final int font = !night ? 0 : -1;
        shared.setData(0);
        phase2 = new Date();
        for (int idx = 0; idx < nThreads; ++idx) {
            final int myStart = threadWork * idx,
                    myEnd = idx == nThreads - 1 ? height : (idx + 1) * threadWork;
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
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
            });
        }
        lock.lock();
        while (shared.getData() < nThreads) {
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        lock.unlock();
        long el = new Date().getTime() - startDate.getTime();
        Log.d("imageProc", "Phase #3 took ms: " + (new Date().getTime() - phase2.getTime()));
        Log.d("imageProc", "Finished, elapsed ms: " + el);
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
                paint.setColorFilter(nightFilter);
                c.drawBitmap(pageBackground, 0,0 , paint);
                pageBackground.recycle();
                pageBackground = tmp;
            }
            return pageBackground;
        } catch (Exception e) {
            e.printStackTrace();
            AnalyticsTrackers.getInstance(this).sendException("readBorders()", e);
            return null;
        } finally {
            readBordersLock.unlock();
        }
    }

    public static boolean isShowPageBorders(SharedPreferences pref) {
        return pref.getBoolean("showPageBorders", false);
    }

    public static void checkOutOfMemory() throws MyOutOfMemoryException {
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
                    throw new MyOutOfMemoryException(totalDeviceRamMg, totalDeviceRamMg);
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
                    if (night) invertPaint.setColorFilter(nightFilter);
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
                    int color = night ? Color.BLACK :
                            displayMode == 0 ? yellowColor : Color.WHITE;
                    if (!isLeftPage) {
                        int second = Color.GRAY;
                        paint.setShader(new LinearGradient(offset, 0, 0, 0,
                                color, second, Shader.TileMode.MIRROR));
                        c.drawRect(bitmap.getWidth() - offset, 0,
                                bitmap.getWidth(), bitmap.getHeight(), paint);
                    } else {
                        int first = Color.GRAY;
                        paint.setShader(new LinearGradient(0, 0, offset, 0,
                                first, color, Shader.TileMode.MIRROR));
                        c.drawRect(0, 0, offset, bitmap.getHeight(), paint);
                    }
                }
            }
            return bitmap;
        } finally {
            readPageLock.unlock();
        }
    }

    @Override
    public void onSettingChanged(String keyName, Object newValue) {
    }

    private void downloadAll() {
        final ProgressDialog show = new ProgressDialog(this);
        show.setTitle("تحميل المصحف كاملا");
        show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        show.setIndeterminate(false);
        final int MAX_PAGE = FullScreenImageAdapter.MAX_PAGE;
        show.setMax(MAX_PAGE);
        show.setProgress(0);
        show.setCancelable(false);
        show.show();
        final String downloadUrls[] = {
                getString(R.string.downloadPageUrl),
                getString(R.string.downloadPageUrl2)
        };
        final AsyncTask<Void, Integer, String[]> execute = new AsyncTask<Void, Integer, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                final int nThreads = 4;
                final Shared shared = new Shared();
                final Lock lock = new ReentrantLock(true);
                final Condition condition = lock.newCondition();
                shared.setData(0);
                final Shared progress = new Shared();
                final Utils.DownloadStatusArray error = new Utils.DownloadStatusArray(nThreads);
                progress.setData(MAX_PAGE - notDownloaded.size());
                for (int th = 0; th < nThreads; ++th) {
                    final int myIdx = th;
                    AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                        byte[] buf = new byte[1024];
                        while (!isCancelled() && error.isAnyOk()) {
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
                        lock.lock();
                        shared.increment();
                        condition.signal();
                        lock.unlock();
                    });
                }
                lock.lock();
                while (shared.getData() < nThreads) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                lock.unlock();
                Utils.saveNonExistPagesToFile(getApplicationContext(), notDownloaded);
                int err = error.getFirstError();
                if (err == Utils.DOWNLOAD_MALFORMED_URL
                        || err == Utils.DOWNLOAD_SERVER_INVALID_RESPONSE)
                    return new String[]{"خطأ", "لا يمكن الاتصال بخادم التحميل. تأكد من اتصالك بالإنترنت أو حاول لاحقا"};
                else if (err == Utils.DOWNLOAD_IO_EXCEPTION
                        || err == Utils.DOWNLOAD_FILE_NOT_FOUND)
                    return new String[]{"خطأ", "لا يمكن كتابة الملف. تأكد من اتصال الإنترنت ووجود مساحة كافية"};
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
            protected void onPostExecute(final String[] strings) {
                //super.onPostExecute(strings);
                if (!isFinishing() && show.isShowing()) {
                    show.dismiss();
                    Utils.showAlert(MainActivity.this, strings[0], strings[1], (dialog, which) -> {
                        if (!strings[1].contains("نجاح")) {
                            MainActivity.this.finish();
                        }
                    });
                    if (strings[1] != null && strings[1].contains("نجاح")) {
                        initViewPagerAdapter();
                        AnalyticsTrackers.getInstance(getApplicationContext()).sendDownloadPages();
                    }
                }
            }
        }.execute();
        show.setOnCancelListener(dialog -> execute.cancel(true));
        //user close activity?
        show.setOnDismissListener(dialog -> {
            if (!execute.isCancelled())
                execute.cancel(true);
        });
    }
}