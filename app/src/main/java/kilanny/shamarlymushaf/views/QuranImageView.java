package kilanny.shamarlymushaf.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;

import com.ortiz.touch.TouchImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.data.Ayah;
import kilanny.shamarlymushaf.data.Page;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.R;

/**
 * Created by ibraheem on 05/11/2015.
 */
public class QuranImageView extends TouchImageView {

    public static final int SELECTION_ALL = -1;
    public static final int SELECTION_NONE = -2;

    private int[] colors;
    private final float[] matrixVals = new float[9];
    private Paint rectPaint, fontPaint;
    public Page currentPage;
    public int selectedAyahIndex = SELECTION_NONE;
    private int drawColor;
    public SharedPreferences pref;
    private Resources res = getResources();
    public Bitmap myBitmap;
    public boolean isMultiSelectMode = false;
    public final ArrayList<Ayah> mutliSelectList = new ArrayList<>();

    private void init() {
        rectPaint = new Paint();
        fontPaint = new Paint();
        rectPaint.setStyle(Paint.Style.FILL);
        fontPaint.setColor(Color.WHITE);
        fontPaint.setTextSize(20);
        if (!isInEditMode()) {
            AssetManager am = getContext().getAssets();
            fontPaint.setTypeface(Typeface.createFromAsset(am, "DroidNaskh-Bold.ttf"));
            String[] arr = res.getStringArray(R.array.listValues);
            colors = new int[arr.length];
            for (int i = 0; i < arr.length; ++i)
                colors[i] = Color.parseColor(arr[i]);
        }
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
        myBitmap = bm;
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        if (rectPaint == null || myBitmap != null && myBitmap.isRecycled()) {
            AnalyticsTrackers.sendFatalError(getContext(), "QuranImageView.draw",
                    "Attempted to draw finalized Image");
            return;
        }
        //http://stackoverflow.com/a/17002006/3441905
        Canvas mCanvas = null;
        try {
            super.draw(canvas);
            mCanvas=canvas;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCanvas == null) return;
        canvas = mCanvas;
        if (currentPage != null && currentPage.ayahs != null) {
            initPrefs();
            int sel = selectedAyahIndex; // prevent errors caused by other threads modifying this field
            if (isMultiSelectMode) {
                rectPaint.setColor(drawColor);
                rectPaint.setAlpha(125);
                for (Ayah a : mutliSelectList)
                    for (RectF rect : a.rects)
                        canvas.drawRect(getScaledRectFromImageRect(rect), rectPaint);
            } else if (sel == SELECTION_ALL) {
                int idx = 0;
                for (Ayah a : currentPage.ayahs) {
                    rectPaint.setColor(colors[idx]);
                    rectPaint.setAlpha(125);
                    idx = (idx + 1) % colors.length;
                    for (RectF rect : a.rects)
                        canvas.drawRect(getScaledRectFromImageRect(rect), rectPaint);
                }
            } else if (sel >= 0 && sel < currentPage.ayahs.size()) {
                Ayah a = currentPage.ayahs.get(sel);
                rectPaint.setColor(drawColor);
                rectPaint.setAlpha(125);
                for (RectF rect : a.rects)
                    canvas.drawRect(getScaledRectFromImageRect(rect), rectPaint);
            }
        }
    }

    private RectF getScaledRectFromImageRect(RectF r) {
        Matrix matrix = getImageMatrix();
        matrix.getValues(matrixVals);
        float x = Math.abs(matrixVals[Matrix.MTRANS_X]);
        float y = Math.abs(matrixVals[Matrix.MTRANS_Y]);
        float w = getImageWidth() / (float) QuranData.NORMAL_PAGE_WIDTH;
        float h = getImageHeight() / (float) QuranData.NORMAL_PAGE_HEIGHT;
        return new RectF(r.left * w - x, r.top * h - y, r.right * w - x, r.bottom * h - y);
    }

    public int getAyahAtPos(float x, float y) {
        if (currentPage != null && currentPage.ayahs != null) {
            for (int i = 0; i < currentPage.ayahs.size(); ++i)
                for (RectF rect : currentPage.ayahs.get(i).rects)
                    if (getScaledRectFromImageRect(rect).contains(x, y))
                        return currentPage.ayahs.get(i).ayah == 0 && isMultiSelectMode ? -1 : i; // fix exception: no Basmalah in xml file

        }
        return -1;
    }

    public static void sortMutliSelectList(ArrayList<Ayah> list) {
        Collections.sort(list, new Comparator<Ayah>() {
            @Override
            public int compare(Ayah lhs, Ayah rhs) {
                if (lhs.sura != rhs.sura)
                    return lhs.sura - rhs.sura;
                if (lhs.ayah != rhs.ayah)
                    return lhs.ayah - rhs.ayah;
                return 0;
            }
        });
    }

    public void saveSelectedAyatAsImage(File file, QuranData quranData) {
        if (!isMultiSelectMode)
            throw new IllegalStateException("This method can be only invoked in multi-select mode");
        if (myBitmap != null) {
            sortMutliSelectList(mutliSelectList);
            float totalHeight = 100 + 90;
            for (Ayah a : mutliSelectList) {
                float mny = a.rects.get(0).top,
                        mxy = a.rects.get(a.rects.size() - 1).bottom;
                //TODO: check intersecting rects
                totalHeight += mxy - mny + 100;
            }
            Bitmap draw = Bitmap.createBitmap(QuranData.NORMAL_PAGE_WIDTH, (int) Math.ceil(totalHeight),
                    myBitmap.getConfig());
            Canvas canvas = new Canvas(draw);
            String text = "سورة " + quranData.surahs[mutliSelectList.get(0).sura - 1].name;
            Rect bounds = new Rect();
            fontPaint.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text, QuranData.NORMAL_PAGE_WIDTH / 2 - bounds.height() / 2, 45, fontPaint);
            int y = 100;
            for (Ayah a : mutliSelectList) {
                for (int i = 0; i < a.rects.size(); ++i) {
                    RectF rect = a.rects.get(i);
                    int increment = i == a.rects.size() - 1 ? 20 : 0;
                    canvas.drawBitmap(myBitmap,
                            new Rect((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom + increment),
                            new Rect((int) rect.left, y, (int) rect.right, y + (int) rect.height() + increment),
                            null);
                    y += (int) rect.height();
                }
                y += 80;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = myBitmap.getConfig();
            Bitmap tmp = BitmapFactory.decodeResource(res, R.drawable.googleplay, options);
            canvas.drawBitmap(tmp, null,
                    new Rect(2, (int) totalHeight - 90, QuranData.NORMAL_PAGE_WIDTH - 2, (int) totalHeight - 5),
                    null);
            tmp.recycle();
            FileOutputStream outputStream;
            try {
                if (!file.exists())
                    file.createNewFile();
                outputStream = new FileOutputStream(file, false);
                draw.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            draw.recycle();
        } else throw new IllegalStateException("myBitmap is null");
    }
    
    @Override
    protected void finalize() throws Throwable {
        this.mutliSelectList.clear();
        if (currentPage != null && currentPage.ayahs != null) {
            Page page = currentPage;
            currentPage = null;
            for (Ayah a : page.ayahs) {
                a.rects.clear();
                a.rects = null;
            }
            page.ayahs.clear();
            page.ayahs = null;
        }
        this.pref = null;
        this.res = null;
        this.rectPaint = null;
        this.fontPaint = null;
        if (this.myBitmap != null) {
            //this.myBitmap.recycle();
            setImageBitmap(this.myBitmap = null);
        }
        super.finalize();
        System.out.println("Finalized Image");
    }
}
