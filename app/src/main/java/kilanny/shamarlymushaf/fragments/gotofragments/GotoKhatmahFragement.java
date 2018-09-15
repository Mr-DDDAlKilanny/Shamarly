package kilanny.shamarlymushaf.fragments.gotofragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.data.Khatmah;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.util.ResultCallback;
import kilanny.shamarlymushaf.util.Utils;

public class GotoKhatmahFragement extends GotoFragment {

    private ArrayAdapter<Khatmah> adapter;
    private View mView;

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
                txt.setText("سورة " + quranData.findSurahAtPage(item.page).name
                        + "، ص" + item.page + " - آخر استخدام: " + Utils.getTimeAgoString(item.lastReadDate));
                return rowView;
            }
        };
        final ListView listView = (ListView) mView.findViewById(R.id.listViewKhatmat);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Khatmah itemValue = (Khatmah) listView.getItemAtPosition(position);
                Intent mainActivityIntent = getShowMainActivityIntent(itemValue.page);
                mainActivityIntent.putExtra(MainActivity.EXTRA_KHATMAH_NAME, itemValue.name);
                getActivity().startActivity(mainActivityIntent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Khatmah item = (Khatmah) parent.getItemAtPosition(position);
                final AlertDialog.Builder b = new AlertDialog.Builder(parent.getContext());
                b.setCancelable(true);
                b.setTitle(item.name);
                final String[] options = new String[] {
                        "تعديل",
                        "حذف"
                };
                b.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        switch (which) {
                            case 0:
                                inputString(options[0], "اسم الختمة", item.name, new ResultCallback<String>() {
                                    @Override
                                    public void onResult(String result) {
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
                                    }
                                });
                                break;
                            case 1: {
                                AlertDialog.Builder b1 = new AlertDialog.Builder(b.getContext());
                                b1.setTitle(options[1]);
                                b1.setMessage("متأكد أنك تريد حذف هذه الختمة؟");
                                b1.setCancelable(true);
                                b1.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setting.khatmat.remove(item);
                                        setting.save(b.getContext());
                                        adapter.remove(item);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                                b1.setNegativeButton(getString(android.R.string.cancel), null);
                                b1.show();
                            }
                            break;
                        }
                    }
                });
                b.show();
                return true;
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragement_goto_khatmah, container, false);
        initList();
        FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.fabAddKhatmah);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                inputString("إضافة ختمة", "اسم الختمة", "", new ResultCallback<String>() {
                    @Override
                    public void onResult(String result) {
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
                        Snackbar.make(view,
                                "تم إضافة الختمة",
                                Snackbar.LENGTH_LONG)
                                //.setAction("Action", null)
                                .show();
                    }
                });
            }
        });
        return mView;
    }


    private void inputString(String title, String hint, String initValue,
                             final ResultCallback<String> resultCallback) {
        AlertDialog.Builder b = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setCancelable(true);
        final EditText editText = new EditText(b.getContext());
        editText.setText(initValue);
        editText.setHint(hint);
        b.setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resultCallback.onResult(editText.getText().toString());
                        dialog.dismiss();
                    }
                });
        b.show();
    }
}
