package kilanny.shamarlymushaf;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final int[] AYAH_COUNT = {
            7,
            286,
            200,
            176,
            120,
            165,
            206,
            75,
            129,
            109,
            123,
            111,
            43,
            52,
            99,
            128,
            111,
            110,
            98,
            135,
            112,
            78,
            118,
            64,
            77,
            227,
            93,
            88,
            69,
            60,
            34,
            30,
            73,
            54,
            45,
            83,
            182,
            88,
            75,
            85,
            54,
            53,
            89,
            59,
            37,
            35,
            38,
            29,
            18,
            45,
            60,
            49,
            62,
            55,
            78,
            96,
            29,
            22,
            24,
            13,
            14,
            11,
            11,
            18,
            12,
            12,
            30,
            52,
            52,
            44,
            28,
            28,
            20,
            56,
            40,
            31,
            50,
            40,
            46,
            42,
            29,
            19,
            36,
            25,
            22,
            17,
            19,
            26,
            30,
            20,
            15,
            21,
            11,
            8,
            8,
            19,
            5,
            8,
            8,
            11,
            11,
            8,
            3,
            9,
            5,
            4,
            7,
            3,
            6,
            3,
            5,
            4,
            5,
            6
    };

    public static final String SHOW_PAGE_MESSAGE = "kilanny.shamarlymushaf.MainActivity.showPage";
    private static boolean active = false;
    private FullScreenImageAdapter adapter;
    private ViewPager viewPager;
    private final String settingFilename = "myfile";
    private Setting setting;
    SharedPreferences pref;
    public final DbManager db;
    private ProgressBar bar;
    private MediaPlayer player;
    private int sura, ayah;
    private boolean allPagePlay = false;
    private boolean autoSwipPage = false;
    private Typeface tradionalArabicFont, tradionalArabicBoldFont;
    public static MainActivity instance;

    public MainActivity() {
        DbManager.init(instance = this);
        db = DbManager.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
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

    private int getNumExistingPages() {
        int num = 0;
        for (int i = 1; i <= FullScreenImageAdapter.MAX_PAGE; ++i) {
            if (pageExists(i))
                ++num;
        }
        return num;
    }

    private String getReciteUrl(String name, int sura, int ayah) {
        return String.format(Locale.US, "http://www.everyayah.com/data/%s/%03d%03d.mp3", name, sura, ayah);
    }

    private void showNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Thumb File Deleted")
                        .setContentText("A thumb file with size has been deleted");
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1992, mBuilder.build());
    }

    private void initViewPagerAdapter() {
        int num = getNumExistingPages();
        adapter = new FullScreenImageAdapter(this, num);
        viewPager.setAdapter(adapter);
        // displaying selected image first
        Intent i = getIntent();
        int page = i.getIntExtra(SHOW_PAGE_MESSAGE, -1);
        showPage(page == -1 ? setting.page : page);
    }

    private QuranImageView getCurrentPage() {
        try {
            return (QuranImageView) viewPager.findViewWithTag(setting.page).findViewById(R.id.quranPage);
        } catch (NullPointerException ex){
            return null;
        }
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
        final GestureDetector tapGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    @Override
                    public void onLongPress(MotionEvent e) {
                        super.onLongPress(e);
                        QuranImageView imageView = getCurrentPage();
                        Ayah a = imageView.getAyahAtPos(e.getX(), e.getY());
                        if (a != null) {
                            imageView.selectedAyahIndex = imageView.currentPage.ayahs.indexOf(a);
                            imageView.invalidate();
                            stopPlayback();
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
                            if (imageView.selectedAyahIndex >= -1) {
                                imageView.selectedAyahIndex = -2;
                                imageView.invalidate();
                            }
                        }
                        return false;
                    }
                });
        contentView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                tapGestureDetector.onTouchEvent(event);
                return false;
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
                saveSettings();
                if (last != null) {
                    last.selectedAyahIndex = -2;
                    last.invalidate();
                }
                try {
                    last = getCurrentPage();
                } catch (Exception ex) {

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
        dialog.setTitle("ذهاب إلى الصفحة");
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
                            showPage(db.getPage(Integer.parseInt(s), Integer.parseInt(a)));
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
            for (int i = 0; i < image.currentPage.ayahs.size(); ++i) {
                Ayah a = image.currentPage.ayahs.get(i);
                if (a.sura == fromSurah && a.ayah == fromAyah) {
                    currentAyaxIndex.setData(image.selectedAyahIndex = i);
                    image.invalidate();
                    break;
                }
            }
        }
        else if (image.selectedAyahIndex < 0 ||
                image.selectedAyahIndex >= image.currentPage.ayahs.size()
                || pref.getBoolean("playContinues", false)) {
            allPagePlay = true;
            if (image.selectedAyahIndex < 0) {
                currentAyaxIndex.setData(image.selectedAyahIndex = 0);
                image.invalidate();
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
                        int next = AYAH_COUNT[sura - 1] >= ayah + 1 ?
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
                                if (++sura > AYAH_COUNT.length) {
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
                                if (++sura > AYAH_COUNT.length) {
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
                            if (image.currentPage.ayahs.get(image.selectedAyahIndex).ayah ==0)
                                currentAyaxIndex.setData(image.selectedAyahIndex =
                                        currentAyaxIndex.getData() + 1);
                        }
                        image.invalidate();
                        bar.setVisibility(View.VISIBLE);
                        try {
                            player.reset();
                            player.setDataSource(getReciteUrl(getSelectedSound(),
                                    sura, ayah));
                            player.prepareAsync();
                        } catch (IOException e) {
                            e.printStackTrace();
                            bar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "لا يمكن تشغيل التلاوة. ربما لم يعد الملف موجودا",
                                    Toast.LENGTH_SHORT).show();
                            image.selectedAyahIndex = -1;
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
            Ayah a = image.currentPage.ayahs.get(image.selectedAyahIndex);
            player.setDataSource(getReciteUrl(getSelectedSound(),
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
                if (image.selectedAyahIndex < 0) {
                    Toast.makeText(MainActivity.this, "حدد آية لتفسيرها، بالضغط عليها مطولا",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Ayah a = image.currentPage.ayahs.get(image.selectedAyahIndex);
                displayTafseer(db.getTafseer(a.sura, a.ayah));
            }
        });
        btn = (Button) findViewById(R.id.repeat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getCount() < FullScreenImageAdapter.MAX_PAGE) {
                    Toast.makeText(MainActivity.this, "يستخدم هذا الزر لتكرار تلاوة مجموعة من الآيات",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                displayRepeatDlg();
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
                f = Math.min(f, AYAH_COUNT[sf - 1]);
                t = Math.min(t, AYAH_COUNT[st - 1]);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "فشل بدء التطبيق. تأكد من وجود سعة تخزين كافية",
                    Toast.LENGTH_LONG).show();
            finish();
        }
        setContentView(R.layout.activity_main);
        bar = (ProgressBar) this.findViewById(R.id.progressBar);
        tradionalArabicFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
        tradionalArabicBoldFont = Typeface.createFromAsset(getAssets(), "DroidNaskh-Bold.ttf");
        try {
            pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            readSettings();
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

    private void showAlert(String title, String msg) {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage(msg);
        dlgAlert.setTitle(title);
        dlgAlert.setPositiveButton("موافق", null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    private void showConfirm(String title, String msg, DialogInterface.OnClickListener ok) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("نعم", ok)
                .setNegativeButton("لا", null)
                .show();
    }

    private Bitmap getBitmapFromURL(String link) {
        System.out.println(link);
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            BitmapFactory.Options o = new BitmapFactory.Options();
            return BitmapFactory.decodeStream(input, new Rect(0, 0, 0, 0), o);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
        Display display = getWindowManager().getDefaultDisplay();
        final Point p = new Point();
        display.getSize(p);
        final AsyncTask<Void, Integer, String[]> execute = new AsyncTask<Void, Integer, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                final ConcurrentLinkedQueue<Integer> q = new ConcurrentLinkedQueue<>();
                int exist = 0;
                for (int p = 1; !isCancelled() && p <= MAX_PAGE; ++p) {
                    Bitmap bb = readPage(p);
                    if (bb == null) {
                        q.add(p);
                    } else {
                        publishProgress(++exist);
                        bb.recycle();
                    }
                }
                if (isCancelled()) return null;
                Thread[] threads = new Thread[4];
                final Shared progress = new Shared();
                final Shared error = new Shared();
                error.setData(0);
                progress.setData(exist);
                if (exist == 0) {
                    final Lock lock = new ReentrantLock(true);
                    final Condition condition = lock.newCondition();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("حدد جودة التحميل");
                            final Runnable signal = new Runnable() {
                                @Override
                                public void run() {
                                    lock.lock();
                                    condition.signalAll();
                                    lock.unlock();
                                }
                            };
                            builder.setItems(new String[]{"جودة عالية (250 ميغا تقريبا)", "جودة عادية (80 ميغا تقريبا)"},
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == 0)
                                                setting.downloadQuality = Setting.QUALITY_HIGH;
                                            else
                                                setting.downloadQuality = Setting.QUALITY_LOW;
                                            saveSettings();
                                            signal.run();
                                        }
                                    });
                            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    setting.downloadQuality = Setting.QUALITY_UNSPECIFIED;
                                    saveSettings();
                                    signal.run();
                                }
                            });
                            builder.show();
                        }
                    });
                    try {
                        lock.lock();
                        condition.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    lock.unlock();
                }
                if (setting.downloadQuality == Setting.QUALITY_UNSPECIFIED) {
                    cancel(false);
                    return null; // user cancelled quality selection dialog
                }
                final String downloadUrl = getString(setting.downloadQuality == Setting.QUALITY_HIGH ?
                        R.string.downloadPage_high : R.string.downloadPage_low);
                for (int th = 0; th < threads.length; ++th) {
                    threads[th] = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            while (!isCancelled() && error.getData() == 0) {
                                Integer per = q.poll();
                                if (per == null) break;
                                String path = String.format(Locale.US, downloadUrl, per);
                                Bitmap b = getBitmapFromURL(path);
                                if (b == null) {
                                    error.setData(1);
                                    break;
                                } else {
                                    try {
                                        if (!writePage(per, b, p)) {
                                            error.setData(2);
                                            break;
                                        }
                                    } finally {
                                        b.recycle();
                                    }
                                }
                                progress.increment();
                                publishProgress(progress.getData());
                            }
                        }
                    });
                }
                for (int i = 0; i < threads.length; ++i)
                    threads[i].start();
                for (int i = 0; i < threads.length; ++i)
                    try {
                        threads[i].join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                if (error.getData() == 1)
                    return new String[]{"خطأ", "فشلت عملية التحميل. تأكد من اتصالك بالانترنت"};
                else if (error.getData() == 2)
                    return new String[]{"خطأ", "لا يمكن كتابة الملف. تأكد من وجود مساحة كافية"};
                else if (!isCancelled()) {
                    return new String[]{"تحميل المصحف", "جميع الصفحات تم تحميلها بنجاح"};
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(final Integer... values) {
                show.setProgress(values[0]);
            }

            @Override
            protected void onCancelled() {
                //super.onCancelled();
                show.dismiss();
            }

            @Override
            protected void onPostExecute(String[] strings) {
                //super.onPostExecute(strings);
                show.dismiss();
                showAlert(strings[0], strings[1]);
                if (strings[1] != null && strings[1].contains("نجاح"))
                    initViewPagerAdapter();
            }
        }.execute();
        show.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                execute.cancel(true);
            }
        });
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

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getAlbumStorageDir(Context context) {
        // Get the directory for the app's private pictures directory.
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "quran_Images");
        if (!file.exists() && !file.mkdirs()) {
            Log.e("QuranShemerly", "Directory not created");
        }
        return file;
    }

    private boolean pageExists(int idx) {
        File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
        return filename.exists();
    }

    public Bitmap readPage(int idx) {
        try {
            File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(filename.getPath(), options);
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

    private boolean writePage(int idx, Bitmap b, Point screenSize) {
        FileOutputStream out = null;
        File filename = new File(getAlbumStorageDir(getApplicationContext()), idx + "");
        int width = Math.min(screenSize.x, screenSize.y);
        int height = Math.max(screenSize.x, screenSize.y);
        try {
            out = new FileOutputStream(filename);
            float factor = (float) width / b.getWidth();
            Bitmap bb = Bitmap.createScaledBitmap(b, width, (int) (height * factor), false);
            b.recycle();
            b = bb;
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                    return true;
                }
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void readSettings() {
        try {
            FileInputStream fis = openFileInput(settingFilename);
            ObjectInputStream is = new ObjectInputStream(fis);
            setting = (Setting) is.readObject();
            is.close();
            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (setting == null) {
            setting = new Setting();
            setting.bookmarks = new ArrayList<>();
        }
    }

    private void saveSettings() {
        try {
            FileOutputStream fos = openFileOutput(settingFilename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(setting);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
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
    int page = 1;
    public static final short QUALITY_UNSPECIFIED = 0;
    public static final short QUALITY_LOW = 1;
    public static final short QUALITY_HIGH = 2;
    short downloadQuality = QUALITY_UNSPECIFIED;
    ArrayList<ListItem> bookmarks;

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