package kilanny.shamarlymushaf;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import kilanny.shamarlymushaf.data.alarms.Alarm;
import kilanny.shamarlymushaf.data.alarms.AlarmDb;
import kilanny.shamarlymushaf.util.Utils;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        Task asyncTask = new Task(pendingResult, context);
        asyncTask.execute();
    }

    private static class Task extends AsyncTask<Void, Void, Void> {

        private final PendingResult pendingResult;
        private final WeakReference<Context> mContext;

        private Task(PendingResult pendingResult, Context context) {
            this.pendingResult = pendingResult;
            mContext = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Alarm[] alarms = AlarmDb.getInstance(mContext.get()).alarmDao().getAllEnabled();
            Utils.scheduleAndDeletePrevious(mContext.get(), alarms);
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            super.onPostExecute(s);
            // Must call finish() so the BroadcastReceiver can be recycled.
            pendingResult.finish();
        }
    }
}