package kilanny.shamarlymushaf.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import java.util.Date;
import java.util.Locale;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.fragments.AdsFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class WelcomeActivity extends AppCompatActivity {

    private static boolean hasCheckedForUpdates = false, hasLaterForMaqraah = false;

    private void checkForUpdates() {
        if (hasCheckedForUpdates) return;
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                final String[] info = Utils.getAppVersionInfo("kilanny.shamarlymushaf");
                if (info != null && info[0] != null && !info[0].isEmpty()) {
                    hasCheckedForUpdates = true;
                    if (!info[0].equals(BuildConfig.VERSION_NAME)) {
                        runOnUiThread(() -> {
                            try {
                                Utils.showConfirm(this, "إصدار أحدث " + info[0],
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
        if (Utils.isGooglePlayServicesAvailable(this)) {
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                FirebaseMessagingDb messagingDb = FirebaseMessagingDb.getInstance(this);
                int count = Math.min(messagingDb.receivedTopicMessageDao().unreadCount(), 99);
                runOnUiThread(() -> {
                    TextView textView = findViewById(R.id.messages_badge);
                    textView.setVisibility(count > 0 ? View.VISIBLE : View.INVISIBLE);
                    textView.setText(String.format(Locale.ENGLISH, "%d", count));
                });
            });
            TextView textView = findViewById(R.id.videos_badge);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            textView.setVisibility(preferences.getBoolean("hasUnseenVideos", true) ?
                    View.VISIBLE : View.INVISIBLE);
        }
        if (!maqraahAd() && Utils.isConnected(this) != Utils.CONNECTION_STATUS_NOT_CONNECTED) {
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
                fragment.show(fm, "fragment_ads");
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
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);
        boolean googlePlayServicesAvailable = Utils.isGooglePlayServicesAvailable(this);
        findViewById(R.id.playServicesLayout).setVisibility(
                googlePlayServicesAvailable ? View.VISIBLE : View.GONE);
        if (googlePlayServicesAvailable) {
            findViewById(R.id.btnShowMessages).setOnClickListener(v ->
                    startActivity(new Intent(this, MessageTopicListActivity.class)));

            //Utils.animateView(this, findViewById(R.id.btnShowChoocenVideosLayout));

            findViewById(R.id.btnShowChoocenVideos).setOnClickListener(v -> {
                startActivity(new Intent(this, VideosActivity.class));
//                String[] array = getResources().getStringArray(R.array.topic_names);
//                Random random = new Random();
//                for (int i = 0; i < 1; ++i) {
//                    int t = random.nextInt(array.length + 1);
//                    String topic = t >= array.length ? getString(R.string.dayAyahTopic) : array[t];
//                    FirebaseMessagingDb.getInstance(this).receivedTopicMessageDao().insert(
//                            new ReceivedTopicMessage(topic, "https://youtu.be/dZ72i0Gnx_w", new Date()));
//                }
//                Log.i("insertRandom", "Ok");
            });
        }

        ImageButton btn = findViewById(R.id.openQuran);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        btn = findViewById(R.id.openSearch);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, GotoActivity.class)));
        btn = findViewById(R.id.openSettings);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        btn = findViewById(R.id.openHelp);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, HelpActivity.class)));
        btn = findViewById(R.id.reciter_download);
        btn.setOnClickListener(v ->
                startActivity(new Intent(this, ReciterListActivity.class)));
        findViewById(R.id.sendComments).setOnClickListener(v ->
                startActivity(new Intent(this, ReportIssueActivity.class)));
    }
}
