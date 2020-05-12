package kilanny.shamarlymushaf.data.msgs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;

@Dao
public interface TopicDao {

    @Query("SELECT subscribed_date FROM topic WHERE name = :topic")
    @Nullable
    Date getSubscribeDate(@NonNull String topic);

    @Query("UPDATE topic SET subscribed_date = :subscribeDate WHERE name = :topic")
    int setSubscribedDate(@NonNull String topic, @Nullable Date subscribeDate);

    @Query("SELECT * FROM topic")
    Topic[] getAll();

    @Query("SELECT notify FROM topic WHERE name = :topic")
    boolean getNotify(String topic);

    @Query("UPDATE topic SET notify = :notify WHERE name = :topic")
    int setNotify(String topic, boolean notify);

    @Insert
    long[] insert(Topic... topics);
}
