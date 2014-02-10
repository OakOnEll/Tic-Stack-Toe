package com.oakonell.ticstacktoe.ui.game;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.Achievements;
import com.oakonell.ticstacktoe.GameListener;
import com.oakonell.ticstacktoe.Leaderboards;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.ByteBufferDebugger;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.InvalidMoveException;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.State.Win;
import com.oakonell.ticstacktoe.settings.SettingsActivity;
import com.oakonell.ticstacktoe.ui.SquareRelativeLayoutView;
import com.oakonell.ticstacktoe.ui.SquareRelativeLayoutView.OnMeasureDependent;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.dragndrop.DragConfig;
import com.oakonell.utils.activity.dragndrop.DragController;
import com.oakonell.utils.activity.dragndrop.DragLayer;
import com.oakonell.utils.activity.dragndrop.DragSource;
import com.oakonell.utils.activity.dragndrop.DragView;
import com.oakonell.utils.activity.dragndrop.ImageDropTarget;
import com.oakonell.utils.activity.dragndrop.OnDragListener;
import com.oakonell.utils.activity.dragndrop.OnDropListener;

public class GameFragment extends AbstractGameFragment {

	private DragController mDragController;
	private DragLayer mDragLayer;

	private ImageManager imgManager;

	private View blackHeaderLayout;
	private View whiteHeaderLayout;
	private TextView blackWins;
	private TextView whiteWins;

	private TextView gameNumber;
	private TextView numMoves;

	private List<ImageDropTarget> buttons = new ArrayList<ImageDropTarget>();
	private WinOverlayView winOverlayView;

	private Game game;
	private ScoreCard score;

	private boolean disableButtons;

	private WindowManager mWindowManager;

	// private Cell firstPickCell;

	@Override
	public void onPause() {
		// TODO write the game state to the server?
		mWindowManager = null;
		super.onPause();
	}

	protected boolean isOnline() {
		return game.getMode() == GameMode.ONLINE;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (getMainActivity().getRoomListener() != null) {
			getMainActivity().getRoomListener().onFragmentResume();
		}
		Log.i("GameFragment", "onResume");

		if (inOnResume != null) {
			inOnResume.run();
		}
	}

	Runnable inOnResume;
	Runnable inOnCreate;

	public void startGame(final Game game, final ScoreCard score,
			final String waitingText, final boolean showMove) {

		inOnCreate = new Runnable() {
			@Override
			public void run() {
				boolean undoAndAnimateMove = false;
				final State endState = game.getBoard().getState();
				AbstractMove move = null;
				if (showMove) {
					move = game.getBoard().getState().getLastMove();
					undoAndAnimateMove = move != null;
				}
				if (undoAndAnimateMove) {
					GameFragment.this.score = score;
					GameFragment.this.game = game;

					game.undo(move);

					configureNonLocalProgresses();
					if (getView() != null) {
						addPieceListeners(getView());
					}
					updateHeader(getView());

					winOverlayView.clearWins();
					winOverlayView.invalidate();

					// update the
					final AbstractMove theMove = move;
					inOnResume = new Runnable() {
						@Override
						public void run() {
							inOnResume = null;
							Handler handler = new Handler();
							handler.post(new Runnable() {
								@Override
								public void run() {
									highlightAndMakeMove(theMove);
								}
							});
						}
					};
					if (getView() != null) {
						inOnResume.run();
					}
				} else {
					GameFragment.this.score = score;
					GameFragment.this.game = game;
					configureNonLocalProgresses();
					if (getView() != null) {
						addPieceListeners(getView());
					}
					updateHeader(getView());

					winOverlayView.clearWins();
					winOverlayView.invalidate();

					postMove(game.getBoard().getState(), showMove);
				}
				inOnCreate = null;
				if (waitingText != null) {
					setThinkingText(waitingText);
				} else {
					resetThinkingText();
					hideStatusText();
				}

			}
		};
		if (getMainActivity() != null) {
			inOnCreate.run();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// if (useUpNavigation()) {
			// NavUtils.navigateUpFromSameTask(this);
			// } else {
			leaveGame();
			// }
			return true;
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
		return false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_game, container,
				false);

		// Listen for changes in the back stack
		getMainActivity().getSupportFragmentManager()
				.addOnBackStackChangedListener(
						new OnBackStackChangedListener() {
							@Override
							public void onBackStackChanged() {
								configureDisplayHomeUp();
							}
						});
		// Handle when activity is recreated like on orientation Change
		configureDisplayHomeUp();

		mDragLayer = (DragLayer) view.findViewById(R.id.drag_layer);
		mDragController = new DragController(getActivity());
		mDragLayer.setDragController(mDragController);
		mDragController.setDragListener(mDragLayer);

		invalidateMenu();
		setHasOptionsMenu(true);

		imgManager = ImageManager.create(getMainActivity());

		blackHeaderLayout = view.findViewById(R.id.black_name_layout);
		whiteHeaderLayout = view.findViewById(R.id.white_name_layout);

		winOverlayView = (WinOverlayView) view.findViewById(R.id.win_overlay);

		blackWins = (TextView) view.findViewById(R.id.num_black_wins);
		whiteWins = (TextView) view.findViewById(R.id.num_white_wins);
		// draws = (TextView) view.findViewById(R.id.num_draws);

		gameNumber = (TextView) view.findViewById(R.id.game_number);

		numMoves = (TextView) view.findViewById(R.id.num_moves);

		View num_games_container = view.findViewById(R.id.num_games_container);
		num_games_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showGameStats();
			}
		});

		if (inOnCreate != null) {
			inOnCreate.run();
		}

		gameNumber.setText("" + score.getTotalGames());
		winOverlayView.setBoardSize(game.getBoard().getSize());
		addPieceListeners(view);

		view.setKeepScreenOn(game.getMode() == GameMode.ONLINE);
		if (game.getBoard().getSize() == 3) {
			((ViewGroup) view.findViewById(R.id.grid_container))
					.setBackgroundResource(R.drawable.wood_grid_3x3);
		}
		if (game.getMode() != GameMode.PASS_N_PLAY) {
			initThinkingText(view, game.getNonLocalPlayer().getName());
			setOpponentThinking();
		} else {
			initThinkingText(view, null);
		}
		configureNonLocalProgresses();

		updateHeader(view);

		PieceStackImageView stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack1);
		stackView
				.setOnMeasureDependent(new PieceStackImageView.OnMeasureDependent() {
					@Override
					public void onMeasureCalled(
							PieceStackImageView squareRelativeLayoutView,
							int size, int origWidth, int origHeight) {
						pieceStackHeight = origHeight;
						resizeBoardAndStacks(view);

					}
				});

		squareView = (SquareRelativeLayoutView) view
				.findViewById(R.id.grid_container);
		squareView.setOnMeasureDependent(new OnMeasureDependent() {
			@Override
			public void onMeasureCalled(
					SquareRelativeLayoutView squareRelativeLayoutView,
					int size, int origWidth, int origHeight) {
				boardHeight = origHeight;
				boardWidth = origWidth;
				boardSize = size;
				resizeBoardAndStacks(view);
			}
		});

		return view;
	}

	protected void resizeBoardAndStacks(View view) {
		if (wasResized) {
			return;
		}
		if (boardSize == 0 || pieceStackHeight == 0) {
			return;
		}
		wasResized = true;

		int boardSize = game.getBoard().getSize();
		int size = (boardHeight + 2 * pieceStackHeight) / (boardSize + 2);
		int newBoardPixSize = size * boardSize;
		Log.i("GameFragment", "resizing: Board height = " + boardHeight
				+ ", piece height = " + pieceStackHeight
				+ ", calculated single piece size =" + size + ", new board = "
				+ newBoardPixSize);

		resizePlayerStacks(getView(), size);

		LayoutParams layoutParams = squareView.getLayoutParams();
		layoutParams.height = newBoardPixSize;
		squareView.requestLayout();

	}

	boolean wasResized;
	int boardWidth;
	int boardHeight;
	int boardSize;
	int pieceStackHeight;

	private void configureDisplayHomeUp() {
		if (getMainActivity() == null)
			return;
		getMainActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	private void resizePlayerStacks(final View view, int size) {
		Log.i("GameFragment", "resizePlayerStacks, size = " + size);

		PieceStackImageView stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack1);
		resize(stackView, size, size);
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack2);
		resize(stackView, size, size);
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack3);
		resize(stackView, size, size);

		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack1);
		resize(stackView, size, size);
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack2);
		resize(stackView, size, size);
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack3);
		resize(stackView, size, size);

		View whitePieces = view.findViewById(R.id.whitePieceLayout);
		LayoutParams layoutParams = whitePieces.getLayoutParams();
		layoutParams.height = size;

		View blackPieces = view.findViewById(R.id.blackPieceLayout);
		layoutParams = blackPieces.getLayoutParams();
		layoutParams.height = size;

	}

	private void resize(View view, int height, int width) {
		LayoutParams layoutParams = view.getLayoutParams();
		layoutParams.height = height;
		layoutParams.width = width;
		view.requestLayout();
	}

	private void addPieceListeners(View view) {
		int size = game.getBoard().getSize();
		BoardPieceStackImageView button = (BoardPieceStackImageView) view
				.findViewById(R.id.button_r1c1);
		configureUICell(button, new Cell(0, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c2);
		configureUICell(button, new Cell(0, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c3);
		configureUICell(button, new Cell(0, 2));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c4);
		if (size > 3) {
			configureUICell(button, new Cell(0, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r1c5);
			configureUICell(button, new Cell(0, 4));
			button.setVisibility(View.VISIBLE);
		}

		// row2
		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c1);
		configureUICell(button, new Cell(1, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c2);
		configureUICell(button, new Cell(1, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c3);
		configureUICell(button, new Cell(1, 2));

		if (size > 3) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r2c4);
			configureUICell(button, new Cell(1, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r2c5);
			configureUICell(button, new Cell(1, 4));
			button.setVisibility(View.VISIBLE);
		}
		// row3
		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c1);
		configureUICell(button, new Cell(2, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c2);
		configureUICell(button, new Cell(2, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c3);
		configureUICell(button, new Cell(2, 2));

		if (size > 3) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r3c4);
			configureUICell(button, new Cell(2, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r3c5);
			configureUICell(button, new Cell(2, 4));
			button.setVisibility(View.VISIBLE);
		}

		if (size > 3) {
			view.findViewById(R.id.button_row4).setVisibility(View.VISIBLE);
			// row4
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c1);
			configureUICell(button, new Cell(3, 0));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c2);
			configureUICell(button, new Cell(3, 1));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c3);
			configureUICell(button, new Cell(3, 2));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c4);
			configureUICell(button, new Cell(3, 3));
			button.setVisibility(View.VISIBLE);

			if (size > 4) {
				button = (BoardPieceStackImageView) view
						.findViewById(R.id.button_r4c5);
				configureUICell(button, new Cell(3, 4));
				button.setVisibility(View.VISIBLE);
			}
		}

		if (size > 4) {
			view.findViewById(R.id.button_row5).setVisibility(View.VISIBLE);
			// row5
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c1);
			configureUICell(button, new Cell(4, 0));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c2);
			configureUICell(button, new Cell(4, 1));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c3);
			configureUICell(button, new Cell(4, 2));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c4);
			configureUICell(button, new Cell(4, 3));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c5);
			configureUICell(button, new Cell(4, 4));
			button.setVisibility(View.VISIBLE);
		}

		// handle the player stacks
		PieceStackImageView stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack1);
		configurePlayerStack(stackView, 0, game.getBlackPlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack2);
		configurePlayerStack(stackView, 1, game.getBlackPlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack3);
		if (game.getType().getNumberOfStacks() > 2) {
			configurePlayerStack(stackView, 2, game.getBlackPlayer());
		} else {
			stackView.setVisibility(View.GONE);
		}

		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack1);
		configurePlayerStack(stackView, 0, game.getWhitePlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack2);
		configurePlayerStack(stackView, 1, game.getWhitePlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack3);
		if (game.getType().getNumberOfStacks() > 2) {
			configurePlayerStack(stackView, 2, game.getWhitePlayer());
		} else {
			stackView.setVisibility(View.GONE);
		}
	}

	private void updatePlayerStack(Player player, int stackNum) {
		List<PieceStack> playerPieces = player.getPlayerPieces();
		final PieceStack pieceStack = playerPieces.get(stackNum);

		PieceStackImageView stackView = getPlayerStackView(player, stackNum);
		updatePlayerStack(pieceStack, stackView);

	}

	private PieceStackImageView getPlayerStackView(Player player, int stackNum) {
		PieceStackImageView stackView = null;
		if (player.isBlack()) {
			if (stackNum == 0) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.black_piece_stack1);
			} else if (stackNum == 1) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.black_piece_stack2);
			} else if (stackNum == 2) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.black_piece_stack3);
			}
		} else {
			if (stackNum == 0) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.white_piece_stack1);
			} else if (stackNum == 1) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.white_piece_stack2);
			} else if (stackNum == 2) {
				stackView = (PieceStackImageView) getView().findViewById(
						R.id.white_piece_stack3);
			}
		}
		if (stackView == null) {
			throw new RuntimeException("Invalid player piece stack specified");
		}
		return stackView;
	}

	private void updatePlayerStack(final PieceStack pieceStack,
			PieceStackImageView stackView) {
		Piece topPiece = pieceStack.getTopPiece();
		if (topPiece == null) {
			stackView.setImageDrawable(null);
		} else {
			stackView.setImageResource(topPiece.getImageResourceId());
		}
	}

	private void configurePlayerStack(final PieceStackImageView stackView,
			final int stackNum, final Player player) {
		List<PieceStack> playerPieces = player.getPlayerPieces();
		final PieceStack pieceStack = playerPieces.get(stackNum);

		Piece topPiece = pieceStack.getTopPiece();
		if (topPiece == null) {
			stackView.setImageDrawable(null);
		} else {
			stackView.setImageResource(topPiece.getImageResourceId());
		}

		final DragSource dragSource = new DragSource() {
			@Override
			public void setDragController(DragController dragger) {
			}

			@Override
			public void onDropCompleted(View target, boolean success) {
				if (!hadInvaldMove) {
					update();
				}
			}

			@Override
			public void onDropCanceled(DragView dragView) {
				Piece topPiece = pieceStack.getTopPiece();
				int resId = 0;
				// if it is 0... how?
				if (topPiece != null) {
					resId = topPiece.getImageResourceId();
				}
				animateInvalidMoveReturn(dragView, resId, stackView,
						new Runnable() {
							@Override
							public void run() {
								update();
							}
						});
			}

			@Override
			public boolean allowDrag() {
				// if we are in strict mode, and a board piece was touched, no
				// stack moves area allowed
				if (game.getFirstPickedCell() != null) {
					return false;
				}

				boolean isCurrentPlayersPiece = pieceStack.getTopPiece()
						.isBlack() == game.getCurrentPlayer().isBlack();
				if (!isCurrentPlayersPiece)
					return false;
				return pieceStack.getTopPiece() != null;
			}

			private void update() {
				updatePlayerStack(pieceStack, stackView);
			}
		};

		OnTouchListener onTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_DOWN)
					return false;

				if (disableButtons) {
					return false;
				}
				if (!game.getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a human
					return false;
				}

				// TODO depending on game rules, only allow the active stack to
				// be played
				Piece topPiece = pieceStack.getTopPiece();
				if (topPiece == null) {
					return false;
				}

				boolean isCurrentPlayersPiece = topPiece.isBlack() == game
						.getCurrentPlayer().isBlack();
				if (!isCurrentPlayersPiece) {
					return false;
				}

				if (!dragSource.allowDrag()) {
					return false;
				}

				// We are starting a drag. Let the DragController handle it.
				DragConfig dragConfig = new DragConfig();
				dragConfig.alpha = 255;
				dragConfig.drawSelected = false;
				dragConfig.vibrate = false;
				dragConfig.animationScale = 1.0f;

				OnDropMove newPieceOnDrop = new OnDropMove() {
					@Override
					public void postMove() {
						updatePlayerStack(pieceStack, stackView);
					}

					@Override
					public State droppedOn(Cell cell) {
						return game.placePlayerPiece(stackNum, cell);
					}

					@Override
					public boolean originatedFrom(Cell otherCell) {
						return false;
					}

					@Override
					public int getMovedImageResourceId() {
						return pieceStack.getTopPiece().getImageResourceId();
					}

					@Override
					public ImageView getSourceView() {
						return stackView;
					}

					@Override
					public void update() {
						postMove();
					}

				};

				mDragController.startDrag(v, dragSource, newPieceOnDrop,
						DragController.DRAG_ACTION_COPY, dragConfig);

				Piece nextPiece = pieceStack.peekNextPiece();
				if (nextPiece == null) {
					stackView.setImageDrawable(null);
				} else {
					stackView.setImageResource(nextPiece.getImageResourceId());
				}

				return true;
			}
		};
		stackView.setOnTouchListener(onTouchListener);

	}

	protected void animateInvalidMoveReturn(final DragView dragView,
			int movedResourceId, ImageView target, final Runnable runnable) {
		hadInvaldMove = true;

		final ImageView movingView = creatInvalidDragMovingView(dragView,
				movedResourceId);

		disableButtons = true;
		AnimationSet replaceAnimation = new AnimationSet(false);
		// animations should be applied on the finish line
		replaceAnimation.setFillAfter(false);

		int[] targetPost = new int[2];
		int[] sourcePost = new int[2];
		target.getLocationOnScreen(targetPost);
		dragView.getLocationOnScreen(sourcePost);
		int xChange = targetPost[0] - sourcePost[0];
		int yChange = targetPost[1] - sourcePost[1];

		// create translation animation
		TranslateAnimation trans = new TranslateAnimation(0, xChange, 0,
				yChange);
		trans.setDuration(400);

		// add new animations to the set
		replaceAnimation.addAnimation(trans);

		Interpolator interpolator = new AnticipateInterpolator();
		replaceAnimation.setInterpolator(interpolator);

		replaceAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				disableButtons = false;
				hadInvaldMove = false;
				movingView.setVisibility(View.GONE);
				runnable.run();
			}

		});
		//
		// // start our animation
		movingView.startAnimation(replaceAnimation);

	}

	// used while dropping, in case an invalid move was made, to NOT update UI,
	// and let the animation take place
	private boolean hadInvaldMove;
	private SquareRelativeLayoutView squareView;

	private void configureUICell(final BoardPieceStackImageView button,
			final Cell cell) {
		Piece visiblePiece = game.getBoard().getVisiblePiece(cell.getX(),
				cell.getY());
		if (visiblePiece != null) {
			button.setImageResource(visiblePiece.getImageResourceId());
		} else {
			button.setImageDrawable(null);
		}

		if (cell.equals(game.getFirstPickedCell())) {
			highlightStrictFirstTouchedPiece(button);
		} else {
			unhighlightStrictFirstTouchedPiece(button);
		}

		final DragSource dragSource = new DragSource() {

			@Override
			public boolean allowDrag() {
				// if we are in strict, and a piece was already chosen, only
				// that one can be dragged
				if (game.getFirstPickedCell() != null) {
					return game.getFirstPickedCell().equals(cell);
				}
				// conditionally alow dragging, if there is a piece and it is my
				// color
				Piece visiblePiece = getVisiblePiece();
				return visiblePiece != null
						&& visiblePiece.isBlack() == game.getCurrentPlayer()
								.isBlack();
			}

			public void update() {
				updateBoardPiece(cell);
			}

			public Piece getVisiblePiece() {
				return game.getBoard()
						.getVisiblePiece(cell.getX(), cell.getY());
			}

			@Override
			public void onDropCompleted(View target, boolean success) {
				if (!hadInvaldMove) {
					update();
				}
				if (game.getFirstPickedCell() == null) {
					unhighlightStrictFirstTouchedPiece(button);
				}
			}

			@Override
			public void onDropCanceled(DragView dragView) {
				int resId = 0;
				Piece visiblePiece = getVisiblePiece();
				if (visiblePiece != null) {
					resId = visiblePiece.getImageResourceId();
				}
				animateInvalidMoveReturn(dragView, resId, button,
						new Runnable() {
							public void run() {
								update();
								// TODO animateInvalidMoveReturn( onDropMove,
								// x,y);
								if (game.getType().isStrict()) {
									highlightStrictFirstTouchedPiece(button);
								}
							}
						});
				// in strict mode, highlight the chosen piece, as only it can be
				// moved

			}

			@Override
			public void setDragController(DragController dragger) {
			}
		};

		button.setOnDropListener(new OnDropListener() {
			@Override
			public void onDrop(View target, DragSource source, int x, int y,
					int xOffset, int yOffset, DragView dragView, Object dragInfo) {

				if (disableButtons) {
					return;
				}

				if (!game.getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a
					// human
					return;
				}
				// if same piece is droped on its original spot..
				// TODO mark that this piece MUST be moved?

				final OnDropMove onDropMove = (OnDropMove) dragInfo;
				if (onDropMove.originatedFrom(cell)) {
					// if in strict mode and the user already chose this piece,
					// make sure it remains highlighted
					if (game.getFirstPickedCell() != null) {
						highlightStrictFirstTouchedPiece(button);
					}

					return;
				}

				State state;
				try {
					state = onDropMove.droppedOn(cell);
					// TODO play valid move sound, based on previous target
					// exist, new piece ?
				} catch (final InvalidMoveException e) {
					int resId = onDropMove.getMovedImageResourceId();
					animateInvalidMoveReturn(dragView, resId,
							onDropMove.getSourceView(), new Runnable() {
								@Override
								public void run() {
									updateBoardPiece(cell);
									onDropMove.update();
									if (game.getFirstPickedCell() != null) {
										BoardPieceStackImageView source = (BoardPieceStackImageView) findButtonFor(game
												.getFirstPickedCell());
										highlightStrictFirstTouchedPiece(source);
									}
									int messageId = e.getErrorResourceId();
									Toast toast = Toast.makeText(getActivity(),
											messageId, Toast.LENGTH_SHORT);
									toast.setGravity(Gravity.CENTER, 0, 0);
									toast.show();
								}
							});
					getMainActivity().playSound(Sounds.INVALID_MOVE);
					return;
				}
				onDropMove.postMove();

				game.setFirstPickedCell(null);

				updateBoardPiece(cell);
				postMove(state, true);

				GameListener appListener = getMainActivity().getRoomListener();
				if (appListener != null) {
					AbstractMove lastMove = state.getLastMove();
					appListener.sendMove(game, lastMove, score);
					// getMainActivity().getGameHelper().showAlert(
					// "Sending move " + lastMove);
				}

			}

			@Override
			public boolean acceptDrop(View target, DragSource source, int x,
					int y, int xOffset, int yOffset, DragView dragView,
					Object dragInfo) {
				return true;
			}
		});
		button.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_DOWN)
					return false;

				if (disableButtons) {
					return false;
				}

				if (!game.getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a
					// human
					return false;
				}

				final Piece visiblePiece = game.getBoard().getVisiblePiece(
						cell.getX(), cell.getY());
				if (visiblePiece == null) {
					return false;
				}
				if (visiblePiece.isBlack() != game.getCurrentPlayer().isBlack()) {
					return false;
				}

				if (!dragSource.allowDrag()) {
					return false;
				}

				// We are starting a drag. Let the DragController handle it.
				DragConfig dragConfig = new DragConfig();
				dragConfig.alpha = 255;
				dragConfig.drawSelected = false;
				dragConfig.vibrate = false;
				dragConfig.animationScale = 1.0f;

				OnDropMove boardToBoardDropMove = new OnDropMove() {
					@Override
					public void postMove() {
						updateBoardPiece(cell);
					}

					@Override
					public State droppedOn(Cell toCell) {
						return game.movePiece(cell, toCell);
					}

					@Override
					public boolean originatedFrom(Cell otherCell) {
						return otherCell.equals(cell);
					}

					@Override
					public int getMovedImageResourceId() {
						return visiblePiece.getImageResourceId();
					}

					@Override
					public ImageView getSourceView() {
						return button;
					}

					@Override
					public void update() {
						postMove();
					}

				};

				// in case the piece was marked in strict mode, remove the
				// background highlight
				unhighlightStrictFirstTouchedPiece(button);

				mDragController.startDrag(v, dragSource, boardToBoardDropMove,
						DragController.DRAG_ACTION_COPY, dragConfig);

				if (game.getType().isStrict()) {
					game.setFirstPickedCell(cell);
				}

				// for a board move, let's leave the moved/top piece
				// visible/greyed?
				// TODO And not expose the piece underneath?

				Piece nextPiece = game.getBoard().peekNextPiece(cell);
				if (nextPiece == null) {
					button.setImageDrawable(null);
				} else {
					button.setImageResource(nextPiece.getImageResourceId());
				}

				return true;
			}
		});

		button.setOnDragListener(new OnDragListener() {
			@Override
			public void onDragOver(View target, DragSource source, int x,
					int y, int xOffset, int yOffset, DragView dragView,
					Object dragInfo) {
			}

			@Override
			public void onDragExit(View target, DragSource source, int x,
					int y, int xOffset, int yOffset, DragView dragView,
					Object dragInfo) {
			}

			@Override
			public void onDragEnter(View target, DragSource source, int x,
					int y, int xOffset, int yOffset, DragView dragView,
					Object dragInfo) {
			}
		});

		buttons.add(button);
		mDragLayer.addTarget(button);
	}

	protected void updateBoardPiece(Cell cell) {
		Piece visiblePiece = game.getBoard().getVisiblePiece(cell.getX(),
				cell.getY());
		ImageDropTarget button = findButtonFor(cell);
		if (visiblePiece == null) {
			button.setImageDrawable(null);
		} else {
			int imageResourceId = visiblePiece.getImageResourceId();
			button.setImageResource(imageResourceId);
		}
	}

	protected void showGameStats() {
		GameStatDialogFragment dialog = new GameStatDialogFragment();
		dialog.initialize(this, game, score);
		dialog.show(getChildFragmentManager(), "stats");
	}

	public void playAgain() {
		Player currentPlayer = game.getCurrentPlayer();
		game = new Game(game.getType(), game.getMode(), game.getBlackPlayer(),
				game.getWhitePlayer(), currentPlayer);
		addPieceListeners(getView());
		updateHeader(getView());
		winOverlayView.clearWins();
		winOverlayView.invalidate();
		// reset board
		for (ImageDropTarget each : buttons) {
			each.setImageDrawable(null);
		}
		// reset player stacks
		for (int i = 0; i < game.getType().getNumberOfStacks(); i++) {
			updatePlayerStack(game.getBlackPlayer(), i);
			updatePlayerStack(game.getWhitePlayer(), i);
		}
		acceptMove();
	}

	// private final class ButtonPressListener implements View.OnClickListener {
	// private final Cell cell;
	//
	// public ButtonPressListener(Cell cell) {
	// this.cell = cell;
	// }
	//
	// @Override
	// public void onClick(View view) {
	// if (disableButtons) {
	// return;
	// }
	// if (!game.getCurrentPlayer().getStrategy().isHuman()) {
	// // ignore button clicks if the current player is not a human
	// return;
	// }
	// // TODO button click is wrong event here... needs to be drop event
	// // Piece marker = game.getMarkerToPlay();
	// //
	// // boolean wasValid = makeAndDisplayMove(marker, cell);
	// // if (!wasValid)
	// // return;
	// //
	// // // send move to opponent
	// // RoomListener appListener = getMainActivity().getRoomListener();
	// // if (appListener != null) {
	// // appListener.sendMove(marker, cell);
	// // }
	//
	// }
	//
	// }

	private void updateHeader(View view) {
		if (view == null) {
			// safely allow calls when UI not created yet
			return;
		}
		TextView blackName = (TextView) view.findViewById(R.id.blackName);
		blackName.setText(game.getBlackPlayer().getName());
		TextView whiteName = (TextView) view.findViewById(R.id.whiteName);
		whiteName.setText(game.getWhitePlayer().getName());

		ImageView blackImage = (ImageView) view.findViewById(R.id.black_back);
		ImageView whiteImage = (ImageView) view.findViewById(R.id.white_back);

		game.getBlackPlayer().updatePlayerImage(imgManager, blackImage);
		game.getWhitePlayer().updatePlayerImage(imgManager, whiteImage);

		Player player = game.getCurrentPlayer();
		if (player.isBlack()) {
			blackHeaderLayout.setBackgroundResource(R.drawable.current_player);
			whiteHeaderLayout.setBackgroundResource(R.drawable.inactive_player);

			highlightPlayerTurn(blackHeaderLayout, whiteHeaderLayout);

		} else {
			whiteHeaderLayout.setBackgroundResource(R.drawable.current_player);
			blackHeaderLayout.setBackgroundResource(R.drawable.inactive_player);

			highlightPlayerTurn(whiteHeaderLayout, blackHeaderLayout);
		}

		updateGameStatDisplay();
	}

	private void updateGameStatDisplay() {
		numMoves.setText("" + game.getNumberOfMoves());
		blackWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, score.getBlackWins(),
				score.getBlackWins()));
		whiteWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, score.getWhiteWins(),
				score.getWhiteWins()));
		// draws.setText("" + score.getDraws());
		gameNumber.setText("" + score.getTotalGames());
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void highlightPlayerTurn(View highlight, View dimmed) {
		float notTurnAlpha = 0.50f;
		if (Utils.hasHoneycomb()) {
			highlight.setAlpha(1f);
			dimmed.setAlpha(notTurnAlpha);
		}
	}

	public boolean makeAndDisplayMove(AbstractMove move) {
		State outcome = null;
		try {
			outcome = move.applyToGame(game);
		} catch (InvalidMoveException e) {
			getMainActivity().playSound(Sounds.INVALID_MOVE);
			int messageId = e.getErrorResourceId();
			Toast toast = Toast.makeText(getActivity(), messageId,
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			return false;
		}
		// // TODO diff sounds for diff size piece/color..
		// if (move.getPlayer().isBlack()) {
		// getMainActivity().playSound(Sounds.PLAY_X);
		// } else {
		// getMainActivity().playSound(Sounds.PLAY_O);
		// }
		animateMove(move, outcome);
		return true;
	}

	private void animateMove(final AbstractMove move, final State outcome) {
		ImageDropTarget targetButton = findButtonFor(move.getTargetCell());

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

		// TODO expose the pieces underneath when animation starts
		// TODO use a new separate view (like the DragView) to actually animate,
		// so it stays above all the layers

		ImageView markerToPlayView;
		int movedResourceId;
		Runnable theUpdate = null;
		int exposedSourceResId;
		// markerToPlayView.setImageDrawable(null);
		int xChange;
		int yChange;

		movedResourceId = move.getPlayedPiece().getImageResourceId();
		if (move instanceof ExistingPieceMove) {
			final ExistingPieceMove theMove = (ExistingPieceMove) move;
			ImageDropTarget source = findButtonFor(theMove.getSource());
			Cell source2 = theMove.getSource();
			Piece nextPiece = game.getBoard().getVisiblePiece(source2.getX(),
					source2.getY());
			exposedSourceResId = nextPiece != null ? nextPiece
					.getImageResourceId() : 0;

			// TODO these are probably correct
			// xChange = targetButton.getLeft() - source.getLeft();
			// yChange = targetButton.getTop()
			// + ((View) targetButton.getParent()).getTop()
			// - (source.getTop() + ((View) source.getParent()).getTop());

			int[] targetPost = new int[2];
			int[] sourcePost = new int[2];
			targetButton.getLocationOnScreen(targetPost);
			source.getLocationOnScreen(sourcePost);
			xChange = targetPost[0] - sourcePost[0];
			yChange = targetPost[1] - sourcePost[1];

			theUpdate = new Runnable() {
				@Override
				public void run() {
					updateBoardPiece(theMove.getSource());
				}
			};
			markerToPlayView = source;
		} else {
			final PlaceNewPieceMove theMove = (PlaceNewPieceMove) move;
			final int stackNum = theMove.getStackNum();
			PieceStackImageView playerStackView = getPlayerStackView(
					theMove.getPlayer(), stackNum);

			PieceStack pieceStack = move.getPlayer().getPlayerPieces()
					.get(stackNum);
			Piece nextPiece = pieceStack.getTopPiece();
			exposedSourceResId = nextPiece != null ? nextPiece
					.getImageResourceId() : 0;

			// TODO these are not correct
			// need to find a common reference frame
			int[] targetPost = new int[2];
			int[] sourcePost = new int[2];
			targetButton.getLocationOnScreen(targetPost);
			playerStackView.getLocationOnScreen(sourcePost);
			xChange = targetPost[0] - sourcePost[0];
			yChange = targetPost[1] - sourcePost[1];
			theUpdate = new Runnable() {
				@Override
				public void run() {
					updatePlayerStack(move.getPlayer(), stackNum);
				}
			};
			markerToPlayView = playerStackView;
		}
		final Runnable update = theUpdate;

		if (exposedSourceResId == 0) {
			markerToPlayView.setImageDrawable(null);
		} else {
			markerToPlayView.setImageResource(exposedSourceResId);
		}
		final ImageView movingView = creatMovingView(markerToPlayView,
				movedResourceId);

		// create translation animation
		TranslateAnimation trans = new TranslateAnimation(0, xChange, 0,
				yChange);
		trans.setDuration(1000);

		// add new animations to the set
		replaceAnimation.addAnimation(trans);

		// AlphaAnimation fade = new AlphaAnimation(1, 0);
		// fade.setStartOffset(800);
		// fade.setDuration(200);
		// replaceAnimation.addAnimation(fade);

		Interpolator interpolator = new AnticipateInterpolator();
		replaceAnimation.setInterpolator(interpolator);

		replaceAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				movingView.setVisibility(View.GONE);
				update.run();
				updateBoardPiece(move.getTargetCell());
				postMove(outcome, true);
			}

		});
		//
		// // start our animation
		movingView.startAnimation(replaceAnimation);
	}

	private ImageView creatInvalidDragMovingView(View markerToPlayView,
			int resource) {
		ImageView animatorImage = (ImageView) getView().findViewById(
				R.id.moving_view);
		animatorImage.setImageResource(resource);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				markerToPlayView.getWidth(), markerToPlayView.getHeight());
		int[] windowLocation = new int[2];
		markerToPlayView.getLocationOnScreen(windowLocation);

		View main = getView().findViewById(R.id.drag_layer);
		int[] rootLocation = new int[2];
		main.getLocationOnScreen(rootLocation);

		params.leftMargin = windowLocation[0] - rootLocation[0];
		params.topMargin = windowLocation[1] - rootLocation[1];

		animatorImage.setLayoutParams(params);
		animatorImage.setVisibility(View.VISIBLE);

		animatorImage.bringToFront();

		return animatorImage;
	}

	private ImageView creatMovingView(View markerToPlayView, int resource) {
		ImageView animatorImage = (ImageView) getView().findViewById(
				R.id.moving_view);
		animatorImage.setImageResource(resource);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				markerToPlayView.getWidth(), markerToPlayView.getHeight());
		int[] windowLocation = new int[2];
		markerToPlayView.getLocationInWindow(windowLocation);

		View main = getView().findViewById(R.id.game_root);
		int[] rootLocation = new int[2];
		main.getLocationInWindow(rootLocation);

		params.leftMargin = windowLocation[0] - rootLocation[0];
		params.topMargin = windowLocation[1] - rootLocation[1];

		animatorImage.setLayoutParams(params);
		animatorImage.setVisibility(View.VISIBLE);

		animatorImage.bringToFront();

		return animatorImage;
	}

	private void postMove(State outcome, boolean playSound) {
		if (outcome.isOver()) {
			updateGameStatDisplay();
			endGame(outcome, playSound);
		} else {
			evaluateInGameAchievements(outcome);
			updateHeader(getView());
			acceptMove();
		}
	}

	private void endGame(State outcome, boolean playSound) {
		numMoves.setText("" + game.getNumberOfMoves());
		evaluateGameEndAchievements(outcome);
		evaluateLeaderboards(outcome);
		Player winner = outcome.getWinner();
		String title;
		if (winner != null) {
			winOverlayView.clearWins();
			score.incrementScore(winner);
			for (Win each : outcome.getWins()) {
				winOverlayView.addWinStyle(each.getWinStyle());
			}
			winOverlayView.invalidate();

			if (playSound) {
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
			}

			title = getString(R.string.player_won, winner.getName());

		} else {
			score.incrementScore(null);
			getMainActivity().playSound(Sounds.GAME_DRAW);
			title = getString(R.string.draw);
		}
		promptToPlayAgain(winner.getName(), title);
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

		aiMakeMove(currentStrategy);
	}

	private void configureNonLocalProgresses() {
		// if (thinking == null || thinkingText == null) {
		// // safety for when start called before activity is created
		// return;
		// }
		PlayerStrategy strategy = game.getCurrentPlayer().getStrategy();
		if (strategy.isHuman()) {
			hideStatusText();
			disableButtons = false;
			return;
		}
		disableButtons = true;
		setOpponentThinking();
		showStatusText();

	}

	public void onlineMakeMove(final ByteBufferDebugger buffer) {
		AbstractMove move = AbstractMove.fromMessageBytes(buffer, game);
		highlightAndMakeMove(move);
	}

	private void aiMakeMove(final PlayerStrategy currentStrategy) {
		AsyncTask<Void, Void, AbstractMove> aiMove = new AsyncTask<Void, Void, AbstractMove>() {
			@Override
			protected AbstractMove doInBackground(Void... params) {
				return currentStrategy.move(game);
			}

			@Override
			protected void onPostExecute(final AbstractMove move) {
				highlightAndMakeMove(move);
			}
		};
		aiMove.execute((Void) null);
	}

	private void highlightAndMakeMove(final AbstractMove move) {
		// hide the progress icon
		hideStatusText();

		// delay and highlight the move so the human player has a
		// chance to see it
		// TODO incorporate source cell...

		// find source piece- either player stack, or on board
		// View source;

		makeAndDisplayMove(move);
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

	public interface OnDropMove {

		State droppedOn(Cell cell);

		void update();

		ImageView getSourceView();

		int getMovedImageResourceId();

		boolean originatedFrom(Cell cell);

		void postMove();

	}

	private void highlightStrictFirstTouchedPiece(
			final BoardPieceStackImageView button) {
		button.setBackgroundColor(getResources().getColor(
				R.color.holo_blue_light));
	}

	private void unhighlightStrictFirstTouchedPiece(
			final BoardPieceStackImageView button) {
		button.setBackgroundDrawable(null);
	}

	public Game getGame() {
		return game;
	}

	public ScoreCard getScore() {
		return score;
	}

}
