package kilanny.shamarlymushaf.activities;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NavUtils;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.HashSet;

import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.fragments.ReciterDetailFragment;
import kilanny.shamarlymushaf.fragments.ReciterListFragment;
import kilanny.shamarlymushaf.util.Utils;

/**
 * An activity representing a single Reciter detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link ReciterListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link ReciterDetailFragment}.
 */
public class ReciterDetailActivity extends AppCompatActivity {

    private ReciterDetailFragment fragment;
    private String myReciter;
    private AsyncTask downloadAll;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.reciter_download_menu, menu);
        menu.getItem(2).setVisible(false);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciter_detail);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Show the Up button in the action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ReciterDetailFragment.ARG_ITEM_ID,
                    myReciter = getIntent().getStringExtra(ReciterDetailFragment.ARG_ITEM_ID));
            fragment = new ReciterDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.reciter_detail_container, fragment)
                    .commit();
            String name = ReciterListFragment.getName(getResources(), myReciter);
            if (name != null) {
                setTitle(name);
                getSupportActionBar().setTitle(name);
            }
        } else {
            fragment = (ReciterDetailFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.reciter_detail_container);
            myReciter = savedInstanceState.getString("reciter");
            //if (fragment.mItem == null)
            //    fragment.mItem = myReciter;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("reciter", myReciter);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadAll != null && !downloadAll.isCancelled())
            downloadAll.cancel(true);
        fragment.cancelActiveOperations();
    }

    @Override
    public void onBackPressed() {
        if (downloadAll != null && !downloadAll.isCancelled()) {
            Utils.showConfirm(this, "تأكيد", "إيقاف التحميل الجاري؟", (dialog, which) -> {
                //downloadAll will be cancelled by onStop()
                ReciterDetailActivity.super.onBackPressed();
            }, null);
        } else super.onBackPressed();
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
            NavUtils.navigateUpTo(this, new Intent(this, ReciterListActivity.class));
            return true;
        }
        switch (item.getItemId()) {
            case R.id.downloadAll:
                if (downloadAll != null) {
                    if (!downloadAll.isCancelled())
                        downloadAll.cancel(true);
                    Toast.makeText(this,
                            "يتم إيقاف التحميل...", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (Utils.isSaveSoundsUri(this) == null) {
                    Toast.makeText(this,
                            "فضلا اختر حافظة تحميل التلاوات أولا",
                            Toast.LENGTH_LONG).show();
                    return true;
                }
                fragment.cancelActiveOperations();
                fragment.setCanDoSingleOperation(false);
                Toast.makeText(this,
                        "يتم التحميل...", Toast.LENGTH_SHORT).show();
                fragment.setCurrentDownloadSurah(1);
                final HashSet<Integer> integers = new HashSet<>();
                downloadAll = Utils.downloadAll(this, myReciter, (surah, ayah) -> {
                    fragment.setSurahProgress(surah, ayah, true);
                    if (integers.add(surah))
                        AnalyticsTrackers.getInstance(this).sendDownloadRecites(myReciter, surah);
                }, result -> {
                    String msg = null;
                    fragment.setCanDoSingleOperation(true);
                    downloadAll = null;
                    switch (result) {
                        case Utils.DOWNLOAD_USER_CANCEL:
                            break;
                        case Utils.DOWNLOAD_OK:
                            msg = "تم تحميل جميع التلاوات بنجاح";
                            break;
                        default:
                            msg = result == Utils.DOWNLOAD_QUOTA_EXCEEDED ?
                                    "تم بلوغ الكمية القصوى للتحميل لهذا اليوم. نرجوا المحاولة غدا، أو اختر تحميل لا محدود من القائمة"
                                    : "فشل التحميل. تأكد من اتصالك بالشبكة ووجود مساحة كافية بجهازك";
                    }
                    fragment.setCurrentDownloadSurah(ReciterDetailFragment.CURRENT_SURAH_NONE);
                    if (msg != null)
                        Utils.showAlert(ReciterDetailActivity.this, "تحميل جميع التلاوات", msg, null);
                }, QuranData.getInstance(this));
                return true;
            case R.id.deleteAll:
                if (downloadAll != null) return true;
                fragment.cancelActiveOperations();
                fragment.setCanDoSingleOperation(false);
                Utils.deleteAll(this, myReciter,
                        progress -> fragment.setSurahProgress(progress, 0, false), () -> {
                    fragment.setCanDoSingleOperation(true);
                    Toast.makeText(ReciterDetailActivity.this,
                            "تم حذف جميع التلاوات لهذا القارئ",
                            Toast.LENGTH_LONG).show();
                    AnalyticsTrackers.getInstance(this).sendDeleteRecites(myReciter);
                });
                return true;
            case R.id.refreshRecites:
                if (downloadAll != null) return true;
                Utils.refreshNumDownloaded(this, myReciter, fragment::reload);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
