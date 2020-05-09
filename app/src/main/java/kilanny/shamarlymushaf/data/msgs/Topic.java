package kilanny.shamarlymushaf.data.msgs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "topic")
public class Topic {

    @PrimaryKey
    @ColumnInfo(name = "name")
    @NonNull
    public String name;

    @ColumnInfo(name = "subscribed_date")
    @Nullable
    public Date subscribedDate;

    public Topic() {
    }

    @Ignore
    public Topic(@NonNull String name, @Nullable Date subscribedDate) {
        this.name = name;
        this.subscribedDate = subscribedDate;
    }
}
