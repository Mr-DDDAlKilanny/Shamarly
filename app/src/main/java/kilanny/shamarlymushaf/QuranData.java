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
    public static final int[] AYAH_COUNT = {
            7,
            286,
            200,
            176,
            120,
            165,
            206,
            75,
            129,
            109,
            123,
            111,
            43,
            52,
            99,
            128,
            111,
            110,
            98,
            135,
            112,
            78,
            118,
            64,
            77,
            227,
            93,
            88,
            69,
            60,
            34,
            30,
            73,
            54,
            45,
            83,
            182,
            88,
            75,
            85,
            54,
            53,
            89,
            59,
            37,
            35,
            38,
            29,
            18,
            45,
            60,
            49,
            62,
            55,
            78,
            96,
            29,
            22,
            24,
            13,
            14,
            11,
            11,
            18,
            12,
            12,
            30,
            52,
            52,
            44,
            28,
            28,
            20,
            56,
            40,
            31,
            50,
            40,
            46,
            42,
            29,
            19,
            36,
            25,
            22,
            17,
            19,
            26,
            30,
            20,
            15,
            21,
            11,
            8,
            8,
            19,
            5,
            8,
            8,
            11,
            11,
            8,
            3,
            9,
            5,
            4,
            7,
            3,
            6,
            3,
            5,
            4,
            5,
            6
    };

    private static QuranData instance;

    public final ListItem[] juzs = new ListItem[31];
    public final ListItem[] hizbs = new ListItem[61];
    public final ListItem[] surahs2 = new ListItem[115];
    public final Surah[] surahs = new Surah[114];

    public static QuranData getInstance(Context context) {
        if (instance == null)
            instance = new QuranData(context);
        return instance;
    }

    private QuranData(Context context) {
        XmlResourceParser parser = context.getResources().getXml(R.xml.qurandata);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                String name;
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("surah")) {
                            Surah s = new Surah();
                            s.index = parser.getAttributeIntValue(null, "index", 0);
                            s.name = parser.getAttributeValue(null, "name").trim();
                            s.page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                            surahs[s.index - 1] = s;
                        } else if (name.equals("hizb")) {
                            ListItem h = new ListItem();
                            int idx = Integer.parseInt(parser.getAttributeValue(null, "index").trim());
                            int page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                            h.name = "الحزب " + ArabicNumbers.numToStr(idx);
                            h.value = page;
                            hizbs[idx] = h;
                            if (idx % 2 == 1) {
                                ListItem j = new ListItem();
                                j.name = "الجزء " + ArabicNumbers
                                        .numToStr(1 + idx / 2);
                                j.value = page;
                                juzs[1 + idx / 2] = j;
                            }
                        }
                        break;
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
}
