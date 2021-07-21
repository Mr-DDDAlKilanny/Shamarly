package kilanny.shamarlymushaf.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MessageTopicDetailActivity;
import kilanny.shamarlymushaf.activities.MessageTopicListActivity;
import kilanny.shamarlymushaf.adapters.MessagesAdapter;
import kilanny.shamarlymushaf.adapters.msgs.MessageItemDetailsLookup;
import kilanny.shamarlymushaf.adapters.msgs.MessageKeyProvider;
import kilanny.shamarlymushaf.data.msgs.FirebaseMessagingDb;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

/**
 * A fragment representing a single MessageTopic detail screen.
 * This fragment is either contained in a {@link MessageTopicListActivity}
 * in two-pane mode (on tablets) or a {@link MessageTopicDetailActivity}
 * on handsets.
 */
public class MessageTopicDetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_ITEM_TITLE = "item_title";
    public static final String ARG_IS_SUBSCRIBED = "is_subscribed";

    private String mItem, mTitle;
    private boolean mIsSubscribed;
    private AlertDialog mProg;
    private LinearLayoutManager mLinearLayoutManager;
    private MessagesAdapter mAdapter;
    private OnFragmentEventListener mFragmentEventListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public MessageTopicDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = getArguments().getString(ARG_ITEM_ID);
            mTitle = getArguments().getString(ARG_ITEM_TITLE);
            mIsSubscribed = getArguments().getBoolean(ARG_IS_SUBSCRIBED);
        }
    }

    private void onFabClick(View view) {
        FragmentActivity activity = getActivity();
        if (mProg != null || activity == null) return;
        Utils.showConfirm(activity,
                "تأكيد",
                mIsSubscribed ? "إيقاف الرسائل من هذا الموضوع؟" : "تفعيل الرسائل من هذا الموضوع؟",
                (dialog, which) -> {
                    if (Utils.isConnected(activity) != Utils.CONNECTION_STATUS_CONNECTED) {
                        Snackbar.make(view, "لا يمكن إتمام العملية وجهازك دون اتصال بالإنترنت",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Task<Void> task;
                    mProg = Utils.showIndeterminateProgressDialog(activity, "يتم الاتصال بالخادم...");
                    Context context = activity.getApplicationContext();
                    if (mIsSubscribed) {
                        task = FirebaseMessaging.getInstance().unsubscribeFromTopic(mItem);
                    } else {
                        task = FirebaseMessaging.getInstance().subscribeToTopic(mItem);
                    }
                    task.addOnCompleteListener(command -> {
                        if (command.isSuccessful()) {
                            mIsSubscribed = !mIsSubscribed;
                            Date date = mIsSubscribed ? new Date() : null;
                            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                                FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(context);
                                db.topicDao().setSubscribedDate(mItem, date);
                            });
                            if (mFragmentEventListener != null)
                                mFragmentEventListener.onSubscriptionUpdated(mItem, date);
                            AnalyticsTrackers trackers = AnalyticsTrackers.getInstance(context);
                            if (mIsSubscribed)
                                trackers.logTopicSubscribed(mItem);
                            else
                                trackers.logTopicUnsubscribed(mItem);
                        }
                        mProg.dismiss();
                        mProg = null;
                        Snackbar.make(view, command.isSuccessful() ? "تمت العملية بنجاح" : "فشلت العملية، حاول ثانية",
                                Snackbar.LENGTH_LONG).show();
                    });
                }, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.messagetopic_detail, container, false);

        if (mItem != null) {
            CollapsingToolbarLayout appBarLayout = rootView.findViewById(R.id.toolbar_layout);
            appBarLayout.setTitle(mTitle);
            String[] topicDescrs = getResources().getStringArray(R.array.topic_descriptions);
            TextView txtTopicDescr = rootView.findViewById(R.id.txtTopicDescr);
            boolean mCanSubscribe;
            if (mItem.equals(getString(R.string.dayAyahTopic))) {
                txtTopicDescr.setText(topicDescrs[0]);
                mCanSubscribe = false;
            } else {
                mCanSubscribe = true;
                String[] topics = getResources().getStringArray(R.array.topic_names);
                boolean ex = false;
                for (int i = 0; i < topics.length; ++i) {
                    if (topics[i].equals(mItem)) {
                        txtTopicDescr.setText(topicDescrs[i + 1]);
                        ex = true;
                        break;
                    }
                }
                if (!ex) txtTopicDescr.setText("");
            }

            FloatingActionButton fab = rootView.findViewById(R.id.fab);
            if (mCanSubscribe)
                fab.setOnClickListener(this::onFabClick);
            else
                fab.setVisibility(View.INVISIBLE);

            setupRecyclerView(rootView.findViewById(R.id.recycler_view));
        }

        return rootView;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        mLinearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLinearLayoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            private boolean hasFixedLastItemNotVisible = false;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!hasFixedLastItemNotVisible &&
                        !recyclerView.canScrollVertically(10) &&
                        newState==RecyclerView.SCROLL_STATE_IDLE) {
                    hasFixedLastItemNotVisible = true;
                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            }
        });

        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            FirebaseMessagingDb db = FirebaseMessagingDb.getInstance(getActivity());
            int count = db.receivedTopicMessageDao().count(mItem);
            int unreadCount = db.receivedTopicMessageDao().unreadCount(mItem);
            FragmentActivity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                mAdapter = new MessagesAdapter(getActivity(), mLinearLayoutManager,
                        mItem, count, unreadCount, input -> {
                    if (mFragmentEventListener != null)
                        mFragmentEventListener.onUnreadCountChanged(mItem, input);
                    return null;
                });
                recyclerView.setAdapter(mAdapter);

                mAdapter.setSelectionTracker(new SelectionTracker.Builder<>(
                        "my-uri-selection", recyclerView, new MessageKeyProvider(mAdapter),
                        new MessageItemDetailsLookup(recyclerView), StorageStrategy.createLongStorage())
                        .build());

                if (count > 0) {
                    int scroll = unreadCount == 0 ? count - 1 : count - unreadCount;
                    mLinearLayoutManager.scrollToPositionWithOffset(scroll, 20);
                }
            });
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentEventListener)
            mFragmentEventListener = (OnFragmentEventListener) context;
    }

    @Override
    public void onStop() {
        Context context = getContext();
        if (mItem != null && context != null && mAdapter != null && mAdapter.getReadCount() > 0)
            AnalyticsTrackers.getInstance(context).logMessagesRead(mItem, mAdapter.getReadCount());
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentEventListener = null;
    }

    public interface OnFragmentEventListener {
        void onSubscriptionUpdated(String topic, @Nullable Date subsciptionDate);
        void onUnreadCountChanged(String topic, int unreadCount);
    }
}
