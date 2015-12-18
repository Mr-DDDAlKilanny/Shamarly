package kilanny.shamarlymushaf;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.astuetz.PagerSlidingTabStrip;

public class GotoActivity extends FragmentActivity {

    public class MyPagerAdapter extends FragmentPagerAdapter {

        public static final int LENGTH = 7;
        private final String[] TITLES = { "السورة", "الصفحة", "المعلمة",
                "البحث", "الجزء", "الحزب" , "الرقم" };

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
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
            switch (position) {
                case 0:
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                            View root = inflater.inflate(R.layout.fragment_goto_search, container, false);
                            final ListView results = (ListView) root.findViewById(R.id.listViewResults);
                            results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    SearchResult result = (SearchResult) results.getItemAtPosition(position);
                                    Intent intent = new Intent(GotoActivity.this, MainActivity.class);
                                    intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE, result.page);
                                    intent.putExtra(MainActivity.SHOW_AYAH_MESSAGE,
                                            String.format("%d,%d", result.surah, result.ayah));
                                    startActivity(intent);
                                }
                            });
                            final SearchView search = (SearchView) root.findViewById(R.id.searchTextView);
                            search.requestFocusFromTouch();
                            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                @Override
                                public boolean onQueryTextSubmit(String query) {
                                    DbManager db = DbManager.getInstance(GotoActivity.this);
                                    SearchResult res[] = db.search(query,
                                            QuranData.getInstance(GotoActivity.this))
                                            .toArray(new SearchResult[0]);
                                    results.setAdapter(new ArrayAdapter<>(GotoActivity.this,
                                            android.R.layout.simple_list_item_1, res));
                                    View view = getCurrentFocus();
                                    if (view != null) {
                                        search.clearFocus();
                                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                                Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                                    }
                                    return true;
                                }

                                @Override
                                public boolean onQueryTextChange(String newText) {
                                    return false;
                                }
                            });
                            return root;
                        }
                    };
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
            }
            return null;
        }
    }

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
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);
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
