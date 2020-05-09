package kilanny.shamarlymushaf.data.alarms;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface AlarmDao {

    @Query("SELECT * FROM alarm ORDER BY id")
    Alarm[] getAll();

    @Query("SELECT * FROM alarm WHERE enabled = 1 ORDER BY id")
    Alarm[] getAllEnabled();

    @Insert
    long insert(Alarm alarm);

    @Update
    void update(Alarm alarm);

    @Delete
    void delete(Alarm alarm);

    @Query("SELECT COUNT(*) FROM alarm")
    int count();
}
