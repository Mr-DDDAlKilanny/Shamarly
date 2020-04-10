package kilanny.shamarlymushaf.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.activities.MainActivity;
import kilanny.shamarlymushaf.data.DbManager;
import kilanny.shamarlymushaf.data.Setting;
import kilanny.shamarlymushaf.data.TafsserViewModel;
import kilanny.shamarlymushaf.util.AnalyticsTrackers;
import kilanny.shamarlymushaf.util.Utils;

/**
 * Implementation of kilanny.shamarlymushaf.App Widget functionality.
 * TODO: BUG: Multiple widgets refresh only the first one
 */
public class TafseerAppWidget extends AppWidgetProvider {

    private static final String LAST_VIEW_MODEL_FILENAME = "LAST_VIEW_MODEL";
    private static final String RELOAD_CLICKED = "kilanny.shamarlymushaf.widgets.TafseerAppWidget#btnReload";
    private static final String SHOW_AYAH_CLICKED = "kilanny.shamarlymushaf.widgets.TafseerAppWidget#btnShowAyah";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, Class<?> cls) {

        TafsserViewModel viewModel = DbManager.getInstance(context).getRandomTafseer();
        saveViewModel(context, appWidgetId, viewModel);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.tafseer_app_widget);
        views.setTextViewText(R.id.txtSurahAyah, String.format(Locale.getDefault(),
                "سورة %s: %d", viewModel.surahName, viewModel.ayah));
        Intent intent = new Intent(context, WidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widgetListTexts, intent);
        views.setOnClickPendingIntent(R.id.btnReload,
                getPendingSelfIntent(context, RELOAD_CLICKED, appWidgetId, cls));
        views.setOnClickPendingIntent(R.id.btnShowAyah,
                getPendingSelfIntent(context, SHOW_AYAH_CLICKED, appWidgetId, cls));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetListTexts);
        AnalyticsTrackers.getInstance(context).sendWidgetRefresh(viewModel.sura, viewModel.ayah);
    }

    private static PendingIntent getPendingSelfIntent(Context context, String action,
                                                      int appWidgetId, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static void saveViewModel(Context context, int appWidgetId, TafsserViewModel viewModel) {
        try {
            FileOutputStream outputStream = context.openFileOutput(LAST_VIEW_MODEL_FILENAME + appWidgetId, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(viewModel);
            objectOutputStream.close();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static TafsserViewModel getLastViewModel(Context context, int appWidgetId) {
        try {
            FileInputStream inputStream = context.openFileInput(LAST_VIEW_MODEL_FILENAME + appWidgetId);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            TafsserViewModel viewModel = (TafsserViewModel) objectInputStream.readObject();
            objectInputStream.close();
            inputStream.close();
            return viewModel;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, getClass());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (RELOAD_CLICKED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            updateAppWidget(context, appWidgetManager, appWidgetId, getClass());
        } else if (SHOW_AYAH_CLICKED.equals(intent.getAction())) {
            if (Utils.canWarmStart(context)) {
                int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                TafsserViewModel lastViewModel = getLastViewModel(context, appWidgetId);
                if (lastViewModel == null)
                    return;
                Setting setting = Setting.getInstance(context);
                intent = new Intent(context, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(MainActivity.SHOW_PAGE_MESSAGE,
                        lastViewModel.page / (setting.lastWasDualPage ? 2 : 1));
                intent.putExtra(MainActivity.SHOW_AYAH_MESSAGE, String.format(Locale.ENGLISH, "%d,%d",
                        lastViewModel.sura, lastViewModel.ayah));
                context.startActivity(intent);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        AnalyticsTrackers.getInstance(context).sendWidgetEnabled(true);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        AnalyticsTrackers.getInstance(context).sendWidgetEnabled(false);
    }
}

