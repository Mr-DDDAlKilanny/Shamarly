package kilanny.shamarlymushaf.fragments.gotofragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.data.Khatmah;
import kilanny.shamarlymushaf.util.ResultCallback;
import kilanny.shamarlymushaf.util.Utils;
import kilanny.shamarlymushaf.views.AutoHideFabScrollListener;

public class GotoKhatmahFragement extends GotoFragment {

    private ArrayAdapter<Khatmah> adapter;
    private View mView;
    private AlertDialog mInputStringDlg;

    @Override
    public void onStart() {
        super.onStart();
        if (mView != null) {
            initList(); // if user is back from MainActivity, refresh last position
        }
    }

    private void initList() {
        fillFields();
        adapter = new ArrayAdapter<Khatmah>(getContext(), android.R.layout.simple_list_item_2,
                setting.khatmat) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View rowView;
                if (convertView == null)
                    rowView = getLayoutInflater().inflate(
                            android.R.layout.simple_list_item_2, parent, false);
                else
                    rowView = convertView;
                Khatmah item = adapter.getItem(position);
                TextView txt = (TextView) rowView.findViewById(android.R.id.text1);
                txt.setText("ختمة: " + item.name);
                txt = rowView.findViewById(android.R.id.text2);
                if (item.page > 1) {
                    txt.setText("سورة " + quranData.findSurahAtPage(item.page).name
                            + "، ص" + item.page + " - آخر استخدام: " + Utils.getTimeAgoString(item.lastReadDate));
                }
                return rowView;
            }
        };
        final ListView listView = mView.findViewById(R.id.listViewKhatmat);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Khatmah itemValue = (Khatmah) listView.getItemAtPosition(position);
            Intent mainActivityIntent = getShowMainActivityIntent(itemValue.page);
            mainActivityIntent.putExtra(MainActivity.EXTRA_KHATMAH_NAME, itemValue.name);
            getActivity().startActivity(mainActivityIntent);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            final Khatmah item = (Khatmah) parent.getItemAtPosition(position);
            final AlertDialog.Builder b = new AlertDialog.Builder(parent.getContext());
            b.setCancelable(true);
            b.setTitle(item.name);
            final String[] options = new String[] {
                    "تعديل",
                    "حذف"
            };
            b.setItems(options, (dialog, which) -> {
                dialog.dismiss();
                switch (which) {
                    case 0:
                        inputString(options[0], "اسم الختمة", item.name, result -> {
                            if (result.trim().length() == 0) {
                                Toast.makeText(b.getContext(),
                                        "حقل الاسم لا يحتوي على قيمة",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (!result.trim().equals(item.name) &&
                                    setting.khatmahNameExists(result.trim())) {
                                Toast.makeText(getContext(),
                                        "الاسم موجود مسبقا",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            item.name = result;
                            setting.save(b.getContext());
                            adapter.notifyDataSetChanged();
                        });
                        break;
                    case 1: {
                        AlertDialog.Builder b1 = new AlertDialog.Builder(b.getContext());
                        b1.setTitle(options[1]);
                        b1.setMessage("متأكد أنك تريد حذف هذه الختمة؟");
                        b1.setCancelable(true);
                        b1.setPositiveButton(getString(android.R.string.yes), (dialog1, which1) -> {
                            setting.khatmat.remove(item);
                            setting.save(b.getContext());
                            adapter.remove(item);
                            adapter.notifyDataSetChanged();
                        });
                        b1.setNegativeButton(getString(android.R.string.cancel), null);
                        b1.show();
                    }
                    break;
                }
            });
            b.show();
            return true;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragement_goto_khatmah, container, false);
        initList();
        FloatingActionButton fab = mView.findViewById(R.id.fabAddKhatmah);
        fab.setOnClickListener(view -> inputString("إضافة ختمة", "اسم الختمة", "", result -> {
            if (result.trim().length() == 0) {
                Toast.makeText(getContext(),
                        "حقل الاسم لا يحتوي على قيمة",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (setting.khatmahNameExists(result.trim())) {
                Toast.makeText(getContext(),
                        "الاسم موجود مسبقا",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Khatmah khatmah = new Khatmah();
            khatmah.name = result;
            setting.khatmat.add(khatmah);
            setting.save(view.getContext());
            adapter.notifyDataSetChanged();
            Snackbar.make(view, "تم إضافة الختمة", Snackbar.LENGTH_LONG)
                    //.setAction("Action", null)
                    .show();
        }));

        ListView listView = mView.findViewById(R.id.listViewKhatmat);
        listView.setOnScrollListener(new AutoHideFabScrollListener(listView, fab));
        return mView;
    }

    private void inputString(String title, String hint, String initValue,
                             final ResultCallback<String> resultCallback) {
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(new ContextThemeWrapper(getContext(), R.style.AlertDialogTheme))
                .setTitle(title)
                .setView(R.layout.edit_text)
                .setCancelable(true);
        View view = View.inflate(getContext(), R.layout.edit_text, null);
        TextInputEditText textView = view.findViewById(android.R.id.text1);
        textView.setText(initValue);
        TextInputLayout text = view.findViewById(R.id.text_input_layout);
        text.setHint(hint);
        textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        textView.setImeActionLabel("إضافة", KeyEvent.KEYCODE_ENTER);
        textView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (v.getText() != null) {
                    resultCallback.onResult(v.getText().toString());
                    mInputStringDlg.dismiss();
                }
                return true;
            }
            return false;
        });
        b.setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    TextInputEditText input = ((AlertDialog) dialog).findViewById(android.R.id.text1);
                    if (input.getText() == null) return;
                    resultCallback.onResult(input.getText().toString());
                    dialog.dismiss();
                });
        mInputStringDlg = b.create();
        mInputStringDlg.show();
    }
}
