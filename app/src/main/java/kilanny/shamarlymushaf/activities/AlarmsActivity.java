package kilanny.shamarlymushaf.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.AlarmListAdapter;
import kilanny.shamarlymushaf.data.alarms.Alarm;
import kilanny.shamarlymushaf.data.alarms.AlarmDao;
import kilanny.shamarlymushaf.data.alarms.AlarmDb;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;
import kilanny.shamarlymushaf.views.AutoHideFabScrollListener;

public class AlarmsActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_ALARM = 0;
    private static final int REQUEST_EDIT_ALARM = 1;

    private static boolean addNotified = false;
    private AlarmListAdapter mAdapter;

    @Override
    protected void onStart() {
        super.onStart();

        if (mAdapter.getCount() == 0 && !addNotified) {
            addNotified = true;
            Utils.showAlert(this, "تنبيهات الورد",
                    "لا يوجد أي تنبيهات. قم بإضافة واحد جديد من خلال الضغط على الزر أدناه", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setRotation(0);
        ViewCompat.animate(fab)
                .rotation(360)
                .withLayer()
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        checkPermission();
    }

    private void checkPermission() {
        if (mAdapter.getCount() > 0 && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Utils.showAlert(this, "صلاحيات مطلوبة",
                    "يرجى منح التطبيق صلاحية الوصول إلى الإشعارات لإرسال التنبيه", (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1));
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setTitle("تنبيهات الورد");

        ListView listView = findViewById(R.id.listView);
        mAdapter = new AlarmListAdapter(this, alarm -> {
            Intent intent = new Intent(this, EditAlarmActivity.class);
            intent.putExtra(EditAlarmActivity.ARG_ALARM, alarm);
            startActivityForResult(intent, REQUEST_EDIT_ALARM);
            return null;
        });
        listView.setAdapter(mAdapter);

        Alarm[] alarms = AlarmDb.getInstance(this).alarmDao().getAll();
        mAdapter.clear();
        mAdapter.addAll(alarms);
        mAdapter.notifyDataSetChanged();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivityForResult(
                new Intent(this, EditAlarmActivity.class), REQUEST_ADD_ALARM));
        listView.setOnScrollListener(new AutoHideFabScrollListener(listView, fab));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_ALARM:
            case REQUEST_ADD_ALARM:
                if (resultCode == EditAlarmActivity.RESULT_CODE_OK && data != null) {
                    Alarm alarm = data.getParcelableExtra(EditAlarmActivity.RESULT_ALARM);
                    if (alarm != null) {
                        boolean isNew = requestCode == REQUEST_ADD_ALARM;
                        onAlarmEdited(isNew, alarm);
                        AnalyticsTrackers.getInstance(this).logModifyAlarm(alarm, isNew);
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onAlarmEdited(final boolean isNew, final Alarm alarm) {
        mAdapter.clear();
        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            AlarmDao alarmDao = AlarmDb.getInstance(this).alarmDao();
            alarm.enabled = true;
            if (isNew) {
                alarmDao.insert(alarm);
            } else {
                alarmDao.update(alarm);
            }
            Alarm[] alarms = alarmDao.getAll();
            runOnUiThread(() -> {
                Utils.scheduleAndDeletePrevious(this, alarms);
                mAdapter.addAll(alarms);
                mAdapter.notifyDataSetChanged();
                checkPermission();
            });
        });
    }
}
