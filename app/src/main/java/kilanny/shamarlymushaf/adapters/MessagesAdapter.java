package kilanny.shamarlymushaf.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.Locale;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.msgs.MessageViewHolder;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.data.msgs.ReceivedTopicMessage;
import kilanny.shamarlymushaf.data.msgs.ReceivedTopicMessageDao;

public class MessagesAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    public final Activity mActivity;
    public final String mTopic;
    private final int mCount;
    private final LinearLayoutManager mLayoutManager;
    private final Function<Integer, Void> mUnreadCountUpdated;
    //private final Map<Integer, ReceivedTopicMessage> cache = new HashMap<>();
    public final int _newCountMessagePos;
    private final boolean[] mUpdatedRead;

    private int mReadCount = 0;
    private SelectionTracker<Long> mSelectionTracker;
    private int mNewCount;

    public int getReadCount() {
        return mReadCount;
    }

    public MessagesAdapter(Activity activity, LinearLayoutManager layoutManager,
                           String topic, int totalCount, int newCount, Function<Integer, Void> onUnreadCountUpdated) {
        mActivity = activity;
        mTopic = topic;
        mCount = totalCount;
        mLayoutManager = layoutManager;
        if (newCount == 0)
            _newCountMessagePos = -1;
        else
            _newCountMessagePos = mCount - newCount;
        mNewCount = newCount;
        mUpdatedRead = new boolean[mCount];
        mUnreadCountUpdated = onUnreadCountUpdated;
    }

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.mSelectionTracker = selectionTracker;
    }

//        public void setPokemonClickListener(@Nullable PokemonClickListener pokemonClickListener) {
//            this.mPokemonClickListener = pokemonClickListener;
//        }


    @Override
    public int getItemViewType(int position) {
        if (position == _newCountMessagePos)
            return 2;
        int iData = position - (_newCountMessagePos >= 0 && _newCountMessagePos < position
                ? 1 : 0);
        FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(mActivity);
        String m = db.receivedTopicMessageDao().getRange(mTopic, iData, 1)[0].msg;
        if (m.toLowerCase().contains("https://youtu.be/"))
            return 3;
        return 1;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case 1:
                layoutId = R.layout.item_message_send;
                break;
            case 2:
                layoutId = R.layout.item_message_new_count;
                break;
            case 3:
                layoutId = R.layout.item_message_youtube;
                break;
            default:
                throw new RuntimeException("Invalid view type");
        }
        return new MessageViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false), viewType);
    }

    @Override
    public void onViewRecycled(@NonNull MessageViewHolder holder) {
        holder.recycle();
        super.onViewRecycled(holder);
    }

    //    public int getSelectedItemCount() {
//        int count = 0;
//        if (mSelectionTracker != null) {
//            for (String mDatum : mData) {
//                if (mSelectionTracker.isSelected(mDatum)) {
//                    ++count;
//                }
//            }
//        }
//        return count;
//    }

    private void doBind(int position, MessageViewHolder holder, ReceivedTopicMessage msg) {
        boolean isSelected = false;
        if (mSelectionTracker != null) {
            if (mSelectionTracker.isSelected(msg.id)) {
                isSelected = true;
            }
        }
        holder.bind(position, isSelected, msg.msg, msg.id, msg.addDate);
        //if (isSelected || getSelectedItemCount() > 0)
        //    mActivity.startSupportActionMode();
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        int iData = position - (_newCountMessagePos >= 0 && _newCountMessagePos < position
                ? 1 : 0);
        if (position == _newCountMessagePos) {
            holder.bind(position, false, String.format(Locale.ENGLISH,
                    "%d رسالة جديدة", getItemCount() - _newCountMessagePos - 1),
                    null, null);
        //} else if (cache.containsKey(iData)) {
        //    doBind(position, holder, cache.get(iData));
        } else {
            FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(mActivity);
//            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
//                final int max = 250;
//                ReceivedTopicMessage[] messages = db.receivedTopicMessageDao().getRange(mTopic,
//                        Math.max(iData - max + 1, 0), max);
//                ReceivedTopicMessage msg = messages[messages.length - 1];
//                mActivity.runOnUiThread(() -> doBind(position, holder, msg));
//                for (int i = 0; i < messages.length; ++i)
//                    cache.put(iData - messages.length + i, messages[i]);
//                if (iData - max + 1 > 0) {
//                    messages = db.receivedTopicMessageDao().getRange(mTopic, iData + 1, max);
//                    for (int i = 0; i < messages.length; ++i)
//                        cache.put(iData + i + 1, messages[i]);
//                }
//                ReceivedTopicMessage msg = db.receivedTopicMessageDao().getRange(mTopic, iData, 1)[0];
//                mActivity.runOnUiThread(() -> doBind(position, holder, msg));
//            });
            ReceivedTopicMessage msg = db.receivedTopicMessageDao().getRange(mTopic, iData, 1)[0];
            if (msg.readDate != null)
                mUpdatedRead[iData] = true;
            doBind(position, holder, msg);
        }
        Log.d("onBindViewHolder", "pos = " + position);


        markRead(mLayoutManager.findFirstVisibleItemPosition(),
                mLayoutManager.findLastCompletelyVisibleItemPosition());
    }

    private void markRead(int startPos, int endPos) {
        Log.d("markAsRead", "" + startPos + "," + endPos);
        if (startPos < 0) return;
        if (endPos == -1)
            endPos = startPos;
        ReceivedTopicMessageDao dao = FirebaseMessagingDb.getInstance(mActivity)
                .receivedTopicMessageDao();
        for (int i = startPos; i <= endPos; ++i) {
            int iData = i - (_newCountMessagePos >= 0 && _newCountMessagePos < i ? 1 : 0);
            boolean value = mUpdatedRead[iData];
            if (!value)
                mUpdatedRead[iData] = true;
            if (!value) {
                ReceivedTopicMessage msg = dao.getRange(mTopic, iData, 1)[0];
                if (msg.readDate == null) {
                    dao.markAsRead(new Date(), msg.id);
                    mUnreadCountUpdated.apply(--mNewCount);
                    ++mReadCount;
                    Log.d("markAsRead_done", msg.msg);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mCount + (_newCountMessagePos >= 0 ? 1 : 0);
    }
}
