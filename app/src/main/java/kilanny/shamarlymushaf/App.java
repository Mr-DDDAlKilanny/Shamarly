package kilanny.shamarlymushaf;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.google.firebase.analytics.FirebaseAnalytics;

public class App extends Application {

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
        }
        super.onCreate();
    }
}
