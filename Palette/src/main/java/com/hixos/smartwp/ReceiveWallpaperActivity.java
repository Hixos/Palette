package com.hixos.smartwp;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.WallpaperCropper;
import com.hixos.smartwp.triggers.ServiceUtils;
import com.hixos.smartwp.triggers.geofence.GeofenceDatabase;
import com.hixos.smartwp.triggers.geofence.GeofencePickerActivity;
import com.hixos.smartwp.triggers.geofence.GeofenceService;
import com.hixos.smartwp.triggers.slideshow.SlideshowDatabase;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.widget.ProgressDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class ReceiveWallpaperActivity extends ActionBarActivity implements View.OnClickListener {
    private static final int REQUEST_PICK_CROP_WALLPAPER = 1;
    private static final int REQUEST_PICK_GEOFENCE = 2;

    private SlideshowWpCreator mSlideshowWpCreator;
    private GeofenceWpCreator mGeofenceWpCreator;

    private int mSelectedService = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_wallpaper);
        findViewById(R.id.layout_geofence).setOnClickListener(this);
        findViewById(R.id.layout_slideshow).setOnClickListener(this);
        CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox_autocrop);
        checkBox.setOnClickListener(this);
        checkBox.setChecked(Preferences.getBoolean(this, R.string.preference_auto_crop,
                getResources().getBoolean(R.bool.auto_crop_default_val)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mSelectedService == ServiceUtils.SERVICE_SLIDESHOW) {
            mSlideshowWpCreator.onActivityResult(requestCode, resultCode, data);
        } else {
            mGeofenceWpCreator.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Uri imageUri = null;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }
        }

        switch (v.getId()) {
            case R.id.checkbox_autocrop:
                CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox_autocrop);
                Preferences.setBoolean(this, R.string.preference_auto_crop, checkBox.isChecked());
                break;
            case R.id.layout_slideshow:
                if (imageUri != null) {
                    if (mSelectedService == 0) {
                        mSelectedService = ServiceUtils.SERVICE_SLIDESHOW;
                        mSlideshowWpCreator = new SlideshowWpCreator();
                        mSlideshowWpCreator.cropWallpaper(imageUri);
                    }
                } else {
                    finish(false);
                }
                break;
            case R.id.layout_geofence:
                if (imageUri != null) {
                    if (mSelectedService == 0) {
                        mSelectedService = ServiceUtils.SERVICE_GEOFENCE;
                        mGeofenceWpCreator = new GeofenceWpCreator();
                        mGeofenceWpCreator.cropWallpaper(imageUri);
                    }
                } else {
                    finish(false);
                }
                break;
        }
    }

    public void finish(boolean success) {
        if (success) {
            String text;
            if (mSelectedService == ServiceUtils.SERVICE_SLIDESHOW) {
                text = getString(R.string.toast_add_slideshow_wallpaper_success);
            } else {
                text = getString(R.string.toast_add_geofence_wallpaper_success);
            }
            Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.toast_add_wallpaper_failed),
                    Toast.LENGTH_LONG).show();
        }
        super.finish();
    }

    private class SlideshowWpCreator implements BitmapIO.OnImageCroppedCallback {
        private Context mContext;
        private SlideshowDatabase mSlideshowDatabase;
        private String mCurrentUid;

        public SlideshowWpCreator() {
            mContext = ReceiveWallpaperActivity.this;
            mSlideshowDatabase = new SlideshowDatabase(mContext);
        }

        public void cropWallpaper(Uri imageUri) {
            mCurrentUid = mSlideshowDatabase.getNewUid();

            if (!Preferences.getBoolean(mContext, R.string.preference_auto_crop,
                    getResources().getBoolean(R.bool.auto_crop_default_val))) {
                //Manual cropping
                Intent i = new Intent(mContext, CropperActivity.class);
                i.putExtra(CropperActivity.EXTRA_IMAGE, imageUri);
                i.putExtra(CropperActivity.EXTRA_OUTPUT,
                        ImageManager.getInstance().getPictureUri(mCurrentUid));
                startActivityForResult(i, REQUEST_PICK_CROP_WALLPAPER);
            } else {
                //Automatic cropping
                ProgressDialogFragment f = new ProgressDialogFragment();
                f.show(getFragmentManager(), "crop_progress");
                WallpaperCropper.autoCropWallpaper(mContext, imageUri,
                        ImageManager.getInstance().getPictureUri(mCurrentUid),
                        this);
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    if (resultCode == RESULT_OK) {
                        onImageCropped(null);
                    } else if (resultCode == CropperActivity.RESULT_ERROR) {
                        finish(false);
                    } else {
                        finish();
                    }
            }
        }

        @Override
        public void onImageCropped(Uri croppedImage) {
            mSlideshowDatabase.createWallpaper(mCurrentUid);
            finish(true);
        }

        @Override
        public void onImageCropFailed() {
            finish(false);
        }
    }

    private class GeofenceWpCreator implements BitmapIO.OnImageCroppedCallback {
        private Context mContext;
        private GeofenceDatabase mGeofenceDatabase;
        private String mCurrentUid;

        public GeofenceWpCreator() {
            mContext = ReceiveWallpaperActivity.this;
            mGeofenceDatabase = new GeofenceDatabase(mContext);
        }

        public void cropWallpaper(Uri imageUri) {
            mCurrentUid = mGeofenceDatabase.getNewUid();

            if (!Preferences.getBoolean(mContext, R.string.preference_auto_crop,
                    getResources().getBoolean(R.bool.auto_crop_default_val))) {
                //Manual cropping
                Intent i = new Intent(mContext, CropperActivity.class);
                i.putExtra(CropperActivity.EXTRA_IMAGE, imageUri);
                i.putExtra(CropperActivity.EXTRA_OUTPUT,
                        ImageManager.getInstance().getPictureUri(mCurrentUid));
                startActivityForResult(i, REQUEST_PICK_CROP_WALLPAPER);
            } else {
                //Automatic cropping
                DialogFragment f = new ProgressDialogFragment();
                f.show(getFragmentManager(), "crop_progress");
                WallpaperCropper.autoCropWallpaper(mContext, imageUri,
                        ImageManager.getInstance().getPictureUri(mCurrentUid),
                        this);
            }
        }

        private void pickLocation() {
            Intent i = new Intent(mContext, GeofencePickerActivity.class);
            i.putExtra(GeofencePickerActivity.EXTRA_UID, mCurrentUid);
            i.putExtra(GeofencePickerActivity.EXTRA_COLOR, mGeofenceDatabase.getLeastUsedColor());
            i.putParcelableArrayListExtra(GeofencePickerActivity.EXTRA_GEOFENCES,
                    mGeofenceDatabase.getGeofencesByDistance());
            startActivityForResult(i, REQUEST_PICK_GEOFENCE);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    if (resultCode == RESULT_OK) {
                        onImageCropped(null);
                    } else if (resultCode == CropperActivity.RESULT_ERROR) {
                        finish(false);
                    } else {
                        finish();
                    }
                    break;
                case REQUEST_PICK_GEOFENCE:
                    if (resultCode == RESULT_OK) {
                        double latitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LATITUDE, 45);
                        double longitude = data.getDoubleExtra(GeofencePickerActivity.RESULT_LONGITUDE, 8);
                        float radius = data.getFloatExtra(GeofencePickerActivity.RESULT_RADIUS, 1);
                        float distance = data.getFloatExtra(GeofencePickerActivity.RESULT_DISTANCE, -1);
                        float zoom = data.getFloatExtra(GeofencePickerActivity.RESULT_ZOOM, 17);

                        mGeofenceDatabase.createGeowallpaper(mCurrentUid, new LatLng(latitude, longitude), radius,
                                mGeofenceDatabase.getLeastUsedColor(), distance, zoom);
                        List<String> uid = new ArrayList<>();
                        uid.add(mCurrentUid);

                        GeofenceService.broadcastAddGeofence(mContext, uid);
                        finish(true);
                    } else {
                        finish();
                    }
            }
        }

        @Override
        public void onImageCropped(Uri croppedImage) {
            pickLocation();
        }

        @Override
        public void onImageCropFailed() {
            finish(false);
        }
    }
}
