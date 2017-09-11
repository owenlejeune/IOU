package com.owenlejeune.iou;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;

public class ContactActivity extends AppCompatActivity {

    private static int RESULT_PICK_CONTACT = 0;
    private String number;
    private Uri contactUri;
    private Uri photoU;
    private Bitmap photo;
    private String name;
    private static InputDialogFragment parent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //opens a new contact picker
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == RESULT_PICK_CONTACT){
            if(resultCode == RESULT_OK){
                //gets the Uri for the selected contact
                contactUri = data.getData();

                Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
                cursor.moveToFirst();

                //gets the contact number
                int numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                number = cursor.getString(numCol);

                //gets the contact name
                int nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY);
                name = cursor.getString(nameCol);

                //gets the Uri for the contact photo
                int photoURI = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_THUMBNAIL_URI);
                String temp = cursor.getString(photoURI);

                if(temp != null) {
                    photoU = Uri.parse(temp);

                    //gets the full sized image thumbnail from the photo Uri
                    try{
                        InputStream image_stream = getContentResolver().openInputStream(photoU);
                        photo = BitmapFactory.decodeStream(image_stream);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                } else {
                    photo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_account_circle_white_24dp);
                }

                //passes all the data to the parent InputDialogFragment
                Object[] fields = new Object[]{number, photo, name, contactUri};
                InputDialogFragment.setFields(fields, parent);

                finish();

            }
        }
    }

    public static void setParent(InputDialogFragment parent) {
        ContactActivity.parent = parent;
    }
}
