package com.oakonell.ticstacktoe.ui.game;

import android.content.Context;
import android.util.AttributeSet;

import com.oakonell.utils.activity.dragndrop.ImageDropTarget;

/**
 * UI Component for pieces on the board- implementing DropTarget to have pieces
 * dropped on it.
 * 
 */
public class BoardPieceStackImageView extends ImageDropTarget {

	public BoardPieceStackImageView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public BoardPieceStackImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BoardPieceStackImageView(Context context) {
		super(context);
	}

}
