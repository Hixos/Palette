package com.hixos.smartwp.triggers.timeofday;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.graphics.Palette;
import android.util.LruCache;

import com.hixos.smartwp.bitmaps.BitmapUtils;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.utils.Hour24;

import java.util.List;

/**
 * Created by Luca on 07/05/2015.
 */
public class TimeOfDayPickerFragment extends WallpaperPickerFragment {
    private int mVibrantColor, mMutedColor;
    private boolean mPickingDefault = false;

    @Override
    protected TimeOfDayDB getDatabase(){
        return (TimeOfDayDB)super.getDatabase();
    }

    protected void pickColors(String uid, final OnColorPickedCallback callback){
        BitmapUtils.generatePaletteAsync(getActivity(), uid,
                new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int defaultColor = Color.BLACK;
                        int vibrantColor = palette.getVibrantColor(defaultColor);
                        if(vibrantColor == defaultColor){
                            vibrantColor = palette.getDarkVibrantColor(defaultColor);
                            if(vibrantColor == defaultColor){
                                vibrantColor = palette.getMutedColor(defaultColor);
                            }
                        }
                        int mutedColor = palette.getMutedColor(defaultColor);
                        if(mutedColor == defaultColor){
                            mutedColor = palette.getDarkVibrantColor(defaultColor);
                            if(mutedColor == defaultColor){
                                mutedColor = palette.getDarkMutedColor(defaultColor);
                                mutedColor = Color.argb(200, Color.red(mutedColor),
                                        Color.green(mutedColor), Color.blue(mutedColor));
                            }else{
                                mutedColor = Color.argb(85, Color.red(mutedColor),
                                        Color.green(mutedColor), Color.blue(mutedColor));
                            }
                        }else{
                            mutedColor = Color.argb(127, Color.red(mutedColor),
                                    Color.green(mutedColor), Color.blue(mutedColor));
                        }
                        callback.onColorPicked(vibrantColor, mutedColor);
                    }
                });
    }

    protected void pickTimeOfDay(final String uid, int color, final OnTimeOfDayPickedCallback callback) {
        List<TimeOfDayWallpaper> wallpapers = getDatabase().getWallpapersByStartHour();

        DialogFragment dialog = TodPickerDialog.getInstance(wallpapers, color,
                new TodPickerDialog.TodPickerDialogListener() {
                    @Override
                    public void onTimePicked(Hour24 startHour, Hour24 endHour) {
                        callback.onTimeOfDayPicked(uid, startHour, endHour);
                    }

                    @Override
                    public void onCancel() {
                        callback.onTimeOfDayPickCanceled(uid);
                    }
                });
        dialog.show(getFragmentManager(), "todpicker");
    }

    @Override
    protected void reset(){
        super.reset();
        mMutedColor = 0;
        mVibrantColor = 0;
        mPickingDefault = false;
    }

    @Override
    public void onImageCropped(final Uri croppedImage) {
        pickColors(uid, new OnColorPickedCallback() {
            @Override
            public void onColorPicked(int vibrantColor, int mutedColor) {
                mVibrantColor = vibrantColor;
                mMutedColor = mutedColor;
                TimeOfDayPickerFragment.super.onImageCropped(croppedImage);
            }
        });
    }

    @Override
    protected void onImagePicked(){
        if(!mPickingDefault) {
            pickTimeOfDay(uid, mVibrantColor, new OnTimeOfDayPickedCallback() {
                @Override
                public void onTimeOfDayPicked(String uid, Hour24 startHour, Hour24 endHour) {
                    if (getDatabase().addWallpaper(uid, startHour,
                            endHour, mMutedColor, mVibrantColor) != null) {
                        success();
                    } else {
                        fail(REASON_UNKNOWN);
                    }
                }

                @Override
                public void onTimeOfDayPickCanceled(String uid) {
                    fail(REASON_CANCELED);
                }
            });
        }else{
            LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
            if (cache != null) {
                cache.remove(TimeOfDayDB.DEFAULT_WALLPAPER_UID);
            }
            success();
        }
    }

    public void pickDefaultWallpaper(OnWallpaperPickedCallback callback){
        mPickingDefault = true;
        pickWallpaper(callback, TimeOfDayDB.DEFAULT_WALLPAPER_UID);
    }

    public interface OnColorPickedCallback {
        void onColorPicked(int vibrantColor, int mutedColor);
    }

    public interface OnTimeOfDayPickedCallback {
        void onTimeOfDayPicked(String uid, Hour24 startHour, Hour24 endHour);
        void onTimeOfDayPickCanceled(String uid);
    }

}
