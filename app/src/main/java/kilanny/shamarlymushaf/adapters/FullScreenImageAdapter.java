package kilanny.shamarlymushaf.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
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
    public final boolean isDualPage;

    // constructor
    public FullScreenImageAdapter(MainActivity activity, int count, boolean isDualPage) {
        super(activity.getSupportFragmentManager());
        this._activity = activity;
        this.isDualPage = isDualPage;
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
        return Math.max(1, actualDownloaded / (isDualPage ? 2 : 1));
    }

    public boolean isNotAllDownloaded() {
        if (!isDualPage) return getCount() < MAX_PAGE;
        return getCount() * 2 < MAX_PAGE;
    }

    @Override
    public Fragment getItem(int position) {
        QuranImageFragment fragment = QuranImageFragment.newInstance(isNotAllDownloaded() ? -1 : position,
                isDualPage, _activity, getInstantiateQuranImageViewListener());
        fragments.add(fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
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