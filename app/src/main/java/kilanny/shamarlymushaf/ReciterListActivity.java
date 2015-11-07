package kilanny.shamarlymushaf;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;
import java.util.HashSet;


/**
 * An activity representing a list of Reciters. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ReciterDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ReciterListFragment} and the item details
 * (if present) is a {@link ReciterDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ReciterListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ReciterListActivity extends ActionBarActivity
        implements ReciterListFragment.Callbacks,
        DirectoryChooserFragment.OnFragmentInteractionListener {

    private ReciterDetailFragment fragment;
    private AsyncTask downloadAll;
    private boolean itemChanged = false;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private Setting setting;

    private DirectoryChooserFragment mDialog;

    private boolean forceDialogSelection;

    private void chooseDir(final boolean force) {
        forceDialogSelection = force;
        Utils.showAlert(this, "تحميل التلاوات",
                "فضلا اضغط موافق، ثم اختر الحافظة التي سيتم التحميل فيها",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                                .newDirectoryName("DialogSample")
                                .build();
                        mDialog = DirectoryChooserFragment.newInstance(config);
                        mDialog.setCancelable(!force);
                        mDialog.show(getFragmentManager(), null);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reciter_list);
        if (!Utils.isExternalStorageWritable()) {
            Toast.makeText(this, "عفوا الذاكرة في جهازك غير قابلة للكتابة",
                    Toast.LENGTH_LONG).show();
            NavUtils.navigateUpFromSameTask(this);
            finish();
            return;
        }
        setting = Setting.getInstance(this);
        if (setting.saveSoundsDirectory == null || !new File(setting.saveSoundsDirectory).exists())
            chooseDir(true);
        // Show the Up button in the action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        if (findViewById(R.id.reciter_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ReciterListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.reciter_list))
                    .setActivateOnItemClick(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.reciter_download_menu, menu);
        if (findViewById(R.id.reciter_detail_container) == null) {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
        }
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadAll != null && !downloadAll.isCancelled())
            downloadAll.cancel(true);
        if (fragment != null && fragment.isDownloadActive())
            fragment.cancelActiveOperations();
    }

    @Override
    public void onBackPressed() {
        if (downloadAll != null && !downloadAll.isCancelled()) {
            Utils.showConfirm(this, "تأكيد", "إيقاف التحميل الجاري؟", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //downloadAll will be cancelled by onStop()
                    ReciterListActivity.super.onBackPressed();
                }
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
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        if (item.getItemId() == R.id.chooseDownloadDir)
            chooseDir(false);
        else if (fragment != null) {
            final String myReciter = fragment.mItem;
            switch (item.getItemId()) {
                case R.id.downloadAll:
                    if (downloadAll != null) {
                        if (!downloadAll.isCancelled())
                            downloadAll.cancel(true);
                        Toast.makeText(this,
                                "يتم إيقاف التحميل...", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    fragment.cancelActiveOperations();
                    fragment.setCanDoSingleOperation(false);
                    Toast.makeText(this,
                            "يتم التحميل...", Toast.LENGTH_SHORT).show();
                    fragment.setCurrentDownloadSurah(1);
                    final HashSet<Integer> integers = new HashSet<>();
                    downloadAll = Utils.downloadAll(this, myReciter, new DownloadAllProgressChangeListener() {
                        @Override
                        public void onProgressChange(int surah, int ayah) {
                            fragment.setSurahProgress(surah, ayah, true);
                            integers.add(surah);
                        }
                    }, new DownloadTaskCompleteListener() {
                        @Override
                        public void taskCompleted(int result) {
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
                                    msg = "فشل تحميل التلاوات. تأكد من اتصالك بالإنترنت ووجود مساحة كافية";
                            }
                            if (!integers.isEmpty())
                                AnalyticsTrackers
                                        .sendDownloadRecites(ReciterListActivity.this,
                                                myReciter, integers);
                            fragment.setCurrentDownloadSurah(ReciterDetailFragment.CURRENT_SURAH_NONE);
                            if (msg != null)
                                Utils.showAlert(ReciterListActivity.this, "تحميل جميع التلاوات", msg, null);
                        }
                    });
                    return true;
                case R.id.deleteAll:
                    if (downloadAll != null) return true;
                    itemChanged = false;
                    fragment.cancelActiveOperations();
                    fragment.setCanDoSingleOperation(false);
                    Utils.deleteAll(this, myReciter, new RecoverySystem.ProgressListener() {
                        @Override
                        public void onProgress(int progress) {
                            if (!itemChanged)
                                fragment.setSurahProgress(progress, 0, false);
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            fragment.setCanDoSingleOperation(true);
                            Toast.makeText(ReciterListActivity.this,
                                    "تم حذف جميع التلاوات لهذا القارئ",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method from {@link ReciterListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final String id) {
        if (mTwoPane) {
            if (downloadAll != null && !downloadAll.isCancelled()) {
                downloadAll.cancel(true);
                Toast.makeText(this,
                        "تم إيقاف التحميل", Toast.LENGTH_SHORT).show();
            }
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ReciterDetailFragment.ARG_ITEM_ID, id);
            itemChanged = true;
            if (fragment != null && fragment.isDownloadActive())
                fragment.cancelActiveOperations();
            fragment = new ReciterDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.reciter_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            fragment = null;
            Intent detailIntent = new Intent(this, ReciterDetailActivity.class);
            detailIntent.putExtra(ReciterDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    @Override
    public void onSelectDirectory(@NonNull String path) {
        setting.saveSoundsDirectory = path;
        setting.save(this);
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
        if (forceDialogSelection) {
            Utils.showConfirm(this, "اختيار الحافظة", "لا بد من اختيار حافظة للتحميل. اختيار الآن؟", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    chooseDir(true);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    NavUtils.navigateUpFromSameTask(ReciterListActivity.this);
                    finish();
                }
            });
        }
    }
}
