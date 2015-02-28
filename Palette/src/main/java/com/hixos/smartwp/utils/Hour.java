package com.hixos.smartwp.utils;

import android.content.Context;

import com.hixos.smartwp.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Luca on 19/02/2015.
 */
public class Hour {

    public static final int AM = 0x01;
    public static final int PM = 0x10;

    private int mHour;
    private int mMinute;
    private int mPeriod;

    public Hour() {
        mHour = 0;
        mMinute = 0;
        mPeriod = AM;
    }

    public Hour(int hour, int minute, int period) {
        mHour = hour % 12;
        mMinute = minute % 60;
        mPeriod = period == PM ? PM : AM;
    }

    public void log(String tag, String info) {
        Logger.w(tag, info + " %d:%d %s", getHour(),
                getMinute(), getPeriod() == AM ? "AM" : "PM");
    }

    public int getHour() {
        return mHour;
    }

    public void setHour(int hour) {
        hour = hour % 12;
        if(hour < 0){
            hour = 12 + hour;
        }
        this.mHour = hour;
    }

    public int getRealHour() {
        if(getPeriod() == AM)
            return mHour;
        else
            return mHour == 0 ? 12 : mHour;
    }

    public int getMilitaryHour(){
        return getHour() + (getPeriod() == PM ? 12 : 0);
    }

    public int getMinute() {
        return mMinute;
    }

    public void setMinute(int minute) {
        minute = minute % 60;
        if(minute < 0){
            minute = 60 + minute;
        }
        this.mMinute = minute;
    }

    public int getPeriod() {
        return mPeriod;
    }

    public void setPeriod(int period) {
        this.mPeriod = period;
    }


    public int compare(Hour hour) {
        int t1 = (getHour() + (getPeriod() == PM ? 12 : 0)) * 60 + getMinute();
        int t2 = (hour.getHour() + (hour.getPeriod() == PM ? 12 : 0)) * 60 + hour.getMinute();

        return t1 - t2;
    }

    public boolean equals(Hour hour) {
        return compare(hour) == 0;
    }

    public boolean equals(int hour, int minute, int period) {
        return equals(new Hour(hour, minute, period));
    }

    public void set(Hour hour) {
        mHour = hour.getHour();
        mMinute = hour.getMinute();
        mPeriod = hour.getPeriod();
    }

    public void set(int hour, int minute, int period) {
        setHour(hour);
        setMinute(minute);
        setPeriod(period);
    }

    @Override
    public String toString(){
        return String.format("%d:%d %s", mHour, mMinute, mPeriod == PM ? "PM" : "AM");
    }

    public Hour subtract(Hour hour){
        set(subtract(this, hour));
        return this;
    }

    public Hour add(Hour hour){
        set(add(this, hour));
        return this;
    }

    public Hour subtract(int minutes){
        set(subtract(this, Hour.fromMinute(minutes)));
        return this;
    }

    public Hour add(int minutes){
        set(add(this, Hour.fromMinute(minutes)));
        return this;
    }

    public static Hour subtract(Hour hour1, Hour hour2){
        return subtract(hour1, hour2.toMinutes());
    }

    public static Hour subtract(Hour hour, int minutes){
        int t1 = hour.toMinutes();

        int t = (t1 - minutes) % 1440;
        if(t < 0) t += 1440;

        return fromMinute(t);
    }

    public static Hour add(Hour hour1, Hour hour2){
        return add(hour1, toMinutes(hour2));
    }

    public static Hour add(Hour hour, int minutes){
        int t1 = hour.toMinutes();

        int t = (t1 + minutes) % 1440;
        if(t < 0) t += 1440;

        return fromMinute(t);
    }

    public static Hour fromMinute(int minutes){
        Hour out = new Hour();
        out.setMinute(minutes);
        int hour = (minutes - out.getMinute()) / 60;
        if(hour > 11){
            out.setPeriod(PM);
        }else{
            out.setPeriod(AM);
        }
        out.setHour(hour);
        return out;
    }

    public int toMinutes(){
        return toMinutes(this);
    }

    public static int toMinutes(Hour hour){
        return (hour.getHour() + (hour.getPeriod() == PM ? 12 : 0)) * 60 + hour.getMinute();
    }

    public Calendar getCalendar(Context context){
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, getMilitaryHour());
        cal.set(Calendar.MINUTE, getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Logger.e("HOUR", "Cal: " + cal.toString());
        return cal;
    }
}