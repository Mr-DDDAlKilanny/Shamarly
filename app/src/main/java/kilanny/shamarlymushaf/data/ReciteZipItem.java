package kilanny.shamarlymushaf.data;

import android.content.Context;

import androidx.annotation.NonNull;

import kilanny.shamarlymushaf.R;

public final class ReciteZipItem {
    public final String name, url, dirName, fileName, size;

    private ReciteZipItem(String name, String url, String dirName, String fileName, String size) {
        this.name = name;
        this.url = url;
        this.dirName = dirName;
        this.fileName = fileName;
        this.size = size;
    }

    public static ReciteZipItem[] getAll(@NonNull Context context) {
        String[] names = context.getResources().getStringArray(R.array.reciter_names);
        String[] values = context.getResources().getStringArray(R.array.reciter_values);
        String[] fileNames = context.getResources().getStringArray(R.array.full_quran_download_file_names);
        String[] sizes = context.getResources().getStringArray(R.array.full_quran_download_sizes);
        String[] urls = context.getResources().getStringArray(R.array.full_quran_download_files);
        ReciteZipItem[] items = new ReciteZipItem[names.length];
        for (int i = 0; i < names.length; ++i) {
            items[i] = new ReciteZipItem(names[i], urls[i], values[i], fileNames[i],
                    sizes[i].replace("G.B.", "غيغا"));
        }
        return items;
    }
}
