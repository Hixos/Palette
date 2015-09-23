package com.hixos.smartwp.triggers.slideshow;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;
import com.hixos.smartwp.widget.AsyncImageView;

import java.util.ArrayList;
import java.util.List;

public class SlideshowDatabaseAdapter extends AnimatedListAdapter{
    int mLastMovedPosition = -1;
    private SlideshowDB mDatabase;
    private List<SlideshowWallpaper> mData;
   // private List<String> mBeforeDragIDs;
    private Context mContext;
    private String newPosition;

    public SlideshowDatabaseAdapter(SlideshowDB mDatabase,
                                    Context context) {
        this.mDatabase = mDatabase;
        this.mContext = context;

        mData = new ArrayList<>();
        reloadDatabase();

      //  mBeforeDragIDs = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public SlideshowWallpaper getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        if (i >= 0 && i < getCount())
            return mData.get(i).getUid().hashCode();
        else
            return -1;
    }

    @Override
    public String getItemUid(int i) {
        if (i >= 0 && i < getCount())
            return mData.get(i).getUid();
        else
            return "";
    }

    @Override
    public int getItemPosition(String id) {
        for (int i = 0; i < mData.size(); i++) {
            if (mData.get(i).getUid().equals(id))
                return i;
        }
        return -1;
    }

    @Override
    public void dragStarted(String itemId) {
       /* mBeforeDragIDs.clear();
        for (SlideshowWallpaper d : mData) {
            mBeforeDragIDs.add(d.getUid());
        }*/
    }

    @Override
    public void drag(String itemId, String beforeId) {
        newPosition = beforeId;
        int oldPosition = getItemPosition(itemId);
        int newPosition = getItemPosition(beforeId);

        if (oldPosition >= 0 && newPosition >= 0) {
            mLastMovedPosition = newPosition;

            SlideshowWallpaper item = mData.get(oldPosition);
            mData.remove(oldPosition);
            mData.add(newPosition, item);
        }
    }

    @Override
    public void dragEndend(String itemID) {
        if (mLastMovedPosition != -1)
            mDatabase.moveBefore(itemID, newPosition);

        mLastMovedPosition = -1;
    }

    @Override
    public void removeAt(int position) {
        mData.remove(position);
        String uid = getItemUid(position);
        deleteWallpaper(uid);
    }

    @Override
    public void remove(String id) {
        for (SlideshowWallpaper data : mData) {
            if (data.getUid().equals(id)) {
                mData.remove(data);
                break;
            }
        }
        deleteWallpaper(id);
    }

    private void deleteWallpaper(final String uid){
        AsyncTask<String, Void, Boolean> deleteTask = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                boolean reload = uid.equals(mDatabase.getCurrentWallpaperUid());
                mDatabase.deleteWallpaper(uid);
                return reload;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                if(aBoolean){
                    SlideshowService.broadcastReloadWallpaper(mContext);
                }
                notifyWallpaperDeleted(uid);
            }
        };

        deleteTask.execute(uid);
    }

    /*@Override
    public void onElementRemoved(String uid) {
        for (SlideshowWallpaper data : mData) {
            if (data.getUid().equals(uid)) {
                mData.remove(data);
                break;
            }
        }
        notifyDataSetChanged();
    }
*/

    public void reloadDatabase() {
        AsyncTask<Void, Void, List<SlideshowWallpaper>> reloadTask = new AsyncTask<Void, Void, List<SlideshowWallpaper>>() {
            @Override
            protected List<SlideshowWallpaper> doInBackground(Void... params) {
                return mDatabase.getOrderedWallpaperList();
            }

            @Override
            protected void onPostExecute(List<SlideshowWallpaper> slideshowWallpapers) {
                mData.clear();
                mData.addAll(slideshowWallpapers);
                notifyDataSetChanged();
            }
        };

       reloadTask.execute();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View v;
        AsyncImageView imageView;
        String uid = mData.get(i).getUid();

        if (convertView != null) {
            v = convertView;
        } else {
            v = View.inflate(mContext, R.layout.grid_item_slideshow, null);
        }

        if (v.getTag() == null) {
            imageView = (AsyncImageView) v.findViewById(R.id.image_thumbnail);
            v.setTag(imageView);
        } else {
            imageView = (AsyncImageView) v.getTag();
        }
        imageView.setImageUID(uid);

        return v;
    }
}
