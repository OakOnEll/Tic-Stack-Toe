package com.oakonell.ticstacktoe.ui.game;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Participant;
import com.oakonell.ticstacktoe.Achievements;
import com.oakonell.ticstacktoe.Leaderboards;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.InvalidMoveException;
import com.oakonell.ticstacktoe.model.Move;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.State.Win;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;
import com.oakonell.utils.StringUtils;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.dragndrop.DragController;
import com.oakonell.utils.activity.dragndrop.DragLayer;
import com.oakonell.utils.activity.dragndrop.ImageDropTarget;

public class GameFragment extends SherlockFragment {
	private static final int NON_HUMAN_OPPONENT_HIGHLIGHT_MOVE_PAUSE_MS = 300;

	private ImageManager imgManager;

	private View xHeaderLayout;
	private View oHeaderLayout;
	private TextView xWins;
	private TextView oWins;
	private TextView draws;

	private TextView gameNumber;
	private TextView numMoves;

	private List<ImageDropTarget> buttons = new ArrayList<ImageDropTarget>();
	private WinOverlayView winOverlayView;

	private Game game;
	private ScoreCard score;

	private List<ChatMessage> messages = new ArrayList<ChatMessage>();
	private int numNewMessages;

	private ChatDialogFragment chatDialog;
	private MenuItem chatMenuItem;

	private boolean disableButtons;
	private View thinking;
	private TextView thinkingText;

	boolean exitOnResume;

	@Override
	public void onPause() {
		exitOnResume = game.getMode() == GameMode.ONLINE;
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (exitOnResume) {
			(new AlertDialog.Builder(getMainActivity()))
					.setMessage(R.string.you_left_the_game)
					.setNeutralButton(android.R.string.ok,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									leaveGame();
									dialog.dismiss();
								}
							}).create().show();
		}
		final FragmentActivity activity = getActivity();
		// adjust the width or height to make sure the board is a square
		activity.findViewById(R.id.grid_container).getViewTreeObserver()
				.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						View squareView = activity
								.findViewById(R.id.grid_container);
						if (squareView == null) {
							// We get this when we are leaving the game?
							return;
						}
						LayoutParams layout = squareView.getLayoutParams();
						int min = Math.min(squareView.getWidth(),
								squareView.getHeight());
						layout.height = min;
						layout.width = min;
						squareView.setLayoutParams(layout);
						// squareView.getViewTreeObserver()
						// .removeGlobalOnLayoutListener(this);

						LayoutParams params = winOverlayView.getLayoutParams();
						params.height = layout.height;
						params.width = layout.width;
						winOverlayView.setLayoutParams(params);
					}
				});
	}

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

	private void invalidateMenu() {
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
		boolean isOnline = getMainActivity().getRoomListener() != null;
		chatMenuItem.setVisible(isOnline);
		if (!isOnline) {
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
			if (game.getMode() == GameMode.ONLINE) {

				// show an abbreviated "settings"- notably the sound fx and
				// other immediate game play settings
				OnlineSettingsDialogFragment onlineSettingsFragment = new OnlineSettingsDialogFragment();
				onlineSettingsFragment.show(getChildFragmentManager(),
						"settings");
				return true;
			}
			// create special intent
			Intent prefIntent = new Intent(getActivity(),
					SettingsActivity.class);

			GameHelper helper = getMainActivity().getGameHelper();
			Info info = null;
			TicStackToe app = (TicStackToe) getActivity().getApplication();
			if (helper.isSignedIn()) {
				info = new Info(helper);
			}
			app.setDevelopInfo(info);
			// ugh.. does going to preferences leave the room!?
			getActivity().startActivityForResult(prefIntent,
					MainActivity.RC_UNUSED);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openChatDialog() {
		getMainActivity().getRoomListener().sendInChat(true);
		chatDialog = new ChatDialogFragment();
		chatDialog.initialize(this, messages, getMainActivity()
				.getRoomListener().getMe(), getMainActivity().getRoomListener()
				.getOpponentName());
		chatDialog.show(getChildFragmentManager(), "chat");
	}

	public void startGame(Game game, ScoreCard score) {
		this.score = score;
		this.game = game;
		configureNonLocalProgresses();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_game, container, false);
		view.setKeepScreenOn(game.getMode() == GameMode.ONLINE);

		DragLayer mDragLayer = (DragLayer) view.findViewById(R.id.drag_layer);
		DragController mDragController;
		mDragController = new DragController(getActivity());
		mDragLayer.setDragController(mDragController);
		mDragController.setDragListener(mDragLayer);

		// TODO
		// mDragLayer.addTarget(trashCan);

		// TODO
		// mDragController.startDrag(v, dragSource, dragSource,
		// DragController.DRAG_ACTION_MOVE);

		invalidateMenu();
		setHasOptionsMenu(true);
		thinkingText = (TextView) view.findViewById(R.id.thinking_text);
		if (game.getMode() != GameMode.PASS_N_PLAY) {
			thinkingText.setText(getResources().getString(
					R.string.opponent_is_thinking,
					game.getNonLocalPlayer().getName()));
		}
		thinking = view.findViewById(R.id.thinking);
		configureNonLocalProgresses();

		imgManager = ImageManager.create(getMainActivity());

		TextView blackName = (TextView) view.findViewById(R.id.blackName);
		blackName.setText(game.getBlackPlayer().getName());
		TextView whiteName = (TextView) view.findViewById(R.id.whiteName);
		whiteName.setText(game.getWhitePlayer().getName());

		ImageView blackImage = (ImageView) view.findViewById(R.id.black_back);
		ImageView whiteImage = (ImageView) view.findViewById(R.id.white_back);

		game.getBlackPlayer().updatePlayerImage(imgManager, blackImage);
		game.getWhitePlayer().updatePlayerImage(imgManager, whiteImage);

		xHeaderLayout = view.findViewById(R.id.black_name_layout);
		oHeaderLayout = view.findViewById(R.id.white_name_layout);

		winOverlayView = (WinOverlayView) view.findViewById(R.id.win_overlay);
		winOverlayView.setBoardSize(game.getBoard().getSize());

		addButtonClickListeners(view);

		xWins = (TextView) view.findViewById(R.id.num_black_wins);
		oWins = (TextView) view.findViewById(R.id.num_white_wins);
		draws = (TextView) view.findViewById(R.id.num_draws);

		gameNumber = (TextView) view.findViewById(R.id.game_number);
		gameNumber.setText("" + score.getTotalGames());

		numMoves = (TextView) view.findViewById(R.id.num_moves);

		View num_games_container = view.findViewById(R.id.num_games_container);
		num_games_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showGameStats();
			}
		});

		if (savedInstanceState != null) {
			updateHeader(true);
		}

		return view;
	}

	private void addButtonClickListeners(View view) {
		int size = game.getBoard().getSize();
		ImageDropTarget button = (ImageDropTarget) view
				.findViewById(R.id.button_r1c1);
		configureUICell(button, new Cell(0, 0));

		button = (ImageDropTarget) view.findViewById(R.id.button_r1c2);
		configureUICell(button, new Cell(0, 1));

		button = (ImageDropTarget) view.findViewById(R.id.button_r1c3);
		configureUICell(button, new Cell(0, 2));

		button = (ImageDropTarget) view.findViewById(R.id.button_r1c4);
		if (size > 3) {
			configureUICell(button, new Cell(0, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (ImageDropTarget) view.findViewById(R.id.button_r1c5);
			configureUICell(button, new Cell(0, 4));
			button.setVisibility(View.VISIBLE);
		}

		// row2
		button = (ImageDropTarget) view.findViewById(R.id.button_r2c1);
		configureUICell(button, new Cell(1, 0));

		button = (ImageDropTarget) view.findViewById(R.id.button_r2c2);
		configureUICell(button, new Cell(1, 1));

		button = (ImageDropTarget) view.findViewById(R.id.button_r2c3);
		configureUICell(button, new Cell(1, 2));

		if (size > 3) {
			button = (ImageDropTarget) view.findViewById(R.id.button_r2c4);
			configureUICell(button, new Cell(1, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (ImageDropTarget) view.findViewById(R.id.button_r2c5);
			configureUICell(button, new Cell(1, 4));
			button.setVisibility(View.VISIBLE);
		}
		// row3
		button = (ImageDropTarget) view.findViewById(R.id.button_r3c1);
		configureUICell(button, new Cell(2, 0));

		button = (ImageDropTarget) view.findViewById(R.id.button_r3c2);
		configureUICell(button, new Cell(2, 1));

		button = (ImageDropTarget) view.findViewById(R.id.button_r3c3);
		configureUICell(button, new Cell(2, 2));

		if (size > 3) {
			button = (ImageDropTarget) view.findViewById(R.id.button_r3c4);
			configureUICell(button, new Cell(2, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (ImageDropTarget) view.findViewById(R.id.button_r3c5);
			configureUICell(button, new Cell(2, 4));
			button.setVisibility(View.VISIBLE);
		}

		if (size > 3) {
			view.findViewById(R.id.button_row4).setVisibility(View.VISIBLE);
			// row4
			button = (ImageDropTarget) view.findViewById(R.id.button_r4c1);
			configureUICell(button, new Cell(3, 0));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r4c2);
			configureUICell(button, new Cell(3, 1));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r4c3);
			configureUICell(button, new Cell(3, 2));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r4c4);
			configureUICell(button, new Cell(3, 3));
			button.setVisibility(View.VISIBLE);

			if (size > 4) {
				button = (ImageDropTarget) view.findViewById(R.id.button_r4c5);
				configureUICell(button, new Cell(3, 4));
				button.setVisibility(View.VISIBLE);
			}
		}

		if (size > 4) {
			view.findViewById(R.id.button_row5).setVisibility(View.VISIBLE);
			// row5
			button = (ImageDropTarget) view.findViewById(R.id.button_r5c1);
			configureUICell(button, new Cell(4, 0));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r5c2);
			configureUICell(button, new Cell(4, 1));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r5c3);
			configureUICell(button, new Cell(4, 2));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r5c4);
			configureUICell(button, new Cell(4, 3));
			button.setVisibility(View.VISIBLE);

			button = (ImageDropTarget) view.findViewById(R.id.button_r5c5);
			configureUICell(button, new Cell(4, 4));
			button.setVisibility(View.VISIBLE);
		}
	}

	private void configureUICell(ImageDropTarget button, Cell cell) {
		button.setPadding(5, 5, 5, 5);
//		if (cell.getX() % 2 == 0 ) {
//			if (cell.getY() % 2 ==0 ) {
//			} else {
//				button.setBackgroundColor(getResources().getColor(
//						android.R.color.holo_green_light));								
//			}
//		} else {
//			if (cell.getY() % 2 ==0 ) {
//				button.setBackgroundColor(getResources().getColor(
//						android.R.color.holo_green_light));				
//			} else {
//				
//			}
//		}
		button.setOnClickListener(new ButtonPressListener(cell));
		buttons.add(button);
	}

	private void showGameStats() {
		GameStatDialogFragment dialog = new GameStatDialogFragment();
		dialog.initialize(this, game, score);
		dialog.show(getChildFragmentManager(), "stats");
	}

	private final class ButtonPressListener implements View.OnClickListener {
		private final Cell cell;

		public ButtonPressListener(Cell cell) {
			this.cell = cell;
		}

		@Override
		public void onClick(View view) {
			if (disableButtons) {
				return;
			}
			if (!game.getCurrentPlayer().getStrategy().isHuman()) {
				// ignore button clicks if the current player is not a human
				return;
			}
			// TODO button click is wrong event here... needs to be drop event
			// Piece marker = game.getMarkerToPlay();
			//
			// boolean wasValid = makeAndDisplayMove(marker, cell);
			// if (!wasValid)
			// return;
			//
			// // send move to opponent
			// RoomListener appListener = getMainActivity().getRoomListener();
			// if (appListener != null) {
			// appListener.sendMove(marker, cell);
			// }

		}

	}

	private void updateHeader(boolean animateMarker) {
		Player player = game.getCurrentPlayer();
		if (player.isBlack()) {
			xHeaderLayout.setBackgroundResource(R.drawable.current_player);
			oHeaderLayout.setBackgroundResource(R.drawable.inactive_player);

			highlightPlayerTurn(xHeaderLayout, oHeaderLayout);

		} else {
			oHeaderLayout.setBackgroundResource(R.drawable.current_player);
			xHeaderLayout.setBackgroundResource(R.drawable.inactive_player);

			highlightPlayerTurn(oHeaderLayout, xHeaderLayout);
		}

		updateGameStatDisplay();
	}

	private void updateGameStatDisplay() {
		numMoves.setText("" + game.getNumberOfMoves());
		xWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, score.getBlackWins(),
				score.getBlackWins()));
		oWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, score.getWhiteWins(),
				score.getWhiteWins()));
		draws.setText("" + score.getDraws());
		gameNumber.setText("" + score.getTotalGames());
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void highlightPlayerTurn(View highlight, View dimmed) {
		float notTurnAlpha = 0.25f;
		if (Utils.hasHoneycomb()) {
			highlight.setAlpha(1f);
			dimmed.setAlpha(notTurnAlpha);
		}
	}

	private OnlinePlayAgainFragment onlinePlayAgainDialog;

	public boolean makeAndDisplayMove(Move move) {
		State outcome = null;
		try {
			outcome = game.placePlayerPiece(move.getTargetCell());
		} catch (InvalidMoveException e) {
			getMainActivity().playSound(Sounds.INVALID_MOVE);
			int messageId = e.getErrorResourceId();
			Toast toast = Toast.makeText(getActivity(), messageId,
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return false;
		}
		// TODO diff sounds for diff size piece/color..
		if (move.getPlayer().isBlack()) {
			getMainActivity().playSound(Sounds.PLAY_X);
		} else {
			getMainActivity().playSound(Sounds.PLAY_O);
		}
		privateMakeMove(move, outcome);
		return true;
	}

	private void privateMakeMove(Move move, State outcome) {
		ImageDropTarget cellButton = findButtonFor(move.getTargetCell());
		animateMove(move.getPlayedMarker(), cellButton, outcome);
	}

	private void animateMove(final Piece marker,
			final ImageDropTarget cellButton, final State outcome) {
		boolean animate = true;
		if (!animate) {
			postMove(marker, cellButton, outcome);
			return;
		}
		disableButtons = true;
		// experimenting with animations...
		// create set of animations
		AnimationSet replaceAnimation = new AnimationSet(false);
		// animations should be applied on the finish line
		replaceAnimation.setFillAfter(false);

		// float xScale = ((float) cellButton.getWidth())
		// / markerToPlayView.getWidth();
		// float yScale = ((float) cellButton.getHeight())
		// / markerToPlayView.getHeight();
		//
		// View grid_container =
		// getActivity().findViewById(R.id.grid_container);
		// int xChange = cellButton.getLeft() - markerToPlayView.getLeft();
		// int yChange = cellButton.getTop()
		// + ((View) cellButton.getParent()).getTop()
		// + grid_container.getTop() - markerToPlayView.getTop();
		//
		// // create scale animation
		// ScaleAnimation scale = new ScaleAnimation(1.0f, xScale, 1.0f,
		// yScale);
		// scale.setDuration(1000);
		//
		// // create translation animation
		// TranslateAnimation trans = new TranslateAnimation(0, xChange, 0,
		// yChange);
		// trans.setDuration(1000);
		//
		// // add new animations to the set
		// replaceAnimation.addAnimation(scale);
		// replaceAnimation.addAnimation(trans);
		//
		// AlphaAnimation fade = new AlphaAnimation(1, 0);
		// fade.setStartOffset(800);
		// fade.setDuration(200);
		// replaceAnimation.addAnimation(fade);
		//
		// Interpolator interpolator = new AnticipateInterpolator();
		// replaceAnimation.setInterpolator(interpolator);
		//
		// replaceAnimation.setAnimationListener(new AnimationListener() {
		// @Override
		// public void onAnimationStart(Animation animation) {
		// // empty
		// }
		//
		// @Override
		// public void onAnimationRepeat(Animation animation) {
		// // empty
		// }
		//
		// @Override
		// public void onAnimationEnd(Animation animation) {
		// markerToPlayView.setImageDrawable(null);
		// postMove(marker, cellButton, outcome);
		// }
		//
		// });
		//
		// // start our animation
		// markerToPlayView.startAnimation(replaceAnimation);
	}

	private void postMove(Piece marker, ImageDropTarget cellButton,
			State outcome) {
		if (marker == Piece.EMPTY) {
			cellButton.setImageDrawable(null);
		} else {
			int resId = marker.isBlack() ? R.drawable.filled_circle_icon_12972
					: R.drawable.hollowed_circle_icon_12971;
			cellButton.setImageResource(resId);
		}
		if (outcome.isOver()) {
			updateGameStatDisplay();
			endGame(outcome);
		} else {
			evaluateInGameAchievements(outcome);
			updateHeader(true);
			acceptMove();
		}
	}

	private void endGame(State outcome) {
		numMoves.setText("" + game.getNumberOfMoves());
		evaluateGameEndAchievements(outcome);
		evaluateLeaderboards(outcome);
		Player winner = outcome.getWinner();
		if (winner != null) {
			winOverlayView.clearWins();
			score.incrementScore(winner);
			for (Win each : outcome.getWins()) {
				winOverlayView.addWinStyle(each.getWinStyle());
			}
			winOverlayView.invalidate();

			if (game.getMode() == GameMode.PASS_N_PLAY) {
				getMainActivity().playSound(Sounds.GAME_WON);
			} else {
				// the player either won or lost
				if (winner.equals(game.getLocalPlayer())) {
					getMainActivity().playSound(Sounds.GAME_WON);
				} else {
					getMainActivity().playSound(Sounds.GAME_LOST);
				}
			}

			String title = getString(R.string.player_won, winner.getName());

			promptToPlayAgain(title);
		} else {
			score.incrementScore(null);
			getMainActivity().playSound(Sounds.GAME_DRAW);
			String title = getString(R.string.draw);

			promptToPlayAgain(title);
		}
	}

	private void promptToPlayAgain(String title) {
		if (game.getMode() == GameMode.ONLINE) {
			onlinePlayAgainDialog = new OnlinePlayAgainFragment();
			onlinePlayAgainDialog.initialize(this, getMainActivity()
					.getRoomListener().getOpponentName(), title);
			onlinePlayAgainDialog.show(getChildFragmentManager(), "playAgain");
			// TODO wire up the play again / not play again message handling via
			// the dialog
			return;
		}

		OnClickListener cancelListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				leaveGame();
			}

		};
		OnClickListener playAgainListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				playAgain();
			}

		};

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(R.string.play_again);
		builder.setCancelable(false);

		builder.setNegativeButton(R.string.no, cancelListener);
		builder.setPositiveButton(R.string.yes, playAgainListener);

		AlertDialog dialog = builder.create();

		dialog.show();
	}

	private void acceptMove() {
		final PlayerStrategy currentStrategy = game.getCurrentPlayer()
				.getStrategy();
		if (currentStrategy.isHuman()) {
			disableButtons = false;
			// let the buttons be pressed for a human interaction
			return;
		}
		// show a thinking/progress icon, suitable for network play and ai
		// thinking..
		configureNonLocalProgresses();
		if (!currentStrategy.isAI()) {
			return;
		}

		AsyncTask<Void, Void, Move> aiMove = new AsyncTask<Void, Void, Move>() {
			@Override
			protected Move doInBackground(Void... params) {
				return currentStrategy.move(game);
			}

			@Override
			protected void onPostExecute(final Move move) {
				highlightAndMakeMove(move);
			}
		};
		aiMove.execute((Void) null);
	}

	private void configureNonLocalProgresses() {
		if (thinking == null || thinkingText == null) {
			// safety for when start called before activity is created
			return;
		}
		PlayerStrategy strategy = game.getCurrentPlayer().getStrategy();
		if (strategy.isHuman()) {
			thinking.setVisibility(View.GONE);
			disableButtons = false;
			return;
		}
		disableButtons = true;
		thinking.setVisibility(View.VISIBLE);
		thinkingText.setVisibility(View.VISIBLE);

	}

	public void onlineMakeMove(final Move move) {
		highlightAndMakeMove(move);
	}

	public void highlightAndMakeMove(final Move move) {
		// hide the progress icon
		thinking.setVisibility(View.GONE);
		// delay and highlight the move so the human player has a
		// chance to see it
		// TODO incorporate source cell...
		final ImageDropTarget cellButton = findButtonFor(move.getTargetCell());
		final Drawable originalBackGround = cellButton.getBackground();
		cellButton.setBackgroundColor(getResources().getColor(
				R.color.holo_blue_light));
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				cellButton.setBackgroundDrawable(originalBackGround);
				makeAndDisplayMove(move);
			}
		}, NON_HUMAN_OPPONENT_HIGHLIGHT_MOVE_PAUSE_MS);
	}

	private ImageDropTarget findButtonFor(Cell cell) {
		int id;
		int x = cell.getX();
		int y = cell.getY();
		if (x == 0) {
			if (y == 0) {
				id = R.id.button_r1c1;
			} else if (y == 1) {
				id = R.id.button_r1c2;
			} else if (y == 2) {
				id = R.id.button_r1c3;
			} else if (y == 3) {
				id = R.id.button_r1c4;
			} else if (y == 4) {
				id = R.id.button_r1c5;
			} else {
				throw new RuntimeException("Invalid cell");
			}
		} else if (x == 1) {
			if (y == 0) {
				id = R.id.button_r2c1;
			} else if (y == 1) {
				id = R.id.button_r2c2;
			} else if (y == 2) {
				id = R.id.button_r2c3;
			} else if (y == 3) {
				id = R.id.button_r2c4;
			} else if (y == 4) {
				id = R.id.button_r2c5;
			} else {
				throw new RuntimeException("Invalid cell");
			}
		} else if (x == 2) {
			if (y == 0) {
				id = R.id.button_r3c1;
			} else if (y == 1) {
				id = R.id.button_r3c2;
			} else if (y == 2) {
				id = R.id.button_r3c3;
			} else if (y == 3) {
				id = R.id.button_r3c4;
			} else if (y == 4) {
				id = R.id.button_r3c5;
			} else {
				throw new RuntimeException("Invalid cell");
			}
		} else if (x == 3) {
			if (y == 0) {
				id = R.id.button_r4c1;
			} else if (y == 1) {
				id = R.id.button_r4c2;
			} else if (y == 2) {
				id = R.id.button_r4c3;
			} else if (y == 3) {
				id = R.id.button_r4c4;
			} else if (y == 4) {
				id = R.id.button_r4c5;
			} else {
				throw new RuntimeException("Invalid cell");
			}
		} else if (x == 4) {
			if (y == 0) {
				id = R.id.button_r5c1;
			} else if (y == 1) {
				id = R.id.button_r5c2;
			} else if (y == 2) {
				id = R.id.button_r5c3;
			} else if (y == 3) {
				id = R.id.button_r5c4;
			} else if (y == 4) {
				id = R.id.button_r5c5;
			} else {
				throw new RuntimeException("Invalid cell");
			}
		} else {
			throw new RuntimeException("Invalid cell");
		}
		return (ImageDropTarget) getActivity().findViewById(id);
	}

	private void evaluateGameEndAchievements(State outcome) {
		TicStackToe application = ((TicStackToe) getActivity().getApplication());

		Achievements achievements = application.getAchievements();
		achievements.testAndSetForGameEndAchievements(getMainActivity()
				.getGameHelper(), getActivity(), game, outcome);
	}

	private void evaluateInGameAchievements(State outcome) {
		TicStackToe application = ((TicStackToe) getActivity().getApplication());

		Achievements achievements = application.getAchievements();
		achievements.testAndSetForInGameAchievements(getMainActivity()
				.getGameHelper(), getActivity(), game, outcome);
	}

	private void evaluateLeaderboards(State outcome) {
		TicStackToe application = ((TicStackToe) getActivity().getApplication());

		Leaderboards leaderboards = application.getLeaderboards();
		leaderboards.submitGame(getMainActivity().getGameHelper(),
				getActivity(), game, outcome, score);

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
		getMainActivity().getRoomListener().sendInChat(false);
		chatDialog = null;
		numNewMessages = 0;
		invalidateMenu();
	}

	public void leaveGame() {
		if (onlinePlayAgainDialog != null) {
			// let the play again dialog handle it
			return;
		}

		onGameStatsClose = new Runnable() {
			@Override
			public void run() {
				getMainActivity().getSupportFragmentManager().popBackStack();
				getMainActivity().gameEnded();
			}
		};
		showGameStats();
	}

	public void playAgain() {
		Player currentPlayer = game.getCurrentPlayer();
		game = new Game(game.getBoard().getSize(), game.getMode(),
				currentPlayer, currentPlayer.opponent());
		updateHeader(true);
		winOverlayView.clearWins();
		winOverlayView.invalidate();
		for (ImageDropTarget each : buttons) {
			each.setImageDrawable(null);
		}
		acceptMove();
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
		getMainActivity().getRoomListener().restartGame();
	}

	private boolean opponentLeftIsShowing;

	public void opponentLeft() {
		getView().setKeepScreenOn(false);
		if (onlinePlayAgainDialog != null) {
			// the user is in the play again dialog, let him read the info
			return;

		}
		opponentLeftIsShowing = true;
		final MainActivity activity = getMainActivity();
		String message = activity.getResources().getString(
				R.string.peer_left_the_game,
				getMainActivity().getRoomListener().getOpponentName());
		(new AlertDialog.Builder(getMainActivity())).setMessage(message)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						leaveGame();
					}
				}).create().show();

	}

	public void onDisconnectedFromRoom() {
		if (onlinePlayAgainDialog != null || opponentLeftIsShowing) {
			// the user is in the play again dialog, let him read the info
			return;

		}
	}

	private boolean opponentInChat = false;

	public void opponentInChat() {
		opponentInChat = true;
		// show "animated" menu icon
		invalidateMenu();

		// update the display text
		thinkingText.setText(getResources().getString(
				R.string.opponent_is_in_chat,
				getMainActivity().getRoomListener().getOpponentName()));
	}

	public void opponentClosedChat() {
		// stop animated menu icon
		opponentInChat = false;
		invalidateMenu();

		// update the display text
		thinkingText.setText(getResources().getString(
				R.string.opponent_is_thinking,
				getMainActivity().getRoomListener().getOpponentName()));
	}

	private boolean helpShown = false;

	public void gameHelpClosed() {
		updateHeader(!helpShown);
		helpShown = true;
	}

	private Runnable onGameStatsClose;

	public void gameStatsClosed() {
		if (onGameStatsClose != null) {
			onGameStatsClose.run();
		}
	}
}
