package jp.gr.java_conf.neko_daisuki.android.widget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public class UzumakiDiagram
{
    private Point mPoint = new Point();
    private Path mPath = new Path();

    public void draw(Canvas canvas, int centerX, int centerY, int startAngle,
                     int sweepAngle, int outerDiameter, int innerDiameter,
                     Paint paint) {
        computePoint(mPoint, startAngle, centerX, centerY, startAngle,
                     sweepAngle, outerDiameter, innerDiameter);
        mPath.rewind();
        mPath.moveTo(mPoint.x, mPoint.y);

        /*
         * About "resolution"
         * ==================
         *
         * I'm using Acer A500. Its screen is 10inch (25.4cm) and 1270x800pixel.
         * So it is 0.17mm/pixel. If I draw one circle as large as possible
         * (in other words, the circle's radius is 400pixel), the length of its
         * outline is about 2,500pixel (because of 2 * pi * radius). The length
         * for one degree is about 7pixel (2,500 / 360), it will be 2mm
         * (7pixel * 0.17mm/pixel).
         *
         * The outline length of four degrees is about 8mm. I felt that it is
         * smooth enough.
         */
        int resolution = 4;
        int direction = 0 < sweepAngle ? 1 : -1;
        int lastAngle = Math.abs(sweepAngle) + resolution;
        for (int angle = 0; angle < lastAngle; angle += resolution) {
            computePoint(mPoint, startAngle + direction * angle, centerX,
                         centerY, startAngle, sweepAngle, outerDiameter,
                         innerDiameter);
            mPath.lineTo(mPoint.x, mPoint.y);
        }

        canvas.drawPath(mPath, paint);
    }

    private void computePoint(Point dest, int angle, int centerX, int centerY,
                              int startAngle, int sweepAngle, int outerDiameter,
                              int innerDiameter) {
        int diameterDelta = outerDiameter - innerDiameter;
        float ratio = (float)(startAngle - angle) / Math.abs(sweepAngle);
        float radius = (outerDiameter - diameterDelta * ratio) / 2;
        float x = radius * (float)Math.cos(Math.toRadians(angle));
        float y = radius * (float)Math.sin(Math.toRadians(angle));
        dest.x = centerX + x;
        dest.y = centerY + y;
    }

    private class Point {

        public float x = 0.0f;
        public float y = 0.0f;
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
