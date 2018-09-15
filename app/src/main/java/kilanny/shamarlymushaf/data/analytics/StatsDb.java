package kilanny.shamarlymushaf.data.analytics;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {Stat.class}, version = 1, exportSchema = false)
@TypeConverters({ Converters.class })
public abstract class StatsDb extends RoomDatabase {
    public abstract StatDao statDao();
}
