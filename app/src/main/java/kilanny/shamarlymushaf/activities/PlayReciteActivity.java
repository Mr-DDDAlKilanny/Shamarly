package kilanny.shamarlymushaf.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Ayah;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.services.PlayReciteService;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class PlayReciteActivity extends AppCompatActivity implements ServiceConnection {

    private static final int PERMISSION_RQUEST = 987;
    private static boolean hasLater;
    private ImageButton pauseBtn;
    private boolean isShown;
    private PlayReciteService.LocalBinder mBinder;
    private BroadcastReceiver mServiceEventsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (PlayReciteService.ACTION_BROADCAST_EVENTS.equals(intent.getAction())) {
                update();
            }
        }
    };

    private void update() {
        if (isFinishing() || mBinder == null && !isShown)
            return;
        if (mBinder == null || mBinder.getService().isStopped()) {
            finish(); // service was stopped
        } else if (mBinder.getService().isLoading()) {
            showProgress(true);
            updateButtons();
        } else if (mBinder.getService().hasError()) {
            showProgress(false);
            updateUi();
            updateButtons();
        } else if (mBinder.getService().isPaused()) {
            updateButtons();
        } else {
            showProgress(false);
            updateUi();
            updateButtons();
        }
    }

    private boolean checkServiceNotRunning() {
        if (!Utils.isServiceRunning(this, PlayReciteService.class)) {
            Utils.createToast(this, "تم إيقاف تشغيل التلاوات، أعد بدء التطبيق من الأيقونة",
                    Toast.LENGTH_LONG, Gravity.CENTER).show();
            finish();
            return true;
        }
        return false;
    }

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
        try { unbindService(this); } catch (IllegalArgumentException ex) { ex.printStackTrace(); }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkServiceNotRunning()) return;
        isShown = true;
        bindService(new Intent(this, PlayReciteService.class), this, BIND_ABOVE_CLIENT);

        boolean vis = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            vis = true;
            SerializableInFile<Integer> maqraahResponse = new SerializableInFile<>(
                    getApplicationContext(), "phonePermiss__st", 0);
            if (maqraahResponse.getData() == 0 && !hasLater) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("طلب الإذن");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage("هل تريد أن يقوم التطبيق بإيقاف تشغيل التلاوة تلقائيا عند رن هاتفك بواسطة مكالمة؟ إذا كنت تريد هذه الميزة الرجاء منح التطبيق الصلاحية المطلوبة");
                builder.setPositiveButton("نعم أريد", (dialog, id) -> {
                    dialog.cancel();
                    ActivityCompat.requestPermissions(this,
                            new String[] {Manifest.permission.READ_PHONE_STATE}, PERMISSION_RQUEST);
                });
                builder.setNeutralButton("لاحقا", (dialog, id) -> {
                    dialog.cancel();
                    hasLater = true;
                });
                builder.setNegativeButton("لا أريد هذه الميزة", (dialog, id) -> {
                    dialog.cancel();
                    maqraahResponse.setData(-1, getApplicationContext());
                });
                builder.create().show();
            }
        }
        findViewById(R.id.permissionCheckLayout).setVisibility(vis ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (mBinder != null)
                mBinder.getService().onGrantedPhoneStatePermission();
            findViewById(R.id.permissionCheckLayout).setVisibility(View.INVISIBLE);
        }
    }

    private String getSelectedSoundName() {
        QuranData quranData = QuranData.getInstance(this);
        int idx = Utils.indexOf(quranData.reciterValues, mBinder.getService().getSelectedSound());
        if (idx >= 0)
            return quranData.reciterNames[idx];
        return null;
    }

    private void updateUi() {
        updateUi(false);
    }

    private void updateUi(boolean force) {
        if (mBinder == null)
            return;
        TextView sName = (TextView) findViewById(R.id.textViewSurahAyah);
        TextView rName = (TextView) findViewById(R.id.textViewReciter);
        TextView aName = (TextView) findViewById(R.id.textViewAyahText);
        if (aName.getMovementMethod() == null)
            aName.setMovementMethod(new ScrollingMovementMethod());
        TextView eName = (TextView) findViewById(R.id.textViewError);
        if (force || isShown) {
            QuranData quranData = QuranData.getInstance(this);
            sName.setText("سورة " + quranData.surahs[mBinder.getService().getCurrentSurah() - 1].name);
            rName.setText("القارئ/ " + getSelectedSoundName());
            ArrayList<Ayah> a = new ArrayList<>();
            Ayah aa = new Ayah();
            aa.ayah = mBinder.getService().getCurrentAyah();
            aa.sura = mBinder.getService().getCurrentSurah();
            a.add(aa);
            String ayah = Utils.getAllAyahText(this, a, quranData);
            if (ayah == null) ayah = "";
            int tmp = ayah.indexOf('{');
            if (tmp >= 0) {
                ayah = ayah.substring(tmp);
                ayah = ayah.substring(0, ayah.indexOf('}') + 1);
            } else ayah = "";
            aName.setText(ayah);

            if (mBinder.getService().hasError()) {
                eName.setText("قد يكون جهازك غير متصل بالشبكة أو أن الخادم لا يستجيب");
                eName.setVisibility(View.VISIBLE);
            } else eName.setVisibility(View.INVISIBLE);

            final TextView time = findViewById(R.id.textViewTiming);
            if (mBinder.getService().getLastTimerTickMilliLeft() > 0) {
                long n = mBinder.getService().getLastTimerTickMilliLeft() / 1000;
                int day = (int) TimeUnit.SECONDS.toDays(n);
                long hours = TimeUnit.SECONDS.toHours(n) - (day *24);
                long minute = TimeUnit.SECONDS.toMinutes(n) - (TimeUnit.SECONDS.toHours(n)* 60);
                long second = TimeUnit.SECONDS.toSeconds(n) - (TimeUnit.SECONDS.toMinutes(n) *60);
                time.setText(String.format(Locale.ENGLISH, "%02d:%02d:%02d\n", hours, minute, second));
            } else time.setText("(بلا)");
        }
    }

    private void showProgress(boolean visible) {
        ProgressBar bar = findViewById(R.id.progressBarLoadingRecite);
        bar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    private Drawable myGetDrawable(int id) {
        //ResourcesCompat.getDrawable(getResources(), id, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            return getDrawable(id);
        else
            return getResources().getDrawable(id);
    }

    private void pauseRecite() {
        if (mBinder == null) return;
        mBinder.getService().onClick(true);
        updateButtons();
    }

    private void updateButtons() {
        if (mBinder == null) return;
        if (mBinder.getService().isPaused()) {
            pauseBtn.setImageDrawable(myGetDrawable(android.R.drawable.ic_media_play));
        } else {
            pauseBtn.setImageDrawable(myGetDrawable(android.R.drawable.ic_media_pause));
        }
        findViewById(R.id.imageButtonNextAyah).setEnabled(mBinder.getService().isPaused());
        findViewById(R.id.imageButtonPrevAyah).setEnabled(mBinder.getService().isPaused());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_recite);
        if (checkServiceNotRunning()) {
            mServiceEventsBroadcastReceiver = null;
            return;
        }
        registerReceiver(mServiceEventsBroadcastReceiver,
                new IntentFilter(PlayReciteService.ACTION_BROADCAST_EVENTS));
        Typeface typeface = Typeface.createFromAsset(getAssets(), "DroidNaskh-Regular.ttf");
        ((TextView) findViewById(R.id.textViewAyahText)).setTypeface(typeface);
        pauseBtn = findViewById(R.id.imageButtonPause);
        pauseBtn.setOnClickListener(v -> pauseRecite());
        findViewById(R.id.imageButtonStop).setOnClickListener(v -> {
            if (mBinder == null)
                return;
            mBinder.getService().onClick(false);
        });
        findViewById(R.id.imageButtonNextAyah).setOnClickListener(view -> {
            if (mBinder == null)
                return;
            mBinder.getService().nextAyah();
            updateUi();
        });
        findViewById(R.id.imageButtonPrevAyah).setOnClickListener(view -> {
            if (mBinder == null)
                return;
            mBinder.getService().prevAyah();
            updateUi();
        });
        findViewById(R.id.btnPermissions).setOnClickListener(view ->
                ActivityCompat.requestPermissions(this,
                        new String[] {Manifest.permission.READ_PHONE_STATE}, PERMISSION_RQUEST));
    }

    @Override
    protected void onDestroy() {
        if (mServiceEventsBroadcastReceiver != null) {
            unregisterReceiver(mServiceEventsBroadcastReceiver);
            mServiceEventsBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("onServiceConnected", componentName.getClassName());
        mBinder = (PlayReciteService.LocalBinder) iBinder;
        update();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d("onServiceDisconnected", componentName.getClassName());
        mBinder = null;
    }
}
