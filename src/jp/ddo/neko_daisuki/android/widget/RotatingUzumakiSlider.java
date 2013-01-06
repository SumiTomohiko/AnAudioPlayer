package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

class RotatingUzumakiSlider extends UzumakiSlider {

    private static String LOG_TAG = "RotatingUzumakiSlider";

    private int headerSize;

    public RotatingUzumakiSlider(Context context) {
        super(context);
        this.initialize();
    }

    public RotatingUzumakiSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public RotatingUzumakiSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize();
    }

    public void placeHead(float pointerX, float pointerY) {
        int outerRadius = this.getAbsoluteOuterDiameter() / 2;
        float len = this.getWidth() / 2 + outerRadius - pointerX;
        int innerRadius = this.getAbsoluteInnerDiameter() / 2;
        int maxLen = outerRadius - innerRadius;
        int min = this.getMin();
        float progress = len * this.getSize() / maxLen + min;
        int n = (int)Math.max(Math.min(progress, this.getMax()), min);
        this.setProgress(n);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.drawTie(canvas);
        this.drawRotatingUzumaki(canvas);
        this.drawHeader(canvas);
    }

    protected void updateHead() {
        int x = this.computeHeadPosition();
        int y = this.getHeight() / 2;
        this.head.changePointerPosition(x, y);
    }

    private void drawHeader(Canvas canvas) {
        Path path = new Path();
        path.moveTo(this.computeHeadPosition(), this.getHeight() / 2);
        path.rLineTo(this.headerSize, - this.headerSize);
        path.rLineTo(-2 * this.headerSize, 0);
        path.close();

        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }

    private void initialize() {
        this.headerSize = 42;
    }

    private int computeHeadPosition() {
        int center = this.getWidth() / 2;
        int innerRadius = this.getAbsoluteInnerDiameter() / 2;
        int step = this.getMax() - this.getProgress();
        int outerRadius = this.getAbsoluteOuterDiameter() / 2;
        int span = (outerRadius - innerRadius) * step / this.getSize();
        return center + innerRadius + span;
    }

    private float computeCurrentAngle() {
        int progress = this.getProgress() - this.getMin();
        return this.getSweepAngle() * (float)progress / this.getSize();
    }

    private void drawRotatingUzumaki(Canvas canvas) {
        int x = this.getWidth() / 2;
        int y = this.getHeight() / 2;
        float angle = this.computeCurrentAngle();
        canvas.save();
        try {
            canvas.rotate(- angle, x, y);
            this.drawUzumaki(canvas);
        }
        finally {
            canvas.restore();
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
