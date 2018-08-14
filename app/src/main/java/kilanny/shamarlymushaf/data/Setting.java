package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Setting implements Serializable {
    private static Setting instnace;
    private static final String settingFilename = "myfile";

    public int page = 1;
    public boolean lastWasDualPage = false;
    public String saveSoundsDirectory;
    public ArrayList<ListItem> bookmarks = new ArrayList<>();

    private Setting() {
    }

    @Nullable
    private ListItem getBookmark(int p) {
        for (ListItem i : bookmarks) {
            if (Integer.parseInt(i.name) == p)
                return i;
        }
        return null;
    }

    public boolean isBookmarked(int p) {
        return getBookmark(p) != null;
    }

    public boolean toggleBookmark(int p) {
        ListItem b = getBookmark(p);
        if (b == null) {
            b = new ListItem();
            b.name = p + "";
            bookmarks.add(b);
            return true;
        } else {
            bookmarks.remove(b);
            return false;
        }
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(settingFilename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Setting getInstance(Context context) {
        if (instnace != null)
            return instnace;
        Setting setting = null;
        try {
            FileInputStream fis = context.openFileInput(settingFilename);
            ObjectInputStream is = new ObjectInputStream(fis);
            setting = (Setting) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (setting == null) {
            setting = new Setting();
        }
        return instnace = setting;
    }
}
