package com.hixos.smartwp.triggers.timeofday;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.DefaultWallpaperTile;
import com.hixos.smartwp.widget.AsyncImageView;
import com.hixos.smartwp.widget.TimeDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 24/02/2015.
 */
public class TodDatabaseAdapter extends AnimatedListAdapter{
    private final static int VIEW_TYPE_DEFAULT = 0;
    private final static int VIEW_TYPE_TOD = 1;

    private TimeOfDayDB mDatabase;
    private List<TimeOfDayWallpaper> mWallpapers;
    private DefaultWallpaperTile.DefaultWallpaperTileListener mListener;
    private Context mContext;

    public TodDatabaseAdapter(Context context, TimeOfDayDB database,
                              DefaultWallpaperTile.DefaultWallpaperTileListener listener) {
        this.mDatabase = database;
        this.mContext = context;
        mListener = listener;

        mWallpapers = new ArrayList<>();
        reloadDatabase();
    }

    @Override
    public void dragStarted(String itemId) {

    }

    @Override
    public void drag(String itemId, String beforeId) {

    }

    @Override
    public void dragEndend(String itemID) {

    }

    @Override
    public int getItemPosition(String id) {
        for (int i = 0; i < mWallpapers.size(); i++) {
            if (mWallpapers.get(i).getUid().equals(id))
                return i + 1;
        }
        return -1;
    }

    @Override
    public void removeAt(int position) {
        if (position < 1) return;

        mWallpapers.remove(position - 1);
        String uid = getItemUid(position);
        deleteWallpaper(uid);
    }

    @Override
    public void remove(String id) {
        for (TimeOfDayWallpaper data : mWallpapers) {
            if (data.getUid().equals(id)) {
                mWallpapers.remove(data);
                break;
            }
        }
        deleteWallpaper(id);
    }

    private void deleteWallpaper(final String uid){
        AsyncTask<String, Void, Void> deleteTask = new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                mDatabase.deleteWallpaper(uid);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                TimeOfDayService.broadcastReloadWallpaper(mContext);
                notifyWallpaperDeleted(uid);
            }
        };

        deleteTask.execute(uid);
    }

    @Override
    public String getItemUid(int position) {
        position -= 1;
        if (position >= 0 && position < getCount())
            return mWallpapers.get(position).getUid();
        else
            return "";
    }

    @Override
    public int getCount() {
        if (mWallpapers.size() > 0 || TimeOfDayDB.hasDefaultWallpaper()) {
            return mWallpapers.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        if(position < 1) return null;
        return mWallpapers.get(position - 1);
    }

    @Override
    public long getItemId(int position) {
        position -= 1;
        if (position >= 0 && position < mWallpapers.size())
            return mWallpapers.get(position).getUid().hashCode();
        else
            return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_DEFAULT: {
                View v = DefaultWallpaperTile
                        .inflate(mContext, !TimeOfDayDB.hasDefaultWallpaper(), mListener);
                if(TimeOfDayDB.hasDefaultWallpaper()){
                    DefaultWallpaperTile.setThumbnail(v, TimeOfDayDB.DEFAULT_WALLPAPER_UID);
                }
                return v;
            }
            default: {
                position--;
                View v;
                AsyncImageView imageThumbnail;
                TimeDisplay timeDisplayStart, timeDisplayEnd;
                FrameLayout frameTime;

                String uid = mWallpapers.get(position).getUid();

                if (convertView != null) {
                    v = convertView;
                } else {
                    v = View.inflate(mContext, R.layout.grid_item_tod, null);
                }

                ViewHolder holder = (ViewHolder) v.getTag();

                if (holder == null) {
                    imageThumbnail = (AsyncImageView) v.findViewById(R.id.image_thumbnail);
                    timeDisplayStart = (TimeDisplay) v.findViewById(R.id.timedisplay_start);
                    timeDisplayEnd = (TimeDisplay) v.findViewById(R.id.timedisplay_end);
                    frameTime = (FrameLayout) v.findViewById(R.id.frameTime);

                    holder = new ViewHolder();
                    holder.imageThumbnail = imageThumbnail;
                    holder.timeDisplayStart = timeDisplayStart;
                    holder.timeDisplayEnd = timeDisplayEnd;
                    holder.frameTime = frameTime;
                    v.setTag(holder);
                } else {
                    imageThumbnail = holder.imageThumbnail;
                    timeDisplayStart = holder.timeDisplayStart;
                    timeDisplayEnd = holder.timeDisplayEnd;
                    frameTime = holder.frameTime;
                }
                imageThumbnail.setImageUID(uid);
                TimeOfDayWallpaper wp = mWallpapers.get(position);
                timeDisplayStart.setTime(wp.getStartHour());
                timeDisplayEnd.setTime(wp.getEndHour());
                frameTime.setBackgroundColor(wp.getMutedColor());
                return v;
            }
        }
    }

    public void reloadDatabase(){

        AsyncTask<Void, Void, List<TimeOfDayWallpaper>> reloadTask
                = new AsyncTask<Void, Void, List<TimeOfDayWallpaper>>() {
            @Override
            protected List<TimeOfDayWallpaper> doInBackground(Void... params) {
                return mDatabase.getWallpapersByStartHour();
            }

            @Override
            protected void onPostExecute(List<TimeOfDayWallpaper> wallpapers) {
                mWallpapers.clear();
                mWallpapers.addAll(wallpapers);
                notifyDataSetChanged();
            }
        };

        reloadTask.execute();
    }

    @Override
    public int getItemViewType(int position) {
        return (mWallpapers.size() > 0 || TimeOfDayDB.hasDefaultWallpaper())
                && position == 0 ? VIEW_TYPE_DEFAULT : VIEW_TYPE_TOD;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }


    private class ViewHolder {
        AsyncImageView imageThumbnail;
        TimeDisplay timeDisplayStart, timeDisplayEnd;
        FrameLayout frameTime;
    }
}
