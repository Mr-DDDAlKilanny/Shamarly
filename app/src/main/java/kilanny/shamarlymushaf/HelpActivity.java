package kilanny.shamarlymushaf;

import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TabHost;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class HelpActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        TabHost tabHost = (TabHost) findViewById(R.id.tabHostHelp);
        tabHost.setup();
        TabHost.TabSpec tab1 = tabHost.newTabSpec("tab1");
        TabHost.TabSpec tab2 = tabHost.newTabSpec("tab2");
        TabHost.TabSpec tab3 = tabHost.newTabSpec("tab3");
        TabHost.TabSpec tab4 = tabHost.newTabSpec("tab4");
        TabHost.TabSpec tab5 = tabHost.newTabSpec("tab5");
        tab1.setIndicator("حول");
        tab1.setContent(R.id.حول);
        tab2.setIndicator("المصحف");
        tab2.setContent(R.id.المصحف);
        tab3.setIndicator("التلاوة");
        tab3.setContent(R.id.التلاوة);
        tab4.setIndicator("أخرى");
        tab4.setContent(R.id.أخرى);
        tab5.setIndicator("رسالة");
        tab5.setContent(R.id.رسالة);
        /** Add the tabs  to the TabHost to display. */
        tabHost.addTab(tab1);
        tabHost.addTab(tab2);
        tabHost.addTab(tab3);
        tabHost.addTab(tab4);
        tabHost.addTab(tab5);
        AssetManager am = getAssets();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Typeface typeface = pref.getBoolean("fontBold", false) ?
                Typeface.createFromAsset(am, "DroidNaskh-Regular.ttf")
                : Typeface.createFromAsset(am, "DroidNaskh-Bold.ttf");
        TextView textViewAbout = (TextView) findViewById(R.id.helpTextAbout);
        textViewAbout.setTypeface(typeface);
        textViewAbout.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        TextView textViewMushaf = (TextView) findViewById(R.id.helpTextMushaf);
        textViewMushaf.setTypeface(typeface);
        textViewMushaf.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        TextView textViewRecite = (TextView) findViewById(R.id.helpTextRecite);
        textViewRecite.setTypeface(typeface);
        textViewRecite.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        TextView helpTextOther = (TextView) findViewById(R.id.helpTextOther);
        helpTextOther.setTypeface(typeface);
        helpTextOther.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        TextView textViewMsg = (TextView) findViewById(R.id.helpTextMsg);
        textViewMsg.setTypeface(typeface);
        textViewMsg.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                Float.parseFloat(pref.getString("fontSize", "20")));
        String all;
        try {
            InputStream is = am.open("help.txt");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int i;
            i = is.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = is.read();
            }
            all = byteArrayOutputStream.toString();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int idx = all.indexOf("*");
        idx = all.indexOf("*", idx + 1);
        textViewAbout.setText(all.substring(0, idx));
        int tmp = idx;
        idx = all.indexOf("*", idx + 1);
        textViewMushaf.setText(all.substring(tmp, idx));
        tmp = idx;
        idx = all.indexOf("*", idx + 1);
        textViewRecite.setText(all.substring(tmp, idx));
        tmp = idx;
        idx = all.indexOf("=", idx + 1);
        helpTextOther.setText(all.substring(tmp, idx));
        textViewMsg.setText(all.substring(all.lastIndexOf("=") + 1));
    }
}
