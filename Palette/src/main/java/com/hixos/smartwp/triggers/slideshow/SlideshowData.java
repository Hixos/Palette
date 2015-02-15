package com.hixos.smartwp.triggers.slideshow;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class SlideshowData implements Parcelable {
    public final static String[] DATA_COLUMNS = {
            SlideshowDatabase.COLUMN_DATA_ID};
    public static final Creator<SlideshowData> CREATOR
            = new Creator<SlideshowData>() {
        public SlideshowData createFromParcel(Parcel in) {
            return new SlideshowData(in);
        }

        public SlideshowData[] newArray(int size) {
            return new SlideshowData[size];
        }
    };
    private String mUid;

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

    public String getUid() {
        return mUid;
    }

    public void setUid(String uid) {
        this.mUid = uid;
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
}
