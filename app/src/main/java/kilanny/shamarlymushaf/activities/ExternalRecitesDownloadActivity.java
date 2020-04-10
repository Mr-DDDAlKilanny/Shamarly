package kilanny.shamarlymushaf.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.ReciterDownloadAdapter;
import kilanny.shamarlymushaf.data.ReciteZipItem;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.fragments.WebViewFragment;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;
import kilanny.shamarlymushaf.views.AutoHideFabScrollListener;

public class ExternalRecitesDownloadActivity extends AppCompatActivity {

    private static final int IMPORT_ZIP_REQUEST = 12345;

    private ListView listViewReciters;
    private ProgressDialog mImportProgressDlg;
    private SerializableInFile<String> lastIncompleteImport;
    private String lastImportFileName, lastImportName;

    public static SerializableInFile<String> getLastIncompleteImportFile(Context context) {
        return new SerializableInFile<>(context, "__lastReciteImportFileName");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_recites_download);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("تحميل غير محدود للتلاوات");

        listViewReciters = findViewById(R.id.listViewReciters);
        listViewReciters.setAdapter(new ReciterDownloadAdapter(this,
                this::onDownloadClick, this::onImportClick));

        FloatingActionButton fab = findViewById(R.id.fabHelp);
        fab.setOnClickListener(view -> Utils.showAlert(this, "تحميل التلاوات",
                "من خلال هذه الشاشة، يمكن تحميل تلاوات قارئ بشكل غير محدود.\nيتم تحميل التلاوات بشكل ملف، وبعد تحميل الملف يجب الضغط على زر استيراد لإضافة التلاوات إلى التطبيق.\nالرجاء التأكد من توفر المساحة قبل التحميل، حيث تم توضيح المساحة المطلوبة لكل عنصر تحت اسم القارئ.",
                null));
        lastIncompleteImport = getLastIncompleteImportFile(this);
        listViewReciters.setOnScrollListener(new AutoHideFabScrollListener(listViewReciters, fab));

        if (savedInstanceState != null) {
            lastImportFileName = savedInstanceState.getString("lastImportFileName");
            lastImportName = savedInstanceState.getString("lastImportName");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("lastImportName", lastImportName);
        outState.putString("lastImportFileName", lastImportFileName);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean c = ReciterListActivity.checkStoragePermission(this, true,
                () -> ReciterListActivity.checkStoragePermission(this, false, null));
        if (c && lastIncompleteImport.getData() != null) {
            File zipFile = new File(lastIncompleteImport.getData());
            if (zipFile.exists()) {
                int idx = Utils.findReciteZipItemByFileName(this, zipFile.getName());
                if (idx >= 0) {
                    Utils.showConfirm(this,
                            "استيراد تلاوة",
                            "هناك عملية استيراد لم يتم إكمالها بنجاح. هل تود إكمالها الآن",
                            (dialogInterface, i) -> AppExecutors.getInstance().executeOnCachedExecutor(
                                    () -> doImport(zipFile, ReciteZipItem.getAll(this)[idx].name)),
                            null);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ReciterListActivity.WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length == 0 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "لقد رفضت منح التطبيق صلاحية الكتابة في الذاكرة لذلك لا يمكنك التحميل. فضلا امنح الصلاحية من: إعدادات الجهاز - التطبيقات - مصحف الشمرلي",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void warnIfUriSelected(Runnable ifNot) {
        Boolean saveSoundsUri = Utils.isSaveSoundsUri(this);
        if (saveSoundsUri != null && saveSoundsUri) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("استيراد التلاوات من الملف")
                    .setMessage("لقد قمت باختيار تحميل التلاوات في خارج ذاكرة التطبيق في حافظة أخرى (وعليه قد تستغرق عملية الاستيراد بضع ساعات)، إذا كنت تريد سرعة أكبر يرجى تغيير الحافظة إلى ذاكرة التطبيق من القائمة العلوية في الشاشة الخاصة بتحميل التلاوات")
                    .setPositiveButton("استمرار (بطيء)", (dialog, which) -> ifNot.run())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else ifNot.run();
    }

    private Void onImportClick(@NonNull ReciteZipItem reciteZipItem) {
        Boolean saveSoundsUri = Utils.isSaveSoundsUri(this);
        if (saveSoundsUri == null) {
            Utils.showAlert(this, "لا يمكن الاستيراد",
                    "حافظة التحميل لم يتم تحديدها أو أنك قمت بحذفها أو نقلها، يجب تحديدها أولا من الشاشة السابقة بالضغط على الثلاث نقاط في القائمة العلوية",
                    null);
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            warnIfUriSelected(() -> {
                new AlertDialog.Builder(this)
                        .setTitle("استيراد التلاوات من الملف")
                        .setMessage("فضلا اضغط موافق، ثم حدد الملف الذي يبدء اسمه بـ\n" + reciteZipItem.fileName + "\nالموجود في مجلد التحميلات")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/zip");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI);
                            lastImportName = reciteZipItem.name;
                            lastImportFileName = reciteZipItem.fileName;
                            startActivityForResult(intent, IMPORT_ZIP_REQUEST);
                        })
                        .show();
            });
            return null;
        }
        warnIfUriSelected(() -> {
            AlertDialog progDlg = Utils.showIndeterminateProgressDialog(this,
                    "يتم البحث عن الملفات المحملة في جهازك");
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File zipFile = null;
                if (downloadDir != null && downloadDir.exists()) {
                    File[] files = downloadDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().contains(reciteZipItem.fileName)) {
                                zipFile = file;
                                break;
                            }
                        }
                    }
                    if (zipFile == null) {
                        runOnUiThread(() -> {
                            progDlg.dismiss();
                            new AlertDialog.Builder(this)
                                    .setTitle("تحديد الملف")
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setMessage("لم يتم العثور على ملف التحميل\n" + reciteZipItem.fileName
                                            + "\nفي مجلد التحميلات. هل ترغب بتحديد الملف بنفسك؟")
                                    .setPositiveButton(android.R.string.yes, (dialogInterface, i) ->
                                            new ChooserDialog(this)
                                                    .withFilter(false, false, "zip")
                                                    .withStringResources("اختيار ملف التلاوة المحمل", "اختيار", "إلغاء")
                                                    .withChosenListener((path, pathFile) -> {
                                                        if (pathFile.getName().contains(reciteZipItem.fileName)) {
                                                            showProgressDlg(reciteZipItem.name);
                                                            AppExecutors.getInstance().executeOnCachedExecutor(
                                                                    () -> doImport(pathFile, reciteZipItem.name));
                                                        } else
                                                            Toast.makeText(this,
                                                                    "اسم الملف غير صحيح", Toast.LENGTH_LONG).show();
                                                    })
                                                    .build()
                                                    .show())
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setCancelable(false)
                                    .show();
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        progDlg.dismiss();
                        showProgressDlg(reciteZipItem.name);
                    });
                    doImport(zipFile, reciteZipItem.name);
                }
            });
        });
        return null;
    }

    private void doImport(File zipFile, String reciteName) {
        boolean ok;
        try {
            lastIncompleteImport.setData(zipFile.getAbsolutePath(), this);
            ok = Utils.extractReciteZipFile(this, zipFile, mImportProgressDlg::setProgress);
        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
            AnalyticsTrackers.getInstance(this).sendImportReciteFail(reciteName, false,
                    e.getMessage());
        }
        final boolean isOk = ok;
        runOnUiThread(() -> {
            mImportProgressDlg.dismiss();
            if (isOk) {
                AnalyticsTrackers.getInstance(this).sendImportRecite(reciteName, false);
                lastIncompleteImport.setData(null, this);
                Utils.showConfirm(this, "نجاح الاستيراد",
                        "تم استيراد " + reciteName + "\nلم يعد البرنامج بحاجة إلى الملف الذي قمت بتحميله فقد تم استيراده بالفعل. هل تريد حذفه الآن لتوفير المساحة؟",
                        (dialogInterface, i) -> zipFile.delete(),
                        null);
            } else
                Utils.showAlert(this, "لم يتم الاستيراد",
                        "لم يتم استيراد ملف التلاوات. ربما الملف تالف ولم يتم تحميله بشكل كامل أو المساحة المتوفرة في جهازك غير كافية.", null);
        });
    }

    private void showProgressDlg(String reciterName) {
        ProgressDialog show = new ProgressDialog(this);
        show.setTitle("يتم استيراد: " + reciterName);
        show.setMessage("الرجاء الانتظار فقد تستغرق عملية الاستيراد عدة دقائق");
        show.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        show.setIndeterminate(false);
        show.setCancelable(false);
        show.setMax(114);
        show.setProgress(0);
        show.show();
        mImportProgressDlg = show;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void doImport(Uri uri, String fileName, String reciteName) {
        showProgressDlg(reciteName);
        AppExecutors.getInstance().executeOnCachedExecutor(() -> {
            boolean ok;
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);
                ok = Utils.extractReciteZipFile(this, fileName, inputStream,
                        mImportProgressDlg::setProgress);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "لا يمكن العثور على الملف المطلوب",
                        Toast.LENGTH_LONG).show();
                AnalyticsTrackers.getInstance(this).sendImportReciteFail(reciteName, true,
                        e.getMessage());
                ok = false;
            } catch (Throwable e) {
                e.printStackTrace();
                AnalyticsTrackers.getInstance(this).sendImportReciteFail(reciteName, true,
                        e.getMessage());
                ok = false;
            } finally {
                if (inputStream != null) {
                    try { inputStream.close(); }
                    catch (Throwable ex) { ex.printStackTrace(); }
                }
            }
            final boolean isOk = ok;
            runOnUiThread(() -> {
                mImportProgressDlg.dismiss();
                if (isOk) {
                    AnalyticsTrackers.getInstance(this).sendImportRecite(reciteName, true);
                    lastIncompleteImport.setData(null, this);
                    Utils.showConfirm(this, "نجاح الاستيراد",
                            "تم استيراد " + reciteName + "\nلم يعد البرنامج بحاجة إلى الملف الذي قمت بتحميله فقد تم استيراده بالفعل. هل تريد حذفه الآن لتوفير المساحة؟",
                            (dialogInterface, i) -> {
                                boolean deleted = false;
                                try {
                                    deleted = DocumentsContract.deleteDocument(getContentResolver(), uri);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(this, deleted ? "تم حذف الملف غير الضروري" : "فشل حذف الملف غير الضروري",
                                        Toast.LENGTH_LONG).show();
                            }, null);
                } else
                    Utils.showAlert(this, "لم يتم الاستيراد",
                            "لم يتم استيراد ملف التلاوات. ربما الملف تالف ولم يتم تحميله بشكل كامل أو المساحة المتوفرة في جهازك غير كافية.", null);
            });
        });
    }

    private Void onDownloadClick(ReciteZipItem item) {
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_CONNECTED) {
            Snackbar.make(listViewReciters, "أنت غير متصل بالشبكة", Snackbar.LENGTH_LONG).show();
            return null;
        }
        //TODO: Download button not working on Android Q
        WebViewFragment.newInstance(item.name, item.url)
                .show(getSupportFragmentManager(), "fragment_edit_name");
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                requestCode == IMPORT_ZIP_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Cursor cursor = getContentResolver().query(uri,
                    null, null, null, null, null);
            boolean valid = false;
            String displayName = null;
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    displayName = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    valid = displayName.contains(lastImportFileName);
                    Log.d("ImportFile", "Display Name: " + displayName);
                }
                cursor.close();
            }
            if (!valid) {
                new AlertDialog.Builder(this)
                        .setTitle("استيراد التلاوات من الملف")
                        .setMessage("عذرا، الملف الذي قمت باختياره\n" + displayName
                                + "\nلا يطابق اسم الملف المطلوب:\n" + lastImportFileName)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            doImport(uri, displayName, lastImportName);
        }
    }
}
