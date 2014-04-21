package com.oakonell.ticstacktoe.ui.game;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.Achievements;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.GameStrategy.OnHumanMove;
import com.oakonell.ticstacktoe.Leaderboards;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.Sounds;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.InvalidMoveException;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.State.Win;
import com.oakonell.ticstacktoe.ui.SquareRelativeLayoutView;
import com.oakonell.ticstacktoe.ui.SquareRelativeLayoutView.OnMeasureDependent;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
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

	private int boardSize;

	private SquareRelativeLayoutView squareView;

	private List<ImageDropTarget> buttons = new ArrayList<ImageDropTarget>();
	private WinOverlayView winOverlayView;

	private static class SquareBoardResizeInfo {
		int boardWidth;
		int boardHeight;
		int boardSize;
		int pieceStackHeight;
	}

	private SquareBoardResizeInfo resizeInfo = new SquareBoardResizeInfo();

	private boolean disableButtons;
	// used while dropping, in case an invalid move was made, to NOT update UI,
	// and let the animation take place
	private boolean hadInvaldMove;

	// state management
	private Runnable inOnResume;
	private Runnable inOnCreate;
	private GameContext gameContext;
	private TextView gameTypeTextView;

	public GameFragment() {
		// for finding references
	}

	public static GameFragment createFragment() {
		GameFragment fragment = new GameFragment();
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		gameContext = (GameContext) activity;
	}

	private GameStrategy getGameStrategy() {
		return gameContext.getGameStrategy();
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.i("GameFragment", "onResume");
		if (inOnResume != null) {
			inOnResume.run();
		}
	}

	public void startGame(final String waitingText, final boolean showMove) {
		inOnCreate = new Runnable() {
			@Override
			public void run() {
				Game game = getGameStrategy().getGame();
				boolean undoAndAnimateMove = false;
				final State endState = game.getBoard().getState();
				AbstractMove move = null;
				if (showMove) {
					move = game.getBoard().getState().getLastMove();
					undoAndAnimateMove = move != null;
				}
				gameTypeTextView.setText(GameTypeSpinnerHelper.getTypeName(
						getActivity(), game.getType()));
				if (undoAndAnimateMove) {
					game.undo(move);

					if (getView() != null) {
						configureBoardButtons(getView());
						updateHeader(getView());
						winOverlayView.clearWins();
						winOverlayView.invalidate();
					}

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
									makeAndDisplayMove(theMove);
								}
							});
						}
					};
					if (getView() != null) {
						inOnResume.run();
					}
				} else {
					if (getView() != null) {
						configureBoardButtons(getView());
						updateHeader(getView());
						winOverlayView.clearWins();
						winOverlayView.invalidate();
					}

					postMove(game.getBoard().getState(), showMove);
				}
				inOnCreate = null;

			}
		};
		if (getActivity() != null) {
			inOnCreate.run();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			leaveGame();
			return true;
		}
		if (getGameStrategy().onOptionsItemSelected(this, item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (getGameStrategy() != null) {
			// TODO when gameStrategy is "loaded", make sure to invalidate the
			// menu
			getGameStrategy().onCreateOptionsMenu(this, menu, inflater);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (getGameStrategy() != null) {
			// TODO when gameStrategy is "loaded", make sure to invalidate the
			// menu
			getGameStrategy().onPrepareOptionsMenu(this, menu);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		super.onSaveInstanceState(bundle);
		bundle.putInt("GAME_BOARD_SIZE", boardSize);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_game, container,
				false);

		// Listen for changes in the back stack
		getSherlockActivity().getSupportFragmentManager()
				.addOnBackStackChangedListener(
						new OnBackStackChangedListener() {
							@Override
							public void onBackStackChanged() {
								configureDisplayHomeUp();
							}
						});
		// Handle when activity is recreated like on orientation Change
		configureDisplayHomeUp();
		setHasOptionsMenu(true);

		storeViewReferences(view);

		View num_games_container = view.findViewById(R.id.num_games_container);
		num_games_container.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showGameStats();
			}
		});

		gameTypeTextView = (TextView) view.findViewById(R.id.game_type);
		gameTypeTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO show an alert with game play instructions
				AlertDialog.Builder builder = new Builder(getActivity());
				builder.setTitle("How to Play");
				builder.setMessage(GameTypeSpinnerHelper
						.getTypeDescriptionStringResource(getGameStrategy()
								.getGame().getType()));
				builder.setNeutralButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				builder.show();
			}
		});

		boolean keepScreenOn = false;
		if (savedInstanceState != null) {
			// will need to wait for the strategy/match to load
			keepScreenOn = false;
			boardSize = savedInstanceState.getInt("GAME_BOARD_SIZE");
			disableButtons = true;
		} else {
			// Don't know why this is null on a restored Activity in the middle
			// of a game fragment, and then back is hit
			if (getGameStrategy() != null) {
				boardSize = getGame().getBoard().getSize();
				keepScreenOn = getGameStrategy().shouldKeepScreenOn();
				configureBoardButtons(view);
				updateHeader(view);
			}
		}

		if (inOnCreate != null) {
			inOnCreate.run();
		}

		winOverlayView.setBoardSize(boardSize);

		view.setKeepScreenOn(keepScreenOn);
		if (boardSize == 3) {
			((ViewGroup) view.findViewById(R.id.grid_container))
					.setBackgroundResource(R.drawable.wood_grid_3x3);
		}

		// if (getGame().getMode() != GameMode.PASS_N_PLAY) {
		// initThinkingText(view, getGame().getNonLocalPlayer().getName());
		// setOpponentThinking();
		// } else {
		// initThinkingText(view, null);
		// }

		PieceStackImageView stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack1);
		stackView
				.setOnMeasureDependent(new PieceStackImageView.OnMeasureDependent() {
					@Override
					public void onMeasureCalled(
							PieceStackImageView squareRelativeLayoutView,
							int size, int origWidth, int origHeight) {
						if (resizeInfo != null) {
							resizeInfo.pieceStackHeight = origHeight;
							resizeBoardAndStacks(view);
						}

					}
				});

		squareView.setOnMeasureDependent(new OnMeasureDependent() {
			@Override
			public void onMeasureCalled(
					SquareRelativeLayoutView squareRelativeLayoutView,
					int size, int origWidth, int origHeight) {
				if (resizeInfo != null) {
					resizeInfo.boardHeight = origHeight;
					resizeInfo.boardWidth = origWidth;
					resizeInfo.boardSize = size;
					resizeBoardAndStacks(view);
				}
			}
		});

		return view;
	}

	private void storeViewReferences(final View view) {
		initThinkingText(view);

		mDragLayer = (DragLayer) view.findViewById(R.id.drag_layer);
		mDragController = new DragController(getActivity());
		mDragLayer.setDragController(mDragController);
		mDragController.setDragListener(mDragLayer);

		imgManager = ImageManager.create(getActivity());

		blackHeaderLayout = view.findViewById(R.id.black_name_layout);
		whiteHeaderLayout = view.findViewById(R.id.white_name_layout);

		winOverlayView = (WinOverlayView) view.findViewById(R.id.win_overlay);

		blackWins = (TextView) view.findViewById(R.id.num_black_wins);
		whiteWins = (TextView) view.findViewById(R.id.num_white_wins);
		// draws = (TextView) view.findViewById(R.id.num_draws);

		gameNumber = (TextView) view.findViewById(R.id.game_number);

		numMoves = (TextView) view.findViewById(R.id.num_moves);

		squareView = (SquareRelativeLayoutView) view
				.findViewById(R.id.grid_container);
	}

	protected void resizeBoardAndStacks(View view) {
		if (resizeInfo.boardSize == 0 || resizeInfo.pieceStackHeight == 0) {
			return;
		}

		int size = (resizeInfo.boardHeight + 2 * resizeInfo.pieceStackHeight)
				/ (boardSize + 2);
		int newBoardPixSize = size * boardSize;
		Log.i("GameFragment", "resizing: Board height = "
				+ resizeInfo.boardHeight + ", piece height = "
				+ resizeInfo.pieceStackHeight
				+ ", calculated single piece size =" + size + ", new board = "
				+ newBoardPixSize);

		resizePlayerStacks(getView(), size);

		LayoutParams layoutParams = squareView.getLayoutParams();
		layoutParams.height = newBoardPixSize;
		squareView.requestLayout();
		getView().requestLayout();
		
		resizeInfo = null;
	}

	private void configureDisplayHomeUp() {
		if (getSherlockActivity() == null) {
			return;
		}
		getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(
				true);
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

	private void configureBoardButtons(View view) {
		int size = getGame().getBoard().getSize();
		BoardPieceStackImageView button = (BoardPieceStackImageView) view
				.findViewById(R.id.button_r1c1);
		configureBoardButton(button, new Cell(0, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c2);
		configureBoardButton(button, new Cell(0, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c3);
		configureBoardButton(button, new Cell(0, 2));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r1c4);
		if (size > 3) {
			configureBoardButton(button, new Cell(0, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r1c5);
			configureBoardButton(button, new Cell(0, 4));
			button.setVisibility(View.VISIBLE);
		}

		// row2
		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c1);
		configureBoardButton(button, new Cell(1, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c2);
		configureBoardButton(button, new Cell(1, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r2c3);
		configureBoardButton(button, new Cell(1, 2));

		if (size > 3) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r2c4);
			configureBoardButton(button, new Cell(1, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r2c5);
			configureBoardButton(button, new Cell(1, 4));
			button.setVisibility(View.VISIBLE);
		}
		// row3
		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c1);
		configureBoardButton(button, new Cell(2, 0));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c2);
		configureBoardButton(button, new Cell(2, 1));

		button = (BoardPieceStackImageView) view.findViewById(R.id.button_r3c3);
		configureBoardButton(button, new Cell(2, 2));

		if (size > 3) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r3c4);
			configureBoardButton(button, new Cell(2, 3));
			button.setVisibility(View.VISIBLE);
		}
		if (size > 4) {
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r3c5);
			configureBoardButton(button, new Cell(2, 4));
			button.setVisibility(View.VISIBLE);
		}

		if (size > 3) {
			view.findViewById(R.id.button_row4).setVisibility(View.VISIBLE);
			// row4
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c1);
			configureBoardButton(button, new Cell(3, 0));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c2);
			configureBoardButton(button, new Cell(3, 1));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c3);
			configureBoardButton(button, new Cell(3, 2));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r4c4);
			configureBoardButton(button, new Cell(3, 3));
			button.setVisibility(View.VISIBLE);

			if (size > 4) {
				button = (BoardPieceStackImageView) view
						.findViewById(R.id.button_r4c5);
				configureBoardButton(button, new Cell(3, 4));
				button.setVisibility(View.VISIBLE);
			}
		}

		if (size > 4) {
			view.findViewById(R.id.button_row5).setVisibility(View.VISIBLE);
			// row5
			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c1);
			configureBoardButton(button, new Cell(4, 0));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c2);
			configureBoardButton(button, new Cell(4, 1));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c3);
			configureBoardButton(button, new Cell(4, 2));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c4);
			configureBoardButton(button, new Cell(4, 3));
			button.setVisibility(View.VISIBLE);

			button = (BoardPieceStackImageView) view
					.findViewById(R.id.button_r5c5);
			configureBoardButton(button, new Cell(4, 4));
			button.setVisibility(View.VISIBLE);
		}

		// handle the player stacks
		PieceStackImageView stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack1);
		configurePlayerStack(stackView, 0, getGame().getBlackPlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack2);
		configurePlayerStack(stackView, 1, getGame().getBlackPlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.black_piece_stack3);
		if (getGame().getType().getNumberOfStacks() > 2) {
			configurePlayerStack(stackView, 2, getGame().getBlackPlayer());
		} else {
			stackView.setVisibility(View.GONE);
		}

		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack1);
		configurePlayerStack(stackView, 0, getGame().getWhitePlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack2);
		configurePlayerStack(stackView, 1, getGame().getWhitePlayer());
		stackView = (PieceStackImageView) view
				.findViewById(R.id.white_piece_stack3);
		if (getGame().getType().getNumberOfStacks() > 2) {
			configurePlayerStack(stackView, 2, getGame().getWhitePlayer());
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
								boolean hasExposedPiece = pieceStack
										.peekNextPiece() != null;
								if (hasExposedPiece) {
									getGameStrategy()
											.playSound(Sounds.SLIDE_ON);
								} else {
									getGameStrategy().playSound(Sounds.PUT_ON);
								}
								update();
							}
						});
			}

			@Override
			public boolean allowDrag() {
				// if we are in strict mode, and a board piece was touched, no
				// stack moves area allowed
				if (getGame().getFirstPickedCell() != null) {
					return false;
				}

				boolean isCurrentPlayersPiece = pieceStack.getTopPiece()
						.isBlack() == getGame().getCurrentPlayer().isBlack();
				if (!isCurrentPlayersPiece) {
					return false;
				}
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
				if (!getGame().getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a human
					return false;
				}

				Piece topPiece = pieceStack.getTopPiece();
				if (topPiece == null) {
					return false;
				}

				boolean isCurrentPlayersPiece = topPiece.isBlack() == getGame()
						.getCurrentPlayer().isBlack();
				if (!isCurrentPlayersPiece) {
					return false;
				}

				if (!dragSource.allowDrag()) {
					return false;
				}

				// We are starting a drag. Let the DragController handle it.
				DragConfig dragConfig = createDragConfiguration();

				OnDropMove newPieceOnDrop = new OnDropMove() {
					@Override
					public void postMove() {
						updatePlayerStack(pieceStack, stackView);
					}

					@Override
					public AbstractMove createMove(Cell targetCell) {
						Piece playedPiece = getGame().getCurrentPlayerPieces()
								.get(stackNum).getTopPiece();
						Piece existingTargetPiece = getGame().getBoard()
								.getVisiblePiece(targetCell);
						return new PlaceNewPieceMove(getGame()
								.getCurrentPlayer(), playedPiece, stackNum,
								targetCell, existingTargetPiece);
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

				boolean hasExistingPiece = nextPiece != null;
				if (hasExistingPiece) {
					getGameStrategy().playSound(Sounds.SLIDE_OFF);
				} else {
					getGameStrategy().playSound(Sounds.TAKE_OFF);
				}

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

	private DragConfig createDragConfiguration() {
		DragConfig dragConfig = new DragConfig();
		dragConfig.alpha = 255;
		dragConfig.drawSelected = false;
		dragConfig.vibrate = false;
		dragConfig.animationScale = 1.0f;
		return dragConfig;
	}

	protected void animateInvalidMoveReturn(final DragView dragView,
			int movedResourceId, ImageView target, final Runnable runnable) {
		hadInvaldMove = true;

		final ImageView movingView = createInvalidDragMovingView(dragView,
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

	private void configureBoardButton(final BoardPieceStackImageView button,
			final Cell cell) {
		Piece visiblePiece = getGame().getBoard().getVisiblePiece(cell);
		if (visiblePiece != null) {
			button.setImageResource(visiblePiece.getImageResourceId());
		} else {
			button.setImageDrawable(null);
		}

		if (cell.equals(getGame().getFirstPickedCell())) {
			highlightStrictFirstTouchedPiece(button);
		} else {
			unhighlightStrictFirstTouchedPiece(button);
		}

		final DragSource dragSource = new DragSource() {

			@Override
			public boolean allowDrag() {
				if (disableButtons) {
					return false;
				}
				if (!getGame().getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a
					// human
					return false;
				}
				// if we are in strict, and a piece was already chosen, only
				// that one can be dragged
				if (getGame().getFirstPickedCell() != null) {
					return getGame().getFirstPickedCell().equals(cell);
				}
				// conditionally alow dragging, if there is a piece and it is my
				// color
				Piece visiblePiece = getVisiblePiece();
				return visiblePiece != null
						&& visiblePiece.isBlack() == getGame()
								.getCurrentPlayer().isBlack();
			}

			public void update() {
				updateBoardPiece(cell);
			}

			public Piece getVisiblePiece() {
				return getGame().getBoard().getVisiblePiece(cell);
			}

			@Override
			public void onDropCompleted(View target, boolean success) {
				if (!hadInvaldMove) {
					update();
				}
				if (getGame().getFirstPickedCell() == null) {
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
								// play the appropriate sound
								boolean hasExposedPiece = getGame().getBoard()
										.peekNextPiece(cell) != null;
								if (hasExposedPiece) {
									getGameStrategy()
											.playSound(Sounds.SLIDE_ON);
								} else {
									getGameStrategy().playSound(Sounds.PUT_ON);
								}
								update();
								if (getGame().getType().isStrict()) {
									// in strict mode, highlight the chosen
									// piece, as only it can be
									// moved
									highlightStrictFirstTouchedPiece(button);
								}
							}
						});

			}

			@Override
			public void setDragController(DragController dragger) {
			}
		};

		button.setOnDropListener(new OnDropListener() {
			@Override
			public void onDrop(View target, DragSource source, int x, int y,
					int xOffset, int yOffset, final DragView dragView,
					Object dragInfo) {

				if (disableButtons) {
					return;
				}

				if (!getGame().getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a
					// human
					return;
				}
				final OnDropMove onDropMove = (OnDropMove) dragInfo;
				if (onDropMove.originatedFrom(cell)) {
					// if in strict mode and the user already chose this piece,
					// make sure it remains highlighted
					if (getGame().getFirstPickedCell() != null) {
						highlightStrictFirstTouchedPiece(button);
					}

					return;
				}

				AbstractMove move = onDropMove.createMove(cell);
				getGameStrategy().attemptHumanMove(move, new OnHumanMove() {
					@Override
					public void onSuccess(State state) {
						boolean hasExistingPiece = getGame().getBoard()
								.peekNextPiece(cell) != null;
						if (hasExistingPiece) {
							getGameStrategy().playSound(Sounds.SLIDE_ON);
						} else {
							getGameStrategy().playSound(Sounds.PUT_ON);
						}
						onDropMove.postMove();

						getGame().setFirstPickedCell(null);

						updateBoardPiece(cell);
						postMove(state, true);
					}

					@Override
					public void onInvalid(final InvalidMoveException e) {
						int resId = onDropMove.getMovedImageResourceId();
						animateInvalidMoveReturn(dragView, resId,
								onDropMove.getSourceView(), new Runnable() {
									@Override
									public void run() {
										updateBoardPiece(cell);
										onDropMove.update();
										if (getGame().getFirstPickedCell() != null) {
											BoardPieceStackImageView source = (BoardPieceStackImageView) findButtonFor(getGame()
													.getFirstPickedCell());
											highlightStrictFirstTouchedPiece(source);
										}
										int messageId = e.getErrorResourceId();
										Toast toast = Toast.makeText(
												getActivity(), messageId,
												Toast.LENGTH_SHORT);
										toast.setGravity(Gravity.CENTER, 0, 0);
										toast.show();
									}
								});
						getGameStrategy().playSound(Sounds.INVALID_MOVE);
					}

				});
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

				if (!getGame().getCurrentPlayer().getStrategy().isHuman()) {
					// ignore button clicks if the current player is not a
					// human
					return false;
				}

				final Piece visiblePiece = getGame().getBoard()
						.getVisiblePiece(cell);
				if (visiblePiece == null) {
					return false;
				}
				if (visiblePiece.isBlack() != getGame().getCurrentPlayer()
						.isBlack()) {
					return false;
				}

				if (!dragSource.allowDrag()) {
					return false;
				}

				// We are starting a drag. Let the DragController handle it.
				DragConfig dragConfig = createDragConfiguration();

				OnDropMove boardToBoardDropMove = new OnDropMove() {
					@Override
					public void postMove() {
						updateBoardPiece(cell);
					}

					@Override
					public AbstractMove createMove(Cell targetCell) {
						Piece playedPiece = getGame().getBoard()
								.getVisiblePiece(cell);
						Piece exposedSourcePiece = getGame().getBoard()
								.peekNextPiece(cell);
						Piece existingTargetPiece = getGame().getBoard()
								.getVisiblePiece(targetCell);
						return new ExistingPieceMove(getGame()
								.getCurrentPlayer(), playedPiece,
								exposedSourcePiece, cell, targetCell,
								existingTargetPiece);
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

				if (getGame().getType().isStrict()) {
					getGame().setFirstPickedCell(cell);
				}

				Piece nextPiece = getGame().getBoard().peekNextPiece(cell);
				boolean hasExistingPiece = nextPiece != null;
				if (hasExistingPiece) {
					getGameStrategy().playSound(Sounds.SLIDE_OFF);
				} else {
					getGameStrategy().playSound(Sounds.TAKE_OFF);
				}
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
		Piece visiblePiece = getGame().getBoard().getVisiblePiece(cell);
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
		dialog.initialize(this, getGame(), getScore());
		dialog.show(getChildFragmentManager(), "stats");
	}

	public void refreshHeader() {
		if (getView() != null) {
			updateHeader(getView());
		}
	}

	private void updateHeader(View view) {
		if (view == null) {
			// safely allow calls when UI not created yet
			return;
		}
		TextView blackName = (TextView) view.findViewById(R.id.blackName);
		blackName.setText(getGame().getBlackPlayer().getName());
		TextView whiteName = (TextView) view.findViewById(R.id.whiteName);
		whiteName.setText(getGame().getWhitePlayer().getName());

		TextView blackRank = (TextView) view.findViewById(R.id.black_rank);
		TextView whiteRank = (TextView) view.findViewById(R.id.white_rank);
		RankInfo rankInfo = getGameStrategy().getRankInfo();
		if (rankInfo != null) {
			blackRank.setVisibility(View.VISIBLE);
			whiteRank.setVisibility(View.VISIBLE);
			blackRank.setText(rankInfo.blackRank() + "");
			whiteRank.setText(rankInfo.whiteRank() + "");
		} else {
			blackRank.setVisibility(View.GONE);
			whiteRank.setVisibility(View.GONE);
		}

		ImageView blackImage = (ImageView) view.findViewById(R.id.black_back);
		ImageView whiteImage = (ImageView) view.findViewById(R.id.white_back);

		getGame().getBlackPlayer().updatePlayerImage(imgManager, blackImage);
		getGame().getWhitePlayer().updatePlayerImage(imgManager, whiteImage);

		Player player = getGame().getCurrentPlayer();
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
		numMoves.setText("" + getGame().getNumberOfMoves());
		blackWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, getScore().getBlackWins(),
				getScore().getBlackWins()));
		whiteWins.setText(getResources().getQuantityString(
				R.plurals.num_wins_with_label, getScore().getWhiteWins(),
				getScore().getWhiteWins()));
		// draws.setText("" + score.getDraws());
		gameNumber.setText("" + getScore().getTotalGames());
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void highlightPlayerTurn(View highlight, View dimmed) {
		float notTurnAlpha = 0.50f;
		if (Utils.hasHoneycomb()) {
			highlight.setAlpha(1f);
			dimmed.setAlpha(notTurnAlpha);
		}
	}

	private boolean makeAndDisplayMove(AbstractMove move) {
		State outcome = null;
		try {
			outcome = move.applyToGame(getGame());
		} catch (InvalidMoveException e) {
			getGameStrategy().playSound(Sounds.INVALID_MOVE);
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

	public void animateMove(final AbstractMove move, final State outcome) {
		hideStatusText();
		ImageDropTarget targetButton = findButtonFor(move.getTargetCell());

		disableButtons = true;
		// experimenting with animations...
		// create set of animations
		AnimationSet replaceAnimation = new AnimationSet(false);
		// animations should be applied on the finish line
		replaceAnimation.setFillAfter(false);

		ImageView markerToPlayView;
		int movedResourceId;
		Runnable theUpdate = null;
		int exposedSourceResId;
		int xChange;
		int yChange;

		movedResourceId = move.getPlayedPiece().getImageResourceId();
		if (move instanceof ExistingPieceMove) {
			final ExistingPieceMove theMove = (ExistingPieceMove) move;
			ImageDropTarget source = findButtonFor(theMove.getSource());
			Cell source2 = theMove.getSource();
			Piece nextPiece = getGame().getBoard().getVisiblePiece(source2);
			exposedSourceResId = nextPiece != null ? nextPiece
					.getImageResourceId() : 0;

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

		Sounds liftSound;
		if (exposedSourceResId == 0) {
			markerToPlayView.setImageDrawable(null);
			liftSound = Sounds.TAKE_OFF;
		} else {
			markerToPlayView.setImageResource(exposedSourceResId);
			liftSound = Sounds.SLIDE_OFF;
		}
		final ImageView movingView = createMovingView(markerToPlayView,
				movedResourceId);

		// create translation animation
		TranslateAnimation trans = new TranslateAnimation(0, xChange, 0,
				yChange);
		trans.setDuration(1000);

		// add new animations to the set
		replaceAnimation.addAnimation(trans);

		Interpolator interpolator = new AnticipateInterpolator();
		replaceAnimation.setInterpolator(interpolator);

		getGameStrategy().playSound(liftSound);
		replaceAnimation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				Cell targetCell = move.getTargetCell();
				boolean hasExistingTargetPiece = getGame().getBoard()
						.peekNextPiece(targetCell) != null;
				if (hasExistingTargetPiece) {
					getGameStrategy().playSound(Sounds.SLIDE_ON);
				} else {
					getGameStrategy().playSound(Sounds.PUT_ON);
				}
				movingView.setVisibility(View.GONE);
				update.run();
				updateBoardPiece(move.getTargetCell());
				postMove(outcome, true);
			}

		});
		// start our animation
		movingView.startAnimation(replaceAnimation);
	}

	private ImageView createInvalidDragMovingView(View markerToPlayView,
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

	private ImageView createMovingView(View markerToPlayView, int resource) {
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
			getGameStrategy().acceptMove();
		}
	}

	private void endGame(State outcome, boolean playSound) {
		numMoves.setText("" + getGame().getNumberOfMoves());
		evaluateGameEndAchievements(outcome);
		evaluateLeaderboards(outcome);
		Player winner = outcome.getWinner();
		String title;
		if (winner != null) {
			winOverlayView.clearWins();
			getScore().incrementScore(winner);
			for (Win each : outcome.getWins()) {
				winOverlayView.addWinStyle(each.getWinStyle());
			}
			winOverlayView.invalidate();

			if (playSound) {
				if (getGame().getMode() == GameMode.PASS_N_PLAY) {
					getGameStrategy().playSound(Sounds.GAME_WON);
				} else {
					// the player either won or lost
					if (winner.equals(getGame().getLocalPlayer())) {
						getGameStrategy().playSound(Sounds.GAME_WON);
					} else {
						getGameStrategy().playSound(Sounds.GAME_LOST);
					}
				}
			}

			title = getString(R.string.player_won, winner.getName());

		} else {
			getScore().incrementScore(null);
			getGameStrategy().playSound(Sounds.GAME_DRAW);
			title = getString(R.string.draw);
		}
		getGameStrategy().promptToPlayAgain(winner.getName(), title);
	}

	public void acceptHumanMove() {
		hideStatusText();
		// let the buttons be pressed for a human interaction
		disableButtons = false;
		return;
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
		achievements.testAndSetForGameEndAchievements(gameContext, getGame(),
				outcome);
	}

	private void evaluateInGameAchievements(State outcome) {
		TicStackToe application = ((TicStackToe) getActivity().getApplication());

		Achievements achievements = application.getAchievements();
		achievements.testAndSetForInGameAchievements(gameContext, getGame(),
				outcome);
	}

	private void evaluateLeaderboards(State outcome) {
		TicStackToe application = ((TicStackToe) getActivity().getApplication());

		Leaderboards leaderboards = application.getLeaderboards();
		// leaderboards.submitGame(gameContext.getGameHelper(),
		// gameContext.getContext(), getGame(), outcome, getScore());

	}

	public interface OnDropMove {

		AbstractMove createMove(Cell targetCell);

		void update();

		ImageView getSourceView();

		int getMovedImageResourceId();

		boolean originatedFrom(Cell cell);

		void postMove();

	}

	private void highlightStrictFirstTouchedPiece(
			final BoardPieceStackImageView button) {
		// TODO make new method on BoardPieceStackImageView
		button.setBackgroundColor(getResources().getColor(
				R.color.holo_blue_light));
	}

	private void unhighlightStrictFirstTouchedPiece(
			final BoardPieceStackImageView button) {
		// TODO make new method on BoardPieceStackImageView
		button.setBackgroundDrawable(null);
	}

	public Game getGame() {
		return getGameStrategy().getGame();
	}

	public ScoreCard getScore() {
		return getGameStrategy().getScore();
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
		gameContext.gameEnded();
	}
}
