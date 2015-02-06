package com.hixos.smartwp.bitmaps;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

public class RectUtils {

    public static int getSampleSize(Rect size, int outWidth, int outHeight) {
        int ss = 1;
        do {
            ss = ss << 1;
        } while ((float) size.width() / (float) ss > outWidth
                && (float) size.height() / (float) ss > outHeight);

        return ss >> 1;
    }

    public static Rect getCropArea(float ratio, Rect bounds) {
        float originalRatio = (float) bounds.width() / (float) bounds.height();

        Rect out = new Rect();
        if (ratio == originalRatio) {
            out.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
        } else if (ratio < originalRatio) {
            out.top = bounds.top;
            out.bottom = bounds.bottom;
            int w = (int) (bounds.height() * ratio);
            out.left = bounds.left + (int) ((bounds.width() - w) / 2f);
            out.right = out.left + w;
        } else {
            out.left = bounds.left;
            out.right = bounds.right;
            int h = (int) (bounds.width() / ratio);
            out.top = bounds.top + (int) ((bounds.height() - h) / 2f);
            out.bottom = out.top + h;
        }
        return out;
    }

    public static RectF getCropArea(float ratio, RectF bounds) {
        float originalRatio = bounds.width() / bounds.height();

        RectF out = new RectF();
        if (ratio == originalRatio) {
            out.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
        } else if (ratio < originalRatio) {
            out.top = bounds.top;
            out.bottom = bounds.bottom;
            float w = bounds.height() * ratio;
            out.left = bounds.left + (bounds.width() - w) / 2f;
            out.right = out.left + w;
        } else {
            out.left = bounds.left;
            out.right = bounds.right;
            float h = bounds.width() / ratio;
            out.top = bounds.top + (bounds.height() - h) / 2f;
            out.bottom = out.top + h;
        }
        return out;
    }

    public static Rect getFullRect(RectF relativeRect, Rect originalSize) {
        if (relativeRect == null) {
            throw new IllegalArgumentException("Argument (relativeRect) is null!");
        }
        if (originalSize == null) {
            throw new IllegalArgumentException("Argument (originalSize) is null!");
        }
        Rect out = new Rect();
        out.left = Math.round(relativeRect.left * originalSize.width() + originalSize.left);
        out.right = Math.round(relativeRect.right * originalSize.width() + originalSize.left);
        out.top = Math.round(relativeRect.top * originalSize.height() + originalSize.top);
        out.bottom = Math.round(relativeRect.bottom * originalSize.height() + originalSize.top);

        return out;
    }

    public static RectF getFullRect(RectF relativeRect, RectF originalSize) {
        if (relativeRect == null) {
            throw new IllegalArgumentException("Argument (relativeRect) is null!");
        }
        if (originalSize == null) {
            throw new IllegalArgumentException("Argument (originalSize) is null!");
        }

        RectF out = new RectF();
        out.left = relativeRect.left * originalSize.width() + originalSize.left;
        out.right = relativeRect.right * originalSize.width() + originalSize.left;
        out.top = relativeRect.top * originalSize.height() + originalSize.top;
        out.bottom = relativeRect.bottom * originalSize.height() + originalSize.top;

        return out;
    }

    public static RectF getRelativeRect(RectF rect, RectF container) {
        if (rect == null) {
            throw new IllegalArgumentException("Argument (rect) is null!");
        }
        if (container == null) {
            throw new IllegalArgumentException("Argument (container) is null!");
        }
        float left = rect.left - container.left;
        float right = rect.right - container.left;
        float top = rect.top - container.top;
        float bottom = rect.bottom - container.top;

        RectF relative = new RectF();
        relative.left = left / container.width();
        relative.right = right / container.width();
        relative.top = top / container.height();
        relative.bottom = bottom / container.height();

        return relative;
    }

    public static Rect rotateRect(Rect rect, Rect container, int rotation) {
        RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
        RectF containerF = new RectF(container.left, container.top, container.right, container.bottom);
        Rect out = new Rect();
        rotateRect(rectF, containerF, rotation).roundOut(out);
        return out;
    }

    public static RectF rotateRect(RectF rect, RectF container, int rotation) {
        RectF out = new RectF();
        float dX = 0, dY = 0;
        switch (rotation) {
            case 90:
                dX = container.height();
                break;
            case 180:
                dX = container.width();
                dY = container.height();
                break;
            case 270:
                dY = container.width();
        }

        Matrix cropMatrix = new Matrix();
        cropMatrix.postRotate(rotation);
        cropMatrix.postTranslate(dX, dY);
        cropMatrix.mapRect(out, rect);
        return out;
    }
}
