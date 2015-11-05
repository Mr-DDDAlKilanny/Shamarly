package kilanny.shamarlymushaf;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import java.io.Serializable;

public class WelcomeActivity extends AppCompatActivity {

    private Serializable serializable;
    private static final int MAIN_REQUEST = 6236;
    private static final int SEARCH_REQUEST = 6214;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Intent myIntent = getIntent();
        serializable = myIntent.getSerializableExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES);
        ImageButton btn = (ImageButton) findViewById(R.id.openQuran);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES, serializable);
                startActivityForResult(i, MAIN_REQUEST);
            }
        });
        btn = (ImageButton) findViewById(R.id.openSearch);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(WelcomeActivity.this, SearchActivity.class);
                i.putExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES, serializable);
                startActivityForResult(i, SEARCH_REQUEST);
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
        btn = (ImageButton) findViewById(R.id.reciter_download);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, ReciterListActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAIN_REQUEST || requestCode == SEARCH_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Serializable tmp = data.getSerializableExtra(MainActivity.EXTRA_NON_DOWNLOADED_PAGES);
                if (tmp != null)
                    serializable = tmp;
            }
        }
    }
}
