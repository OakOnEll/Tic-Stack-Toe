package com.oakonell.ticstacktoe.ui.network.turn;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationBuffer;
import com.google.android.gms.games.multiplayer.OnInvitationsLoadedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchInitiatedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchLoadedListener;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdatedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayerListener;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.OnlineStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;
import com.oakonell.ticstacktoe.ui.network.AbstractNetworkedGameStrategy;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class TurnBasedMatchGameStrategy extends AbstractNetworkedGameStrategy
		implements TurnBasedMultiplayerListener {
	private static final int TOAST_DELAY = Toast.LENGTH_SHORT;
	private static final String TAG = "TurnBasedMatchGameStrategy";
	private static final int PROTOCOL_VERSION = 1;

	private TurnBasedMatch mMatch;
	private String mMyParticipantId;

	private GameType type;
	private boolean isQuick;
	private String blackParticipantId;
	// remove isVisible variable?
	private boolean isVisible;

	TurnBasedPlayAgainFragment playAgainDialog;

	public TurnBasedMatchGameStrategy(MainActivity activity,
			SoundManager soundManager, GameHelper helper, GameType type,
			boolean isQuick) {
		super(activity, soundManager, helper);
		this.type = type;
		this.isQuick = isQuick;
		helper.getGamesClient().registerMatchUpdateListener(this);
	}

	public TurnBasedMatchGameStrategy(MainActivity mainActivity,
			SoundManager soundManager, GameHelper gameHelper,
			TurnBasedMatch match) {
		super(mainActivity, soundManager, gameHelper);
		getHelper().getGamesClient().registerMatchUpdateListener(this);

		mMatch = match;

		if (match != null) {
			mMyParticipantId = match.getParticipantId(getHelper()
					.getGamesClient().getCurrentPlayerId());
		}

		// GameState state = GameState.fromMatch(match);
		// // need to do this now?
		// type = state.game.getType();
		// TOOD ??
		isQuick = false;

	}

	@Override
	public void onSignInFailed(MainActivity mainActivity) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				mainActivity);

		alertDialogBuilder.setTitle("Log In failed...");
		alertDialogBuilder
				.setMessage("Can't continue game without being logged in.");
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton("Menu", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				leaveRoom();
			}
		});

	}

	@Override
	public void onSignInSuccess(MainActivity activity) {
		Log.i(TAG, "Reassociating a fragment with the TurnListener");
		activity.getGameFragment().resetThinkingText();
		this.setMainActivity(activity);
		setHelper(activity.getGameHelper());
		getHelper().getGamesClient().registerMatchUpdateListener(this);
		// also reload the possibly updated match
		getHelper().getGamesClient().getTurnBasedMatch(
				new OnTurnBasedMatchLoadedListener() {

					@Override
					public void onTurnBasedMatchLoaded(int statusCode,
							TurnBasedMatch match) {
						if (!checkStatusCode(match, statusCode)) {
							return;
						}
						updateMatch(match);
					}
				}, mMatch.getMatchId());
	}

	@Override
	public void onTurnBasedMatchCanceled(int statusCode, String matchId) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(null, statusCode)) {
			return;
		}

		showWarning("Match",
				"This match is canceled.  All other players will have their game ended.");

		leaveRoom();
	}

	@Override
	public void onTurnBasedMatchLeft(int statusCode, TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}
		showWarning("Left", "You've left this match.");
		leaveRoom();
	}

	@Override
	public void onTurnBasedMatchInitiated(int statusCode, TurnBasedMatch match) {
		if (match == null) {
			Log.i("TurnListener",
					"onTurnBasedMatchInitiated - null match, status="
							+ statusCode + "?");
			return;
		}
		Log.i("TurnListener", "onTurnBasedMatchInitiated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}
		showOrStartMatch(match);
	}

	private void showOrStartMatch(TurnBasedMatch match) {
		mMatch = match;
		if (match.getData() != null) {
			// This is a game that has already started, so I'll just start
			updateMatch(match);
			return;
		}

		startMatch(match);
	}

	public void showFromMenu() {
		showOrStartMatch(mMatch);
	}

	private void startMatch(TurnBasedMatch match) {
		mMatch = match;
		mMyParticipantId = match.getParticipantId(getHelper().getGamesClient()
				.getCurrentPlayerId());

		byte[] previousMatchData = match.getPreviousMatchData();
		ScoreCard score;
		if (previousMatchData != null) {
			GameState state = GameState.fromBytes(getContext(), getHelper()
					.getGamesClient(), mMatch, previousMatchData, true);
			blackParticipantId = state.blackPlayerId;
			score = state.score;
			if (type == null) {
				type = state.game.getType();
			}
		} else {
			score = new ScoreCard(0, 0, 0);
			if (type == null) {
				throw new RuntimeException(
						"Can't start a new game of unknown type?!");
			}
		}

		boolean iAmBlack = blackParticipantId == null
				|| blackParticipantId.equals(mMyParticipantId);

		Player blackPlayer;
		Player whitePlayer;
		String localPlayerName = getContext().getString(
				R.string.local_player_name);
		if (iAmBlack) {
			blackParticipantId = mMyParticipantId;
			blackPlayer = HumanStrategy.createPlayer(localPlayerName, true,
					getMeForChat().getIconImageUri());
			whitePlayer = OnlineStrategy.createPlayer(getOpponentName(), false,
					getOpponentParticipant().getIconImageUri());
		} else {
			blackParticipantId = getOpponentParticipant().getParticipantId();
			whitePlayer = HumanStrategy.createPlayer(localPlayerName, false,
					getMeForChat().getIconImageUri());
			blackPlayer = OnlineStrategy.createPlayer(getOpponentName(), true,
					getOpponentParticipant().getIconImageUri());
		}

		Game game = new Game(type, GameMode.TURN_BASED, blackPlayer,
				whitePlayer, blackPlayer);

		// write the game data
		GameState gameState = new GameState(game, score, blackParticipantId,
				isQuick, false);

		byte[] bytes = gameState.toBytes(getHelper());

		// Taking this turn will cause turnBasedMatchUpdated
		getHelper().getGamesClient().takeTurn(this, match.getMatchId(), bytes,
				mMyParticipantId);
	}

	@Override
	public void onTurnBasedMatchUpdated(int statusCode, TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchUpdated");
		if (!checkStatusCode(match, statusCode)) {
			return;
		}

		updateMatch(match);

	}

	public void acceptInvite(String inviteId) {
		getMainActivity().getGameFragment().setThinkingText(
				"Accepting invite to rematch from " + getOpponentName(), true);
		getHelper().getGamesClient().acceptTurnBasedInvitation(
				TurnBasedMatchGameStrategy.this, inviteId);
	}

	// Handle notification events.
	@Override
	public void onInvitationReceived(final Invitation invitation) {
		if (invitation.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED) {
			if (invitation.getInviter().getDisplayName()
					.equals(getOpponentParticipant().getActualDisplayName())
					&& rematchId != null) {
				rematchId = null;

				openPlayAgain(getMainActivity().getGameFragment(),
						"Can I get here?");

				playAgainDialog.displayAcceptInvite(invitation
						.getInvitationId());
				return;
			}
		}
		Toast.makeText(
				getContext(),
				"An invitation has arrived from "
						+ invitation.getInviter().getDisplayName(), TOAST_DELAY)
				.show();
	}

	Runnable onResume;

	public void playAgainClosed() {
		playAgainDialog = null;
	}

	@SuppressLint("NewApi")
	private void openPlayAgain(final GameFragment fragment, String winner) {
		if (playAgainDialog != null) {
			return;
		}
		playAgainDialog = new TurnBasedPlayAgainFragment();
		playAgainDialog.initialize(this, getOpponentName(), winner);
		// TODO... this uses new api
		if (fragment.getActivity() == null) {
			// huh?
			onResume = new Runnable() {
				@Override
				public void run() {
					onResume = null;
					playAgainDialog.show(fragment.getChildFragmentManager(),
							"playAgain");
				}
			};
			return;
		}
		playAgainDialog.show(fragment.getChildFragmentManager(), "playAgain");
	}

	@Override
	public void onInvitationRemoved(String invitationId) {
		Toast.makeText(getContext(), "An invitation was removed.", TOAST_DELAY)
				.show();
	}

	@Override
	public void onTurnBasedMatchesLoaded(int statusCode,
			LoadMatchesResponse response) {
		Log.i("TurnListener", "onTurnBasedMatchesLoaded");
		// Not used.
	}

	@Override
	public void onTurnBasedMatchReceived(TurnBasedMatch match) {
		Log.i("TurnListener", "onTurnBasedMatchReceived");
		if (mMatch == null || match.getMatchId().equals(mMatch.getMatchId())) {
			updateMatch(match);
			return;
		}
		if (match.getMatchId().equals(rematchId)) {
			Toast.makeText(getContext(), "The re-match was updated.",
					TOAST_DELAY).show();
			updateMatch(match);
			return;
		}
		Toast.makeText(getContext(), "A match was updated.", TOAST_DELAY)
				.show();
	}

	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		Toast.makeText(getContext(), "A match was removed.", TOAST_DELAY)
				.show();

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
					getContext(),
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
			return true;
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
		case GamesClient.STATUS_MATCH_ERROR_INVALID_PARTICIPANT_STATE:
			showErrorMessage(match, statusCode, "Invalid participant state");
			break;
		case GamesClient.STATUS_MATCH_ERROR_OUT_OF_DATE_VERSION:
			showErrorMessage(match, statusCode, "Match out of date");
			break;
		case GamesClient.STATUS_MATCH_NOT_FOUND:
			showErrorMessage(match, statusCode, "Match not found");
			// get this for a rematch, where the game has not yet started
		default:

			showErrorMessage(match, statusCode, R.string.unexpected_status);
			Log.d(TAG, "Did not have warning or string to deal with: "
					+ statusCode);
		}

		return false;
	}

	public void showErrorMessage(TurnBasedMatch match, int statusCode,
			int stringId) {
		showWarning("Warning", getContext().getResources().getString(stringId));
	}

	public void showErrorMessage(TurnBasedMatch match, int statusCode,
			String string) {
		showWarning("Warning", string);
	}

	private void showWarning(String title, String message) {
		getHelper().showAlert(title, message);
	}

	@Override
	public void leaveRoom() {
		isVisible = false;
		if (mMatch != null
				&& mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_ACTIVE
				&& mMatch.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
			GameState gameState = new GameState(getMainActivity()
					.getGameFragment().getGame(), getMainActivity()
					.getGameFragment().getScore(), blackParticipantId, isQuick,
					true);
			byte[] bytes = gameState.toBytes(getHelper());
			getHelper().getGamesClient().takeTurn(this, mMatch.getMatchId(),
					bytes, getMeForChat().getParticipantId());
		}
		getMainActivity().getMenuFragment().leaveRoom();
	}

	@Override
	public void sendMove(Game game, AbstractMove lastMove, ScoreCard score) {
		// store the game, take turn
		GameState gameState = new GameState(game, score, blackParticipantId,
				isQuick, false);
		byte[] bytes = gameState.toBytes(getHelper());
		State state = game.getBoard().getState();
		getMainActivity().getGameFragment().setThinkingText(
				getOpponentName() + " hasn't seen your move.", true);
		if (state.isOver()) {
			finishGame(bytes, state);
		} else {
			final ProgressDialog progress = ProgressDialog.show(getContext(),
					"Sending Move", "Please wait");
			getHelper().getGamesClient().takeTurn(
					new OnTurnBasedMatchUpdatedListener() {
						@Override
						public void onTurnBasedMatchUpdated(int statusCode,
								TurnBasedMatch match) {
							progress.dismiss();
							TurnBasedMatchGameStrategy.this.onTurnBasedMatchUpdated(
									statusCode, match);
						}
					}, mMatch.getMatchId(), bytes,
					getOpponentParticipant().getParticipantId());
		}
	}

	private void finishGame(byte[] bytes, State state) {
		ParticipantResult myResult;
		ParticipantResult opponentResult;
		if (state.isDraw()) {
			myResult = new ParticipantResult(mMyParticipantId,
					ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
			opponentResult = new ParticipantResult(getOpponentParticipant()
					.getParticipantId(), ParticipantResult.MATCH_RESULT_TIE,
					ParticipantResult.PLACING_UNINITIALIZED);
		} else {
			boolean blackWon = state.getWinner().isBlack();
			// ParticipantResult result = new ParticipantResult(, result,
			// placing);
			boolean iWon = false;
			if (blackParticipantId.equals(getMeForChat().getParticipantId())
					&& blackWon) {
				iWon = true;
			} else if (!blackParticipantId.equals(getMeForChat()
					.getParticipantId()) && !blackWon) {
				iWon = true;
			}

			myResult = new ParticipantResult(mMyParticipantId,
					iWon ? ParticipantResult.MATCH_RESULT_WIN
							: ParticipantResult.MATCH_RESULT_LOSS,
					ParticipantResult.PLACING_UNINITIALIZED);
			opponentResult = new ParticipantResult(getOpponentParticipant()
					.getParticipantId(),
					!iWon ? ParticipantResult.MATCH_RESULT_WIN
							: ParticipantResult.MATCH_RESULT_LOSS,
					ParticipantResult.PLACING_UNINITIALIZED);
		}
		final ProgressDialog progress = ProgressDialog.show(getContext(),
				"Finishing Move", "Please wait");
		getHelper().getGamesClient().finishTurnBasedMatch(
				new OnTurnBasedMatchUpdatedListener() {
					@Override
					public void onTurnBasedMatchUpdated(int statusCode,
							TurnBasedMatch match) {
						progress.dismiss();
						TurnBasedMatchGameStrategy.this.onTurnBasedMatchUpdated(
								statusCode, match);
					}
				}, mMatch.getMatchId(), bytes, myResult, opponentResult);
	}

	@Override
	public Participant getMeForChat() {
		ArrayList<Participant> mParticipants = mMatch.getParticipants();

		if (!mParticipants.get(0).getParticipantId().equals(mMyParticipantId)) {
			return mParticipants.get(1);
		}
		return mParticipants.get(0);
	}

	private static class OpponentWrapper {
		Participant participant;
		boolean isQuick;

		public OpponentWrapper(Participant participant, boolean isQuick) {
			this.participant = participant;
			this.isQuick = isQuick;
		}

		public String getParticipantId() {
			if (participant == null) {
				return null;
			}
			return participant.getParticipantId();
		}

		public Uri getIconImageUri() {
			if (participant == null) {
				return null;
			}
			return participant.getIconImageUri();
		}

		public String getDisplayName() {
			if (participant == null || isQuick) {
				return "Anonymous";
			}
			return participant.getDisplayName();
		}

		public Object getActualDisplayName() {
			if (participant == null) {
				return "null";
			}
			return participant.getDisplayName();
		}
	}

	private OpponentWrapper getOpponentParticipant() {
		ArrayList<Participant> mParticipants = mMatch.getParticipants();

		if (!mParticipants.get(0).getParticipantId().equals(mMyParticipantId)) {
			return new OpponentWrapper(mParticipants.get(0), isQuick);
		}
		if (mParticipants.size() == 1) {
			return new OpponentWrapper(null, isQuick);
		}
		return new OpponentWrapper(mParticipants.get(1), isQuick);
	}

	@Override
	public String getOpponentName() {
		return getOpponentParticipant().getDisplayName();
	}

	@Override
	public void backFromWaitingRoom() {
		// start the match ?!
		// do nothing for turn based matches?
	}

	// If you choose to rematch, then call it and wait for a response.
	public void rematch() {
		getMainActivity().getGameFragment().setThinkingText("Starting rematch",
				true);
		final ProgressDialog progress = ProgressDialog.show(getContext(),
				"Starting a new match", "Please Wait...");
		Log.i("TurnListener", "rematch");
		getHelper().getGamesClient().rematchTurnBasedMatch(
				new OnTurnBasedMatchInitiatedListener() {
					@Override
					public void onTurnBasedMatchInitiated(int statusCode,
							TurnBasedMatch match) {
						progress.dismiss();
						getMainActivity().getGameFragment().resetThinkingText();
						getMainActivity().getGameFragment().hideStatusText();
						TurnBasedMatchGameStrategy.this
								.onTurnBasedMatchInitiated(statusCode, match);
					}
				}, mMatch.getMatchId());
		mMatch = null;
	}

	// This is the main function that gets called when players choose a match
	// from the inbox, or else create a match and want to start it.
	public void updateMatch(TurnBasedMatch match) {
		Log.i("TurnListener", "updateMatch");
		if (mMatch == null || !match.getMatchId().equals(mMatch.getMatchId())) {
			Log.i("TurnListener", "  mMatch=" + mMatch);
		}
		mMatch = match;
		mMyParticipantId = match.getParticipantId(getHelper().getGamesClient()
				.getCurrentPlayerId());
		showGame();
	}

	public static class GameState {
		public final Game game;
		public final ScoreCard score;
		public final String blackPlayerId;
		public final boolean isQuick;
		public boolean wasSeen;

		public GameState(Game game, ScoreCard score, String blackPlayerId,
				boolean isQuick, boolean wasSeen) {
			this.game = game;
			this.score = score;
			this.blackPlayerId = blackPlayerId;
			this.isQuick = isQuick;
			this.wasSeen = wasSeen;
		}

		public byte[] toBytes(GameHelper helper) {
			ByteBuffer theBuffer = ByteBuffer.allocate(helper.getGamesClient()
					.getMaxTurnBasedMatchDataSize());
			ByteBufferDebugger buffer = new ByteBufferDebugger(theBuffer);

			buffer.putInt("Protocol version", PROTOCOL_VERSION);

			buffer.put("wasSeen", (byte) (wasSeen ? 1 : 0));
			buffer.put("isQuicK", (byte) (isQuick ? 1 : 0));
			byte[] bytes;
			try {
				bytes = blackPlayerId.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unsupported UTF-8!");
			}
			buffer.putInt("Black player Id num bytes", bytes.length);
			buffer.put("Black player id bytes", bytes);

			buffer.putInt("Black wins", score.getBlackWins());
			buffer.putInt("White wins", score.getWhiteWins());
			buffer.putInt("Draws", score.getTotalGames());

			game.writeBytes(blackPlayerId, buffer);

			return theBuffer.array();
		}

		public static GameState fromMatch(Context context, GamesClient client,
				TurnBasedMatch match) {
			byte[] data = match.getData();
			boolean myTurn = match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN;
			return GameState.fromBytes(context, client, match, data, myTurn);
		}

		public static GameState fromBytes(Context context, GamesClient client,
				TurnBasedMatch match, byte[] data, boolean myTurn) {
			ByteBuffer theBuffer = ByteBuffer.wrap(data);
			ByteBufferDebugger buffer = new ByteBufferDebugger(theBuffer);

			int protocolVersion = buffer.getInt("Protocol version");
			if (protocolVersion != PROTOCOL_VERSION) {
				// TODO uh oh...
			}
			boolean wasSeen = buffer.get("wasSeen") == 1;
			boolean isQuick = buffer.get("isQuick") == 1;

			int len = buffer.getInt("Black player Id num bytes");
			byte[] blackplayerIdBytes = new byte[len];
			buffer.get("Black player id bytes", blackplayerIdBytes);
			String blackPlayerIdString;
			try {
				blackPlayerIdString = new String(blackplayerIdBytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Unsupported UTF-8!");
			}

			int blackWins = buffer.getInt("Black wins");
			int whiteWins = buffer.getInt("White wins");
			int totalGames = buffer.getInt("Draws");
			ScoreCard score = new ScoreCard(blackWins, whiteWins, totalGames
					- (blackWins + whiteWins));

			Player blackPlayer;
			Player whitePlayer;
			String localPlayerName = context
					.getString(R.string.local_player_name);
			Player currentPlayer;

			OpponentWrapper opponent;
			Participant me;
			Participant participant = match.getParticipants().get(0);
			if (participant.getPlayer() != null
					&& participant.getPlayer().getPlayerId()
							.equals(client.getCurrentPlayerId())) {
				me = participant;
				// TODO what about auto matching..
				if (match.getParticipants().size() == 1) {
					opponent = new OpponentWrapper(null, false);
				} else {
					opponent = new OpponentWrapper(match.getParticipants().get(
							1), false);
				}
			} else {
				me = match.getParticipants().get(1);
				opponent = new OpponentWrapper(participant, false);
			}

			if (blackPlayerIdString.equals(me.getParticipantId())) {
				blackPlayer = HumanStrategy.createPlayer(localPlayerName, true,
						me.getIconImageUri());
				whitePlayer = OnlineStrategy.createPlayer(
						opponent.getDisplayName(), false,
						opponent.getIconImageUri());
				if (myTurn) {
					currentPlayer = blackPlayer;
				} else {
					currentPlayer = whitePlayer;
				}
			} else {
				whitePlayer = HumanStrategy.createPlayer(localPlayerName,
						false, me.getIconImageUri());
				blackPlayer = OnlineStrategy.createPlayer(
						opponent.getDisplayName(), true,
						opponent.getIconImageUri());
				if (!myTurn) {
					currentPlayer = blackPlayer;
				} else {
					currentPlayer = whitePlayer;
				}
			}

			Game game = Game.fromBytes(blackPlayer, whitePlayer, currentPlayer,
					buffer);

			GameState gameState = new GameState(game, score,
					blackPlayerIdString, isQuick, wasSeen);

			return gameState;
		}

	}

	public void showGame() {
		Log.i("TurnListener", "showGame");
		int status = mMatch.getStatus();
		int turnStatus = mMatch.getTurnStatus();

		String waitingText = null;
		switch (status) {
		case TurnBasedMatch.MATCH_STATUS_CANCELED:
			showWarning("Canceled!", "This game was canceled!");
		case TurnBasedMatch.MATCH_STATUS_EXPIRED:
			showWarning("Expired!", "This game is expired.  So sad!");
		case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
			showWarning("Waiting for auto-match...",
					"We're still waiting for an automatch partner.");
			waitingText = "Waiting for someone to join";
		case TurnBasedMatch.MATCH_STATUS_COMPLETE:
			Log.i("TurnListener", "   showGame - complete");
			// handled down below
		}

		// OK, it's active. Check on turn status.
		switch (turnStatus) {
		case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
			Log.i("TurnListener", "   showGame - my turn");
			// setGameplayUI();
			break;
		case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
			Log.i("TurnListener", "   showGame - their turn");
			break;
		case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
			Log.i("TurnListener", "   showGame - status invited");
			showWarning("Good inititative!",
					"Still waiting for invitations.\n\nBe patient!");
			break;
		}

		GameFragment gameFragment;
		if (!isVisible) {
			Log.i("TurnListener", "  showing fragment");
			gameFragment = GameFragment.createFragment(this);

			FragmentManager manager = getMainActivity()
					.getSupportFragmentManager();
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(R.id.main_frame, gameFragment,
					MainActivity.FRAG_TAG_GAME);
			transaction.addToBackStack(null);
			transaction.commit();
		} else {
			Log.i("TurnListener", "  reusing fragment");
			gameFragment = getMainActivity().getGameFragment();
		}
		isVisible = true;

		GameState state = GameState.fromMatch(getContext(), getHelper()
				.getGamesClient(), mMatch);
		blackParticipantId = state.blackPlayerId;
		type = state.game.getType();
		isQuick = state.isQuick;
		if (!state.game.getBoard().getState().isOver()
				&& turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN
				&& !state.wasSeen) {
			Log.i("TurnListener", "  marking that we saw the opponent's move");
			state.wasSeen = true;
			getHelper().getGamesClient().takeTurn(
					new OnTurnBasedMatchUpdatedListener() {
						@Override
						public void onTurnBasedMatchUpdated(int statusCode,
								TurnBasedMatch match) {
							// we don't need to receive notice about marking we
							// saw the move
							// otherwise we occasionally get a stale board on
							// update from this
							if (statusCode == GamesClient.STATUS_OK) {
								return;
							}
							if (statusCode == GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED) {
								Log.i(TAG,
										"Deferring message that we saw the opponent's move");
								return;
							}
							showWarning("Error",
									"Error marking that we saw opponent's move. Status = "
											+ statusCode);

						}
					}, mMatch.getMatchId(), state.toBytes(getHelper()),
					mMyParticipantId);
		}
		Log.i("TurnListener",
				"   showGame - move#" + state.game.getNumberOfMoves());

		// TODO Undo the last move, so it can be reapplied and animated?

		// play winning sound, and animate the move received
		boolean showMove = turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN;
		gameFragment.startGame(state.game, state.score, waitingText, showMove);

		// show if someone won
		State state2 = state.game.getBoard().getState();
		if (state2.isOver()) {
			Log.i("TurnListener", "  game is over, calling handleGameOver");
			handleGameOver(status, turnStatus, gameFragment, state);
			return;
		}
		if (!state.wasSeen) {
			gameFragment.setThinkingText(getOpponentName()
					+ " hasn't seen your move.", true);
		} else {
			gameFragment.setThinkingText(null);
			gameFragment.setOpponentThinking();
			if (!showMove) {
				gameFragment.showStatusText();
			}
		}
	}

	private void handleGameOver(int status, int turnStatus,
			GameFragment gameFragment, GameState state) {
		Log.i("TurnListener", "handleGameOver");
		if (completeMatch(status, turnStatus)) {
			Log.i("TurnListener", "  handleGameOver complete match");
			return;
		}
		// TODO
		String winnerName;
		if (state.game.getBoard().getState().getWinner().isBlack()) {
			if (blackParticipantId.equals(getMeForChat().getParticipantId())) {
				winnerName = "You";
			} else {
				winnerName = getOpponentName();
			}
		} else {
			if (!blackParticipantId.equals(getMeForChat().getParticipantId())) {
				winnerName = "You";
			} else {
				winnerName = getOpponentName();
			}
		}
		String winTitle = winnerName + " won!";
		gameFragment.setThinkingText(winTitle, true);
		gameFragment.showStatusText();
		promptToPlayAgain(winnerName, winTitle, gameFragment);
	}

	private boolean completeMatch(int status, int turnStatus) {
		if (status == TurnBasedMatch.MATCH_STATUS_COMPLETE
				&& turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
			getHelper().getGamesClient().finishTurnBasedMatch(
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
							// String rematchId = match.getRematchId();
							// if (rematchId == null) {
							// showWarning("no rematch", "no rematch");
							// } else {
							// showWarning("Rematch", "Rematch");
							// }
						}
					}, mMatch.getMatchId());
			return true;
		}
		return false;
	}

	protected String rematchId;

	private void lookForInviteForRematch(final String winner,
			final String title, final String rematchId) {
		getMainActivity().getGameFragment().setThinkingText(
				title + "Rematch requested, looking for match invite.", true);
		getHelper().getGamesClient().loadInvitations(
				new OnInvitationsLoadedListener() {

					@Override
					public void onInvitationsLoaded(int status,
							InvitationBuffer invites) {
						if (!checkStatusCode(mMatch, status)) {
							alreadyLoadingRematch = false;
							invites.close();
							return;
						}

						List<Invitation> turnInvitesFromPlayer = new ArrayList<Invitation>();
						for (Iterator<Invitation> iter = invites.iterator(); iter
								.hasNext();) {
							Invitation invite = iter.next();
							if (invite.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED) {
								if (invite
										.getInviter()
										.getDisplayName()
										.equals(getOpponentParticipant()
												.getActualDisplayName())) {
									turnInvitesFromPlayer.add(invite);
								}
							}
						}
						if (turnInvitesFromPlayer.isEmpty()) {
							alreadyLoadingRematch = false;

							openPlayAgain(getMainActivity().getGameFragment(),
									winner);
							playAgainDialog.displayWaitingForInvite();

							getMainActivity()
									.getGameFragment()
									.setThinkingText(
											title
													+ "Rematch requested, awaiting a move from "
													+ getOpponentName() + ".",
											true);
							// listen for invites
							getHelper().getGamesClient()
									.registerInvitationListener(
											TurnBasedMatchGameStrategy.this);
							TurnBasedMatchGameStrategy.this.rematchId = rematchId;

						} else {
							alreadyLoadingRematch = false;
							askToAcceptRematchInvite(winner, title,
									turnInvitesFromPlayer, invites);
						}
						invites.close();

					}

				});
	}

	private boolean alreadyLoadingRematch = false;

	private void promptAndGoToRematch(final String winner, final String title,
			final String rematchId) {
		Log.i("TurnListener", "  promptAndGoToRematch");
		// attempt to get the re-match
		if (alreadyLoadingRematch) {
			Log.i("TurnListener",
					"  promptAndGoToRematch- already loading rematch");
			return;
		}

		alreadyLoadingRematch = true;
		getHelper().getGamesClient().getTurnBasedMatch(
				new OnTurnBasedMatchLoadedListener() {
					@Override
					public void onTurnBasedMatchLoaded(int status,
							final TurnBasedMatch match) {
						if (status == GamesClient.STATUS_MATCH_NOT_FOUND) {
							Log.i("TurnListener",
									"  promptAndGoToRematch match not found");
							// attempt to find invite for this match?
							lookForInviteForRematch(winner, title, rematchId);
							return;
						}
						// we got a match, or some other unexpected error, run
						// with it
						if (!checkStatusCode(match, status)) {
							alreadyLoadingRematch = false;
							return;
						}
						Log.i("TurnListener",
								"  promptAndGoToRematch match found");
						// display to the user that a rematch exists, and on
						// click, go to it

						openPlayAgain(getMainActivity().getGameFragment(),
								winner);
						playAgainDialog.displayGoToRematch(match);

						alreadyLoadingRematch = false;
					}
				}, rematchId);

	}

	private void askToAcceptRematchInvite(final String winner,
			final String title, List<Invitation> turnInvitesFromPlayer,
			final InvitationBuffer invites) {
		Log.i("TurnListener", "  askToAcceptRematchInvite");
		if (turnInvitesFromPlayer.size() == 1) {
			Log.i("TurnListener", "  askToAcceptRematchInvite one invite");
			getMainActivity().getGameFragment().setThinkingText(
					title + "A possible rematch invite exists.", true);
			Invitation theInvite = turnInvitesFromPlayer.get(0);
			final String inviteId = theInvite.getInvitationId();

			openPlayAgain(getMainActivity().getGameFragment(), winner);
			playAgainDialog.displayAcceptInvite(inviteId);

		} else {
			Log.i("TurnListener", "  askToAcceptRematchInvite multiple invites");
			// TODO show a list prompt...
			getMainActivity().getGameFragment().setThinkingText(
					title + "Rematch requested, multiple invites from "
							+ getOpponentName() + "...", true);
			showWarning("Multiple invites", "Multiple invites from "
					+ getOpponentName() + ", can't tell which is the rematch.");
		}
	}

	@Override
	public boolean warnToLeave() {
		return false;
	}

	@Override
	public void promptToPlayAgain(String winner, String title) {
		promptToPlayAgain(winner, title, getMainActivity().getGameFragment());
	}

	public void promptToPlayAgain(final String winner, final String title,
			final GameFragment fragment) {
		Log.i("TurnListener", "promptToPlayAgain");
		fragment.setThinkingText(title, true);
		if (mMatch.getStatus() != TurnBasedMatch.MATCH_STATUS_COMPLETE) {
			// wait till we receive notice of our move, and then prompt whether
			// to play again
			fragment.setThinkingText(title, true);
			return;
		}

		if (!mMatch.canRematch()) {
			Log.i("TurnListener", "  promptToPlayAgain can't rematch");
			final String rematchId = mMatch.getRematchId();
			if (rematchId != null) {
				promptAndGoToRematch(winner, title, rematchId);
				return;
			}
			Log.i("TurnListener",
					"  promptToPlayAgain can't rematch, no existing rematch");
		}

		if (!mMatch.canRematch()) {
			showWarning("Can't rematch", "But no existing rematch id?!");
		} else {
			Log.i("TurnListener", "  promptToPlayAgain can rematch");

			openPlayAgain(fragment, winner);
			playAgainDialog.displayAskRematch();
		}
	}

	@Override
	public void sendInChat(boolean b) {
		// do nothing for a turn based message chat
	}

	@Override
	public void sendMessage(String string) {

		// TODO Auto-generated method stub
		// TODO append the message to the chat session, to be relayed when this
		// turn is submitted
	}

	public void onFragmentResume() {
		if (onResume != null) {
			onResume.run();
		}
	}

	@Override
	public void onResume(MainActivity theActivity) {
		setMainActivity(theActivity);

		if (!getHelper().isSignedIn()) {
			if (getHelper().getGamesClient().isConnecting()) {
				getMainActivity().getGameFragment().setThinkingText(
						"Reconnecting...", true);
				getMainActivity().getGameFragment().showStatusText();
			}
		}

		// TODO not reconnecting, give warning, and go back to main menu..
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				getContext());

		alertDialogBuilder.setTitle("Not logged in...");
		alertDialogBuilder
				.setMessage("Can't continue game without being logged in.");
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setPositiveButton("Menu", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				leaveRoom();
			}
		});
	}

}
