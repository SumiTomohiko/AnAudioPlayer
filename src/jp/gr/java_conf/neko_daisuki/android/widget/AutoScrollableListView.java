package jp.gr.java_conf.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.ListView;

import jp.gr.java_conf.neko_daisuki.anaudioplayer.R;

public class AutoScrollableListView extends ListView {

    private class AutoScroller implements Runnable {

        private int mDirection;

        public AutoScroller(int direction) {
            mDirection = direction;
        }

        @Override
        public void run() {
            if (!mAutoScrolling) {
                return;
            }
            int distance = 256;
            int duration = 50;
            smoothScrollBy(mDirection * distance, duration);
            postDelayed(this, duration);
        }
    }

    private class Animator {

        private class InvisibleTask implements Runnable {

            @Override
            public void run() {
                mView.setVisibility(GONE);
                invalidate();
            }
        }

        private class FadeOutTask implements Runnable {

            @Override
            public void run() {
                float step = 0.1f;
                float alpha = mView.getAlpha() - step;
                mView.setAlpha(alpha);
                invalidate();
                postDelayed(0.0f < alpha - step ? this : mInvisibleTask, 20);
            }
        }

        private View mView;

        private Runnable mFadeOutTask = new FadeOutTask();
        private Runnable mInvisibleTask = new InvisibleTask();

        public Animator(View view) {
            mView = view;
        }

        public void start() {
            mView.setAlpha(1.0f);
            mView.setVisibility(VISIBLE);
            invalidate();
            postDelayed(mFadeOutTask, 300);
        }
    }

    private enum Direction {
        UP,
        DOWN;
    }

    // State
    private boolean mAutoScrolling;
    private Direction mDirection;
    private float mLastY = 0.0f;
    private int mPrevScrollOffset = 0;

    // Views
    private ImageView mUpArrow;
    private ImageView mDownArrow;
    private ImageView mTopNoEntryIcon;
    private ImageView mBottomNoEntryIcon;
    private Animator mTopNoEntryAnimation;
    private Animator mBottomNoEntryAnimation;

    // Helpers
    private Runnable mUpScroller = new AutoScroller(1);
    private Runnable mDownScroller = new AutoScroller(-1);
    private View[] mViews;
    private View[] mTopViews;
    private View[] mBottomViews;

    public AutoScrollableListView(Context context) {
        super(context);
        initialize(context);
    }

    public AutoScrollableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public AutoScrollableListView(Context context, AttributeSet attrs,
                                  int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            mLastY = event.getY();
            break;
        case MotionEvent.ACTION_MOVE:
            if (Math.abs(event.getY() - mLastY) < 8.0f) {
                return super.onTouchEvent(event);
            }

            float prevY = mLastY;
            mLastY = event.getY();
            if (!mAutoScrolling) {
                if (mLastY < prevY) {
                    mUpArrow.setVisibility(VISIBLE);
                    mDownArrow.setVisibility(GONE);
                    if ((mUpArrow.getTop() <= mLastY) && (mLastY < mUpArrow.getBottom())) {
                        post(mUpScroller);
                        mAutoScrolling = true;
                        mDirection = Direction.UP;
                        return true;
                    }
                }
                else if (prevY < mLastY) {
                    mUpArrow.setVisibility(GONE);
                    mDownArrow.setVisibility(VISIBLE);
                    if ((mDownArrow.getTop() <= mLastY) && (mLastY < mDownArrow.getBottom())) {
                        post(mDownScroller);
                        mAutoScrolling = true;
                        mDirection = Direction.DOWN;
                        return true;
                    }
                }
            }
            else {
                if (mDirection == Direction.UP) {
                    if (prevY < mLastY) {
                        stopAutoScrolling();
                        restartTouch(event);
                        return true;
                    }
                }
                else {
                    if (mLastY < prevY) {
                        stopAutoScrolling();
                        restartTouch(event);
                        return true;
                    }
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            stopAutoScrolling();
            break;
        default:
            break;
        }
        return super.onTouchEvent(event);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.AT_MOST);
        int height = MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec),
                MeasureSpec.UNSPECIFIED);
        for (View view: mViews) {
            view.measure(width, height);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = right - left;
        for (View view: mTopViews) {
            view.layout(0, 0, width, view.getMeasuredHeight());
        }

        int height = bottom - top;
        for (View view: mBottomViews) {
            view.layout(0, height - view.getMeasuredHeight(), width, height);
        }
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        for (View view: mViews) {
            if (view.getVisibility() == VISIBLE) {
                drawChild(canvas, view, 0);
            }
        }
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        int offset = computeVerticalScrollOffset();
        if (offset != mPrevScrollOffset) {
            if (offset == 0) {
                mTopNoEntryAnimation.start();
            }
            else {
                int range = computeVerticalScrollRange();
                int extent = computeVerticalScrollExtent();
                if (range == offset + extent) {
                    mBottomNoEntryAnimation.start();
                }
            }
        }
        mPrevScrollOffset = offset;
    }

    private ImageView makeNoEntryIcon(Context context) {
        ImageView view = new ImageView(context);
        view.setImageResource(R.drawable.ic_no_entry);
        view.setVisibility(GONE);
        return view;
    }

    private ImageView makeArrow(Context context, int resId,
                                int backgroundColor) {
        ImageView view = new ImageView(context);
        view.setBackgroundColor(backgroundColor);
        view.setImageResource(resId);
        return view;
    }

    private void initialize(Context context) {
        setOverScrollMode(OVER_SCROLL_NEVER);

        int backgroundColor = Color.argb(64, 0, 0, 0);
        mUpArrow = makeArrow(context, R.drawable.ic_arrow_up, backgroundColor);
        mDownArrow = makeArrow(context, R.drawable.ic_arrow_down,
                               backgroundColor);
        mTopNoEntryIcon = makeNoEntryIcon(context);
        mTopNoEntryAnimation = new Animator(mTopNoEntryIcon);
        mBottomNoEntryIcon = makeNoEntryIcon(context);
        mBottomNoEntryAnimation = new Animator(mBottomNoEntryIcon);

        stopAutoScrolling();
        mViews = new View[] {
                mUpArrow,
                mDownArrow,
                mTopNoEntryIcon,
                mBottomNoEntryIcon
        };
        mTopViews = new View[] { mUpArrow, mTopNoEntryIcon };
        mBottomViews = new View[] { mDownArrow, mBottomNoEntryIcon };
    }

    private void stopAutoScrolling() {
        mAutoScrolling = false;
        mUpArrow.setVisibility(GONE);
        mDownArrow.setVisibility(GONE);
    }

    private void restartTouch(MotionEvent event) {
        MotionEvent me = MotionEvent.obtain(event);
        try {
            me.setAction(MotionEvent.ACTION_DOWN);
            super.onTouchEvent(me);
        }
        finally {
            me.recycle();
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4