package kilanny.shamarlymushaf.gotofragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import kilanny.shamarlymushaf.FullScreenImageAdapter;
import kilanny.shamarlymushaf.R;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoBookmarkFragment extends GotoFragment {

    private View mView;
    
    @Override
    public void onStart() {
        super.onStart();
        if (mView != null) {
            load(); // if user is back from MainActivity, refresh last position
        }
    }

    private void load() {
        final ListView l4 = (ListView) mView.findViewById(R.id.listViewBookmarks);
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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fillFields();
        mView = inflater.inflate(R.layout.fragment_goto_bookmarked, container, false);
        //some users have bookmarked first page, remove it
        int min = quranData.surahs[0].page,
                max = quranData.surahs[quranData.surahs.length - 1].page;
        for (int i = setting.bookmarks.size() - 1; i >= 0; --i) {
            int num = Integer.parseInt(setting.bookmarks.get(i).name);
            if (num < min || num > max)
                setting.bookmarks.remove(i);
        }
        load();
        return mView;
    }
}
