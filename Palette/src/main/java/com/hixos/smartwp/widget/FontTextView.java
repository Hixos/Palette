package com.hixos.smartwp.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.hixos.smartwp.R;

public class FontTextView extends TextView {

    protected static final String LOGTAG = "CustomFontTextView";

    public FontTextView(Context c) {
        super(c);
    }

    public FontTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            Typeface typeface;
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FontTextView,
                    0, 0);
            String font = "fonts/";
            try {
                font += a.getString(R.styleable.FontTextView_textFont);
            } finally {
                a.recycle();
            }
            font += ".ttf";

            try {
                typeface = Typeface.createFromAsset(context.getAssets(), font);
            } catch (RuntimeException re) {
                Log.e(LOGTAG, "Error setting typeface, selected font may not exist | " + re.getMessage());
                typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
            }

            setTypeface(typeface);
        }

    }
}
