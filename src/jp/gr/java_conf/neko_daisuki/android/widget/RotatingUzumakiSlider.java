package jp.gr.java_conf.neko_daisuki.android.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import jp.gr.java_conf.neko_daisuki.android.view.MotionEventDispatcher;

public class RotatingUzumakiSlider extends UzumakiSlider {

    public interface OnStartRotatingListener {

        public void onStartRotating(RotatingUzumakiSlider slider);
    }

    public interface OnStopRotatingListener {

        public void onStopRotating(RotatingUzumakiSlider slider);
    }

    private abstract class MotionEventProc implements MotionEventDispatcher.Proc {

        private RotatingUzumakiSlider mSlider;

        public MotionEventProc(RotatingUzumakiSlider slider) {
            mSlider = slider;
        }

        public boolean run(MotionEvent event) {
            return callback(mSlider, event);
        }

        protected abstract boolean callback(RotatingUzumakiSlider slider, MotionEvent event);
    }

    private class ActionDownProc extends MotionEventProc {

        public ActionDownProc(RotatingUzumakiSlider slider) {
            super(slider);
        }

        protected boolean callback(RotatingUzumakiSlider slider, MotionEvent event) {
            return slider.onActionDown(event);
        }
    }

    private class ActionMoveProc extends MotionEventProc {

        public ActionMoveProc(RotatingUzumakiSlider slider) {
            super(slider);
        }

        protected boolean callback(RotatingUzumakiSlider slider, MotionEvent event) {
            return slider.onActionMove(event);
        }
    }

    private class ActionUpProc extends MotionEventProc {

        public ActionUpProc(RotatingUzumakiSlider slider) {
            super(slider);
        }

        protected boolean callback(RotatingUzumakiSlider slider, MotionEvent event) {
            return slider.onActionUp(event);
        }
    }

    private List<OnStartRotatingListener> mOnStartRotatingListeners;
    private List<OnStopRotatingListener> mOnStopRotatingListeners;
    private MotionEventDispatcher mDispatcher;
    private double mRotationAngle;  // [degree]

    private int mHeaderSize;

    public RotatingUzumakiSlider(Context context) {
        super(context);
        initialize();
    }

    public RotatingUzumakiSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public RotatingUzumakiSlider(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mDispatcher.dispatch(event);
    }

    public void addOnStartRotatingListener(OnStartRotatingListener listener) {
        mOnStartRotatingListeners.add(listener);
    }

    public void addOnStopRotatingListener(OnStopRotatingListener listener) {
        mOnStopRotatingListeners.add(listener);
    }

    public void slideHead(int progressOld, float deltaX, float deltaY) {
        int outerRadius = getAbsoluteOuterDiameter() / 2;
        int innerRadius = getAbsoluteInnerDiameter() / 2;
        int maxLen = outerRadius - innerRadius;
        int range = getMax() - getMin();
        float ratio = (float)range / maxLen;
        float deltaProgress = (-1) * deltaX * ratio;
        setProgress(progressOld + (int)deltaProgress);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawDisc(canvas);
        drawRotatingUzumaki(canvas);

        // The following statement is useful in debug.
        //this.drawHeader(canvas);
    }

    protected void layoutHead(View head, int l, int t, int r, int b) {
        int x = computeHeadPosition();
        int y = getHeight() / 2;
        ((UzumakiHead)head).movePointer(l + x, t + y, l, t, r, b);
    }

    @SuppressWarnings("unused")
    private void drawHeader(Canvas canvas) {
        /*
         * I do not remove this unused method because this is useful in
         * debugging.
         */
        Path path = new Path();
        path.moveTo(computeHeadPosition(), getHeight() / 2);
        path.rLineTo(mHeaderSize, - mHeaderSize);
        path.rLineTo(-2 * mHeaderSize, 0);
        path.close();

        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
    }

    private void initialize() {
        mOnStartRotatingListeners = new ArrayList<OnStartRotatingListener>();
        mOnStopRotatingListeners = new ArrayList<OnStopRotatingListener>();
        mDispatcher = new MotionEventDispatcher();
        mDispatcher.setDownProc(new ActionDownProc(this));
        mDispatcher.setMoveProc(new ActionMoveProc(this));
        mDispatcher.setUpProc(new ActionUpProc(this));

        mHeaderSize = 42;
    }

    private int computeHeadPosition() {
        int center = getWidth() / 2;
        int innerRadius = getAbsoluteInnerDiameter() / 2;
        int step = getMax() - getProgress();
        int outerRadius = getAbsoluteOuterDiameter() / 2;
        int span = (outerRadius - innerRadius) * step / getSize();
        return center + innerRadius + span;
    }

    private float computeCurrentAngle() {
        return getSweepAngle() * (float)(getProgress() - getMin()) / getSize();
    }

    private void drawRotatingUzumaki(Canvas canvas) {
        int x = getWidth() / 2;
        int y = getHeight() / 2;
        float angle = computeCurrentAngle();
        canvas.save();
        try {
            canvas.rotate(- angle, x, y);
            drawUzumaki(canvas);
        }
        finally {
            canvas.restore();
        }
    }

    private boolean onActionDown(MotionEvent event) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float x = event.getX();
        float y = event.getY();
        float deltaX = x - centerX;
        float deltaY = y - centerY;
        double len = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        int outerRadius = getOutlineOuterDiameter() / 2;
        int innerRadius = getAbsoluteOutlineInnerDiameter() / 2;
        boolean isTarget = (innerRadius <= len) && (len <= outerRadius);

        return isTarget ? startRotating(x, y) : false;
    }

    private double computeRotationAngle(float x, float y) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float deltaX = x - centerX;
        float deltaY = y - centerY;
        double radian = Math.atan2(deltaY, deltaX);
        return Math.toDegrees(radian);
    }

    private boolean startRotating(float x, float y) {
        mRotationAngle = computeRotationAngle(x, y);

        for (OnStartRotatingListener listener: mOnStartRotatingListeners) {
            listener.onStartRotating(this);
        }

        /*
         * Return value's type of this method must be void. But to make the
         * last expression of onActionDown() simple, this method always returns
         * true. This is a bad way exactly.
         */
        return true;
    }

    private boolean onActionMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        double angle = computeRotationAngle(x, y);
        double angleDelta = angle - mRotationAngle;
        double absDelta = Math.abs(angleDelta);
        double calibration = absDelta < 180 ? 0 : angleDelta / absDelta * 360;
        double actualDelta = angleDelta - calibration;
        double progressDelta = actualDelta / getSweepAngle() * getSize() + getMin();
        int direction = 0 < getSweepAngle() ? 1 : -1;
        setProgress(getProgress() + direction * (int)progressDelta);
        mRotationAngle = angle;

        return true;
    }

    private boolean onActionUp(MotionEvent event) {
        for (OnStopRotatingListener listener: mOnStopRotatingListeners) {
            listener.onStopRotating(this);
        }

        return true;
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
