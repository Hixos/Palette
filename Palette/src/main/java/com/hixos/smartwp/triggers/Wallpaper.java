package com.hixos.smartwp.triggers;

import android.database.Cursor;
import android.os.Parcel;

import java.security.InvalidParameterException;

public class Wallpaper{
    public final static String TABLE_WALLPAPERS = "[wallpapers]";

    private static final String COL_UID = "uid";
    public static final String COLUMN_UID = "[" + COL_UID + "]";
    private static final String COL_DELETED = "deleted";
    public static final String COLUMN_DELETED = "[" + COL_DELETED + "]";

    private String mUid;
    private boolean mDeleted;

    public Wallpaper(Parcel in) {
        readFromParcel(in);
    }

    public Wallpaper(Cursor c){
        int uidCol = c.getColumnIndex(COL_UID);
        int delCol = c.getColumnIndex(COL_DELETED);
        if(delCol == -1 || uidCol == -1){
            throw new InvalidParameterException("Required columns not found");
        }
        mUid = c.getString(uidCol);
        mDeleted = c.getInt(delCol) != 0;
    }

    /*protected Creator<Wallpaper> generateCreator(){
        return new Creator<Wallpaper>() {
            public Wallpaper createFromParcel(Parcel in) {
                return new Wallpaper(in);
            }

            public Wallpaper[] newArray(int size) {
                return new Wallpaper[size];
            }
        };
    }*/

    public String getUid() {
        return mUid;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public void writeToParcel(Parcel out, int i) {
        out.writeString(mUid);
        out.writeInt(mDeleted ? 1 : 0);
    }

    protected void readFromParcel(Parcel in){
        mUid = in.readString();
        mDeleted = in.readInt() != 0;
    }
}
