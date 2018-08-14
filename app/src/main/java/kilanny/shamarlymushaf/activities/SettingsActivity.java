package kilanny.shamarlymushaf.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;

/**
 * Created by Yasser on 10/02/2015.
 */
public class SettingsActivity extends PreferenceActivity {

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs);
            final CheckBoxPreference checkboxPref = (CheckBoxPreference) findPreference("displayDualPages");
            final ListPreference list = (ListPreference) findPreference("pageRotationMode");
            checkboxPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Boolean) {
                        if ((Boolean) newValue && !list.getValue().equals("2")) {
                            Utils.showConfirm(getActivity(), "تنبيه", "يعمل هذا الإعداد فقط عندما يكون عرض الصفحة أفقي دائما. تفعيل وضع الصفحة الأفقي الدائم الآن؟", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    list.setValue("2");
                                }
                            }, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    checkboxPref.setChecked(false);
                                }
                            });
                        }
                    }
                    return true;
                }
            });
            list.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue != null && newValue instanceof String) {
                        if (!newValue.equals("2"))
                            checkboxPref.setChecked(false);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }
}