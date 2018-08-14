package kilanny.shamarlymushaf.data;

public class SajdahSakt {
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
