package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.ArabicNumbers;

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
    public static final long[] PAGE_FILE_SIZE = {59676, 107437, 111190, 78855, 78045, 79375, 78734, 79419,
            82427, 82376, 88379, 82771, 83279, 84298, 84792, 85271, 84555, 84252, 85333, 85074,
            86951, 86342, 84923, 91859, 86708, 91012, 91313, 87972, 87097, 91831, 93837, 89380,
            89555, 88573, 87239, 91239, 88573, 87784, 86176, 89463, 91078, 98387, 87918, 88995,
            90127, 91581, 91029, 86764, 87296, 79329, 87007, 81920, 88507, 86230, 86384, 88449,
            87178, 87514, 88960, 90275, 94278, 95498, 89840, 104853, 91548, 91470, 91261, 92796,
            93830, 94332, 91073, 88935, 89223, 89230, 91387, 91351, 95411, 91049, 93359, 91854,
            91956, 94520, 96495, 93367, 95423, 95203, 103545, 92776, 90929, 91605, 89828, 89783,
            91169, 90870, 90757, 91959, 90331, 94354, 94686, 92712, 95343, 92344, 93547, 89763,
            101136, 94377, 94135, 94981, 94223, 92997, 94059, 96815, 94811, 96065, 94122, 88336,
            93771, 90096, 92696, 91808, 92835, 90605, 100646, 92677, 91311, 96832, 95346, 91708,
            93893, 93259, 94403, 95194, 95319, 91124, 93092, 92800, 91195, 90967, 92784, 92792,
            97213, 91000, 91172, 107815, 88680, 90063, 93017, 91414, 91590, 91482, 90733, 89321,
            112411, 93466, 93101, 94864, 98344, 89687, 90807, 88577, 91130, 92790, 91520, 91382,
            92405, 91438, 92441, 86969, 98243, 89344, 91343, 90504, 93829, 93089, 95666, 91419,
            92791, 89822, 89483, 87879, 100696, 89776, 89208, 90956, 90054, 95299, 85390, 87115,
            90925, 91474, 93783, 91383, 97288, 85659, 87450, 88165, 89260, 85494, 90510, 91000,
            89905, 94256, 91956, 110112, 88965, 91317, 94786, 95815, 91539, 102422, 90767, 90410,
            94759, 96415, 92507, 101153, 85812, 87532, 92783, 90508, 103111, 90477, 92079, 91337,
            90917, 88326, 92165, 90904, 93495, 95004, 90750, 93237, 105878, 89217, 92485, 93515,
            93023, 94238, 92172, 96880, 100498, 95844, 108105, 90535, 97131, 91615, 88598, 93116,
            89833, 88383, 90821, 89279, 103909, 87164, 90070, 86732, 90756, 93182, 95654, 107670,
            91359, 92957, 95419, 93364, 93608, 94514, 96634, 107178, 98256, 98307, 92161, 94658,
            93761, 95156, 94325, 111206, 94834, 101622, 96240, 98987, 96772, 103764, 104863, 109080,
            97405, 100233, 96680, 97544, 96874, 101126, 106572, 102386, 102705, 102563, 100537,
            102129, 99780, 98180, 99270, 109205, 95991, 98833, 96599, 96555, 98006, 110161, 98033,
            97936, 94084, 93774, 97874, 97318, 100674, 96835, 107509, 97384, 99366, 101756, 98736,
            104739, 104909, 97500, 108864, 99470, 97586, 93897, 96798, 97273, 96896, 103085, 102340,
            110349, 98365, 97293, 96522, 103021, 100011, 107228, 98128, 92736, 99685, 93638, 112477,
            93545, 96349, 99974, 110465, 93563, 112211, 94159, 100004, 95986, 94463, 94498, 96565,
            100227, 99258, 106287, 98255, 97733, 99874, 97227, 97368, 110849, 97481, 101390, 100855,
            105175, 99705, 89637, 87558, 87287, 87724, 102904, 92990, 90896, 90500, 93910, 92882,
            104706, 93720, 93726, 87295, 89075, 107261, 93463, 93251, 92298, 92192, 90629, 89041,
            101299, 88176, 90283, 91188, 95293, 95267, 94535, 88406, 103861, 95011, 91057, 91141,
            92750, 100685, 92948, 93545, 96102, 87315, 90915, 103531, 92994, 92622, 90898, 90951,
            92422, 104838, 93603, 99483, 92134, 97804, 109572, 90155, 90098, 93539, 93504, 106861,
            90154, 94421, 109501, 90603, 95311, 93793, 103070, 94258, 93951, 102950, 95268, 108050,
            97755, 96074, 107308, 96956, 105263, 96836, 95821, 106818, 96691, 107404, 98271, 101544,
            107504, 95469, 93473, 109595, 101282, 98332, 99626, 109519, 98602, 100324, 108533, 98244,
            95453, 110577, 97715, 95260, 112797, 106941, 112692, 93082, 106360, 95276, 104025, 91873,
            109089, 93955, 108284, 94486, 104967, 92350, 105826, 95261, 108724, 96027, 108553, 96186,
            109380, 91187, 109504, 108504, 97201, 105760, 105446, 90220, 102377, 92002, 109465, 114335,
            92372, 103905, 107865, 104216, 106277, 108815, 102105, 106851, 106066, 105932, 107894,
            104445, 121101, 121957, 114897, 120394, 107949, 113633, 109568, 127054, 130299, 108819, 124726};

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

