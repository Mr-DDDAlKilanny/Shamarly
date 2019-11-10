package kilanny.shamarlymushaf.activities;

import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.ExpandableListAdapter;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

public class ReportIssueActivity extends AppCompatActivity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);
        expListView = findViewById(R.id.expList);

        prepareListData();
        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);
        expListView.setAdapter(listAdapter);

        findViewById(R.id.btnReportIssue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsTrackers.send(getApplicationContext());
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setDataAndType(Uri.parse("mailto:"), "text/plain");
                try {
                    emailIntent.putExtra(Intent.EXTRA_EMAIL,
                            new String[] {"ibrahimalkilanny@gmail.com"});
                    emailIntent.putExtra(Intent.EXTRA_TEXT,
                            "السلام عليكم ورحمة الله\n\n\n***\nمعلومات الجهاز الخاص بي:\n"
                                    + AnalyticsTrackers.getDeviceInfo(ReportIssueActivity.this));
                    startActivity(Intent.createChooser(emailIntent, "إرسال إيميل"));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(ReportIssueActivity.this,
                            "لا يوجد مزود بريد إلكتروني.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        XmlResourceParser parser = getResources().getXml(R.xml.helpdisk_articles);
        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("article")) {
                        String head = parser.getAttributeValue(null, "subject").trim();
                        listDataHeader.add(head);
                        ArrayList<String> childs = new ArrayList<>();
                        childs.add(parser.getAttributeValue(null, "text").trim());
                        listDataChild.put(head, childs);
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
