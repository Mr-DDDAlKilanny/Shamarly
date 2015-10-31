package kilanny.shamarlymushaf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FullScreenImageAdapter extends PagerAdapter {

    private final MainActivity _activity;
    public static final int MAX_PAGE = 522;
    private static final BitmapFactory.Options options;
    private final int actualDownloaded;
    private OnInstantiateQuranImageViewListener instantiateQuranImageViewListener;

    static {
        options = new BitmapFactory.Options();
        options.inDither = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
    }

    // constructor
    public FullScreenImageAdapter(MainActivity activity, int count) {
        this._activity = activity;
        this.actualDownloaded = count;
    }

    @Override
    public int getCount() {
        return Math.max(1, actualDownloaded);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        QuranImageView imgDisplay;
        position = getCount() - position;
        LayoutInflater inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container, false);
        imgDisplay = (QuranImageView) viewLayout.findViewById(R.id.quranPage);
        imgDisplay.pref = _activity.pref;
        DbManager db = DbManager.getInstance(_activity);
        if (position > 1)
            imgDisplay.currentPage = db.getPage(position);
        Bitmap bitmap;
        if (getCount() < MAX_PAGE) {
            bitmap = BitmapFactory.decodeResource(_activity.getResources(), R.drawable.pls_download,
                    options);
        } else {
            bitmap = _activity.readPage(position);
            viewLayout.setTag(position);
        }
        imgDisplay.setImageBitmap(bitmap);
        container.addView(viewLayout);
        if (getInstantiateQuranImageViewListener() != null)
            getInstantiateQuranImageViewListener().onInstantiate(imgDisplay, viewLayout);
        return viewLayout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        LinearLayout layout = (LinearLayout) object;
        ((ViewPager) container).removeView(layout);
        ImageView imgDisplay = (ImageView) layout.findViewById(R.id.quranPage);
        Drawable drawable = imgDisplay.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null) //when reading page fails, this will be null
                bitmap.recycle();
        }
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