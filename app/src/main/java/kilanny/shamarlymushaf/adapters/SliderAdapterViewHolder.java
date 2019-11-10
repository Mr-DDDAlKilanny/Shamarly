package kilanny.shamarlymushaf.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.smarteist.autoimageslider.SliderViewAdapter;

import kilanny.shamarlymushaf.R;

class SliderAdapterViewHolder extends SliderViewAdapter.ViewHolder {

    View itemView;
    ImageView imageViewBackground;
    TextView textViewDescription;

    public SliderAdapterViewHolder(View itemView) {
        super(itemView);
        imageViewBackground = itemView.findViewById(R.id.iv_auto_image_slider);
        textViewDescription = itemView.findViewById(R.id.tv_auto_image_slider);
        this.itemView = itemView;
    }
}
