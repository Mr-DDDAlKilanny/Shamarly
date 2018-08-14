package kilanny.shamarlymushaf.activities;

import android.app.ActionBar;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;

import com.astuetz.PagerSlidingTabStrip;

import kilanny.shamarlymushaf.adapters.GotoAdapter;
import kilanny.shamarlymushaf.R;

public class GotoActivity extends FragmentActivity {

    private Handler handler = new Handler();
    private PagerSlidingTabStrip tabs;
    private Drawable oldBackground;
    private int currentColor = 0xFF666666;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goto);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.gotoTabs);
        ViewPager pager = (ViewPager) findViewById(R.id.gotoPager);
        GotoAdapter adapter = new GotoAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);
        pager.setCurrentItem(GotoAdapter.LENGTH - 1);
        try {
            changeColor(currentColor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            ActionBar bar = getActionBar();
            if (bar != null) bar.setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
        }
    };

    private void changeColor(int newColor) {
        tabs.setIndicatorColor(newColor);

        // change ActionBar color just if an ActionBar is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            Drawable colorDrawable = new ColorDrawable(newColor);
            LayerDrawable ld = new LayerDrawable(new Drawable[] { colorDrawable });

            if (oldBackground == null) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ld.setCallback(drawableCallback);
                } else {
                    ActionBar bar = getActionBar();
                    if (bar != null) bar.setBackgroundDrawable(ld);
                }

            } else {

                TransitionDrawable td = new TransitionDrawable(new Drawable[] { oldBackground, ld });

                // workaround for broken ActionBarContainer drawable handling on
                // pre-API 17 builds
                // https://github.com/android/platform_frameworks_base/commit/a7cc06d82e45918c37429a59b14545c6a57db4e4
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    td.setCallback(drawableCallback);
                } else {
                    ActionBar bar = getActionBar();
                    if (bar != null) bar.setBackgroundDrawable(td);
                }

                td.startTransition(200);

            }
            oldBackground = ld;
            ActionBar bar = getActionBar();
            if (bar != null) {
                // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
                bar.setDisplayShowTitleEnabled(false);
                bar.setDisplayShowTitleEnabled(true);
            }
        }
        currentColor = newColor;
    }
}
