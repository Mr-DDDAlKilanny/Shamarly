package kilanny.shamarlymushaf.data.msgs;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.Date;

@Dao
public interface ReceivedTopicMessageDao {

    @Query("SELECT * FROM r_t_msg WHERE topic = :topic AND add_date ORDER BY add_date LIMIT :skip, :count")
    ReceivedTopicMessage[] getRange(String topic, int skip, int count);

    @Query("SELECT COUNT(*) FROM r_t_msg WHERE topic = :topic AND id < :msgId ORDER BY add_date")
    long getPosition(String topic, long msgId);

    @Insert
    long[] insert(ReceivedTopicMessage... messages);

    @Query("UPDATE r_t_msg SET read_date = :date WHERE id IN (:messageIds)")
    int markAsRead(Date date, long... messageIds);

    @Delete
    int delete(ReceivedTopicMessage... messages);

    @Query("SELECT COUNT(*) FROM r_t_msg WHERE read_date IS NULL")
    int unreadCount();

    @Query("SELECT COUNT(*) FROM r_t_msg WHERE topic = :topic AND read_date IS NULL")
    int unreadCount(String topic);

    @Query("SELECT topic, COUNT(*) unreadCount FROM r_t_msg WHERE read_date IS NULL GROUP BY topic")
    UnreadTopicsResult[] getUnreadTopics();

    @Query("SELECT COUNT(*) FROM r_t_msg WHERE topic = :topic")
    int count(String topic);
}
