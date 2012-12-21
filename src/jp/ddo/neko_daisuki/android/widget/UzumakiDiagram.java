package jp.ddo.neko_daisuki.android.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class UzumakiDiagram extends View
{
	private int inner_diameter;
	private int outer_diameter;
	private int start_angle;
	private int sweep_angle;

	private static final int DEFAULT_SWEEP_ANGLE = -3 * 360;

	public UzumakiDiagram(Context context) {
		super(context);

		this.inner_diameter = 0;
		this.outer_diameter = 0;
		this.start_angle = 0;
		this.sweep_angle = DEFAULT_SWEEP_ANGLE;
	}

	public UzumakiDiagram(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.readAttribute(attrs);
	}

	public UzumakiDiagram(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.readAttribute(attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int size = Math.min(this.getWidth(), this.getHeight());
		int outer_diameter = this.outer_diameter == 0 ? size : this.outer_diameter;
		int inner_diameter = this.inner_diameter == 0 ? (outer_diameter / 10 * 9) : this.inner_diameter;
		this.draw(canvas, outer_diameter, inner_diameter);
	}

	private void draw(Canvas canvas, int outer_diameter, int inner_diameter) {
		int left = (this.getWidth() - outer_diameter) / 2;
		int top = (this.getHeight() - outer_diameter) / 2;
		RectF oval = new RectF(left, top, left + outer_diameter, top + outer_diameter);

		Path path = new Path();
		int diameter_step = (outer_diameter - inner_diameter) / (Math.abs(this.sweep_angle) / 180) / 2;
		int angle_step = (this.sweep_angle < 0 ? -1 : 1) * 90;
		int angle;
		for (angle = this.start_angle; angle != this.sweep_angle; angle += angle_step) {
			if ((angle % 180) == 0) {
				oval.bottom -= diameter_step;
				oval.top += diameter_step;
			}
			else {
				oval.left += diameter_step;
				oval.right -= diameter_step;
			}
			path.addArc(oval, angle, angle_step);
		}

		Paint paint = new Paint();
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(0xffffffff);

		canvas.drawPath(path, paint);
	}

	private void readAttribute(AttributeSet attrs) {
		this.inner_diameter = attrs.getAttributeIntValue(null, "inner_diameter", 0);
		this.outer_diameter = attrs.getAttributeIntValue(null, "outer_diameter", 0);
		this.start_angle = attrs.getAttributeIntValue(null, "start_angle", 0);
		this.sweep_angle = attrs.getAttributeIntValue(null, "sweep_angle", DEFAULT_SWEEP_ANGLE);
	}
}
