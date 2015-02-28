package com.hixos.smartwp.triggers.timeofday;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hixos.smartwp.CropperActivity;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapIO;
import com.hixos.smartwp.bitmaps.BitmapUtils;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.bitmaps.WallpaperCropper;
import com.hixos.smartwp.triggers.geofence.GeofenceDatabase;
import com.hixos.smartwp.triggers.slideshow.SlideshowService;
import com.hixos.smartwp.utils.Hour;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.widget.AnimatedGridView;
import com.hixos.smartwp.widget.ArrowView;
import com.hixos.smartwp.widget.ProgressDialogFragment;
import com.hixos.smartwp.widget.UndoBarController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 11/02/2015.
 */
public class TodFragment extends Fragment implements UndoBarController.UndoListener, TodDatabase.OnElementRemovedListener {

    private Handler handler = new Handler();

    private TodDatabase mDatabase;
    private UndoBarController mUndoBarController;
    private long mUndobarShownTimestamp = -1;
    private View mEmptyState;
    private ArrowView mArrowView;
    private boolean mEmptyStateAnimated = false;
    private AnimatedGridView mGridView;
    private TimeOfDayEditor mEditor = new TimeOfDayEditor();

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mEditor.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mDatabase = new TodDatabase(getActivity());
        mDatabase.setOnElementRemovedListener(this);
        mDatabase.clearDeletedWallpapers();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timeofday, container, false);

        mGridView = (AnimatedGridView) view.findViewById(R.id.gridView);
        setAdapter();

        int paddingTop = MiscUtils.UI.getActionBarHeight(getActivity());
        int paddingBottom = 0;
        if (MiscUtils.UI.addStatusBarPadding(getActivity())) {
            paddingTop += MiscUtils.UI.getStatusBarHeight(getActivity());
        }
        if (MiscUtils.UI.hasTranslucentNavigation(getActivity())) {
            paddingBottom += MiscUtils.UI.getNavBarHeight(getActivity());
        }

        mGridView.setPadding(mGridView.getPaddingLeft(),
                mGridView.getPaddingTop() + paddingTop,
                mGridView.getPaddingRight(),
                mGridView.getPaddingBottom() + paddingBottom);

        LinearLayout undoBar = (LinearLayout) view.findViewById(R.id.undobar);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) undoBar.getLayoutParams();
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
                params.bottomMargin + paddingBottom);

        undoBar.setLayoutParams(params);

        mUndoBarController = null;
        mUndoBarController = new UndoBarController(undoBar, this);
        restoreUndobar();

        initEmptyState(view);
        return view;
    }

    @Override
    public void onStop() {
        TimeOfDayService.broadcastReloadWallpaper(getActivity());
        super.onStop();
    }

    private void initEmptyState(View view) {
       if (mDatabase.getWallpaperCount() != 0) {
            mArrowView = null;
            mEmptyState = null;
            return;
        }

        mEmptyState = view.findViewById(R.id.emptyState);
        mEmptyState.setVisibility(View.VISIBLE);

        mArrowView = (ArrowView) view.findViewById(R.id.emptyStateArrow);
        final TextView textView = (TextView) view.findViewById(R.id.emptyStateText);

        final ViewTreeObserver viewTreeObserver = getActivity().getWindow().getDecorView().getViewTreeObserver();
        if (viewTreeObserver != null) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    View menuButton = getActivity().findViewById(R.id.action_new_wallpaper);

                    if (menuButton != null) {
                        if (viewTreeObserver.isAlive()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                viewTreeObserver.removeOnGlobalLayoutListener(this);
                            } else {
                                viewTreeObserver.removeGlobalOnLayoutListener(this);
                            }
                        }

                        int[] actionLocation = new int[2];
                        menuButton.getLocationInWindow(actionLocation);

                        int[] textLocation = new int[2];
                        textView.getLocationInWindow(textLocation);
                        final Point start = new Point(textLocation[0] + textView.getWidth() / 2,
                                textLocation[1]);
                        final Point end = new Point(actionLocation[0] + menuButton.getWidth() / 2,
                                actionLocation[1] + (int) (menuButton.getHeight() * 1.5f));

                        if (mEmptyStateAnimated) {
                            mArrowView.setPoints(start, end, false);
                        } else {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mEmptyStateAnimated = true;
                                    mArrowView.setPoints(start, end, true);
                                }
                            }, 250);
                        }
                    }
                }
            });
        }
    }

    private void hideEmptyState() {
        if (mEmptyState != null) {
            mEmptyState.setVisibility(View.GONE);
        }
    }

    private void setAdapter() {
        TodAdapter adapter = new TodAdapter(getActivity(), mDatabase);
        mGridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.timeofday, menu);

        menu.findItem(R.id.action_auto_crop).setChecked(Preferences.getBoolean(
                getActivity(), R.string.preference_auto_crop,
                getResources().getBoolean(R.bool.auto_crop_default_val)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_wallpaper:
                if(!mDatabase.isFull()){
                    mEditor.beginPickWallpaper(mDatabase.getNewUid());
                }else {
                    //TODO: show toast
                }

                break;
            case R.id.action_settings:
                break;
            case R.id.action_auto_crop:
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                Preferences.setBoolean(getActivity(), R.string.preference_auto_crop, !checked);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUndo(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {

            mDatabase.restoreWallpapersAsync();
        }
    }

    @Override
    public void onHide(long uid) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            mDatabase.clearDeletedWallpapersAsync();
        }
    }

    private void showUndobar() {
        showUndobar(0);
    }

    private void showUndobar(int elapsed) {
        mUndobarShownTimestamp = System.currentTimeMillis();
        int quantity = mDatabase.getDeletedWallpapersCount();

        String message = getResources().getQuantityString(R.plurals.wallpaper_deleted, quantity, quantity);
        mUndoBarController.showUndoBar(message, 0, elapsed);
    }

    private void restoreUndobar() {
        if (mUndobarShownTimestamp > 0) {
            int elapsed = ((int) (System.currentTimeMillis() - mUndobarShownTimestamp));
            showUndobar(elapsed);
        }
    }

    @Override
    public void onElementRemoved(String uid) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showUndobar();
            }
        });
        LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
        if (cache != null) {
            cache.remove(uid);
            cache.remove(GeofenceDatabase.getSnapshotUid(uid));
        }
    }

    private class TimeOfDayEditor implements BitmapIO.OnImageCroppedCallback {
        private static final int REQUEST_PICK_WALLPAPER = 0;
        private static final int REQUEST_PICK_CROP_WALLPAPER = 1;
        private static final int REQUEST_PICK_TIMEOFDAY = 2;

        public boolean isPicking = false;

        private String mCurrentUid;
        private int mAutoCropRequest;
        private int mMutedColor, mVibrantColor;

        private void onActivityResult(int requestCode, int resultCode, final Intent data) {
            switch (requestCode) {
                case REQUEST_PICK_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK){
                        continuePickWallpaper(data);
                    }else{
                        isPicking = false;
                    }
                    break;
                case REQUEST_PICK_CROP_WALLPAPER:
                    if (resultCode == Activity.RESULT_OK) {
                        pickTimeOfDay();
                    }else {
                        isPicking = false;
                    }
                    break;
                case REQUEST_PICK_TIMEOFDAY:
                    if(resultCode == Activity.RESULT_OK){
                        finishPickWallpaper(data);
                    }else {
                        isPicking = false;
                    }
            }
        }

        /**
         * Begins the procedure to pick a new wallpaper
         * Shows the gallery picker
         *
         * @param uid
         */
        private void beginPickWallpaper(String uid) {
            if (isPicking) return;

            isPicking = true;
            mCurrentUid = uid;

            Intent i = MiscUtils.Activity.galleryPickerIntent();
            startActivityForResult(i, REQUEST_PICK_WALLPAPER);
        }

        /**
         * First part of the procedure to pick a new wallpaper
         * Determines how to crop the wallpaper
         *
         * @param data intent passed in onActivityResult
         */
        private void continuePickWallpaper(Intent data) {
            if (data == null || data.getData() == null) {
                Toast.makeText(getActivity(), getString(R.string.error_picture_pick_fail), Toast.LENGTH_LONG).show();
                isPicking = false;
                return;
            }
            Uri image = data.getData();

            if (!Preferences.getBoolean(getActivity(), R.string.preference_auto_crop,
                    getResources().getBoolean(R.bool.auto_crop_default_val))) {
                //Manual cropping
                Intent i = new Intent(getActivity(), CropperActivity.class);
                i.putExtra(CropperActivity.EXTRA_IMAGE, image);
                i.putExtra(CropperActivity.EXTRA_OUTPUT,
                        ImageManager.getInstance().getPictureUri(mCurrentUid));
                startActivityForResult(i, REQUEST_PICK_CROP_WALLPAPER);
            } else {
                //Automatic cropping
                DialogFragment f = new ProgressDialogFragment();
                f.show(getFragmentManager(), "crop_progress");
                mAutoCropRequest = REQUEST_PICK_CROP_WALLPAPER;
                WallpaperCropper.autoCropWallpaper(getActivity(), image,
                        ImageManager.getInstance().getPictureUri(mCurrentUid), this);
            }
        }

        private void pickTimeOfDay(){
            BitmapUtils.generatePaletteAsync(getActivity(), mCurrentUid,
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
                    mMutedColor = mutedColor;
                    mVibrantColor = vibrantColor;

                    Intent intent = new Intent(getActivity(),TimePickerActivity.class);
                    List<TimeOfDayWallpaper> wallpapers = mDatabase.getOrderedWallpapers();
                    ArrayList<Parcelable> wallpapersParc = new ArrayList<>();
                    for(TimeOfDayWallpaper wp : wallpapers){
                        wallpapersParc.add(wp);
                    }
                    intent.putExtra(TimePickerActivity.EXTRA_WALLPAPERS, wallpapersParc);
                    intent.putExtra(TimePickerActivity.EXTRA_COLOR, mVibrantColor);
                    startActivityForResult(intent, REQUEST_PICK_TIMEOFDAY);
                }
            });
        }

        /**
         * Last part of the wallpaper picking procedure
         * Stores the wallpaper in the database
         */
        private void finishPickWallpaper(final Intent data) {
            Hour startHour = new Hour(data.getIntExtra(TimePickerActivity.RESULT_START_HOUR, 0),
                    data.getIntExtra(TimePickerActivity.RESULT_START_MINUTE, 0),
                    data.getIntExtra(TimePickerActivity.RESULT_START_PERIOD, Hour.AM));
            Hour endHour = new Hour(data.getIntExtra(TimePickerActivity.RESULT_END_HOUR, 0),
                    data.getIntExtra(TimePickerActivity.RESULT_END_MINUTE, 0),
                    data.getIntExtra(TimePickerActivity.RESULT_END_PERIOD, Hour.AM));
            mDatabase.createWallpaper(mCurrentUid, startHour, endHour, mMutedColor, mVibrantColor);
            isPicking = false;
            hideEmptyState();
            //checkLiveWallpaper();
        }

        @Override
        public void onImageCropped(Uri wallpaper) {
            DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("crop_progress");
            if (f != null) {
                f.dismiss();
            }
            switch (mAutoCropRequest) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    pickTimeOfDay();
                    break;
            }
        }

        @Override
        public void onImageCropFailed() {
            DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("crop_progress");
            if (f != null) {
                f.dismiss();
            }
            switch (mAutoCropRequest) {
                case REQUEST_PICK_CROP_WALLPAPER:
                    setAdapter();
                    break;
            }
            isPicking = false;
        }
    }
}
