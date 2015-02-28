package com.hixos.smartwp.triggers.timeofday;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.hixos.smartwp.utils.Hour;

/**
 * Created by Luca on 19/02/2015.
 */
public class TimeOfDayWallpaper implements Parcelable {
    public final static String[] DATA_COLUMNS = {
            TodDatabase.COLUMN_DATA_ID,
            TodDatabase.COLUMN_DATA_START_HOUR,
            TodDatabase.COLUMN_DATA_START_MINUTE,
            TodDatabase.COLUMN_DATA_START_PERIOD,
            TodDatabase.COLUMN_DATA_END_HOUR,
            TodDatabase.COLUMN_DATA_END_MINUTE,
            TodDatabase.COLUMN_DATA_END_PERIOD,
            TodDatabase.COLUMN_DATA_COLOR_MUTED,
            TodDatabase.COLUMN_DATA_COLOR_VIBRANT,
            TodDatabase.COLUMN_DATA_DELETED};

    public static final Creator<TimeOfDayWallpaper> CREATOR
            = new Creator<TimeOfDayWallpaper>() {
        public TimeOfDayWallpaper createFromParcel(Parcel in) {
            return new TimeOfDayWallpaper(in);
        }

        public TimeOfDayWallpaper[] newArray(int size) {
            return new TimeOfDayWallpaper[size];
        }
    };

    private final String mUid;
    private Hour mStartHour;
    private Hour mEndHour;
    private int mMutedColor, mVibrantColor;
    private boolean mDeleted;

    public String getUid() {
        return mUid;
    }

    public Hour getStartHour() {
        return mStartHour;
    }

    public void setStartHour(Hour startHour) {
        this.mStartHour = startHour;
    }

    public Hour getEndHour() {
        return mEndHour;
    }

    public void setEndHour(Hour endHour) {
        this.mEndHour = endHour;
    }

    public int getMutedColor() {
        return mMutedColor;
    }

    public void setMutedColor(int color) {
        this.mMutedColor = color;
    }

    public int getVibrantColor() {
        return mVibrantColor;
    }

    public void setVibrantColor(int color) {
        this.mVibrantColor = color;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean deleted) {
        this.mDeleted = deleted;
    }

    public TimeOfDayWallpaper(String uid){
        mUid = uid;
    }

    public TimeOfDayWallpaper(Parcel in) {
        mUid = in.readString();
        mStartHour = new Hour(
                in.readInt(),
                in.readInt(),
                in.readInt()
        );
        mEndHour = new Hour(
                in.readInt(),
                in.readInt(),
                in.readInt()
        );
        mMutedColor = in.readInt();
        mVibrantColor = in.readInt();
        mDeleted = in.readByte() == 1;
    }

    public static TimeOfDayWallpaper fromCursor(Cursor cursor){
        TimeOfDayWallpaper data = new TimeOfDayWallpaper(cursor.getString(0));
        data.mStartHour = new Hour(
                cursor.getInt(1),
                cursor.getInt(2),
                cursor.getInt(3));
        data.mEndHour = new Hour(
                cursor.getInt(4),
                cursor.getInt(5),
                cursor.getInt(6));
        data.mMutedColor = cursor.getInt(7);
        data.mVibrantColor = cursor.getInt(7);
        data.mDeleted = cursor.getInt(8) == 1;
        return data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mUid);
        dest.writeInt(mStartHour.getHour());
        dest.writeInt(mStartHour.getMinute());
        dest.writeInt(mStartHour.getPeriod());
        dest.writeInt(mEndHour.getHour());
        dest.writeInt(mEndHour.getMinute());
        dest.writeInt(mEndHour.getPeriod());
        dest.writeInt(mMutedColor);
        dest.writeInt(mVibrantColor);
        dest.writeInt(mDeleted ? 1 : 0);
    }
}
