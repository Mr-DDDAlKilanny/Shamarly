package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

import kilanny.shamarlymushaf.R;

public class DownloadedAyat {

    private static DownloadedAyat instance;

    public static DownloadedAyat getInstance(Context context) {
        if (instance == null)
            instance = new DownloadedAyat(context);
        return instance;
    }

    private final SerializableInFile<HashMap<String, Boolean[][]>> serializable;
    private final HashMap<String, Boolean[][]> data;

    public boolean get(String reciter, int sura, int ayah) {
        if (sura > 1)
            --ayah;
        return data.get(reciter)[sura - 1][ayah];
    }

    public int getSuraLength(String reciter, int sura) {
        return data.get(reciter)[sura - 1].length;
    }

    public void set(String reciter, int sura, int ayah, boolean value) {
        if (sura > 1)
            --ayah;
        Boolean[][] arr = data.get(reciter);
        if (arr != null) {
            if (sura - 1 >= 0 && ayah >= 0 && sura - 1 < arr.length && ayah < arr[sura - 1].length) {
                arr[sura - 1][ayah] = value;
            } else {
                // Mustafa_Ismail_48kbps.zip/89/31 (while max is 30)
                Log.w("DownloadAyat/set", "Ignored Invalid indexes " + sura + "," + ayah);
            }
        }
    }

    public void save(Context context) {
        serializable.setData(data, context);
    }

    private DownloadedAyat(Context context) {
        serializable = new SerializableInFile<>(context, "_downloadedAyat");
        HashMap<String, Boolean[][]> data = serializable.getData();
        if (data == null)
            data = new HashMap<>();
        this.data = data;
        if (data.isEmpty())
            reset(context);
    }

    public void reset(Context context) {
        reset(context, data, serializable);
    }

    private static void reset(Context context, HashMap<String, Boolean[][]> data,
                              SerializableInFile<HashMap<String, Boolean[][]>> serializable) {
        data.clear();
        for (String reciter : context.getResources().getStringArray(R.array.reciter_values)) {
            Boolean[][] suras = new Boolean[114][];
            for (int i = 0; i < 114; ++i) {
                suras[i] = new Boolean[QuranData.getInstance(context).surahs[i].ayahCount + (i == 0 ? 1 : 0)];
                Arrays.fill(suras[i], false);
            }
            data.put(reciter, suras);
        }
        serializable.setData(data, context);
    }
}
