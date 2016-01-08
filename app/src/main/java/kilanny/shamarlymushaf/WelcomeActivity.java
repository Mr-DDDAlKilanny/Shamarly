package kilanny.shamarlymushaf;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

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
                                    Utils.showAlert(WelcomeActivity.this, "إصدار أحدث " + info[0],
                                            "قم بتحديث التطبيق من المتجر الآن"
                                                    + "\nمالجديد:\n" + info[1], null);
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
                AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
                builder.setTitle("إرسال تعليقات");
                // Set up the input
                final EditText input = new EditText(WelcomeActivity.this);
                input.setHint("يمكنك كتابة بريدك الإلكتروني لنرد عليك");
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setView(input);
                // Set up the buttons
                builder.setPositiveButton("إرسال", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString().trim();
                        if (m_Text.length() < 5) {
                            Utils.showAlert(WelcomeActivity.this, "خطأ", "أدخل تفاصيل كافية لإرسالها", null);
                            return;
                        }
                        AnalyticsTrackers.sendComment(WelcomeActivity.this, m_Text);
                        Toast.makeText(WelcomeActivity.this,
                                "شكرا. سيتم إرسال تعليقاتك في أقرب فرصة ممكنة إن شاء الله",
                                Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }
}
