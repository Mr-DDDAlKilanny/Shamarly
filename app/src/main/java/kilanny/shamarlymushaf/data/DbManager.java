package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.RectF;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import kilanny.shamarlymushaf.util.Utils;

/**
 * Created by Yasser on 09/29/2015.
 */
public class DbManager extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "shamerly.db";
    private static final short[] SELECTED_TAFSEER_AYAT = {
            20,28,32,39,50,51,52,117,119,122,123,124,131,158,159,160,170,171,172,181,187,190,195,
            202,204,208,218,221,222,225,233,235,236,239,240,241,245,251,252,261,262,264,268,270,
            271,272,274,275,278,282,283,284,285,287,288,293,295,298,301,307,309,310,319,320,321,
            325,333,352,360,378,385,395,396,397,403,411,422,423,424,425,426,427,428,450,452,455,
            462,473,478,491,493,495,503,506,507,510,511,520,522,524,527,529,530,533,541,552,562,
            567,570,572,586,588,599,601,603,604,608,609,615,617,618,627,635,636,638,661,667,670,
            672,675,676,677,678,679,681,686,702,707,716,720,723,724,732,733,735,741,742,748,767,
            802,803,804,806,816,821,825,832,842,848,849,850,852,859,861,862,884,885,886,887,892,
            905,909,911,944,949,964,981,984,997,998,1008,1009,1010,1050,1051,1052,1053,1054,1132,
            1139,1158,1159,1162,1178,1180,1184,1185,1187,1188,1189,1193,1196,1205,1206,1207,1224,
            1249,1251,1253,1254,1255,1259,1268,1273,1308,1313,1339,1346,1347,1351,1354,1361,1363,
            1390,1391,1408,1431,1469,1471,1472,1476,1479,1480,1525,1529,1558,1563,1585,1586,1587,
            1588,1589,1596,1704,1709,1710,1715,1716,1725,1733,1757,1769,1784,1789,1790,1791,1792,
            1797,1798,1805,1818,1821,1823,1849,1915,1941,1946,1950,1954,1962,1978,1979,1980,1981,
            1982,2008,2013,2026,2027,2029,2038,2044,2047,2055,2056,2058,2061,2063,2066,2082,2107,
            2167,2168,2170,2186,2249,2250,2285,2346,2417,2422,2430,2460,2479,2480,2506,2518,2577,
            2588,2590,2596,2606,2636,2672,2673,2788,2791,2793,2802,2812,2813,2814,2823,2824,2826,
            2828,2830,2842,2846,2847,2856,2887,2898,2900,2902,2903,2913,2916,2917,2918,2922,2923,
            2932,3178,3219,3220,3221,3222,3223,3224,3232,3234,3268,3340,3346,3348,3349,3385,3398,
            3400,3402,3404,3409,3420,3428,3429,3430,3431,3432,3433,3434,3435,3436,3439,3440,3446,
            3447,3452,3479,3480,3481,3482,3483,3484,3485,3486,3487,3488,3489,3491,3495,3496,3497,
            3498,3499,3500,3501,3502,3503,3507,3508,3509,3515,3516,3518,3524,3525,3527,3530,3534,
            3539,3549,3554,3555,3565,3566,3573,3582,3589,3590,3591,3592,3596,3642,3643,3644,3645,
            3652,3661,3662,3663,3665,3666,3667,3668,3669,3670,3671,3672,3673,3674,3675,3678,3689,
            3692,3705,3716,3717,3738,3741,3742,3786,3787,3788,3996,3997,3998,3999,4061,4062,4063,
            4064,4065,4066,4067,4068,4069,4078,4079,4080,4081,4082,4099,4100,4104,4105,4106,4107,
            4110,4111,4112,4113,4114,4123,4125,4126,4127,4136,4140,4148,4149,4152,4168,4173,4184,
            4189,4190,4191,4192,4193,4194,4195,4197,4198,4199,4200,4201,4215,4248,4251,4252,4254,
            4255,4264,4265,4277,4279,4280,4281,4282,4283,4284,4285,4286,4287,4288,4289,4292,4293,
            4297,4298,4299,4300,4302,4312,4315,4319,4361,4369,4484,4488,4494,4495,4496,4525,4564,
            4578,4611,4612,4615,4622,4623,4624,4625,4627,4646,4649,4668,4675,4722,4723,4724,4725,
            4726,4782,5078,5079,5080,5081,5082,5091,5092,5093,5094,5095,5096,5097,5103,5106,5125,
            5126,5133,5140,5148,5149,5150,5151,5163,5165,5167,5172,5177,5185,5186,5197,5198,5210,
            5215,5216,5229,5242,5246,5253,5254,5262,5263,5264,5265,5271,5592,5593,5595,5596,5712,
            5920,5957,6008,6136,6169,6177,6226,6228,6234,5,7
    };
    private static DbManager instance;

    private Context mContext;

    private DbManager(Context context) {
        super(new MyDbContext(context), DATABASE_NAME , null, 1);
    }

    public static DbManager getInstance(Context context) {
        if (instance == null)
            instance = new DbManager(context);
        instance.mContext = context;
        return instance;
    }

    public static DbManager getInstanceWithTest(Context context) {
        DbManager inst = getInstance(context);
        inst.getPage(1, 3); //test
        inst.mContext = context;
        return inst;
    }

    public static void dispose() {
        try {
            instance.close();
            instance.getWritableDatabase().close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        instance = null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public String getTafseer(int sura, int ayah) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(String.format(Locale.US,
                "select tafseer from mushaf where sura = %d and ayah = %d", sura, ayah), null);
        cursor.moveToFirst();
        try {
            if (cursor.isAfterLast() == false) {
                return cursor.getString(cursor.getColumnIndex("tafseer"));
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public TafsserViewModel getRandomTafseer() {
        Random random = new Random();
        int idx = SELECTED_TAFSEER_AYAT[random.nextInt(SELECTED_TAFSEER_AYAT.length)];
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(String.format(Locale.US,
                "select sura, ayah, page, tafseer from mushaf where ayah_index = %d", idx),
                null);
        cursor.moveToFirst();
        try {
            if (cursor.isAfterLast() == false) {
                TafsserViewModel res = new TafsserViewModel();
                res.ayahIndex = idx;
                res.sura = cursor.getInt(cursor.getColumnIndex("sura"));
                res.surahName = QuranData.getInstance(mContext).surahs[res.sura - 1].name;
                res.ayah = cursor.getInt(cursor.getColumnIndex("ayah"));
                res.page = cursor.getInt(cursor.getColumnIndex("page"));
                res.ayahText = Utils.getAyahText(mContext, res.sura, res.ayah);
                res.tafseer = cursor.getString(cursor.getColumnIndex("tafseer"));
                return res;
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public Page getPage(int page) {
        ArrayList<Ayah> result = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =  db.rawQuery("select * from mushaf where page = " + page + " order by sura, ayah", null);
        cursor.moveToFirst();
        int px, py, brx, blx;
        if (page <= 3) {
            px = 749; py = 380;
            blx = 113;
        } else {
            px = 870; py = 36;
            blx = 13;
        }
        brx = px;
        int lineHeight = 80;
        while (cursor.isAfterLast() == false) {
            int x = cursor.getInt(cursor.getColumnIndex("x")),
                    y = cursor.getInt(cursor.getColumnIndex("y"));
            Ayah item = new Ayah();
            item.ayah = cursor.getInt(cursor.getColumnIndex("ayah"));
            item.sura = cursor.getInt(cursor.getColumnIndex("sura"));
            item.rects = new ArrayList<>();
            int h = py - 10;
            int w = px;
            while (true) {
                if (h + lineHeight < y) {
                    if (w - blx > 35) // prevent ayah at line end small rect
                        item.rects.add(new RectF(blx, h, w, h + lineHeight));
                    h += lineHeight;
                    w = brx;
                } else {
                    item.rects.add(new RectF(x, h, w, y + 55));
                    break;
                }
            }
            if (item.rects.size() > 1) {
                RectF r = item.rects.get(item.rects.size() - 2);
                r.set(r.left, r.top, r.right, y - lineHeight / 3);
                r = item.rects.get(item.rects.size() - 1);
                r.set(r.left, y - lineHeight / 3, r.right, r.bottom);
            }
            result.add(item);
            px = x; py = y;
            cursor.moveToNext();
        }
        cursor.close();
        Page pg = new Page();
        pg.ayahs = result;
        pg.page = page;
        return pg;
    }

    public ArrayList<SearchResult> search(String word, QuranData quranData) {
        ArrayList<SearchResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =  db.rawQuery("select * from mushaf where ayahtext like ?",
                new String[] { "%" + word + "%" });
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            SearchResult res = new SearchResult(quranData);
            res.text = cursor.getString(cursor.getColumnIndex("ayahtext"));
            res.ayah = cursor.getInt(cursor.getColumnIndex("ayah"));
            res.surah = cursor.getInt(cursor.getColumnIndex("sura"));
            res.page = cursor.getInt(cursor.getColumnIndex("page"));
            res.query = word;
            results.add(res);
            cursor.moveToNext();
        }
        cursor.close();
        return results;
    }

    public int getPage(int sura, int ayah) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select page from mushaf where sura = ? and ayah = ?",
                new String[] { String.format(Locale.ENGLISH, "%d", sura), String.format(Locale.ENGLISH, "%d", ayah) });
        cursor.moveToFirst();
        if (cursor.isAfterLast() == false) {
            return cursor.getInt(cursor.getColumnIndex("page"));
        }
        cursor.close();
        return -1;
    }

//    private int numberOfRows(){
//        SQLiteDatabase db = this.getWritableDatabase();
//        int numRows = (int) DatabaseUtils.queryNumEntries(db, "contact");
//        return numRows;
//    }
}

class MyDbContext extends ContextWrapper {

    private final File externalFilesDir;

    private boolean shouldCreateDbInGetPath = true;

    public MyDbContext(Context base) {
        super(base);
        externalFilesDir = Utils.getDatabaseDir(base);
    }

    private void createDbIfNotExists(File dbFile) {
        if (!dbFile.exists() || dbFile.length() < 5000) {
            if (dbFile.exists())
                dbFile.delete();
            try {
                Utils.extractZippedFile(getAssets().open("shamerly.zip"), dbFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("فشل إنشاء قاعدة البيانات. تأكد من وجود سعة كافية بالجهاز");
            }
        }
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = externalFilesDir.getAbsolutePath();
        if (!dbfile.endsWith(File.separator))
            dbfile += File.separator;
        dbfile += name;
        if (!dbfile.endsWith(".db")) dbfile += ".db";
        File dbFile = new File(dbfile);
        if (shouldCreateDbInGetPath)
            createDbIfNotExists(dbFile);
        return dbFile;
    }

    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                                               SQLiteDatabase.CursorFactory factory,
                                               DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name, mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                                               SQLiteDatabase.CursorFactory factory) {
        shouldCreateDbInGetPath = false;
        File dbFile = getDatabasePath(name);
        boolean firstTry = true;
        while (true) {
            createDbIfNotExists(dbFile);
            try {
                return SQLiteDatabase.openDatabase(dbFile.getPath(), factory,
                        SQLiteDatabase.OPEN_READWRITE);
            } catch (Exception ex) {
                ex.printStackTrace();
                if (firstTry) {
                    dbFile.delete();
                    firstTry = false;
                } else throw ex;
            }
        }
    }
}