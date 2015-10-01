package kilanny.shamarlymushaf;

/**
 * Created by Yasser on 09/29/2015.
 */
final class ArabicNumbers {
    private static final String ahaad[] = { "الأول", "الثاني", "الثالث", "الرابع",
            "الخامس", "السادس", "السابع", "الثامن", "التاسع", "العاشر", "الحادي عشر",
            "الثاني عشر", "الثالث عشر", "الرابع عشر", "الخامس عشر", "السادس عشر",
            "السابع عشر", "الثامن عشر", "التاسع عشر"};
    private static final String asharaat[] = {"", "والعشرون", "والثلاثون", "والأربعون",
            "والخمسون", "والستون"};

    public static String numToStr(int x) {
        if (x < 20)
            return ahaad[x - 1];
        if (x % 10 == 0)
            return asharaat[x / 10 - 1].substring(1);
        String first = x % 10 == 1 ? "الحادي" : ahaad[x % 10 - 1];
        return first + " " + asharaat[x / 10 - 1];
    }

    private ArabicNumbers() {}
}
