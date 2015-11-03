package kilanny.shamarlymushaf;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.View;

public class FullScreenImageAdapter extends FragmentStatePagerAdapter {

    private final MainActivity _activity;
    public static final int MAX_PAGE = 522;
    private final int actualDownloaded;
    private OnInstantiateQuranImageViewListener instantiateQuranImageViewListener;

    // constructor
    public FullScreenImageAdapter(MainActivity activity, int count) {
        super(activity.getSupportFragmentManager());
        this._activity = activity;
        this.actualDownloaded = count;
    }

    @Override
    public int getCount() {
        return Math.max(1, actualDownloaded);
    }

    @Override
    public Fragment getItem(int position) {
        return QuranImageFragment.newInstance(getCount() < MAX_PAGE ? -1 : position, _activity,
                getInstantiateQuranImageViewListener());
    }

    public OnInstantiateQuranImageViewListener getInstantiateQuranImageViewListener() {
        return instantiateQuranImageViewListener;
    }

    public void setInstantiateQuranImageViewListener(OnInstantiateQuranImageViewListener instantiateQuranImageViewListener) {
        this.instantiateQuranImageViewListener = instantiateQuranImageViewListener;
    }

    public static interface OnInstantiateQuranImageViewListener {
        void onInstantiate(QuranImageView image, View parent);
    }
}