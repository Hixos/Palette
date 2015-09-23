package com.hixos.smartwp;

import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class AnimatedListAdapter extends BaseAdapter {

    private List<OnWallpaperDeletedListener> mListeners = new ArrayList<>();

    public void addOnWallpaperDeletedListener(OnWallpaperDeletedListener listener){
        mListeners.add(listener);
    }

    public void removeOnWallpaperDeletedListener(OnWallpaperDeletedListener listener){
        mListeners.remove(listener);
    }

    protected void notifyWallpaperDeleted(String uid){
        for(OnWallpaperDeletedListener listener : mListeners){
            if(listener != null)
                listener.onWallpaperDeleted(uid);
        }
    }

    protected void clearWallpaperDeletedListeners() {
        mListeners.clear();
    }

    public abstract void dragStarted(String itemId);

    public abstract void drag(String itemId, String beforeId);

    public abstract void dragEndend(String itemID);

    public abstract int getItemPosition(String id);

    public abstract void removeAt(int position);

    public abstract void remove(String id);

    public abstract String getItemUid(int position);

    public interface OnWallpaperDeletedListener {
        void onWallpaperDeleted(String uid);
    }
}
