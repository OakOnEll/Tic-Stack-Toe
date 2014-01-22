package com.oakonell.ticstacktoe.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomFontTextView extends TextView {
	public CustomFontTextView(Context context) {
		super(context);
		init();
	}

	public CustomFontTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CustomFontTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void init() {
		if (isInEditMode())
			return;
		Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/CabinSketch-Bold.ttf");
		setTypeface(tf);
	}
}
