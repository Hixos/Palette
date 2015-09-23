package com.hixos.smartwp.triggers.timeofday;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.hixos.smartwp.triggers.Wallpaper;
import com.hixos.smartwp.utils.Hour24;

import java.security.InvalidParameterException;

/**
 * Created by Hixos on 16/09/2015.
 */

public class TimeOfDayWallpaper extends Wallpaper implements Parcelable{
    public final static String TABLE_NAME = "[timeofday]";
    public static final Creator<Wallpaper> CREATOR = new Creator<Wallpaper>() {
        public TimeOfDayWallpaper createFromParcel(Parcel in) {
            return new TimeOfDayWallpaper(in);
        }

        public TimeOfDayWallpaper[] newArray(int size) {
            return new TimeOfDayWallpaper[size];
        }
    };
    private final static String COL_UID = "tod_uid";
    public final static String COLUMN_UID = "[" + COL_UID + "]";
    private final static String COL_START_HOUR = "start_hour";
    public final static String COLUMN_START_HOUR = "[" + COL_START_HOUR + "]";
    private final static String COL_END_HOUR = "end_hour";
    public final static String COLUMN_END_HOUR = "[" + COL_END_HOUR  + "]";
    private final static String COL_COLOR_MUTED = "color_muted";
    public final static String COLUMN_COLOR_MUTED = "[" + COL_COLOR_MUTED  + "]";
    private final static String COL_COLOR_VIBRANT = "color_vibrant";
    public final static String COLUMN_COLOR_VIBRANT = "[" + COL_COLOR_VIBRANT  + "]";
    private Hour24 mStartHour;
    private Hour24 mEndHour;
    private int mMutedColor, mVibrantColor;

    public TimeOfDayWallpaper(Parcel in) {
        super(in);
    }
    public TimeOfDayWallpaper(Cursor cursor) {
        super(cursor);
        int colStartHour = cursor.getColumnIndex(COL_START_HOUR);
        int colEndHour = cursor.getColumnIndex(COL_END_HOUR);
        int colMutedColor = cursor.getColumnIndex(COL_COLOR_MUTED);
        int colVibrantColor = cursor.getColumnIndex(COL_COLOR_VIBRANT);

        if(colStartHour < 0 || colEndHour < 0 || colMutedColor < 0 || colVibrantColor < 0){
            throw new InvalidParameterException("Required columns not found");
        }

        mStartHour = new Hour24(cursor.getInt(colStartHour));
        mEndHour = new Hour24(cursor.getInt(colEndHour));
        mMutedColor = cursor.getInt(colMutedColor);
        mVibrantColor = cursor.getInt(colVibrantColor);
    }

    public Hour24 getStartHour() {
        return mStartHour;
    }

    public Hour24 getEndHour() {
        return mEndHour;
    }

    public int getMutedColor() {
        return mMutedColor;
    }

    public int getVibrantColor() {
        return mVibrantColor;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int i) {
        super.writeToParcel(out, i);

        out.writeInt(mStartHour.getMinutes());
        out.writeInt(mEndHour.getMinutes());
        out.writeInt(mMutedColor);
        out.writeInt(mVibrantColor);
    }

    @Override
    protected void readFromParcel(Parcel in) {
        super.readFromParcel(in);

        mStartHour = new Hour24(in.readInt());
        mEndHour = new Hour24(in.readInt());
        mMutedColor = in.readInt();
        mVibrantColor = in.readInt();
    }
}
