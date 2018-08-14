package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import kilanny.shamarlymushaf.util.Utils;

/**
 * Created by Yasser on 10/31/2015.
 */
public class TafseerDbManager extends SQLiteOpenHelper {

    private static TafseerDbManager instance;

    public static TafseerDbManager getInstance(Context context) {
        if (instance == null)
            instance = new TafseerDbManager(context);
        return instance;
    }

    private ArrayList<ListItem> tafaseer;

    private TafseerDbManager(Context context) {
        super(new ContextWrapper(context) {

            @Override
            public File getDatabasePath(String name) {
                return Utils.getTafaseerDbFile(getApplicationContext());
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
                return SQLiteDatabase.openDatabase(getDatabasePath(name).getPath(), factory,
                        SQLiteDatabase.OPEN_READWRITE);
            }
        }, "tafaseer.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<ListItem> getAvailableTafaseer() {
        if (tafaseer != null) return tafaseer;
        ArrayList<ListItem> results = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor =  db.rawQuery("select * from tafseerName", null);
        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            ListItem res = new ListItem();
            res.name = cursor.getString(cursor.getColumnIndex("Name"));
            res.value = cursor.getInt(cursor.getColumnIndex("ID"));
            results.add(res);
            cursor.moveToNext();
        }
        cursor.close();
        return tafaseer = results;
    }

    public String getTafseer(int id, int sura, int ayah) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(String.format(Locale.ENGLISH,
                "select nass from tafseer where tafseer = %d and sura = %d and ayah = %d",
                id, sura, ayah), null);
        cursor.moveToFirst();
        try {
            if (cursor.isAfterLast() == false) {
                return cursor.getString(cursor.getColumnIndex("nass"));
            }
            return null;
        } finally {
            cursor.close();
        }
    }
}
