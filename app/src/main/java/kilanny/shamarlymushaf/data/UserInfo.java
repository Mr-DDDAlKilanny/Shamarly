package kilanny.shamarlymushaf.data;

import android.content.Context;
import android.os.Build;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import kilanny.shamarlymushaf.BuildConfig;
import kilanny.shamarlymushaf.util.Utils;

public class UserInfo implements Serializable {
    private static UserInfo instance;
    private static final String settingFilename = "UserInfo.dat";

    static final long serialVersionUID = 1L;

    public final String appInstanceId;
    public final String appVersionCode, appVersionName,
            deviceOsVersion, deviceSdkVersion,
            deviceProduct, deviceName, deviceModel;
    public Integer ageCategory;
    public Boolean gender;
    public Integer locationCategory;

    private UserInfo() {
        appInstanceId = Utils.newUid();
        appVersionName = BuildConfig.VERSION_NAME;
        appVersionCode = "" + BuildConfig.VERSION_CODE;
        deviceOsVersion = System.getProperty("os.version");
        deviceSdkVersion = "" + Build.VERSION.SDK_INT;
        deviceName = Build.DEVICE;
        deviceModel = Build.MODEL;
        deviceProduct = Build.PRODUCT;
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(settingFilename, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UserInfo getInstance(Context context) {
        if (instance != null)
            return instance;
        UserInfo setting = null;
        try {
            FileInputStream fis = context.openFileInput(settingFilename);
            ObjectInputStream is = new ObjectInputStream(fis);
            setting = (UserInfo) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (setting == null) {
            setting = new UserInfo();
            setting.save(context);
        }
        return instance = setting;
    }

    public String[] asStrings() {
        return new String[] {
                "Id: " + appInstanceId,
                "VC: " + appVersionCode,
                "VN: " + appVersionName,
                "dO: " + deviceOsVersion,
                "dS: " + deviceSdkVersion,
                "dP: " + deviceProduct,
                "dN: " + deviceName,
                "dM: " + deviceModel,
                "ag: " + ageCategory,
                "ge: " + gender,
                "lo: " + locationCategory
        };
    }
}
