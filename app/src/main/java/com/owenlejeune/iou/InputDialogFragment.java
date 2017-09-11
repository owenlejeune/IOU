package com.owenlejeune.iou;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.net.Uri;
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
import android.widget.Toast;

import java.util.Date;

/**
 * Created by owenlejeune on 2017-04-11.
 */

public class InputDialogFragment extends DialogFragment {

    private static InputDialogListenerInterface listener;
    private ImageButton cpicker;
    private EditText contactText;
    private EditText amountText;
    private EditText noteText;
    private DatePicker dueDatePicker;

    private String number;
    private Bitmap photo;
    private String name;
    private Uri contactUri;
    private String amount;
    private String note;
    private boolean type;
    private Date dueDate;

    private View view;

    private static Object[] fields;
    private static boolean firstStart = true;
    private Spinner typesList;
    private ArrayAdapter<CharSequence> adapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        view = inflater.inflate(R.layout.input_dialog_layout, null);

        builder.setView(view);

        cpicker = (ImageButton)view.findViewById(R.id.contact_picker_imageButton);
        cpicker.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_circle_white_24dp, null));

        contactText = (EditText)view.findViewById(R.id.contact_text);

        if(getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED){
            //passes this to ContactActivity to make data updating easier (see later methods)
            ContactActivity.setParent(this);

            cpicker.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(view.getContext(), ContactActivity.class);
                    startActivityForResult(i, 0);
                }
            });

            //if(firstStart){
                contactText.setHint("<-- Press that button to add a contact!");
                firstStart = false;
            //}

        }

        dueDatePicker = (DatePicker)view.findViewById(R.id.due_date_picker);

        amountText = (EditText)view.findViewById(R.id.dialogAmount);
        noteText = (EditText)view.findViewById(R.id.dialogNote);

        typesList = (Spinner)view.findViewById(R.id.type_menu);
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.types, android.R.layout.simple_spinner_dropdown_item);
        typesList.setAdapter(adapter);

        typesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 0){
                    type = true;
                }else{
                    type = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("Spinner", "Nothing selected");
            }
        });

        builder.setNegativeButton("CANCEL", null);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                amount = amountText.getText().toString();
                note = noteText.getText().toString();
                name = contactText.getText().toString();
                dueDate = getDateFromDatePicker(dueDatePicker);

                //ensures all fields are filled out (accounts for lack of contact permissions)
                //TODO - prevent dialog from closing if all fields aren't filled

                if (!amount.isEmpty() && !note.isEmpty() && !name.isEmpty()) {
                    listener.onDialogPositiveClick(InputDialogFragment.this);
                }else{
                    Toast.makeText(getContext(), "IOU can only be created when all fields are full!", Toast.LENGTH_SHORT).show();
                }    
            }
        });
        return builder.create();
    }

    public void update(){

        //sets all values to current information from ContactActivity

        number = (String)fields[0];
        photo = (Bitmap)fields[1];
        name = (String)fields[2];
        contactUri = (Uri)fields[3];

        cpicker.setImageBitmap(photo);

        contactText.setText(name);
    }

    //method made static so it can be called from ContactActivity
    public static void setFields(Object[] input, InputDialogFragment parent){
        //gets contact information passed from ContactActivity and performs an update
        fields = input;
        parent.update();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        try{
            listener = (InputDialogListenerInterface) activity;
        }catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement InputDialogListenerInterface");
        }
    }

    private Date getDateFromDatePicker(DatePicker datePicker){
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    public String getNumber() {
        return number;
    }

    public Bitmap getPhoto() {
        return photo;
    }

    public String getName() {
        return name;
    }

    public Uri getContactUri() {
        return contactUri;
    }

    public Float getAmount() {
        return Float.parseFloat(amount);
    }

    public String getNote() {
        return note;
    }

    public boolean getType() {
        return type;
    }

    public Date getDueDate() {
        return dueDate;
    }
}
