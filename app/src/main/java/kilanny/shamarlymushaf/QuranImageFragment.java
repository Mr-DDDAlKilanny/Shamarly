package kilanny.shamarlymushaf;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import kilanny.shamarlymushaf.util.MyOutOfMemoryException;

public class QuranImageFragment extends Fragment {
    int fragPos;
    MainActivity _activity;
    FullScreenImageAdapter.OnInstantiateQuranImageViewListener listener;
    private boolean finalized = false;
    private QuranImageView imgDisplay, imgDisplay2;
    private ImageView imgDisplayBorders, imgDisplayBorders2;
    private boolean isDualPage;

    public static QuranImageFragment newInstance(int val, boolean isDualPage, MainActivity _activity,
                              FullScreenImageAdapter.OnInstantiateQuranImageViewListener listener) {
        QuranImageFragment imageFragment = new QuranImageFragment();
        imageFragment._activity = _activity;
        imageFragment.listener = listener;
        imageFragment.isDualPage = isDualPage;
        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("pos", val);
        imageFragment.setArguments(args);
        return imageFragment;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (!finalized) {
            finalized = true;
            if (imgDisplay != null) {
                Bitmap old = imgDisplay.myBitmap;
                imgDisplay.setImageBitmap(null);
                if (old != null) old.recycle();
            }
            if (imgDisplay2 != null) {
                Bitmap old = imgDisplay2.myBitmap;
                imgDisplay2.setImageBitmap(null);
                if (old != null) old.recycle();
            }
            imgDisplay = null;
            imgDisplay2 = null;
            if (imgDisplayBorders != null) {
                Bitmap old = ((BitmapDrawable) imgDisplayBorders.getDrawable()).getBitmap();
                imgDisplayBorders.setImageBitmap(null);
                if (old != null) old.recycle();
            }
            if (imgDisplayBorders2 != null) {
                Bitmap old = ((BitmapDrawable) imgDisplayBorders2.getDrawable()).getBitmap();
                imgDisplayBorders2.setImageBitmap(null);
                if (old != null) old.recycle();
            }
            imgDisplayBorders = null;
            imgDisplayBorders2 = null;
            this._activity = null;
            this.listener = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (finalized) { // explicit GC collect causes errors !!
            AnalyticsTrackers.sendFatalError(getActivity(), "QuranImageFragment.onCreate",
                    "finalized = true");
            return;
        }
        fragPos = getArguments() != null ? getArguments().getInt("pos") : -1;
    }

    private static void showProgress(View view, Bitmap bitmap, Bitmap borders, int id, int idBorders) {
        try {
            final QuranImageView imgDisplay = (QuranImageView) view.findViewById(id);
            LinearLayout linlaHeaderProgress = (LinearLayout) view.findViewById(R.id.linlaHeaderProgress);
            imgDisplay.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
            linlaHeaderProgress.setVisibility(bitmap != null ? View.GONE : View.VISIBLE);
            ImageView bord = (ImageView) view.findViewById(idBorders);
            bord.setVisibility(borders == null ? View.GONE : View.VISIBLE);
            bord.setImageBitmap(borders);
            if (borders != null) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        float fw = (1 - (float) QuranData.NORMAL_PAGE_WIDTH / QuranData.BORDERED_PAGE_WIDTH),
                                fh = (1 - (float) QuranData.NORMAL_PAGE_HEIGHT / QuranData.BORDERED_PAGE_HEIGHT);
                        int left = (int) (imgDisplay.getWidth() * fw * 0.53f + 0.5f); //round
                        int right = (int) (imgDisplay.getWidth() * fw * 0.47f + 0.5f);
                        int top = (int) (imgDisplay.getHeight() * fh * 0.47f + 0.5f);
                        int bottom = (int) (imgDisplay.getHeight() * fh * 0.53f + 0.5f);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                RelativeLayout.LayoutParams.MATCH_PARENT
                        );
                        params.setMargins(left, top, right, bottom);
                        imgDisplay.setLayoutParams(params);
                    }
                };
                imgDisplay.post(runnable);
            }
            if (bitmap != null) {
                imgDisplay.setImageBitmap(bitmap);
                imgDisplay.invalidate();
            } else {
                imgDisplay.setImageDrawable(view.getResources().getDrawable(R.drawable.background_tab));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View viewLayout = inflater.inflate(isDualPage ?
                R.layout.layout_fullscreen_image_dual : R.layout.layout_fullscreen_image,
                container, false);
        if (finalized || _activity == null) { // explicit GC collect causes errors !!
            //occurs mostly when activity resume() after stop()
            //or when out-of-memory error occurs
            AnalyticsTrackers.sendFatalError(getActivity(), "QuranImageFragment.onCreateView",
                    "finalized = true");
            return viewLayout;
        }
        if (!isDualPage) {
            imgDisplay = (QuranImageView) viewLayout.findViewById(R.id.quranPage);
            imgDisplayBorders = (ImageView) viewLayout.findViewById(R.id.quranPageBorder);
        } else {
            imgDisplay = (QuranImageView) viewLayout.findViewById(R.id.quranPage_right);
            imgDisplay2 = (QuranImageView) viewLayout.findViewById(R.id.quranPage_left);
            imgDisplayBorders = (ImageView) viewLayout.findViewById(R.id.quranPageBorder_right);
            imgDisplayBorders2 = (ImageView) viewLayout.findViewById(R.id.quranPageBorder_left);
        }
        imgDisplay.pref = _activity.pref;
        if (isDualPage) imgDisplay2.pref = _activity.pref;
        if (fragPos == -1) {
            Display display = _activity.getWindowManager().getDefaultDisplay();
            Point p = new Point();
            display.getSize(p);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = MainActivity.calculateInSampleSize(p.x, p.y);
            Bitmap bitmap = BitmapFactory.decodeResource(_activity.getResources(), R.drawable.pls_download,
                    options);
            imgDisplay.setImageBitmap(bitmap);
            if (isDualPage) imgDisplay2.setImageBitmap(bitmap);
        } else {
            final int position = _activity.adapter.getCount() - fragPos;
            if (position >= 0) {
                DbManager db = DbManager.getInstance(getActivity());
                final Page page = db.getPage(isDualPage ? position * 2 : position);
                final Page page2 = isDualPage ? db.getPage(position * 2 + 1) : null;
                //set pages before anything
                ((QuranImageView) viewLayout.findViewById(isDualPage ? R.id.quranPage_right : R.id.quranPage))
                        .currentPage = page;
                if (isDualPage)
                    ((QuranImageView) viewLayout.findViewById(R.id.quranPage_left)).currentPage = page2;
                showProgress(viewLayout, null, null,
                        isDualPage ? R.id.quranPage_right : R.id.quranPage,
                        isDualPage ? R.id.quranPageBorder_right : R.id.quranPageBorder);
                if (isDualPage)
                    showProgress(viewLayout, null, null,
                            R.id.quranPage_left, R.id.quranPageBorder_left);
                final String memoryFullError = "الذاكرة ممتلئة. لتوفيرها لا تستخدم (غمق الرسم، عرض حدود الصحفحة، الوضع الليلي)";
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Bitmap bitmap = _activity.readPage(page.page);
                            Bitmap tmp = null;
                            SharedPreferences pref = null;
                            if (_activity != null)
                                pref = PreferenceManager.getDefaultSharedPreferences(_activity);
                            if (!finalized && pref != null && MainActivity.isShowPageBorders(pref)) {
                                tmp = _activity.readBorders(page.page);
                            }
                            final Bitmap bitmapBorders = tmp;
                            if (bitmap == null)
                                throw new IllegalAccessException(memoryFullError);
                            if (!finalized) {
                                _activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showProgress(viewLayout, bitmap, bitmapBorders,
                                                isDualPage ? R.id.quranPage_right : R.id.quranPage,
                                                isDualPage ? R.id.quranPageBorder_right : R.id.quranPageBorder);
                                        if (listener != null) {
                                            listener.onInstantiate(new WeakReference<>(imgDisplay), viewLayout);
                                        }
                                    }
                                });
                                if (isDualPage) {
                                    final Bitmap bitmap2 = _activity.readPage(page2.page);
                                    Bitmap tmp2 = null;
                                    if (!finalized && pref != null && MainActivity.isShowPageBorders(pref)) {
                                        tmp2 = _activity.readBorders(page2.page);
                                    }
                                    final Bitmap bitmapBorders2 = tmp2;
                                    if (bitmap2 == null)
                                        throw new IllegalAccessException(memoryFullError);
                                    if (!finalized) _activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showProgress(viewLayout, bitmap2, bitmapBorders2,
                                                    R.id.quranPage_left, R.id.quranPageBorder_left);
                                            if (listener != null) {
                                                listener.onInstantiate(new WeakReference<>(imgDisplay2), viewLayout);
                                            }
                                        }
                                    });
                                    else {
                                        bitmap2.recycle();
                                        if (bitmapBorders2 != null)
                                            bitmapBorders2.recycle();
                                    }
                                }
                            } else {
                                bitmap.recycle();
                                if (bitmapBorders != null)
                                    bitmapBorders.recycle();
                            }
                        } catch (MyOutOfMemoryException | OutOfMemoryError err) {
                            err.printStackTrace();
                            if (!finalized) {
                                _activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Toast.makeText(_activity, memoryFullError, Toast.LENGTH_LONG).show();
                                            _activity.finish();
                                        } catch (Exception ex) {
                                        }
                                    }
                                });
                                AnalyticsTrackers.sendException(_activity, err);
                            }
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            if (!finalized) {
                                _activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(_activity, "خطأ أثناء محاولة فتح الصفحة " +
                                                        ex.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                        _activity.finish();
                                    }
                                });
                                AnalyticsTrackers.sendException(_activity, ex);
                            }
                        }
                    }
                };
                new Thread(runnable).start(); //TODO: causes screen flickering
            }
            viewLayout.setTag(position);
        }
        //container.addView(viewLayout);
        return viewLayout;
    }
}
