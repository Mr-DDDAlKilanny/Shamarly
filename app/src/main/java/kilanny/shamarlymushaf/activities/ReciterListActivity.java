package kilanny.shamarlymushaf.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.DownloadedAyat;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.fragments.ReciterDetailFragment;
import kilanny.shamarlymushaf.fragments.ReciterListFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
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
    private static final int SELECT_DIR_REQUEST_CODE = 54321;

    private ReciterDetailFragment fragment;
    private AsyncTask downloadAll;
    private ArrayList<String> allReciters;
    private int updateAllIndex;
    private ProgressDialog updateAllProgDlg;
    private boolean itemChanged = false;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private Setting setting;

    private boolean forceDialogSelection;

    private void chooseDir(boolean force, boolean forceSaf) {
        forceDialogSelection = force;
        Boolean saveSoundsUri = Utils.isSaveSoundsUri(this);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("تحميل التلاوات")
                .setMessage("فضلا اضغط موافق، ثم اختر الحافظة التي سيتم التحميل فيها")
                .setCancelable(false)
                .setPositiveButton("موافق", (dialog, which) -> {
                    if ((!forceSaf && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                            || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        new ChooserDialog(ReciterListActivity.this)
                                .withFilter(true, false)
                                .withStringResources("اختيار حافظة التحميل", "اختيار", "إلغاء")
                                .withStartFile(force || saveSoundsUri == null || saveSoundsUri ?
                                        null : setting.saveSoundsDirectory)
                                .withOnCancelListener(ReciterListActivity.this)
                                .withChosenListener(ReciterListActivity.this)
                                .withNegativeButtonListener(ReciterListActivity.this)
                                .build()
                                .show();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                //| DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
                        startActivityForResult(intent, SELECT_DIR_REQUEST_CODE);
                    }
                })
                .setNegativeButton("ليس الآن", (dialog, which) -> {
                    if (force) {
                        Utils.showConfirm(this, "حافظة التحميل", "لا بد من اختيار حافظة للتحميل. اختيار الآن؟",
                                "اختيار", "خروج",
                                (dialog1, which1) -> chooseDir(force, forceSaf), (dialog1, which1) -> finish());
                    }
                })
                .setNeutralButton("مساعدة",
                        (dialog, which) -> startActivity(new Intent(this, ReportIssueActivity.class)))
                .show();
    }

    private void updateSlowWarning() {
        Boolean saveSoundsUri = Utils.isSaveSoundsUri(this);
        findViewById(R.id.txtSlowWarning).setVisibility(saveSoundsUri != null && saveSoundsUri ?
                View.VISIBLE : View.GONE);
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        setting = Setting.getInstance(this);
        initWithPermissionCheck(true);
    }

    private void handleInvalidDirSelected() {
        Toast.makeText(this, "عفوا، لم تقم باختيار حافظة صالحة للتحميل",
                Toast.LENGTH_LONG).show();
        chooseDir(Utils.isSaveSoundsUri(this) == null, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                requestCode == SELECT_DIR_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    AlertDialog progressDialog = Utils.showIndeterminateProgressDialog(this,
                            "يتم اختبار الحافظة التي اخترتها...");
                    AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                        boolean valid = false;
                        DocumentFile tree = DocumentFile.fromTreeUri(this, uri);
                        if (tree != null) {
                            DocumentFile recites = tree.findFile("recites");
                            DocumentFile[] documentFiles = null;
                            if (recites == null)
                                recites = tree.createDirectory("recites");
                            else
                                documentFiles = recites.listFiles();
                            if (recites != null) {
                                valid = true;
                                getContentResolver().takePersistableUriPermission(uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                setting.saveSoundsDirectory = uri.toString();
                                setting.save(this);
                                AnalyticsTrackers.getInstance(this).sendChooceDir(true);

                                DownloadedAyat.getInstance(this).reset(this);
                                DocumentFile[] children = documentFiles;
                                runOnUiThread(() -> {
                                    updateSlowWarning();
                                    progressDialog.dismiss();
                                    if (children != null && children.length > 0)
                                        refreshAllDownloads(children);
                                });
                            }
                        }
                        if (!valid)
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                handleInvalidDirSelected();
                            });
                    });
                }
            } else
                handleInvalidDirSelected();
        }
    }

    private void refreshAllDownloads(DocumentFile[] dirs) {
        String[] allReciters = getResources().getStringArray(R.array.reciter_values);
        this.allReciters = new ArrayList<>();
        for (DocumentFile documentFile : dirs) {
            for (String recite : allReciters) {
                if (documentFile.getName() != null && documentFile.getName().equals(recite)
                        && documentFile.isDirectory()) {
                    this.allReciters.add(recite);
                    break;
                }
            }
        }
        if (this.allReciters.isEmpty()) return;
        Utils.showConfirm(this, "تحديث الملفات",
                "الحافظة التي اخترتها قد يكون فيها تلاوات، هل تريد تحديث الملفات التي تم تحميلها؟ ينصح بشدة بتحديث الملفات الآن، لكن قد تستغرق هذه العملية وقتا"
                , (dialog, which) -> {
                    updateAllProgDlg = new ProgressDialog(this);
                    updateAllProgDlg.setTitle("تحديث الملفات");
                    updateAllProgDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    updateAllProgDlg.setIndeterminate(false);
                    updateAllProgDlg.setCancelable(false);
                    updateAllProgDlg.setMax(114);
                    updateAllProgDlg.setProgress(0);
                    updateAllProgDlg.show();
                    updateAllIndex = 0;
                    _refreshAllDownloads();
                }, (dialog, which) -> {});
    }

    private void _refreshAllDownloads() {
        updateAllProgDlg.setTitle("تحديث الملفات: " + (updateAllIndex + 1) + " من " + allReciters.size());
        Utils.refreshNumDownloaded(this, allReciters.get(updateAllIndex), updateAllProgDlg::setProgress, () -> {
            if (++updateAllIndex == allReciters.size()) {
                updateAllProgDlg.dismiss();
            } else
                _refreshAllDownloads();
        });
    }

    private void initWithPermissionCheck(boolean shouldShowExplainDlg) {
        //https://developer.android.com/about/versions/11/privacy/storage#app-specific-external
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                checkStoragePermission(this, shouldShowExplainDlg,
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

    private void initAndroidQ(Runnable ifNot) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("تحميل التلاوات")
                    .setMessage("يمكن تحميل التلاوات إما في ذاكرة التطبيق (أسرع وموصى به) أو في خارج ذاكرة التطبيق في كارت الذاكرة مثلا (تحذير: يحعل التحميل بطيء جدا ولذا فهو غير موصى به)")
                    .setPositiveButton("تحميل في ذاكرة التطبيق (سريع وموصى به)", (dialog, which) -> {
                        setting.saveSoundsDirectory = getExternalFilesDir(null).getAbsolutePath();
                        setting.save(this);
                        AnalyticsTrackers.getInstance(this).sendChooceDir(false);
                        updateSlowWarning();
                    })
                    .setNegativeButton("تحميل في ذاكرة أخرى (بطيء)", (dialog, which) -> ifNot.run())
                    .setNeutralButton("مساعدة",
                            (dialog, which) -> startActivity(new Intent(this, ReportIssueActivity.class)))
                    .show();
        } else ifNot.run();
    }

    private boolean testPreAndroidQAccess(boolean force) {
        Boolean saveSoundsUri = Utils.isSaveSoundsUri(this);
        if (saveSoundsUri != null && !saveSoundsUri) {
            try {
                File file = new File(setting.saveSoundsDirectory, "tmp.tmp");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write("hello".getBytes());
                fileOutputStream.close();
                file.delete();
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("تحميل التلاوات")
                        .setMessage("عفوا، الحافظة التي اخترتها لا يمكن الوصول إليها، يمكن تحميل التلاوات إما في ذاكرة التطبيق (أسرع وموصى به) أو في خارج ذاكرة التطبيق في كارت الذاكرة مثلا (تحذير: يحعل التحميل بطيء جدا ولذا فهو غير موصى به)")
                        .setPositiveButton("تحميل في ذاكرة التطبيق (سريع وموصى به)", (dialog, which) -> {
                            setting.saveSoundsDirectory = getExternalFilesDir(null).getAbsolutePath();
                            setting.save(this);
                            AnalyticsTrackers.getInstance(this).sendChooceDir(false);
                            updateSlowWarning();
                        })
                        .setNegativeButton("تحميل في ذاكرة أخرى (بطيء)",
                                (dialog, which) -> chooseDir(force, true))
                        .show();
                return false;
            } catch (IOException e) {
                e.printStackTrace(); //maybe no space?
            }
        }
        return true;
    }

    private void init() {
        if (Utils.isSaveSoundsUri(this) == null) {
            initAndroidQ(() -> chooseDir(true, false));
        } else if (testPreAndroidQAccess(true)) {
            updateSlowWarning();
            SerializableInFile<Integer> appResponse = new SerializableInFile<>(
                    getApplicationContext(), "down__st", 0);
            if (appResponse.getData() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle("تحميل مصحف القارئ كاملا")
                        .setMessage("تم فتح إمكانية تحميل القارئ بالكامل بدون كمية يومية!")
                        .setPositiveButton("أرني كيف", (dialogInterface, i) -> {
                            Toast.makeText(this,
                                    "اضغط على علامة القائمة (النقاط الثلاث الرأسية) أو الزر أعلاه لفتح هذه الخاصية",
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
            menu.getItem(4).setVisible(false);
        }
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
            initAndroidQ(() -> chooseDir(false, false));
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
                                        "تم بلوغ الكمية القصوى للتحميل لهذا اليوم. نرجوا المحاولة غدا أو استخدام التحميل اللامحدود"
                                        : "فشل التحميل. تأكد من اتصالك بالشبكة ووجود مساحة كافية بجهازك";
                        }
                        fragment.setCurrentDownloadSurah(ReciterDetailFragment.CURRENT_SURAH_NONE);
                        if (msg != null)
                            Utils.showAlert(ReciterListActivity.this, "تحميل جميع التلاوات", msg, null);
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
                case R.id.refreshRecites:
                    if (downloadAll != null) return true;
                    Utils.refreshNumDownloaded(this, fragment.mItem, fragment::reload);
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
        if (!testPreAndroidQAccess(forceDialogSelection))
            updateSlowWarning();
        AnalyticsTrackers.getInstance(this).sendChooceDir(false);
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        if (forceDialogSelection) {
            Utils.showConfirm(this, "اختيار الحافظة",
                    "لا بد من اختيار حافظة للتحميل. اختيار الآن؟",
                    (dialog, which) -> chooseDir(true, false),
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
