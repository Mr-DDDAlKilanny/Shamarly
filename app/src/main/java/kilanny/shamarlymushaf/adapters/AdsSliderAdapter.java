package kilanny.shamarlymushaf.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.arch.core.util.Function;

import com.smarteist.autoimageslider.SliderViewAdapter;

import kilanny.shamarlymushaf.R;

public class AdsSliderAdapter extends SliderViewAdapter<SliderAdapterViewHolder> {

    private static final int[] drawables = {
            R.drawable.screenshot_1572235462, R.drawable.screenshot_1572235604,
            R.drawable.screenshot_1572235497, R.drawable.screenshot_1572235517,
            R.drawable.screenshot_1572235626, R.drawable.screenshot_1572235638,
            R.drawable.screenshot_1572235645, R.drawable.screenshot_1572235491
    };

    private static final String[] texts = {
            "إضافة منبهات عديدة لصلاة الفجر و الجمعة وغيرها",
            "تحديد وقت التنبيه",
            "تحريك الهاتف حتى يتوقف المنبه عن الرن",
            "كما يمكن حل المسائل لإيقاف المنبه",
            "تحديد صوت المنبه بنغمات عالية وقوية",
            "اختيار أحد طرق إيقاف المنبه",
            "إضافة غفوة للمنبه",
            "شاشة رن المنبه"
    };

    private static final int[] drawables2 = {
            R.drawable.screenshot_1573100075, R.drawable.screenshot_1573099822,
            R.drawable.screenshot_1573099876, R.drawable.screenshot_1573099837,
            R.drawable.screenshot_1573099842, R.drawable.screenshot_1573100084,
            R.drawable.screenshot_1573099883
    };

    private static final String[] texts2 = {
            "الشاشة الرئيسية",
            "إضافة قائمة للرن، بالضغط على زر (+) في الشاشة الرئيسية",
            "قائمة الاتصال، يمكن تعديل عدد مرات الاتصال لكل شخص على حدة، ويمكن تغيير مدينته",
            "اختيار اسم لإضافته لقائمة الرن، بالضغط على زر + في قائمة الاتصال",
            "تغيير مدينة الشخص",
            "يمكن إضافة مدن جديدة من هذه الشاشة",
            "عرض السجل والمساعدة"
    };

    private final boolean mPrayerAlarm;
    private final ImageView[] imageViews;

    public AdsSliderAdapter(boolean prayerAlarm) {
        mPrayerAlarm = prayerAlarm;
        imageViews = new ImageView[getCount()];
    }

    @Override
    public SliderAdapterViewHolder onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_slider_layout_item, null);
        return new SliderAdapterViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(SliderAdapterViewHolder viewHolder, int pos) {
        Function<Integer, Void> f = (position) -> {
            viewHolder.textViewDescription.setText(mPrayerAlarm ? texts[position] : texts2[position]);
            Bitmap bitmap = decodeSampledBitmapFromResource(viewHolder.imageViewBackground.getResources(),
                    mPrayerAlarm ? drawables[position] : drawables2[position], 357, 634);
            viewHolder.imageViewBackground.setImageBitmap(bitmap);
            imageViews[position] = viewHolder.imageViewBackground;
            return null;
        };
        try {
            f.apply(pos);
        } catch (IndexOutOfBoundsException ex) { // exception in ViewPager
            ex.printStackTrace();
            f.apply(getCount() - 1 - pos);
        }
    }

    public void destroy() {
        for (int i = 0; i < imageViews.length; ++i) {
            if (imageViews[i] != null) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) imageViews[i].getDrawable();
                if (bitmapDrawable != null) {
                    try {
                        imageViews[i].setImageBitmap(null);
                        bitmapDrawable.getBitmap().recycle();
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    @Override
    public int getCount() {
        //slider view count could be dynamic size
        return mPrayerAlarm ? 8 : 7;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmapFromResource(Resources res,
                                                          int resId, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}