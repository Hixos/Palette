package com.hixos.smartwp.triggers.timeofday;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.Wallpaper;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.Hour24;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Hixos on 16/09/2015.
 */
public class TimeOfDayDB extends DatabaseManager {
    public static final String TOD_ID_PREFIX = "tod-";
    public static final String DEFAULT_WALLPAPER_UID = TOD_ID_PREFIX + "1337";
    private final static String LOGTAG = "TimeOfDayDB";
    private final static String COLUMNS[] = {
            Wallpaper.COLUMN_UID,
            Wallpaper.COLUMN_DELETED,
            TimeOfDayWallpaper.COLUMN_START_HOUR,
            TimeOfDayWallpaper.COLUMN_END_HOUR,
            TimeOfDayWallpaper.COLUMN_COLOR_MUTED,
            TimeOfDayWallpaper.COLUMN_COLOR_VIBRANT};

    public TimeOfDayDB(Context context) {
        super(context);
    }

    public static boolean hasDefaultWallpaper() {
        return FileUtils.fileExistance(ImageManager.getInstance()
                .getPictureUri(DEFAULT_WALLPAPER_UID));
    }

    /**
     * Return false if there is already a wallpaper for every moment of the day
     * @return false if no wallpaper can be added
     */
    public boolean canAddMoreWallpapers(){
        List<TimeOfDayWallpaper> wallpapers = getWallpapersByStartHour();
        if(wallpapers.size() > 0){
            if(wallpapers.get(0).getStartHour().equals(Hour24.Hour0000())
                    && wallpapers.get(wallpapers.size() - 1).getEndHour().equals(Hour24.Hour2400())){
                for(int i = 1; i < wallpapers.size(); i++){
                    if(!wallpapers.get(i).getStartHour().equals(wallpapers.get(i-1).getEndHour())){
                        return true;
                    }
                }
                return false;
            }else {
                return true;
            }
        }
        return true;
    }

    public TimeOfDayWallpaper addWallpaper(String uid, Hour24 startHour, Hour24 endHour,
                                              int mutedColor, int vibrantColor){
        if(addUid(uid, false)) {
            if (startHour == null || endHour == null || uid == null) {
                String param = startHour == null ? "startHour = null" : "";
                param += endHour == null ? " endHour = null" : "";
                param += uid == null ? " uid = null" : "";
                throw new IllegalArgumentException("createWallpaper (tod) - " + param);
            }
            SQLiteDatabase database = openDatabase();
            ContentValues v = new ContentValues();
            v.put(TimeOfDayWallpaper.COLUMN_UID, uid);
            v.put(TimeOfDayWallpaper.COLUMN_START_HOUR, startHour.getMinutes());
            v.put(TimeOfDayWallpaper.COLUMN_END_HOUR, endHour.getMinutes());
            v.put(TimeOfDayWallpaper.COLUMN_COLOR_MUTED, mutedColor);
            v.put(TimeOfDayWallpaper.COLUMN_COLOR_VIBRANT, vibrantColor);

            if (database.insert(TimeOfDayWallpaper.TABLE_NAME, null, v) != -1) {
                return getWallpaper(uid);
            } else {
                return null;
            }
        }else {
            Logger.e(LOGTAG, "Error adding new uid to wallpaper table");
            return null;
        }
    }

    /**
     * Reads wallpaper data from the database
     * @param uid The wallpaper to be read
     * @return Wallpaper class
     */
    public TimeOfDayWallpaper getWallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        TimeOfDayWallpaper out;

        Cursor data = queryWallpaper(database, TimeOfDayWallpaper.TABLE_NAME,
                TimeOfDayWallpaper.COLUMN_UID, COLUMNS, Wallpaper.COLUMN_UID + " = '" + uid + "'");

        data.moveToFirst();

        if (!data.isAfterLast()) {
            out = new TimeOfDayWallpaper(data);
            data.close();
            return out;
        } else {
            data.close();
            return null;
        }
    }
    
    /**
     * Returns the currently active wallpaper, or null if none is active.
     * @param calendar current date & time
     * @return Wallpaper or null
     */
    public TimeOfDayWallpaper getCurrentWallpaper(Calendar calendar){
        List<TimeOfDayWallpaper> wallpapers = getWallpapersByStartHour();
        Hour24 current = Hour24.fromCalendar(calendar);
        for(TimeOfDayWallpaper wp : wallpapers){
            if(current.compare(wp.getStartHour()) >= 0 && current.compare(wp.getEndHour()) < 0){
                return wp;
            }
        }
        return null;
    }

    /**
     * Returns the start time of the next wallpaper to be displayed on screen
     * @param calendar Current date & time
     * @return Calendar
     */
    public Calendar getNextWallpaperStart(Calendar calendar){
        List<TimeOfDayWallpaper> wallpapers = getWallpapersByStartHour();
        for(TimeOfDayWallpaper wp : wallpapers){
            if(calendar.before(wp.getStartHour().toCalendar())){
                return wp.getStartHour().toCalendar();
            }
        }
        //If no more wallpapers are planned for today, the first one is going to start tomorrow
        if(wallpapers.size() > 0){
            Calendar out = wallpapers.get(0).getStartHour().toCalendar();
            out.add(Calendar.DAY_OF_YEAR, 1); //Set it for tomorrow
            return out;
        }else{
            return null;
        }
    }

    /**
     * Returns the list of wallpapers currently stored in the database, ordered by start hour.
     * @return list of wallpapers ordered by start hour
     */
    public List<TimeOfDayWallpaper> getWallpapersByStartHour(){
        return getWallpaperList(TimeOfDayWallpaper.COLUMN_START_HOUR + " ASC", false);
    }

    /**
     * Returns the list of wallpapers currently stored in the database, ordered by the column
     * specified by "orderBy"
     * @return list of wallpapers ordered by "orderBy"
     */
    private List<TimeOfDayWallpaper> getWallpaperList(String orderBy, boolean includingDeleted){
        SQLiteDatabase database = openDatabase();
        Cursor data = queryWallpaper(database, TimeOfDayWallpaper.TABLE_NAME,
                TimeOfDayWallpaper.COLUMN_UID, COLUMNS,
                includingDeleted ? null : Wallpaper.COLUMN_DELETED + " = 0",
                orderBy);

        data.moveToFirst();
        return cursorToList(data);
    }

    /**
     * Returns the number of geowallpapers currently stored in the database
     *
     * @return Number of geowallpapers
     */
    public int getWallpaperCount() {
        SQLiteDatabase database = openDatabase();
        final SQLiteStatement stmt = database.compileStatement("SELECT COUNT(" +
                TimeOfDayWallpaper.COLUMN_UID + ") FROM "
                + Wallpaper.TABLE_WALLPAPERS + ", " + TimeOfDayWallpaper.TABLE_NAME
                + " WHERE " + Wallpaper.COLUMN_UID + " = " + TimeOfDayWallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 0");
        return  (int)stmt.simpleQueryForLong();
    }

    public void deleteWallpaper(String uid){
        SQLiteDatabase database = openDatabase();
        ContentValues values = new ContentValues();
        values.put(Wallpaper.COLUMN_DELETED, true);
        database.update(Wallpaper.TABLE_WALLPAPERS, values, Wallpaper.COLUMN_UID + " = '" + uid + "'", null);
    }

    public List<TimeOfDayWallpaper> getDeletedWallpapers(){
        SQLiteDatabase database = openDatabase();
        Cursor c = queryWallpaper(database, TimeOfDayWallpaper.TABLE_NAME,
                TimeOfDayWallpaper.COLUMN_UID,
                COLUMNS, Wallpaper.COLUMN_DELETED + " = 1", null);
        c.moveToFirst();
        return cursorToList(c);
    }

    public int getDeletedWallpapersCount(){
        SQLiteDatabase database = openDatabase();
        String query = "SELECT COUNT(" +
                Wallpaper.COLUMN_UID + ") FROM " + TimeOfDayWallpaper.TABLE_NAME
                +  ", " + Wallpaper.TABLE_WALLPAPERS
                + " WHERE " + TimeOfDayWallpaper.COLUMN_UID + " = " + Wallpaper.COLUMN_UID
                + " AND " + Wallpaper.COLUMN_DELETED + " = 1";
        final SQLiteStatement stmt = database.compileStatement(query);
        int count = (int)stmt.simpleQueryForLong();
        return count;
    }

    public void undoDeletion(){
        SQLiteDatabase database = openDatabase();
        database.execSQL("UPDATE " + Wallpaper.TABLE_WALLPAPERS
                + " SET " + Wallpaper.COLUMN_DELETED + " = 0"
                + " WHERE " + Wallpaper.COLUMN_DELETED + " = 1"
                + " AND " + Wallpaper.COLUMN_UID + " IN ("
                + "SELECT " + TimeOfDayWallpaper.COLUMN_UID
                + " FROM " + TimeOfDayWallpaper.TABLE_NAME + ")");

    }

    /**
     * Permanently deletes all the logically deleted wallpapers and all the files
     * associated with them.
     */
    public void confirmDeletion() {
        SQLiteDatabase database = openDatabase();
        Cursor c = database.rawQuery("SELECT ? FROM " + Wallpaper.TABLE_WALLPAPERS + ", "
                        + TimeOfDayWallpaper.TABLE_NAME
                        + " WHERE " + Wallpaper.COLUMN_DELETED + " = 1"
                        + " AND " + Wallpaper.COLUMN_UID + " = " + TimeOfDayWallpaper.COLUMN_UID,
                new String[]{Wallpaper.COLUMN_UID});

        c.moveToFirst();
        while (!c.isAfterLast()){
            String uid = c.getString(0);
            FileUtils.deleteFile(ImageManager.getInstance().getThumbnailUri(uid));
            FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
            c.moveToNext();
        }

        c.close();
        database.delete(Wallpaper.TABLE_WALLPAPERS, Wallpaper.COLUMN_DELETED + " = 1", null);
    }

    private List<TimeOfDayWallpaper> cursorToList(Cursor c){
        return cursorToList(c, true);
    }

    private List<TimeOfDayWallpaper> cursorToList(Cursor c, boolean close){
        List<TimeOfDayWallpaper> out = new ArrayList<>();
        while (!c.isAfterLast()){
            out.add(new TimeOfDayWallpaper(c));
            c.moveToNext();
        }
        if(close){
            c.close();
        }
        return out;
    }
}
