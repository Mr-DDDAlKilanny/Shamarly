package kilanny.shamarlymushaf.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.arch.core.util.Function;
import androidx.core.view.ViewCompat;

import com.google.android.material.internal.ViewUtils;

import java.text.SimpleDateFormat;
import java.util.Locale;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.alarms.Alarm;
import kilanny.shamarlymushaf.data.alarms.AlarmDao;
import kilanny.shamarlymushaf.data.alarms.AlarmDb;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.AppExecutors;
import kilanny.shamarlymushaf.util.Utils;

public class AlarmListAdapter extends ArrayAdapter<Alarm>
        implements PopupMenu.OnMenuItemClickListener {

    private final SimpleDateFormat timeF;
    private final Function<Alarm, Void> onAlarmEdit;
    private final Handler mHandler;
    private boolean mIsPendingOperation = false;
    private Alarm mClickedAlarm;

    public AlarmListAdapter(@NonNull Context context, @NonNull Function<Alarm, Void> onAlarmEdit) {
        super(context, R.layout.item_alarm);
        this.onAlarmEdit = onAlarmEdit;
        timeF = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        mHandler = new Handler(context.getMainLooper());
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View rowView;
        final Alarm alarm = getItem(position);
        if (convertView == null) {
            rowView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.item_alarm, parent, false);
        } else
            rowView = convertView;

        final Context context = rowView.getContext();

        rowView.setOnClickListener(view -> {
            if (!mIsPendingOperation)
                onAlarmEdit.apply(alarm);
        });
        final AppCompatTextView alarmTime = rowView.findViewById(R.id.alarmTime);
        if (alarm.enabled) {
            Utils.NextAlarmInfo info = Utils.getNextAlarmDate(alarm);
            alarmTime.setText(String.format("%s", timeF.format(info.date)));
        } else {
            alarmTime.setText(null);
        }

        AppCompatCheckBox chkAlarmEnabled = rowView.findViewById(R.id.chkAlarmEnabled);
        chkAlarmEnabled.setChecked(alarm.enabled);
        chkAlarmEnabled.setOnCheckedChangeListener((compoundButton, b) -> {
            compoundButton.setEnabled(false);
            AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                // if any other operations running wait them first
                while (mIsPendingOperation) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mIsPendingOperation = true;
                alarm.enabled = b;
                AlarmDao alarmDao = AlarmDb.getInstance(getContext()).alarmDao();
                alarmDao.update(alarm);
                Alarm[] alarms = alarmDao.getAll();
                mHandler.post(() -> {
                    mIsPendingOperation = false;
                    compoundButton.setEnabled(true);
                    clear();
                    addAll(alarms);
                    notifyDataSetChanged();
                    Utils.scheduleAndDeletePrevious(getContext(), alarms);
                    Toast.makeText(getContext(), b ? "تم تفعيل التنبيه" : "تم تعطيل التنبيه",
                            Toast.LENGTH_LONG).show();
                });
            });
        });

        AppCompatTextView alarmDays = rowView.findViewById(R.id.alarmDays);
        alarmDays.setText(Utils.getAlarmDays(context, alarm));

        final AppCompatImageButton imgDots = rowView.findViewById(R.id.imgDots);
        imgDots.setOnClickListener(view -> {
            if (mIsPendingOperation) return;
            mClickedAlarm = alarm;
            PopupMenu popupMenu = new PopupMenu(context, imgDots);
            popupMenu.inflate(R.menu.menu_list_item_alarm);
            MenuBuilder menu = (MenuBuilder) popupMenu.getMenu();
            menu.setOptionalIconsVisible(true);
            for (MenuItem item : menu.getVisibleItems()) {
                int iconMarginPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 8, getContext().getResources().getDisplayMetrics());

                if (item.getIcon() != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0));
                    } else {
                        item.setIcon(new InsetDrawable(item.getIcon(), iconMarginPx, 0, iconMarginPx, 0) {
                            @Override
                            public int getIntrinsicWidth() {
                                return getIntrinsicHeight() + iconMarginPx + iconMarginPx;
                            }
                        });
                    }
                }
            }
            popupMenu.setOnMenuItemClickListener(this);
            MenuPopupHelper menuHelper = new MenuPopupHelper(getContext(), menu, imgDots);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        });

        float elevation = ViewUtils.dpToPx(getContext(), 16);
        ViewCompat.setElevation(rowView, elevation);
        return rowView;
    }

    private void onDeleteAlarm() {
        new AlertDialog.Builder(getContext())
                .setTitle("حذف تنبيه الورد")
                .setMessage("متأكد من حذف تنبيه الورد ؟")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    mIsPendingOperation = true;
                    AppExecutors.getInstance().executeOnCachedExecutor(() -> {
                        AlarmDao alarmDao = AlarmDb.getInstance(getContext()).alarmDao();
                        alarmDao.delete(mClickedAlarm);
                        AnalyticsTrackers.getInstance(getContext()).logAlarmDeleted(mClickedAlarm);
                        Alarm[] alarms = alarmDao.getAll();
                        mHandler.post(() -> {
                            remove(mClickedAlarm);
                            notifyDataSetChanged();
                            Utils.scheduleAndDeletePrevious(getContext(), alarms);
                            mIsPendingOperation = false;
                        });
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuDeleteAlarm:
                onDeleteAlarm();
                break;
        }
        return false;
    }
}
