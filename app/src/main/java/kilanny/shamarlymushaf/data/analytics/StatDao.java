package kilanny.shamarlymushaf.data.analytics;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

@Dao
public interface StatDao {
    @Query("SELECT * FROM stat ORDER BY id LIMIT :count")
    Stat[] getFirstItems(int count);

    @Insert
    void insertMany(Stat... stats);

    @Delete
    void deleteMany(Stat... stat);

    @Query("SELECT COUNT(*) FROM stat")
    int count();
}
