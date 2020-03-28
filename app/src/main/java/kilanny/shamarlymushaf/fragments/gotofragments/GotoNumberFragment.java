package kilanny.shamarlymushaf.fragments.gotofragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import kilanny.shamarlymushaf.adapters.FullScreenImageAdapter;
import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;

/**
 * Created by Yasser on 12/06/2016.
 */

public class GotoNumberFragment extends GotoFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fillFields();
        final View root = inflater.inflate(R.layout.fragment_goto_number, container, false);
        root.findViewById(R.id.buttonGotoPage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText txt = (EditText) root.findViewById(R.id.editTextPageNum);
                if (!txt.getText().toString().trim().isEmpty()) {
                    int num = -1;
                    try {
                        num = Integer.parseInt(txt.getText().toString());
                    } catch (Exception ignored) {
                    }
                    if (num > 0 && num <= FullScreenImageAdapter.MAX_PAGE) {
                        showMainActivity(num);
                    } else {
                        Utils.showAlert(getActivity(), "خطأ",
                                String.format("أدخل رقم صفحة صحيح في المدى (1-%d)",
                                        FullScreenImageAdapter.MAX_PAGE), null);
                    }
                }
            }
        });
        root.findViewById(R.id.buttonGotoSuraAyahNumber).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText sura = (EditText) root.findViewById(R.id.editTextSuraNum);
                EditText ayah = (EditText) root.findViewById(R.id.editTextAyahNum);
                String s = sura.getText().toString().trim();
                String a = ayah.getText().toString().trim();
                if (!s.isEmpty() && !a.isEmpty()) {
                    int ss = -1, aa = -1;
                    try {
                        ss = Integer.parseInt(s);
                        aa = Integer.parseInt(a);
                    } catch (Exception ignored) {
                    }
                    if (ss < 1 || ss > quranData.surahs.length)
                        Utils.showAlert(getActivity(), "خطأ", "رقم السورة غير صحيح", null);
                    else if (aa < 1 || aa > quranData.surahs[ss - 1].ayahCount)
                        Utils.showAlert(getActivity(), "خطأ", "رقم الآية غير صحيح", null);
                    else
                        showMainActivity(db.getPage(ss, aa), ss, aa);
                }
            }
        });
        return root;
    }
}
