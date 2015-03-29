package com.hixos.smartwp.triggers.geofence;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;
import com.hixos.smartwp.utils.DefaultWallpaperTile;
import com.hixos.smartwp.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.List;

public class GeofenceDatabaseAdapter extends AnimatedListAdapter implements GeofenceDatabase.DatabaseObserver {
    private final static int VIEW_TYPE_DEFAULT = 0;
    private final static int VIEW_TYPE_GEOFENCE = 1;
    private GeofenceDatabase mDatabase;
    private List<GeofenceData> mData;
    private DefaultWallpaperTile.DefaultWallpaperTileListener mListener;
    private Context mContext;

    public GeofenceDatabaseAdapter(Context context, GeofenceDatabase database,
                                   DefaultWallpaperTile.DefaultWallpaperTileListener listener){
        this.mContext = context;

        mDatabase = database;
        mListener = listener;
        mDatabase.setDatabaseObserver(this);

        mData = new ArrayList<>();
        reloadData();
    }

    @Override
    public int getCount() {
        if (mData.size() > 0 || GeofenceDatabase.hasDefaultWallpaper()) {
            return mData.size() + 1;
        } else {
            return 0;
        }
    }

    @Override
    public GeofenceData getItem(int i) {
        if (i < 1) return null;

        return mData.get(i - 1);
    }

    @Override
    public long getItemId(int i) {
        i -= 1;
        if (i >= 0 && i < mData.size())
            return mData.get(i).getUid().hashCode();
        else
            return -1;
    }

    @Override
    public String getItemUid(int i) {
        i -= 1;
        if (i >= 0 && i < getCount())
            return mData.get(i).getUid();
        else
            return "";
    }

    @Override
    public int getItemPosition(String id) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getUid().equals(id))
                return i + 1;
        }
        return -1;
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
    public void removeAt(int position) {
        if (position < 1) return;

        mData.remove(position - 1);
        String uid = getItemUid(position);
        mDatabase.adapterDeleteGeofence(uid);
    }

    @Override
    public void remove(String id) {
        for (GeofenceData data : mData) {
            if (data.getUid().equals(id)) {
                mData.remove(data);
                break;
            }
        }
        mDatabase.adapterDeleteGeofence(id);
    }

    @Override
    public void onElementRemoved(String uid) {
        for (GeofenceData data : mData) {
            if (data.getUid().equals(uid)) {
                mData.remove(data);
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onElementCreated(GeofenceData element) {
        reloadData();
    }

    @Override
    public void onDataSetChanged() {
        reloadData();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        switch (getItemViewType(i)) {
            case VIEW_TYPE_DEFAULT: {
                return DefaultWallpaperTile
                        .inflate(mContext, GeofenceDatabase.hasDefaultWallpaper(), mListener);
            }
            default: {
                i -= 1;

                View v;
                AsyncImageView thumbnail;
                AsyncImageView snapshot;

                String uid = mData.get(i).getUid();

                if (convertView != null) {
                    v = convertView;
                } else {
                    v = View.inflate(mContext, R.layout.grid_item_geofence, null);
                }

                ViewHolder holder = (ViewHolder) v.getTag();

                if (holder == null) {
                    thumbnail = (AsyncImageView) v.findViewById(R.id.image_thumbnail);
                    snapshot = (AsyncImageView) v.findViewById(R.id.image_snapshot);
                    holder = new ViewHolder();
                    holder.Snapshot = snapshot;
                    holder.Thumbnail = thumbnail;
                    v.setTag(holder);
                } else {
                    thumbnail = holder.Thumbnail;
                    snapshot = holder.Snapshot;
                }
                thumbnail.setImageUID(uid);
                snapshot.setImageUID(GeofenceDatabase.getSnapshotUid(uid));
                return v;
            }
        }
    }

    public void reloadData() {
        mData.clear();
        mDatabase.getGeofencesByDistanceAsync(new GeofenceDatabase.OnWallpapersLoadedListener() {
            @Override
            public void onWallpapersLoaded(List<GeofenceData> wallpapers) {
                mData.addAll(wallpapers);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return (mData.size() > 0 || GeofenceDatabase.hasDefaultWallpaper())
                && position == 0 ? VIEW_TYPE_DEFAULT : VIEW_TYPE_GEOFENCE;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private class ViewHolder {
        public AsyncImageView Thumbnail;
        public AsyncImageView Snapshot;
    }
}
