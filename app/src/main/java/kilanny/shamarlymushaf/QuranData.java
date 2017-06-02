package kilanny.shamarlymushaf;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by Yasser on 10/26/2015.
 */
public class QuranData {
    public static final int NORMAL_PAGE_WIDTH = 886;
    public static final int NORMAL_PAGE_HEIGHT = 1377;
    public static final int BORDERED_PAGE_WIDTH = 1190;
    public static final int BORDERED_PAGE_HEIGHT = 1684;

    private static QuranData instance;

    public final ListItem[] juzs = new ListItem[31];
    public final ListItem[] hizbs = new ListItem[61];
    public final ListItem[] arba3 = new ListItem[241];
    public final ListItem[] surahs2 = new ListItem[115];
    public final Surah[] surahs = new Surah[114];
    public final SajdahSakt[] sajdat = new SajdahSakt[15];
    public final SajdahSakt[] saktat = new SajdahSakt[4];
    public final String[] reciterNames;
    public final String[] reciterValues;
    public final String[] reciterValues_alt;

    public static QuranData getInstance(Context context) {
        if (instance == null)
            instance = new QuranData(context);
        return instance;
    }

    private QuranData(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.qurandata);
        reciterNames = context.getResources().getStringArray(R.array.reciter_names);
        reciterValues = context.getResources().getStringArray(R.array.reciter_values);
        reciterValues_alt = context.getResources().getStringArray(R.array.reciter_values_alt);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("surah")) {
                        Surah s = new Surah();
                        s.index = parser.getAttributeIntValue(null, "index", 0);
                        s.name = parser.getAttributeValue(null, "name").trim();
                        s.page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                        s.ayahCount = Integer.parseInt(parser.getAttributeValue(null, "ayah_count").trim());
                        surahs[s.index - 1] = s;
                    } else if (name.equals("sajdah")) {
                        SajdahSakt s = new SajdahSakt();
                        s.index = parser.getAttributeIntValue(null, "index", 0);
                        s.page = parser.getAttributeIntValue(null, "page", 0);
                        s.surah = parser.getAttributeIntValue(null, "surah", 0);
                        s.ayah = parser.getAttributeIntValue(null, "ayah", 0);
                        s.afterWord = parser.getAttributeValue(null, "after_word");
                        s.khelaf = parser.getAttributeValue(null, "khelaf");
                        s.surahName = surahs[s.surah - 1].name;
                        s.isSajdah = true;
                        sajdat[s.index - 1] = s;
                    } else if (name.equals("sakt")) {
                        SajdahSakt s = new SajdahSakt();
                        s.index = parser.getAttributeIntValue(null, "index", 0);
                        s.page = parser.getAttributeIntValue(null, "page", 0);
                        s.surah = parser.getAttributeIntValue(null, "surah", 0);
                        s.ayah = parser.getAttributeIntValue(null, "ayah", 0);
                        s.afterWord = parser.getAttributeValue(null, "after_word");
                        s.surahName = surahs[s.surah - 1].name;
                        s.isSajdah = false;
                        saktat[s.index - 1] = s;
                    } else if (name.equals("rob3")) {
                        ListItem r = new ListItem();
                        int idx = Integer.parseInt(parser.getAttributeValue(null, "index").trim());
                        int page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                        switch ((idx - 1) % 4) {
                            case 1:
                                r.name = "ربع الحزب";
                                break;
                            case 2:
                                r.name = "نصف الحزب";
                                break;
                            case 3:
                                r.name = "ثلاثة أرباع الحزب";
                                break;
                            case 0: {
                                ListItem h = new ListItem();
                                int myIdx = (idx - 1) / 4 + 1;
                                h.name = "الحزب " + ArabicNumbers.numToStr(myIdx);
                                h.value = page;
                                hizbs[myIdx] = h;
                                if ((idx - 1) % 8 == 0) {
                                    ListItem j = new ListItem();
                                    myIdx = (idx - 1) / 8 + 1;
                                    j.name = "الجزء " + ArabicNumbers.numToStr(myIdx);
                                    j.value = page;
                                    juzs[myIdx] = j;
                                }
                            }
                            r.name = "الحزب";
                            break;
                        }
                        r.value = page;
                        arba3[idx] = r;
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        ListItem juz = new ListItem(), hizb = new ListItem(), surah = new ListItem();
        juz.name = "اختر الجزء";
        hizb.name = "اختر الحزب";
        surah.name = "اختر السورة";
        juzs[0] = juz;
        hizbs[0] = hizb;
        surahs2[0] = surah;
        for (int i = 1; i < 114 + 1; ++i) {
            ListItem item = new ListItem();
            item.value = i;
            item.name = surahs[i - 1].name;
            surahs2[i] = item;
        }
    }

    public Surah findSurahAtPage(int page) {
        if (page <= 1)
            throw new IllegalArgumentException();
        for (int i = 0; i < surahs.length; ++i) {
            if (surahs[i].page == page)
                return surahs[i];
            else if (surahs[i].page > page)
                return surahs[i - 1];
        }
        if (page > surahs[surahs.length - 1].page)
            return surahs[surahs.length - 1];
        return null;
    }

    public ListItem findJuzAtPage(int page) {
        if (page <= 1)
            throw new IllegalArgumentException();
        for (int i = 1; i < juzs.length; ++i) {
            int val = (int) juzs[i].value;
            if (page == val) {
                return juzs[i];
            } else if (page < val) {
                return juzs[i - 1];
            }
        }
        if ((int) juzs[juzs.length - 1].value < page)
            return juzs[juzs.length - 1];
        return null;
    }
}

class SajdahSakt {
    int index, page, surah, ayah;
    String surahName, afterWord, khelaf;
    boolean isSajdah;

    @Override
    public String toString() {
        if (isSajdah)
            return "سجدة في سورة " + surahName + " الآية " + ayah
                    + " بعد {" + afterWord + "}" + (khelaf == null ? "" : " (" + khelaf + ")");
        else
            return "سكتة لطيفة في سورة " + surahName + " الآية " + ayah
                    + " بعد {" + afterWord + "}";
    }
}