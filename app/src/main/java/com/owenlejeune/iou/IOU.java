package com.owenlejeune.iou;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by owenlejeune on 2017-04-11.
 */

public class IOU implements java.io.Serializable {

    //ContactsContract.Contacts person;
    private Uri contactUri;
    private String contactName;
    private Bitmap contactPhoto;
    private String contactNumber;
    private float amount;
    private boolean type;
    private String note;
    private Date dueDate;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    public IOU(Uri contactUri, String contactNumber, String contactName, Bitmap contactPhoto, float amount, boolean type, String note, Date dueDate) {
        this.contactUri = contactUri;
        this.contactNumber = contactNumber;
        this.contactName = contactName;
        this.contactPhoto = contactPhoto;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.dueDate = dueDate;
    }

    public IOU() {
        this.contactUri = null;
        this.contactNumber = "";
        this.contactName = "";
        this.amount = 0;
        this.type = false;
        this.note = "Add a new IOU!";
    }

    public void writeTo(DataOutputStream file, Activity activity) {
        try {
            file.writeUTF(contactUri.toString());
            file.writeUTF(contactName);
            file.writeUTF(saveToInternalStorage(contactPhoto, activity));
            file.writeUTF(contactNumber);
            file.writeFloat(amount);
            file.writeBoolean(type);
            file.writeUTF(note);
            file.writeUTF(SDF.format(dueDate));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static IOU readFrom(DataInputStream file) {
        try {
            Uri contactUri = Uri.parse(file.readUTF());
            String contactName = file.readUTF();
            Bitmap contactPhoto = loadImageFromStorage(contactName, file.readUTF());
            String contactNumber = file.readUTF();
            float amount = file.readFloat();
            boolean type = file.readBoolean();
            String note = file.readUTF();
            Date dueDate = SDF.parse(file.readUTF());

            return new IOU(contactUri, contactNumber, contactName, contactPhoto, amount, type, note, dueDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String saveToInternalStorage(Bitmap image, Activity activity) {
        ContextWrapper cw = new ContextWrapper(activity.getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myPath = new File(directory, contactName.replace(" ", "") + ".jpg");
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(myPath);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    private static Bitmap loadImageFromStorage(String contactName, String path) {
        try {
            File f = new File(path, contactName.replace(" ", "") + ".jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Uri getContactUri() {
        return contactUri;
    }

    public String getContactName() {
        return contactName;
    }

    public Bitmap getContactPhoto() {
        return contactPhoto;
    }

    public float getAmount() {
        return amount;
    }

    public boolean isType() {
        return type;
    }

    public String getNote() {
        return note;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getFloatString() {
        return "$" + String.format("%,.2f", amount);
    }

    public void setContactUri(Uri contactUri) {
        this.contactUri = contactUri;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhoto(Bitmap contactPhoto) {
        this.contactPhoto = contactPhoto;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    public void setTypeByString(String type) {
        if (type.toLowerCase().equals("credits")) {
            this.type = true;
        } else {
            this.type = false;
        }
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
}
