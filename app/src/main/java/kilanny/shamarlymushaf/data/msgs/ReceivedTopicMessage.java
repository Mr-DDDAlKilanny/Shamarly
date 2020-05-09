package kilanny.shamarlymushaf.data.msgs;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "r_t_msg")
public class ReceivedTopicMessage {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "msg")
    @NonNull
    public String msg;

    @ColumnInfo(name = "topic", index = true)
    @NonNull
    public String topic;

    @ColumnInfo(name = "add_date")
    @NonNull
    public Date addDate;

    @ColumnInfo(name = "read_date")
    @Nullable
    public Date readDate;

    @Keep
    public ReceivedTopicMessage() {
    }

    @Ignore
    public ReceivedTopicMessage(@NonNull String topic, @NonNull String msg, @NonNull Date addDate) {
        this.topic = topic;
        this.msg = msg;
        this.addDate = addDate;
    }
}
