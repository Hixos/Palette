package com.hixos.smartwp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hixos.smartwp.services.geofence.GeofenceDatabase;
import com.hixos.smartwp.services.slideshow.SlideshowDatabase;

/**
 * Created by Luca on 22/10/13.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public final static String DATABASE_NAME = "SmartWallpaper";
    public final static int DATABASE_VERSION = 10;

    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String sql;

        sql = "CREATE TABLE " + SlideshowDatabase.TABLE_DATA + "(";
        sql += SlideshowDatabase.COLUMN_DATA_ID + " TEXT PRIMARY KEY, ";
        sql += SlideshowDatabase.COLUMN_DATA_ORDER + " INTEGER, ";
        sql += SlideshowDatabase.COLUMN_DATA_SHUFFLE_ORDER + " INTEGER, ";
        sql += SlideshowDatabase.COLUMN_DATA_DELETED + " INTEGER";
        sql += ");";

        sqLiteDatabase.execSQL(sql);

        sql = "CREATE TABLE " + GeofenceDatabase.TABLE_DATA + "(";
        sql += GeofenceDatabase.COLUMN_DATA_ID + " TEXT PRIMARY KEY, ";
        sql += GeofenceDatabase.COLUMN_DATA_DISTANCE + " REAL, ";
        sql += GeofenceDatabase.COLUMN_DATA_LATITUDE + " REAL, ";
        sql += GeofenceDatabase.COLUMN_DATA_LONGITUDE + " REAL, ";
        sql += GeofenceDatabase.COLUMN_DATA_RADIUS + " REAL, ";
        sql += GeofenceDatabase.COLUMN_DATA_COLOR + " INTEGER, ";
        sql += GeofenceDatabase.COLUMN_DATA_DELETED + " INTEGER, ";
        sql += GeofenceDatabase.COLUMN_DATA_ZOOM_LEVEL + " REAL DEFAULT 17, ";
        sql += GeofenceDatabase.COLUMN_DATA_NAME + " TEXT DEFAULT '', ";
        sql += GeofenceDatabase.COLUMN_DATA_ACTIVE + " INTEGER DEFAULT 0";
        sql += ");";

        sqLiteDatabase.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        /*if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }*/
    }
}
