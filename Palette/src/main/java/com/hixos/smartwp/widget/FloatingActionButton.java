package com.hixos.smartwp.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.hixos.smartwp.R;

public class FloatingActionButton extends FrameLayout implements View.OnClickListener {

    public FloatingActionButton(Context context) {
        super(context);
        init(null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(generateDefaultLayoutParams());
        imageView.setClickable(true);
        imageView.setOnClickListener(this);
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FloatingActionButton,
                    0, 0);
            imageView.setImageDrawable(a.getDrawable(R.styleable.FloatingActionButton_image));
            imageView.setBackground(a.getDrawable(R.styleable.FloatingActionButton_selector));
            //imageView.setBackgroundResource(R.drawable.fab_selector);
            //imageView.setImageResource(R.drawable.fab_icon);
        }
        addView(imageView);
    }


    @Override
    public void onClick(View v) {
        callOnClick();
    }
}
