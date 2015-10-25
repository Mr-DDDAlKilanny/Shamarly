package kilanny.shamarlymushaf;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by ibraheem on 05/11/2015.
 */
public class QuranImageView extends TouchImageView {

    public static final int SELECTION_ALL = -1;
    public static final int SELECTION_NONE = -2;
    public static final int IMAGE_WIDTH = 886;
    public static final int IMAGE_HEIGHT = 1377;

    private int[] colors;
    private final float[] matrixVals = new float[9];
    private Paint rectPaint, fontPaint;
    Page currentPage;
    int selectedAyahIndex = SELECTION_NONE;
    private int drawColor;
    SharedPreferences pref;
    private Resources res = getResources();
    Bitmap myBitmap;
    boolean isMultiSelectMode = false;
    final ArrayList<Ayah> mutliSelectList = new ArrayList<>();

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
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (currentPage != null) {
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
        float w = getImageWidth() / (float) IMAGE_WIDTH;
        float h = getImageHeight() / (float) IMAGE_HEIGHT;
        return new RectF(r.left * w - x, r.top * h - y, r.right * w - x, r.bottom * h - y);
    }

    public int getAyahAtPos(float x, float y) {
        if (currentPage != null) {
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
            Bitmap draw = Bitmap.createBitmap(IMAGE_WIDTH, (int) Math.ceil(totalHeight), myBitmap.getConfig());
            Canvas canvas = new Canvas(draw);
            String text = "سورة " + quranData.surahs[mutliSelectList.get(0).sura - 1].name;
            Rect bounds = new Rect();
            fontPaint.getTextBounds(text, 0, text.length(), bounds);
            canvas.drawText(text, IMAGE_WIDTH / 2 - bounds.height() / 2, 45, fontPaint);
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
                    new Rect(2, (int) totalHeight - 90, IMAGE_WIDTH - 2, (int) totalHeight - 5),
                    null);
            tmp.recycle();
            FileOutputStream outputStream;
            try {
                if (!file.exists())
                    file.createNewFile();
                outputStream = new FileOutputStream(file, false);
                draw.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            draw.recycle();
        } else throw new IllegalStateException("myBitmap is null");
    }
}
