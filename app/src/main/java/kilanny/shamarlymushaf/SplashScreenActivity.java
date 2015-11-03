package kilanny.shamarlymushaf;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RecoverySystem;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import java.util.concurrent.ConcurrentLinkedQueue;

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
        new AsyncTask<Void, Integer, ConcurrentLinkedQueue<Integer>>() {

            @Override
            protected ConcurrentLinkedQueue<Integer> doInBackground(Void... params) {
                return Utils.getNonExistPages(SplashScreenActivity.this, FullScreenImageAdapter.MAX_PAGE,
                        new RecoverySystem.ProgressListener() {
                            @Override
                            public void onProgress(int progress) {
                                publishProgress(progress);
                            }
                        });
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                //super.onProgressUpdate(values);
                bar.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(ConcurrentLinkedQueue<Integer> integers) {
                //super.onPostExecute(integers);
                Intent i = new Intent();
                i.putExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES, integers);
                i.setClass(SplashScreenActivity.this, WelcomeActivity.class);
                startActivity(i);
            }
        }.execute();
    }
}
