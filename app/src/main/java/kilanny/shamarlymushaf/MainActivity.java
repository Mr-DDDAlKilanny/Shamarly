package kilanny.shamarlymushaf;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;
import kilanny.shamarlymushaf.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
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

    public static final String SHOW_PAGE_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage";
    private FullScreenImageAdapter adapter;
    private ViewPager viewPager;
    private Setting setting;
    SharedPreferences pref;
    public final DbManager db;
    private ProgressBar bar;
    private MediaPlayer player;
    private int sura, ayah;
    private boolean allPagePlay = false;
    private boolean autoSwipPage = false;
    private Typeface tradionalArabicFont, tradionalArabicBoldFont;
    private QuranImageView shareImageView;

    public MainActivity() {
        DbManager.init(this);
        db = DbManager.getInstance();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
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
        adapter = new FullScreenImageAdapter(this);
        final GestureDetector tapGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    super.onLongPress(e);
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
                    QuranImageView imageView = getCurrentPage();
                    if (imageView != null && imageView.selectedAyahIndex >= QuranImageView.SELECTION_ALL) {
                        imageView.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                        imageView.invalidate();
                    }
                    return false;
                }
        });
        adapter.setInstantiateQuranImageViewListener(new FullScreenImageAdapter.OnInstantiateQuranImageViewListener() {
            @Override
            public void onInstantiate(QuranImageView image, View parent) {
                image.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        tapGestureDetector.onTouchEvent(event);
                        return false;
                    }
                });
                // this is not called at onPageSelected
                // when activity starts in landscape, so call here
                configOrientation(image);
                initCurrentPageInfo(image, parent);
            }
        });
        viewPager.setAdapter(adapter);
        // displaying selected image first
        Intent i = getIntent();
        int page = i.getIntExtra(SHOW_PAGE_MESSAGE, -1);
        page = page == -1 ? setting.page : page;
        showPage(page);
    }

    private QuranImageView getCurrentPage() {
        try {
            return (QuranImageView) viewPager.findViewWithTag(setting.page).findViewById(R.id.quranPage);
        } catch (NullPointerException ex){
            return null;
        }
    }

    private void initCurrentPageInfo(QuranImageView image, View parent) {
        if (!pref.getBoolean("showPageInfo", true)) {
            parent.findViewById(R.id.pageInfoLayout).setVisibility(View.GONE);
        } else if (image.currentPage != null && image.currentPage.ayahs.size() > 0) {
            AutoScrollViewPager pager = (AutoScrollViewPager) parent.findViewById(R.id.pageTitleViewPager);
            int page = image.currentPage.page;
            String juz = "", hizb = "";
            for (int i = 1; i < WelcomeActivity.juzs.length; ++i) {
                int val = (int) WelcomeActivity.juzs[i].value;
                if (page == val) {
                    juz = WelcomeActivity.juzs[i].name;
                    break;
                } else if (page < val) {
                    juz = WelcomeActivity.juzs[i - 1].name;
                    break;
                }
            }
            if (juz.equals("") &&
                    (int) WelcomeActivity.juzs[WelcomeActivity.juzs.length - 1].value < page) {
                juz = WelcomeActivity.juzs[WelcomeActivity.juzs.length - 1].name;
            }
            for (int i = 1; i < WelcomeActivity.hizbs.length; ++i) {
                int val = (int) WelcomeActivity.hizbs[i].value;
                if (val == page) {
                    hizb = WelcomeActivity.hizbs[i].name;
                    break;
                } else if (val > page) {
                    hizb = WelcomeActivity.hizbs[i - 1].name;
                    break;
                }
            }
            if (hizb.equals("") &&
                    (int) WelcomeActivity.hizbs[WelcomeActivity.hizbs.length - 1].value < page) {
                hizb = WelcomeActivity.hizbs[WelcomeActivity.hizbs.length - 1].name;
            }
            PageInfoAdapter adapter = new PageInfoAdapter();
            adapter.setSurahName("سورة " + WelcomeActivity.surahs[image.currentPage.ayahs.get(0).sura - 1].name);
            adapter.setJuzNumber(juz);
            adapter.setPageNumber("صفحة " + ArabicNumbers.convertDigits(page + ""));
            adapter.setHizbNumber(hizb);
            pager.setAdapter(adapter);
            pager.setCurrentItem(adapter.getCount() - 1);
            pager.setInterval(5000);
            pager.setDirection(AutoScrollViewPager.LEFT);
            pager.startAutoScroll();
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
            private QuranImageView last;

            @Override
            public void onPageSelected(int position) {
                if (!autoSwipPage)
                    stopPlayback();
                else autoSwipPage = false;
                setting.page = adapter.getCount() - position;
                setBookmarkMenuItem(setting.isBookmarked(setting.page));
                setting.save(MainActivity.this);
                if (last != null) {
                    last.selectedAyahIndex = QuranImageView.SELECTION_NONE;
                    last.invalidate();
                }
                try {
                    last = getCurrentPage();
                    configOrientation(last);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // find hizb-juz
                for (int i = 1; i < WelcomeActivity.hizbs.length; ++i) {
                    int val = (int) WelcomeActivity.hizbs[i].value;
                    if (val == setting.page) {
                        String txt;
                        if (i % 2 == 1)
                            txt = WelcomeActivity.juzs[1 + i / 2].name;
                        else
                            txt = WelcomeActivity.hizbs[i].name;
                        Toast.makeText(MainActivity.this, txt, Toast.LENGTH_SHORT).show();
                        break;
                    } else if (val > setting.page) break;
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
                android.R.layout.simple_list_item_1, android.R.id.text1, WelcomeActivity.surahs));
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Surah itemValue = (Surah) l.getItemAtPosition(position);
                dialog.dismiss();
                showPage(itemValue.page);
            }
        });
        final ListView l4 = (ListView) dialog.findViewById(R.id.listViewBookmarks);
        l4.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                setting.bookmarks.toArray()));
        l4.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem itemValue = (ListItem) l4.getItemAtPosition(position);
                dialog.dismiss();
                showPage(Integer.parseInt(itemValue.name));
            }
        });
        Spinner spinner = (Spinner) dialog.findViewById(R.id.juzNumber);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, WelcomeActivity.juzs));
        spinner = (Spinner) dialog.findViewById(R.id.hizbNumber);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, WelcomeActivity.hizbs));
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
                            if (ss < 1 || ss > Utils.AYAH_COUNT.length)
                                showError("رقم السورة غير صحيح");
                            else if (aa < 1 || aa > Utils.AYAH_COUNT[ss - 1])
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
                    if (!allPagePlay) {
                        stopPlayback();
                    }
                    else {
                        QuranImageView image = getCurrentPage();
                        int next = Utils.AYAH_COUNT[sura - 1] >= ayah + 1 ?
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
                                if (++sura > Utils.AYAH_COUNT.length) {
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
                                if (++sura > Utils.AYAH_COUNT.length) {
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
                        viewPager.getCurrentItem() == FullScreenImageAdapter.MAX_PAGE) {
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
                QuranImageView image = getCurrentPage();
                if (image == null || image.currentPage == null) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لعرض تفسير آية",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                int sel = image.selectedAyahIndex; // prevent errors caused by other threads modifying this field
                if (sel < 0) {
                    Toast.makeText(MainActivity.this, "حدد آية لتفسيرها، بالضغط عليها مطولا",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Ayah a = image.currentPage.ayahs.get(sel);
                displayTafseer(db.getTafseer(a.sura, a.ayah));
            }
        });
        btn = (Button) findViewById(R.id.repeat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayRepeatDlg();
            }
        });
        btn = (Button) findViewById(R.id.shareAyat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayShareDlg();
            }
        });
    }

    private void displayTafseer(String tafseer) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_view_tafseer);
        TextView textView = (TextView) dialog.findViewById(R.id.tafseerText);
        textView.setTypeface(pref.getBoolean("fontBold", false) ?
                tradionalArabicFont : tradionalArabicBoldFont);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        textView.setText(tafseer);
        dialog.setTitle("عرض تفسير آية");
        dialog.show();
    }

    private void displayRepeatDlg() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.fragment_repeat_recite);
        final Spinner spinner1 = (Spinner) dialog.findViewById(R.id.fromSurah);
        spinner1.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, WelcomeActivity.surahs2));
        final Spinner spinner2 = (Spinner) dialog.findViewById(R.id.toSurah);
        spinner2.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, WelcomeActivity.surahs2));
        QuranImageView image = getCurrentPage();
        final EditText from = (EditText) dialog.findViewById(R.id.fromAyah);
        final EditText to = (EditText) dialog.findViewById(R.id.toAyah);
        if (image != null && image.currentPage != null && image.currentPage.ayahs.size() >0) {
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
                        || ff == null || tt == null
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
                f = Math.min(f, Utils.AYAH_COUNT[sf - 1]);
                t = Math.min(t, Utils.AYAH_COUNT[st - 1]);
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
        if (image == null || image.currentPage == null || image.currentPage.ayahs.size() == 0) {
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
                    shareImageView.saveSelectedAyatAsImage(path);
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
                    String text = Utils.getAllAyahText(MainActivity.this, shareImageView.mutliSelectList);
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
                    String text = Utils.getAllAyahText(MainActivity.this, shareImageView.mutliSelectList);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.initDatabaseDir(this);
        DbManager.init(this);
        deleteAll();
        WelcomeActivity.initQuranData(this);
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) this.findViewById(R.id.progressBar);
        tradionalArabicFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
        tradionalArabicBoldFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Bold.ttf");
        try {
            pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            setting = Setting.getInstance(this);
            initViewPager();
            initButtons();
            getActionBar().hide();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(this, "فشل بدء التطبيق. تأكد من وجود سعة تخزين كافية",
                    Toast.LENGTH_LONG).show();
            finish();
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

    public Bitmap readPage(int idx) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            InputStream istr = getAssets().open(String.format(Locale.US, "pages/%03d.png", idx));
            Bitmap bitmap = BitmapFactory.decodeStream(istr, null, options);
            istr.close();
            bitmap.setHasAlpha(true);
            if (idx > 3) {
                Bitmap tmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                        bitmap.getConfig());
                tmp.eraseColor(Color.WHITE);  // set its background to white, or whatever color you want
                Canvas canvas = new Canvas(tmp);
                canvas.drawBitmap(bitmap, 0, 0, null);
                bitmap.recycle();
                bitmap = tmp;
                if (pref.getBoolean("nightMode", false)) {
                    Paint invertPaint = new Paint();
                    invertPaint.setColorFilter(filter);
                    tmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                            bitmap.getConfig());
                    canvas = new Canvas(tmp);
                    canvas.drawBitmap(bitmap, 0, 0, invertPaint);
                    bitmap.recycle();
                    bitmap = tmp;
                }
            }
            return bitmap;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
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
        } catch (IOException | ClassNotFoundException ex) {
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