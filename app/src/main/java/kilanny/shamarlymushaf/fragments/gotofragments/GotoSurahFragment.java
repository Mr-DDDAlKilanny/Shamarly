package kilanny.shamarlymushaf.fragments.gotofragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Surah;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoSurahFragment extends GotoFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fillFields();
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
}
