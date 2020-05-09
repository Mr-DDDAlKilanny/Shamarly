package kilanny.shamarlymushaf.data;

import android.content.Context;

import java.io.Serializable;

public class User implements Serializable {

    static final long serialVersionUID=0L;

    private static User instance;

    public static User getInstance(Context context) {
        if (instance == null) {
            SerializableInFile<User> serializableInFile = getUserSerializableInFile(context);
            instance = serializableInFile.getData();
            if (instance == null) {
                instance = new User();
                serializableInFile.setData(instance, context);
            }
        }
        return instance;
    }

    private static SerializableInFile<User> getUserSerializableInFile(Context context) {
        return new SerializableInFile<>(context, "__user");
    }

    public String analyticsUserId, fcmToken, appInstanceId;
    public boolean isSent;

    public void save(Context context) {
        SerializableInFile<User> serializableInFile = getUserSerializableInFile(context);
        serializableInFile.setData(this, context);
    }

    protected User() { }
}
