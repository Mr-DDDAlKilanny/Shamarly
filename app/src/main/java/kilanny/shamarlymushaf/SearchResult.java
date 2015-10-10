package kilanny.shamarlymushaf;

import android.graphics.RectF;

import java.util.ArrayList;

/**
 * Created by Yasser on 09/29/2015.
 */
class SearchResult {
    public int page, surah, ayah;
    public String text;

    @Override
    public String toString() {
        if (text.length() < 100)
            return text;
        int idx = 99;
        while (idx < text.length() && !Character.isWhitespace(text.charAt(idx)))
            ++idx;
        return "سورة "
                + WelcomeActivity.surahs[surah - 1].name
                + " " + ayah + "\n"
                + text.substring(0, idx);
    }
}

class Ayah {
    public int sura, ayah;
    public ArrayList<RectF> rects;
}

class Page {
    public ArrayList<Ayah> ayahs;
    public int page;
}