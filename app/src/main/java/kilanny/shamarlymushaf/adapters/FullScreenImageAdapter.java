package kilanny.shamarlymushaf.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.fragments.QuranImageFragment;
import kilanny.shamarlymushaf.views.QuranImageView;

public class FullScreenImageAdapter extends FragmentStatePagerAdapter {

    private final MainActivity _activity;
    public static final int MAX_PAGE = 522;
    private final int actualDownloaded;
    private OnInstantiateQuranImageViewListener instantiateQuranImageViewListener;
    private final CopyOnWriteArrayList<QuranImageFragment> fragments = new CopyOnWriteArrayList<>();
    public final boolean isDualPage, isAutoHidePageInfo;

    // constructor
    public FullScreenImageAdapter(MainActivity activity, int count, boolean isDualPage, boolean isAutoHidePageInfo) {
        super(activity.getSupportFragmentManager());
        this._activity = activity;
        this.isDualPage = isDualPage;
        this.isAutoHidePageInfo = isAutoHidePageInfo;
        this.actualDownloaded = count;
    }

    public void recycle() {
        if (fragments == null) return;
        ArrayList<QuranImageFragment> fragments = new ArrayList<>(this.fragments);
        this.fragments.clear();
        for (QuranImageFragment f : fragments)
            f.recycle();
    }

    @Override
    public int getCount() {
        int count = Math.max(1, actualDownloaded / (isDualPage ? 2 : 1));
        if (isNotAllDownloaded(count, isDualPage))
            return 1;
        return count;
    }

    private static boolean isNotAllDownloaded(int count, boolean isDualPage) {
        if (!isDualPage) return count < MAX_PAGE;
        return count * 2 < MAX_PAGE;
    }

    public boolean isNotAllDownloaded() {
        return isNotAllDownloaded(getCount(), isDualPage);
    }

    @Override
    public Fragment getItem(int position) {
        Log.d("ImgGet", "getting at " + position);
        QuranImageFragment fragment = QuranImageFragment.newInstance(isNotAllDownloaded() ? -1 : position,
                isDualPage, isAutoHidePageInfo, _activity, getInstantiateQuranImageViewListener());
        fragments.add(fragment);
        Log.d("ImgGet", "done getting at " + position);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        Log.d("ImgDestroy", "destroying at " + position);
        try {
            QuranImageFragment fragment = (QuranImageFragment) object;
            fragments.remove(fragment);
            fragment.recycle();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        super.destroyItem(container, position, object);
    }

    public OnInstantiateQuranImageViewListener getInstantiateQuranImageViewListener() {
        return instantiateQuranImageViewListener;
    }

    public void setInstantiateQuranImageViewListener(OnInstantiateQuranImageViewListener instantiateQuranImageViewListener) {
        this.instantiateQuranImageViewListener = instantiateQuranImageViewListener;
    }

    public interface OnInstantiateQuranImageViewListener {
        void onInstantiate(WeakReference<QuranImageView> image, View parent);
    }
}