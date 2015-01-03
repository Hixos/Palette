package com.hixos.smartwp.utils;

import android.graphics.PointF;

public class Straight {
    public float m;
    public float q;

    public Straight(float m, float q) {
        this.m = m;
        this.q = q;
    }

    public static PointF intersect(Straight straight1, Straight straight2) {
        if (straight1.m == straight2.m) return null;
        float x = (straight2.q - straight1.q) / (straight1.m - straight2.m);
        float y = straight1.y(x);

        return new PointF(x, y);
    }

    public static Straight passTrough(float slope, PointF point) {
        float q = point.y - slope * point.x;
        return new Straight(slope, q);
    }

    public static Straight passTrough(PointF point1, PointF point2) {
        float m = (point1.x - point2.x) / (point1.y - point2.y);
        float q = point1.y - m * point1.x;

        return new Straight(m, q);
    }

    public float y(float x) {
        return m * x + q;
    }

    public float x(float y) {
        return (y - q) / m;
    }

    public PointF intersect(Straight s) {
        return intersect(this, s);
    }

    public void passTrough(PointF point) {
        passTrough(point.x, point.y);
    }

    public void passTrough(float x, float y) {
        q = y - m * x;
    }
}
