package jp.gr.java_conf.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import jp.gr.java_conf.neko_daisuki.android.view.MotionEventDispatcher;

public class UzumakiImageHead extends ImageView implements UzumakiHead {

    private abstract class MotionEventProc implements MotionEventDispatcher.Proc {

        private UzumakiImageHead mHead;

        public MotionEventProc(UzumakiImageHead head) {
            mHead = head;
        }

        public boolean run(MotionEvent event) {
            callback(mHead, event);
            return true;
        }

        protected abstract void callback(UzumakiImageHead head, MotionEvent event);
    }

    private class ActionMoveProc extends MotionEventProc {

        public ActionMoveProc(UzumakiImageHead head) {
            super(head);
        }

        protected void callback(UzumakiImageHead head, MotionEvent event) {
            head.onActionMove(event);
        }
    }

    private class ActionDownProc extends MotionEventProc {

        public ActionDownProc(UzumakiImageHead head) {
            super(head);
        }

        protected void callback(UzumakiImageHead head, MotionEvent event) {
            head.onActionDown(event);
        }
    }

    private class ActionUpProc extends MotionEventProc {

        public ActionUpProc(UzumakiImageHead head) {
            super(head);
        }

        protected void callback(UzumakiImageHead head, MotionEvent event) {
            head.onActionUp(event);
        }
    }

    private MotionEventDispatcher mDispatcher;
    private UzumakiSlider mSlider;
    private float mXAtDown;
    private float mYAtDown;
    private int mProgressAtDown;

    public UzumakiImageHead(Context context) {
        super(context);
        initialize();
    }

    public UzumakiImageHead(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public UzumakiImageHead(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public void setSlider(UzumakiSlider slider) {
        mSlider = slider;
    }

    public void movePointer(int x, int y, int l, int t, int r, int b) {
        Drawable d = getDrawable();
        int width = d.getMinimumWidth();
        int height = d.getMinimumHeight();
        layout(x - width / 2, y - height, x + width / 2, y);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mDispatcher.dispatch(event);
    }

    private void disableActionMove() {
        mDispatcher.removeMoveProc();
    }

    private void enableActionMove() {
        mDispatcher.setMoveProc(new ActionMoveProc(this));
    }

    private void initialize() {
        mDispatcher = new MotionEventDispatcher();
        mDispatcher.setDownProc(new ActionDownProc(this));
        mDispatcher.setUpProc(new ActionUpProc(this));
    }

    private float convertToSliderAxisX(float x) {
        View view = this;
        float parentX = x + view.getLeft();
        while ((view = (View)view.getParent()) != mSlider) {
            parentX += view.getLeft();
        }

        return parentX;
    }

    private float convertToSliderAxisY(float y) {
        /*
         * The return value of this method is not used in current applications.
         * So I cannot test this method. I cannot commit code which was not
         * tested. This is why I did not implement this method.
         */
        return 0.0f;
    }

    private void onActionDown(MotionEvent event) {
        mSlider.fireOnStartHeadMovingListeners(this);
        enableActionMove();
        mXAtDown = convertToSliderAxisX(event.getX());
        mYAtDown = convertToSliderAxisY(event.getY());
        mProgressAtDown = mSlider.getProgress();
    }

    private void onActionUp(MotionEvent event) {
        mSlider.fireOnStopHeadMovingListeners(this);
        disableActionMove();
    }

    private void onActionMove(MotionEvent event) {
        float x = convertToSliderAxisX(event.getX());
        float y = convertToSliderAxisY(event.getY());
        float deltaX = x - mXAtDown;
        float deltaY = y - mYAtDown;
        mSlider.slideHead(mProgressAtDown, deltaX, deltaY);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
