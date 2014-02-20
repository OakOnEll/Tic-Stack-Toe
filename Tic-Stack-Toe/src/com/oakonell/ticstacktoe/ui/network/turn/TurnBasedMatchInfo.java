package com.oakonell.ticstacktoe.ui.network.turn;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchInitiatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchGameStrategy.GameState;

public class TurnBasedMatchInfo implements MatchInfo {

	private String text;
	private String subtext;
	private Uri opponentPicUri;
	private String matchId;
	private boolean canRematch;
	private long lastUpdated;

	public TurnBasedMatchInfo(Context context, GamesClient client,
			TurnBasedMatch match) {
		lastUpdated = match.getLastUpdatedTimestamp();
		matchId = match.getMatchId();

		// TODO store a snapshot of the board state

		String opponentName = "Anonymous";
		String currentPlayerId = client.getCurrentPlayerId();
		for (Participant participant : match.getParticipants()) {
			if (participant.getPlayer() != null
					&& !participant.getPlayer().getPlayerId()
							.equals(currentPlayerId)) {
				opponentName = participant.getDisplayName();
				opponentPicUri = participant.getIconImageUri();
				break;
			}
		}

		if (match.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
			canRematch = match.canRematch();
			// TODO offload this from the main thread
			if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
				GameState state = GameState.fromMatch(context, client, match);
				State state2 = state.game.getBoard().getState();
				Player winner = state2.getWinner();
				text = winner.getName() + " won!";
			}
		} else {
			canRematch = false;
			text = opponentName;
		}

		CharSequence timeSpanString = DateUtils.getRelativeDateTimeString(
				context, lastUpdated, DateUtils.MINUTE_IN_MILLIS,
				DateUtils.WEEK_IN_MILLIS, 0);
		subtext = "Last Played " + timeSpanString;

	}

	public Uri getIconImageUri() {
		return opponentPicUri;
	}

	public String getMatchId() {
		return matchId;
	}

	public List<MatchMenuItem> getMenuItems() {
		List<MatchMenuItem> result = new ArrayList<MatchMenuItem>();
		MatchMenuItem dismiss = new MatchMenuItem("Dismiss", new ItemExecute() {
			@Override
			public void execute(MenuFragment fragment, List<MatchInfo> matches) {
				GamesClient gamesClient = fragment.getMainActivity()
						.getGamesClient();
				gamesClient.dismissTurnBasedMatch(matchId);
				matches.remove(TurnBasedMatchInfo.this);
			}
		});
		result.add(dismiss);

		if (canRematch) {
			MatchMenuItem rematch = new MatchMenuItem("Rematch",
					new ItemExecute() {
						@Override
						public void execute(final MenuFragment fragment,
								List<MatchInfo> matches) {
							fragment.setInactive();
							GamesClient gamesClient = fragment
									.getMainActivity().getGamesClient();
							gamesClient.rematchTurnBasedMatch(
									new OnTurnBasedMatchInitiatedListener() {
										@Override
										public void onTurnBasedMatchInitiated(
												int status, TurnBasedMatch match) {
											if (status != GamesClient.STATUS_OK) {
												fragment.getMainActivity()
														.getGameHelper()
														.showAlert(
																"Error starting rematch");
												fragment.refreshMatches();
												fragment.setActive();
												return;
											}
											fragment.showMatch(match
													.getMatchId());
										}
									}, matchId);
						}
					});
			result.add(rematch);
		}

		return result;
	}

	@Override
	public void onClick(MenuFragment fragment) {
		fragment.showMatch(getMatchId());
	}

	@Override
	public CharSequence getSubtext(Context context) {
		return subtext;
	}

	@Override
	public CharSequence getText(Context context) {
		return text;
	}

	@Override
	public long getUpdatedTimestamp() {
		return lastUpdated;
	}

}
