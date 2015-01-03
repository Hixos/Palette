package com.hixos.smartwp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.WallpaperCropper;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.wallpaper.WallpaperUtils;
import com.hixos.smartwp.widget.CropView;


public class CropperActivity extends ActionBarActivity implements BitmapIO.OnImageCroppedCallback, ImageManager.OnImageLoadedListener {
    public final static String EXTRA_IMAGE = "com.hixos.smartwp.EXTRA_IMAGE";
    public final static String EXTRA_OUTPUT = "com.hixos.smartwp.EXTRA_OUTPUT";
    public static final int RESULT_ERROR = 2;
    private CropView mCropView;
    private CropModeSpinnerAdapter mSpinnerAdapter;
    private FrameLayout mProgressLayout;
    private Uri mInUri, mOutUri;
    private int mSelectedCropMode = 0;
    private View mDoneButton;
    private boolean mCropping = false;
    private boolean mImageLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_button, null);
        mDoneButton = customActionBarView.findViewById(R.id.actionbar_done);
        final ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView);

        setContentView(R.layout.activity_cropper);

        int top, bottom = 0;
        top = MiscUtils.UI.getActionBarHeight(this);

        if (getResources().getBoolean(R.bool.has_translucent_navbar)) {

            bottom += MiscUtils.UI.getNavBarHeight(this);
        }

        if (getResources().getBoolean(R.bool.has_translucent_statusbar)) {
            top += MiscUtils.UI.getStatusBarHeight(this);
        }
        FrameLayout root = (FrameLayout) findViewById(R.id.root);
        root.setPadding(root.getPaddingLeft(),
                root.getPaddingTop() + top,
                root.getPaddingRight(),
                root.getPaddingBottom() + bottom);


        mProgressLayout = (FrameLayout) findViewById(R.id.progressLayout);

        mSpinnerAdapter = new CropModeSpinnerAdapter(this);

        mCropView = (CropView) findViewById(R.id.cropView);

        showProgressLayout(true);

        if (savedInstanceState != null) {
            mCropping = savedInstanceState.getBoolean("cropping");
            if (!mCropping) {
                mCropView.onRestoreInstanceState(savedInstanceState);
            }
            mSelectedCropMode = savedInstanceState.getInt("selected_crop_mode");
        } else {
            mSelectedCropMode = Preferences.getInt(this, R.string.preference_crop_mode,
                    MiscUtils.usingGoogleNowLauncher(this) ? 1 : 0);
        }

        mOutUri = getIntent().getExtras().getParcelable(EXTRA_OUTPUT);
        mInUri = getIntent().getExtras().getParcelable(EXTRA_IMAGE);

        if (!mCropping) {
            if (mInUri != null) {
                mCropView.setImageURI(mInUri, this);
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCropView.onSaveInstanceState(outState);
        outState.putInt("selected_crop_mode", mSelectedCropMode);
        outState.putBoolean("cropping", mCropping);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cropper, menu);
        MenuItem cancelItem = menu.findItem(R.id.action_cancel);
        MenuItem spinnerItem = menu.findItem(R.id.action_crop_mode);


        Spinner spinner = (Spinner) spinnerItem.getActionView();
        spinner.setAdapter(mSpinnerAdapter);
        spinner.setSelection(mSelectedCropMode);

        // spinner.setPopupBackgroundResource(R.drawable.spinner_material_popup_background);

        if (mImageLoaded && !mCropping) {
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    mCropView.setCropMode(mSpinnerAdapter.getItem(i));
                    mSelectedCropMode = i;
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        if (!mImageLoaded || mCropping) {
            cancelItem.setEnabled(false);
            spinner.setEnabled(false);
        } else {
            cancelItem.setEnabled(true);
            spinner.setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_cancel:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cropWallpaper() {
        mCropping = true;
        invalidateOptionsMenu();
        mDoneButton.setOnClickListener(null);
        showProgressLayout(false);
        mCropView.setEnabled(false);
        RectF relativeCrop = mCropView.getRelativeCropRect();
        Point screen = MiscUtils.UI.getDisplaySize(getApplicationContext());

        int outHeight = mCropView.getCropMode() == CropView.CROP_MODE_LANDSCAPE ?
                WallpaperUtils.getLandscapeWallpaperSize(this).y
                : Math.max(screen.x, screen.y);


        Preferences.setInt(this, R.string.preference_crop_mode, mSelectedCropMode);
        WallpaperCropper.cropWallpaper(mInUri, mOutUri, relativeCrop, outHeight,
                this, getApplicationContext());
    }

    @Override
    public void onImageCropped(Uri croppedImage) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onImageCropFailed() {
        setResult(RESULT_ERROR);
        finish();
    }

    private void showProgressLayout(boolean hideCrop) {
        int animationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        mProgressLayout.setVisibility(View.VISIBLE);
        if (hideCrop) {
            mCropView.setVisibility(View.INVISIBLE);
        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(mProgressLayout, "alpha", 0, 1);
        animator.setDuration(animationDuration);
        animator.start();
    }

    private void hideProgressLayout() {
        int animationDuration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

        ObjectAnimator animatorCrop = ObjectAnimator.ofFloat(mCropView, "alpha", 1);
        animatorCrop.setDuration(animationDuration);
        animatorCrop.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCropView.setVisibility(View.VISIBLE);
            }
        });
        animatorCrop.start();

        ObjectAnimator animator = ObjectAnimator.ofFloat(mProgressLayout, "alpha", 0);
        animator.setDuration(animationDuration);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mProgressLayout.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    @Override
    public void onImageLoaded(Bitmap image, String id) {
        mImageLoaded = true;
        hideProgressLayout();
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropWallpaper();
            }
        });
        invalidateOptionsMenu();
    }

    private static class CropModeSpinnerAdapter extends BaseAdapter {

        private Context mContext;

        public CropModeSpinnerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Integer getItem(int i) {
            switch (i) {
                default:
                case 0:
                    return CropView.CROP_MODE_DEFAULT;
                case 1:
                    return CropView.CROP_MODE_KITKAT;
                case 2:
                    return CropView.CROP_MODE_PORTRAIT;
                case 3:
                    return CropView.CROP_MODE_LANDSCAPE;
            }
        }

        public String getText(int i) {
            switch (i) {
                default:
                case 0:
                    return mContext.getString(R.string.crop_full);
                case 1:
                    return mContext.getString(R.string.crop_gel);
                case 2:
                    return mContext.getString(R.string.crop_port);
                case 3:
                    return mContext.getString(R.string.crop_land);
            }
        }

        public int getIcon(int i) {
            switch (i) {
                default:
                case 0:
                    return R.drawable.ic_crop_full;
                case 1:
                    return R.drawable.ic_crop_gel;
                case 2:
                    return R.drawable.ic_crop_port;
                case 3:
                    return R.drawable.ic_crop_land;
            }
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View v = inflater.inflate(R.layout.spinner_item_cropper, null);
            TextView t = (TextView) v.findViewById(R.id.textView);
            t.setText(getText(i));
            t.setCompoundDrawablesWithIntrinsicBounds(getIcon(i), 0, 0, 0);
            return v;
        }

        @Override
        public View getDropDownView(int i, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View v = inflater.inflate(R.layout.spinner_item_cropper_dropdown, null);
            TextView t = (TextView) v.findViewById(R.id.textView);
            t.setText(getText(i));
            t.setCompoundDrawablesWithIntrinsicBounds(getIcon(i), 0, 0, 0);
            return v;
        }
    }
}


