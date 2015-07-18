package com.hixos.smartwp.bitmaps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.hixos.smartwp.Logger;
import com.hixos.smartwp.utils.ExternalStorageAccessException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapIO {
    private static final String LOGTAG = "BitmapIO";

    /**
     * Returns the size of the image, rotated based on getMetadataRotation(...)
     *
     * @param context Current context
     * @param image   Uri of the image
     * @return Rect
     */
    public static Rect getImageSize(Context context, Uri image) {
        return getImageSize(context, image, true);
    }

    /**
     * Returns the size of the image
     *
     * @param context  Current context
     * @param imageUri Uri of the image
     * @param rotate   If the result should be rotated according to image metadata rotation
     * @return RectF, null if error
     */
    public static Rect getImageSize(Context context, Uri imageUri, boolean rotate) {
        if (context == null) {
            throw new IllegalArgumentException("No context");
        }
        if (imageUri == null) {
            throw new IllegalArgumentException("imageUri is null");
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream inputStream = null;
        try {
            try {
                inputStream = context.getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                Logger.e(LOGTAG, "Cannot open inputStream - " + e.getMessage());
                return null;
            }
            BitmapFactory.decodeStream(inputStream, null, options);
            int width = options.outWidth;
            int height = options.outHeight;
            if (width <= 0 || height <= 0) {
                Log.e(LOGTAG, "Error decoding picture: Area is 0");
                return null;
            }
            if (rotate && getMetadataRotation(context, imageUri) % 180 == 90) {
                return new Rect(0, 0, height, width);
            } else {
                return new Rect(0, 0, width, height);
            }
        } finally {
            closeStream(inputStream);
        }
    }

    public static int getMetadataRotation(Context context, Uri uri) {
        if (context == null) {
            throw new IllegalArgumentException("No context");
        }
        if (uri == null) {
            throw new IllegalArgumentException("Uri is null");
        }

        BufferedInputStream inputStream = null;
        Metadata metadata = null;

        try {
            inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));
            metadata = ImageMetadataReader.readMetadata(inputStream, true);
            Directory directory = metadata.getDirectory(ExifIFD0Directory.class);

            int orientation = 1;
            try {
                if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                    orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (MetadataException me) {
                me.printStackTrace();
            }
            return getOrientationInDegrees(orientation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(inputStream);
        }
        return 0;
    }

    /**
     * Loads an image into memory
     *
     * @param context   Current context
     * @param bitmapUri The uri of the image to be loaded
     * @return Bitmap
     */
    public static Bitmap loadBitmap(Context context, Uri bitmapUri) {
        return loadBitmap(context, bitmapUri, true);
    }

    /**
     * Loads an image into memory
     *
     * @param context   Current context
     * @param bitmapUri The uri of the image to be loaded
     * @return Bitmap
     */
    public static Bitmap loadBitmap(Context context, Uri bitmapUri, boolean recycle) {
        Rect size = getImageSize(context, bitmapUri, true); //Image size
        return size != null
                ? loadBitmap(context, bitmapUri, size.width(), size.height(), size, recycle)
                : null;
    }

    /**
     * Returns the specified image with the exact given dimensions
     *
     * @param bitmapUri Uri of the bitmap
     * @param width     Output width
     * @param height    Output height
     * @param context   context
     * @return bitmap or null if error
     */
    public static Bitmap loadBitmap(Context context, Uri bitmapUri, int width, int height) {
        return loadBitmap(context, bitmapUri, width, height, true);
    }

    /**
     * Returns the specified image with the exact given dimensions
     *
     * @param bitmapUri Uri of the bitmap
     * @param width     Output width
     * @param height    Output height
     * @param context   context
     * @return bitmap or null if error
     */
    public static Bitmap loadBitmap(Context context, Uri bitmapUri, int width, int height,
                                    boolean recycle) {
        Rect size = getImageSize(context, bitmapUri, true);
        if (size != null) {
            Rect cropRect = RectUtils.getCropArea((float) width / (float) height, size);
            return loadBitmap(context, bitmapUri, width, height, cropRect, recycle);
        } else {
            return null;
        }

    }

    /**
     * Loads the specified image into memory, scaling it to the required size
     *
     * @param context   Current context
     * @param imageUri  Uri of the image to load
     * @param outWidth  Required output width
     * @param outHeight Required output height
     * @param cropRect  Part of the image to be loaded
     * @return Loaded bitmap with specified size. Null if error.
     */
    public static Bitmap loadBitmap(Context context, Uri imageUri, int outWidth, int outHeight,
                                    Rect cropRect) {
        return loadBitmap(context, imageUri, outWidth, outHeight, cropRect, true);

    }

    /**
     * Loads the specified image into memory, scaling it to the required size
     *
     * @param context   Current context
     * @param imageUri  Uri of the image to load
     * @param outWidth  Required output width
     * @param outHeight Required output height
     * @param cropRect  Part of the image to be loaded
     * @return Loaded bitmap with specified size. Null if error.
     */
    public static Bitmap loadBitmap(Context context, Uri imageUri, int outWidth, int outHeight,
                                    Rect cropRect, boolean recycle) {
        if (outWidth <= 0 || outHeight <= 0)
            throw new IllegalArgumentException("outWidth and outHeight must be > 0");
        if (imageUri == null) {
            throw new IllegalArgumentException("source must be defined");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must be defined");
        }
        if (cropRect == null) {
            throw new IllegalArgumentException("cropRect must be defined");
        }

        ImageManager.RecycleBin recycleBin = null;
        if (recycle) {
            recycleBin = ImageManager.getInstance().getRecycleBin();
        }

        Rect size = getImageSize(context, imageUri, true); //Image size

        if (size == null) {
            Logger.e(LOGTAG, "Couldn't get image size");
            return null;
        }

        Rect decodedCropRect = new Rect(); //Decoded image size
        InputStream inputStream = null;
        Bitmap decodedImage = null;
        Bitmap out = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        int sampleSize = RectUtils.getSampleSize(cropRect, outWidth, outHeight);
        int rotation = getMetadataRotation(context, imageUri);
        float scaleX, scaleY;
        Matrix matrix = new Matrix();

        scaleX = (float) outWidth / ((float) cropRect.width() / (float) sampleSize);
        scaleY = (float) outHeight / ((float) cropRect.height() / (float) sampleSize);

        Rect rotatedCropRect = RectUtils.rotateRect(cropRect, size, 360 - rotation);

        decodedCropRect.left = Math.round((float) rotatedCropRect.left / (float) sampleSize);
        decodedCropRect.right = Math.round((float) rotatedCropRect.right / (float) sampleSize);
        decodedCropRect.top = Math.round((float) rotatedCropRect.top / (float) sampleSize);
        decodedCropRect.bottom = Math.round((float) rotatedCropRect.bottom / (float) sampleSize);

        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
        if ((Build.VERSION.SDK_INT >= 19 || sampleSize == 1) && recycle && recycleBin != null) {
            options.inBitmap = recycleBin.restore((int) (size.width() / (float) sampleSize),
                    (int) (size.height() / (float) sampleSize), Bitmap.Config.ARGB_8888);
        }

        try {
            try {
                inputStream = context.getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException FNFE) {
                Logger.e(LOGTAG, "File not found - " + FNFE.getMessage());
                return null;
            }
            try {
                decodedImage = BitmapFactory.decodeStream(inputStream, null, options);
                if (!decodedImage.equals(options.inBitmap) && options.inBitmap != null) {
                    Logger.wtf(LOGTAG, "Decoded bitmap not the same as the reused one! :S");
                }
            } catch (IllegalArgumentException IAE) {
                Logger.e(LOGTAG, "Errore loading image, " + IAE.getMessage());
                closeStream(inputStream);
                try {
                    inputStream = context.getContentResolver().openInputStream(imageUri);
                } catch (FileNotFoundException ex) {
                    Log.e(LOGTAG, "Picture not found: " + ex.getMessage());
                    return null;
                }
                options.inBitmap = null;
                decodedImage = BitmapFactory.decodeStream(inputStream, null, options);
            }
            if (decodedImage == null) return null;

            matrix.postScale(scaleX, scaleY);
            matrix.postRotate(rotation);
            out = Bitmap.createBitmap(decodedImage, decodedCropRect.left, decodedCropRect.top,
                    decodedCropRect.width(), decodedCropRect.height(), matrix, true); //TODO: Check if rotation OK

            if (out.getWidth() != outWidth || out.getHeight() != outHeight) {
                Logger.e(LOGTAG, "Bitmap dimensions: Required w: " + outWidth + " out: " + out.getWidth()
                        + " Required h: " + outHeight + " out: " + out.getHeight());
                Logger.e(LOGTAG, "Bitmap dimensions do not match!");
                //throw new IllegalStateException("Bitmap dimensions do not match!");
            }
            return out;
        } finally {
            closeStream(inputStream);
            if (recycle && out != null && !out.equals(decodedImage) && recycleBin != null) {
                recycleBin.put(decodedImage);
            }
            matrix = null;
            options = null;
        }

    }

    public static boolean saveBitmapToFile(Context context, Bitmap bm, Uri file,
                                           Bitmap.CompressFormat compressFormat, int compressQuality)
            throws IOException, ExternalStorageAccessException, IllegalArgumentException {
        OutputStream outStream = null;
        ByteArrayOutputStream tmpOut = new ByteArrayOutputStream(2048);
        try {
            try {
                outStream = context.getContentResolver().openOutputStream(file);
            } catch (FileNotFoundException fnfe) {
                Log.e(LOGTAG, "Error opening inputStream, FILE NOT FOUND. " + fnfe.getMessage());
                return false;
            }
            if (outStream != null && bm.compress(compressFormat, compressQuality, tmpOut)) {
                try {
                    outStream.write(tmpOut.toByteArray());
                    return true;
                } catch (IOException e) {
                    Log.e(LOGTAG, "Cannot save bitmap - IO Exception " + e.getMessage());
                }
            }
        } finally {
            closeStream(outStream);
            closeStream(tmpOut);
        }
        return false;
    }

    public static boolean cropImageToFile(Context context, Uri inUri, Uri outUri, Rect trueCropRect,
                                          int outWidth, int outHeight) {
        if (inUri == null || outUri == null || trueCropRect == null || context == null) {
            Log.e(LOGTAG, "Error cropping image - One of the arguments is null");
            return false;
        }
        if (outWidth <= 0 || outHeight <= 0) {
            Log.e(LOGTAG, "Error cropping image - out width and height must be greater than 0");
            throw new IllegalArgumentException("outWidth or outHeight <= 0");
        }
        ImageManager.RecycleBin recycleBin = ImageManager.getInstance().getRecycleBin();
        Bitmap image = null;
        try {
            image = loadBitmap(context, inUri, outWidth, outHeight, trueCropRect);
            if (image != null) {
                return saveBitmapToFile(context, image, outUri, Bitmap.CompressFormat.JPEG,
                        context.getResources()
                                .getInteger(com.hixos.smartwp.R.integer.default_jpeg_quality));
            } else {
                return false;
            }

        } catch (IOException IOE) {
            Logger.e(LOGTAG, "cropImageToFile - " + IOE.getMessage());
            return false;
        } catch (ExternalStorageAccessException ESAE) {
            Logger.e(LOGTAG, "cropImageToFile - " + ESAE.getMessage());
            return false;
        } finally {
            if (image != null && recycleBin != null) {
                recycleBin.put(image);
                image = null;
            }
        }
    }

    private static int getOrientationInDegrees(int orientation) {
        switch (orientation) {
            case 3:
                return 180;
            case 6:
                return 90;
            case 8:
                return 270;
            default:
                return 0;
        }
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public interface OnImageCroppedCallback {
        public void onImageCropped(Uri imageUri);

        public void onImageCropFailed();
    }
}
