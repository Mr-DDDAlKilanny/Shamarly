package kilanny.shamarlymushaf;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import java.util.Arrays;

public class GotoActivity extends FragmentActivity {

    private Setting setting;
    private QuranData quranData;
    private DbManager db;

    public class MyPagerAdapter extends FragmentPagerAdapter {

        public static final int LENGTH = 6;
        private final String[] TITLES = { "المعلمة", "السورة", "الرقم",
                "البحث", "الجزء", "الحزب" };

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
            for (int i = 0; i < TITLES.length / 2; ++i) {
                String tmp = TITLES[i];
                TITLES[i] = TITLES[TITLES.length - i - 1];
                TITLES[TITLES.length - i - 1] = tmp;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        private void showMainActivity(int page) {
            Intent intent = new Intent(GotoActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                    page / (setting.lastWasDualPage ? 2 : 1));
            startActivity(intent);
        }

        private void showMainActivity(int page, int sura, int ayah) {
            Intent intent = new Intent(GotoActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                    page / (setting.lastWasDualPage ? 2 : 1));
            intent.putExtra(MainActivity.SHOW_AYAH_MESSAGE, String.format("%d,%d", sura, ayah));
            startActivity(intent);
        }

        @Override
        public Fragment getItem(int position) {
            position = LENGTH - position - 1;
            switch (position) {
                case 0:
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                            View root = inflater.inflate(R.layout.fragment_goto_bookmarked, container, false);
                            //some users have bookmarked first page, remove it
                            int min = quranData.surahs[0].page,
                                    max = quranData.surahs[quranData.surahs.length - 1].page;
                            for (int i = setting.bookmarks.size() - 1; i >= 0; --i) {
                                int num = Integer.parseInt(setting.bookmarks.get(i).name);
                                if (num < min || num > max)
                                    setting.bookmarks.remove(i);
                            }
                            final ListView l4 = (ListView) root.findViewById(R.id.listViewBookmarks);
                            int num = setting.page;
                            if (setting.lastWasDualPage)
                                num *= 2;
                            if (num > FullScreenImageAdapter.MAX_PAGE) //some save error in last session
                                num = -1;
                            int inc = num == -1 ? 0 : 1;
                            String[] book = new String[inc + setting.bookmarks.size()];
                            if (num != -1) {
                                String tmp = num > 1 ? quranData.findSurahAtPage(num).name : "";
                                book[0] = "آخر موضع: " + tmp + ": " + num;
                            }
                            for (int i = 0; i < setting.bookmarks.size(); ++i) {
                                String name = setting.bookmarks.get(i).name;
                                book[i + inc] = quranData.findSurahAtPage(Integer.parseInt(name)).name + ": " + name;
                            }
                            l4.setAdapter(new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_list_item_1, android.R.id.text1,
                                    book));
                            l4.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    String itemValue = (String) l4.getItemAtPosition(position);
                                    showMainActivity(Integer.parseInt(itemValue.substring(itemValue.lastIndexOf(":") + 2)));
                                }
                            });
                            return root;
                        }
                    };
                case 1:
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                            View root = inflater.inflate(R.layout.fragment_goto_sura, container, false);
                            final ListView l = (ListView) root.findViewById(R.id.listViewSurah);
                            l.setAdapter(new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_list_item_1, android.R.id.text1,
                                    quranData.surahs));
                            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Surah itemValue = (Surah) l.getItemAtPosition(position);
                                    showMainActivity(itemValue.page);
                                }
                            });
                            return root;
                        }
                    };
                case 2:
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                            final View root = inflater.inflate(R.layout.fragment_goto_number, container, false);
                            root.findViewById(R.id.buttonGotoPage).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    EditText txt = (EditText) root.findViewById(R.id.editTextPageNum);
                                    if (!txt.getText().toString().trim().isEmpty()) {
                                        int num = Integer.parseInt(txt.getText().toString());
                                        if (num > 0 && num <= FullScreenImageAdapter.MAX_PAGE) {
                                            showMainActivity(num);
                                        } else {
                                            Utils.showAlert(getActivity(), "خطأ",
                                                    String.format("أدخل رقم صفحة صحيح في المدى (1-%d)",
                                                            FullScreenImageAdapter.MAX_PAGE), null);
                                        }
                                    }
                                }
                            });
                            root.findViewById(R.id.buttonGotoSuraAyahNumber).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    EditText sura = (EditText) root.findViewById(R.id.editTextSuraNum);
                                    EditText ayah = (EditText) root.findViewById(R.id.editTextAyahNum);
                                    String s = sura.getText().toString().trim();
                                    String a = ayah.getText().toString().trim();
                                    if (!s.isEmpty() && !a.isEmpty()) {
                                        int ss = Integer.parseInt(s);
                                        int aa = Integer.parseInt(a);
                                        if (ss < 1 || ss > QuranData.AYAH_COUNT.length)
                                            Utils.showAlert(getActivity(), "خطأ", "رقم السورة غير صحيح", null);
                                        else if (aa < 1 || aa > QuranData.AYAH_COUNT[ss - 1])
                                            Utils.showAlert(getActivity(), "خطأ", "رقم الآية غير صحيح", null);
                                        else
                                            showMainActivity(db.getPage(ss, aa), ss, aa);
                                    }
                                }
                            });
                            return root;
                        }
                    };
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
                                    showMainActivity(result.page, result.surah, result.ayah);
                                }
                            });
                            final SearchView search = (SearchView) root.findViewById(R.id.searchTextView);
                            search.requestFocusFromTouch();
                            final TextView numResults = (TextView) root.findViewById(R.id.textViewNumResults);
                            search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                @Override
                                public boolean onQueryTextSubmit(String query) {
                                    DbManager db = DbManager.getInstance(GotoActivity.this);
                                    SearchResult res[] = db.search(query,
                                            QuranData.getInstance(GotoActivity.this))
                                            .toArray(new SearchResult[0]);
                                    numResults.setText("تم العثور على " + res.length + " نتيجة");
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
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                            View root = inflater.inflate(R.layout.fragment_goto_juz, container, false);
                            final ListView l = (ListView) root.findViewById(R.id.listViewJuzs);
                            l.setAdapter(new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_list_item_1, android.R.id.text1,
                                    Arrays.copyOfRange(quranData.juzs, 1, quranData.juzs.length)));
                            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    ListItem itemValue = (ListItem) l.getItemAtPosition(position);
                                    showMainActivity((Integer) itemValue.value);
                                }
                            });
                            return root;
                        }
                    };
                case 5:
                    return new Fragment() {
                        @Nullable
                        @Override
                        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
                            View root = inflater.inflate(R.layout.fragment_goto_hizb, container, false);
                            final ListView l = (ListView) root.findViewById(R.id.listViewHizbs);
                            l.setAdapter(new ArrayAdapter<>(getActivity(),
                                    android.R.layout.simple_list_item_1, android.R.id.text1,
                                    Arrays.copyOfRange(quranData.hizbs, 1, quranData.hizbs.length)));
                            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    ListItem itemValue = (ListItem) l.getItemAtPosition(position);
                                    showMainActivity((Integer) itemValue.value);
                                }
                            });
                            return root;
                        }
                    };
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
        setting = Setting.getInstance(this);
        quranData = QuranData.getInstance(this);
        db = DbManager.getInstance(this);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.gotoTabs);
        ViewPager pager = (ViewPager) findViewById(R.id.gotoPager);
        MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                getResources().getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        tabs.setShouldExpand(true);
        tabs.setViewPager(pager);
        pager.setCurrentItem(MyPagerAdapter.LENGTH - 1);
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
