package kilanny.shamarlymushaf.activities;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.util.HashSet;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.fragments.ReciterDetailFragment;
import kilanny.shamarlymushaf.fragments.ReciterListFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.DownloadAllProgressChangeListener;
import kilanny.shamarlymushaf.util.DownloadTaskCompleteListener;
import kilanny.shamarlymushaf.util.Utils;


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
public class ReciterListActivity extends AppCompatActivity
        implements ReciterListFragment.Callbacks,
        ChooserDialog.Result,
        DialogInterface.OnCancelListener,
        DialogInterface.OnClickListener {

    public static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    private ReciterDetailFragment fragment;
    private AsyncTask downloadAll;
    private boolean itemChanged = false;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private Setting setting;

    private boolean forceDialogSelection;

    private void chooseDir(boolean force) {
        forceDialogSelection = force;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Utils.showAlert(this, "تحميل التلاوات",
                    "فضلا اضغط موافق، ثم اختر الحافظة التي سيتم التحميل فيها",
                    (dialog, which) -> new ChooserDialog(ReciterListActivity.this)
                            .withFilter(true, false)
                            .withStringResources("اختيار حافظة التحميل", "اختيار", "إلغاء")
                            .withStartFile(force ? null : setting.saveSoundsDirectory)
                            .withOnCancelListener(ReciterListActivity.this)
                            .withChosenListener(ReciterListActivity.this)
                            .withNegativeButtonListener(ReciterListActivity.this)
                            .build()
                            .show());
        } else {
            setting.saveSoundsDirectory = getExternalFilesDir(null).getAbsolutePath();
        }
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

        initWithPermissionCheck(true);
    }

    private void initWithPermissionCheck(boolean shouldShowExplainDlg) {
        if (checkStoragePermission(this, shouldShowExplainDlg,
                () -> initWithPermissionCheck(false))) {
            init();
        }
    }

    public static boolean checkStoragePermission(Activity context, boolean shouldShowExplainDlg, Runnable onOk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowExplainDlg) {
                    Utils.showAlert(context,
                            "صلاحية القرص",
                            context.getString(R.string.request_storage_permission_msg),
                            (dialog, which) -> onOk.run());
                } else {
                    context.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                }
                return false;
            } else
                return true;
        }
        return true;
    }

    private void init() {
        setting = Setting.getInstance(this);
        if (setting.saveSoundsDirectory == null || !new File(setting.saveSoundsDirectory).exists())
            chooseDir(true);
        else {
            SerializableInFile<Integer> appResponse = new SerializableInFile<>(
                    getApplicationContext(), "down__st", 0);
            if (appResponse.getData() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("تحميل مصحف القارئ كاملا")
                        .setMessage("تم فتح إمكانية تحميل القارئ بالكامل بدون كمية يومية!")
                        .setPositiveButton("أرني كيف", (dialogInterface, i) -> {
                            Toast.makeText(this,
                                    "اضغط على علامة القائمة (النقاط الثلاث الرأسية) لفتح هذه الخاصية",
                                    Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("لا تخبرني ثانية", (dialogInterface, i) -> {
                            appResponse.setData(-1, this);
                        })
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                init();
            else {
                Toast.makeText(getApplicationContext(),
                        "لقد رفضت منح التطبيق صلاحية الكتابة في الذاكرة لذلك لا يمكنك التحميل. فضلا امنح الصلاحية من: إعدادات الجهاز - التطبيقات - مصحف الشمرلي",
                        Toast.LENGTH_LONG).show();
                finish();
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            menu.getItem(2).setVisible(false);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadAll != null && !downloadAll.isCancelled())
            downloadAll.cancel(true);
        if (fragment != null)
            fragment.cancelActiveOperations();
    }

    @Override
    public void onBackPressed() {
        if (downloadAll != null && !downloadAll.isCancelled()) {
            Utils.showConfirm(this, "تأكيد", "إيقاف التحميل الجاري؟", (dialog, which) -> {
                //downloadAll will be cancelled by onStop()
                ReciterListActivity.super.onBackPressed();
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
        if (item.getItemId() == R.id.chooseDownloadDir) {
            chooseDir(false);
        } else if (item.getItemId() == R.id.unlimitedDownload) {
            startActivity(new Intent(this, ExternalRecitesDownloadActivity.class));
        } else if (fragment != null) { //make sure user has selected a reciter
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
                    if (Utils.getSurahDir(this, myReciter, 1) == null) {
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
                                    msg = result == Utils.DOWNLOAD_QUOTA_EXCEEDED ?
                                            "تم بلوغ الكمية القصوى للتحميل لهذا اليوم. نرجوا المحاولة غدا"
                                            : "فشل التحميل. تأكد من اتصالك بالشبكة ووجود مساحة كافية بجهازك";
                            }
                            if (!integers.isEmpty())
                                AnalyticsTrackers
                                        .sendDownloadRecites(ReciterListActivity.this,
                                                myReciter, integers);
                            fragment.setCurrentDownloadSurah(ReciterDetailFragment.CURRENT_SURAH_NONE);
                            if (msg != null)
                                Utils.showAlert(ReciterListActivity.this, "تحميل جميع التلاوات", msg, null);
                        }
                    }, QuranData.getInstance(this));
                    return true;
                case R.id.deleteAll:
                    if (downloadAll != null) return true;
                    itemChanged = false;
                    fragment.cancelActiveOperations();
                    fragment.setCanDoSingleOperation(false);
                    Utils.deleteAll(this, myReciter,
                            progress -> {
                                if (!itemChanged)
                                    fragment.setSurahProgress(progress, 0, false);
                            }, () -> {
                                fragment.setCanDoSingleOperation(true);
                                Toast.makeText(ReciterListActivity.this,
                                        "تم حذف جميع التلاوات لهذا القارئ",
                                        Toast.LENGTH_LONG).show();
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
            if (fragment != null)
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
    public void onChoosePath(String dir, File dirFile) {
        setting.saveSoundsDirectory = dir;
        setting.save(this);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (forceDialogSelection) {
            Utils.showConfirm(this, "اختيار الحافظة",
                    "لا بد من اختيار حافظة للتحميل. اختيار الآن؟",
                    (dialog, which) -> chooseDir(true),
                    (dialog, which) -> {
                        NavUtils.navigateUpFromSameTask(ReciterListActivity.this);
                        finish();
                    });
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        onCancel(dialogInterface);
    }
}
