package kilanny.shamarlymushaf.activities;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.TypedValue;

import com.astuetz.PagerSlidingTabStrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import kilanny.shamarlymushaf.fragments.HelpFragment;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;

public class HelpActivity extends FragmentActivity {

    public class MyPagerAdapter extends FragmentPagerAdapter {

        public static final int LENGTH = 5;
        private final String[] TITLES = { "حول", "المصحف", "التلاوة", "أخرى", "رسالة" };
        private final String[] strings;
        private final Typeface typeface;
        private final int fontSize;

        public MyPagerAdapter(FragmentManager fm, String[] strings,
                              Typeface fontFace, int fontSize) {
            super(fm);
            this.strings = strings;
            this.fontSize = fontSize;
            this.typeface = fontFace;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            return HelpFragment.newInstance(position, strings[position], typeface, fontSize);
        }

    }

    private Handler handler = new Handler();
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private MyPagerAdapter adapter;
    private Drawable oldBackground;
    private int currentColor = 0xFF666666;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.helpTabs);
        pager = (ViewPager) findViewById(R.id.helpPager);
        String[] strings = new String[MyPagerAdapter.LENGTH];
        AssetManager am = getAssets();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int size = Integer.parseInt(pref.getString("fontSize", "20"));
        String all;
        try {
            InputStream is = am.open("help.txt");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            i = is.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = is.read();
            }
            all = byteArrayOutputStream.toString();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int idx = all.indexOf("*");
        idx = all.indexOf("*", idx + 1);
        strings[0] = all.substring(0, idx);
        int tmp = idx;
        idx = all.indexOf("*", idx + 1);
        strings[1] = all.substring(tmp, idx);
        tmp = idx;
        idx = all.indexOf("*", idx + 1);
        strings[2] = all.substring(tmp, idx);
        tmp = idx;
        idx = all.indexOf("=", idx + 1);
        strings[3] = all.substring(tmp, idx);
        strings[4] = all.substring(all.lastIndexOf("=") + 1);
        Typeface typeface = pref.getBoolean("fontBold", false) ?
                Typeface.createFromAsset(am, "DroidNaskh-Regular.ttf")
                : Typeface.createFromAsset(am, "DroidNaskh-Bold.ttf");
        adapter = new MyPagerAdapter(getSupportFragmentManager(), strings, typeface, size);
        pager.setAdapter(adapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);
        changeColor(currentColor);
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

        Drawable colorDrawable = new ColorDrawable(newColor);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable});

        if (oldBackground == null) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ld.setCallback(drawableCallback);
            } else {
                ActionBar bar = getActionBar();
                if (bar != null) bar.setBackgroundDrawable(ld);
            }

        } else {

            TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, ld});

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
        currentColor = newColor;
    }
}

