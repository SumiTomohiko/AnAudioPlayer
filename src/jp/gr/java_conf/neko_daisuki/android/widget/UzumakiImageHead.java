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

        private UzumakiImageHead head;

        public MotionEventProc(UzumakiImageHead head) {
            this.head = head;
        }

        public boolean run(MotionEvent event) {
            this.callback(this.head, event);
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

    private MotionEventDispatcher dispatcher;
    private UzumakiSlider slider;
    private float xAtDown;
    private float yAtDown;
    private int progressAtDown;

    public UzumakiImageHead(Context context) {
        super(context);
        this.initialize();
    }

    public UzumakiImageHead(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.initialize();
    }

    public UzumakiImageHead(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.initialize();
    }

    public void setSlider(UzumakiSlider slider) {
        this.slider = slider;
    }

    public void movePointer(int x, int y, int l, int t, int r, int b) {
        Drawable d = this.getDrawable();
        int width = d.getMinimumWidth();
        int height = d.getMinimumHeight();
        this.layout(x - width / 2, y - height, x + width / 2, y);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return this.dispatcher.dispatch(event);
    }

    private void disableActionMove() {
        this.dispatcher.removeMoveProc();
    }

    private void enableActionMove() {
        this.dispatcher.setMoveProc(new ActionMoveProc(this));
    }

    private void initialize() {
        this.dispatcher = new MotionEventDispatcher();
        this.dispatcher.setDownProc(new ActionDownProc(this));
        this.dispatcher.setUpProc(new ActionUpProc(this));
    }

    private float convertToSliderAxisX(float x) {
        View view = this;
        float parentX = x + view.getLeft();
        while ((view = (View)view.getParent()) != this.slider) {
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
        this.slider.fireOnStartHeadMovingListeners(this);
        this.enableActionMove();
        this.xAtDown = this.convertToSliderAxisX(event.getX());
        this.yAtDown = this.convertToSliderAxisY(event.getY());
        this.progressAtDown = this.slider.getProgress();
    }

    private void onActionUp(MotionEvent event) {
        this.slider.fireOnStopHeadMovingListeners(this);
        this.disableActionMove();
    }

    private void onActionMove(MotionEvent event) {
        float x = this.convertToSliderAxisX(event.getX());
        float y = this.convertToSliderAxisY(event.getY());
        float deltaX = x - this.xAtDown;
        float deltaY = y - this.yAtDown;
        this.slider.slideHead(this.progressAtDown, deltaX, deltaY);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
