package com.owenlejeune.iou;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import java.util.Date;

/**
 * Created by owenlejeune on 2017-04-16.
 */

public class EditDialogFragment extends DialogFragment {

    private static MainActivity listener;
    private ImageButton cpicker;
    private EditText contactText;
    private EditText amountText;
    private EditText noteText;
    private DatePicker dueDatePicker;

    private String name;
    private String amount;
    private String note;
    private String type;
    private Date dueDate;

    private View view;

    private Spinner typesList;
    private ArrayAdapter<CharSequence> adapter;

    private static IOU toEdit;

    public Dialog onCreateDialog(Bundle savedInstanceState){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.input_dialog_layout, null);

        builder.setView(view);

        cpicker = (ImageButton)view.findViewById(R.id.contact_picker_imageButton);
        cpicker.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));

        contactText = (EditText)view.findViewById(R.id.contact_text);
        amountText = (EditText)view.findViewById(R.id.dialogAmount);
        noteText = (EditText)view.findViewById(R.id.dialogNote);

        typesList = (Spinner)view.findViewById(R.id.type_menu);
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.types, android.R.layout.simple_spinner_dropdown_item);
        typesList.setAdapter(adapter);

        dueDatePicker = (DatePicker)view.findViewById(R.id.due_date_picker);

        typesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                type = typesList.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("Spinner", "Nothing selected");
            }
        });

        builder.setNegativeButton("CANCEL", null);

        builder.setPositiveButton("FINISH", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                amount = amountText.getText().toString();
                note = noteText.getText().toString();
                name = contactText.getText().toString();
                dueDate = getDateFromDatePicker(dueDatePicker);

                if (amount != null && note != null && name != null && type != null) {
                    listener.editIOU(toEdit, amount, note, name, type, dueDate);
                }

            }
        });

        updateFields();

        return builder.create();
    }

    public void updateFields(){
        if(toEdit != null){
            if(toEdit.getContactPhoto() != null){
                cpicker.setImageBitmap(toEdit.getContactPhoto());
            }
            contactText.setText(toEdit.getContactName());
            amountText.setText(toEdit.getAmount() + "");
            noteText.setText(toEdit.getNote());
            if(toEdit.isType()){
                typesList.setSelection(0);
            }else{
                typesList.setSelection(1);
            }
        }
    }


    public static void setToEdit(IOU iou){
        toEdit = iou;
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        try{
            listener = (MainActivity) activity;
        }catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement InputDialogListenerInterface");
        }
    }

    private Date getDateFromDatePicker(DatePicker datePicker){
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();


        return new Date(year, month, day);
    }
}
