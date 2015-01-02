package com.hixos.smartwp.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.hixos.smartwp.R;

/**
 * Created by Luca on 16/03/14.
 */
public class ArrowView extends View {

    private Path mPath;
    private float mPathLenght;

    private Paint mPathPaint;
    private Paint mArrowPaint;


    private ObjectAnimator mAnimator;
    private ObjectAnimator mAlphaAnimator;

    private int mAnimationTime;
    private int mArrowLenght, mArrowHeight;
    private int mLineWidth;

    private int mMaxAlpha;

    public ArrowView(Context context) {
        super(context);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArrowView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public void init(){
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mAnimationTime = getResources().getInteger(R.integer.empty_state_animation_time);
        mArrowLenght = getResources().getDimensionPixelSize(R.dimen.empty_state_arrow_lenght);
        mArrowHeight = getResources().getDimensionPixelSize(R.dimen.empty_state_arrow_height);
        mLineWidth = getResources().getDimensionPixelSize(R.dimen.empty_state_line_width);

        int color = getResources().getColor(R.color.empty_state_arrow);

        mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPathPaint.setColor(color);
        mPathPaint.setStrokeWidth(mLineWidth);
        mPathPaint.setStyle(Paint.Style.STROKE);

        mMaxAlpha = mPathPaint.getAlpha();

        mArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArrowPaint.setColor(color);
    }

    public void setPoints(Point source, Point dest, boolean animate){
        setPoints(source.x, source.y, dest.x, dest.y, animate);
    }

    public void setPoints(float sX, float sY, float dX, float dY, boolean animate){
        mPath = new Path();
        mPath.moveTo(sX, sY);
        float C1x, C1y, C2x, C2y;
        C1x = sX;
        C2x = dX;
        C1y = C2y = (sY + dY) / 2;
        mPath.cubicTo(C1x, C1y, C2x, C2y, dX, dY);

        mPathLenght = new PathMeasure(mPath, false).getLength();
        if(animate){
            if(mAnimator != null) mAnimator.cancel();

            mAnimator = ObjectAnimator.ofFloat(this, "phase", 0, 1);
            mAnimator.setDuration(mAnimationTime);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimator.start();
        }else{
            setPhase(1);
        }
    }


    public void setPhase(float value){
        float phase = -value * (mPathLenght - 1);

        int alpha = Math.min(mMaxAlpha, (int)(value * 3 * mMaxAlpha));

        PathEffect pathEffect = new DashPathEffect(new float[] {mPathLenght, mPathLenght},
                phase + mPathLenght);

        mPathPaint.setPathEffect(pathEffect);
        mPathPaint.setAlpha(alpha);

        PathEffect arrowEffect = new PathDashPathEffect(
                makeArrow(mArrowLenght, mArrowHeight),    // "stamp"
                mPathLenght,                            // advance, or distance between two stamps
                phase,                             // phase, or offset before the first stamp
                PathDashPathEffect.Style.ROTATE); // how to transform each stamp

        mArrowPaint.setPathEffect(arrowEffect);
        mArrowPaint.setAlpha(alpha);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mPath != null){
            canvas.drawPath(mPath, mPathPaint);
            canvas.drawPath(mPath, mArrowPaint);
        }


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private Path makeArrow(float length, float height) {
        Path p = new Path();
        p.moveTo(0.0f, -height / 2.0f);
        p.lineTo(length, 0.0f);
        p.lineTo(0.0f, height / 2.0f);
        //p.lineTo(length / 4.0f, 0.0f);
        p.close();
        return p;
    }


}
