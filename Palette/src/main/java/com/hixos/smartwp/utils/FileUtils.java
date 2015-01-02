package com.hixos.smartwp.utils;

import android.net.Uri;
import android.os.Environment;

import java.io.File;

/**
 * Created by Luca on 17/11/13.
 */
public class FileUtils {
    private static final String LOGTAG = "FILEUTILS";

    public static boolean fileExistance(Uri file)
    {
        return new java.io.File(file.getEncodedPath()).exists();
    }

    /**
     *  Checks if external storage is available for read and write
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     *  Checks if external storage is available to at least read
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    public static void createDirectoryTree(String directory) {
        File dir = new File(directory);
        if(!dir.exists())
            dir.mkdirs();
    }

    /**
     * Deletes the file
     * @param file Uri of the file to be deleted
     * @return true if succes, false if not
     */
    public static boolean deleteFile(Uri file) {
        File f = new File(file.getEncodedPath());
        if(f.exists()) {
            return f.delete();
        }else {
            return false;
        }
    }
}
