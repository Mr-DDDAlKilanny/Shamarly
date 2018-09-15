package kilanny.shamarlymushaf.activities;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class WelcomeActivity extends AppCompatActivity {

    private static boolean hasCheckedForUpdates = false;

    private void checkForUpdates() {
        if (hasCheckedForUpdates) return;
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            long availMem = mi.availMem ;
            if (availMem < 10240)
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
        AnalyticsTrackers.send(getApplicationContext());
    }
}
