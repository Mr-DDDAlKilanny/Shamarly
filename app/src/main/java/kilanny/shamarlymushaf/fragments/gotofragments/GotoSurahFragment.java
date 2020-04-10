package kilanny.shamarlymushaf.fragments.gotofragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.Arrays;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Surah;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoSurahFragment extends GotoFragment implements SearchView.OnQueryTextListener {

    private ArrayAdapter<Surah> adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fillFields();
        View root = inflater.inflate(R.layout.fragment_goto_sura, container, false);
        SearchView searchSurah = root.findViewById(R.id.searchSurah);
        searchSurah.setOnQueryTextListener(this);
        ListView listView = root.findViewById(R.id.listViewSurah);
        listView.setAdapter(adapter = new ArrayAdapter<Surah>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1,
                new ArrayList<>(Arrays.asList(quranData.surahs))) {
            private Filter filter = new SurahFilter(quranData.surahs);

            @NonNull
            @Override
            public Filter getFilter() {
                return filter;
            }
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Surah itemValue = (Surah) listView.getItemAtPosition(position);
            showMainActivity(itemValue.page);
        });
        return root;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        adapter.getFilter().filter(newText);
        return true;
    }

    private class SurahFilter extends Filter {

        private final Surah[] surahs;

        SurahFilter(Surah[] surahs) {
            this.surahs = surahs;
        }

        private String prepare(String s) {
            return s.replaceAll("أ", "ا")
                    .replaceAll("إ", "ا")
                    .replaceAll("آ", "ا")
                    .replaceAll("ؤ", "ء")
                    .replaceAll("ئ", "ء");
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            ArrayList<Surah> matches = new ArrayList<>();
            if (constraint == null)
                constraint = "";
            else
                constraint = prepare(constraint.toString());
            for (Surah surah : surahs) {
                if (TextUtils.isEmpty(constraint) || prepare(surah.name).contains(constraint)) {
                    matches.add(surah);
                }
            }
            filterResults.count = matches.size();
            filterResults.values = matches;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Surah> matches = (ArrayList<Surah>) results.values;
            adapter.clear();
            for (Surah surah : matches)
                adapter.add(surah);
            adapter.notifyDataSetChanged();
        }
    }
}
