package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.hixos.smartwp.Logger;

/**
 * Created by Luca on 20/02/2015.
 */
public class NewCircleView extends FrameLayout {
    public NewCircleView(Context context) {
        super(context);
        fabulize();
    }

    public NewCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        fabulize();
    }

    public NewCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        fabulize();
    }

    private void fabulize(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, getWidth(), getHeight());
                }
            });
            setClipToOutline(true);
            Logger.w("FABBED", "DBFSAFA");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        if(w > h){
            setMeasuredDimension(w, w);
        }else{
            setMeasuredDimension(h,h);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            invalidateOutline();
        }
    }

}
