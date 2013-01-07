package jp.ddo.neko_daisuki.android.widget;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import jp.ddo.neko_daisuki.android.view.MotionEventDispatcher;

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

    public void changePointerPosition(int pointerX, int pointerY) {
        this.move(pointerX - this.getWidth() / 2, pointerY - this.getHeight());
    }

    public void setSlider(UzumakiSlider slider) {
        this.slider = slider;
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

    private void onActionDown(MotionEvent event) {
        this.slider.fireOnStartHeadMovingListeners();
        this.enableActionMove();
        this.xAtDown = this.getEventX(event);
        this.yAtDown = this.getEventY(event);
    }

    private void onActionUp(MotionEvent event) {
        this.slider.fireOnStopHeadMovingListeners();
        this.disableActionMove();
    }

    private float getEventX(MotionEvent event) {
        return event.getX(event.getPointerId(0));
    }

    private float getEventY(MotionEvent event) {
        return event.getY(event.getPointerId(0));
    }

    private void move(int left, int top) {
        // API level 11 has View.setX()/setY().
        this.layout(left, top, left + this.getWidth(), top + this.getHeight());
    }

    private void onActionMove(MotionEvent event) {
        int pointerIndex = event.getPointerId(0);
        float x = event.getX(pointerIndex) - this.xAtDown + this.getLeft() - this.slider.getLeft();
        float y = event.getY(pointerIndex) - this.yAtDown + this.getTop() - this.slider.getTop();
        this.slider.placeHead(x + this.getWidth() / 2, y + this.getHeight());
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
