package com.hixos.smartwp.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;

/**
 * Created by Luca on 22/03/2015.
 */
public class CheckableFrameLayout extends FrameLayout implements Checkable {
    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changes.
     */
    public static interface OnCheckedChangeListener {
        void onCheckedChanged(CheckableFrameLayout checkableFrameLayout, boolean isChecked);
    }

    /**
     * An array of states.
     */
    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    private static final String TAG = "FloatingActionButton";

    // A boolean that tells if the FAB is checked or not.
    private boolean mChecked;

    // A listener to communicate that the FAB has changed it's state
    private OnCheckedChangeListener mOnCheckedChangeListener;

    public CheckableFrameLayout(Context context) {
        this(context, null, 0, 0);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr);

        setClickable(true);
    }

    /**
     * Sets the checked/unchecked state of the FAB.
     * @param checked
     */
    public void setChecked(boolean checked) {
        // If trying to set the current state, ignore.
        if (checked == mChecked) {
            return;
        }
        mChecked = checked;

        // Now refresh the drawable state (so the icon changes)
        refreshDrawableState();

        if (mOnCheckedChangeListener != null) {
            mOnCheckedChangeListener.onCheckedChanged(this, checked);
        }
    }

    /**
     * Register a callback to be invoked when the checked state of this button
     * changes.
     *
     * @param listener the callback to call on checked state change
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
