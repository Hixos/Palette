package com.hixos.smartwp.bitmaps;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

public class ImageLoadTask extends AsyncTask<Uri, Void, Bitmap> {
    private String mUid;
    private int mWidth, mHeight;

    private Context mContext;

    private ImageManager.OnImageLoadedListener mListener;

    public ImageLoadTask(Context context, int reqWidth, int reqHeight, String id,
                         ImageManager.OnImageLoadedListener listener) {
        this.mListener = listener;
        this.mWidth = reqWidth;
        this.mHeight = reqHeight;
        this.mContext = context.getApplicationContext();
        this.mUid = id;
    }

    @Override
    protected Bitmap doInBackground(Uri... uris) {
        return BitmapIO.loadBitmap(mContext, uris[0], mWidth, mHeight);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (mListener != null) {
            mListener.onImageLoaded(bitmap, mUid);
        }
        mListener = null;
    }

    @Override
    protected void onCancelled(Bitmap bitmap) {
        if (bitmap != null) {
            ImageManager.getInstance().getRecycleBin().put(bitmap);
        }
        mListener = null;
    }
}
