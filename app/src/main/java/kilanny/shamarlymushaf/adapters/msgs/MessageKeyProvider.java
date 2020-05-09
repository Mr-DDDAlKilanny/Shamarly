package kilanny.shamarlymushaf.adapters.msgs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;

import kilanny.shamarlymushaf.adapters.MessagesAdapter;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;

public class MessageKeyProvider extends ItemKeyProvider<Long> {

    private MessagesAdapter mAdapter;

    public MessageKeyProvider(MessagesAdapter adapter) {
        super(SCOPE_CACHED);
        mAdapter = adapter;
    }

    @Nullable
    @Override
    public Long getKey(int position) {
//            if (mAdapter._newCountMessagePos == position) return null;
        if (mAdapter._newCountMessagePos >= 0 && mAdapter._newCountMessagePos <= position)
            --position;
        FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(mAdapter.mActivity);
        return db.receivedTopicMessageDao().getRange(mAdapter.mTopic, position, 1)[0].id;
    }

    @Override
    public int getPosition(@NonNull Long key) {
        FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(mAdapter.mActivity);
        int i = (int) db.receivedTopicMessageDao().getPosition(mAdapter.mTopic, key);
        return i + (mAdapter._newCountMessagePos >= 0 && mAdapter._newCountMessagePos <= i ? 1 : 0);
    }
}

