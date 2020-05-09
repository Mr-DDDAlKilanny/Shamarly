package kilanny.shamarlymushaf.activities;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.NavUtils;

import android.view.MenuItem;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.fragments.MessageTopicDetailFragment;

/**
 * An activity representing a single MessageTopic detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link MessageTopicListActivity}.
 */
public class MessageTopicDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messagetopic_detail);
        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Create the detail fragment and add it to the activity
        // using a fragment transaction.
        Bundle arguments = new Bundle();
        arguments.putString(MessageTopicDetailFragment.ARG_ITEM_ID,
                getIntent().getStringExtra(MessageTopicDetailFragment.ARG_ITEM_ID));
        arguments.putString(MessageTopicDetailFragment.ARG_ITEM_TITLE,
                getIntent().getStringExtra(MessageTopicDetailFragment.ARG_ITEM_TITLE));
        arguments.putBoolean(MessageTopicDetailFragment.ARG_IS_SUBSCRIBED,
                getIntent().getBooleanExtra(MessageTopicDetailFragment.ARG_IS_SUBSCRIBED, false));
        MessageTopicDetailFragment fragment = new MessageTopicDetailFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.messagetopic_detail_container, fragment)
                .commitAllowingStateLoss();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, MessageTopicListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
