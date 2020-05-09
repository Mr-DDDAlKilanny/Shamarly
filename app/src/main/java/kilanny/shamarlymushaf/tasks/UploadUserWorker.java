package kilanny.shamarlymushaf.tasks;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import kilanny.shamarlymushaf.data.User;
import kilanny.shamarlymushaf.util.Utils;

public class UploadUserWorker extends Worker {

    public UploadUserWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Utils.isConnected(getApplicationContext()) != Utils.CONNECTION_STATUS_CONNECTED)
            return Result.retry();

        final AtomicReference<FirebaseUser> currentUser = new AtomicReference<>(null);
        final Lock lock = new ReentrantLock(true);
        final Condition condition = lock.newCondition();

        lock.lock();
        FirebaseUser tmp = Utils.getOrCreateAnonymousFirebaseUser(input -> {
            currentUser.set(input);
            lock.lock();
            condition.signalAll();
            lock.unlock();
            return null;
        });
        if (tmp != null) {
            currentUser.set(tmp);
            lock.unlock();
        } else {
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Result.retry();
            } finally {
                lock.unlock();
            }
        }
        if (currentUser.get() == null) {
            Log.w("FCM", "Failed to get current FB user");
            return Result.retry();
        }

        lock.lock();
        AtomicBoolean success = new AtomicBoolean(false);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(getApplicationContext());
        analytics.getAppInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String pseudoUserId = task.getResult();
                Log.i("FCM/onNewToken", "User: " + pseudoUserId);
                User user = User.getInstance(getApplicationContext());
                user.analyticsUserId = pseudoUserId;
                user.appInstanceId = analytics.getFirebaseInstanceId();
                user.save(getApplicationContext());
            } else Log.w("FCM/onNewToken", "Cannot get User", task.getException());
            lock.lock();
            success.set(task.isSuccessful());
            condition.signalAll();
            lock.unlock();
        });
        try {
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Result.retry();
        } finally {
            lock.unlock();
        }
        if (!success.get())
            return Result.retry();

        User user = User.getInstance(getApplicationContext());
        Map<String, Object> values = new HashMap<>();
        values.put("ltcsUsrId", user.analyticsUserId);
        values.put("fbInstId", user.appInstanceId);
        values.put("fcmTok", user.fcmToken);

        lock.lock();
        success.set(false);
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s", currentUser.get().getUid()))
                .setValue(values).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                Log.w("FCM", "failed to save to fd db", task.getException());
            lock.lock();
            success.set(task.isSuccessful());
            condition.signalAll();
            lock.unlock();
        });
        try {
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        if (!success.get()) {
            Log.w("FCM/onNewToken", "Failed submitting user info");
            return Result.retry();
        }
        FirebaseAnalytics.getInstance(getApplicationContext()).setUserId(currentUser.get().getUid());
        user.isSent = true;
        user.save(getApplicationContext());
        Log.i("FCM/onNewToken", "Successfully submitted user info: " + currentUser.get().getUid());
        return Result.success();
    }
}
