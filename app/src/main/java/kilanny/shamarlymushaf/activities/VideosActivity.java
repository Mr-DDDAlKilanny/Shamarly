package kilanny.shamarlymushaf.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONException;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.VideosAdapter;
import kilanny.shamarlymushaf.util.Utils;

public class VideosActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);
        if (Utils.isConnected(this) != Utils.CONNECTION_STATUS_CONNECTED) {
            Toast.makeText(getApplicationContext(),
                    "يجب الاتصال بالإنترنت لعرض خدمة المرئيات",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show());
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        FirebaseRemoteConfig instance = FirebaseRemoteConfig.getInstance();
        instance.fetchAndActivate().addOnCompleteListener(command -> {
            if (command.isSuccessful()) {
                String s = instance.getString("yt_videos");
                if (!s.equals(FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING)) {
                    try {
                        JSONArray jsonObject = new JSONArray(s);
                        String[] videos = new String[jsonObject.length()];
                        for (int i = 0; i < videos.length; ++i)
                            videos[i] = jsonObject.getJSONObject(i).getString("ytId");
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                        preferences.edit().putLong("videosHash", Utils.hash(s)).apply();
                        recyclerView.setAdapter(new VideosAdapter(videos));
                        return;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            Toast.makeText(getApplicationContext(),
                    "فشل الاتصال بالخادم",
                    Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean("hasUnseenVideos", false).apply();
    }
}
