package kilanny.shamarlymushaf;

import android.graphics.RectF;

import java.util.ArrayList;

/**
 * Created by Yasser on 09/29/2015.
 */
public class SearchResult {
    public int page, surah, ayah;
    public String text;
    public String query;
    private final QuranData quranData;

    public SearchResult(QuranData quranData) {
        this.quranData = quranData;
    }

    @Override
    public String toString() {
        String str;
        if (text.length() < 100)
            str = text;
        else {
            int idx = text.indexOf(query);
            if (idx < 0) {
                idx = 99;
                while (idx < text.length() && !Character.isWhitespace(text.charAt(idx)))
                    ++idx;
                str = text.substring(0, idx) + "...";
            } else {
                int end = idx + query.length();
                boolean stopStart = false, stopEnd = false;
                while (!stopStart || !stopEnd) {
                    if (!stopStart) {
                        if (idx <= 0 ||
                                (Character.isWhitespace(text.charAt(idx)) && end - idx > 90))
                            stopStart = true;
                        else --idx;
                    }
                    if (!stopEnd) {
                        if (end >= text.length() - 1
                                || (Character.isWhitespace(text.charAt(end)) && end - idx > 90))
                            stopEnd = true;
                        else ++end;
                    }
                }
                if (end == text.length() - 1)
                    ++end;
                else end = Math.min(text.length(), end);
                idx = Math.max(0, idx);
                str = text.substring(idx, end);
                if (idx > 0)
                    str = "..." + str;
                if (end < text.length())
                    str += "...";
            }
        }
        return "سورة "
                + quranData.surahs[surah - 1].name
                + " " + ayah + ": {"
                + str + "}";
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