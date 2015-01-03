package com.hixos.smartwp.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.hixos.smartwp.AnimatedListAdapter;
import com.hixos.smartwp.R;

import java.util.HashMap;
import java.util.Set;

public class AnimatedGridView extends GridView {
    private static final int SCROLL_MIN_SPEED = 15; //Min speed
    private static final int SCROLL_MAX_SPEED = 50; //Max speed
    private static final int SCROLL_FULL_SPEED_TIME = 5000; //Time needed to accelerate to max speed

    AnimatedListAdapter mAdapter;

    private boolean mAnimating = false;
    private int mAnimationTime = 200;

    private int mDownX, mDownY;
    private int mLastEventX, mLastEventY;

    private int mSlop;

    private Rect mBitmapCellOriginalBounds;
    private Rect mBitmapCellCurrentBounds;
    private AlphaBitmapDrawable mBitmapCell;

    //Drag & Drop
    private boolean mDraggable = true;
    private boolean mDragging = false;

    private boolean mScrolling = false;
    private long mScrollBeginTime;

    private String mDragCellId;
    private String mSwapCellId;

    private int mScrollMinAmount, mScrollMaxAmount;
    private int mScrollAmount;

    //Swipe
    private boolean mSwipeEnabled = true;
    private OnScrollListener mScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView absListView, int scrollState) {
            if (mScrolling && mDragging) {
                int deltaTime = (int) (System.currentTimeMillis() - mScrollBeginTime);
                float percent = Math.min((float) deltaTime / (float) SCROLL_FULL_SPEED_TIME, 1);

                int deltaAmount = mScrollMaxAmount - mScrollMinAmount;
                mScrollAmount = (int) (mScrollMinAmount + deltaAmount * percent);
                handleScroll(mBitmapCellCurrentBounds);
                handleSwap();
            }
            mSwipeEnabled = scrollState == OnScrollListener.SCROLL_STATE_IDLE;
        }

        @Override
        public void onScroll(AbsListView absListView, int i, int i2, int i3) {
        }
    };
    private boolean mSwiping = false;
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener =
            new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
                    return !(mSwiping || mAnimating) && startDrag(position);
                }
            };
    private String mSwipeCellId;
    private VelocityTracker mVelocityTracker;
    private int mMinFlingVelocity;
    private int mDismissTime = 300; //Ms

    public AnimatedGridView(Context context) {
        super(context);
        init(context);
    }

    public AnimatedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.AnimatedGridView,
                    0, 0);
            mDraggable = a.getBoolean(R.styleable.AnimatedGridView_draggable, true);
        }
        init(context);
    }

    public AnimatedGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.AnimatedGridView,
                    0, 0);
            mDraggable = a.getBoolean(R.styleable.AnimatedGridView_draggable, true);
        }
        init(context);
    }

    private void init(Context context) {
        // mListeners = new ArrayList<GridViewActionListener>();
        if (mDraggable)
            setOnItemLongClickListener(mOnItemLongClickListener);

        setOnScrollListener(mScrollListener);

        //Drag & Drop
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mScrollMinAmount = (int) (SCROLL_MIN_SPEED / metrics.density);
        mScrollMaxAmount = (int) (SCROLL_MAX_SPEED / metrics.density);
        mScrollAmount = mScrollMinAmount;

        //Swipe to dismiss
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 6;
    }

    public AnimatedListAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(ListAdapter listadapter) {
        if ((listadapter instanceof AnimatedListAdapter) || listadapter == null) {
            super.setAdapter(listadapter);
            mAdapter = (AnimatedListAdapter) listadapter;
        } else {
            throw new IllegalArgumentException("Adapter must be a child of AnimatedListAdapter");
        }
    }

    private boolean startDrag(int position) {
        int itemNum = position - getFirstVisiblePosition();

        mSwapCellId = getAdapter().getItemUid(position);
        View selectedView = getChildAt(itemNum);
        mDragCellId = getAdapter().getItemUid(position);

        getAdapter().dragStarted(mDragCellId);

        if (selectedView == null) {
            return false;
        }

        mBitmapCell = getViewBitmapDrawable(selectedView, 127);
        selectedView.setVisibility(INVISIBLE);
        mDragging = true;
        return true;

    }

    private void drag(int deltaX, int deltaY) {
        mBitmapCellCurrentBounds.offsetTo(mBitmapCellOriginalBounds.left + deltaX,
                mBitmapCellOriginalBounds.top + deltaY);
        mBitmapCell.setBounds(mBitmapCellCurrentBounds);
        invalidate();
        handleSwap();
        mScrolling = handleScroll(mBitmapCellCurrentBounds);
    }

    private void handleSwap() {
        String swapCell = getAdapter().getItemUid(pointToPosition(mLastEventX, mLastEventY));
        if (swapCell.isEmpty()) {
            mSwapCellId = "";
        } else if (!swapCell.equals(mSwapCellId)) {
            mSwapCellId = swapCell;
            int firstPosition = getFirstVisiblePosition();
            int hoverPosition = getAdapter().getItemPosition(mDragCellId)
                    - firstPosition;
            int swapPosition = getAdapter().getItemPosition(mSwapCellId)
                    - firstPosition;

            final HashMap<String, Point> oldLocations = new HashMap<>();
            int start = Math.min(hoverPosition, swapPosition);
            int end = Math.max(hoverPosition, swapPosition);

            if (start == hoverPosition) {
                start += 1;
            } else {
                end -= 1;
            }

            for (int i = start; i <= end; i++) {
                View v = getChildAt(i);
                if (v != null) {
                    Point p = new Point(v.getLeft(), v.getTop());
                    String id = getAdapter().getItemUid(i + firstPosition);
                    oldLocations.put(id, p);
                }
            }

            getAdapter().drag(mDragCellId, mSwapCellId);
            getAdapter().notifyDataSetChanged();

            View hover = getChildAt(hoverPosition);
            View swap = getChildAt(swapPosition);

            if (hover != null) {
                hover.setVisibility(VISIBLE);
            }
            if (swap != null) {
                swap.setVisibility(INVISIBLE);
            }
            final ViewTreeObserver observer = getViewTreeObserver();
            if (observer != null) {
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        observer.removeOnPreDrawListener(this);
                        //Log.w("GRID", "Predraw");
                        Set<String> keys = oldLocations.keySet();
                        for (String key : keys) {
                            Point oldPos = oldLocations.get(key);
                            View swapView = getViewFromID(key);

                            if (swapView == null) continue;

                            float deltaX = oldPos.x - swapView.getX();
                            float deltaY = oldPos.y - swapView.getY();

                            swapView.setTranslationX(deltaX);
                            swapView.setTranslationY(deltaY);

                            ObjectAnimator animatorY = ObjectAnimator.ofFloat(swapView,
                                    View.TRANSLATION_Y, 0);
                            animatorY.setDuration(mAnimationTime)
                                    .setInterpolator(new DecelerateInterpolator());
                            animatorY.start();
                            ObjectAnimator animatorX = ObjectAnimator.ofFloat(swapView,
                                    View.TRANSLATION_X, 0);
                            animatorX.setDuration(mAnimationTime)
                                    .setInterpolator(new DecelerateInterpolator());
                            animatorX.start();
                        }
                        return true;
                    }
                });
            }
        }
    }

    private boolean handleScroll(Rect hoverRect) {
        int offset = computeVerticalScrollOffset();
        int height = getHeight();
        int extent = computeVerticalScrollExtent();
        int range = computeVerticalScrollRange();
        int hoverViewTop = hoverRect.top;
        int hoverHeight = hoverRect.height();

        if (hoverViewTop <= 0 && offset > 0) {
            if (!mScrolling)
                mScrollBeginTime = System.currentTimeMillis();
            smoothScrollBy(-mScrollAmount, 0);
            return true;
        }

        if (hoverViewTop + hoverHeight >= height && (offset + extent) < range) {
            if (!mScrolling)
                mScrollBeginTime = System.currentTimeMillis();
            smoothScrollBy(mScrollAmount, 0);
            return true;
        }
        return false;
    }

    private void endDrag() {
        getAdapter().dragEndend(mDragCellId);

        final View hoverCell = getViewFromID(mDragCellId);
        mDragging = false;
        mDragCellId = "";
        mSwapCellId = "";

        if (hoverCell != null) {
            mAnimating = true;
            final int startX = mBitmapCellCurrentBounds.left;
            final int endX = hoverCell.getLeft();

            final int startY = mBitmapCellCurrentBounds.top;
            final int endY = hoverCell.getTop();

            final int startAlpha = mBitmapCell.getAlpha();
            final int endAlpha = 255;

            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(mAnimationTime)
                    .setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (Float) valueAnimator.getAnimatedValue();
                    int x = (int) interpolate(startX, endX, value);
                    int y = (int) interpolate(startY, endY, value);
                    int alpha = (int) interpolate(startAlpha, endAlpha, value);
                    mBitmapCellCurrentBounds.offsetTo(x, y);
                    mBitmapCell.setBounds(mBitmapCellCurrentBounds);
                    mBitmapCell.setAlpha(alpha);
                    invalidate();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mBitmapCell = null;
                    hoverCell.setVisibility(VISIBLE);
                    mAnimating = false;
                }
            });
            animator.start();
        }
    }

    private boolean startSwipe() {
        View swipeView = getViewFromID(mSwipeCellId);
        if (swipeView != null) {
            mSwiping = true;
            swipeView.setVisibility(INVISIBLE);
            mBitmapCell = getViewBitmapDrawable(swipeView, 255);
            return true;
        } else {
            return false;
        }
    }

    private void swipe(int deltaX) {
        //if(mBitmapCell == null) return;

        mBitmapCellCurrentBounds.offsetTo(mBitmapCellOriginalBounds.left + deltaX,
                mBitmapCellOriginalBounds.top);
        mBitmapCell.setBounds(mBitmapCellCurrentBounds);

        int totalTranslation = getSwipeTranslationX(
                mBitmapCellOriginalBounds.width(), deltaX > 0);
        float perc = Math.max(0, 1 - (float) deltaX / totalTranslation);
        mBitmapCell.setAlpha((int) (255 * perc));
        invalidate();
    }

    private void endSwipe(float deltaX, float velocityX, float velocityY) {
        mSwiping = false;
        mAnimating = true;

        final View swipeView = getViewFromID(mSwipeCellId);
        final String id = mSwipeCellId;

        boolean dismiss = false, dismissRight = false;
        int width = mBitmapCellOriginalBounds.width();
        float dismissDelta = (float) width / 2;
        if (mMinFlingVelocity <= Math.abs(velocityX) && Math.abs(velocityY) < Math.abs(velocityX)) {
            dismiss = deltaX * velocityX >= 0;
            dismissRight = velocityX > 0;
        } else if (Math.abs(deltaX) > dismissDelta) {
            dismiss = deltaX * velocityX >= 0;
            dismissRight = deltaX > 0;
        }

        final int startX = mBitmapCellCurrentBounds.left;
        final int endX = dismiss
                ? mBitmapCellOriginalBounds.left + getSwipeTranslationX(width, dismissRight)
                : mBitmapCellOriginalBounds.left;

        final int startAlpha = mBitmapCell.getAlpha();
        final int endAlpha = dismiss ? 0 : 255;

        final boolean fDismiss = dismiss;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Float value = (Float) valueAnimator.getAnimatedValue();
                int x = (int) interpolate(startX, endX, value);
                mBitmapCellCurrentBounds.offsetTo(x, mBitmapCellCurrentBounds.top);
                mBitmapCell.setBounds(mBitmapCellCurrentBounds);
                int alpha = (int) interpolate(startAlpha, endAlpha, value);
                mBitmapCell.setAlpha(alpha);
                invalidate();
            }
        });
        animator.setDuration(mDismissTime)
                .setInterpolator(new DecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                swipeView.setVisibility(VISIBLE);
                mBitmapCell = null;
                mAnimating = false;
                if (fDismiss) dismissItem(id);
            }
        });
        animator.start();
    }

    private void dismissItem(String dismissId) {
        final HashMap<String, Point> oldPositions = new HashMap<>();
        int firstPosition = getFirstVisiblePosition();
        int dismissPos = getAdapter().getItemPosition(dismissId) - firstPosition;
        for (int i = dismissPos; i <= getLastVisiblePosition() - firstPosition; i++) {
            String id = getAdapter().getItemUid(i + firstPosition);
            View v = getChildAt(i);
            if (v != null) {
                oldPositions.put(id, new Point(v.getLeft(), v.getTop()));
            }
        }
        getAdapter().remove(dismissId);
        getAdapter().notifyDataSetChanged();

        final ViewTreeObserver observer = getViewTreeObserver();
        if (observer != null) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);
                    Set<String> keys = oldPositions.keySet();
                    for (String key : keys) {
                        View dismisssView = getViewFromID(key);
                        if (dismisssView != null) {
                            int deltaX = oldPositions.get(key).x - dismisssView.getLeft();
                            int deltaY = oldPositions.get(key).y - dismisssView.getTop();
                            dismisssView.setTranslationX(deltaX);
                            dismisssView.setTranslationY(deltaY);

                            AnimatorSet set = new AnimatorSet();

                            ObjectAnimator animatorY = ObjectAnimator.ofFloat(dismisssView,
                                    View.TRANSLATION_Y, 0);
                            animatorY.setDuration(mAnimationTime)
                                    .setInterpolator(new DecelerateInterpolator());
                            //animatorY.start();
                            ObjectAnimator animatorX = ObjectAnimator.ofFloat(dismisssView,
                                    View.TRANSLATION_X, 0);
                            animatorX.setDuration(mAnimationTime)
                                    .setInterpolator(new DecelerateInterpolator());
                            //animatorX.start();

                            set.play(animatorX).with(animatorY);
                            set.start();
                        }
                    }
                    return true;
                }
            });
        }
    }

    public void removeItemAnimated(String itemID) {
        mSwipeCellId = itemID;
        if (!mAnimating && startSwipe()) {
            endSwipe(1, mMinFlingVelocity * 2, 1);
        } else {
            getAdapter().remove(itemID);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int) event.getX();
                mDownY = (int) event.getY();
                int swipeCell = pointToPosition(mDownX, mDownY);
                if (swipeCell != INVALID_POSITION && !mSwiping && !mAnimating) {
                    mSwipeCellId = getAdapter().getItemUid(swipeCell);
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mAnimating) //Don't scroll or anything
                    return true;

                mLastEventY = (int) event.getY();
                mLastEventX = (int) event.getX();

                int deltaX = mLastEventX - mDownX;
                int deltaY = mLastEventY - mDownY;

                if (mDragging) {
                    drag(deltaX, deltaY);
                    return true;
                } else if (Math.abs(deltaX) > mSlop
                        && Math.abs(deltaX) > (Math.abs(deltaY) * 1.75f)
                        && mSwipeEnabled && !mSwiping
                        && pointToPosition((int) event.getX(), (int) event.getY()) != INVALID_POSITION) {

                    startSwipe();
                    //Cancel other touch events (removes highlight from item)
                    MotionEvent cancelEvent = MotionEvent.obtain(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                            (event.getActionIndex()
                                    << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    onTouchEvent(cancelEvent);
                }
                if (mSwiping && mSwipeEnabled) {
                    mVelocityTracker.addMovement(event);
                    swipe(deltaX);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDragging) {
                    endDrag();
                }
                if (mSwiping) {
                    float dX = event.getX() - mDownX;
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = mVelocityTracker.getXVelocity();
                    float velocityY = mVelocityTracker.getYVelocity();
                    endSwipe(dX, velocityX, velocityY);
                }
                mSwapCellId = "";
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mBitmapCell != null) {
            mBitmapCell.draw(canvas);
        }
    }

    private int getSwipeTranslationX(int cellWidth, boolean right) {
        int t = getNumColumns() == 1 ? (int) ((float) cellWidth / 4 * 3) : cellWidth;
        return right ? t : -t;
    }

    private View getViewFromID(String itemID) {
        int firstPosition = getFirstVisiblePosition();
        AnimatedListAdapter adapter = getAdapter();
        int position = adapter.getItemPosition(itemID) - firstPosition;
        return getChildAt(position);
    }

    @Override
    public int pointToPosition(int x, int y) {
        int pos = super.pointToPosition(x, y);
        View v = getChildAt(pos - getFirstVisiblePosition());
        //Don't detect views that are being translated
        if (v != null && (v.getTranslationX() != 0 || v.getTranslationY() != 0)) {
            return INVALID_POSITION;
        }
        return pos;
    }

    private AlphaBitmapDrawable getViewBitmapDrawable(View view, int alpha) {
        int w = view.getWidth();
        int h = view.getHeight();
        int top = view.getTop();
        int left = view.getLeft();

        AlphaBitmapDrawable drawable = new AlphaBitmapDrawable(getResources(), getBitmapFromView(view));
        drawable.setAlpha(alpha);
        mBitmapCellOriginalBounds = new Rect(left, top, left + w, top + h);
        mBitmapCellCurrentBounds = new Rect(mBitmapCellOriginalBounds);
        drawable.setBounds(mBitmapCellCurrentBounds);
        return drawable;
    }

    private Bitmap getBitmapFromView(View v) {
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        v.draw(canvas);
        return bitmap;
    }

    private float interpolate(float start, float end, float f) {
        return start + f * (end - start);
    }
}
