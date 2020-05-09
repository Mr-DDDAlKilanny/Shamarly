package kilanny.shamarlymushaf.data.msgs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Date;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.RoomConverters;
import kilanny.shamarlymushaf.util.AppExecutors;

@Database(entities = {ReceivedTopicMessage.class, Topic.class}, version = 1, exportSchema = false)
@TypeConverters({ RoomConverters.class })
public abstract class FirebaseMessagingDb extends RoomDatabase {

    private static FirebaseMessagingDb instance;

    public static FirebaseMessagingDb getInstance(Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            instance = Room.databaseBuilder(appContext, FirebaseMessagingDb.class, "fbmessaging-db")
                    .addCallback(new DbSeeder(appContext))
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public abstract ReceivedTopicMessageDao receivedTopicMessageDao();
    public abstract TopicDao topicDao();

    private static class DbSeeder extends Callback {

        private Context appContext;

        DbSeeder(Context context) {
            appContext = context;
        }

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                instance.receivedTopicMessageDao().insert(new ReceivedTopicMessage(
                        appContext.getString(R.string.dayAyahTopic),
                        appContext.getString(R.string.intro_day_msg),
                        new Date()
                ));
            });
        }
    }
}
