package kilanny.shamarlymushaf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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

import java.util.Arrays;

/**
 * Created by Yasser on 01/11/2016.
 */
public class GotoAdapter extends FragmentPagerAdapter {

    public static final int LENGTH = 6;
    private final String[] TITLES = { "المعلمة", "السورة", "الرقم",
            "البحث", "الجزء", "الحزب" };
    private final Setting setting;
    private final QuranData quranData;
    private final GotoActivity context;
    private final DbManager db;

    public GotoAdapter(FragmentManager fm, GotoActivity activity,
                       Setting setting, DbManager db, QuranData quranData) {
        super(fm);
        this.context = activity;
        this.db = db;
        this.quranData = quranData;
        this.setting = setting;
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
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                page / (setting.lastWasDualPage ? 2 : 1));
        context.startActivity(intent);
    }

    private void showMainActivity(int page, int sura, int ayah) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                page / (setting.lastWasDualPage ? 2 : 1));
        intent.putExtra(MainActivity.SHOW_AYAH_MESSAGE, String.format("%d,%d", sura, ayah));
        context.startActivity(intent);
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
                                DbManager db = DbManager.getInstance(context);
                                SearchResult res[] = db.search(query,
                                        QuranData.getInstance(context))
                                        .toArray(new SearchResult[0]);
                                numResults.setText("تم العثور على " + res.length + " آية");
                                results.setAdapter(new ArrayAdapter<>(context,
                                        android.R.layout.simple_list_item_1, res));
                                View view = context.getCurrentFocus();
                                if (view != null) {
                                    search.clearFocus();
                                    InputMethodManager imm = (InputMethodManager)
                                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(view.getWindowToken(),
                                            InputMethodManager.HIDE_IMPLICIT_ONLY);
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
