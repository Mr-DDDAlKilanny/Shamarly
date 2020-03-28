package kilanny.shamarlymushaf.fragments;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.Step;

public class StepFragment extends StepView {

    private TextView title;
    private TextView content;
    private TextView summary;
    private ImageView imageView;
    private View layout;


    public static StepFragment createFragment(Step step) {
        StepFragment fragment = new StepFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable("step", step);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        step = getArguments().getParcelable("step");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layout = step.getViewType() > 0 ? step.getViewType() : R.layout.fragment_step;

        View view = inflater.inflate(layout, container, false);

        initViews(view);
        initData();

        return view;
    }

    private void initData() {
        if (title != null) {
            title.setText(step.getTitle());
        }
        if (content != null) {
            content.setText(step.getContent());
        }
        if (summary != null) {
            summary.setText(step.getSummary());
        }
        if (imageView != null) {
            imageView.setImageResource(step.getDrawable());
        }

        if (layout != null) {
            layout.setBackgroundColor(step.getBackgroundColor());
        }
    }

    private void initViews(View view) {
        title = view.findViewById(R.id.title);
        content = view.findViewById(R.id.content);
        summary = view.findViewById(R.id.summary);
        imageView = view.findViewById(R.id.image);
        layout = view.findViewById(R.id.container);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (imageView != null) {
            imageView.setImageDrawable(null);
        }
    }
}