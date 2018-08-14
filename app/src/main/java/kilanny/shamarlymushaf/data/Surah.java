package kilanny.shamarlymushaf.data;

public class Surah {
    public String name;
    public int page;
    public int index;
    public int ayahCount;

    @Override
    public String toString() {
        return "سورة " + name;
    }
}
