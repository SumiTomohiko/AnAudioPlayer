package jp.ddo.neko_daisuki.android.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public class UzumakiDiagram
{
    private int start_angle;
    private int sweep_angle;
    private int outer_diameter;
    private int inner_diameter;
    private Paint paint;

    public UzumakiDiagram(int start_angle, int sweep_angle, int outer_diameter, int inner_diameter, Paint paint) {
        this.start_angle = start_angle;
        this.sweep_angle = sweep_angle;
        this.outer_diameter = outer_diameter;
        this.inner_diameter = inner_diameter;
        this.paint = paint;
    }

    public void draw(Canvas canvas) {
        int outer_diameter = this.outer_diameter == 0 ? Math.min(canvas.getWidth(), canvas.getHeight()) : this.outer_diameter;
        this.draw(canvas, outer_diameter, this.inner_diameter);
    }

    private void draw(Canvas canvas, int outer_diameter, int inner_diameter) {
        int left = (canvas.getWidth() - outer_diameter) / 2;
        int top = (canvas.getHeight() - outer_diameter) / 2;
        RectF oval = new RectF(left, top, left + outer_diameter, top + outer_diameter);

        Path path = new Path();
        int diameter_step = (outer_diameter - inner_diameter) / (Math.abs(this.sweep_angle) / 180) / 2;
        RectShrinker verticalShrinker = new VerticalShrinker(diameter_step);
        RectShrinker horizontalShrinker = new HorizontalShrinker(diameter_step);
        int angle_step = (this.sweep_angle < 0 ? -1 : 1) * 90;
        int angle;
        for (angle = this.start_angle; angle != this.sweep_angle; angle += angle_step) {
            RectShrinker shrinker = angle % 180 == 0 ? verticalShrinker : horizontalShrinker;
            shrinker.shrink(oval);
            path.addArc(oval, angle, angle_step);
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
