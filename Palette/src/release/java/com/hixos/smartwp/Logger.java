package com.hixos.smartwp;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

/**
 * Created by Luca on 13/01/14.
 */
public final class Logger {
    public static final String LOG_FILE = "log.txt";
    private static final String LOGTAG = "LogUtils";

    public static void w(String logtag, String message){
        Log.w(logtag, message);
    }

    public static void w(String logtag, String message, Object... args){
        String text = message + " (";
        boolean first = true;
        for(Object o : args){
            text = text + (first ? o.toString() : ", " + o.toString());
            first = false;
        }
        text = text + ")";

        w(logtag, text);
    }

    public static void e(String logtag, String message){
        Log.w(logtag, message);
    }

    public static void wtf(String logtag, String message){
        Log.w(logtag, message);
    }

    public static void logRect(String logtag, String text, Rect r){
        float ratio = r.height() == 0 ? 0 : (float)r.width() / (float)r.height();
        w(logtag, String.format("%s(Left: %d, Top: %d, Width: %d, Height: %d, Ratio %f)", text, r.left, r.top, r.width(), r.height(), ratio));
    }

    public static void logRect(String logtag, String text, RectF r){
        float ratio = r.height() == 0 ? 0 : r.width() / r.height();
        w(logtag, String.format("%s(Left: %f, Top: %f, Width: %f, Height: %f, Ratio %f)",text, r.left, r.top, r.width(), r.height(), ratio));
    }

    public static void fileW(Context c, String tag, String text){

    }

    public static void fileW(Context c, String tag, String text, Object... args) {

    }

    public static void fileNewLine(Context c){

    }

    private static String getLogFilePath(Context c){
        return c.getExternalFilesDir(null).getPath() + "/" + LOG_FILE;
    }
}

