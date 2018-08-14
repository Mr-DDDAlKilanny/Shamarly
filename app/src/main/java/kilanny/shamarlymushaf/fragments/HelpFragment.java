package kilanny.shamarlymushaf.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import kilanny.shamarlymushaf.R;

public class HelpFragment extends Fragment {

    private static final String ARG_POSITION = "position";
    private static final String ARG_STRING = "string";

    private int position;
    private String string;
    private Typeface typeface;
    private int fontSize;

    public static HelpFragment newInstance(int position, String string,
                                           Typeface face, int fontSize) {
        HelpFragment f = new HelpFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        b.putString(ARG_STRING, string);
        f.fontSize = fontSize;
        f.typeface = face;
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        string = getArguments().getString(ARG_STRING);
        position = getArguments().getInt(ARG_POSITION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.fragment_help,
                container, false);
        TextView v = (TextView) scrollView.findViewById(R.id.helpText);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(typeface);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        v.setText(string);
        return scrollView;
    }

}
