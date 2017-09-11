package com.owenlejeune.iou;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by owenlejeune on 2017-04-11.
 */

public class IOUArrayAdapter extends ArrayAdapter {
    private final Context context;
    private final ArrayList<IOU> values;

    public IOUArrayAdapter(Context context, ArrayList<IOU> values){
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        //sets the custom row layout
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.listview_item_row, parent, false);

        IOU current = values.get(position);

        ImageView imageView = (ImageView)rowView.findViewById(R.id.contactImage);
        imageView.setImageDrawable(rowView.getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));

        //if the app has contact permissions, set the image view to the contact photo
        if(getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED){
            if(current.getContactPhoto() != null) {
                imageView.setImageBitmap(current.getContactPhoto());
            }
        }

        TextView textAmountView = (TextView)rowView.findViewById(R.id.rowIOUAmount);
        TextView textNoteView = (TextView)rowView.findViewById(R.id.rowIOUNote);
        TextView textContact = (TextView)rowView.findViewById(R.id.contact_name);
        TextView textDueDate = (TextView)rowView.findViewById(R.id.due_date);

        //format the IOU amount to a dollar figure
        if(current.getAmount() != 0){
            textAmountView.setText(current.getFloatString());
        }
        textNoteView.setText(current.getNote());

        //set the text colour for the amount (green for credit, red for debit)
        if(current.isType()){
            textAmountView.setTextColor(rowView.getResources().getColor(R.color.green, getDropDownViewTheme()));
        }else{
            textAmountView.setTextColor(rowView.getResources().getColor(R.color.red, getDropDownViewTheme()));
        }

        textContact.setText(current.getContactName());

        if(current.getDueDate() != null){
            String[] date = current.getDueDate().toString().split(" ");

            textDueDate.setText(date[0] + " " + date[1] + " " + date[2]);

            if(current.getDueDate().before(new Date())){
                textDueDate.setTextColor(rowView.getResources().getColor(R.color.red, getDropDownViewTheme()));
                textDueDate.setShadowLayer(1.6f, 1.5f, 1.3f, R.color.red);
            }
        }

        return rowView;
    }


}
