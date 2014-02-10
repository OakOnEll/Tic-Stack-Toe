package com.oakonell.ticstacktoe.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class SquareRelativeLayoutView extends RelativeLayout {

	public SquareRelativeLayoutView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public SquareRelativeLayoutView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareRelativeLayoutView(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int size = 0;
		int width = getMeasuredWidth();
		int height = getMeasuredHeight();
		int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
		int heigthWithoutPadding = height - getPaddingTop()
				- getPaddingBottom();

		// set the dimensions
		if (widthWithoutPadding > heigthWithoutPadding) {
			size = heigthWithoutPadding;
		} else {
			size = widthWithoutPadding;
		}

		setMeasuredDimension(size + getPaddingLeft() + getPaddingRight(), size
				+ getPaddingTop() + getPaddingBottom());
		Log.i("SquareRelativeLayoutView", "onMeasure, size = " + size);
		if (listener != null) {
			listener.onMeasureCalled(this, size, width, height);
		}
	}

	public interface OnMeasureDependent {
		void onMeasureCalled(SquareRelativeLayoutView squareRelativeLayoutView,
				int size, int origWidth, int origHeight);

	}

	private OnMeasureDependent listener;

	public void setOnMeasureDependent(OnMeasureDependent listener) {
		this.listener = listener;
	}

}
