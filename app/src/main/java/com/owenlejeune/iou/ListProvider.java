package com.owenlejeune.iou;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by owenlejeune on 2017-08-03.
 */

public class ListProvider implements RemoteViewsService.RemoteViewsFactory{
    private static ArrayList<IOU> listItemList = new ArrayList<>();
    private Context context = null;
    private int appWidgetId;

    public ListProvider(Context context, Intent intent){
        this.context = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        //populateListItems();
    }

//    private void populateListItems(){
//
//    }

    @Override
    public int getCount(){
        return listItemList.size();
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public RemoteViews getViewAt(int position){
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.listview_item_row);
        IOU iou = listItemList.get(position);
        remoteViews.setImageViewBitmap(R.id.contactImage, iou.getContactPhoto());
        remoteViews.setTextViewText(R.id.contact_name, iou.getContactName());
        remoteViews.setTextViewText(R.id.rowIOUAmount, iou.getFloatString());

        if(iou.isType()){
            remoteViews.setTextColor(R.id.rowIOUAmount, context.getResources().getColor(R.color.green));
        }else{
            remoteViews.setTextColor(R.id.rowIOUAmount, context.getResources().getColor(R.color.red));
        }

        remoteViews.setTextViewText(R.id.rowIOUNote, iou.getNote());
        String[] date = iou.getDueDate().toString().split(" ");
        remoteViews.setTextViewText(R.id.due_date, date[0] + " " + date[1] + " " + date[2]);

        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView(){
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
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


    public static void setWidgetData(ArrayList<IOU> data){
        listItemList.clear();
        listItemList = data;
    }
}
