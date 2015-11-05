package kilanny.shamarlymushaf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.Serializable;

public class SearchActivity extends Activity {

    private static final int MAIN_REQUEST = 6225;
    private Serializable serializable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        serializable = getIntent().getSerializableExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES);
        final ListView results = (ListView) findViewById(R.id.listViewResults);
        results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchResult result = (SearchResult) results.getItemAtPosition(position);
                Intent intent = new Intent(SearchActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE, result.page);
                intent.putExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES, serializable);
                startActivityForResult(intent, MAIN_REQUEST);
            }
        });
        final SearchView search = (SearchView) findViewById(R.id.searchTextView);
        search.requestFocusFromTouch();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                DbManager db = DbManager.getInstance(SearchActivity.this);
                SearchResult res[] = db.search(query, QuranData.getInstance(SearchActivity.this))
                        .toArray(new SearchResult[0]);
                results.setAdapter(new ArrayAdapter<>(SearchActivity.this,
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAIN_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Serializable tmp = data.getSerializableExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES);
                if (tmp != null)
                    serializable = tmp;
            }
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        Intent i = new Intent();
        i.putExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES, serializable);
        setResult(RESULT_OK, i);
        finish();
    }
}
