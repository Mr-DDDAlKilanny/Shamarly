package kilanny.shamarlymushaf.data.alarms;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import kilanny.shamarlymushaf.data.RoomConverters;

@Database(entities = {Alarm.class}, version = 1, exportSchema = false)
@TypeConverters({ RoomConverters.class })
public abstract class AlarmDb extends RoomDatabase {

    private static AlarmDb instance;

    public static AlarmDb getInstance(Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            instance = Room.databaseBuilder(appContext, AlarmDb.class, "alarm-db")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public abstract AlarmDao alarmDao();
}
