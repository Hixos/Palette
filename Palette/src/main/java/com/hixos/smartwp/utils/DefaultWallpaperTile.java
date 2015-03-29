package com.hixos.smartwp.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.hixos.smartwp.R;
import com.hixos.smartwp.widget.AsyncImageView;

/**
 * Created by Luca on 29/03/2015.
 */
public class DefaultWallpaperTile{
    public interface DefaultWallpaperTileListener {
        public void onDefaultWallpaperEmptyStateClick(View view);
        public void changeDefaultWallpaper();
    }

    public static View inflate(Context context, final boolean emptyStateVisible,
                               final DefaultWallpaperTileListener listener){
        View tileView = LayoutInflater.from(context).inflate(R.layout.grid_item_default_wallpaper, null);
        final AsyncImageView thumbnail =  (AsyncImageView)tileView.findViewById(R.id.image_thumbnail);
        final View overflowButton = tileView.findViewById(R.id.button_overflow);
        final View emptyState = tileView.findViewById(R.id.emptyState);

        if(emptyStateVisible){
            emptyState.setVisibility(View.VISIBLE);
            thumbnail.setVisibility(View.GONE);
            overflowButton.setVisibility(View.GONE);
        }else{
            emptyState.setVisibility(View.GONE);
            thumbnail.setVisibility(View.VISIBLE);
            overflowButton.setVisibility(View.VISIBLE);
        }

        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(overflowButton.getContext(), overflowButton);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.popup_default_wallpaper_tile, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        listener.changeDefaultWallpaper();
                        return true;
                    }
                });
                popup.show();
            }
        });
        emptyState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onDefaultWallpaperEmptyStateClick(view);
            }
        });
        return tileView;
    }
}
