package kilanny.shamarlymushaf.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RecoverySystem;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.adapters.FullScreenImageAdapter;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;
import kilanny.shamarlymushaf.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashScreenActivity extends Activity {

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private SystemUiHider mSystemUiHider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        View contentView = findViewById(R.id.fullscreen_content);
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mSystemUiHider.hide();
        final ProgressBar bar = (ProgressBar) findViewById(R.id.progressBarLoadingPages);
        bar.setMax(FullScreenImageAdapter.MAX_PAGE);
        int tmp;
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            tmp = am.getMemoryClass();
        } catch (Exception ex) {
            ex.printStackTrace();
            tmp = -1;
        }
        if (tmp >= 64)
            tmp = 8;
        else if (tmp >= 32)
            tmp = 4;
        else tmp = 2;
        final int numThreads = tmp;
        new AsyncTask<Void, Integer, Throwable>() {

            @Override
            protected Throwable doInBackground(Void... params) {
                try {
                    Utils.getNonExistPages(SplashScreenActivity.this, FullScreenImageAdapter.MAX_PAGE,
                            new RecoverySystem.ProgressListener() {
                                @Override
                                public void onProgress(int progress) {
                                    publishProgress(progress);
                                }
                            }, numThreads);
                } catch (Throwable throwable) {
                    AnalyticsTrackers.sendException(SplashScreenActivity.this, throwable);
                    return throwable;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                //super.onProgressUpdate(values);
                bar.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Throwable result) {
                //super.onPostExecute(integers);
                if (result != null) {
                    Utils.showAlert(SplashScreenActivity.this, "خطأ", "حدث خطأ: لا يمكن بدء التطبيق\n"
                            + result.getMessage(), null);
                } else {
                    Intent i = new Intent();
                    i.setClass(SplashScreenActivity.this, WelcomeActivity.class);
                    startActivity(i);
                }
            }
        }.execute();
    }
}
