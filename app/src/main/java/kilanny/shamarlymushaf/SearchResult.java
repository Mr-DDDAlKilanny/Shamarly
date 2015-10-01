package kilanny.shamarlymushaf;

import android.graphics.RectF;

import java.util.ArrayList;

/**
 * Created by Yasser on 09/29/2015.
 */
class SearchResult {
    public int page, surah, ayah;
    public String text;
}

class Ayah {
    public int sura, ayah;
    public ArrayList<RectF> rects;
}

class Page {
    public ArrayList<Ayah> ayahs;
    public int page;
}