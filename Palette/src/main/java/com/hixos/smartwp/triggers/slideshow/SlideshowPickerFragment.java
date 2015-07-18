package com.hixos.smartwp.triggers.slideshow;

import android.app.Activity;
import android.content.Intent;

import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.utils.MiscUtils;

/**
 * Created by Luca on 02/04/2015.
 */
public class SlideshowPickerFragment extends WallpaperPickerFragment {
    protected static final int REQUEST_PICK_INTERVAL = 2;

    public interface OnIntervalPickedCallback {
        public void onIntervalPicked(long newInterval);
    }

    protected OnIntervalPickedCallback intervalCallback;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_PICK_INTERVAL:
                if(resultCode == Activity.RESULT_OK){
                    finishPickInterval(data);
                }else {
                    reset();
                }
        }
    }

    @Override
    protected void onImagePicked() {
        getDatabase().createWallpaper(uid);
        success();
    }

    @Override
    protected SlideshowDatabase getDatabase(){
        return (SlideshowDatabase)super.getDatabase();
    }

    /**
     * Displays interval picker activity
     */
    public void pickInterval(OnIntervalPickedCallback callback) {
        if(isPicking) return; //TODO: log error

        intervalCallback = callback;
        isPicking = true;
        Intent i = new Intent(getActivity(), IntervalPickerActivty.class);
        i.putExtra(IntervalPickerActivty.EXTRA_INTERVAL, getDatabase().getIntervalMs());
        startActivityForResult(i, REQUEST_PICK_INTERVAL);
    }

    /**
     * Updates the interval in the database
     *
     * @param data
     */
    private void finishPickInterval(Intent data) {
        if (data == null) {
            return;
        }
        long newInterval = data.getLongExtra(IntervalPickerActivty.RESULT_INTERVAL, 10 * 60 * 1000);
        getDatabase().setIntervalMs(newInterval);
        SlideshowService.broadcastIntervalChanged(getActivity());
        intervalCallback.onIntervalPicked(newInterval);
        reset();
    }
}
