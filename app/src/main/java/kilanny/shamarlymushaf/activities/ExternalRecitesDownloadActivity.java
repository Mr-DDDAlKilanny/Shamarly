package kilanny.shamarlymushaf.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.obsez.android.lib.filechooser.ChooserDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.ReciterDownloadAdapter;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.ReciteZipItem;
import kilanny.shamarlymushaf.data.SerializableInFile;
import kilanny.shamarlymushaf.fragments.WebViewFragment;
import kilanny.shamarlymushaf.util.Utils;

public class ExternalRecitesDownloadActivity extends AppCompatActivity {

    private ListView listViewReciters;
    private AlertDialog mImportProgressDlg;
    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private SerializableInFile<String> lastIncompleteImport;

    public static SerializableInFile<String> getLastIncompleteImportFile(Context context) {
        return new SerializableInFile<>(context, "__lastReciteImportFileName");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_external_recites_download);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("تحميل غير محدود للتلاوات");

        listViewReciters = findViewById(R.id.listViewReciters);
        listViewReciters.setAdapter(new ReciterDownloadAdapter(this,
                this::onDownloadClick, this::onImportClick));

        FloatingActionButton fab = findViewById(R.id.fabHelp);
        fab.setOnClickListener(view -> Utils.showAlert(this, "تحميل التلاوات",
                "من خلال هذه الشاشة، يمكن تحميل تلاوات قارئ بشكل غير محدود.\nالرجاء التأكد من توفر المساحة قبل التحميل، حيث تم توضيح المساحة المطلوبة لكل عنصر تحت اسم القارئ.",
                null));
        lastIncompleteImport = getLastIncompleteImportFile(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean c = ReciterListActivity.checkStoragePermission(this, true,
                () -> ReciterListActivity.checkStoragePermission(this, false, null));
        if (c && lastIncompleteImport.getData() != null) {
            File zipFile = new File(lastIncompleteImport.getData());
            if (zipFile.exists()) {
                int idx = Utils.findReciteZipItemByFileName(this, zipFile);
                if (idx >= 0) {
                    Utils.showConfirm(this,
                            "استيراد تلاوة",
                            "هناك عملية استيراد لم يتم إكمالها بنجاح. هل تود إكمالها الآن",
                            (dialogInterface, i) -> mExecutor.execute(
                                    () -> doImport(zipFile, ReciteZipItem.getAll(this)[idx])),
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

    private Void onImportClick(@NonNull ReciteZipItem reciteZipItem) {
        AlertDialog progDlg = Utils.showIndeterminateProgressDialog(this,
                "يتم البحث عن الملفات المحملة في جهازك");
        mExecutor.execute(() -> {
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
                                                    mImportProgressDlg = Utils.showIndeterminateProgressDialog(
                                                            this, "يتم استيراد: " + reciteZipItem.name);
                                                    mExecutor.execute(() -> doImport(pathFile, reciteZipItem));
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
                mImportProgressDlg = Utils.showIndeterminateProgressDialog(
                        this, "يتم استيراد: " + reciteZipItem.name);
            });
            doImport(zipFile, reciteZipItem);
        });
        return null;
    }

    private void doImport(File zipFile, ReciteZipItem reciteZipItem) {
        boolean ok;
        try {
            lastIncompleteImport.setData(zipFile.getAbsolutePath(), this);
            ok = Utils.extractReciteZipFile(this, zipFile);
        } catch (Exception e) {
            e.printStackTrace();
            ok = false;
        }
        final boolean isOk = ok;
        runOnUiThread(() -> {
            mImportProgressDlg.dismiss();
            if (isOk) {
                lastIncompleteImport.setData(null, this);
                Utils.showConfirm(this, "نجاح الاستيراد",
                        "تم استيراد " + reciteZipItem.name + "\nلم يعد البرنامج بحاجة إلى الملف الذي قمت بتحميله فقد تم استيراده بالفعل. هل تريد حذفه الآن لتوفير المساحة؟",
                        (dialogInterface, i) -> zipFile.delete(),
                        null);
            } else
                Utils.showAlert(this, "لم يتم الاستيراد",
                        "لم يتم استيراد ملف التلاوات. ربما الملف تالف ولم يتم تحميله بشكل كامل أو المساحة المتوفرة في جهازك غير كافية.", null);
        });
    }

    private Void onDownloadClick(ReciteZipItem item) {
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_CONNECTED) {
            Snackbar.make(listViewReciters, "أنت غير متصل بالشبكة", Snackbar.LENGTH_LONG).show();
            return null;
        }
        WebViewFragment.newInstance(item.name, item.url)
                .show(getSupportFragmentManager(), "fragment_edit_name");
        return null;
    }
}
