package kilanny.shamarlymushaf.adapters.videos;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class VideoHolder extends RecyclerView.ViewHolder {

    public final TextView videoTitle;
    public final AppCompatImageButton imgThumb;

    public VideoHolder(@NonNull View itemView) {
        super(itemView);
        videoTitle = itemView.findViewById(R.id.videoTitle);
        imgThumb = itemView.findViewById(R.id.imgThumb);
    }

    public void recycle() {
        videoTitle.setText(null);
        if (imgThumb != null) {
            Drawable drawable = imgThumb.getDrawable();
            imgThumb.setImageDrawable(null);
            if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                Bitmap bitmap = bitmapDrawable.getBitmap();
                if (bitmap != null) bitmap.recycle();
            }
        }
    }

    public void bind(String youtubeId) {
        recycle();
        if (Utils.isConnected(imgThumb.getContext()) == Utils.CONNECTION_STATUS_CONNECTED) {
            Handler handler = new Handler(imgThumb.getContext().getMainLooper());
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                String videoTitle = Utils.getYoutubeVideoTitle(youtubeId);
                Bitmap bitmap = Utils.downloadYoutubeVideoThumb(youtubeId);
                handler.post(() -> {
                    Log.d("youTubeTitle", videoTitle + "");
                    imgThumb.setImageBitmap(bitmap);
                    this.videoTitle.setText(videoTitle);
                });
            });
        }
        imgThumb.setOnClickListener(v -> {
            Utils.showYoutubeVideoPopup(imgThumb.getContext(), youtubeId);
            AnalyticsTrackers.getInstance(imgThumb.getContext()).logVideoOpened(youtubeId, 1);
        });
    }
}
