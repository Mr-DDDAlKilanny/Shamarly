package kilanny.shamarlymushaf.fragments.gotofragments;

import androidx.fragment.app.Fragment;
import android.content.Context;
import android.content.Intent;

import java.util.Locale;

import kilanny.shamarlymushaf.data.DbManager;
import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.data.QuranData;
import kilanny.shamarlymushaf.data.Setting;

/**
 * Created by Yasser on 12/06/2016.
 */

public abstract class GotoFragment extends Fragment {

    protected QuranData quranData;
    protected DbManager db;
    protected Setting setting;

    protected void fillFields() {
        db = DbManager.getInstance(getContext());
        quranData = QuranData.getInstance(getContext());
        setting = Setting.getInstance(getContext());
    }

    protected Intent getShowMainActivityIntent(int page) {
        Context context = getActivity();
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                page / (setting.lastWasDualPage ? 2 : 1));
        return intent;
    }

    protected void showMainActivity(int page) {
        Context context = getActivity();
        context.startActivity(getShowMainActivityIntent(page));
    }

    protected void showMainActivity(int page, int sura, int ayah) {
        Context context = getActivity();
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                page / (setting.lastWasDualPage ? 2 : 1));
        intent.putExtra(MainActivity.SHOW_AYAH_MESSAGE, String.format(Locale.ENGLISH, "%d,%d",
                sura, ayah));
        context.startActivity(intent);
    }

}
