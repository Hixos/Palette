package com.hixos.smartwp.triggers.geofence;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.graphics.Palette;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapUtils;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;

import java.util.Random;

/**
 * Created by Luca on 07/05/2015.
 */
public class GeofencePickerFragment extends WallpaperPickerFragment {
    public static final int REQUEST_PICK_GEOFENCE = 3;

    private boolean mPickingDefault = false;
    private int mColor;

    public static int getRandomAccentColor(Context context){
        int[] colors = {R.color.accent_blue, R.color.accent_green, R.color.accent_orange,
                R.color.accent_yellow, R.color.accent_purple, R.color.accent_red};
        int chosen = new Random().nextInt(colors.length);
        return context.getResources().getColor(colors[chosen]);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_PICK_GEOFENCE:
                if (resultCode == Activity.RESULT_OK)
                    onLocationPicked(data);
                else {
                    fail(REASON_CANCELED);
                }
                break;
        }
    }

    protected void pickColors(String uid, final OnColorPickedCallback callback){
        BitmapUtils.generatePaletteAsync(getActivity(), uid,
                new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int defaultColor = getRandomAccentColor(getActivity());
                        int vibrantColor = palette.getVibrantColor(defaultColor);
                        if (vibrantColor == defaultColor) {
                            vibrantColor = palette.getLightVibrantColor(defaultColor);
                            if (vibrantColor == defaultColor) {
                                vibrantColor = palette.getDarkVibrantColor(defaultColor);
                            }
                        }
                        callback.onColorPicked(vibrantColor);
                    }
                });
    }

    @Override
    public void onImageCropped(final Uri croppedImage) {
        pickColors(uid, new OnColorPickedCallback() {
            @Override
            public void onColorPicked(int color) {
                mColor = color;
                GeofencePickerFragment.super.onImageCropped(croppedImage);
            }
        });
    }

    @Override
    protected GeofenceDB getDatabase(){
        return (GeofenceDB)super.getDatabase();
    }

    @Override
    protected void onImagePicked() {
        if(mPickingDefault){
            success();
        }else {
            pickLocation();
        }

    }

    private void pickLocation(){
        Intent i = new Intent(getActivity(), GeofencePickerActivity.class);
        i.putExtra(GeofencePickerActivity.EXTRA_UID, uid);
        i.putExtra(GeofencePickerActivity.EXTRA_COLOR, mColor);
        i.putParcelableArrayListExtra(GeofencePickerActivity.EXTRA_GEOFENCES,
                getDatabase().getWallpapersByDistance());
        startActivityForResult(i, REQUEST_PICK_GEOFENCE);
    }

    private void onLocationPicked(Intent data){
        if (data == null) {
            //Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail), Toast.LENGTH_LONG).show();
            fail(REASON_UNKNOWN);
            return;
        }

        double latitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LATITUDE, 45);
        double longitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LONGITUDE, 8);
        float radius = data.getFloatExtra(GeofencePickerActivity.RESULT_RADIUS, 1);
        float zoom = data.getFloatExtra(GeofencePickerActivity.RESULT_ZOOM, 17);

        getDatabase().addWallpaper(uid, new LatLng(latitude, longitude), radius, mColor, zoom);

        success();
    }

    public void pickDefaultWallpaper(OnWallpaperPickedCallback callback){
        mPickingDefault = true;
        pickWallpaper(callback, GeofenceDB.DEFAULT_WALLPAPER_UID);
    }

    @Override
    protected void reset(){
        super.reset();
        mPickingDefault = false;
    }

    private interface OnColorPickedCallback {
        void onColorPicked(int color);
    }

}
