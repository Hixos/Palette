package com.hixos.smartwp.services.slideshow;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;
import com.hixos.smartwp.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Luca on 01/03/14.
 */
public class SlideshowDatabaseAdapter extends AnimatedListAdapter implements SlideshowDatabase.DatabaseObserver{

    private SlideshowDatabase mDatabase;

    private List<SlideshowData> mData;
    private List<String> mBeforeDragIDs;

    private Context mContext;

    int mLastMovedPosition = -1;

    public SlideshowDatabaseAdapter(SlideshowDatabase mDatabase,
                                    Context context) {
        this.mDatabase = mDatabase;
        this.mContext = context;

        mDatabase.setDatabaseObserver(this);

        mData = new ArrayList<SlideshowData>();
        mBeforeDragIDs = new ArrayList<String>();
        reloadData();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public SlideshowData getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        if(i >= 0 && i < getCount())
            return mData.get(i).getUid().hashCode();
        else
            return -1;
    }

    @Override
    public String getItemUid(int i) {
        if(i >= 0 && i < getCount())
            return mData.get(i).getUid();
        else
            return "";
    }

    @Override
    public int getItemPosition(String id) {
        for(int i = 0; i < mData.size(); i++){
            if(mData.get(i).getUid().equals(id))
                return i;
        }
        return -1;
    }

    @Override
    public void dragStarted(String itemId) {
        mBeforeDragIDs.clear();
        for(SlideshowData d : mData){
            mBeforeDragIDs.add(d.getUid());
        }
    }

    @Override
    public void drag(String itemId, String beforeId) {
        int oldPosition = getItemPosition(itemId);
        int newPosition = getItemPosition(beforeId);

        if(oldPosition >= 0 && newPosition >= 0){
            mLastMovedPosition = newPosition;

            SlideshowData item = mData.get(oldPosition);
            mData.remove(oldPosition);
            mData.add(newPosition, item);
        }
    }

    @Override
    public void dragEndend(String itemID) {
        if(mLastMovedPosition != -1)
            mDatabase.updateOrderAsync(mData, false);

        mLastMovedPosition = -1;
    }

    @Override
    public void removeAt(int position) {
        mData.remove(position);
        String uid = getItemUid(position);
        mDatabase.adapterDeleteWallpaper(uid);
    }

    @Override
    public void remove(String id) {
        for(SlideshowData data : mData){
            if(data.getUid().equals(id)){
                mData.remove(data);
                break;
            }
        }
        mDatabase.adapterDeleteWallpaper(id);
    }

    @Override
    public void onElementRemoved(String uid) {
        for(SlideshowData data : mData){
            if(data.getUid().equals(uid)){
                mData.remove(data);
                break;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public void onElementCreated(SlideshowData element) {
        reloadData();
    }

    @Override
    public void onDataSetChanged() {
        reloadData();
    }

    public void reloadData(){
        mData.clear();

        mDatabase.getOrderedWallpapersAsync(new SlideshowDatabase.OnWallpapersLoadedListener() {
            @Override
            public void onWallpapersLoaded(List<SlideshowData> wallpapers) {
                mData = wallpapers;
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View v;
        AsyncImageView imageView;
        String uid = mData.get(i).getUid();

        if(convertView != null){
            v = convertView;
        }else {
            v = View.inflate(mContext, R.layout.grid_item_slideshow, null);
        }

        if(v.getTag() == null){
            imageView = (AsyncImageView)v.findViewById(R.id.image_thumbnail);
            v.setTag(imageView);
        }else{
            imageView = (AsyncImageView)v.getTag();
        }
        imageView.setImageUID(uid);

        return v;
    }
}
