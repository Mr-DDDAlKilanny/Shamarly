package kilanny.shamarlymushaf.data;

import android.arch.persistence.room.Room;
import android.content.Context;

import java.util.Date;

import kilanny.shamarlymushaf.data.analytics.Stat;
import kilanny.shamarlymushaf.data.analytics.StatsDb;

public class AnalyticData {

    public String eventName;
    public Date date;
    public String payload;

    public static class Queue {
        private static final Object ox = new Object();
        public static final int MAX_ENTRIES = 6636 + 114;
        private static StatsDb statsDbInstance;

        private static StatsDb getDb(Context context) {
            if (statsDbInstance != null)
                return statsDbInstance;
            statsDbInstance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    StatsDb.class, "analytics-db").build();
            return statsDbInstance;
        }

        public static int size(Context context) {
            synchronized (ox) {
                return getDb(context).statDao().count();
            }
        }

        public static void enqueueMany(Context context, AnalyticData... analyticData) {
            int sz = size(context);
            synchronized (ox) {
                StatsDb db = getDb(context);
                if (sz + analyticData.length > MAX_ENTRIES) {
                    Stat[] items = db.statDao().getFirstItems(
                            analyticData.length - (MAX_ENTRIES - sz));
                    db.statDao().deleteMany(items);
                }
                Stat[] stats = new Stat[analyticData.length];
                for (int i = 0; i < analyticData.length; ++i) {
                    stats[i] = new Stat(analyticData[i]);
                }
                db.statDao().insertMany(stats);
            }
        }

        public static AnalyticData[] dequeueMany(Context context, int count) {
            int sz = size(context);
            synchronized (ox) {
                if (sz < count)
                    throw new IllegalArgumentException();
                StatsDb db = getDb(context);
                Stat[] items = db.statDao().getFirstItems(count);
                AnalyticData[] data = new AnalyticData[items.length];
                for (int i = 0; i < data.length; ++i)
                    data[i] = items[i].toAnalyticData();
                db.statDao().deleteMany(items);
                return data;
            }
        }
    }
}
