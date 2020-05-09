package kilanny.shamarlymushaf.activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.arch.core.util.Function;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.MessagingTopicAdapter;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.data.msgs.Topic;
import kilanny.shamarlymushaf.data.msgs.UnreadTopicsResult;
import kilanny.shamarlymushaf.fragments.MessageTopicDetailFragment;
import kilanny.shamarlymushaf.util.AppExecutors;

/**
 * An activity representing a list of MessageTopics. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link MessageTopicDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class MessageTopicListActivity extends AppCompatActivity
        implements MessageTopicDetailFragment.OnFragmentEventListener {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private MessagingTopicAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messagetopic_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        if (findViewById(R.id.messagetopic_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setupRecyclerView(findViewById(R.id.messagetopic_list));
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(mAdapter = null);
        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            Map<String, Date> mTopicSubscribedDate = new HashMap<>();
            FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(this);
            String[] topicNames = getResources().getStringArray(R.array.topic_names);
            String[] topicDisplayNames = getResources().getStringArray(R.array.topic_display_names);
            Topic[] all = db.topicDao().getAll();
            String dayAyahTopic = getString(R.string.dayAyahTopic);
            Function<String, Date> insertTopicIfNotExists = input -> {
                for (Topic t : all) {
                    if (t.name.equals(input)) {
                        mTopicSubscribedDate.put(input, t.subscribedDate);
                        return t.subscribedDate;
                    }
                }
                db.topicDao().insert(new Topic(input, null));
                mTopicSubscribedDate.put(input, null);
                return null;
            };
            insertTopicIfNotExists.apply(dayAyahTopic);
            for (String topic : topicNames) {
                insertTopicIfNotExists.apply(topic);
            }

            UnreadTopicsResult[] unreadTopics = db.receivedTopicMessageDao().getUnreadTopics();
            Function<String, Integer> getCount = input -> {
                for (UnreadTopicsResult result : unreadTopics) {
                    if (result.topic.equals(input)) return result.unreadCount;
                }
                return 0;
            };
            runOnUiThread(() -> {
                ArrayList<MessagingTopicAdapter.Topic> topics = new ArrayList<>();
                topics.add(new MessagingTopicAdapter.Topic(dayAyahTopic, "كل يوم آية",
                        new Date(), getCount.apply(dayAyahTopic)));
                for (int i = 0; i < topicNames.length; ++i) {
                    topics.add(new MessagingTopicAdapter.Topic(
                            topicNames[i],
                            topicDisplayNames[i],
                            mTopicSubscribedDate.get(topicNames[i]),
                            getCount.apply(topicNames[i])));
                }
                recyclerView.setAdapter(mAdapter =
                        new MessagingTopicAdapter(this, topics, mTwoPane));
            });
        });
    }

    @Override
    public void onSubscriptionUpdated(String topic, @Nullable Date subsciptionDate) {
        if (mAdapter != null) {
            for (MessagingTopicAdapter.Topic topic1 : mAdapter.mValues) {
                if (topic1.id.equals(topic)) {
                    topic1.subscribedDate = subsciptionDate;
                    mAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    @Override
    public void onUnreadCountChanged(String topic, int unreadCount) {
        if (mAdapter != null) {
            for (MessagingTopicAdapter.Topic topic1 : mAdapter.mValues) {
                if (topic1.id.equals(topic)) {
                    topic1._newMessages = unreadCount;
                    mAdapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }
}
