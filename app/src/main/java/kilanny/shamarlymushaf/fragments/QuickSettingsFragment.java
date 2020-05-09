package kilanny.shamarlymushaf.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceFragmentCompat;

import kilanny.shamarlymushaf.R;

@Keep
public class QuickSettingsFragment extends PreferenceFragmentCompat {

    private Callbacks mCallbacks;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.prefs_quick, rootKey);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
//        if (!(context instanceof Callbacks))
//            throw new IllegalArgumentException();
//        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public interface Callbacks {
        void onSettingChanged(String keyName, Object newValue);
    }
}
