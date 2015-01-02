package com.hixos.smartwp.services.slideshow;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Luca on 12/10/13.
 */


public class SlideshowData implements Parcelable {
    public final static String[] DATA_COLUMNS = {
            SlideshowDatabase.COLUMN_DATA_ID};


    private String mUid;

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
    }

    public SlideshowData() {
    }

    public SlideshowData(Parcel in) {
        readFromParcel(in);
    }

    public static SlideshowData fromCursor(Cursor c) {
        SlideshowData d = new SlideshowData();
        d.mUid = c.getString(0);
        return d;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(mUid);
    }

    private void readFromParcel(Parcel in) {
        mUid = in.readString();
    }

    public static final Creator<SlideshowData> CREATOR
            = new Creator<SlideshowData>() {
        public SlideshowData createFromParcel(Parcel in) {
            return new SlideshowData(in);
        }

        public SlideshowData[] newArray(int size) {
            return new SlideshowData[size];
        }
    };
}
