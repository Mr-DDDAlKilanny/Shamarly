package kilanny.shamarlymushaf.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;

/**
 * Created by Yasser on 10/02/2015.
 */
public class SettingsActivity extends AppCompatActivity {

    public static class MyPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.prefs, rootKey);
            final SwitchPreferenceCompat checkboxPref = findPreference("displayDualPages");
            final ListPreference list = findPreference("pageRotationMode");
            checkboxPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof Boolean) {
                    if ((Boolean) newValue && !list.getValue().equals("2")) {
                        Utils.showConfirm(getActivity(), "تنبيه", "يعمل هذا الإعداد فقط عندما يكون عرض الصفحة أفقي دائما. تفعيل وضع الصفحة الأفقي الدائم الآن؟",
                                (dialog, which) -> list.setValue("2"),
                                (dialog, which) -> checkboxPref.setChecked(false));
                    }
                }
                return true;
            });
            list.setOnPreferenceChangeListener((preference, newValue) -> {
                if (newValue instanceof String) {
                    if (!newValue.equals("2"))
                        checkboxPref.setChecked(false);
                }
                return true;
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }
}