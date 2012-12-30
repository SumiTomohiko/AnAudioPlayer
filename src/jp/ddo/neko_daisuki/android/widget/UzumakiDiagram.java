package jp.ddo.neko_daisuki.android.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class UzumakiDiagram
{
    private int x;
    private int y;
    private int startAngle;
    private int sweepAngle;
    private int outerDiameter;
    private int innerDiameter;
    private Paint paint;

    public UzumakiDiagram(int x, int y, int startAngle, int sweepAngle, int outerDiameter, int innerDiameter, Paint paint) {
        this.x = x;
        this.y = y;
        this.startAngle = startAngle;
        this.sweepAngle = sweepAngle;
        this.outerDiameter = outerDiameter;
        this.innerDiameter = innerDiameter;
        this.paint = paint;
    }

    public void draw(Canvas canvas) {
        int outerDiameter = this.outerDiameter;
        int innerDiameter = this.innerDiameter;
        int left = this.x - outerDiameter / 2;
        int top = this.y - outerDiameter / 2;
        RectF oval = new RectF(left, top, left + outerDiameter, top + outerDiameter);

        Path path = new Path();
        int diameterStep = (outerDiameter - innerDiameter) / (Math.abs(this.sweepAngle) / 180) / 2;
        RectShrinker verticalShrinker = new VerticalShrinker(diameterStep);
        RectShrinker horizontalShrinker = new HorizontalShrinker(diameterStep);
        int angleStep = (this.sweepAngle < 0 ? -1 : 1) * 90;
        int angle;
        for (angle = this.startAngle; angle != this.sweepAngle; angle += angleStep) {
            RectShrinker shrinker = angle % 180 == 0 ? verticalShrinker : horizontalShrinker;
            shrinker.shrink(oval);
            path.addArc(oval, angle, angleStep);
        }

        canvas.drawPath(path, this.paint);
    }

    private abstract class RectShrinker {

        public RectShrinker(int size) {
            this.size = size;
        }

        public abstract void shrink(RectF rect);

        protected int size;
    }

    private class HorizontalShrinker extends RectShrinker {

        public HorizontalShrinker(int size) {
            super(size);
        }

        public void shrink(RectF rect) {
            rect.left += this.size;
            rect.right -= this.size;
        }
    }

    private class VerticalShrinker extends RectShrinker {

        public VerticalShrinker(int size) {
            super(size);
        }

        public void shrink(RectF rect) {
            rect.bottom -= this.size;
            rect.top += this.size;
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
