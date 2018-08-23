package kilanny.shamarlymushaf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kilanny.shamarlymushaf.data.Ayah;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Shared;
import kilanny.shamarlymushaf.util.Utils;

public class PlayReciteActivity extends AppCompatActivity {

    public static final String AUTO_STOP_PERIOD_MINUTES_EXTRA
            = "autoStopPeriodMinutes";
    public static final String REPEAT_STRING_EXTRA = "repeatReciteDescStr";

    private ImageButton pauseBtn;
    private long timerTickMillis;
    private MediaPlayer player;
    private SharedPreferences pref;
    private QuranData quranData;
    private int fromSurah, fromAyah, toSurah, toAyah;
    private int currentSurah, currentAyah;
    private boolean isShown;
    private long lastTimerTickMilliLeft;
    private CountDownTimer countDownTimer;
    private boolean paused = false;

    @Override
    protected void onResume() {
        super.onResume();
        isShown = true;
        updateUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isShown = false;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("تأكيد الخروج")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("هل تريد فعلا إنهاء التلاوة في الخلفية؟")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PlayReciteActivity.super.onBackPressed();
                        stopPlayback(false);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void stopPlayback(boolean finish) {
        if (player != null) {
            if (player.isPlaying()) player.stop();
            player.release();
            player = null;
        }
        if (finish && !isFinishing()) finish();
    }

    private String getSelectedSound() {
        return MainActivity.getSelectedSound(pref, this);
    }

    private String getSelectedSoundName() {
        int idx = Utils.indexOf(quranData.reciterValues, getSelectedSound());
        if (idx >= 0)
            return quranData.reciterNames[idx];
        return null;
    }

    private void updateUi() {
        updateUi(false, null);
    }

    private void updateUi(boolean force, String errorMessage) {
        if (currentSurah == 0 || currentAyah == 0)
            return;
        TextView sName = (TextView) findViewById(R.id.textViewSurahAyah);
        TextView rName = (TextView) findViewById(R.id.textViewReciter);
        TextView aName = (TextView) findViewById(R.id.textViewAyahText);
        TextView eName = (TextView) findViewById(R.id.textViewError);
        if (force || isShown) {
            sName.setText("سورة " + quranData.surahs[currentSurah - 1].name);
            rName.setText("القارئ/ " + getSelectedSoundName());
            ArrayList<Ayah> a = new ArrayList<>();
            Ayah aa = new Ayah();
            aa.ayah = currentAyah;
            aa.sura = currentSurah;
            a.add(aa);
            String ayah = Utils.getAllAyahText(this, a, quranData);
            if (ayah == null) ayah = "";
            int tmp = ayah.indexOf('{');
            if (tmp >= 0) {
                ayah = ayah.substring(tmp);
                ayah = ayah.substring(0, ayah.indexOf('}') + 1);
            } else ayah = "";
            aName.setText(ayah);

            if (errorMessage != null) {
                eName.setText(errorMessage);
                eName.setVisibility(View.VISIBLE);
            } else eName.setVisibility(View.INVISIBLE);
        }
    }

    private void showProgress(boolean visible) {
        ProgressBar bar = (ProgressBar) findViewById(R.id.progressBarLoadingRecite);
        bar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private void startPlayback() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        final Shared attempt = new Shared();
        attempt.setData(1);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
                    ++currentAyah;
                    if (toSurah > 0 && toAyah > 0) { // repeat choice
                        if (currentSurah == toSurah && currentAyah > toAyah) {
                            currentSurah = fromSurah;
                            currentAyah = fromAyah;
                        } else if (currentAyah > quranData.surahs[currentSurah - 1].ayahCount) {
                            currentAyah = 1;
                            if (++currentSurah > quranData.surahs.length)
                                currentSurah = 1;
                        }
                    } else if (currentAyah > quranData.surahs[currentSurah - 1].ayahCount) { // no repeat
                        currentAyah = 1;
                        if (++currentSurah > quranData.surahs.length) {
                            if (pref.getBoolean("backToBegin", true)) {
                                currentSurah = 1;
                            } else {
                                stopPlayback(false);
                                return;
                            }
                        }
                    }
                    player.reset();
                    if (paused) {
                        pauseBtn.setEnabled(true);
                        return;
                    }
                    player.setDataSource(Utils.getAyahPath(PlayReciteActivity.this,
                            getSelectedSound(),
                            currentSurah, currentAyah, quranData, attempt.getData()));
                    Runnable tmpRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (player == null) //stopPlayback() was called
                                return;
                            showProgress(true);
                            player.prepareAsync();
                        }
                    };
                    if (currentAyah == 1 && currentSurah > 1 && currentSurah != 9)
                        MainActivity.playBasmalah(PlayReciteActivity.this,
                                getSelectedSound(), quranData, tmpRunnable);
                    else tmpRunnable.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                attempt.increment();
                if (!paused && attempt.getData() == 2) {
                    String path = Utils.getAyahPath(PlayReciteActivity.this, getSelectedSound(),
                            currentSurah, currentAyah, quranData, 2);
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
                } else if (paused)
                    pauseBtn.setEnabled(true);
                showProgress(false);
                stopPlayback(false);
                updateUi(true, "قد يكون جهازك غير متصل بالشبكة أو أن الخادم لا يستجيب");
                Toast.makeText(PlayReciteActivity.this,
                        "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (player != null && !paused) { //user closed/cancelled before prepare completes
                    updateUi();
                    showProgress(false);
                    player.start();
                } else if (paused)
                    pauseBtn.setEnabled(true);
            }
        });
        try {
            player.setDataSource(Utils.getAyahPath(this, getSelectedSound(),
                    currentSurah, currentAyah, quranData, 1));
            Runnable tmpRunnable = new Runnable() {
                @Override
                public void run() {
                    if (player == null) //stopPlayback() was called
                        return;
                    player.prepareAsync();
                }
            };
            if (currentAyah == 1 && currentSurah > 1 && currentSurah != 9)
                MainActivity.playBasmalah(this, getSelectedSound(), quranData, tmpRunnable);
            else tmpRunnable.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Drawable myGetDrawable(int id) {
        //ResourcesCompat.getDrawable(getResources(), id, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            return getDrawable(id);
        else
            return getResources().getDrawable(id);
    }

    private void initAutoCloseTimer() {
        final TextView time = (TextView) findViewById(R.id.textViewTiming);
        if (lastTimerTickMilliLeft > 0)
            countDownTimer = new CountDownTimer(lastTimerTickMilliLeft, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    lastTimerTickMilliLeft = millisUntilFinished;
                    long n = lastTimerTickMilliLeft / 1000;
                    int day = (int) TimeUnit.SECONDS.toDays(n);
                    long hours = TimeUnit.SECONDS.toHours(n) - (day *24);
                    long minute = TimeUnit.SECONDS.toMinutes(n) - (TimeUnit.SECONDS.toHours(n)* 60);
                    long second = TimeUnit.SECONDS.toSeconds(n) - (TimeUnit.SECONDS.toMinutes(n) *60);
                    time.setText(String.format(Locale.ENGLISH, "%02d:%02d:%02d\n", hours, minute, second));
                }

                @Override
                public void onFinish() {
                    pauseRecite();
                    lastTimerTickMilliLeft = timerTickMillis;
                }
            }.start();
        else time.setText("(بلا)");
    }

    private void pauseRecite() {
        if (!paused) {
            paused = true;
            if (player != null && player.isPlaying()) {
                pauseBtn.setEnabled(false);
                Toast.makeText(PlayReciteActivity.this,
                        "تم إيقاف التلاوة. انتظر هذه الآية حتى تنتهي",
                        Toast.LENGTH_LONG).show();
            } else
                stopPlayback(false);
            if (countDownTimer != null)
                countDownTimer.cancel();
            pauseBtn.setImageDrawable(myGetDrawable(android.R.drawable.ic_media_play));
        } else {
            paused = false;
            pauseBtn.setImageDrawable(myGetDrawable(android.R.drawable.ic_media_pause));
            initAutoCloseTimer();
            startPlayback();
        }
        findViewById(R.id.imageButtonNextAyah).setEnabled(paused);
        findViewById(R.id.imageButtonPrevAyah).setEnabled(paused);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_recite);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
        ((TextView) findViewById(R.id.textViewAyahText)).setTypeface(typeface);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        quranData = QuranData.getInstance(this);
        pauseBtn = findViewById(R.id.imageButtonPause);
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseRecite();
            }
        });
        findViewById(R.id.imageButtonStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayback(true);
            }
        });
        findViewById(R.id.imageButtonNextAyah).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (++currentAyah > quranData.surahs[currentSurah - 1].ayahCount) {
                    currentAyah = 1;
                    if (++currentSurah > quranData.surahs.length)
                        currentSurah = 1;
                }
                updateUi();
            }
        });
        findViewById(R.id.imageButtonPrevAyah).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (--currentAyah < 1) {
                    if (--currentSurah < 1)
                        currentSurah = quranData.surahs.length;
                    currentAyah = quranData.surahs[currentSurah - 1].ayahCount;
                }
                updateUi();
            }
        });
        Intent intent = getIntent();
        lastTimerTickMilliLeft = timerTickMillis = 60 * 1000 *
                intent.getIntExtra(AUTO_STOP_PERIOD_MINUTES_EXTRA, 0);
        initAutoCloseTimer();
        String stringExtra = intent.getStringExtra(REPEAT_STRING_EXTRA);
        if (stringExtra != null) {
            Pattern p = Pattern.compile("(\\d+):(\\d+)\\-(\\d+):(\\d+)");
            Matcher matcher = p.matcher(stringExtra);
            if (matcher.matches()) {
                fromSurah = Integer.parseInt(matcher.group(1));
                fromAyah = Integer.parseInt(matcher.group(2));
                toSurah = Integer.parseInt(matcher.group(3));
                toAyah = Integer.parseInt(matcher.group(4));
            }
        }
        if (fromSurah == 0)
            fromSurah = 1;
        currentSurah = fromSurah;
        currentAyah = fromAyah;
//        if (savedInstanceState != null) {
//            int currentSurah = savedInstanceState.getInt("currentSurah");
//            int currentAyah = savedInstanceState.getInt("currentAyah");
//            if (currentSurah > 0 && currentAyah > 0) {
//                this.currentSurah = currentSurah;
//                this.currentAyah = currentAyah;
//            }
//        }
        findViewById(R.id.imageButtonNextAyah).setEnabled(false);
        findViewById(R.id.imageButtonPrevAyah).setEnabled(false);
        startPlayback();
    }

//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putInt("currentSurah", currentSurah);
//        outState.putInt("currentAyah", currentAyah);
//    }
}
