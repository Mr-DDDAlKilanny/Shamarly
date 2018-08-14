package kilanny.shamarlymushaf.fragments.gotofragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import kilanny.shamarlymushaf.data.DbManager;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.SearchResult;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoSearchFragment extends GotoFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fillFields();
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
                FragmentActivity context = getActivity();
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
}
