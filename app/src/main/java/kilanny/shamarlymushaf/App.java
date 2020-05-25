package kilanny.shamarlymushaf;

import android.app.Application;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.concurrent.TimeUnit;

import kilanny.shamarlymushaf.tasks.FetchRemoteConfigWorker;
import kilanny.shamarlymushaf.util.Utils;

public class App extends Application {

    @Override
    public void onCreate() {
        if (Utils.isGooglePlayServicesAvailable(this)) {
            try {
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
                FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true);
                FirebaseMessaging.getInstance().setAutoInitEnabled(true);
                FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
                FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(60 * 60 * 8)
                        .build();
                mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                        "FetchRemoteConfig", ExistingPeriodicWorkPolicy.REPLACE,
                        new PeriodicWorkRequest.Builder(FetchRemoteConfigWorker.class, 12, TimeUnit.HOURS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .setBackoffCriteria(BackoffPolicy.LINEAR, 3, TimeUnit.HOURS)
                        .build());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        super.onCreate();
    }
}
