package com.hixos.smartwp.bitmaps;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * Created by Luca on 16/11/13.
 */
public class BitmapUtils {
    public static Bitmap resizeBitmap(Bitmap bitmap, int width, int height)
    {
        Rect size = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect rect = RectUtils.getCropArea((float)width / (float)height, size);
        Matrix matrix = new Matrix();
        matrix.postScale((float)width / (float)rect.width(), (float)height / (float)rect.height());
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(),
                matrix, true);
    }
}
