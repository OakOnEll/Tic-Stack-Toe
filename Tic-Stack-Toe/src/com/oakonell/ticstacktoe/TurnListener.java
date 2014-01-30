package com.oakonell.ticstacktoe;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayerListener;
import com.oakonell.ticstacktoe.RoomListener.PlayAgainState;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Game.ByteBufferDebugger;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.OnlineStrategy;

public class TurnListener implements TurnBasedMultiplayerListener, GameListener {
	private static final int TOAST_DELAY = Toast.LENGTH_SHORT;
	private static final String TAG = "TurnListener";

	private final MainActivity activity;
	private final GameHelper helper;
	private GameType type;
	private final boolean isQuick;
	private final boolean initiatedTheGame;

	private TurnBasedMatch mMatch;
	private String mMyParticipantId;
	private String blackParticipantId;
	private boolean isVisible;

	public TurnListener(MainActivity activity, GameHelper helper,
			GameType type, boolean isQuick, boolean initiatedTheGame) {
		this.activity = activity;
		this.helper = helper;
		this.type = type;
		this.isQuick = isQuick;
		this.initiatedTheGame = initiatedTheGame;
		helper.getGamesClient().registerMatchUpdateListener(this);
	}

	public TurnListener(MainActivity mainActivity, GameHelper gameHelper,
			TurnBasedMatch match) {
		// TODO Auto-generated constructor stub
		activity = mainActivity;
		helper = activity.getGameHelper();
		helper.getGamesClient().registerMatchUpdateListener(this);

		mMatch = match;

		mMyParticipantId = match.getParticipantId(helper.getGamesClient()
				.getCurrentPlayerId());

		// GameState state = GameState.fromMatch(match);
		// // need to do this now?
		// type = state.game.getType();
		// TOOD ??
		initiatedTheGame = false;
		isQuick = false;

	}

	@Override
	public void onTurnBasedMatchCanceled(int statusCode, String matchId) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(null, statusCode)) {
			return;
		}

		showWarning("Match",
				"This match is canceled.  All other players will have their game ended.");

		activity.getMenuFragment().setActive();
	}

	@Override
	public void onTurnBasedMatchLeft(int statusCode, TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}
		showWarning("Left", "You've left this match.");
		activity.getMenuFragment().setActive();
	}

	@Override
	public void onTurnBasedMatchInitiated(int statusCode, TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}

		if (match.getData() != null) {
			// This is a game that has already started, so I'll just start
			updateMatch(match);
			return;
		}

		startMatch(match);
	}

	private void startMatch(TurnBasedMatch match) {
		mMatch = match;
		mMyParticipantId = match.getParticipantId(helper.getGamesClient()
				.getCurrentPlayerId());

		boolean iAmBlack = true;

		ScoreCard score = new ScoreCard(0, 0, 0);
		Player blackPlayer;
		Player whitePlayer;
		String localPlayerName = activity.getString(R.string.local_player_name);
		if (iAmBlack) {
			blackParticipantId = mMyParticipantId;
			blackPlayer = HumanStrategy.createPlayer(localPlayerName, true,
					getMe().getIconImageUri(), blackParticipantId);
			whitePlayer = OnlineStrategy.createPlayer(getOpponentName(), false,
					getOpponentParticipant().getIconImageUri(),
					getOpponentParticipant().getParticipantId());
		} else {
			blackParticipantId = getOpponentParticipant().getParticipantId();
			whitePlayer = HumanStrategy.createPlayer(localPlayerName, false,
					getMe().getIconImageUri(), getMe().getParticipantId());
			blackPlayer = OnlineStrategy.createPlayer(getOpponentName(), true,
					getOpponentParticipant().getIconImageUri(),
					blackParticipantId);
		}

		Game game = new Game(type, GameMode.TURN_BASED, blackPlayer,
				whitePlayer, blackPlayer);

		// write the game data
		GameState gameState = new GameState(game, score, blackParticipantId);

		byte[] bytes = gameState.toBytes(helper);

		// Taking this turn will cause turnBasedMatchUpdated
		helper.getGamesClient().takeTurn(this, match.getMatchId(), bytes,
				mMyParticipantId);
	}

	@Override
	public void onTurnBasedMatchUpdated(int statusCode, TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}

		updateMatch(match);

		if (match.canRematch()) {
			askForRematch();
			return;
		}

	}

	// Handle notification events.
	@Override
	public void onInvitationReceived(Invitation invitation) {
		Toast.makeText(
				activity,
				"An invitation has arrived from "
						+ invitation.getInviter().getDisplayName(), TOAST_DELAY)
				.show();
	}

	@Override
	public void onInvitationRemoved(String invitationId) {
		Toast.makeText(activity, "An invitation was removed.", TOAST_DELAY)
				.show();
	}

	@Override
	public void onTurnBasedMatchesLoaded(int statusCode,
			LoadMatchesResponse response) {
		// Not used.
	}

	@Override
	public void onTurnBasedMatchReceived(TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (match.getMatchId().equals(mMatch.getMatchId())) {
			updateMatch(match);
			return;
		}
		Toast.makeText(activity, "A match was updated.", TOAST_DELAY).show();
	}

	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		Toast.makeText(activity, "A match was removed.", TOAST_DELAY).show();

	}

	// Returns false if something went wrong, probably. This should handle
	// more cases, and probably report more accurate results.
	private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
		switch (statusCode) {
		case GamesClient.STATUS_OK:
			return true;
		case GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
			// This is OK; the action is stored by Google Play Services and will
			// be dealt with later.
			Toast.makeText(
					activity,
					"Stored action for later.  (Please remove this toast before release.)",
					TOAST_DELAY).show();
			// NOTE: This toast is for informative reasons only; please remove
			// it from your final application.
			return true;
		case GamesClient.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
			showErrorMessage(match, statusCode,
					R.string.status_multiplayer_error_not_trusted_tester);
			break;
		case GamesClient.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
			showErrorMessage(match, statusCode,
					R.string.match_error_already_rematched);
			break;
		case GamesClient.STATUS_NETWORK_ERROR_OPERATION_FAILED:
			showErrorMessage(match, statusCode,
					R.string.network_error_operation_failed);
			break;
		case GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED:
			showErrorMessage(match, statusCode,
					R.string.client_reconnect_required);
			break;
		case GamesClient.STATUS_INTERNAL_ERROR:
			showErrorMessage(match, statusCode, R.string.internal_error);
			break;
		case GamesClient.STATUS_MATCH_ERROR_INACTIVE_MATCH:
			showErrorMessage(match, statusCode,
					R.string.match_error_inactive_match);
			break;
		case GamesClient.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
			showErrorMessage(match, statusCode,
					R.string.match_error_locally_modified);
			break;
		default:
			showErrorMessage(match, statusCode, R.string.unexpected_status);
			Log.d(TAG, "Did not have warning or string to deal with: "
					+ statusCode);
		}

		return false;
	}

	public void showErrorMessage(TurnBasedMatch match, int statusCode,
			int stringId) {
		showWarning("Warning", activity.getResources().getString(stringId));
	}

	private void showWarning(String title, String message) {
		helper.showAlert(title, message);
	}

	@Override
	public void leaveRoom() {
		isVisible = false;
		// TODO Auto-generated method stub
		// store the game
		GameState gameState = new GameState(activity.getGameFragment()
				.getGame(), activity.getGameFragment().getScore(),
				blackParticipantId);
		byte[] bytes = gameState.toBytes(helper);
		helper.getGamesClient().takeTurn(this, mMatch.getMatchId(), bytes,
				getMe().getParticipantId());
		helper.getGamesClient().registerMatchUpdateListener(null);
	}

	@Override
	public void sendMove(Game game, AbstractMove lastMove, ScoreCard score) {
		// store the game, take turn
		GameState gameState = new GameState(game, score, blackParticipantId);
		byte[] bytes = gameState.toBytes(helper);
		if (game.getBoard().getState().isOver()) {
			helper.getGamesClient().finishTurnBasedMatch(this,
					mMatch.getMatchId(), bytes);
		} else {
			helper.getGamesClient().takeTurn(this, mMatch.getMatchId(), bytes,
					getOpponentParticipant().getParticipantId());
		}
	}

	@Override
	public void sendNotPlayAgain(Runnable success, Runnable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public PlayAgainState getOpponentPlayAgainState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendPlayAgain(Runnable success, Runnable error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendMessage(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendInChat(boolean b) {
		// TODO Auto-generated method stub
		// if my turn, update the game... otherwise, can't message?
	}

	@Override
	public Participant getMe() {
		ArrayList<Participant> mParticipants = mMatch.getParticipants();

		if (!mParticipants.get(0).getParticipantId().equals(mMyParticipantId)) {
			return mParticipants.get(1);
		}
		return mParticipants.get(0);
	}

	private Participant getOpponentParticipant() {
		ArrayList<Participant> mParticipants = mMatch.getParticipants();

		if (!mParticipants.get(0).getParticipantId().equals(mMyParticipantId)) {
			return mParticipants.get(0);
		}
		return mParticipants.get(1);
	}

	@Override
	public String getOpponentName() {
		return getOpponentParticipant().getDisplayName();
	}

	@Override
	public void restartGame() {
		// TODO Auto-generated method stub

	}

	@Override
	public void backFromWaitingRoom() {
		// start the match ?!
		// do nothing for turn based matches?
	}

	public void askForRematch() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				activity);

		alertDialogBuilder.setMessage("Do you want a rematch?");

		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("Sure, rematch!",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								rematch();
							}
						})
				.setNegativeButton("No.",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
							}
						});

		alertDialogBuilder.show();
	}

	// If you choose to rematch, then call it and wait for a response.
	public void rematch() {
		helper.getGamesClient()
				.rematchTurnBasedMatch(this, mMatch.getMatchId());
		mMatch = null;
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		mMatch = match;
		mMyParticipantId = match.getParticipantId(helper.getGamesClient()
				.getCurrentPlayerId());
		showGame();
	}

	private static class GameState {
		private final Game game;
		private final ScoreCard score;
		private final String blackPlayerId;

		public GameState(Game game, ScoreCard score, String blackPlayerId) {
			this.game = game;
			this.score = score;
			this.blackPlayerId = blackPlayerId;
		}

		public byte[] toBytes(GameHelper helper) {
			ByteBuffer theBuffer = ByteBuffer.allocate(helper.getGamesClient()
					.getMaxTurnBasedMatchDataSize());
			ByteBufferDebugger buffer = new ByteBufferDebugger(theBuffer);

			byte[] bytes;
			try {
				bytes = blackPlayerId.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unsupported UTF-8!");
			}
			theBuffer.putInt(bytes.length);
			theBuffer.put(bytes);

			theBuffer.putInt(score.getBlackWins());
			theBuffer.putInt(score.getWhiteWins());
			theBuffer.putInt(score.getTotalGames());

			game.writeBytes(blackPlayerId, buffer);

			return theBuffer.array();
		}

	}

	public GameState fromMatch(TurnBasedMatch match, boolean myTurn) {
		byte[] data = match.getData();
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int len = buffer.getInt();
		byte[] blackplayerIdBytes = new byte[len];
		buffer.get(blackplayerIdBytes);
		String blackPlayerIdString;
		try {
			blackPlayerIdString = new String(blackplayerIdBytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported UTF-8!");
		}

		int blackWins = buffer.getInt();
		int whiteWins = buffer.getInt();
		int totalGames = buffer.getInt();
		ScoreCard score = new ScoreCard(blackWins, whiteWins, totalGames
				- (blackWins + whiteWins));

		Player blackPlayer;
		Player whitePlayer;
		String localPlayerName = activity.getString(R.string.local_player_name);
		Player currentPlayer;
		if (blackPlayerIdString.equals(getMe().getParticipantId())) {
			blackPlayer = HumanStrategy.createPlayer(localPlayerName, true,
					getMe().getIconImageUri(), getMe().getParticipantId());
			whitePlayer = OnlineStrategy.createPlayer(getOpponentName(), false,
					getOpponentParticipant().getIconImageUri(),
					getOpponentParticipant().getParticipantId());
			if (myTurn) {
				currentPlayer = blackPlayer;
			} else {
				currentPlayer = whitePlayer;
			}
		} else {
			blackParticipantId = getOpponentParticipant().getParticipantId();
			whitePlayer = HumanStrategy.createPlayer(localPlayerName, false,
					getMe().getIconImageUri(), getMe().getParticipantId());
			blackPlayer = OnlineStrategy.createPlayer(getOpponentName(), true,
					getOpponentParticipant().getIconImageUri(),
					blackParticipantId);
			if (!myTurn) {
				currentPlayer = blackPlayer;
			} else {
				currentPlayer = whitePlayer;
			}
		}

		Game game = Game.fromBytes(blackPlayer, whitePlayer, currentPlayer,
				new ByteBufferDebugger(buffer));

		GameState gameState = new GameState(game, score, blackPlayerIdString);

		return gameState;
	}

	public void showGame() {
		int status = mMatch.getStatus();
		int turnStatus = mMatch.getTurnStatus();

		switch (status) {
		case TurnBasedMatch.MATCH_STATUS_CANCELED:
			showWarning("Canceled!", "This game was canceled!");
			return;
		case TurnBasedMatch.MATCH_STATUS_EXPIRED:
			showWarning("Expired!", "This game is expired.  So sad!");
			return;
		case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
			showWarning("Waiting for auto-match...",
					"We're still waiting for an automatch partner.");
			return;
		case TurnBasedMatch.MATCH_STATUS_COMPLETE:
			// handled down below
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
		case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:

			// setGameplayUI();
			break;
		case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
			break;
		case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
			showWarning("Good inititative!",
					"Still waiting for invitations.\n\nBe patient!");
			break;
		}

		GameFragment gameFragment;
		if (!isVisible) {
			gameFragment = new GameFragment();
			FragmentManager manager = activity.getSupportFragmentManager();
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(R.id.main_frame, gameFragment,
					MainActivity.FRAG_TAG_GAME);
			transaction.addToBackStack(null);
			transaction.commit();
		} else {
			gameFragment = activity.getGameFragment();
		}
		GameState state = fromMatch(mMatch,
				turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
		type = state.game.getType();
		blackParticipantId = state.blackPlayerId;

		// this needs to reset all the widgets on the UI
		// TODO Undo the last move, so it can be reapplied and animated?
		
		gameFragment.startGame(state.game, state.score);

		// show if someone won
		State state2 = state.game.getBoard().getState();
		if (state2.isOver()) {
			// show win and complete the game
			if (status != TurnBasedMatch.MATCH_STATUS_COMPLETE) {
				throw new RuntimeException("Match should be complete");
			}
			if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
				helper.getGamesClient().finishTurnBasedMatch(
						new OnTurnBasedMatchUpdatedListener() {
							@Override
							public void onTurnBasedMatchUpdated(int status,
									TurnBasedMatch match) {
								if (status != GamesClient.STATUS_OK
										&& status != GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
									showWarning("Error completing match",
											"Finish match returned " + status);
									return;
								}
								String rematchId=match.getRematchId();
								if (rematchId==null) {
									showWarning("no rematch", "no rematch");
								} else {
									showWarning("no rematch", "no rematch");
								}
							}
						}, mMatch.getMatchId());
			}
		}
		isVisible = true;
	}

	@Override
	public boolean warnToLeave() {
		return false;
	}
}
