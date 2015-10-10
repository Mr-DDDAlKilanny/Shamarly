package kilanny.shamarlymushaf;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.RectF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Yasser on 09/29/2015.
 */
public class DbManager extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "shamerly.db";
    private static DbManager instance;

    private DbManager(Context context) {
        super(new MyDbContext(context), DATABASE_NAME , null, 1);
    }

    public static DbManager getInstance() {
        return instance;
    }

    public static void init(Context context) {
        if (instance == null)
            instance = new DbManager(context);
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
        if (cursor.isAfterLast() == false) {
            return cursor.getString(cursor.getColumnIndex("tafseer"));
        }
        return null;
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
            result.add(item);
            px = x; py = y;
            cursor.moveToNext();
        }
        Page pg = new Page();
        pg.ayahs = result;
        pg.page = page;
        return pg;
    }

    public ArrayList<SearchResult> search(String word) {
        ArrayList<SearchResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =  db.rawQuery("select * from mushaf where ayahtext like '%" + word + "%'", null);
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            SearchResult res = new SearchResult();
            res.text = cursor.getString(cursor.getColumnIndex("ayahtext"));
            res.ayah = cursor.getInt(cursor.getColumnIndex("ayah"));
            res.surah = cursor.getInt(cursor.getColumnIndex("sura"));
            res.page = cursor.getInt(cursor.getColumnIndex("page"));
            results.add(res);
            cursor.moveToNext();
        }
        return results;
    }

    public int getPage(int sura, int ayah) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(String.format(Locale.US,
                "select page from mushaf where sura = %d and ayah = %d", sura, ayah), null);
        cursor.moveToFirst();
        if (cursor.isAfterLast() == false) {
            return cursor.getInt(cursor.getColumnIndex("page"));
        }
        return -1;
    }

    private int numberOfRows(){
        SQLiteDatabase db = this.getWritableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, "contact");
        return numRows;
    }
}

class MyDbContext extends ContextWrapper {

    public static File externalFilesDir;

    public MyDbContext(Context base) {
        super(base);
    }

    @Override
    public File getDatabasePath(String name) {
        String dbfile = externalFilesDir.getAbsolutePath() + File.separator + name;
        if (!dbfile.endsWith(".db")) dbfile += ".db";
        return new File(dbfile);
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
        File dbFile = getDatabasePath(name);
        if (!dbFile.exists()) {
            byte[] buffer = new byte[1024];
            OutputStream myOutput;
            int length;
            // Open your local db as the input stream
            InputStream myInput;
            try {
                ZipEntry ze = null;
                myOutput = new FileOutputStream(dbFile);
                myInput = getAssets().open("shamerly.zip");
                ZipInputStream zipIs = new ZipInputStream(myInput);
                while ((ze = zipIs.getNextEntry()) != null) {
                    while ((length = zipIs.read(buffer)) > 0) {
                        myOutput.write(buffer, 0, length);
                    }
                    zipIs.closeEntry();
                    myOutput.flush();
                    myOutput.close();
                    break;
                }
                zipIs.close();
                myInput.close();
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        SQLiteDatabase result = SQLiteDatabase.openDatabase(dbFile.getPath(), factory,
                SQLiteDatabase.OPEN_READWRITE);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        return result;
    }
}