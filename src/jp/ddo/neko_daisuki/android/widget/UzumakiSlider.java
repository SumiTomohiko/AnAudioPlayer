package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

abstract class UzumakiSlider extends View {

    public UzumakiSlider(Context context) {
        super(context);
        this.initialize();
    }

    public UzumakiSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
        this.readAttribute(attrs);
    }

    public UzumakiSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize();
        this.readAttribute(attrs);
    }

    private void initialize() {
        this.min = 0;
        this.max = 0;
        this.progress = 0;
    }

    private static final int DEFAULT_SWEEP_ANGLE = -3 * 360;

    private void readAttribute(AttributeSet attrs) {
        this.start_angle = attrs.getAttributeIntValue(null, "start_angle", 0);
        this.sweep_angle = attrs.getAttributeIntValue(null, "sweep_angle", DEFAULT_SWEEP_ANGLE);
        this.outer_diameter = attrs.getAttributeIntValue(null, "outer_diameter", 0);
        this.inner_diameter = attrs.getAttributeIntValue(null, "inner_diameter", 0);
        this.outline_outer_diameter = attrs.getAttributeIntValue(null, "outline_outer_diameter", this.outer_diameter);
        this.outline_inner_diameter = attrs.getAttributeIntValue(null, "outline_inner_diameter", this.inner_diameter);
    }

    protected void drawUzumaki(Canvas canvas) {
        Paint paint = new Paint();
        paint.setARGB(255, 255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);

        UzumakiDiagram uzumaki = new UzumakiDiagram(this.start_angle, this.sweep_angle, this.outer_diameter, this.inner_diameter, paint);
        uzumaki.draw(canvas);
    }

    private int min;
    private int max;
    private int progress;

    private int start_angle;
    private int sweep_angle;
    private int outer_diameter;
    private int inner_diameter;
    private int outline_outer_diameter;
    private int outline_inner_diameter;
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
