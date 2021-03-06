package com.hixos.smartwp.triggers.slideshow;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.hixos.smartwp.R;
import com.hixos.smartwp.SetWallpaperActivity;
import com.hixos.smartwp.bitmaps.ImageManager;
import com.hixos.smartwp.triggers.ServicesActivity;
import com.hixos.smartwp.triggers.WallpaperPickerFragment;
import com.hixos.smartwp.utils.FileUtils;
import com.hixos.smartwp.utils.MiscUtils;
import com.hixos.smartwp.utils.Preferences;
import com.hixos.smartwp.widget.AnimatedGridView;
import com.hixos.smartwp.widget.ArrowView;
import com.hixos.smartwp.widget.UndoBarController;

public class SlideshowFragment extends Fragment implements UndoBarController.UndoListener{
    private final static int REQUEST_SET_LIVE_WALLPAPER = 33;
    Handler handler = new Handler();
    private SlideshowDB mDatabase;
    private SlideshowDatabaseAdapter mAdapter;

    private UndoBarController mUndoBarController;
    private long mUndobarShownTimestamp = -1;
    private View mEmptyState;
    private ArrowView mArrowView;
    private boolean mEmptyStateAnimated = false;
    private AnimatedGridView mGridView;
    private boolean mSetWallpaperActivityVisible = false;

    private SlideshowPickerFragment mPickerFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        mDatabase = new SlideshowDB(getActivity());
        mDatabase.confirmDeletion();

        mPickerFragment = new SlideshowPickerFragment();
        mPickerFragment.setDatabase(mDatabase);

        FragmentManager fragmentManager = getActivity().getFragmentManager();
        fragmentManager.beginTransaction().remove(fragmentManager.findFragmentByTag("wallpaper_picker"));
        fragmentManager.beginTransaction().add(mPickerFragment, "wallpaper_picker").commit();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_slideshow, container, false);

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
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.name_slideshow);
        if (!getActivity().getIntent().getBooleanExtra(ServicesActivity.EXTRA_DISABLE_LWP_CHECK, false)) {
            checkLiveWallpaper();
        }
    }

    private void checkLiveWallpaper() {
        if (!MiscUtils.Activity.isLiveWallpaperActive(getActivity())
                && mDatabase.getWallpaperCount() > 0
                && !mSetWallpaperActivityVisible) {
            startActivityForResult(new Intent(getActivity(), SetWallpaperActivity.class),
                    REQUEST_SET_LIVE_WALLPAPER);
            mSetWallpaperActivityVisible = true;
        }
    }

    public void setAdapter() {
        mAdapter = new SlideshowDatabaseAdapter(mDatabase, getActivity());
        mAdapter.addOnWallpaperDeletedListener(new SlideshowDatabaseAdapter.OnWallpaperDeletedListener() {
            @Override
            public void onWallpaperDeleted(String uid) {
                showUndobar();
                LruCache<String, Bitmap> cache = ImageManager.getInstance().getCache();
                if (cache != null) {
                    cache.remove(uid);
                }
            }
        });
        mGridView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        SlideshowService.broadcastReloadWallpaper(getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.slideshow, menu);

        menu.findItem(R.id.action_shuffle).setChecked(mDatabase.isShuffleEnabled());
        menu.findItem(R.id.action_auto_crop).setChecked(Preferences.getBoolean(
                getActivity(), R.string.preference_auto_crop,
                getResources().getBoolean(R.bool.auto_crop_default_val)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_wallpaper:
                mPickerFragment.pickWallpaper(new WallpaperPickerFragment.OnWallpaperPickedCallback() {
                    @Override
                    public void onWallpaperPicked(String uid) {
                        hideEmptyState();
                        mAdapter.reloadDatabase();
                    }

                    @Override
                    public void onWallpaperPickFailed(String uid, int reason) {
                        FileUtils.deleteFile(ImageManager.getInstance().getPictureUri(uid));
                        if(reason != WallpaperPickerFragment.REASON_CANCELED) {
                            Toast.makeText(getActivity(), R.string.error_picture_pick_fail,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }, mDatabase.getNewUid());
                break;
            case R.id.action_pick_interval:
                mPickerFragment.pickInterval(new SlideshowPickerFragment.OnIntervalPickedCallback() {
                    @Override
                    public void onIntervalPicked(long newInterval) {

                    }
                });
                break;
            case R.id.action_settings:
                break;
            case R.id.action_shuffle: {
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                mDatabase.setShuffle(!checked);
                mGridView.setDraggable(checked);
                mAdapter.reloadDatabase();
                break;
            }
            case R.id.action_auto_crop:
                boolean checked = item.isChecked();
                item.setChecked(!checked);
                Preferences.setBoolean(getActivity(), R.string.preference_auto_crop, !checked);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SET_LIVE_WALLPAPER:
                if (resultCode != Activity.RESULT_OK) {
                    getActivity().finish();
                }
                mSetWallpaperActivityVisible = false;
                break;
        }
    }

    @Override
    public void onUndo(long token) {
        if (mDatabase != null) {
            mDatabase.undoDeletion();
        }
        mUndobarShownTimestamp = -1;
        mAdapter.reloadDatabase();
    }

    @Override
    public void onHide(long token) {
        mUndobarShownTimestamp = -1;
        if (mDatabase != null) {
            mDatabase.confirmDeletion();
        }
    }

    /*@Override
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
        }
    }*/

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
}
