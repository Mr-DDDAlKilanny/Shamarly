package kilanny.shamarlymushaf.data.analytics;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

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
