package com.oakonell.ticstacktoe.ui.game;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.oakonell.ticstacktoe.model.Game.ByteBufferDebugger;

public class WinOverlayView extends View {
	private final int OFFSET = 10;
	private Paint linePaint;

	int boardSize;

	public enum WinStyle {
		ROW1, ROW2, ROW3, ROW4, ROW5, //
		COL1, COL2, COL3, COL4, COL5, //
		TOP_LEFT_DIAG, //
		TOP_RIGHT_DIAG; //

		public static WinStyle column(int x) {
			if (x == 0)
				return COL1;
			if (x == 1)
				return COL2;
			if (x == 2)
				return COL3;
			if (x == 3)
				return COL4;
			if (x == 4)
				return COL5;
			throw new RuntimeException("Invalid column " + x);
		}

		public static WinStyle row(int y) {
			if (y == 0)
				return ROW1;
			if (y == 1)
				return ROW2;
			if (y == 2)
				return ROW3;
			if (y == 3)
				return ROW4;
			if (y == 4)
				return ROW5;
			throw new RuntimeException("Invalid row " + y);
		}

		public static WinStyle fromByte(ByteBufferDebugger buffer) {
			byte b = buffer.get("Win type");
			if (b == 'R')
				return row(buffer.get("win row"));
			if (b == 'C')
				return column(buffer.get("win col"));
			if (b == 'D') {
				byte diagType = buffer.get("win diag type");
				if (diagType == 'L')
					return TOP_LEFT_DIAG;
				if (diagType == 'R')
					return TOP_RIGHT_DIAG;
				throw new RuntimeException("Invalid diagonal type win");
			}
			throw new RuntimeException("Invalid win type");
		}

		public void writeBytes(ByteBufferDebugger dBuffer) {
			ByteBuffer buffer = dBuffer.getBuffer();
			if (this == ROW1) {
				buffer.put((byte) 'R');
				buffer.put((byte) 0);
			} else if (this == ROW2) {
				buffer.put((byte) 'R');
				buffer.put((byte) 1);
			} else if (this == ROW3) {
				buffer.put((byte) 'R');
				buffer.put((byte) 2);
			} else if (this == ROW4) {
				buffer.put((byte) 'R');
				buffer.put((byte) 3);
			} else if (this == ROW5) {
				buffer.put((byte) 'R');
				buffer.put((byte) 4);
			} else if (this == COL1) {
				buffer.put((byte) 'C');
				buffer.put((byte) 0);
			} else if (this == COL2) {
				buffer.put((byte) 'C');
				buffer.put((byte) 1);
			} else if (this == COL3) {
				buffer.put((byte) 'C');
				buffer.put((byte) 2);
			} else if (this == COL4) {
				buffer.put((byte) 'C');
				buffer.put((byte) 3);
			} else if (this == COL5) {
				buffer.put((byte) 'C');
				buffer.put((byte) 4);
			} else if (this == TOP_LEFT_DIAG) {
				buffer.put((byte) 'D');
				buffer.put((byte) 'L');
			} else if (this == TOP_RIGHT_DIAG) {
				buffer.put((byte) 'D');
				buffer.put((byte) 'R');
			} else {

			}
		}
	}

	private List<WinStyle> styles = new ArrayList<WinStyle>();

	public WinOverlayView(Context context) {
		super(context);
		createPaint();
	}

	public WinOverlayView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		createPaint();
	}

	public WinOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		createPaint();
	}

	private void createPaint() {
		linePaint = new Paint();
		linePaint.setColor(Color.BLUE);
		linePaint.setStrokeWidth(15);
		linePaint.setMaskFilter(new BlurMaskFilter(10, Blur.SOLID));
		linePaint.setStrokeCap(Paint.Cap.ROUND);
	}

	public void clearWins() {
		this.styles.clear();
	}

	public void addWinStyle(WinStyle style) {
		this.styles.add(style);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (styles.isEmpty())
			return;

		// Drawing commands go here
		int height = getHeight();
		int width = getWidth();

		int startX;
		int startY;
		int stopX;
		int stopY;

		int buttonWidth = width / boardSize;
		int buttonHieght = height / boardSize;
		for (WinStyle style : styles) {
			switch (style) {
			case COL1: {
				startY = OFFSET;
				stopY = height - OFFSET;
				startX = stopX = buttonWidth / 2;
				break;
			}
			case COL2: {
				startY = OFFSET;
				stopY = height - OFFSET;
				startX = stopX = (int) (1.5 * buttonWidth);
				break;
			}
			case COL3: {
				startY = OFFSET;
				stopY = height - OFFSET;
				startX = stopX = (int) (2.5 * buttonWidth);
				break;
			}
			case COL4: {
				startY = OFFSET;
				stopY = height - OFFSET;
				startX = stopX = (int) (3.5 * buttonWidth);
				break;
			}
			case COL5: {
				startY = OFFSET;
				stopY = height - OFFSET;
				startX = stopX = (int) (4.5 * buttonWidth);
				break;
			}

			case ROW1: {
				startX = OFFSET;
				stopX = width - OFFSET;
				startY = stopY = buttonHieght / 2;
				break;
			}
			case ROW2: {
				startX = OFFSET;
				stopX = width - OFFSET;
				startY = stopY = (int) (1.5 * buttonHieght);
				break;
			}
			case ROW3: {
				startX = OFFSET;
				stopX = width - OFFSET;
				startY = stopY = (int) (2.5 * buttonHieght);
				break;
			}
			case ROW4: {
				startX = OFFSET;
				stopX = width - OFFSET;
				startY = stopY = (int) (3.5 * buttonHieght);
				break;
			}
			case ROW5: {
				startX = OFFSET;
				stopX = width - OFFSET;
				startY = stopY = (int) (4.5 * buttonHieght);
				break;
			}

			case TOP_LEFT_DIAG: {
				startX = OFFSET;
				startY = OFFSET;
				stopX = width - OFFSET;
				stopY = height - OFFSET;
				break;
			}

			case TOP_RIGHT_DIAG: {
				startX = width - OFFSET;
				startY = OFFSET;
				stopX = OFFSET;
				stopY = height - OFFSET;
				break;
			}

			default:
				throw new RuntimeException("Invalid winStyle");
			}
			canvas.drawLine(startX, startY, stopX, stopY, linePaint);
		}

	}

	public void setBoardSize(int size) {
		this.boardSize = size;
	}
}
