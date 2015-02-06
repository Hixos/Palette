package com.hixos.smartwp;

import android.widget.BaseAdapter;

public abstract class AnimatedListAdapter extends BaseAdapter {

    public abstract void dragStarted(String itemId);

    public abstract void drag(String itemId, String beforeId);

    public abstract void dragEndend(String itemID);

    public abstract int getItemPosition(String id);

    public abstract void removeAt(int position);

    public abstract void remove(String id);

    public abstract String getItemUid(int position);
}
