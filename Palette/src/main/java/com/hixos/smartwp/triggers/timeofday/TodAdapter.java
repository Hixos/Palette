package com.hixos.smartwp.triggers.timeofday;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;
import com.hixos.smartwp.bitmaps.BitmapUtils;
import com.hixos.smartwp.triggers.geofence.GeofenceData;
import com.hixos.smartwp.triggers.geofence.GeofenceDatabase;
import com.hixos.smartwp.utils.Hour;
import com.hixos.smartwp.widget.AsyncImageView;
import com.hixos.smartwp.widget.FontTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 24/02/2015.
 */
public class TodAdapter extends AnimatedListAdapter implements TodDatabase.DatabaseObserver {

    private TodDatabase mDatabase;
    private List<TimeOfDayWallpaper> mWallpapers;
    private Context mContext;

    public TodAdapter(Context context, TodDatabase database) {
        this.mDatabase = database;
        this.mContext = context;

        mWallpapers = new ArrayList<>();
        mDatabase.setDatabaseObserver(this);
        reloadWallpapers();
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
        for(int i = 0; i < mWallpapers.size(); i++){
            if(mWallpapers.get(i).getUid().equals(id)){
                return i;
            }
        }
        return -1;
    }

    @Override
    public void removeAt(int position) {
        mWallpapers.remove(position);
        String uid = getItemUid(position);
        mDatabase.adapterDeleteWallpaper(uid);
    }

    @Override
    public void remove(String id) {
        for (TimeOfDayWallpaper data : mWallpapers) {
            if (data.getUid().equals(id)) {
                mWallpapers.remove(data);
                break;
            }
        }
        mDatabase.adapterDeleteWallpaper(id);
    }

    @Override
    public String getItemUid(int position) {
        return mWallpapers.get(position).getUid();
    }

    @Override
    public int getCount() {
        return mWallpapers.size();
    }

    @Override
    public Object getItem(int position) {
        return mWallpapers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mWallpapers.get(position).getUid().hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        AsyncImageView imageThumbnail;
        FontTextView textStartTime;
        FontTextView textEndTime;
        FontTextView textStartPeriod;
        FontTextView textEndPeriod;
        FrameLayout frameTime;

        String uid = mWallpapers.get(position).getUid();

        if (convertView != null) {
            v = convertView;
        } else {
            v = View.inflate(mContext, R.layout.grid_item_tod, null);
        }

        ViewHolder holder = (ViewHolder)v.getTag();

        if (holder == null) {
            imageThumbnail = (AsyncImageView) v.findViewById(R.id.image_thumbnail);
            textStartTime = (FontTextView)v.findViewById(R.id.textview_starttime);
            textEndTime = (FontTextView)v.findViewById(R.id.textview_endtime);
            textStartPeriod = (FontTextView)v.findViewById(R.id.textview_starttimeperiod);
            textEndPeriod = (FontTextView)v.findViewById(R.id.textview_endtimeperiod);
            frameTime = (FrameLayout)v.findViewById(R.id.frameTime);

            holder = new ViewHolder();
            holder.imageThumbnail = imageThumbnail;
            holder.textStartTime = textStartTime;
            holder.textEndTime = textEndTime;
            holder.textStartPeriod = textStartPeriod;
            holder.textEndPeriod = textEndPeriod;
            holder.frameTime = frameTime;
            v.setTag(holder);
        } else {
            imageThumbnail = holder.imageThumbnail;
            textStartTime = holder.textStartTime;
            textEndTime = holder.textEndTime;
            textEndPeriod = holder.textEndPeriod;
            textStartPeriod = holder.textStartPeriod;
            frameTime = holder.frameTime;
        }
        imageThumbnail.setImageUID(uid);
        TimeOfDayWallpaper wp = mWallpapers.get(position);
        String startTime = String.format("%d:%02d", wp.getStartHour().getRealHour(),
                wp.getStartHour().getMinute());
        String endTime = String.format("%d:%02d", wp.getEndHour().getRealHour(),
                wp.getEndHour().getMinute());
        textStartTime.setText(startTime);
        textEndTime.setText(endTime);
        textStartPeriod.setText(wp.getStartHour().getPeriod() == Hour.AM ? "am" : "pm");
        textEndPeriod.setText(wp.getEndHour().getPeriod() == Hour.AM ? "am" : "pm");
        frameTime.setBackgroundColor(wp.getMutedColor());
        return v;
    }

    private void reloadWallpapers(){
        mWallpapers.clear();
        mWallpapers.addAll(mDatabase.getOrderedWallpapers()); //TODO: Async
        notifyDataSetChanged();
    }

    @Override
    public void onElementRemoved(String uid) {
        for (TimeOfDayWallpaper data : mWallpapers) {
            if (data.getUid().equals(uid)) {
                mWallpapers.remove(data);
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onElementCreated(TimeOfDayWallpaper element) {
        reloadWallpapers();
    }

    @Override
    public void onDataSetChanged() {
        reloadWallpapers();
    }

    private class ViewHolder {
        AsyncImageView imageThumbnail;
        FontTextView textStartTime;
        FontTextView textEndTime;
        FontTextView textStartPeriod;
        FontTextView textEndPeriod;
        FrameLayout frameTime;
    }
}
