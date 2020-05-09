package kilanny.shamarlymushaf.adapters.msgs;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class MessageViewHolder extends RecyclerView.ViewHolder {

    public final TextView textView, msgTime, videoTitle;
    public final AppCompatImageButton imgThumb;
    public final MessageItemDetails details;
    public final int viewType;
    private static final int selectedItemMarginInPx = 5;

    private final DateFormat dateFormat = new SimpleDateFormat(
            "EEEE, MMMM d, yyyy HH:mm", Locale.getDefault());

    public MessageViewHolder(@NonNull View itemView, int viewType) {
        super(itemView);
        this.viewType = viewType;
        textView = itemView.findViewById(viewType != 2 ? R.id.msgTxt : R.id.newCount);
        msgTime = viewType == 2 ? null : itemView.findViewById(R.id.msgTime);
        details = viewType == 2 ? null : new MessageItemDetails();
        videoTitle = viewType != 3 ? null : itemView.findViewById(R.id.videoTitle);
        imgThumb = viewType != 3 ? null : itemView.findViewById(R.id.imgThumb);
    }

    public void recycle() {
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

    public void bind(int position, boolean selected, String data, @Nullable Long id, @Nullable Date date) {
        textView.setText(data);

        if (viewType != 2) {
            msgTime.setText(dateFormat.format(date));

            if (viewType == 3) {
                recycle();
                Pattern pattern = Pattern.compile("https://youtu.be/([0-9a-zA-Z_\\-]+)");
                Matcher matcher = pattern.matcher(data);
                boolean success = matcher.find();
                if (success) {
                    if (Utils.isConnected(imgThumb.getContext()) == Utils.CONNECTION_STATUS_CONNECTED) {
                        Handler handler = new Handler(imgThumb.getContext().getMainLooper());
                        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                            String videoTitle = Utils.getYoutubeVideoTitle(matcher.group(1));
                            Bitmap bitmap = Utils.downloadYoutubeVideoThumb(matcher.group(1));
                            handler.post(() -> {
                                Log.d("youTubeTitle", videoTitle + "");
                                imgThumb.setImageBitmap(bitmap);
                                this.videoTitle.setText(videoTitle);
                            });
                        });
                    }
                }
                imgThumb.setOnClickListener(v -> {
                    if (success) {
                        Utils.showYoutubeVideoPopup(imgThumb.getContext(), matcher.group(1));
                        AnalyticsTrackers.getInstance(imgThumb.getContext())
                                .logVideoOpened(matcher.group(1), 2);
                    }
                });
            }

            details.identifier = id;
            details.position = position;

            if (selected) {
                textView.setAlpha(0.5f);
                msgTime.setAlpha(0.5f);
                ((ViewGroup.MarginLayoutParams) textView.getLayoutParams()).setMargins(
                        selectedItemMarginInPx, selectedItemMarginInPx, selectedItemMarginInPx,
                        selectedItemMarginInPx);
            } else {
                textView.setAlpha(0.99f);
                msgTime.setAlpha(0.99f);
                ((ViewGroup.MarginLayoutParams) textView.getLayoutParams()).setMargins(
                        0, 0, 0, 0);
            }
        }
    }
}