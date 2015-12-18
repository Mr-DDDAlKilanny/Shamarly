package kilanny.shamarlymushaf;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

public class QuranImageFragment extends Fragment {
    int fragPos;
    MainActivity _activity;
    FullScreenImageAdapter.OnInstantiateQuranImageViewListener listener;
    private boolean finalized = false;
    private WeakReference<QuranImageView> imgDisplay;

    public static QuranImageFragment newInstance(int val, MainActivity _activity,
                              FullScreenImageAdapter.OnInstantiateQuranImageViewListener listener) {
        QuranImageFragment imageFragment = new QuranImageFragment();
        imageFragment._activity = _activity;
        imageFragment.listener = listener;
        // Supply val input as an argument.
        Bundle args = new Bundle();
        args.putInt("pos", val);
        imageFragment.setArguments(args);
        return imageFragment;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        imgDisplay.clear();
        this._activity = null;
        this.listener = null;
        finalized = true;
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

    public static void showProgress(View view, Bitmap bitmap, Page page) {
        QuranImageView imgDisplay = (QuranImageView) view.findViewById(R.id.quranPage);
        LinearLayout linlaHeaderProgress = (LinearLayout) view.findViewById(R.id.linlaHeaderProgress);
        imgDisplay.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
        linlaHeaderProgress.setVisibility(bitmap != null ? View.GONE : View.VISIBLE);
        if (bitmap != null) {
            imgDisplay.currentPage = page;
            imgDisplay.setImageBitmap(bitmap);
            imgDisplay.invalidate();
        } else {
            imgDisplay.setImageDrawable(view.getResources().getDrawable(R.drawable.background_tab));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View viewLayout = inflater.inflate(R.layout.layout_fullscreen_image, container, false);
        if (finalized) { // explicit GC collect causes errors !!
            AnalyticsTrackers.sendFatalError(getActivity(), "QuranImageFragment.onCreateView",
                    "finalized = true");
            return viewLayout;
        }
        imgDisplay = new WeakReference<>((QuranImageView) viewLayout.findViewById(R.id.quranPage));
        imgDisplay.get().pref = _activity.pref;
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
            imgDisplay.get().setImageBitmap(bitmap);
        } else {
            int position = FullScreenImageAdapter.MAX_PAGE - fragPos;
            if (position > 1) {
                showProgress(viewLayout, null, null);
            }
            viewLayout.setTag(position);
        }
        //container.addView(viewLayout);
        if (listener != null)
            listener.onInstantiate(imgDisplay, viewLayout);
        return viewLayout;
    }
}
