package com.hixos.smartwp.utils;

import android.content.Context;

import com.hixos.smartwp.R;

/**
 * Created by Luca on 17/11/13.
 */
public class TimeMath {
    public static final int UNIT_MINUTE = 0;
    public static final int UNIT_HOUR = 1;
    public static final int UNIT_DAY = 2;

    public static String getTimeString(long time, Context c) {
        long t = time / 1000;

        if(t <= 0){
            return null;
        }else if(t < 60){
            return t + " seconds";
        }else if(t < 3600){
            int m = Math.round((float)t / 60f);
            return m + " " + c.getResources().getQuantityString(R.plurals.minute, m);
        }else if(t < 86400){
            int h = (int)((float)t / 3600f);
            int m = Math.round((float) (t - h * 3600) / 60f);
            String out = h + " " + c.getResources().getQuantityString(R.plurals.hour, h);
            if(m > 0){
                out += " " + m + " " + c.getResources().getQuantityString(R.plurals.minute, m);
            }
            return out;
        }else{
            int d = (int)((float)t / 3600f / 24f);
            int h = Math.round((float) (t - d * 3600 * 24) / 3600f);
            String out = d + " " + c.getResources().getQuantityString(R.plurals.day, d);
            if(h > 0){
                out += " " + h + " " + c.getResources().getQuantityString(R.plurals.hour, h);
            }
            return out;
        }
    }

    public static String getTimeUnitString(long time, Context c) {
        long t = time / 1000;
        if(t < 3600) {
            return c.getResources().getQuantityString(R.plurals.minute, getTimeInUnit(time));
        }else if(t < 86400) {
            return c.getResources().getQuantityString(R.plurals.hour, getTimeInUnit(time));
        }else{
            return c.getResources().getQuantityString(R.plurals.day, getTimeInUnit(time));
        }
    }

    public static int getTimeUnitValue(long time) {
        long t = time / 1000;
        if(t < 3600) {
            return UNIT_MINUTE;
        }else if(t < 86400) {
            return UNIT_HOUR;
        }else{
            return UNIT_DAY;
        }
    }


    public static int getTimeInUnit(long time) {
        time = time / 1000;
        if(time < 3600) {
            return Math.round(time / 60);
        }else if(time < 86400) {
            return Math.round(time / 3600);
        }else{
            return Math.round(time / 3600 / 24);
        }
    }
}
