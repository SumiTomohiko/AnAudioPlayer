package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * An instance of this class must have two children. The first child is assumed
 * as a head. The second child is an arm.
 */
public class UzumakiArmHead extends ViewGroup implements UzumakiHead {

    private static final String LOG_TAG = "UzumakiArmHead";

    public UzumakiArmHead(Context context) {
        super(context);
    }

    public UzumakiArmHead(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UzumakiArmHead(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void movePointer(int x, int y, int l, int t, int r, int b) {
        int width = this.getHead().getDrawable().getMinimumWidth();
        this.layout(x - width / 2, t, r, y);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        this.layoutHead(0, 0, width, height);
        this.layoutArm(0, 0, width, height);
    }

    private void layoutHead(int l, int t, int r, int b) {
        ImageView head = this.getHead();
        Drawable drawable = head.getDrawable();
        int width = drawable.getMinimumWidth();
        int height = drawable.getMinimumHeight();
        head.layout(l, b - height, l + width, b);
    }

    private int getBackgroundWidth(View view) {
        /*
         * What is this?
         * =============
         *
         * View.getBackground() returns null at first. Android must delay
         * reading background resource. This method was added for this case with
         * returning zero. Width of zero is harmless.
         */
        Drawable drawable = view.getBackground();
        return drawable != null ? drawable.getMinimumWidth() : 0;
    }

    private void layoutArm(int l, int t, int r, int b) {
        View arm = this.getArm();
        int width = this.getBackgroundWidth(arm);
        int left = l + this.getHalfOfHeadWidth() - width / 2;
        int bottom = b - this.getHalfOfHeadHeight();
        arm.layout(left, t, r, bottom);
    }

    private Drawable getDrawableOfHead() {
        return this.getHead().getDrawable();
    }

    private int getHalfOfHeadWidth() {
        return this.getDrawableOfHead().getMinimumWidth() / 2;
    }

    private int getHalfOfHeadHeight() {
        return this.getDrawableOfHead().getMinimumHeight() / 2;
    }

    private View getArm() {
        return this.getChildAt(1);
    }

    private ImageView getHead() {
        return (ImageView)this.getChildAt(0);
    }

    private void debug(String msg) {
        Log.d(LOG_TAG, msg);
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
