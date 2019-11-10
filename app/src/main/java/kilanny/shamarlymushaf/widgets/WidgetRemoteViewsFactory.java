package kilanny.shamarlymushaf.widgets;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.data.TafsserViewModel;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final int appWidgetId;

    public WidgetRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(),
                position == 0 ?
                        R.layout.tafseer_widget_ayah_textview
                        : R.layout.tafseer_widget_tafseer_textview);
        TafsserViewModel viewModel = TafseerAppWidget.getLastViewModel(context, appWidgetId);
        if (viewModel != null)
            remoteView.setTextViewText(
                    position == 0 ?
                            R.id.txtAyah
                            : R.id.txtTafseer,
                    position == 0 ?
                            viewModel.ayahText
                            : viewModel.tafseer);

        return remoteView;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
    }
}
