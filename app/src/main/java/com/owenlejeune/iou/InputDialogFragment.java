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

public class InputDialogFragment extends DialogFragment{

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
    private IOU iou;

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
            ContactActivity.setParent(this);
            cpicker.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    Intent i = new Intent(view.getContext(), ContactActivity.class);
                    startActivityForResult(i, 0);
                }
            });
            contactText.setHint("<-- Press that button to add a contact!");
            firstStart = false;
        }

        dueDatePicker = (DatePicker)view.findViewById(R.id.due_date_picker);

        amountText = (EditText)view.findViewById(R.id.dialogAmount);
        noteText = (EditText)view.findViewById(R.id.dialogNote);

        typesList = (Spinner)view.findViewById(R.id.type_menu);
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.types, android.R.layout.simple_spinner_dropdown_item);
        typesList.setAdapter(adapter);
        typesList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
                type = (i == 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView){
                Log.i("Spinner", "Nothing selected");
            }
          });

        if(iou != null){
            cpicker.setImageBitmap(iou.getContactPhoto());
            contactText.setText(iou.getContactName());
            amountText.setText("" + iou.getAmount());
            noteText.setText(iou.getNote());
            int s = (iou.isType()) ? 0 : 1;
            typesList.setSelection(s);
            Calendar cal = Calendar.getInstance();
            cal.setTime(iou.getDueDate());
            dueDatePicker.init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), null);
        }

        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i){
                if(iou != null) listener.addIOU(iou);
            }
        })
        .setPositiveButton("CREATE", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                amount = amountText.getText().toString();
                note = noteText.getText().toString();
                name = contactText.getText().toString();
                dueDate = getDateFromPicker(dueDatePicker);


                if (!(amount.isEmpty() || note.isEmpty() || name.isEmpty())){
                    listener.onDialogPositiveClick(InputDialogFragment.this);
                } else {
                    Toast.makeText(getContext(), "IOUs can only be created when all fields are full!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        try{
            listener = (InputDialogListenerInterface)activity;
        }catch (ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement InputDialogListenerInterface");
        }
    }

    private Date getDateFromPicker(DatePicker datePicker){
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);

        return calendar.getTime();
    }

    public void setEditFields(IOU iou){
        this.iou = iou;
    }

    public static void setFields(Object[] input, InputDialogFragment parent){
        fields = input;
        parent.update();
    }

    public void update(){
        number = (String)fields[0];
        photo = (Bitmap)fields[1];
        name = (String)fields[2];
        contactUri = (Uri)fields[3];

        cpicker.setImageBitmap(photo);
        contactText.setText(name);
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
