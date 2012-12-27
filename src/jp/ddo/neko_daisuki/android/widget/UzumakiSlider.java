package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

public abstract class UzumakiSlider extends View {

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

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        this.invalidate();
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

    private void drawLine(Canvas canvas, int x, int y) {
        Paint paint = new Paint();
        paint.setARGB(255, 255, 255, 255);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        int outer_diameter = this.outer_diameter != 0 ? this.outer_diameter : Math.min(Math.min(x, this.getWidth() - x), Math.min(y, this.getHeight() - y));
        UzumakiDiagram uzumaki = new UzumakiDiagram(x, y, this.start_angle, this.sweep_angle, outer_diameter, this.inner_diameter, paint);
        uzumaki.draw(canvas);
    }

    private void drawTie(Canvas canvas, int x, int y) {
        Path outer_outline = new Path();
        outer_outline.addCircle(x, y, 100, Path.Direction.CW);

        Path inner_outline = new Path();
        inner_outline.addCircle(x, y, 50, Path.Direction.CW);

        Paint paint = new Paint();
        paint.setARGB(255, 255, 0, 0);
        paint.setAntiAlias(true);
        canvas.clipPath(outer_outline);
        canvas.clipPath(inner_outline, Region.Op.DIFFERENCE);
        canvas.drawPaint(paint);
    }

    protected void drawUzumaki(Canvas canvas) {
        int x = this.getWidth() / 2;
        int y = this.getHeight() / 2;
        this.drawTie(canvas, x, y);
        this.drawLine(canvas, x, y);
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
