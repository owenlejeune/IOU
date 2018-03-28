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
import android.widget.Spinner;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Date;
import java.util.List;
import java.lang.reflect.Array;
import android.widget.RemoteViews;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements InputDialogListenerInterface{

    private int compareAmount(IOU o1, IOU o2){
    return (int)(o1.getAmount() - o2.getAmount());
    }

    private int compareDate(IOU o1, IOU o2){
        if(o1.getDueDate().before(o2.getDueDate())) return 1;
        if(o1.getDueDate().after(o2.getDueDate())) return -1;
        return 0;
    }

    private Comparator<IOU> sortByAmount = new Comparator<IOU>(){
        public int compare(IOU o1, IOU o2){
            int rc = compareAmount(o1, o2);
            if(rc == 0){
                rc = compareDate(o1, o2);
                if(rc == 0){
                    rc = o1.getContactName().compareTo(o2.getContactName());
                }
            }
            return rc;
        }
    };

    private Comparator<IOU> sortByDateAscending = new Comparator<IOU>(){
        public int compare(IOU o1, IOU o2){
            int rc = compareDate(o1, o2);
            if(rc == 0){
                rc = compareAmount(o1, o2);
                if(rc == 0){
                    rc = o1.getContactName().compareTo(o2.getContactName());
                }
            }
            return rc;
        }
    };

    private Comparator<IOU> sortByDateDescending = new Comparator<IOU>(){
        public int compare(IOU o1, IOU o2){
            int rc = compareDate(o2, o1);
            if(rc == 0){
                rc = compareAmount(o1, o2);
                if(rc == 0){
                    rc = o1.getContactName().compareTo(o2.getContactName());
                }
            }
            return rc;
        }
    };

    private Comparator<IOU> currentComparator = sortByDateAscending;

    private TreeSet<IOU> credits;
    private TreeSet<IOU> debits;
    private TreeSet<IOU> all;
    private TreeSet<IOU> current;
    private IOUArrayAdapter listAdapter;

    private String iouFile = "cache.dat";

    private final int MY_PERMISSION_REQUEST_ACCESS_CONTACTS = 0;
    private final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private final String EMAIL_ADDRESS = "owenlejeuneapps@gmail.com";
    private final String POLICY_ADDRESS = "http://owenlejeuneapps.blogspot.ca/p/iou-app-privacy-policy.html";

    private boolean firstStart = true;

    private ListView mainList;
    private static IOU startupIOU = new IOU();

    private FloatingActionButton fab;
    private Spinner sortBy;
    private Spinner sortDateBy;
    private ArrayAdapter<CharSequence> spinnerAdapter;

    private Activity thisActivity;

    private TelephonyManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean permissions = checkAndRequestPermissions();

        manager = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        thisActivity = this;
        credits = new TreeSet<>(currentComparator);
        debits = new TreeSet<>(currentComparator);
        all = new TreeSet<>(currentComparator);

        mainList = (ListView)findViewById(R.id.mainListView);
        fab = (FloatingActionButton)findViewById(R.id.fab);

        sortBy = (Spinner)findViewById(R.id.sort_spinner);
        spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.sort_settings, android.R.layout.simple_spinner_dropdown_item);
        sortBy.setAdapter(spinnerAdapter);
        sortBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
                updateCurrentData(i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView){
                Log.i("Spinner", "No sort selected");
            }
        });

        sortDateBy = (Spinner)findViewById(R.id.date_sort_spinner);
        spinnerAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.date_sort, android.R.layout.simple_spinner_dropdown_item);
        sortDateBy.setAdapter(spinnerAdapter);
        sortDateBy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l){
                if(!all.contains(startupIOU)){ sort(i); }
                else{ sortDateBy.setSelection(0); }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView){
                Log.i("Spinner", "No sort selected");
            }
        });

        mainList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, int i , long l){
                final IOU iou = (IOU)adapterView.getItemAtPosition(i);

                if(iou == startupIOU){ Toast.makeText(getApplicationContext(), "Try adding a new IOU over there! -->", Toast.LENGTH_LONG).show(); return;}

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("What would you like to do?")
                  .setNegativeButton("Delete", new DialogInterface.OnClickListener(){
                      @Override
                      public void onClick(DialogInterface dialogInterface, int i){
                        deleteIOU(iou);
                      }
                  })
                  .setNeutralButton("EDIT", new DialogInterface.OnClickListener(){
                      @Override
                      public void onClick(DialogInterface dialogInterface, int i){
                        InputDialogFragment dialog = new InputDialogFragment();
                        dialog.show(getFragmentManager(), "New IOU");
                        dialog.setEditFields(iou);
                        all.remove(iou);
                        makeSubLists();
                        update();
                      }
                  });

                if(manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE || !iou.isType() || iou.getContactNumber() == null){
                    builder.setPositiveButton("CANCEL", null);
                }else{
                    builder.setPositiveButton("SEND SMS", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i){
                            sendSMS(iou);
                        }
                    });
                }
                builder.show();
            }
        });

        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                final InputDialogFragment dialogFragment = new InputDialogFragment();
                dialogFragment.show(getFragmentManager(), "New IOU");
                all.remove(startupIOU);
            }
        });

        if(permissions){
            read(iouFile, all);
            update();
        }
    }

    private boolean checkAndRequestPermissions(){
        int contactPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if(contactPermission != PackageManager.PERMISSION_GRANTED) listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        if(!listPermissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
    //switch(requestCode){
      //case MY_PERMISSION_REQUEST_ACCESS_CONTACTS:{
        if(firstStart){
          all.add(startupIOU);
          showStartupAlert();
          update();
        }
        //return
      //}
    //}
    }

    public void showStartupAlert(){
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage("Looks like there's nothing here! Hit the 'plus' button to create your first IOU.")
          .setPositiveButton("SHOW ME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fab.performClick();
            }
          })
          .setNegativeButton("ILL ADD MY OWN LATER", null)
          .show();
        firstStart = false;
    }

    public void update(){
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if(all.isEmpty()){
                    sortBy.setSelection(0);
                    all.add(startupIOU);
                    updateCurrentData(0);
                }else if(all.contains(startupIOU)) sortBy.setSelection(0);

                ArrayList<IOU> temp = new ArrayList<>();
                temp.addAll(current);
                listAdapter = new IOUArrayAdapter(thisActivity, temp);
                mainList.setAdapter(listAdapter);
            }
        });

        write(iouFile, all);

        if(all.contains(startupIOU)){
            TreeSet<IOU> empty = new TreeSet<>();
            updateWidget(empty);
        }else{
            updateWidget(all);
        }
    }

    private void updateWidget(TreeSet<IOU> data){
        ArrayList<IOU> widgetData = new ArrayList<>();
        widgetData.addAll(data);
        ListProvider.setWidgetData((ArrayList<IOU>)widgetData.clone());
        //widgetData = null;

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
            }else{
                debits.add(i);
            }
        }
    }

    public void updateCurrentData(int i){
        makeSubLists();
        if(i == 0){
            current = all;
        }else if(i == 1){
            current = credits;
        }else{
            current = debits;
        }
        update();
    }

    public void sort(int i){
        switch(i){
            case 0:
                currentComparator = sortByDateAscending;
                break;
            case 1:
                currentComparator = sortByDateDescending;
                break;
            case 2:
                currentComparator = sortByAmount;
                break;
        }
        update();
    }

    public void write(String text, TreeSet<IOU> data){
        try{
            FileOutputStream fos = openFileOutput(text, Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(fos);

            //write current sort settings;
            int currentTypeWrite, currentSortWrite;

            if(current == all){
                currentTypeWrite = 0;
            }else if(current == credits){
                currentTypeWrite = 1;
            }else{
                currentTypeWrite = 2;
            }

            if(currentComparator == sortByAmount){
                currentSortWrite = 0;
            }else if(currentComparator == sortByDateAscending){
                currentSortWrite = 1;
            }else{
                currentSortWrite = 2;
            }

            dos.writeInt(currentTypeWrite);
            dos.writeInt(currentSortWrite);

            for(IOU i : data){
                try{
                    i.writeTo(dos, this);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            dos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void read(String text, TreeSet<IOU> data){
        try{
            FileInputStream fis = openFileInput(text);
            DataInputStream din = new DataInputStream(fis);

            //read previous settings
            int currentTypeRead = din.readInt();
            int currentSortRead = din.readInt();

            while(din.available() > 0){
                data.add(IOU.readFrom(din));
            }
            makeSubLists();

            if(currentTypeRead == 0){
                current = all;
            }else if(currentTypeRead == 1){
                current = credits;
            }else{
                current = debits;
            }

            if(currentSortRead == 0){
                currentComparator = sortByAmount;
            }else if(currentSortRead == 1){
                currentComparator = sortByDateAscending;
            }else{
                currentComparator = sortByDateDescending;
            }

            din.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
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
            updateCurrentData(0);
            update();
        }
        return true;
    }

    @Override
    public void onDialogPositiveClick(InputDialogFragment fragment){
        String name = fragment.getName();
        String number = fragment.getNumber();
        Bitmap photo = fragment.getPhoto();
        Uri contactUri = fragment.getContactUri();
        Float amount = fragment.getAmount();
        String note = fragment.getNote();
        boolean type = fragment.getType();
        Date dueDate = fragment.getDueDate();

        all.add(new IOU(contactUri, number, name, photo, amount, type, note, dueDate));

        if(current == all){
            updateCurrentData(0);
        }else if(current == credits){
            updateCurrentData(1);
        }else{
            updateCurrentData(2);
        }

    }

    public void sendSMS(IOU iou){
        String message = "Hey " + iou.getContactName() + "! Just wanted to remind you that you owe me " + iou.getFloatString()
            + " for " + iou.getNote() + ". Thanks!";

        try{
            Intent sms = new Intent(Intent.ACTION_VIEW);
            sms.setData(Uri.parse("smsto:" + iou.getContactNumber().replace("-", "")));
            sms.putExtra("sms_body", message);
            startActivity(sms);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "SMS Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void deleteIOU(IOU iou){
        all.remove(iou);
        makeSubLists();
        update();
    }

    public static IOU getStartupIOU(){
        return startupIOU;
    }


    //  public void editIOU(IOU iou, String amount, String note, String name, String type, Date dueDate){
    //        boolean oldType = iou.isType();
    //        iou.setAmount(Float.parseFloat(amount));
    //        iou.setContactName(name);
    //        iou.setNote(note);
    //        iou.setTypeByString(type);
    //        iou.setDueDate(dueDate);
    //
    //        if(oldType != iou.isType()){
    //            int index;
    //            if(oldType){
    //                index = credits.indexOf(iou);
    //                credits.remove(iou);
    //                debits.add(index, iou);
    //            }else{
    //                index = debits.indexOf(iou);
    //                debits.remove(iou);
    //                credits.add(index, iou);
    //            }
    //        }
    //        update();
    //    }

    public void addIOU(IOU iou){
        all.add(iou);
        makeSubLists();
        update();
    }
}
