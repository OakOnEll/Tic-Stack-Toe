package com.oakonell.ticstacktoe.ui.game;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;

public abstract class AbstractGameFragment extends SherlockFragment {

	private static class StatusText {
		private View thinking;
		private TextView thinkingText;
		private String thinkingString;
		public String opponentName;

	}

	private StatusText statusText = new StatusText();

	protected void initThinkingText(View view, String opponentName) {
		statusText = new StatusText();
		statusText.thinkingText = (TextView) view
				.findViewById(R.id.thinking_text);
		statusText.thinking = view.findViewById(R.id.thinking);
		statusText.opponentName = opponentName;

		if (statusText.thinkingString != null) {
			statusText.thinkingText.setText(statusText.thinkingString);
		}
	}

	public void hideStatusText() {
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinking.setVisibility(View.GONE);
	}

	public void showStatusText() {
		if (statusText.thinking == null) {
			return;
		}
		statusText.thinking.setVisibility(View.VISIBLE);
		statusText.thinkingText.setVisibility(View.VISIBLE);
	}

	public MainActivity getMainActivity() {
		return (MainActivity) super.getActivity();
	}

	public void setThinkingText(String text, boolean visible) {
		setThinkingText(text);
		if (visible) {
			showStatusText();
		} else {
			hideStatusText();
		}
	}

	public void setThinkingText(String text) {
		Log.i("AbstractGameFragment", "set text " + text);
		statusText.thinkingString = text;
		if (statusText.thinkingText == null) {
			return;
		}
		statusText.thinkingText.setText(text);
	}

	public void setOpponentThinking() {
		if (statusText.thinkingString != null) {
			return;
		}
		if (statusText.thinking == null) {
			return;
		}
		setThinkingText(getResources().getString(R.string.opponent_is_thinking,
				statusText.opponentName));
	}

	public void resetThinkingText() {
		Log.i("AbstractGameFragment", "reset text ");
		if (statusText.thinkingText == null) {
			return;
		}
		statusText.thinkingText.setText(statusText.thinkingString);
	}

	public void leaveGame() {
		// TODO show game stats on finish of game sequence
		// onGameStatsClose = new Runnable() {
		// @Override
		// public void run() {
		// getMainActivity().getSupportFragmentManager().popBackStack();
		// getMainActivity().gameEnded();
		// }
		// };
		// showGameStats();

		getActivity().getSupportFragmentManager().popBackStack();
		getMainActivity().gameEnded();
	}

	private Runnable onGameStatsClose;

	public void gameStatsClosed() {
		if (onGameStatsClose != null) {
			onGameStatsClose.run();
		}
	}

	protected abstract void showGameStats();
}
