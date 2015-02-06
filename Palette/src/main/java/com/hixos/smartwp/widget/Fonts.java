package com.hixos.smartwp.widget;

import android.content.Context;
import android.graphics.Typeface;

public class Fonts {


    public static final int STYLE_REGULAR = 0x00000000;
    public static final int STYLE_ITALIC = 0x00000001;
    public static final int STYLE_BOLD = 0x00000002;
    public static final int STYLE_LIGHT = 0x00000004;
    public static final int STYLE_CONDENSED = 0x00000008;

    public static Typeface getTypeface(Context c, int style) {
        String s = "fonts/Roboto";


        if ((style & STYLE_CONDENSED) == STYLE_CONDENSED) {
            s += "Condensed-";
        } else {
            s += "-";
        }

        if ((style & STYLE_BOLD) == STYLE_BOLD) {
            s += "Bold";
        } else if ((style & STYLE_LIGHT) == STYLE_LIGHT) {
            s += "Light";
        }

        if ((style & STYLE_ITALIC) == STYLE_ITALIC) {
            s += "Italic";
        }

        if (s.endsWith("-")) {
            s += "Regular";
        }

        s += ".ttf";

        return Typeface.createFromAsset(c.getAssets(), s);
    }
}


