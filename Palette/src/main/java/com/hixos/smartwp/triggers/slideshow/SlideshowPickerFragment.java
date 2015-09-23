package com.hixos.smartwp.triggers.slideshow;

import android.app.Activity;
import android.content.Intent;

import com.hixos.smartwp.Logger;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;

/**
 * Created by Luca on 02/04/2015.
 */
public class SlideshowPickerFragment extends WallpaperPickerFragment {
    protected static final int REQUEST_PICK_INTERVAL = 2;
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
        getDatabase().addWallpaper(uid);
        success();
    }

    @Override
    protected SlideshowDB getDatabase(){
        return (SlideshowDB)super.getDatabase();
    }

    /**
     * Displays interval picker activity
     */
    public void pickInterval(OnIntervalPickedCallback callback) {
        if(isPicking) {
            Logger.e("SlideshowPicker", "isPicking is already true!");
            return;
        }

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

    public interface OnIntervalPickedCallback {
        void onIntervalPicked(long newInterval);
    }
}
