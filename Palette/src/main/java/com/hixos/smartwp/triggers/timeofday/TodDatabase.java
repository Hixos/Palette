package com.hixos.smartwp.triggers.timeofday;

import android.content.Context;

import com.hixos.smartwp.DatabaseManager;

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
    public final static String COLUMN_DATA_COLOR = "[color]";
    public final static String COLUMN_DATA_DELETED = "[deleted]";

    public static final String TOD_ID_PREFIX = "tod-";
    private static final String TAG = "timeofday";
    private static final String LOGTAG = "TodDatabase";

    public TodDatabase(Context context) {
        super(context);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
