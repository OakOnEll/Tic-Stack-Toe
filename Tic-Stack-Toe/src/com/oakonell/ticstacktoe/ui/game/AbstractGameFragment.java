package com.oakonell.ticstacktoe.ui.game;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.oakonell.ticstacktoe.R;

public abstract class AbstractGameFragment extends SherlockFragment {

	private static class StatusText {
		private boolean isVisible;
		private View thinking;

		private boolean progressIsVisible;
		private ProgressBar thinking_progress;

		private String thinkingString;
		private TextView thinkingText;
	}

	private StatusText statusText = new StatusText();

	protected void initThinkingText(View view) {
		statusText.thinkingText = (TextView) view
				.findViewById(R.id.thinking_text);
		statusText.thinking = view.findViewById(R.id.thinking);
		statusText.thinking_progress = (ProgressBar) view.findViewById(R.id.thinking_progress);

		if (statusText.thinkingString != null) {
			if (statusText.isVisible) {
				showStatusText(statusText.thinkingString,
						statusText.progressIsVisible);
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
		showStatusText(string, true);
	}

	public void showStatusText(String string, boolean progress) {
		setStatusText(string);
		statusText.isVisible = true;
		statusText.progressIsVisible = progress;
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinking.setVisibility(View.VISIBLE);
		if (progress) {
			statusText.thinking_progress.setVisibility(View.VISIBLE);
		} else {
			statusText.thinking_progress.setVisibility(View.GONE);
		}
		statusText.thinkingText.setVisibility(View.VISIBLE);
	}

	private void setStatusText(String string) {
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
