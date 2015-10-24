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
    private static final char[] DIGITS = {'\u0660','\u0661','\u0662','\u0663','\u0664','\u0665','\u0666','\u0667','\u0668','\u0669'};

    /**
     * Returns <code>true</code> if the provided language code uses arabic characters; othersise <code>false</code>.
     * @param lang ISO language code.
     * @return <code>true</code> if the provided language code uses arabic characters; othersise <code>false</code>
     */
    public static boolean isArabic (String lang) {
        return "ar".equals(lang) || "fa".equals(lang) || "ur".equals(lang);
    }

    /**
     * Convert digits in the specified string to arabic digits.
     */
    public static String convertDigits (String str) {
        if (str == null || str.length() == 0) return str;

        char[] s = new char[str.length()];
        for(int i =0;i<s.length;i++)
            s[i] = toDigit( str.charAt( i ) );

        return new String(s);
    }

    /**
     * Convert single digit in the specified string to arabic digit.
     */
    public static char toDigit (char ch) {
        int n = Character.getNumericValue( (int)ch );
        return n >=0 && n < 10 ? DIGITS[n] : ch;
    }

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
