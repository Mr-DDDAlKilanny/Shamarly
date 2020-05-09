package kilanny.shamarlymushaf.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MessageTopicDetailActivity;
import kilanny.shamarlymushaf.activities.MessageTopicListActivity;
import kilanny.shamarlymushaf.fragments.MessageTopicDetailFragment;

public class MessagingTopicAdapter extends RecyclerView.Adapter<MessagingTopicAdapter.ViewHolder> {

    public static class Topic {
        public final String id;
        public final String name;
        public Date subscribedDate;
        public int _newMessages;

        public Topic(String id, String name, Date subscribedDate, int _newMessages) {
            this.id = id;
            this.name = name;
            this.subscribedDate = subscribedDate;
            this._newMessages = _newMessages;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final MessageTopicListActivity mParentActivity;
    public final List<MessagingTopicAdapter.Topic> mValues;
    private final boolean mTwoPane;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            MessagingTopicAdapter.Topic item = (MessagingTopicAdapter.Topic) view.getTag();
            if (mTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putString(MessageTopicDetailFragment.ARG_ITEM_ID, item.id);
                arguments.putString(MessageTopicDetailFragment.ARG_ITEM_TITLE, item.name);
                arguments.putBoolean(MessageTopicDetailFragment.ARG_IS_SUBSCRIBED, item.subscribedDate != null);
                MessageTopicDetailFragment fragment = new MessageTopicDetailFragment();
                fragment.setArguments(arguments);
                mParentActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.messagetopic_detail_container, fragment)
                        .commitAllowingStateLoss();
            } else {
                Context context = view.getContext();
                Intent intent = new Intent(context, MessageTopicDetailActivity.class);
                intent.putExtra(MessageTopicDetailFragment.ARG_ITEM_ID, item.id);
                intent.putExtra(MessageTopicDetailFragment.ARG_ITEM_TITLE, item.name);
                intent.putExtra(MessageTopicDetailFragment.ARG_IS_SUBSCRIBED, item.subscribedDate != null);

                context.startActivity(intent);
            }
        }
    };

    public MessagingTopicAdapter(MessageTopicListActivity parent, List<MessagingTopicAdapter.Topic> items,
                          boolean twoPane) {
        mValues = items;
        mParentActivity = parent;
        mTwoPane = twoPane;
    }

    @Override
    public MessagingTopicAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.messagetopic_list_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MessagingTopicAdapter.ViewHolder holder, int position) {
        int c = mValues.get(position)._newMessages;
        holder.mBadgeView.setText(c > 99 ? "99" : String.format(Locale.ENGLISH, "%d", c));
        holder.mBadgeView.setVisibility(c > 0 ? View.VISIBLE : View.INVISIBLE);
        holder.mContentView.setText(mValues.get(position).name);

        holder.itemView.setTag(mValues.get(position));
        holder.itemView.setOnClickListener(mOnClickListener);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mBadgeView;
        final TextView mContentView;

        ViewHolder(View view) {
            super(view);
            mBadgeView = view.findViewById(R.id.messages_badge);
            mContentView = view.findViewById(R.id.content);
        }
    }
}