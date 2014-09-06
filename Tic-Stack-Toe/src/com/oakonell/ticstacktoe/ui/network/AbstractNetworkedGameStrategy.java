package com.oakonell.ticstacktoe.ui.network;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.Achievements;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.utils.StringUtils;

public abstract class AbstractNetworkedGameStrategy extends GameStrategy {
	private RankInfo rankInfo;

	protected AbstractNetworkedGameStrategy(GameContext context) {
		super(context);
	}

	public RankInfo getRankInfo() {
		return rankInfo;
	}

	protected void setRankInfo(RankInfo rankInfo) {
		this.rankInfo = rankInfo;
	}

	public abstract boolean iAmBlackPlayer();

	@Override
	public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_chat:
			openChatDialog();
			return true;
		}
		return super.onOptionsItemSelected(fragment, item);
	}

	private List<ChatMessage> messages = new ArrayList<ChatMessage>();
	private int numNewMessages;

	private ChatDialogFragment chatDialog;
	private MenuItem chatMenuItem;

	private void openChatDialog() {
		sendInChat(true);
		chatDialog = new ChatDialogFragment();
		chatDialog.initialize(getContext(), this, messages, getMeForChat(),
				getOpponentName());
		chatDialog.show(getGameFragment().getChildFragmentManager(), "chat");
	}

	@Override
	public void onCreateOptionsMenu(Fragment fragment, Menu menu,
			MenuInflater inflater) {
		inflateMenu(menu, inflater);
		chatMenuItem = menu.findItem(R.id.action_chat);
		handleMenu();
	}

	protected void inflateMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.network_game, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Fragment fragment, Menu menu) {
		chatMenuItem = menu.findItem(R.id.action_chat);
		handleMenu();
	}

	protected void handleMenu() {
		if (chatMenuItem == null)
			return;

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
		// chatMenuItemImageView
		// .setBackgroundResource(R.drawable.action_bar_inset);
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

	public List<ChatMessage> getChatMessages() {
		return messages;
	}

	public int getNumNewMessages() {
		return numNewMessages;
	}

	public void setNumNewMessages(int i) {
		this.numNewMessages = i;
	}

	public void messageRecieved(Participant opponentParticipant, String string) {
		messages.add(new ChatMessage(opponentParticipant, string, false, System
				.currentTimeMillis()));
		playSound(Sounds.CHAT_RECEIVED);
		if (chatDialog != null) {
			chatDialog.newMessage();
		} else {
			numNewMessages++;
			invalidateMenu();
		}
	}

	public void chatClosed() {
		sendInChat(false);
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
		getGameFragment().showStatusText(
				getContext().getResources().getString(
						R.string.opponent_is_in_chat, getOpponentName()));
	}

	public void opponentClosedChat() {
		// stop animated menu icon
		opponentInChat = false;
		invalidateMenu();

		// update the display text
		// getGameFragment().resetThinkingText();
	}

	abstract public void sendInChat(boolean b);

	abstract public Participant getMeForChat();

	abstract public void sendMessage(String string);

	abstract public String getOpponentName();

	@Override
	protected void acceptNonHumanPlayerMove(PlayerStrategy currentStrategy) {
		// wait for the networked player's move
		getGameFragment().showStatusText(
				getActivity().getString(R.string.opponent_thinking,
						getOpponentName()));
	}

	public void evaluateGameEndAchievements(Achievements achievements,
			GameContext gameContext, Game game, State outcome) {
		achievements.testAndSetForBeatAFriendAchievements(gameContext, game,
				outcome);
	}

}
