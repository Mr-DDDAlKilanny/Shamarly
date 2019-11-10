package kilanny.shamarlymushaf.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.Date;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.fragments.AdsFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class WelcomeActivity extends AppCompatActivity {

    private static boolean hasCheckedForUpdates = false, hasLaterForMaqraah = false;

    private void checkForUpdates() {
        if (hasCheckedForUpdates) return;
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
            if (!Utils.haveAvailableMemory(Utils.MIN_THREAD_MEMORY_ALLOCATION * 4))
                return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String[] info = Utils.getAppVersionInfo("kilanny.shamarlymushaf");
                    if (info != null && info[0] != null && !info[0].isEmpty()) {
                        hasCheckedForUpdates = true;
                        if (!info[0].equals(BuildConfig.VERSION_NAME)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Utils.showConfirm(WelcomeActivity.this, "إصدار أحدث " + info[0],
                                                "قم بتحديث التطبيق من المتجر الآن"
                                                        + "\nمالجديد:\n" + info[1], new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                                                        try {
                                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                                    Uri.parse("market://details?id=" + appPackageName)));
                                                        } catch (android.content.ActivityNotFoundException anfe) {
                                                            startActivity(new Intent(Intent.ACTION_VIEW,
                                                                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                                        }
                                                    }
                                                }, null);
                                    } catch (Exception ex) { //activity not shown now
                                        ex.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            }).start();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForUpdates();
        if (!maqraahAd() && Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
            final SerializableInFile<Integer> appResponse = new SerializableInFile<>(
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
        builder.setPositiveButton("نعم أريد", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                builder.setTitle("هل تريد المشاركة بصفتك (اختر)");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setItems(new String[] {"طالب", "معلم (هناك بعض الشروط)"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                maqraahResponse.setData(1, getApplicationContext());
                                Utils.openUrlInChromeOrDefault(getApplicationContext(),
                                        getString(R.string.maqraah_student_url));
                                AnalyticsTrackers.sendMaqraahResponse(getApplicationContext(),
                                        1);
                                break;
                            case 1:
                                maqraahResponse.setData(2, getApplicationContext());
                                Utils.openUrlInChromeOrDefault(getApplicationContext(),
                                        getString(R.string.maqraah_teacher_url));
                                AnalyticsTrackers.sendMaqraahResponse(getApplicationContext(),
                                        2);
                                break;
                        }
                    }
                });
                builder.create().show();
            }
        });
        builder.setNeutralButton("لاحقا", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                // because not want then -> later
                maqraahResponse.setData(0, getApplicationContext());
                hasLaterForMaqraah = true;
            }
        });
        builder.setNegativeButton("لا أريد", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                maqraahResponse.setData(-1, getApplicationContext());
                AnalyticsTrackers.sendMaqraahResponse(getApplicationContext(), -1);
            }
        });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ImageButton btn = (ImageButton) findViewById(R.id.openQuran);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openSearch);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, GotoActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openSettings);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openHelp);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, HelpActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.reciter_download);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, ReciterListActivity.class));
            }
        });
        findViewById(R.id.sendComments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, ReportIssueActivity.class));
            }
        });
    }
}
