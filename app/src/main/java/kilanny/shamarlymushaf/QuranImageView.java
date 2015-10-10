package kilanny.shamarlymushaf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by ibraheem on 05/11/2015.
 */
public class QuranImageView extends ImageView {

    private int[] colors;
    private Paint rectPaint;
    Page currentPage;
    int selectedAyahIndex = -2;
    private int drawColor;
    SharedPreferences pref;
    private Resources res = getResources();

    private void init() {
        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.FILL);
        String[] arr = res.getStringArray(R.array.listValues);
        colors = new int[arr.length];
        for (int i = 0; i < arr.length; ++i)
            colors[i] = Color.parseColor(arr[i]);
    }

    public QuranImageView(Context context) {
        super(context);
        init();
    }

    public QuranImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuranImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void initPrefs() {
        drawColor = Color.parseColor(pref.getString("listSelectionColor",
                res.getString(R.string.yellow)));
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (currentPage != null) {
            initPrefs();
            if (selectedAyahIndex == -1) {
                int idx = 0;
                for (Ayah a : currentPage.ayahs) {
                    rectPaint.setColor(colors[idx]);
                    rectPaint.setAlpha(125);
                    idx = (idx + 1) % colors.length;
                    for (RectF rect : a.rects)
                        canvas.drawRect(getScaledRectFromImageRect(rect), rectPaint);
                }
            } else if (selectedAyahIndex >= 0) {
                Ayah a = currentPage.ayahs.get(selectedAyahIndex);
                rectPaint.setColor(drawColor);
                rectPaint.setAlpha(125);
                for (RectF rect : a.rects)
                    canvas.drawRect(getScaledRectFromImageRect(rect), rectPaint);
            }
        }
    }

    public RectF getScaledRectFromImageRect(RectF r) {
        float w = getWidth() / (float) 886;
        float h = getHeight() / (float) 1377;
        return new RectF(r.left * w, r.top * h, r.right * w, r.bottom * h);
    }

    public Ayah getAyahAtPos(float x, float y) {
        if (currentPage != null) {
            for (Ayah a : currentPage.ayahs)
                for (RectF rect : a.rects)
                    if (getScaledRectFromImageRect(rect).contains(x, y))
                        return a;
        }
        return null;
    }
}
