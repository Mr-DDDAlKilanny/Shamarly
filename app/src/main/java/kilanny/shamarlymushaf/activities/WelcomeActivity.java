package kilanny.shamarlymushaf.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Date;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.fragments.AdsFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class WelcomeActivity extends AppCompatActivity {

    private static boolean hasCheckedForUpdates = false, hasLaterForMaqraah = false;

    private void checkForUpdates() {
        if (hasCheckedForUpdates) return;
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
            if (!Utils.haveAvailableMemory(Utils.MIN_THREAD_MEMORY_ALLOCATION * 4))
                return;
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                final String[] info = Utils.getAppVersionInfo("kilanny.shamarlymushaf");
                if (info != null && info[0] != null && !info[0].isEmpty()) {
                    hasCheckedForUpdates = true;
                    if (!info[0].equals(BuildConfig.VERSION_NAME)) {
                        runOnUiThread(() -> {
                            try {
                                Utils.showConfirm(WelcomeActivity.this, "إصدار أحدث " + info[0],
                                        "قم بتحديث التطبيق من المتجر الآن"
                                                + "\nمالجديد:\n" + info[1], (dialog, which) -> {
                                                    final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                                    try {
                                                        startActivity(new Intent(Intent.ACTION_VIEW,
                                                                Uri.parse("market://details?id=" + appPackageName)));
                                                    } catch (android.content.ActivityNotFoundException anfe) {
                                                        startActivity(new Intent(Intent.ACTION_VIEW,
                                                                Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                                    }
                                                }, null);
                            } catch (Exception ex) { //activity not shown now
                                ex.printStackTrace();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForUpdates();
        try {
            java.io.File files[] = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS).listFiles();
            android.util.Log.d("files", java.util.Arrays.toString(files));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (!maqraahAd() && Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            SerializableInFile<Integer> appResponse = new SerializableInFile<>(
                    getApplicationContext(), "app__st", 0);
            Date date = appResponse.getFileLastModifiedDate(getApplicationContext());
            boolean display;
            if (date != null) {
                long diffTime = new Date().getTime() - date.getTime();
                long diffDays = diffTime / (1000 * 60 * 60 * 24);
                display = diffDays >= 7;
            } else
                display = true;
            if (display) {
                appResponse.setData(appResponse.getData() + 1, getApplicationContext());
                FragmentManager fm = getSupportFragmentManager();
                AdsFragment fragment = AdsFragment.newInstance(appResponse.getData() % 2 == 1);
                fragment.show(fm, "fragment_edit_name");
            }
        }
    }

    private boolean maqraahAd() {
        if (Utils.isConnected(this) == Utils.CONNECTION_STATUS_NOT_CONNECTED)
            return false;
        final SerializableInFile<Integer> maqraahResponse = new SerializableInFile<>(
                getApplicationContext(), "maqraah__st", 0);
        if (maqraahResponse.getData() == 0 && !hasLaterForMaqraah) {
            displayMaqraahDlg(maqraahResponse);
            return true;
        } else if (maqraahResponse.getData() == -1) {
            Date date = maqraahResponse.getFileLastModifiedDate(getApplicationContext());
            if (date == null) {
                maqraahResponse.setData(0, getApplicationContext());
                displayMaqraahDlg(maqraahResponse);
                return true;
            }
            long diffTime = new Date().getTime() - date.getTime();
            long diffDays = diffTime / (1000 * 60 * 60 * 24);
            if (diffDays > 14) {
                displayMaqraahDlg(maqraahResponse);
                return true;
            }
        }
        return false;
    }

    private void displayMaqraahDlg(final SerializableInFile<Integer> maqraahResponse) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("هل لديك وقت؟");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage("انضم الآن لمقرأة الشمرلي (طلاب أو معلمون - للرجال مقرأة منفصلة عن النساء)! يمكنك تصحيح تلاوتك وتعلم أحكام التجويد بواسطة معلمين أكفاء! وإذا كنت ترى في نفسك الأهلية لتعليم الناس التجويد يمكنك أيضا المشاركة كمعلم!");
        builder.setPositiveButton("نعم أريد", (dialog, id) -> {
            dialog.cancel();
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setTitle("هل تريد المشاركة بصفتك (اختر)");
            builder2.setIcon(android.R.drawable.ic_dialog_alert);
            builder2.setItems(new String[] {"طالب", "معلم (هناك بعض الشروط)"}, (dialog1, which) -> {
                switch (which) {
                    case 0:
                        maqraahResponse.setData(1, getApplicationContext());
                        Utils.openUrlInChromeOrDefault(getApplicationContext(),
                                getString(R.string.maqraah_student_url));
                        AnalyticsTrackers.getInstance(this).sendMaqraahResponse(1);
                        break;
                    case 1:
                        maqraahResponse.setData(2, getApplicationContext());
                        Utils.openUrlInChromeOrDefault(getApplicationContext(),
                                getString(R.string.maqraah_teacher_url));
                        AnalyticsTrackers.getInstance(this).sendMaqraahResponse(2);
                        break;
                }
            });
            builder2.create().show();
        });
        builder.setNeutralButton("لاحقا", (dialog, id) -> {
            dialog.cancel();
            // because not want then -> later
            maqraahResponse.setData(0, getApplicationContext());
            hasLaterForMaqraah = true;
        });
        builder.setNegativeButton("لا أريد", (dialog, id) -> {
            dialog.cancel();
            maqraahResponse.setData(-1, getApplicationContext());
            AnalyticsTrackers.getInstance(this).sendMaqraahResponse(-1);
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ImageButton btn = (ImageButton) findViewById(R.id.openQuran);
        btn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class)));
        btn = (ImageButton) findViewById(R.id.openSearch);
        btn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, GotoActivity.class)));
        btn = (ImageButton) findViewById(R.id.openSettings);
        btn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class)));
        btn = (ImageButton) findViewById(R.id.openHelp);
        btn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, HelpActivity.class)));
        btn = (ImageButton) findViewById(R.id.reciter_download);
        btn.setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, ReciterListActivity.class)));
        findViewById(R.id.sendComments).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, ReportIssueActivity.class)));
    }
}
