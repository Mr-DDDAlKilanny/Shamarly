package kilanny.shamarlymushaf.fragments.gotofragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;

import kilanny.shamarlymushaf.data.ListItem;
import kilanny.shamarlymushaf.R;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoJuzFragment extends GotoFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fillFields();
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
}
