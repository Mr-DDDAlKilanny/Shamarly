package kilanny.shamarlymushaf.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class FetchRemoteConfigWorker extends Worker {

    public FetchRemoteConfigWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("ApplySharedPref")
    @NonNull
    @Override
    public Result doWork() {
        if (Utils.isConnected(getApplicationContext()) != Utils.CONNECTION_STATUS_CONNECTED)
            return Result.retry();
        if (!Utils.isGooglePlayServicesAvailable(getApplicationContext()))
            return Result.failure();

        final Lock lock = new ReentrantLock(true);
        final Condition condition = lock.newCondition();
        AtomicBoolean success = new AtomicBoolean(false);
        FirebaseRemoteConfig instance = FirebaseRemoteConfig.getInstance();
        AppExecutors.getInstance().executeOnCachedExecutor(() ->
            instance.fetchAndActivate().addOnCompleteListener(command -> {
                if (command.isSuccessful()) {
                    String s = instance.getString("yt_videos");
                    if (!s.equals(FirebaseRemoteConfig.DEFAULT_VALUE_FOR_STRING)) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        long hashCode = Utils.hash(s);
                        if (preferences.getLong("videosHash", 0) != hashCode) {
                            preferences.edit()
                                    .putLong("videosHash", hashCode)
                                    .putBoolean("hasUnseenVideos", true)
                                    .commit();
                        }
                    }
                }
                lock.lock();
                success.set(command.isSuccessful());
                condition.signalAll();
                lock.unlock();
            }));
        try {
            lock.lock();
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.retry();
        } finally {
            lock.unlock();
        }
        if (!success.get())
            return Result.retry();
        return Result.success();
    }
}
