package kilanny.shamarlymushaf.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v4.view.PagerAdapter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import kilanny.shamarlymushaf.R;

/**
 * Created by Yasser on 10/24/2015.
 */
public class PageInfoAdapter extends PagerAdapter {

    private static Typeface typeface;

    private String pageNumber;
    private String juzNumber;
    private String surahName;
    private String hizbNumber;

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public boolean isViewFromObject(View view, Object object)  {
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

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Context context = container.getContext();
        TextView viewLayout = new TextView(context);
        viewLayout.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        viewLayout.setGravity(Gravity.CENTER);
        Resources res = context.getResources();
        viewLayout.setTextColor(res.getColor(R.color.abc_primary_text_disable_only_material_dark));
        if (typeface == null)
            typeface = Typeface.createFromAsset(context.getAssets(), "DroidNaskh-Bold.ttf");
        viewLayout.setTypeface(typeface);
        switch (position) {
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
    public void destroyItem(ViewGroup container, int position, Object object) {
        TextView layout = (TextView) object;
        container.removeView(layout);
    }
}
