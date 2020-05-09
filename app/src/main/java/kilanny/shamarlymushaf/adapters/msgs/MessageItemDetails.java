package kilanny.shamarlymushaf.adapters.msgs;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class MessageItemDetails extends ItemDetailsLookup.ItemDetails<Long> {

    public int position;
    public Long identifier;

    @Override
    public int getPosition() {
        return position;
    }

    @Nullable
    @Override
    public Long getSelectionKey() {
        return identifier;
    }

    @Override
    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
        return false;//don't consider taps as selections => Similar to google photos.
        // if true then consider click as selection
    }

    @Override
    public boolean inDragRegion(@NonNull MotionEvent e) {
        return true;
    }
}
