package com.oakonell.ticstacktoe.ui.network.realtime;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer.ReliableMessageSentCallback;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.rank.GameOutcome;
import com.oakonell.ticstacktoe.model.rank.RankStorage;
import com.oakonell.ticstacktoe.model.rank.RankedGame;
import com.oakonell.ticstacktoe.model.rank.RankingRater;
import com.oakonell.ticstacktoe.rank.RankHelper;
import com.oakonell.ticstacktoe.rank.RankHelper.OnMyRankUpdated;
import com.oakonell.ticstacktoe.rank.RankHelper.OnRankReceived;
import com.oakonell.ticstacktoe.rank.RankHelper.RankInfoUpdated;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.game.OnlineStrategy;
import com.oakonell.ticstacktoe.ui.local.AiGameStrategy.PostRankUpdate;
import com.oakonell.ticstacktoe.ui.network.AbstractNetworkedGameStrategy;
import com.oakonell.ticstacktoe.utils.ByteBufferDebugger;

public class RealtimeGameStrategy extends AbstractNetworkedGameStrategy
		implements RoomUpdateListener, RealTimeMessageReceivedListener,
		RoomStatusUpdateListener {
	private static final Random random = new Random();
	private static final String TAG = RealtimeGameStrategy.class.getName();

	private static final int PROTOCOL_VERSION = 1;
	private static final byte MSG_WHO_IS_X = 1;
	private static final byte MSG_MOVE = 2;
	private static final byte MSG_MESSAGE = 3;
	private static final byte MSG_SEND_VARIANT = 4;
	private static final byte MSG_PLAY_AGAIN = 5;
	private static final byte MSG_IN_CHAT = 6;
	private static final byte MSG_CLOSE_CHAT = 7;
	private static final byte MSG_PROTOCOL_VERSION = 8;
	private static final byte MSG_RANK = 9;
	private static final byte MSG_PLAY_AGAIN_RANK = 10;

	private int opponentProtocolVersion;

	private String mRoomId;
	private ArrayList<Participant> mParticipants;
	private String mMyParticipantId;

	private volatile Long myRandom;
	private volatile Long theirRandom;

	private boolean isRanked;
	private boolean isQuick;
	private boolean isConnected;
	private boolean initiatedTheGame;
	private GameType type;

	private OnlinePlayAgainFragment onlinePlayAgainDialog;
	private boolean iAmBlack;

	public RealtimeGameStrategy(GameContext context, GameType type,
			boolean isQuick, boolean initiatedTheGame, boolean isRanked) {
		super(context);
		// TODO the isRanked may come from the invitation game and need to be
		// adjusted
		this.isRanked = isRanked;
		this.type = type;
		this.isQuick = isQuick;
		this.initiatedTheGame = initiatedTheGame;
	}

	// Called when we are connected to the room. We're not ready to play yet!
	// (maybe not everybody
	// is connected yet).
	@Override
	public void onConnectedToRoom(Room room) {
		isConnected = true;
		announce("onConnectedToRoom");

		// get room ID, participants and my ID:
		mRoomId = room.getRoomId();
		mParticipants = room.getParticipants();
		mMyParticipantId = room.getParticipantId(Games.Players
				.getCurrentPlayerId(getHelper().getApiClient()));

		// print out the list of participants (for debug purposes)
		Log.d(TAG, "Room ID: " + mRoomId);
		Log.d(TAG, "My ID " + mMyParticipantId);
		Log.d(TAG, "<< CONNECTED TO ROOM>>");
	}

	// Called when we get disconnected from the room. We return to the main
	// screen.
	@Override
	public void onDisconnectedFromRoom(Room arg0) {
		announce("onDisconnectedFromRoom");
		isConnected = false;
	}

	// We treat most of the room update callbacks in the same way: we update our
	// list of participants
	@Override
	public void onPeerDeclined(Room room, List<String> arg1) {
		announce("onPeerDeclined");
		updateRoom(room);
	}

	@Override
	public void onPeerInvitedToRoom(Room room, List<String> arg1) {
		announce("onPeerInvitedToRoom");
		updateRoom(room);
	}

	@Override
	public void onPeerJoined(Room room, List<String> arg1) {
		announce("onPeerJoined");
		updateRoom(room);
	}

	@Override
	public void onRoomAutoMatching(Room room) {
		announce("onPeerRoomAutoMatching");
		updateRoom(room);
	}

	@Override
	public void onRoomConnecting(Room room) {
		announce("onRoomConnecting");
		updateRoom(room);
	}

	@Override
	public void onPeersConnected(Room room, List<String> peers) {
		announce("onPeersConnected");
		updateRoom(room);
	}

	@Override
	public void onPeersDisconnected(Room room, List<String> peers) {
		announce("onPeerDisconnected");
		updateRoom(room);
	}

	@Override
	public void onP2PConnected(String arg0) {
		announce("Connected to P2P " + arg0);
	}

	@Override
	public void onP2PDisconnected(String arg0) {
		announce("Disconnected from P2P " + arg0);
	}

	void updateRoom(Room room) {
		mParticipants = room.getParticipants();
	}

	@Override
	public void onPeerLeft(Room room, List<String> peersWhoLeft) {
		// opponent left, notify the main game
		opponentLeft();

		announce("onPeerLeft");
		updateRoom(room);
	}

	// Called when we receive a real-time message from the network.
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage message) {
		byte[] messageData = message.getMessageData();
		ByteBuffer buffer = ByteBuffer.wrap(messageData);
		byte type = buffer.get();
		if (type == MSG_PROTOCOL_VERSION) {
			opponentProtocolVersion = buffer.getInt();
		} else if (type == MSG_WHO_IS_X) {
			theirRandom = buffer.getLong();
			if (myRandom == null) {
				// ignore, let the later backFromWaitingRoom receiver handle
				// this
				return;
			}
			checkWhoIsFirstAndAttemptToStart(false);
		} else if (type == MSG_MOVE) {
			onlineMoveReceived(new ByteBufferDebugger(buffer));
		} else if (type == MSG_MESSAGE) {
			int numBytes = buffer.getInt();
			byte[] bytes = new byte[numBytes];
			buffer.get(bytes);
			String string;
			try {
				string = new String(bytes, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("UTF-8 charset not present!?");
			}

			messageRecieved(getOpponentParticipant(), string);
		} else if (type == MSG_SEND_VARIANT) {
			int sentVariant = buffer.getInt();
			boolean sentIsRanked = buffer.getInt() == 1;
			if (this.type != null) {
				// verify that the size agree
				if (this.type.getVariant() != sentVariant) {
					throw new RuntimeException(
							"Opponent's variant setting does not match!");
				}
				if (isRanked != sentIsRanked) {
					throw new RuntimeException(
							"Opponent's isRanked setting does not match!");
				}
			} else {
				this.type = GameType.fromVariant(sentVariant);
				this.isRanked = sentIsRanked;
				announce("Received variant");
			}
		} else if (type == MSG_PLAY_AGAIN) {
			boolean playAgain = buffer.getInt() != 0;
			receivePlayAgain(playAgain);
		} else if (type == MSG_IN_CHAT) {
			opponentInChat();
		} else if (type == MSG_CLOSE_CHAT) {
			opponentClosedChat();
		} else if (type == MSG_RANK) {
			int opponentRank = buffer.getInt();
			updateOpponentRank((short) opponentRank);
		} else if (type == MSG_PLAY_AGAIN_RANK) {
			int opponentRank = buffer.getInt();
			updatePlayAgainOpponentRank((short) opponentRank);
		} else {
			// handle later version future support
			if (opponentProtocolVersion > PROTOCOL_VERSION) {
				// assume an optional new message type, which can be ignored
				return;
			}
			throw new RuntimeException("unexpected message type! " + type);
		}

	}

	short opponentRank = -1;

	private void updateOpponentRank(short opponentRank) {
		RankInfo info = getRankInfo();
		if (info == null) {
			this.opponentRank = opponentRank;
			return;
		}
		if (iAmBlack) {
			info.setWhiteRank(opponentRank);
		} else {
			info.setBlackRank(opponentRank);
		}
		getGameFragment().refreshHeader();
	}

	public boolean shouldHideAd() {
		return true;
	}

	private void startGame(final boolean iAmBlack) {
		this.iAmBlack = iAmBlack;
		final GameFragment gameFragment = GameFragment.createFragment();
		// ads in online play will leave the room.. hide the ad to avoid the
		// problem
		final ScoreCard score = new ScoreCard(0, 0, 0);
		Player blackPlayer;
		Player whitePlayer;
		String localPlayerName = getContext().getString(
				R.string.local_player_name);
		if (iAmBlack) {
			blackPlayer = HumanStrategy.createPlayer(localPlayerName, true,
					getMeForChat().getIconImageUri());
			whitePlayer = OnlineStrategy.createPlayer(getOpponentName(), false,
					getOpponentParticipant().getIconImageUri());
		} else {
			whitePlayer = HumanStrategy.createPlayer(localPlayerName, false,
					getMeForChat().getIconImageUri());
			blackPlayer = OnlineStrategy.createPlayer(getOpponentName(), true,
					getOpponentParticipant().getIconImageUri());
		}
		Tracker myTracker = EasyTracker.getTracker();
		myTracker.sendEvent(
				getContext().getString(R.string.an_start_game_cat),
				(isQuick ? getContext().getString(
						R.string.an_start_quick_game_action) : getContext()
						.getString(R.string.an_start_online_game_action)),
				type.getVariant() + "", 0L);

		if (!isRanked) {
			startGame(gameFragment, blackPlayer, whitePlayer, score, null);
			return;
		}
		final Player theBlackPlayer = blackPlayer;
		final Player theWhitePlayer = whitePlayer;

		RankHelper.createRankInfo(getGameContext(), type, iAmBlack,
				new RankInfoUpdated() {
					@Override
					public void onRankInfoUpdated(RankInfo rankInfo) {
						if (iAmBlack) {
							sendRank(rankInfo.blackRank());
							if (opponentRank > 0) {
								rankInfo.setWhiteRank(opponentRank);
								opponentRank = -1;
							}
						} else {
							sendRank(rankInfo.whiteRank());
							if (opponentRank > 0) {
								rankInfo.setBlackRank(opponentRank);
								opponentRank = -1;
							}
						}
						startGame(gameFragment, theBlackPlayer, theWhitePlayer,
								score, rankInfo);
					}
				});

	}

	private void startGame(GameFragment gameFragment, Player blackPlayer,
			Player whitePlayer, ScoreCard score, RankInfo rankInfo) {
		Game game = new Game(type, GameMode.ONLINE, blackPlayer, whitePlayer,
				blackPlayer);
		setRankInfo(rankInfo);
		setGame(game);
		setScore(score);

		gameFragment.startGame(null, true);
		FragmentManager manager = getActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				GameContext.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		announce("onJoinedRoom");
		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onJoinedRoom, status " + statusCode);
			showGameError(R.string.onJoinedRoom, statusCode);
			leaveRoom();
			getMenuFragment().setActive();
			return;
		}

		// show the waiting room UI
		showWaitingRoom(room);
	}

	// Called when we've successfully left the room (this happens a result of
	// voluntarily leaving
	// via a call to leaveRoom(). If we get disconnected, we get
	// onDisconnectedFromRoom()).
	@Override
	public void onLeftRoom(int arg0, String arg1) {
		announce("onLeftRoom");
		getActivity().getSupportFragmentManager().popBackStack();
	}

	// Called when room is fully connected.
	@Override
	public void onRoomConnected(int statusCode, Room room) {
		announce("onRoomConnected");

		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
			showGameError(R.string.onRoomConnected, statusCode);
			leaveRoom();
			getMenuFragment().setActive();
			return;
		}
		updateRoom(room);
	}

	// Called when room has been created
	@Override
	public void onRoomCreated(int statusCode, Room room) {
		announce("onRoomCreated");

		if (statusCode != GamesClient.STATUS_OK) {
			Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
			showGameError(R.string.onRoomCreated, statusCode);
			leaveRoom();
			getMenuFragment().setActive();
			return;
		}

		// show the waiting room UI
		showWaitingRoom(room);
	}

	private void announce(String string) {
		Log.d(TAG, string);
	}

	private void showGameError(int typeRes, int errorNum) {
		String message = GooglePlayServicesUtil.getErrorString(errorNum);
		if (message.startsWith("UNKNOWN")) {
			if (errorNum == 6001) {
				message = getContext().getString(
						R.string.cannot_invite_non_tester);
			}
		}
		showAlert(getContext().getString(R.string.communication_error) + " ("
				+ errorNum + ") " + message);
	}

	private void showAlert(String string) {
		Toast.makeText(getContext(), string, Toast.LENGTH_LONG).show();
	}

	// Show the waiting room UI to track the progress of other players as they
	// enter the
	// room and get connected.
	void showWaitingRoom(Room room) {
		// mWaitRoomDismissedFromCode = false;

		// minimum number of players required for our game
		int minPlayersToStart = 2;

		Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(
				getHelper().getApiClient(), room, minPlayersToStart);

		// show waiting room UI
		// can we launch this from the start fragment
		getActivity().startActivityForResult(intent,
				GameContext.RC_WAITING_ROOM);
	}

	public void leaveRoom() {
		if (mRoomId != null) {
			Games.RealTimeMultiplayer.leave(getHelper().getApiClient(), this,
					mRoomId);
		}
	}

	public String getOpponentName() {
		if (isQuick) {
			return "Anonymous";
		}
		return getOpponentParticipant().getDisplayName();
	}

	private Participant getOpponentParticipant() {
		Participant participant0 = mParticipants.get(0);
		String participant0Id = participant0.getParticipantId();
		if (!participant0Id.equals(mMyParticipantId)) {
			return participant0;
		}
		return mParticipants.get(1);
	}

	public String getOpponentId() {
		return getOpponentParticipant().getParticipantId();
	}

	public String getRoomId() {
		return mRoomId;
	}

	public void backFromWaitingRoom() {
		// player wants to start playing
		Log.d(TAG, "Starting game because user requested via waiting room UI.");

		// let other players know we're starting.
		// Toast.makeText(context, "Start the game!",
		// Toast.LENGTH_SHORT).show();

		sendProtocolVersion();
		if (type != null) {
			ByteBuffer buffer = ByteBuffer
					.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
			buffer.put(MSG_SEND_VARIANT);
			buffer.putInt(type.getVariant());
			buffer.putInt(isRanked ? 1 : 0);
			Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
					.getApiClient(), new ReliableMessageSentCallback() {
				@Override
				public void onRealTimeMessageSent(int statusCode, int token,
						String recipientParticipantId) {
					if (statusCode == GamesClient.STATUS_OK) {

					} else if (statusCode == GamesClient.STATUS_REAL_TIME_MESSAGE_SEND_FAILED) {

					} else if (statusCode == GamesClient.STATUS_REAL_TIME_ROOM_NOT_JOINED) {

					} else {

					}

				}
			}, buffer.array(), getRoomId(), getOpponentId());
		}

		myRandom = random.nextLong();
		checkWhoIsFirstAndAttemptToStart(true);
	}

	private void sendProtocolVersion() {
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(MSG_PROTOCOL_VERSION);
		buffer.putLong(PROTOCOL_VERSION);
		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode == GamesClient.STATUS_OK) {

				} else if (statusCode == GamesClient.STATUS_REAL_TIME_MESSAGE_SEND_FAILED) {

				} else if (statusCode == GamesClient.STATUS_REAL_TIME_ROOM_NOT_JOINED) {

				} else {

				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());
	}

	private void checkWhoIsFirstAndAttemptToStart(boolean send) {
		boolean start = false;
		boolean iAmBlack = true;
		if (theirRandom != null) {
			start = true;
			while (true) {
				// keep the move random seeds in sync, as a checksum (no
				// cheating!)
				if (myRandom < theirRandom) {
					// I'm black, they're white
					iAmBlack = true;
					break;
				} else if (myRandom > theirRandom) {
					// I'm white, they're black
					iAmBlack = false;
					break;
				} else {
					// try again
					send = true;
					myRandom = random.nextLong();
					theirRandom = null;
				}
			}
		}
		if (send) {
			ByteBuffer buffer = ByteBuffer
					.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
			buffer.put(MSG_WHO_IS_X);
			buffer.putLong(myRandom);

			Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
					.getApiClient(), new ReliableMessageSentCallback() {

				@Override
				public void onRealTimeMessageSent(int statusCode, int token,
						String recipientParticipantId) {
					if (statusCode == GamesClient.STATUS_OK) {

					} else if (statusCode == GamesClient.STATUS_REAL_TIME_MESSAGE_SEND_FAILED) {

					} else if (statusCode == GamesClient.STATUS_REAL_TIME_ROOM_NOT_JOINED) {

					} else {

					}
				}
			}, buffer.array(), getRoomId(), getOpponentId());

		}
		if (start && type != null) {
			startGame(iAmBlack);
		}
	}

	public void sendHumanMove() {
		AbstractMove move = getGame().getBoard().getState().getLastMove();
		ByteBuffer theBuffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		ByteBufferDebugger buffer = new ByteBufferDebugger(theBuffer);
		buffer.put("Move", MSG_MOVE);

		move.appendBytesToMessage(buffer);

		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode != GamesClient.STATUS_OK) {
					showGameError(R.string.sendMove, statusCode);
					leaveRoom();
				}
			}
		}, theBuffer.array(), getRoomId(), getOpponentId());

	}

	public void restartGame() {
		opponentSendPlayAgain = PlayAgainState.WAITING;
	}

	PlayAgainState opponentSendPlayAgain = PlayAgainState.WAITING;

	private void receivePlayAgain(boolean playAgain) {
		opponentSendPlayAgain = playAgain ? PlayAgainState.PLAY_AGAIN
				: PlayAgainState.NOT_PLAY_AGAIN;
		if (playAgain) {
			opponentWillPlayAgain();
		} else {
			opponentWillNotPlayAgain();
		}
	}

	public enum PlayAgainState {
		WAITING, PLAY_AGAIN, NOT_PLAY_AGAIN;
	}

	public PlayAgainState getOpponentPlayAgainState() {
		return opponentSendPlayAgain;
	}

	public void sendPlayAgain(final Runnable success, final Runnable error) {
		sendPlayAgain(true, success, error);
	}

	public void sendNotPlayAgain(final Runnable success, final Runnable error) {
		sendPlayAgain(false, success, error);
	}

	private void sendPlayAgain(boolean playAgain, final Runnable success,
			final Runnable error) {
		if (!isConnected && !playAgain) {
			success.run();
			return;
		}
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(MSG_PLAY_AGAIN);
		buffer.putInt(playAgain ? 1 : 0);

		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode == GamesClient.STATUS_OK) {
					if (success != null) {
						success.run();
					}
				} else {
					if (error != null) {
						error.run();
					}
				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());

	}

	public void sendMessage(String string) {
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(MSG_MESSAGE);

		byte[] bytes;
		try {
			bytes = string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported UTF-8?!");
		}

		buffer.putInt(bytes.length);
		buffer.put(bytes);
		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode != GamesClient.STATUS_OK) {
					showGameError(R.string.sendRealTimeMessage, statusCode);
				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());
	}

	public Participant getMeForChat() {
		if (mParticipants.get(0).getParticipantId().equals(mMyParticipantId)) {
			return mParticipants.get(0);
		}
		return mParticipants.get(1);
	}

	public void sendInChat(boolean inChat) {
		if (!isConnected) {
			return;
		}
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(inChat ? MSG_IN_CHAT : MSG_CLOSE_CHAT);

		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode == GamesClient.STATUS_OK) {
					// hmm..
				} else {
					// don't worry
				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());

	}

	public boolean isInitiatedTheGame() {
		return initiatedTheGame;
	}

	@Override
	public boolean warnToLeave() {
		return true;
	}

	public void promptToPlayAgain(String winner, String title) {
		onlinePlayAgainDialog = new OnlinePlayAgainFragment();
		onlinePlayAgainDialog.initialize(this, getOpponentName(), title,
				iAmBlack);

		// TODO store the completed real-time game in the db, to show in the
		// history

		if (getRankInfo() == null) {
			onlinePlayAgainDialog.show(getGameFragment()
					.getChildFragmentManager(), "playAgain");
			return;
		}

		RankHelper.loadRankStorage(getGameContext(), new OnRankReceived() {
			@Override
			public void receivedRank(RankStorage storage) {
				short myRank = storage.getRank(getGame().getType()).getRank();
				sendPlayAgainRank(myRank);
				playAgainMy = myRank;
				conditionallyUpdateRanks();
			}

		}, true);

		onlinePlayAgainDialog.show(getGameFragment().getChildFragmentManager(),
				"playAgain");

		// TODO show the updated ranks in the play again dialog...
		// TODO wire up the play again / not play again message handling via
		// the dialog
	}

	private short playAgainOpponent = -1;
	private short playAgainMy = -1;

	private void conditionallyUpdateRanks() {
		if (playAgainOpponent > 0 && playAgainMy > 0) {
			final RankInfo info = getRankInfo();
			if (iAmBlack) {
				info.setBlackRank(playAgainMy);
				info.setWhiteRank(playAgainOpponent);
			} else {
				info.setBlackRank(playAgainOpponent);
				info.setWhiteRank(playAgainMy);
			}
			playAgainOpponent = -1;
			playAgainMy = -1;
			PostRankUpdate postRankUpdate = new PostRankUpdate() {
				@Override
				public void ranksUpdated(short oldBlackRank,
						short newBlackRank, short oldWhiteRank,
						short newWhiteRank) {
					onlinePlayAgainDialog.updateRanks(oldBlackRank,
							newBlackRank, oldWhiteRank, newWhiteRank);
					info.setBlackRank(newBlackRank);
					info.setWhiteRank(newWhiteRank);
				}
			};
			updateRanks(postRankUpdate);
		}
	}

	private void updatePlayAgainOpponentRank(short opponentRank2) {
		playAgainOpponent = opponentRank2;
		conditionallyUpdateRanks();
	}

	private void updateRanks(final PostRankUpdate postUpdate) {
		State state = getGame().getBoard().getState();
		final GameOutcome outcome;
		Player winner = state.getWinner();
		if (winner != null) {
			outcome = iAmBlack == winner.isBlack() ? GameOutcome.WIN
					: GameOutcome.LOSE;
		} else {
			outcome = GameOutcome.DRAW;
		}

		RankInfo rankInfo = getRankInfo();
		// short myStartRank = iAmBlack ? rankInfo.blackRank() :
		// rankInfo.whiteRank();
		final short opponentRank = iAmBlack ? rankInfo.whiteRank() : rankInfo
				.blackRank();
		RankHelper.updateRank(getGameContext(), getGame().getType(),
				new RankedGame((short) opponentRank, outcome),
				new OnMyRankUpdated() {
					@Override
					public void onRankUpdated(short oldRank, short newRank) {
						sendPlayAgainRank(newRank);
						// calculate locally, for real time, should be good?
						// will receive updated above
						short opponentNewRank = RankingRater.Factory
								.getRanker().calculateRank(opponentRank,
										oldRank, outcome.opposite());
						if (postUpdate != null) {
							if (iAmBlack) {
								postUpdate.ranksUpdated(oldRank, newRank,
										opponentRank, opponentNewRank);
							} else {
								postUpdate.ranksUpdated(opponentRank,
										opponentNewRank, oldRank, newRank);
							}
						}
					}
				});

	}

	private void sendPlayAgainRank(short rank) {
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(MSG_PLAY_AGAIN_RANK);
		buffer.putInt(rank);

		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode == GamesClient.STATUS_OK) {
					// hmm...
				} else {
					// hmmm
				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());
	}

	public void opponentWillPlayAgain() {
		if (onlinePlayAgainDialog == null) {
			return;
		}
		onlinePlayAgainDialog.opponentWillPlayAgain();
	}

	public void opponentWillNotPlayAgain() {
		if (onlinePlayAgainDialog == null) {
			return;
		}
		onlinePlayAgainDialog.opponentWillNotPlayAgain();
	}

	public void playAgainClosed() {
		onlinePlayAgainDialog = null;
		restartGame();
	}

	public void opponentLeft() {
		if (getGameFragment() != null && getGameFragment().getView() != null) {
			getGameFragment().getView().setKeepScreenOn(false);
		}
		if (onlinePlayAgainDialog != null) {
			// the user is in the play again dialog, let him read the info
			return;

		}
		String message = getContext().getResources().getString(
				R.string.peer_left_the_game, getOpponentName());
		(new AlertDialog.Builder(getContext())).setMessage(message)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						leaveGame();
					}
				}).create().show();

	}

	public void leaveGame() {
		if (onlinePlayAgainDialog != null) {
			// let the play again dialog handle it
			return;
		}

		if (getGameFragment() != null) {
			getGameFragment().leaveGame();
		}
		getMenuFragment().leaveRoom();
	}

	public void playAgain() {
		final GameFragment gameFragment = getGameFragment();
		Game game = getGame();
		Player currentPlayer = game.getCurrentPlayer();
		game = new Game(game.getType(), game.getMode(), game.getBlackPlayer(),
				game.getWhitePlayer(), currentPlayer);
		setGame(game);

		gameFragment.startGame(null, false);

	}

	private void sendRank(short rank) {
		ByteBuffer buffer = ByteBuffer
				.allocate(GamesClient.MAX_RELIABLE_MESSAGE_LEN);
		buffer.put(MSG_RANK);
		buffer.putInt(rank);

		Games.RealTimeMultiplayer.sendReliableMessage(getHelper()
				.getApiClient(), new ReliableMessageSentCallback() {

			@Override
			public void onRealTimeMessageSent(int statusCode, int token,
					String recipientParticipantId) {
				if (statusCode == GamesClient.STATUS_OK) {
					// hmm...
				} else {
					// hmmm
				}
			}
		}, buffer.array(), getRoomId(), getOpponentId());
	}

	@Override
	public void onSignInSuccess(MainActivity theActivity) {
		// real time game is broken when onResumed, nothing to do here
		// setMainActivity(theActivity);
	}

	@Override
	public void onActivityResume(MainActivity theActivity) {
		// setMainActivity(theActivity);
		// (new AlertDialog.Builder(getContext()))
		// .setMessage(R.string.you_left_the_game)
		// .setNeutralButton(android.R.string.ok, new OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// leaveGame();
		// dialog.dismiss();
		// }
		// }).create().show();

	}

	@Override
	public void onSignInFailed(SherlockFragmentActivity mainActivity) {
		// Real time game, we already disconnected, this won't matter
	}

	@Override
	public void showSettings(Fragment fragment) {
		// show an abbreviated "settings"- notably the sound fx and
		// other immediate game play settings
		OnlineSettingsDialogFragment onlineSettingsFragment = new OnlineSettingsDialogFragment();
		onlineSettingsFragment.show(fragment.getChildFragmentManager(),
				"settings");
	}

	@Override
	public boolean shouldKeepScreenOn() {
		return true;
	}

	public void onlineMoveReceived(ByteBufferDebugger buffer) {
		AbstractMove move = AbstractMove.fromMessageBytes(buffer, getGame());
		State state = applyNonHumanMove(move);
		getGameFragment().animateMove(state.getLastMove(), state);
	}

	protected GameMode getGameMode() {
		return GameMode.ONLINE;
	}

	@Override
	protected String getMatchId() {
		return mRoomId;
	}

	public boolean isRanked() {
		return isRanked;
	}

	@Override
	public boolean iAmBlackPlayer() {
		return iAmBlack;
	}
}
