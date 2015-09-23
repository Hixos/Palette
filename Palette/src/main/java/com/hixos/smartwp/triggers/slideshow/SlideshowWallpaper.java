package com.hixos.smartwp.triggers.slideshow;

import android.database.Cursor;
import android.os.Parcel;

import com.hixos.smartwp.triggers.Wallpaper;

import java.security.InvalidParameterException;

public class SlideshowWallpaper extends Wallpaper {
    public final static String TABLE_NAME = "[slideshow]";

    private final static String COL_UID = "slideshow_uid";
    public final static String COLUMN_UID = "[" + COL_UID + "]";
    private final static String COL_ORDER = "order";
    public final static String COLUMN_ORDER = "[" + COL_ORDER + "]";
    private final static String COL_SHUFFLE_ORDER = "shuffle_order";
    public final static String COLUMN_SHUFFLE_ORDER = "[" + COL_SHUFFLE_ORDER + "]";

    //public final Creator<SlideshowWallpaper> CREATOR;

    private int mOrder;
    private int mShuffleOrder;

    public SlideshowWallpaper(Parcel in) {
        super(in);
    }

    public SlideshowWallpaper(Cursor c) {
        super(c);
        int colOrder = c.getColumnIndex(COL_ORDER);
        int colShuffle = c.getColumnIndex(COL_SHUFFLE_ORDER);
        if(colOrder == -1 || colShuffle == -1){
            throw new InvalidParameterException("Required columns not found");
        }
        mOrder = c.getInt(colOrder);
        mShuffleOrder = c.getInt(colShuffle);
    }

    public int getOrder() {
        return mOrder;
    }

    public int getShuffleOrder() {
        return mShuffleOrder;
    }

    /*@Override
    protected Creator<Wallpaper> generateCreator() {
        return new Creator<Wallpaper>() {
            public SlideshowWallpaper createFromParcel(Parcel in) {
                return new SlideshowWallpaper(in);
            }

            public SlideshowWallpaper[] newArray(int size) {
                return new SlideshowWallpaper[size];
            }
        };
    }*/

    @Override
    public void writeToParcel(Parcel out, int i) {
        super.writeToParcel(out, i);
        out.writeInt(mOrder);
        out.writeInt(mShuffleOrder);

    }

    @Override
    protected void readFromParcel(Parcel in) {
        super.readFromParcel(in);
        mOrder = in.readInt();
        mShuffleOrder = in.readInt();
    }
}
