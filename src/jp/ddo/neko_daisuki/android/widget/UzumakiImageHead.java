package jp.ddo.neko_daisuki.android.widget;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class UzumakiImageHead extends ImageView implements UzumakiHead {

    private interface MotionEventDispatcher {

        public void dispatch(UzumakiImageHead head, MotionEvent event);
    }

    private class FakeDispatcher implements MotionEventDispatcher {

        public void dispatch(UzumakiImageHead head, MotionEvent event) {
            // Does nothing.
        }
    }

    private class ActionMoveDispatcher implements MotionEventDispatcher {

        public void dispatch(UzumakiImageHead head, MotionEvent event) {
            head.onActionMove(event);
        }
    }

    private class ActionDownDispatcher implements MotionEventDispatcher {

        public void dispatch(UzumakiImageHead head, MotionEvent event) {
            head.onActionDown(event);
        }
    }

    private class ActionUpDispatcher implements MotionEventDispatcher {

        public void dispatch(UzumakiImageHead head, MotionEvent event) {
            head.onActionUp(event);
        }
    }

    private Map<Integer, MotionEventDispatcher> dispatchers;
    private UzumakiSlider slider;
    private int xAtDown;
    private int yAtDown;

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
        int action = event.getActionMasked();
        MotionEventDispatcher dispatcher = this.getDispatcher(action);
        dispatcher.dispatch(this, event);
        return true;
    }

    private MotionEventDispatcher getDispatcher(int action) {
        MotionEventDispatcher dispatcher = this.dispatchers.get(action);
        return dispatcher != null ? dispatcher : new FakeDispatcher();
    }

    private void disableActionMove() {
        this.dispatchers.remove(MotionEvent.ACTION_MOVE);
    }

    private void initialize() {
        this.dispatchers = new HashMap<Integer, MotionEventDispatcher>();
        this.dispatchers.put(MotionEvent.ACTION_DOWN, new ActionDownDispatcher());
        this.dispatchers.put(MotionEvent.ACTION_UP, new ActionUpDispatcher());
    }

    private void onActionDown(MotionEvent event) {
        this.slider.fireOnStartHeadMovingListeners();
        this.dispatchers.put(MotionEvent.ACTION_MOVE, new ActionMoveDispatcher());
        this.xAtDown = (int)this.getEventX(event);
        this.yAtDown = (int)this.getEventY(event);
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
        int x = (int)event.getX(pointerIndex) - this.xAtDown + this.getLeft();
        int y = (int)event.getY(pointerIndex) - this.yAtDown + this.getTop();
        this.slider.placeHead(x, y);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
