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
        if (imgDisplay != null && imgDisplay.get() != null) {
            imgDisplay.get().finalize();
            imgDisplay = null;
        }
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
        Bitmap bitmap;
        if (fragPos == -1) {
            Display display = _activity.getWindowManager().getDefaultDisplay();
            Point p = new Point();
            display.getSize(p);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDither = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = MainActivity.calculateInSampleSize(p.x, p.y);
            bitmap = BitmapFactory.decodeResource(_activity.getResources(), R.drawable.pls_download,
                    options);
        } else {
            int position = FullScreenImageAdapter.MAX_PAGE - fragPos;
            if (position > 1) {
                DbManager db = DbManager.getInstance(_activity);
                imgDisplay.get().currentPage = db.getPage(position);
            }
            bitmap = _activity.readPage(position);
            viewLayout.setTag(position);
        }
        imgDisplay.get().setImageBitmap(bitmap);
        //container.addView(viewLayout);
        if (listener != null)
            listener.onInstantiate(imgDisplay, viewLayout);
        return viewLayout;
    }
}
