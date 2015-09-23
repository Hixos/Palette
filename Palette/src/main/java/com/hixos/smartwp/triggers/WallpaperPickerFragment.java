package com.hixos.smartwp.triggers;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.hixos.smartwp.CropperActivity;
import com.hixos.smartwp.DatabaseManager;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.WallpaperCropper;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.widget.ProgressDialogFragment;

public abstract class WallpaperPickerFragment extends Fragment
        implements BitmapIO.OnImageCroppedCallback {

    public final static int REASON_UNKNOWN = 0;
    public final static int REASON_CANCELED = 1;
    public final static int REASON_IMAGE_PICK_FAILED = 2;

    public interface OnWallpaperPickedCallback {
        void onWallpaperPicked(String uid);
        void onWallpaperPickFailed(String uid, int reason);
    }

    public static final String ARGUMENT_AUTO_CROP = "auto_crop";

    protected static final int REQUEST_PICK_WALLPAPER = 0;
    protected static final int REQUEST_CROP_WALLPAPER = 1;

    protected boolean isPicking = false;
    protected String uid;

    private OnWallpaperPickedCallback mWallpaperCallback;

    protected boolean autoCrop = false;

    private DatabaseManager mDatabase;

    public boolean isPicking() {
        return isPicking;
    }

    public String getUid() {
        return uid;
    }

    public boolean getAutoCrop() {
        return autoCrop;
    }

    public void setAutoCrop(boolean autoCrop) {
        this.autoCrop = autoCrop;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_PICK_WALLPAPER:
                if(resultCode == Activity.RESULT_OK){
                    cropPicture(data);
                }else{
                    onPicturePickFailed(false);
                }
                break;
            case REQUEST_CROP_WALLPAPER:
                if(resultCode == Activity.RESULT_OK){
                    onImageCropped(ImageManager.getInstance().getPictureUri(uid));
                }else{
                    onPicturePickFailed(false);
                }
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        autoCrop = args.getBoolean(ARGUMENT_AUTO_CROP, true);
    }

    public void setDatabase(DatabaseManager database){
        mDatabase = database;
    }

    public final void pickWallpaper(OnWallpaperPickedCallback callback, String uid){
        mWallpaperCallback = callback;
        isPicking = true;
        this.uid = uid;
        pickWallpaper();
    }

    protected void pickWallpaper(){
        pickPicture();
    }

    protected void pickPicture(){
        Intent i = MiscUtils.Activity.galleryPickerIntent();
        startActivityForResult(i, REQUEST_PICK_WALLPAPER);
    }

    protected void cropPicture(Intent data){
        if (data == null || data.getData() == null) {
            onPicturePickFailed(true);
            return;
        }
        Uri image = data.getData();

        if (!autoCrop) {
            //Manual cropping
            Intent i = new Intent(getActivity(), CropperActivity.class);
            i.putExtra(CropperActivity.EXTRA_IMAGE, image);
            i.putExtra(CropperActivity.EXTRA_OUTPUT,
                    ImageManager.getInstance().getPictureUri(uid));
            startActivityForResult(i, REQUEST_CROP_WALLPAPER);
        } else {
            //Automatic cropping
            DialogFragment f = new ProgressDialogFragment();
            f.show(getFragmentManager(), "crop_progress");
            WallpaperCropper.autoCropWallpaper(getActivity(), image,
                    ImageManager.getInstance().getPictureUri(uid), this);
        }
    }

    @Override
    public void onImageCropped(Uri croppedImage) {
        DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("crop_progress");
        if (f != null) {
            f.dismiss();
        }
        onImagePicked();
    }

    @Override
    public void onImageCropFailed() {
        onPicturePickFailed(true);
    }

    protected void onPicturePickFailed(boolean showToast){
        if(showToast) {
            Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail),
                    Toast.LENGTH_LONG).show();
        }
        fail(REASON_IMAGE_PICK_FAILED);
    }

    protected void onImagePicked(){
        success();
    }

    protected DatabaseManager getDatabase(){
        return mDatabase;
    }

    protected void reset(){
        isPicking = false;
        uid = null;
    }

    protected final void success(){
        mWallpaperCallback.onWallpaperPicked(uid);
        reset();
    }

    protected final void fail(int reason){
        mWallpaperCallback.onWallpaperPickFailed(uid, reason);
        reset();
    }
}
