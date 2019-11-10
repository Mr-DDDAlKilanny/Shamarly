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

import kilanny.shamarlymushaf.activities.MainActivity;
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
        fontPaint.setColor(Color.rgb(139, 69, 19));
        fontPaint.setTextSize(30);
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
        if (rectPaint == null || myBitmap == null || myBitmap.isRecycled()) {
            return;
        }
        //http://stackoverflow.com/a/17002006/3441905
        Canvas mCanvas = null;
        try {
            super.draw(canvas);
            mCanvas=canvas;
        } catch (Exception e) {
            e.printStackTrace();
            return;
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

    public void saveSelectedAyatAsImage(MainActivity activity, File file, QuranData quranData) {
        if (!isMultiSelectMode)
            throw new IllegalStateException("This method can be only invoked in multi-select mode");
        if (myBitmap != null) {
            Bitmap share;
            Rect bounds = new Rect();
            if (currentPage.page < 4) {
                share = myBitmap.copy(myBitmap.getConfig(), true);
                Canvas canvas = new Canvas(share);
                Bitmap logo = BitmapFactory.decodeResource(res, R.drawable.ic_launcher);
                canvas.drawBitmap(logo,
                        new Rect(0, 0, logo.getWidth(), logo.getHeight()),
                        new Rect(735,
                                1280,
                                735 + 96,
                                1280 + 96),
                        null);
                String text = "تطبيق مصحف الشمرلي (الحرمين)";
                fontPaint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text,
                        745 - bounds.width() - 16,
                        1310 + 64 - bounds.height() + 8,
                        fontPaint);
                logo.recycle();
                rectPaint.setColor(drawColor);
                rectPaint.setAlpha(125);
                for (Ayah a : mutliSelectList)
                    for (RectF rect : a.rects)
                        canvas.drawRect(rect, rectPaint);
            } else {
                MainActivity.checkOutOfMemory();
                Bitmap borders = activity.readBorders(currentPage.page);
                Canvas canvas = new Canvas(borders);
                float fw = (1 - (float) QuranData.NORMAL_PAGE_WIDTH / QuranData.BORDERED_PAGE_WIDTH),
                        fh = (1 - (float) QuranData.NORMAL_PAGE_HEIGHT / QuranData.BORDERED_PAGE_HEIGHT);
                int left = (int) (borders.getWidth() * fw * 0.53f + 0.5f); //round
                int right = borders.getWidth() - (int) (borders.getWidth() * fw * 0.47f + 0.5f);
                int top = (int) (borders.getHeight() * fh * 0.47f + 0.5f);
                int bottom = borders.getHeight() - (int) (borders.getHeight() * fh * 0.53f + 0.5f);
                canvas.drawBitmap(myBitmap,
                        new Rect(0, 0, myBitmap.getWidth(), myBitmap.getHeight()),
                        new Rect(left, top, right, bottom),
                        null);

                String text;
                if (currentPage.ayahs.size() > 0) {
                    text = "سورة " + quranData.surahs[
                            mutliSelectList.size() > 0 ?
                                    mutliSelectList.get(0).sura - 1
                                    : currentPage.ayahs.get(0).sura - 1].name;
                    fontPaint.getTextBounds(text, 0, text.length(), bounds);
                    canvas.drawText(text,
                            right - bounds.width() + 1,
                            top - bounds.height() - 64,
                            fontPaint);
                }
                Bitmap logo = BitmapFactory.decodeResource(res, R.drawable.ic_launcher);
                int w = 128, h = 128;
                canvas.drawBitmap(logo,
                        new Rect(0, 0, logo.getWidth(), logo.getHeight()),
                        new Rect(right - w + 64,
                                bottom + 48,
                                right + 64,
                                bottom + 48 + h),
                        null);
                text = "تطبيق مصحف الشمرلي (الحرمين)";
                fontPaint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text,
                        right - w + 64 - bounds.width() - 16,
                        bottom + 48 + h - bounds.height() - 8,
                        fontPaint);
                logo.recycle();
                rectPaint.setColor(drawColor);
                rectPaint.setAlpha(125);
                for (Ayah a : mutliSelectList)
                    for (RectF rect : a.rects)
                        canvas.drawRect(left + rect.left,
                                top + rect.top,
                                left + rect.right,
                                top + rect.bottom,
                                rectPaint);
                share = borders;
            }

            FileOutputStream outputStream;
            try {
                if (!file.exists())
                    file.createNewFile();
                outputStream = new FileOutputStream(file, false);
                share.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                share.recycle();
            }
        } else throw new IllegalStateException("myBitmap is null");
    }
}
