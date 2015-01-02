package com.hixos.smartwp.widget;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.io.InputStream;

/**
 * Created by Luca on 02/03/14.
 */
public class AlphaBitmapDrawable extends BitmapDrawable {
    private int mAlpha = 255;

    public AlphaBitmapDrawable(Resources res, InputStream is) {
        super(res, is);
    }

    public AlphaBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    public AlphaBitmapDrawable(Resources res, String filepath) {
        super(res, filepath);
    }

    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
        mAlpha = alpha;
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }
}
