package kilanny.shamarlymushaf.data.analytics;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

import kilanny.shamarlymushaf.data.AnalyticData;

@Entity(tableName = "stat")
public class Stat {

    public Stat() {
    }

    public Stat(AnalyticData analyticData) {
        this.date = analyticData.date;
        this.eventName = analyticData.eventName;
        this.payload = analyticData.payload;
    }

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "event_name")
    private String eventName;

    @ColumnInfo(name = "payload")
    private String payload;

    @ColumnInfo(name = "date")
    public Date date;

    public int getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPayload() {
        return payload;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public AnalyticData toAnalyticData() {
        AnalyticData analyticData = new AnalyticData();
        analyticData.payload = payload;
        analyticData.date = date;
        analyticData.eventName = eventName;
        return analyticData;
    }
}
