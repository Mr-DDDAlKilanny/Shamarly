package kilanny.shamarlymushaf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class AyahFile {
    public final DocumentFile suraDir;
    public final String ayah;

    public AyahFile(@NonNull DocumentFile suraDir, @NonNull String ayah) {
        this.suraDir = suraDir;
        this.ayah = ayah;
    }

    @Nullable
    public DocumentFile get() {
        return suraDir.findFile(ayah);
    }

    public DocumentFile getOrCreate() {
        DocumentFile ayahFile = get();
        if (ayahFile != null) return ayahFile;
        return suraDir.createFile("", ayah);
    }
}
