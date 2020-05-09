package kilanny.shamarlymushaf.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.TimePicker;

import com.google.android.material.snackbar.Snackbar;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.alarms.Alarm;

public class EditAlarmActivity extends AppCompatActivity {

    public static final String ARG_ALARM = "alarm";
    public static final String RESULT_ALARM = "alarm";
    public static final int RESULT_CODE_OK = 1;
    public static final int RESULT_CODE_CANCEL = 0;

    private static final int[] checkboxes = {
            R.id.chkFri, R.id.chkSat, R.id.chkSun, R.id.chkMon, R.id.chkTues,
            R.id.chkWedns, R.id.chkThurs};

    private Alarm mAlarm;

    private void cancel() {
        setResult(RESULT_CODE_CANCEL);
        finish();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        cancel();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_alarm);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Alarm alarm = getIntent().getParcelableExtra(ARG_ALARM);
        if (alarm == null) {
            alarm = new Alarm();
            alarm.enabled = true;
            alarm.timeInMins = 8 * 60 + 0;
            setTitle("إضافة تنبيه");
        } else {
            setTitle("تعديل التنبيه");
        }

        for (int i = 0; i < 7; ++i) {
            AppCompatCheckBox checkBox = findViewById(checkboxes[6 - i]);
            checkBox.setChecked((alarm.weekDayFlags & (1 << i)) != 0);
        }
        TimePicker timePicker = findViewById(R.id.timePicker);
        int h = alarm.timeInMins / 60;
        int m = alarm.timeInMins % 60;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(h);
            timePicker.setMinute(m);
        } else {
            timePicker.setCurrentHour(h);
            timePicker.setCurrentMinute(m);
        }

        mAlarm = alarm;

        findViewById(R.id.btnCancel).setOnClickListener(v -> cancel());
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            int count = 0;
            for (int i = 0; i < 7; ++i) {
                AppCompatCheckBox checkBox = findViewById(checkboxes[i]);
                count += checkBox.isChecked() ? 1 : 0;
            }
            if (count == 0) {
                Snackbar.make(v, "الرجاء تحديد أيام التنبيه", Snackbar.LENGTH_LONG).show();
                return;
            }

            mAlarm.weekDayFlags = 0;
            for (int j = 0; j < 7; ++j) {
                AppCompatCheckBox checkBox = findViewById(checkboxes[j]);
                mAlarm.weekDayFlags = (mAlarm.weekDayFlags << 1) | (checkBox.isChecked() ? 1 : 0);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAlarm.timeInMins = timePicker.getHour() * 60 + timePicker.getMinute();
            } else {
                mAlarm.timeInMins = timePicker.getCurrentHour() * 60 + timePicker.getCurrentMinute();
            }
            Intent res = new Intent();
            res.putExtra(RESULT_ALARM, mAlarm);
            setResult(RESULT_CODE_OK, res);
            finish();
        });
    }
}
