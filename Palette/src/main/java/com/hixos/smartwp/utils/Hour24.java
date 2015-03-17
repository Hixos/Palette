package com.hixos.smartwp.utils;

import android.content.Context;

import com.hixos.smartwp.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Luca on 02/03/2015.
 */
public class Hour24 {
    private int mMinutes;

    private static final Hour24 HOUR_0000;
    private static final Hour24 HOUR_1200;
    private static final Hour24 HOUR_2400;

    public static Hour24 Hour0000() {
        return new Hour24(HOUR_0000);
    }

    public static Hour24 Hour1200() {
        return new Hour24(HOUR_1200);
    }

    public static Hour24 Hour2400() {
        return new Hour24(HOUR_2400);
    }

    static {
        HOUR_2400 = new Hour24(24*60);
        HOUR_1200 = new Hour24(12 * 60);
        HOUR_0000 = new Hour24(0);
    }

    public int getHour() {
        return (mMinutes - getMinute()) / 60;
    }

    public void setHour(int hour) {
        hour = hour % 24;
        if(hour < 0){
            hour += 24;
        }
        mMinutes = getMinute() + hour * 60;
    }

    public int getMinute() {
        return mMinutes % 60;
    }

    public void setMinute(int minute) {
        minute = minute % 60;
        if(minute < 0){
            minute += 60;
        }
        mMinutes = mMinutes - getMinute() + minute;
    }

    public int getMinutes(){
        return mMinutes;
    }

    public void setMinutes(int minutes){
        if(minutes == 24 * 60){
            mMinutes = minutes;
            return;
        }
        mMinutes = minutes % (24 * 60);
        if(mMinutes < 0){
            mMinutes += 24 * 60;
        }
    }

    public Hour24(int minutes){
        if(minutes == 24 * 60){
            mMinutes = minutes;
            return;
        }
        mMinutes = minutes % (24 * 60);
        if(mMinutes < 0){
            mMinutes += 24 * 60;
        }
    }

    public Hour24(int hour, int minute){
        setHour(hour);
        setMinute(minute);
    }

    public Hour24(Hour24 hour){
        set(hour);
    }

    public void set(Hour24 hour){
        this.mMinutes = hour.mMinutes;
    }

    public int compare(Hour24 hour){
        return compare(this, hour);
    }

    public boolean between(Hour24 hour1, Hour24 hour2){
        return compare(hour1) >= 0 && compare(hour2) <= 0;
    }

    public static int compare(Hour24 hour1, Hour24 hour2){
        return hour1.mMinutes - hour2.mMinutes;
    }

    public void subtract(int minutes){
        setMinutes(getMinutes() - minutes);
    }

    public void add(int minutes){
        setMinutes(getMinutes() + minutes);
    }

    public static Hour24 fromCalendar(Calendar calendar){
        return new Hour24(calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE));
    }

    public Calendar toCalendar(){
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, getHour() % 24);
        cal.set(Calendar.MINUTE, getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public boolean equals(Hour24 hour){
        return this.compare(hour) == 0;
    }
    public String toString(){
        return String.format("%d:%02d", getHour(), getMinute());
    }
}
