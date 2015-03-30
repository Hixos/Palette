package com.hixos.smartwp.bitmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.v7.graphics.Palette;



public class BitmapUtils {

    public static Bitmap resizeBitmap(Bitmap bitmap, int width, int height) {
        Rect size = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect rect = RectUtils.getCropArea((float) width / (float) height, size);
        Matrix matrix = new Matrix();
        matrix.postScale((float) width / (float) rect.width(), (float) height / (float) rect.height());
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(),
                matrix, true);
    }

    public static void generatePaletteAsync(final Context context, final String uid, final Palette.PaletteAsyncListener listener){
        AsyncTask<String, Void, Palette> asyncTask = new AsyncTask<String, Void, Palette>() {
            @Override
            protected Palette doInBackground(String... params) {
                return generatePalette(context, uid);
            }

            @Override
            protected void onPostExecute(Palette palette) {
                super.onPostExecute(palette);
                listener.onGenerated(palette);
            }
        };
        asyncTask.execute(uid);
    }

    public static Palette generatePalette(final Context context, final String uid){
        Bitmap bitmap = BitmapIO.loadBitmap(context,
                ImageManager.getInstance().getPictureUri(uid), 500,500, false);
        return Palette.generate(bitmap);
    }
}
