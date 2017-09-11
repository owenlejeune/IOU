package com.owenlejeune.iou;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by owenlejeune on 2017-05-25.
 */

public class MyAppWidgetProvider extends AppWidgetProvider {

    public static final String REFRESH_ACTION = "com.owenlejeune.REFRESH_ACTION";

    @Override
    public void onReceive(Context context, Intent intent){
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        if(intent.getAction().equals(REFRESH_ACTION)){
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

            this.onUpdate(context, AppWidgetManager.getInstance(context), appWidgetIds);

            //Toast.makeText(context, "onReceive()", Toast.LENGTH_SHORT).show();

        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
        final int N = appWidgetIds.length;

        for(int i = 0; i < N; i++){
            //int appWidgetId = appWidgetIds[i];

            RemoteViews remoteViews = updateWidgetListView(context, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], remoteViews);

        }

        //Toast.makeText(context, "onUpdate()", Toast.LENGTH_SHORT).show();

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private RemoteViews updateWidgetListView(Context context, int appWidgetId){
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget_layout);

        Intent svcIntent = new Intent(context, WidgetService.class);
        svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
        remoteViews.setRemoteAdapter(R.id.appwidget_listview, svcIntent);
        remoteViews.setEmptyView(R.id.appwidget_listview, R.id.empty_widget_view);

        return remoteViews;
    }


}
