package kilanny.shamarlymushaf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayReciteActivity extends ActionBarActivity {

    public static final String AUTO_STOP_PERIOD_MINUTES_EXTRA = "autoStopPeriodMinutes";
    public static final String REPEAT_STRING_EXTRA = "repeatReciteDescStr";

    private MediaPlayer player;
    private SharedPreferences pref;
    private QuranData quranData;
    private int fromSurah, fromAyah, toSurah, toAyah;
    private int currentSurah, currentAyah;
    private boolean isShown;
    private long lastTimerTickMilliLeft;
    private CountDownTimer countDownTimer;

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
        TextView sName = (TextView) findViewById(R.id.textViewSurahName);
        TextView rName = (TextView) findViewById(R.id.textViewReciter);
        TextView aName = (TextView) findViewById(R.id.textViewAyahNumber);
        TextView eName = (TextView) findViewById(R.id.textViewError);
        if (force || isShown) {
            sName.setText("سورة " + quranData.surahs[currentSurah - 1].name);
            rName.setText("الشيخ/ " + getSelectedSoundName());
            aName.setText("آية " + ArabicNumbers.convertDigits(currentAyah + ""));
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
                    if (++currentAyah > QuranData.AYAH_COUNT[currentSurah - 1]) {
                        currentAyah = 1;
                        if (++currentSurah > QuranData.AYAH_COUNT.length) {
                            if (pref.getBoolean("backToBegin", true)) {
                                currentSurah = 1;
                            } else {
                                stopPlayback(false);
                                return;
                            }
                        }
                    }
                    player.reset();
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
                if (attempt.getData() == 2) {
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
                }
                showProgress(false);
                stopPlayback(false);
                updateUi(true, "قد يكون جهازك غير متصل بالشبكة أو أن الخادم لا يستجيب");
                Toast.makeText(PlayReciteActivity.this, "لا يمكن تشغيل التلاوة. ربما توجد مشكلة في اتصالك بالإنترنت أو أن الخادم لا يستجيب",
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (player != null) { //user closed/cancelled before prepare completes
                    updateUi();
                    showProgress(false);
                    player.start();
                }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlayback(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_recite);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        quranData = QuranData.getInstance(this);
        findViewById(R.id.imageButtonPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
            }
        });
        findViewById(R.id.imageButtonStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlayback(true);
            }
        });
        Intent intent = getIntent();
        int stop = intent.getIntExtra(AUTO_STOP_PERIOD_MINUTES_EXTRA, 0);
        final TextView time = (TextView) findViewById(R.id.textViewTiming);
        if (stop > 0)
            countDownTimer = new CountDownTimer(stop * 60 * 1000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    lastTimerTickMilliLeft = millisUntilFinished;
                    time.setText((new SimpleDateFormat("hh:mm:ss")).format(new Date(millisUntilFinished)));
                }

                @Override
                public void onFinish() {
                    stopPlayback(false);
                }
            }.start();
        else time.setText("(بلا)");
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
        if (fromSurah == 0 || fromAyah == 0) {
            throw new IllegalStateException("Starting surah and ayah are required");
        }
        currentSurah = fromSurah;
        currentAyah = fromAyah;
        startPlayback();
    }
}
