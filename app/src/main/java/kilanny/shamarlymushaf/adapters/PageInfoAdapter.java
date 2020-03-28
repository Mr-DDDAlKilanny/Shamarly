package kilanny.shamarlymushaf.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager.widget.PagerAdapter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Yasser on 10/24/2015.
 */
public class PageInfoAdapter extends PagerAdapter {

    private Typeface typeface;
    private String pageNumber;
    private String juzNumber;
    private String surahName;
    private String hizbNumber;
    private String khatmahName;

    @Override
    public int getCount() {
        return 4 + (khatmahName != null ? 1 : 0);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object)  {
        return view == object;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getJuzNumber() {
        return juzNumber;
    }

    public void setJuzNumber(String juzNumber) {
        this.juzNumber = juzNumber;
    }

    public String getHizbNumber() {
        return hizbNumber;
    }

    public void setHizbNumber(String hizbNumber) {
        this.hizbNumber = hizbNumber;
    }

    public String getSurahName() {
        return surahName;
    }

    public void setSurahName(String surahName) {
        this.surahName = surahName;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Context context = container.getContext();
        TextView viewLayout = new TextView(context);
        viewLayout.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewLayout.setGravity(Gravity.CENTER);
        Resources res = context.getResources();
        viewLayout.setTextColor(ResourcesCompat.getColor(res, android.R.color.primary_text_dark,
                null));
        try {
            if (typeface == null)
                typeface = Typeface.createFromAsset(context.getAssets(),
                        "DroidNaskh-Bold.ttf");
            viewLayout.setTypeface(typeface);
        } catch (Exception ignored) {
        }
        switch (position) {
            case 4:
                viewLayout.setText(getKhatmahName());
                break;
            case 3:
                viewLayout.setText(getPageNumber());
                break;
            case 2:
                viewLayout.setText(getSurahName());
                break;
            case 1:
                viewLayout.setText(getJuzNumber());
                break;
            case 0:
                viewLayout.setText(getHizbNumber());
                break;
            default:
                throw new UnsupportedOperationException();
        }
        container.addView(viewLayout);
        return viewLayout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        TextView layout = (TextView) object;
        container.removeView(layout);
    }

    public String getKhatmahName() {
        return "ختمة: " + khatmahName;
    }

    public void setKhatmahName(String khatmahName) {
        this.khatmahName = khatmahName;
    }
}
