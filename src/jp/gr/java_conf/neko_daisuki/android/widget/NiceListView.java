package jp.gr.java_conf.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import jp.gr.java_conf.neko_daisuki.anaudioplayer.R;

public class NiceListView extends ListView {

    private class SelfListener implements OnScrollListener {

        private class FadeOut implements Runnable {

            private class Invisible implements Runnable {

                public void run() {
                    mView.setVisibility(GONE);
                    invalidateViews();
                }
            }

            private ImageView mView;
            private int mCount = 0;

            public FadeOut(ImageView view) {
                mView = view;
            }

            public void run() {
                float alpha = mView.getAlpha();
                mView.setAlpha(alpha - 0.2f);
                invalidateViews();
                postDelayed(mCount < 4 ? this : new Invisible(), 10);
                mCount++;
            }
        }

        private int mLastOffset;

        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            int offset = computeVerticalScrollOffset();
            if (offset != mLastOffset) {
                int extent = computeVerticalScrollExtent();
                if (offset == 0) {
                    showImage(mTopNoEntryImage);
                }
                else if (offset + extent == computeVerticalScrollRange()) {
                    showImage(mBottomNoEntryImage);
                }
            }
            mLastOffset = offset;
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        private void showImage(ImageView view) {
            view.setVisibility(VISIBLE);
            view.setAlpha(1.0f);
            invalidateViews();
            postDelayed(new FadeOut(view), 400);
        }
    }

    private ImageView mTopNoEntryImage;
    private ImageView mBottomNoEntryImage;

    public NiceListView(Context context) {
        super(context);
        initialize(context);
    }

    public NiceListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public NiceListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        drawImage(canvas, mTopNoEntryImage);
        drawImage(canvas, mBottomNoEntryImage);
    }

    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = mTopNoEntryImage.getMeasuredWidth();
        int height = mTopNoEntryImage.getMeasuredHeight();
        int l = (right - left - width) / 2;
        int r = l + width;
        mTopNoEntryImage.layout(l, 0, r, height);

        int b = bottom - top;
        mBottomNoEntryImage.layout(l, b - height, r, b);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int mode = MeasureSpec.UNSPECIFIED;
        int widthSpec = MeasureSpec.makeMeasureSpec(width, mode);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, mode);
        mTopNoEntryImage.measure(widthSpec, heightSpec);
        mBottomNoEntryImage.measure(widthSpec, heightSpec);
    }

    private void initialize(Context context) {
        mTopNoEntryImage = makeImageView(context);
        mBottomNoEntryImage = makeImageView(context);

        setOverScrollMode(OVER_SCROLL_NEVER);
        setOnScrollListener(new SelfListener());
    }

    private ImageView makeImageView(Context context) {
        ImageView view = new ImageView(context);
        view.setImageResource(R.drawable.ic_no_entry);
        view.setVisibility(GONE);
        return view;
    }

    private void drawImage(Canvas canvas, ImageView view) {
        if (view.getVisibility() == VISIBLE) {
            drawChild(canvas, view, 0);
        }
    }
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
