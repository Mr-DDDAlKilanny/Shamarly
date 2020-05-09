package kilanny.shamarlymushaf.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import kilanny.shamarlymushaf.fragments.gotofragments.GotoBookmarkFragment;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoHizbFragment;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoJuzFragment;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoKhatmahFragement;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoNumberFragment;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoSearchFragment;
import kilanny.shamarlymushaf.fragments.gotofragments.GotoSurahFragment;

/**
 * Created by Yasser on 01/11/2016.
 */
public class GotoAdapter extends FragmentPagerAdapter {

    public static final int LENGTH = 7;
    private final String[] TITLES = { "المعلمة", "السورة", "الرقم",
            "البحث", "الجزء", "الحزب", "الختمة" };

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
            case 6:
                return new GotoKhatmahFragement();
        }
        throw new IllegalArgumentException("position = " + position);
    }
}
