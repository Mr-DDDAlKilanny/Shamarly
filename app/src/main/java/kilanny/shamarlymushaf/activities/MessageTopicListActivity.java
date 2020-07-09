package kilanny.shamarlymushaf.activities;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.arch.core.util.Function;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.MessagingTopicAdapter;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.data.msgs.Topic;
import kilanny.shamarlymushaf.data.msgs.UnreadTopicsResult;
import kilanny.shamarlymushaf.fragments.MessageTopicDetailFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

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

    private static final Lock lock = new ReentrantLock(true);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(this);
            Topic[] all = db.topicDao().getAll();
            String[] topics = new String[all.length];
            String[] names = getResources().getStringArray(R.array.topic_names);
            String[] display = getResources().getStringArray(R.array.topic_display_names);
            String[] topicsDisplay = new String[all.length];
            boolean[] sel = new boolean[topics.length];
            for (int i = 0; i < all.length; ++i) {
                if (all[i].name.equals(getString(R.string.dayAyahTopic))) {
                    topicsDisplay[i] = "آية اليوم";
                } else {
                    int idx;
                    for (idx = 0; idx < names.length; ++idx) {
                        if (names[idx].equals(all[i].name))
                            break;
                    }
                    topicsDisplay[i] = display[idx];
                }
                sel[i] = (all[i].subscribedDate != null
                        || all[i].name.equals(getString(R.string.dayAyahTopic)))
                        && all[i].notify;
            }
            new AlertDialog.Builder(this)
                    .setTitle("الإشعارات بالرسائل")
                    .setCancelable(false)
                    .setMultiChoiceItems(topicsDisplay, sel, (dialog1, which1, isChecked) -> {
                        if (isChecked && all[which1].subscribedDate == null
                                && !all[which1].name.equals(getString(R.string.dayAyahTopic))) {
                            Utils.showConfirm(this,
                                    topicsDisplay[which1],
                                    "غير مشترك بهذا الموضوع. اشترك الآن",
                                    "نعم، اشترك",
                                    "إلغاء",
                                    (dialog2, _which) -> {
                                        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_CONNECTED) {
                                            Toast.makeText(this,
                                                    "لا يمكن إتمام العملية وجهازك دون اتصال بالإنترنت",
                                                    Toast.LENGTH_LONG).show();
                                            dialog1.dismiss();
                                            return;
                                        }
                                        AlertDialog mProg = Utils.showIndeterminateProgressDialog(this, "يتم الاتصال بالخادم...");
                                        FirebaseMessaging.getInstance().subscribeToTopic(all[which1].name).addOnCompleteListener(command -> {
                                            if (command.isSuccessful()) {
                                                sel[which1] = true;
                                                all[which1].notify = true;
                                                all[which1].subscribedDate = new Date();
                                                db.topicDao().setSubscribedDate(all[which1].name, new Date());
                                                AnalyticsTrackers.getInstance(this)
                                                        .logTopicSubscribed(all[which1].name);
                                            } else
                                                dialog1.dismiss();
                                            mProg.dismiss();
                                            Toast.makeText(this,
                                                    command.isSuccessful() ? "تمت العملية بنجاح" : "فشلت العملية، حاول ثانية",
                                                    Toast.LENGTH_LONG).show();
                                        });
                                    },
                                    null);
                        } else {
                            sel[which1] = isChecked;
                            all[which1].notify = isChecked;
                        }
                    })
                    .setPositiveButton("حفظ", (dialog1, which1) -> {
                        for (int i = 0; i < sel.length; ++i) {
                            db.topicDao().setNotify(all[i].name, sel[i]);
                        }
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(mAdapter = null);
        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            Map<String, Date> mTopicSubscribedDate = new HashMap<>();
            FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(this);
            String[] topicNames = getResources().getStringArray(R.array.topic_names);
            String[] topicDisplayNames = getResources().getStringArray(R.array.topic_display_names);
            lock.lock(); // prevent fast close and open activity problems
            String dayAyahTopic = getString(R.string.dayAyahTopic);
            try {
                Topic[] all = db.topicDao().getAll();
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
            } finally {
                lock.unlock();
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
