package kilanny.shamarlymushaf;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import kilanny.shamarlymushaf.gotofragments.GotoBookmarkFragment;
import kilanny.shamarlymushaf.gotofragments.GotoHizbFragment;
import kilanny.shamarlymushaf.gotofragments.GotoJuzFragment;
import kilanny.shamarlymushaf.gotofragments.GotoNumberFragment;
import kilanny.shamarlymushaf.gotofragments.GotoSearchFragment;
import kilanny.shamarlymushaf.gotofragments.GotoSurahFragment;

/**
 * Created by Yasser on 01/11/2016.
 */
public class GotoAdapter extends FragmentPagerAdapter {

    public static final int LENGTH = 6;
    private final String[] TITLES = { "المعلمة", "السورة", "الرقم",
            "البحث", "الجزء", "الحزب" };

    public GotoAdapter(FragmentManager fm) {
        super(fm);
        for (int i = 0; i < TITLES.length / 2; ++i) {
            String tmp = TITLES[i];
            TITLES[i] = TITLES[TITLES.length - i - 1];
            TITLES[TITLES.length - i - 1] = tmp;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TITLES[position];
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

    @Override
    public Fragment getItem(int position) {
        position = LENGTH - position - 1;
        switch (position) {
            case 0:
                return new GotoBookmarkFragment();
            case 1:
                return new GotoSurahFragment();
            case 2:
                return new GotoNumberFragment();
            case 3:
                return new GotoSearchFragment();
            case 4:
                return new GotoJuzFragment();
            case 5:
                return new GotoHizbFragment();
        }
        return null;
    }
}
