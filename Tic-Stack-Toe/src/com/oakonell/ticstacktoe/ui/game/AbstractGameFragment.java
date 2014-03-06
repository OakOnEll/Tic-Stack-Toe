package com.oakonell.ticstacktoe.ui.game;

import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.oakonell.ticstacktoe.R;

public abstract class AbstractGameFragment extends SherlockFragment {

	private static class StatusText {
		private View thinking;
		private TextView thinkingText;
		private String thinkingString;
		private boolean isVisible;
	}

	private StatusText statusText = new StatusText();

	protected void initThinkingText(View view) {
		statusText.thinkingText = (TextView) view
				.findViewById(R.id.thinking_text);
		statusText.thinking = view.findViewById(R.id.thinking);

		if (statusText.thinkingString != null) {
			if (statusText.isVisible) {
				showStatusText(statusText.thinkingString);
			} else {
				statusText.thinkingText.setText(statusText.thinkingString);
			}
		}
	}

	public void hideStatusText() {
		statusText.isVisible = false;
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinking.setVisibility(View.GONE);
	}

	public void showStatusText(String string) {
		setStatusText(string);
		statusText.isVisible = true;
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinking.setVisibility(View.VISIBLE);
		statusText.thinkingText.setVisibility(View.VISIBLE);
	}

	public void setStatusText(String string) {
		statusText.thinkingString = string;
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinkingText.setText(string);
	}

	// public void setThinkingText(String text, boolean visible) {
	// setThinkingText(text);
	// if (visible) {
	// showStatusText();
	// } else {
	// hideStatusText();
	// }
	// }
	//
	// public void setThinkingText(String text) {
	// Log.i("AbstractGameFragment", "set text " + text);
	// statusText.thinkingString = text;
	// if (statusText.thinkingText == null) {
	// return;
	// }
	// statusText.thinkingText.setText(text);
	// }
	//
	// public void setOpponentThinking() {
	// if (statusText.thinkingString != null) {
	// return;
	// }
	// if (statusText.thinking == null) {
	// return;
	// }
	// setThinkingText(getResources().getString(R.string.opponent_is_thinking,
	// statusText.opponentName));
	// }
	//
	// public void resetThinkingText() {
	// Log.i("AbstractGameFragment", "reset text ");
	// if (statusText.thinkingText == null) {
	// return;
	// }
	// statusText.thinkingText.setText(statusText.thinkingString);
	// }
	//

	private Runnable onGameStatsClose;

	public void gameStatsClosed() {
		if (onGameStatsClose != null) {
			onGameStatsClose.run();
		}
	}

	protected abstract void showGameStats();
}
