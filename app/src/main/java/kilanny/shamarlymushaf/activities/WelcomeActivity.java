package kilanny.shamarlymushaf.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;

public class WelcomeActivity extends AppCompatActivity {

    private static boolean hasCheckedForUpdates = false;

    private void checkForUpdates() {
        if (hasCheckedForUpdates) return;
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
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
                                        Utils.showAlert(WelcomeActivity.this, "إصدار أحدث " + info[0],
                                                "قم بتحديث التطبيق من المتجر الآن"
                                                        + "\nمالجديد:\n" + info[1], null);
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
                AnalyticsTrackers.sendComment(WelcomeActivity.this, null);
            }
        });

//        try {
//            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
//            Dialog errorDialog = googleApiAvailability.getErrorDialog(this,
//                    googleApiAvailability.isGooglePlayServicesAvailable(getApplicationContext()),
//                    0);
//            if (errorDialog != null) {
//                errorDialog.setCancelable(true);
//                errorDialog.show();
//            }
//        } catch (Exception e) {
//        }
    }
}
