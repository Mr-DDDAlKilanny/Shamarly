package kilanny.shamarlymushaf.data.alarms;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;

@Entity(tableName = "alarm")
public class Alarm implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "time_mins")
    public int timeInMins;

    @ColumnInfo(name = "weekday_flags")
    public int weekDayFlags;

    @ColumnInfo(name = "enabled")
    public boolean enabled;

    @ColumnInfo(name = "alarm_label")
    public String alarmLabel;

    public Alarm() {
    }

    protected Alarm(Parcel in) {
        id = in.readInt();
        timeInMins = in.readInt();
        weekDayFlags = in.readInt();
        enabled = in.readByte() != 0;
        alarmLabel = in.readString();
    }

    public Alarm copy() {
        Intent intent = new Intent();
        intent.putExtra("o", this);
        return intent.getParcelableExtra("o");
    }

    public String toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("timeInMins", timeInMins);
        jsonObject.put("weekDayFlags", weekDayFlags);
        jsonObject.put("enabled", enabled);
        return jsonObject.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(timeInMins);
        dest.writeInt(weekDayFlags);
        dest.writeByte((byte) (enabled ? 1 : 0));
        dest.writeString(alarmLabel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static Alarm fromJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        Alarm alarm = new Alarm();
        alarm.id = jsonObject.getInt("id");
        alarm.timeInMins = jsonObject.getInt("timeInMins");
        alarm.weekDayFlags = jsonObject.getInt("weekDayFlags");
        alarm.enabled = jsonObject.getBoolean("enabled");
        return alarm;
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };
}
