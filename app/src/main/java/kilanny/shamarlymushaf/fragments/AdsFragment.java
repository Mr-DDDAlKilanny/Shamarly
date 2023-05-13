package kilanny.shamarlymushaf.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.adapters.AdsSliderAdapter;
import kilanny.shamarlymushaf.util.Utils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AdsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdsFragment extends DialogFragment {

    private AdsSliderAdapter mAdsSliderAdapter;
    private boolean mPrayerAlarm;

    public AdsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AdsFragment.
     */
    public static AdsFragment newInstance(boolean prayerAlarm) {
        AdsFragment fragment = new AdsFragment();
        Bundle args = new Bundle();
        args.putBoolean("prayerAlarm", prayerAlarm);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPrayerAlarm = getArguments().getBoolean("prayerAlarm");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ads, container, false);

        TextView txtTitle = view.findViewById(R.id.txtTitle);
        txtTitle.setText(mPrayerAlarm ? R.string.prayer_alarm : R.string.auto_caller);
        TextView txtDetails = view.findViewById(R.id.txtDetails);
        txtDetails.setText(mPrayerAlarm ? R.string.ad_text : R.string.ad2_text);

        SliderView sliderView = view.findViewById(R.id.imageSlider);
        sliderView.setSliderAdapter(mAdsSliderAdapter = new AdsSliderAdapter(mPrayerAlarm));
        sliderView.startAutoCycle();
        sliderView.setIndicatorAnimation(IndicatorAnimationType.WORM);
        sliderView.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION);
        sliderView.setAutoCycleDirection(SliderView.AUTO_CYCLE_DIRECTION_RIGHT);
        sliderView.setIndicatorSelectedColor(Color.WHITE);
        sliderView.setIndicatorUnselectedColor(Color.GRAY);
        sliderView.setScrollTimeInSec(4); //set scroll delay in seconds :
        view.findViewById(R.id.btnOk).setOnClickListener(v -> {
            if (mPrayerAlarm) {
                String appPackageName = "kilanny.muslimalarm";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            } else {
                Utils.openUrlInChromeOrDefault(v.getContext().getApplicationContext(),
                        "https://sites.google.com/view/auto-caller/home");
            }
            getDialog().dismiss();
        });
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> getDialog().dismiss());
        return view;
    }

    @Override
    public void onDestroy() {
        if (mAdsSliderAdapter != null)
            mAdsSliderAdapter.destroy();
        super.onDestroy();
    }
}
