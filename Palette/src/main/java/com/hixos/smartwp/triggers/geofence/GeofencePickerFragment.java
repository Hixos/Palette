package com.hixos.smartwp.triggers.geofence;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.triggers.timeofday.TodDatabase;

/**
 * Created by Luca on 07/05/2015.
 */
public class GeofencePickerFragment extends WallpaperPickerFragment {
    public static final int REQUEST_PICK_GEOFENCE = 3;

    private boolean mPickingDefault = false;

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

    @Override
    protected GeofenceDatabase getDatabase(){
        return (GeofenceDatabase)super.getDatabase();
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
        i.putExtra(GeofencePickerActivity.EXTRA_COLOR, getDatabase().getLeastUsedColor());
        i.putParcelableArrayListExtra(GeofencePickerActivity.EXTRA_GEOFENCES,
                getDatabase().getGeofencesByDistance());
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
        float distance = data.getFloatExtra(GeofencePickerActivity.RESULT_DISTANCE, -1);
        float zoom = data.getFloatExtra(GeofencePickerActivity.RESULT_ZOOM, 17);

        getDatabase().createGeowallpaper(uid, new LatLng(latitude, longitude), radius,
                getDatabase().getLeastUsedColor(), distance, zoom);

        success();
    }

    public void pickDefaultWallpaper(OnWallpaperPickedCallback callback){
        mPickingDefault = true;
        pickWallpaper(callback, GeofenceDatabase.DEFAULT_WALLPAPER_UID);
    }

    @Override
    protected void reset(){
        super.reset();
        mPickingDefault = false;
    }

}
