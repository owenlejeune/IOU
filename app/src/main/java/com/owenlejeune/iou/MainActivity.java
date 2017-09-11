package com.owenlejeune.iou;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements InputDialogListenerInterface {

    private ArrayList<IOU> credits;
    private ArrayList<IOU> debits;
    private ArrayList<IOU> all;
    private ArrayList<IOU> currentData;
    private IOUArrayAdapter listAdapter;

    private String iouFile = "cache.dat";
    private int intSortBy;
    private int allCreditsDebitsSwitch;

    private final int MY_PERMISSION_REQUEST_ACCESS_CONTACTS = 0;
    private final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private final String EMAIL_ADDRESS = "omlejeune@gmail.com";
    private final String POLICY_ADDRESS = "http://owenlejeuneapps.blogspot.ca/p/iou-app-privacy-policy.html";

    private boolean firstStart = true;

    private ListView mainList;
    //private IOU[] currentData;
    private IOU startupIOU = new IOU();

    private FloatingActionButton fab;
    private Spinner sortBy;
    private Spinner sortDateBy;
    private ArrayAdapter<CharSequence> spinnerAdapter;

    private Activity thisActivity;

    private TelephonyManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //request contact permissions from the user
        boolean needsPermissions = checkAndRequestPermissions();

        manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        thisActivity = this;
        credits = new ArrayList<>();
        debits = new ArrayList<>();
        all = new ArrayList<>();
        currentData = new ArrayList<>();
        mainList = (ListView)findViewById(R.id.mainListView);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        sortBy = (Spinner)findViewById(R.id.sort_spinner);
        spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.sort_settings, android.R.layout.simple_spinner_dropdown_item);
        sortBy.setAdapter(spinnerAdapter);
        sortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                allCreditsDebitsSwitch = i;
                //update();
                updateCurrentData(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("Spinner", "No sort selected");
            }
        });

        sortDateBy = (Spinner)findViewById(R.id.date_sort_spinner);
        spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.date_sort, android.R.layout.simple_spinner_dropdown_item);
        sortDateBy.setAdapter(spinnerAdapter);
        sortDateBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!all.contains(startupIOU)){
                    intSortBy = i;
//                update();
                    sort();
                }else{
                    sortDateBy.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i("Spinner", "No date sort selected");
            }
        });

        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, int i, long l) {
                final IOU iou = (IOU)adapterView.getItemAtPosition(i);

                if (iou != startupIOU) {
                    //creates action dialog for the tapped IOU
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("What would you like to do?");

                    builder.setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteIOU(iou);
                        }
                    });

                    builder.setNeutralButton("EDIT", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            EditDialogFragment dialog = new EditDialogFragment();
                            dialog.setToEdit(iou);
                            dialog.show(getFragmentManager(), "Edit IOU");
                        }
                    });

                    //only allows a sms reminder to be sent if the device is a phone (not tablet), the app has sms permissions, and the IOU is a credit (not a debit)
                    if(manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE || /*checkCallingOrSelfPermission("android.permission.SEND_SMS") == PackageManager.PERMISSION_DENIED ||*/ !iou.isType() || iou.getContactNumber() == null) {
                        builder.setPositiveButton("CANCEL", null);
                    }else{
                        builder.setPositiveButton("SEND SMS", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendSMS(iou);
                            }
                        });
                    }

                    builder.show();
                }else{
//                    fab.performClick();
                    Toast.makeText(getApplicationContext(), "Try adding a new IOU over there! -->", Toast.LENGTH_LONG).show();
                }
            }
        });

        //creates the floating action button
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final InputDialogFragment dialogFragment = new InputDialogFragment();
                dialogFragment.show(getFragmentManager(), "New IOU");
                all.remove(startupIOU);
                //makeSubLists();
            }
        });

        if(needsPermissions){
            //reads in previous data
            read(iouFile, all);

            updateCurrentData(0);

            update();
        }
    }

    private boolean checkAndRequestPermissions(){
        //int permissionsSendSMS = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int contactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if(contactPermission != PackageManager.PERMISSION_GRANTED){
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode){
            case MY_PERMISSION_REQUEST_ACCESS_CONTACTS: {
                if(firstStart){
                    all.add(startupIOU);
                    showStartupAlert();
                    update();
                }
                return;
            }
        }
    }

    public void showStartupAlert(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Looks like there's nothing here! Hit the 'plus' button to create your first IOU.");
        alert.setPositiveButton("SHOW ME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fab.performClick();
            }
        });
        alert.setNegativeButton("ILL ADD MY OWN LATER", null);
        alert.show();
        firstStart = false;
    }

    public void update(){
        //runs on UI thread so UI is properly updated
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //makes debit/credit lists from the all list
                //makeSubLists();

                //if there are no IOUs and it is the first time the user opens the application, display an alert prompting them to create an IOU
                if(all.isEmpty()){
                    sortBy.setSelection(0);
                    all.add(startupIOU);
                    updateCurrentData(0);
                }else if(all.contains(startupIOU)){
                    sortBy.setSelection(0);
                }

                //reassigns the currentData value with the correct list
//                updateCurrentData(sortBy.getSelectedItemPosition());

                //contextText.setText(headerText);

                listAdapter = new IOUArrayAdapter(thisActivity, currentData);
                mainList.setAdapter(listAdapter);
            }
        });

        //writes the current data to file
        write(iouFile, all);

        if(all.contains(startupIOU)){
            ArrayList<IOU> empty = new ArrayList<>();
            updateWidget(empty);
        }else{
            updateWidget(all);
        }

//        ListProvider.setWidgetData((ArrayList<IOU>) all.clone());
//
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
//        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), MyAppWidgetProvider.class));
//        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidget_listview);

    }

    private void updateWidget(ArrayList<IOU> data){
        ListProvider.setWidgetData((ArrayList<IOU>) data.clone());

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), MyAppWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.appwidget_listview);
    }

    public void makeSubLists(){

        credits.clear();
        debits.clear();

        for(IOU i : all){
            if(i.isType()){
                credits.add(i);
            }else if(!i.isType()){
                debits.add(i);
            }
        }
    }

    public void updateCurrentData(int i){
        makeSubLists();
        if (i == 0) {
            setCurrentData(all);
        }else if(i == 1){
            setCurrentData(credits);
        }else{
            setCurrentData(debits);
        }
        update();
    }


    public void setCurrentData(ArrayList<IOU> data){
        currentData.clear();
        currentData.addAll(data);
    }

    public void sort(){
        switch (intSortBy){
            case 0:
                switch (allCreditsDebitsSwitch){
                    case 0:
                        setCurrentData(all);
                        break;
                    case 1:
                        setCurrentData(credits);
                        break;
                    case 2:
                        setCurrentData(debits);
                        break;
                }
                break;
            case 1:
                sortByDateAscending();
                break;
            case 2:
                sortByDateDescending();
                break;
        }
        update();
    }

    public void sortByDateAscending(){
        int finalSize = currentData.size();
        ArrayList<IOU> tempArray = new ArrayList<>();
        ArrayList<IOU> data = new ArrayList<>();
        data.addAll(currentData);

        while (tempArray.size() < finalSize){
            IOU temp = data.get(0);
            for(IOU i : data){
                if(i.getDueDate().before(temp.getDueDate())){
                    temp = i;
                }
            }
            Log.i("Data sort", temp.getDueDate().toString());
            data.remove(temp);
            tempArray.add(temp);
        }

        setCurrentData(tempArray);
    }

    public void sortByDateDescending(){
        int finalSize = currentData.size();
        ArrayList<IOU> tempArray = new ArrayList<>();
        ArrayList<IOU> data = new ArrayList<>();
        data.addAll(currentData);

        while (tempArray.size() < finalSize){
            IOU temp = data.get(0);
            for(IOU i : data){
                if(i.getDueDate().after(temp.getDueDate())){
                    temp = i;
                }
            }
            Log.i("Data sort", temp.getDueDate().toString());
            data.remove(temp);
            tempArray.add(temp);
        }

        setCurrentData(tempArray);
    }

    public void write(String text, ArrayList<IOU> data) {

        try{
            FileOutputStream out = openFileOutput(text, Context.MODE_PRIVATE);
            DataOutputStream dataOut = new DataOutputStream(out);

            for(IOU i : data){
                try{
                    i.writeTo(dataOut, this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            dataOut.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void read(String text, ArrayList<IOU> data){

        try{
            FileInputStream in = openFileInput(text);
            DataInputStream dataIn = new DataInputStream(in);

            while (in.available() > 0){
                data.add(IOU.readFrom(dataIn));
            }
            dataIn.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){

        int id = item.getItemId();

        if(id == R.id.contact_me){
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", EMAIL_ADDRESS, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "IOU Feedback");
            startActivity(emailIntent);
        }else if(id == R.id.privacy_policy){
            Intent policyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(POLICY_ADDRESS));
            startActivity(policyIntent);
        }else if(id == R.id.rate_review){
            Uri rateUri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, rateUri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

            try{
                startActivity(goToMarket);
            }catch (ActivityNotFoundException e){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())));
            }
        }else if(id == R.id.clear_all){
            all.clear();
            makeSubLists();
            sortBy.setSelection(0);
            sortDateBy.setSelection(0);
            setCurrentData(all);
            update();
        }

        return true;
    }

    @Override
    public void onDialogPositiveClick(InputDialogFragment fragment){

        //create new IOU from fragment data
        String name = fragment.getName();
        String number = fragment.getNumber();
        Bitmap photo = fragment.getPhoto();
        Uri contactUri = fragment.getContactUri();
        Float amount = fragment.getAmount();
        String note = fragment.getNote();
        boolean type;

        if(fragment.getType()){
            type = true;
        }else{
            type = false;
        }

        Date dueDate = fragment.getDueDate();

        all.add(new IOU(contactUri, number, name, photo, amount, type, note, dueDate));

//        makeSubLists();
        //update();
        updateCurrentDataOnCreateNewIOU();

    }

    private void updateCurrentDataOnCreateNewIOU(){

        makeSubLists();

        switch (sortBy.getSelectedItemPosition()){
            case 0:
                setCurrentData(all);
                break;
            case 1:
                setCurrentData(credits);
                break;
            case 2:
                setCurrentData(debits);
                break;
        }

        sort();

        update();
    }

    public void sendSMS(IOU iou){

        String message = "Hey " + iou.getContactName() + "! Just wanted to remind you that you owe me " + iou.getFloatString() + " for " + iou.getNote() + ". Thanks!\nSent from IOU";

        try{
            //launch an sms compose intent
            Intent sms = new Intent(Intent.ACTION_VIEW);

            sms.setData(Uri.parse("smsto:" + iou.getContactNumber().replace("-", "")));
            sms.putExtra("sms_body"  , message);

            startActivity(sms);

        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void deleteIOU(IOU iou){
        all.remove(iou);
        makeSubLists();
        //update();
        sort();
    }


    public void editIOU(IOU iou, String amount, String note, String name, String type, Date dueDate){
        boolean oldType = iou.isType();
        iou.setAmount(Float.parseFloat(amount));
        iou.setContactName(name);
        iou.setNote(note);
        iou.setTypeByString(type);
        iou.setDueDate(dueDate);

        if(oldType != iou.isType()){
            int index;
            if(oldType){
                index = credits.indexOf(iou);
                credits.remove(iou);
                debits.add(index, iou);
            }else{
                index = debits.indexOf(iou);
                debits.remove(iou);
                credits.add(index, iou);
            }
        }
        sort();
        //update();
    }
}