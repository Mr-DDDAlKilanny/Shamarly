package kilanny.shamarlymushaf.data.analytics;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Stat.class}, version = 1, exportSchema = false)
@TypeConverters({ Converters.class })
public abstract class StatsDb extends RoomDatabase {
    public abstract StatDao statDao();
}
