package kilanny.shamarlymushaf.adapters.msgs;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class MessageItemDetailsLookup extends ItemDetailsLookup<Long> {

    private final RecyclerView mRecyclerView;

    public MessageItemDetailsLookup(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            RecyclerView.ViewHolder viewHolder = mRecyclerView.getChildViewHolder(view);
            if (viewHolder instanceof MessageViewHolder) {
                MessageViewHolder testItem = (MessageViewHolder) viewHolder;
                return testItem.viewType == 2 ? null : testItem.details;
            }
        }
        return null;
    }
}
