package kilanny.shamarlymushaf.views;

import android.widget.AbsListView;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AutoHideFabScrollListener implements AbsListView.OnScrollListener {

    private int mLastFirstVisibleItem = -1;
    private int mLastFirstVisibleItemTop = -1;
    private ListView listView;
    private FloatingActionButton fab;

    public AutoHideFabScrollListener(ListView listView, FloatingActionButton fab) {
        this.listView = listView;
        this.fab = fab;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (view.getId() == listView.getId()) {
            final int currentFirstVisibleItem = listView.getFirstVisiblePosition();
            int currentFirstVisibleItemTop = listView.getChildAt(0).getTop();;
            if (currentFirstVisibleItem > mLastFirstVisibleItem) {
                fab.hide();
            } else if (currentFirstVisibleItem < mLastFirstVisibleItem) {
                fab.show();
            } else {
                if (currentFirstVisibleItemTop < mLastFirstVisibleItemTop) {
                    fab.hide();
                } else if (currentFirstVisibleItemTop > mLastFirstVisibleItemTop) {
                    fab.show();
                }
            }

            mLastFirstVisibleItemTop = currentFirstVisibleItemTop;
            mLastFirstVisibleItem = currentFirstVisibleItem;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }
}
