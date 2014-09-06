package com.oakonell.ticstacktoe.ui.network.turn;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer.InitiateMatchResult;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.network.turn.TurnBasedMatchGameStrategy.GameState;

public class TurnBasedMatchInfo implements MatchInfo {

	private String text;
	private CharSequence subtext;
	private String updatedtext;
	private Uri opponentPicUri;
	private String matchId;
	private boolean canRematch;
	private long lastUpdated;
	private int status;

	private GameHelper helper;
	Context context;

	public TurnBasedMatchInfo(Context context, GameHelper helper,
			TurnBasedMatch match) {
		this.helper = helper;
		lastUpdated = match.getLastUpdatedTimestamp();
		matchId = match.getMatchId();
		status = match.getStatus();
		this.context = context;

		String opponentName = context.getString(R.string.anonymous);
		String currentPlayerId = Games.Players.getCurrentPlayerId(helper
				.getApiClient());
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
				try {
					GameState state = GameState.fromMatch(context, helper,
							match);
					State state2 = state.game.getBoard().getState();
					Player winner = state2.getWinner();
					if (winner.getStrategy().isHuman()) {
						text = context.getString(R.string.you_beat_opponent,
								opponentName);
					} else {
						text = context.getString(R.string.opponent_won,
								winner.getName());
					}
				} catch (Exception e) {
					text = "Error reading match";
				}
			} else {
				text = opponentName;
			}
		} else if (match.getStatus() == TurnBasedMatch.MATCH_STATUS_EXPIRED) {
			canRematch = false;
			text = context.getString(R.string.expired_game, opponentName);
		} else {
			canRematch = false;
			text = opponentName;
		}
		int variant = match.getVariant();
		boolean isRanked = MatchUtils.isRanked(variant);
		GameType type = MatchUtils.getType(variant);

		CharSequence timeSpanString = MatchUtils.getTimeSince(context,
				lastUpdated);
		updatedtext = context.getString(R.string.played_ago, timeSpanString);
		subtext = GameTypeSpinnerHelper.getTypeName(context, type);
		if (isRanked) {
			subtext = context.getString(R.string.ranked_game_subtext, subtext);
		}

	}

	public Uri getIconImageUri() {
		return opponentPicUri;
	}

	public String getMatchId() {
		return matchId;
	}

	public List<MatchMenuItem> getMenuItems(Context context) {
		List<MatchMenuItem> result = new ArrayList<MatchMenuItem>();

		if (canRematch) {
			MatchMenuItem rematch = new MatchMenuItem(
					context.getString(R.string.rematch), new ItemExecute() {
						@Override
						public void execute(final MenuFragment fragment,
								List<MatchInfo> matches) {
							fragment.setInactive();
							Games.TurnBasedMultiplayer
									.rematch(helper.getApiClient(), matchId)
									.setResultCallback(
											new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {

												@Override
												public void onResult(
														InitiateMatchResult result) {
													int status = result
															.getStatus()
															.getStatusCode();
													if (status != GamesClient.STATUS_OK) {
														showAlert("Error starting rematch");
														fragment.refreshMatches();
														fragment.setActive();
														return;
													}
													fragment.showMatch(result
															.getMatch()
															.getMatchId());
												}
											});
						}
					});
			result.add(rematch);
		}

		MatchMenuItem dismiss = new MatchMenuItem(
				context.getString(R.string.dismiss), new ItemExecute() {
					@Override
					public void execute(MenuFragment fragment,
							List<MatchInfo> matches) {
						dismiss(fragment, matches);
					}
				});
		result.add(dismiss);
		MatchInfo.MatchUtils.addDismissThisAndOlder(result, this);

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
	public CharSequence getUpdatedText(Context context) {
		return updatedtext;
	}

	@Override
	public CharSequence getText(Context context) {
		return text;
	}

	@Override
	public long getUpdatedTimestamp() {
		return lastUpdated;
	}

	public void showAlert(String message) {
		(new AlertDialog.Builder(context)).setMessage(message)
				.setNeutralButton(android.R.string.ok, null).create().show();
	}

	@Override
	public int getMatchStatus() {
		return status;
	}

	@Override
	public void dismiss(MenuFragment fragment, List<MatchInfo> matches) {
		Games.TurnBasedMultiplayer.dismissMatch(helper.getApiClient(), matchId);
		matches.remove(this);
	}

}
