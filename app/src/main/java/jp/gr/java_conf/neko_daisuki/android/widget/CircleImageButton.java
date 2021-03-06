package jp.gr.java_conf.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

import jp.gr.java_conf.neko_daisuki.android.view.MotionEventDispatcher;

public class CircleImageButton extends ImageButton {

    private abstract class MotionEventProc implements MotionEventDispatcher.Proc {

        protected CircleImageButton mButton;

        public MotionEventProc(CircleImageButton button) {
            mButton = button;
        }

        public abstract boolean run(MotionEvent event);
    }

    private class ActionDownEventProc extends MotionEventProc {

        public ActionDownEventProc(CircleImageButton button) {
            super(button);
        }

        public boolean run(MotionEvent event) {
            /*
             * About ActionDownEventProc and ActionUpEventProc
             * ===============================================
             *
             * I expected that View.onTouchEvent() does everything needed
             * (changing button state to pressed, firing event listeners and
             * invalidating). But actual is not. So I implemented all manually.
             *
             * At first, what I did at here was calling
             * this.button.invalidte(). But background of the button did not
             * change. I do not know why exactly. Android source code (*1)
             * shows that the button state is changed to PREPRESSED at first.
             * Changing to PRESSED is happened later. This fact might relate.
             *
             * Android 4.0 (API level 14) has
             * ViewGroup.shouldDelayChildPressedState(). Overriding it to
             * return false in UzumakiSlider may be useful (*2. See definitions
             * of onTouchEvent() and isInScrollingContainer()). But I am using
             * Android 3.2, I did not try it.
             *
             * (*1) http://tools.oesf.biz/android-2.3.7_r1.0/xref/frameworks/base/core/java/android/view/View.java
             * (*2) http://tools.oesf.biz/android-4.0.1_r1.0/xref/frameworks/base/core/java/android/view/View.java
             */
            mButton.setPressed(true);
            mButton.invalidate();
            return true;
        }
    }

    private class ActionUpEventProc extends MotionEventProc {

        public ActionUpEventProc(CircleImageButton button) {
            super(button);
        }

        public boolean run(MotionEvent event) {
            mButton.setPressed(false);
            mButton.invalidate();
            mButton.performClick();
            return true;
        }
    }

    private abstract class Drawer {

        protected CircleImageButton mButton;
        protected int mEdgeWidth;

        public Drawer(CircleImageButton button) {
            mButton = button;
            mEdgeWidth = 2;
        }

        public abstract void draw(Canvas canvas, int centerX, int centerY,
                                  int outerRadius);

        protected int computeInnerRadius(int outerRadius) {
            return (int)(0.92 * outerRadius);
        }

        protected void drawButton(Canvas canvas, int centerX, int centerY,
                                  int outerRadius) {
            int innerRadius = computeInnerRadius(outerRadius);
            RectF outerRect = new RectF(centerX - outerRadius,
                                        centerY - outerRadius,
                                        centerX + outerRadius,
                                        centerY + outerRadius);
            RectF innerRect = new RectF(centerX - innerRadius,
                                        centerY - innerRadius,
                                        centerX + innerRadius,
                                        centerY + innerRadius);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);

            Paint mainPaint = new Paint(paint);
            mainPaint.setColor(mButton.mBackgroundColor);
            canvas.drawCircle(centerX, centerY, outerRadius, mainPaint);

            Path topTie = new Path();
            topTie.addArc(outerRect, 0, -180);
            topTie.lineTo(centerX - innerRadius, centerY);
            topTie.addArc(innerRect, -180, 180);
            topTie.lineTo(centerX + outerRadius, centerY);
            Paint topTiePaint = new Paint(paint);
            topTiePaint.setColor(mButton.mBrightColor);
            canvas.drawPath(topTie, topTiePaint);

            Path bottomTie = new Path();
            bottomTie.addArc(outerRect, 0, 180);
            bottomTie.lineTo(centerX - innerRadius, centerY);
            bottomTie.addArc(innerRect, 180, -180);
            bottomTie.lineTo(centerX + outerRadius, centerY);
            Paint bottomTiePaint = new Paint(paint);
            bottomTiePaint.setColor(mButton.mShadowColor);
            canvas.drawPath(bottomTie, bottomTiePaint);

            float radius = 0.86f * outerRadius;
            RectF oval = new RectF(centerX - radius,
                                   centerY - radius,
                                   centerX + radius,
                                   centerY + radius);
            Path circle = new Path();
            circle.addArc(oval, 0, -180);
            Paint circlePaint = new Paint(paint);
            circlePaint.setColor(mButton.mBrightColor);
            circlePaint.setStrokeWidth(1);
            circlePaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(circle, circlePaint);
            Path circle2 = new Path();
            circle2.addArc(oval, 0, 180);
            Paint circlePaint2 = new Paint(paint);
            circlePaint2.setColor(mButton.mShadowColor);
            circlePaint2.setStrokeWidth(1);
            circlePaint2.setStyle(Paint.Style.STROKE);
            canvas.drawPath(circle2, circlePaint2);
        }

        protected void drawEdge(Canvas canvas, int centerX, int centerY,
                                int radius) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(mEdgeWidth);
            RectF rect = new RectF(centerX - radius, centerY - radius,
                                   centerX + radius, centerY + radius);

            paint.setColor(mButton.mShadowColor);
            canvas.drawArc(rect, 0, -180, false, paint);

            //paint.setColor(this.button.brightColor);
            canvas.drawArc(rect, 0, 180, false, paint);
        }
    }

    private class NeutralDrawer extends Drawer {

        public NeutralDrawer(CircleImageButton button) {
            super(button);
        }

        public void draw(Canvas canvas, int centerX, int centerY,
                         int outerRadius) {
            drawButton(canvas, centerX, centerY, outerRadius - mEdgeWidth);
            drawEdge(canvas, centerX, centerY, outerRadius);
        }
    }

    private class PressedDrawer extends Drawer {

        public PressedDrawer(CircleImageButton button) {
            super(button);
        }

        public void draw(Canvas canvas, int centerX, int centerY,
                         int outerRadius) {
            int radius = (int)(0.99 * (outerRadius - mEdgeWidth));
            drawButton(canvas, centerX, centerY, radius);
            drawShadow(canvas, centerX, centerY, outerRadius);
            drawEdge(canvas, centerX, centerY, outerRadius);
        }

        private void drawShadow(Canvas canvas, int centerX, int centerY,
                                int radius) {
            Path path = new Path();
            RectF rect = new RectF(centerX - radius,
                                   centerY - radius,
                                   centerX + radius,
                                   centerY + radius);
            path.addArc(rect, 0, -180);
            RectF rect2 = new RectF((float)(centerX - radius),
                                    (float)(centerY - 0.96 * radius),
                                    (float)(centerX + radius),
                                    (float)(centerY + 0.96 * radius));
            path.arcTo(rect2, -180, 180);
            Paint paint = new Paint();
            paint.setColor(0xff000000);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, paint);
        }
    }

    private int mPadding;
    private int mBackgroundColor;
    private int mBrightColor;
    private int mShadowColor;
    private MotionEventDispatcher mMotionEventDispatcher;
    private Drawer mNeutralDrawer;
    private Drawer mPressedDrawer;

    public CircleImageButton(Context context) {
        super(context);
        initialize();
    }

    public CircleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public CircleImageButton(Context context, AttributeSet attrs,
                             int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float deltaX = event.getX() - getCenterX();
        float deltaY = event.getY() - getCenterY();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        boolean isInCircle = distance < getRadius();
        return isInCircle ? super.dispatchTouchEvent(event) : false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mMotionEventDispatcher.dispatch(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getCenterX();
        int centerY = getCenterY();
        Drawer drawer = isPressed() ? mPressedDrawer : mNeutralDrawer;
        drawer.draw(canvas, centerX, centerY, getRadius());

        super.onDraw(canvas);
    }

    private int getRadius() {
        return Math.min(getWidth(), getHeight()) / 2 - mPadding;
    }

    private int getCenterX() {
        return getWidth() / 2;
    }

    private int getCenterY() {
        return getHeight() / 2;
    }

    private void initialize() {
        mPadding = 1;
        mBackgroundColor = Color.argb(255, 0xcc, 0xcc, 0xcc);
        mBrightColor = Color.argb(255, 255, 255, 255);
        mShadowColor = Color.argb(255, 138, 138, 138);
        mMotionEventDispatcher = new MotionEventDispatcher();
        mMotionEventDispatcher.setDownProc(new ActionDownEventProc(this));
        mMotionEventDispatcher.setUpProc(new ActionUpEventProc(this));
        mNeutralDrawer = new NeutralDrawer(this);
        mPressedDrawer = new PressedDrawer(this);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
