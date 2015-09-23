package com.hixos.smartwp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hixos.smartwp.triggers.Wallpaper;

import java.util.UUID;

public abstract class DatabaseManager {
    private static DatabaseHelper sDbHelper;
    private static SQLiteDatabase sDatabase;

    private Context mContext;


    public DatabaseManager(Context context) {
        mContext = context.getApplicationContext();
        initializeDatabase(mContext);
    }

    public static synchronized void initializeDatabase(Context context) {
        if (sDbHelper == null) {
            sDbHelper = new DatabaseHelper(context.getApplicationContext());
        }
    }

    protected static Cursor queryWallpaper(SQLiteDatabase database, String table, String id,
                                  String[] columns, String where){
        return queryWallpaper(database, table, id, columns, where, null, 0);
    }

    protected static Cursor queryWallpaper(SQLiteDatabase database, String table, String id,
                                           String[] columns, String where, String orderBy){
        return queryWallpaper(database, table, id, columns, where, orderBy, 0);
    }

    /**
     * String tag for the database
     *
     * @return tag
     */
    //public abstract String getTag();

    protected static Cursor queryWallpaper(SQLiteDatabase database, String table, String id,
                                           String[] columns, String where, String orderBy, int limit){
        String sql = "SELECT ";
        for(int i = 0; i < columns.length; i++){
            sql += columns[i] + (i == columns.length - 1 ? "" : ", ");
        }
        sql += " FROM ";
        sql += Wallpaper.TABLE_WALLPAPERS + ", " + table;
        sql += " WHERE ";
        sql += Wallpaper.TABLE_WALLPAPERS + "." + Wallpaper.COLUMN_UID + " = " + table + "." + id;
        if(where != null){
            sql += " AND " + where;
        }
        if(orderBy != null){
            sql += " ORDER BY " + orderBy;
        }

        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        Logger.d(sql);
        return database.rawQuery(sql, null);
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Return the app's database
     *
     * @return App's database
     */
    public synchronized SQLiteDatabase openDatabase() {
        if (sDatabase == null) {
            sDatabase = sDbHelper.getWritableDatabase();
        }
        return sDatabase;
    }

    /**
     * Default method to get a unique id
     *
     * @return uid
     */
    public String getNewUid() {
        return UUID.randomUUID().toString();
    }

    protected boolean addUid(String uid, boolean confirm){
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(Wallpaper.COLUMN_UID, uid);
        values.put(Wallpaper.COLUMN_DELETED, confirm);
        return database.insert(Wallpaper.TABLE_WALLPAPERS, null, values) != -1;

    }
}
