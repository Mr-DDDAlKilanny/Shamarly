package kilanny.shamarlymushaf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FullScreenImageAdapter extends PagerAdapter {

    private final MainActivity _activity;
    private LayoutInflater inflater;
    private int actualPages = 0;
    public static final int MAX_PAGE = 522;

    // constructor
    public FullScreenImageAdapter(MainActivity activity, int actualPages) {
        this._activity = activity;
        this.actualPages = actualPages;
    }

    @Override
    public int getCount() {
        return actualPages == 0 ? 1 : actualPages;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((RelativeLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        QuranImageView imgDisplay;
        position = getCount() - position;
        inflater = (LayoutInflater) _activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container, false);
        imgDisplay = (QuranImageView) viewLayout.findViewById(R.id.quranPage);
        imgDisplay.pref = _activity.pref;
        if (position > 1)
            imgDisplay.currentPage = _activity.db.getPage(position);
        Bitmap bitmap;
        if (getCount() < MAX_PAGE) {
            bitmap = BitmapFactory.decodeResource(_activity.getResources(), R.drawable.pls_download);
        } else {
            bitmap = _activity.readPage(position);
            viewLayout.setTag(position);
        }
        imgDisplay.setImageBitmap(bitmap);
        container.addView(viewLayout);
        return viewLayout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        RelativeLayout layout = (RelativeLayout) object;
        ((ViewPager) container).removeView(layout);
        ImageView imgDisplay = (ImageView) layout.findViewById(R.id.quranPage);
        Drawable drawable = imgDisplay.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.recycle();
        }
    }
}