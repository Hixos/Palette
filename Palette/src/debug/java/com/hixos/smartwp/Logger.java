package com.hixos.smartwp;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
        w(logtag, String.format(message, args));
    }

    public static void e(String logtag, String message){
        Log.e(logtag, message);
    }

    public static void wtf(String logtag, String message){
        Log.wtf(logtag, message);
    }

    public static void d(String message, Object... args){
        w("DEBUG", message, args);
    }

    public static void logRect(String logtag, String text, Rect r){
        float ratio = r.height() == 0 ? 0 : (float)r.width() / (float)r.height();
        w(logtag, String.format("%s(Left: %d, Top: %d, Width: %d, Height: %d, Ratio %f)", text, r.left, r.top, r.width(), r.height(), ratio));
    }

    public static void logRectSides(String logtag, String text, Rect r) {
        w(logtag, String.format("%s(Left: %d, Top: %d, Right: %d, Bottom: %d)", text, r.left, r.top, r.right, r.bottom));
    }

    public static void logRect(String logtag, String text, RectF r){
        float ratio = r.height() == 0 ? 0 : r.width() / r.height();
        w(logtag, String.format("%s(Left: %f, Top: %f, Width: %f, Height: %f, Ratio %f)",text, r.left, r.top, r.width(), r.height(), ratio));
    }


    public static void fileW(Context c, String tag, String text){
        Log.w(tag, text);
        String date = android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", new java.util.Date()).toString();
        FileWriter out = null;
        try {
            File f = new File(getLogFilePath(c));
            out = new FileWriter(f, true);
            out.append(String.format("%s\t%s\t%s", date, tag, text));

            out.append(String.format("\n"));
            out.close();
        }
        catch (IOException e) {
            Log.e(LOGTAG, "File write failed: " + e.toString());
        }finally {
            if(out != null){
                try{
                    out.close();
                }catch (IOException e) {
                    Log.e(LOGTAG, "Error closing writer: " + e.toString());
                }

            }
        }
    }

    public static void fileW(Context c, String tag, String text, Object... args){
        fileW(c, tag, String.format(text, args));
    }

    private static String getLogFilePath(Context c){
        return c.getExternalFilesDir(null).getPath() + "/" + LOG_FILE;
    }
}

