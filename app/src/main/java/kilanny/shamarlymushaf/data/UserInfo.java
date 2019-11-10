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
    public String appVersionCode, appVersionName;
    public final String deviceOsVersion, deviceSdkVersion,
            deviceProduct, deviceName, deviceModel;
    public Integer ageCategory;
    public Boolean gender, hasChanged;
    public Integer locationCategory;

    private UserInfo() {
        appInstanceId = Utils.newUid();
        loadAppVersionDetails();
        deviceOsVersion = System.getProperty("os.version");
        deviceSdkVersion = "" + Build.VERSION.SDK_INT;
        deviceName = Build.DEVICE;
        deviceModel = Build.MODEL;
        deviceProduct = Build.PRODUCT;
    }

    private void loadAppVersionDetails() {
        String code = "" + BuildConfig.VERSION_CODE;
        appVersionName = BuildConfig.VERSION_NAME;
        if (appVersionCode != null && !appVersionCode.equals(code))
            hasChanged = true;
        appVersionCode = code;
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
        setting.loadAppVersionDetails();
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
