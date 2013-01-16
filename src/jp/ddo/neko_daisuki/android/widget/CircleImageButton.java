package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageButton;

public class CircleImageButton extends ImageButton {

    public CircleImageButton(Context context) {
        super(context);
    }

    public CircleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float deltaX = event.getX() - this.getCenterX();
        float deltaY = event.getY() - this.getCenterY();
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        boolean isInCircle = distance < this.getRadius();
        return isInCircle ? super.dispatchTouchEvent(event) : false;
    }

    private int getRadius() {
        return Math.min(this.getWidth(), this.getHeight()) / 2;
    }

    private int getCenterX() {
        return this.getWidth() / 2;
    }

    private int getCenterY() {
        return this.getHeight() / 2;
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
