package com.oakonell.ticstacktoe.ui.game;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;
import com.oakonell.utils.StringUtils;

public abstract class AbstractGameFragment extends SherlockFragment {
	private List<ChatMessage> messages = new ArrayList<ChatMessage>();
	private int numNewMessages;

	private ChatDialogFragment chatDialog;
	private MenuItem chatMenuItem;

	// private OnlinePlayAgainFragment onlinePlayAgainDialog;

	private static class StatusText {
		private View thinking;
		private TextView thinkingText;
		private String thinkingString;
		public String opponentName;

	}

	private StatusText statusText = new StatusText();

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.game, menu);
		chatMenuItem = menu.findItem(R.id.action_chat);
		handleMenu();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		chatMenuItem = menu.findItem(R.id.action_chat);
		handleMenu();
	}

	protected void invalidateMenu() {
		if (!ActivityCompat.invalidateOptionsMenu(getActivity())) {
			handleMenu();
		} else {
			honeyCombInvalidateMenu();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void honeyCombInvalidateMenu() {
		getActivity().invalidateOptionsMenu();
	}

	private void handleMenu() {
		if (chatMenuItem == null)
			return;
		boolean supportsChat = getMainActivity().getRoomListener()
				.getChatHelper() != null;
		chatMenuItem.setVisible(supportsChat);
		if (!supportsChat) {
			return;
		}
		RelativeLayout actionView = (RelativeLayout) chatMenuItem
				.getActionView();
		actionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openChatDialog();
			}
		});
		TextView chatMenuItemTextView = (TextView) actionView
				.findViewById(R.id.actionbar_notifcation_textview);
		ImageView chatMenuItemImageView = (ImageView) actionView
				.findViewById(R.id.actionbar_notifcation_imageview);
		View progressView = actionView
				.findViewById(R.id.actionbar_notifcation_progress);

		chatMenuItemImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openChatDialog();
			}
		});
		progressView.setVisibility(opponentInChat ? View.VISIBLE
				: View.INVISIBLE);
		if (numNewMessages > 0) {
			chatMenuItemTextView.setText("" + numNewMessages);
			chatMenuItemImageView
					.setImageResource(R.drawable.message_available_icon_1332);

			StringUtils.applyFlashEnlargeAnimation(chatMenuItemTextView);
		} else {
			chatMenuItemImageView
					.setImageResource(R.drawable.message_icon_27709);
			chatMenuItemTextView.setText("");
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_chat:
			openChatDialog();
			break;

		case R.id.action_settings:
			getMainActivity().getRoomListener().showSettings(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void showFullSettingsPreference() {
		// create special intent
		Intent prefIntent = new Intent(getActivity(), SettingsActivity.class);

		GameHelper helper = getMainActivity().getGameHelper();
		Info info = null;
		TicStackToe app = (TicStackToe) getActivity().getApplication();
		if (helper.isSignedIn()) {
			info = new Info(helper);
		}
		app.setDevelopInfo(info);
		// ugh.. does going to preferences leave the room!?
		getActivity()
				.startActivityForResult(prefIntent, MainActivity.RC_UNUSED);
	}

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
		if (statusText.thinking != null) {
			statusText.thinking.setVisibility(View.GONE);
		}

	}

	public void showStatusText() {
		if (statusText.thinking == null)
			return;
		statusText.thinking.setVisibility(View.VISIBLE);
		statusText.thinkingText.setVisibility(View.VISIBLE);
	}

	private void openChatDialog() {
		getMainActivity().getRoomListener().getChatHelper().sendInChat(true);
		chatDialog = new ChatDialogFragment();
		chatDialog.initialize(this, messages, getMainActivity()
				.getRoomListener().getChatHelper().getMeForChat(),
				getMainActivity().getRoomListener().getChatHelper()
						.getOpponentName());
		chatDialog.show(getChildFragmentManager(), "chat");
	}

	protected void promptToPlayAgain(String winner, String title) {
		getMainActivity().getRoomListener().promptToPlayAgain(winner, title);
	}

	public MainActivity getMainActivity() {
		return (MainActivity) super.getActivity();
	}

	public void messageRecieved(Participant opponentParticipant, String string) {
		messages.add(new ChatMessage(opponentParticipant, string, false, System
				.currentTimeMillis()));
		getMainActivity().playSound(Sounds.CHAT_RECIEVED);
		if (chatDialog != null) {
			chatDialog.newMessage();
		} else {
			numNewMessages++;
			invalidateMenu();
		}
	}

	public void chatClosed() {
		getMainActivity().getRoomListener().getChatHelper().sendInChat(false);
		chatDialog = null;
		numNewMessages = 0;
		invalidateMenu();
	}

	private boolean opponentInChat = false;

	public void opponentInChat() {
		opponentInChat = true;
		// show "animated" menu icon
		invalidateMenu();

		// update the display text
		statusText.thinkingText.setText(getResources().getString(
				R.string.opponent_is_in_chat,
				getMainActivity().getRoomListener().getChatHelper()
						.getOpponentName()));
	}

	public void opponentClosedChat() {
		// stop animated menu icon
		opponentInChat = false;
		invalidateMenu();

		// update the display text
		resetThinkingText();
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

		getMainActivity().getSupportFragmentManager().popBackStack();
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
