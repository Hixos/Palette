package com.hixos.smartwp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hixos.smartwp.triggers.Wallpaper;
import com.hixos.smartwp.triggers.geofence.GeofenceWallpaper;
import com.hixos.smartwp.triggers.slideshow.SlideshowWallpaper;
import com.hixos.smartwp.triggers.timeofday.TimeOfDayWallpaper;


/**
 * Created by Luca on 22/10/13.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "Palette";
    public final static int DATABASE_VERSION = 1;

    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql;

        sql = "CREATE TABLE " + Wallpaper.TABLE_WALLPAPERS + "(";
        sql += Wallpaper.COLUMN_UID + " TEXT PRIMARY KEY, ";
        sql += Wallpaper.COLUMN_DELETED + " INTEGER";
        sql += ");";

        sqLiteDatabase.execSQL(sql);

        sql = "CREATE TABLE " + SlideshowWallpaper.TABLE_NAME + "(";
        sql += SlideshowWallpaper.COLUMN_UID + " TEXT PRIMARY KEY, ";
        sql += SlideshowWallpaper.COLUMN_ORDER + " INTEGER, ";
        sql += SlideshowWallpaper.COLUMN_SHUFFLE_ORDER + " INTEGER, ";
        sql += "FOREIGN KEY(" + SlideshowWallpaper.COLUMN_UID + ") REFERENCES " +
                Wallpaper.TABLE_WALLPAPERS + "(" + Wallpaper.COLUMN_UID + ")  ON DELETE CASCADE";
        sql += ");";

        sqLiteDatabase.execSQL(sql);

        sql = "CREATE TABLE " + GeofenceWallpaper.TABLE_NAME + "(";
        sql += GeofenceWallpaper.COLUMN_UID + " TEXT PRIMARY KEY, ";
        sql += GeofenceWallpaper.COLUMN_DISTANCE + " REAL, ";
        sql += GeofenceWallpaper.COLUMN_LATITUDE + " REAL, ";
        sql += GeofenceWallpaper.COLUMN_LONGITUDE + " REAL, ";
        sql += GeofenceWallpaper.COLUMN_RADIUS + " REAL, ";
        sql += GeofenceWallpaper.COLUMN_COLOR + " INTEGER, ";
        sql += GeofenceWallpaper.COLUMN_ZOOM_LEVEL + " REAL DEFAULT 17, ";
        sql += GeofenceWallpaper.COLUMN_ACTIVE + " INTEGER DEFAULT 0, ";
        sql += "FOREIGN KEY(" + GeofenceWallpaper.COLUMN_UID + ") REFERENCES " +
                Wallpaper.TABLE_WALLPAPERS + "(" + Wallpaper.COLUMN_UID + ") ON DELETE CASCADE";
        sql += ");";

        sqLiteDatabase.execSQL(sql);

        sql = "CREATE TABLE " + TimeOfDayWallpaper.TABLE_NAME + "(";
        sql += TimeOfDayWallpaper.COLUMN_UID + " TEXT PRIMARY KEY, ";
        sql += TimeOfDayWallpaper.COLUMN_START_HOUR + " INTEGER, ";
        sql += TimeOfDayWallpaper.COLUMN_END_HOUR + " INTEGER, ";
        sql += TimeOfDayWallpaper.COLUMN_COLOR_MUTED + " INTEGER, ";
        sql += TimeOfDayWallpaper.COLUMN_COLOR_VIBRANT + " INTEGER, ";
        sql += "FOREIGN KEY(" + TimeOfDayWallpaper.COLUMN_UID + ") REFERENCES " +
                Wallpaper.TABLE_WALLPAPERS + "(" + Wallpaper.COLUMN_UID + ")  ON DELETE CASCADE";
        sql += ");";

        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        /*switch (oldVersion){

        }*/
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
}

