package kilanny.shamarlymushaf.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import kilanny.shamarlymushaf.data.Step;

public class StepView extends Fragment {

    Step step;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        step = getArguments().getParcelable("step");
    }
}
