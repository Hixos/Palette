package com.hixos.smartwp.triggers.timeofday;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.Hour;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Luca on 15/02/2015.
 */
public class TodDatabase extends DatabaseManager {
    public final static String TABLE_DATA = "[timeofday_data]";
    public final static String COLUMN_DATA_ID = "[_id]";
    public final static String COLUMN_DATA_START_HOUR = "[start_hour]";
    public final static String COLUMN_DATA_START_MINUTE = "[start_minute]";
    public final static String COLUMN_DATA_START_PERIOD = "[start_period]";
    public final static String COLUMN_DATA_END_HOUR = "[end_hour]";
    public final static String COLUMN_DATA_END_MINUTE = "[end_minute]";
    public final static String COLUMN_DATA_END_PERIOD = "[end_period]";
    public final static String COLUMN_DATA_COLOR_MUTED = "[color_muted]";
    public final static String COLUMN_DATA_COLOR_VIBRANT = "[color_vibrant]";
    public final static String COLUMN_DATA_DELETED = "[deleted]";

    public static final String TOD_ID_PREFIX = "tod-";
    private static final String TAG = "timeofday";
    private static final String LOGTAG = "TodDatabase";

    private DatabaseObserver mObserver;
    private OnElementRemovedListener mOnElementRemovedListener;

    public TodDatabase(Context context) {
        super(context);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getNewUid() {
        return TOD_ID_PREFIX + super.getNewUid();
    }

    public void setDatabaseObserver(DatabaseObserver observer) {
        mObserver = observer;
    }

    public void setOnElementRemovedListener(OnElementRemovedListener listener){
        mOnElementRemovedListener = listener;
    }

    public TimeOfDayWallpaper createWallpaper(String uid, Hour startHour, Hour endHour,
                                              int mutedColor, int vibrantColor){
        if(startHour == null || endHour == null || uid == null){
            String param = startHour == null ? "startHour = null" : "";
            param += endHour == null ? " endHour = null" : "";
            param += uid == null ? " uid = null" : "";
            throw new IllegalArgumentException("createWallpaper (tod) - " + param);
        }
        SQLiteDatabase database = openDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_DATA_ID, uid);
        v.put(COLUMN_DATA_START_HOUR, startHour.getHour());
        v.put(COLUMN_DATA_START_MINUTE, startHour.getMinute());
        v.put(COLUMN_DATA_START_PERIOD, startHour.getPeriod());
        v.put(COLUMN_DATA_END_HOUR, endHour.getHour());
        v.put(COLUMN_DATA_END_MINUTE, endHour.getMinute());
        v.put(COLUMN_DATA_END_PERIOD, endHour.getPeriod());
        v.put(COLUMN_DATA_COLOR_MUTED, mutedColor);
        v.put(COLUMN_DATA_COLOR_VIBRANT, vibrantColor);
        v.put(COLUMN_DATA_DELETED, false);

        if(database.insert(TABLE_DATA, null, v) != -1){
            closeDatabase();

            TimeOfDayWallpaper out = new TimeOfDayWallpaper(uid);
            out.setStartHour(startHour);
            out.setEndHour(endHour);
            out.setMutedColor(mutedColor);
            out.setVibrantColor(vibrantColor);
            out.setDeleted(false);
            if (mObserver != null)
                mObserver.onElementCreated(out);
            return out;
        }else{
            closeDatabase();
            return null;
        }
    }

    public TimeOfDayWallpaper getWallpaper(String uid) {
        SQLiteDatabase database = openDatabase();
        TimeOfDayWallpaper out;
        Cursor data = database.query(TABLE_DATA, TimeOfDayWallpaper.DATA_COLUMNS,
                COLUMN_DATA_ID + " = ?", new String[]{uid}, null, null, null);
        data.moveToFirst();

        if (!data.isAfterLast()) {
            out = TimeOfDayWallpaper.fromCursor(data);
            data.close();
            closeDatabase();
            return out;
        } else {
            data.close();
            closeDatabase();
            return null;
        }
    }

    public boolean isFull(){
        List<TimeOfDayWallpaper> wallpapers = getOrderedWallpapers();
        if(wallpapers.size() > 0){
            if(wallpapers.get(0).getStartHour().equals(0,0,Hour.AM)
                    && wallpapers.get(wallpapers.size() - 1).getEndHour().equals(11,59,Hour.PM)){
                for(int i = 1; i < wallpapers.size(); i++){
                    if(!wallpapers.get(i).getStartHour().equals(wallpapers.get(i-1).getEndHour())){
                        return false;
                }
            }
                return true;
            }else {
                return false;
            }
        }else {
            return false;
        }
    }
    /**
     * Permanently deletes all the logically deleted geowallpapers and all the files
     * associated with them.
     */
    public void clearDeletedWallpapers() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA, new String[]{COLUMN_DATA_ID, COLUMN_DATA_DELETED}, COLUMN_DATA_DELETED + " = 1", null, null, null, null);
        c.moveToFirst();

        if (c.isAfterLast()) return;

        List<String> deletedIds = new ArrayList<>();

        do {
            deletedIds.add(c.getString(0));
            c.moveToNext();
        } while (!c.isAfterLast());

        database.delete(TABLE_DATA, COLUMN_DATA_DELETED + " = 1", null);

        for (String uid : deletedIds) {
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
        }
        c.close();
        closeDatabase();
    }

    public void clearDeletedWallpapersAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                clearDeletedWallpapers();
                return null;
            }
        };
        task.execute();
    }

    public TimeOfDayWallpaper getCurrentWallpaper(Context context, Calendar calendar){
        List<TimeOfDayWallpaper> wallpapers = getOrderedWallpapers();
        for(TimeOfDayWallpaper wp : wallpapers){
            if(calendar.after(wp.getStartHour().getCalendar(context))
                    && calendar.before(wp.getEndHour().getCalendar(context))){
                return wp;
            }
        }
        return null;
    }

    public TimeOfDayWallpaper getNextWallpaper(Context context, Calendar calendar){
        List<TimeOfDayWallpaper> wallpapers = getOrderedWallpapers();
        for(TimeOfDayWallpaper wp : wallpapers){
            if(wp.getStartHour().getCalendar(context).after(calendar)){
                return wp;
            }
        }
        if(wallpapers.size() > 0){
            return wallpapers.get(0);
        }else{
            return null;
        }
    }

    public List<TimeOfDayWallpaper> getOrderedWallpapers(){
        SQLiteDatabase database = openDatabase();
        List<TimeOfDayWallpaper> wallpapers = new ArrayList<>();
        Cursor data = database.query(TABLE_DATA, TimeOfDayWallpaper.DATA_COLUMNS,
                COLUMN_DATA_DELETED + " = " + 0, null, null, null,
                COLUMN_DATA_START_PERIOD + " ASC, "
                        + COLUMN_DATA_START_HOUR + " ASC, "
                        + COLUMN_DATA_START_MINUTE + " ASC");
        data.moveToFirst();

        while (!data.isAfterLast()) {
            wallpapers.add(TimeOfDayWallpaper.fromCursor(data));
            data.moveToNext();
        }

        data.close();
        closeDatabase();

        return wallpapers;
    }

    /**
     * Return the number of slideshow wallpaper stored in the database
     *
     * @return int
     */
    public int getWallpaperCount() {
        SQLiteDatabase database = openDatabase();
        Cursor c = null;

        int count = 0;
        try {
            c = database.rawQuery("SELECT count(*) FROM " + TABLE_DATA + " WHERE " + COLUMN_DATA_DELETED + " = 0", null);
            c.moveToFirst();
            if (!c.isAfterLast()) {
                count = c.getInt(0);
            }
        } finally {
            if (c != null) c.close();
            closeDatabase();
        }

        return count;
    }

    public void deleteWallpaper(String uid, boolean notifyObserver){
            SQLiteDatabase database = openDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_DATA_DELETED, 1);
            database.update(TABLE_DATA, v, COLUMN_DATA_ID + " = ?", new String[]{uid});
            closeDatabase();

            if (notifyObserver && mObserver != null) {
                mObserver.onElementRemoved(uid);
            }
            if (mOnElementRemovedListener != null)
                mOnElementRemovedListener.onElementRemoved(uid);
    }

    public void adapterDeleteWallpaper(String uid){
        AsyncTask<String, Void, Void> deleteTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                deleteWallpaper(strings[0], false);
                return null;
            }
        };
        deleteTask.execute(uid);
    }

    public List<String> getDeletedWallpapers() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.query(TABLE_DATA,
                new String[]{COLUMN_DATA_ID}, COLUMN_DATA_DELETED + " = 1", null, null, null, null);

        List<String> deleted = new ArrayList<>();
        c.moveToFirst();

        while (!c.isAfterLast()) {
            deleted.add(c.getString(0));
            c.moveToNext();
        }

        c.close();
        closeDatabase();
        return deleted;
    }

    public int getDeletedWallpapersCount() {
        SQLiteDatabase database = openDatabase();
        String query = "SELECT count(" + COLUMN_DATA_ID + ") FROM " + TABLE_DATA + " WHERE "
                + COLUMN_DATA_DELETED + " = 1";
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();
        int count = 0;
        if (!cursor.isAfterLast()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        closeDatabase();
        return count;
    }

    public void restoreWallpapers() {
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATA_DELETED, false);
        database.update(TABLE_DATA, values, COLUMN_DATA_DELETED + " = 1", null);
        closeDatabase();
    }

    public void restoreWallpapersAsync() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                restoreWallpapers();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (mObserver != null) {
                    mObserver.onDataSetChanged();
                }
            }
        };
        task.execute();
    }

    public interface DatabaseObserver {
        public void onElementRemoved(String uid);

        public void onElementCreated(TimeOfDayWallpaper element);

        public void onDataSetChanged();
    }

    public interface OnElementRemovedListener {
        public void onElementRemoved(String uid);
    }

    public interface OnWallpapersLoadedListener {
        public void onWallpapersLoaded(List<TimeOfDayWallpaper> wallpapers);
    }
}
