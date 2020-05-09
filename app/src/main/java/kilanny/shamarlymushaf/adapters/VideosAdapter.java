package kilanny.shamarlymushaf.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.videos.VideoHolder;

public class VideosAdapter extends RecyclerView.Adapter<VideoHolder> {

    private final String[] mData;

    public VideosAdapter(String[] urls) {
        mData = urls;
    }

    @NonNull
    @Override
    public VideoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
        holder.bind(mData[position]);
    }

    @Override
    public void onViewRecycled(@NonNull VideoHolder holder) {
        holder.recycle();
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return mData.length;
    }
}
