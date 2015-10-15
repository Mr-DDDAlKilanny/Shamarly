package kilanny.shamarlymushaf;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HelpActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        AssetManager am = getAssets();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        TextView textView = (TextView) findViewById(R.id.helpText);
        Typeface tradionalArabicFont = Typeface.createFromAsset(am, "DroidNaskh-Regular.ttf");
        Typeface tradionalArabicBoldFont = Typeface.createFromAsset(am, "DroidNaskh-Bold.ttf");
        textView.setTypeface(pref.getBoolean("fontBold", false) ?
                tradionalArabicFont : tradionalArabicBoldFont);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        try {
            InputStream is = am.open("help.txt");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            i = is.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = is.read();
            }
            textView.setText(byteArrayOutputStream.toString());
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
