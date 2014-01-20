package com.oakonell.ticstacktoe.ui.game;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.utils.activity.dragndrop.DragSource;
import com.oakonell.utils.activity.dragndrop.ImageDropTarget;

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
