package kilanny.shamarlymushaf;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class WelcomeActivity extends AppCompatActivity {
    public static final ListItem[] juzs = new ListItem[31];
    public static final ListItem[] hizbs = new ListItem[61];
    public static final ListItem[] surahs2 = new ListItem[115];
    public static final Surah[] surahs = new Surah[114];

    private void initQuranData() {
        XmlResourceParser parser = getResources().getXml(R.xml.qurandata);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                String name;
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("surah")) {
                            Surah s = new Surah();
                            s.index = parser.getAttributeIntValue(null, "index", 0);
                            s.name = parser.getAttributeValue(null, "name").trim();
                            s.page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                            surahs[s.index - 1] = s;
                        } else if (name.equals("hizb")) {
                            ListItem h = new ListItem();
                            int idx = Integer.parseInt(parser.getAttributeValue(null, "index").trim());
                            int page = Integer.parseInt(parser.getAttributeValue(null, "page").trim());
                            h.name = "الحزب " + ArabicNumbers.numToStr(idx);
                            h.value = page;
                            hizbs[idx] = h;
                            if (idx % 2 == 1) {
                                ListItem j = new ListItem();
                                j.name = "الجزء " + ArabicNumbers
                                        .numToStr(1 + idx / 2);
                                j.value = page;
                                juzs[1 + idx / 2] = j;
                            }
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        ListItem juz = new ListItem(), hizb = new ListItem(), surah = new ListItem();
        juz.name = "اختر الجزء";
        hizb.name = "اختر الحزب";
        surah.name = "اختر السورة";
        juzs[0] = juz;
        hizbs[0] = hizb;
        surahs2[0] = surah;
        for (int i = 1; i < 114 + 1; ++i) {
            ListItem item = new ListItem();
            item.value = i;
            item.name = surahs[i - 1].name;
            surahs2[i] = item;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyDbContext.externalFilesDir = getExternalFilesDir(null);
        DbManager.init(this);
        setContentView(R.layout.activity_welcome);
        initQuranData();

        ImageButton btn = (ImageButton) findViewById(R.id.openQuran);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openSearch);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, SearchActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openSettings);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, SettingsActivity.class));
            }
        });
        btn = (ImageButton) findViewById(R.id.openHelp);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, HelpActivity.class));
            }
        });
    }

}
