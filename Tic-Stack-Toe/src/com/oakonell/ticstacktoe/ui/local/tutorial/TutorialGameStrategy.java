package com.oakonell.ticstacktoe.ui.local.tutorial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.Achievements;
import com.oakonell.ticstacktoe.GameContext;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Cell;
import com.oakonell.ticstacktoe.model.ExistingPieceMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.InvalidMoveException;
import com.oakonell.ticstacktoe.model.Piece;
import com.oakonell.ticstacktoe.model.PlaceNewPieceMove;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.PlayerStrategy;
import com.oakonell.ticstacktoe.model.RankInfo;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.HumanStrategy;
import com.oakonell.ticstacktoe.ui.local.AbstractLocalStrategy;
import com.oakonell.ticstacktoe.ui.local.tutorial.TutorialGameStrategy.TutorialPage.Type;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;

/**
 * This class is a simple game strategy that walks the user through some
 * predetermined moves against the computer, to learn how to play.
 */
public class TutorialGameStrategy extends AbstractLocalStrategy {

	private View tutorialView;

	private View dialog;
	private TextView dialogTitle;
	private TextView dialogText;

	private View slideUp;
	private TextView slideUpTitle;
	private TextView slideUpText;

	private Map<Integer, List<TutorialPage>> tutorialPagesByMoveNum;
	private int pageIndex = -1;

	private Button dialogNext;

	private View next;

	private ImageButton min_max_tutorial;

	private View tutorial_bottom;

	protected int getAnalyticGameActionResId() {
		return R.string.an_start_tutorial_game_action;
	}

	public static class TutorialPlayerStrategy extends PlayerStrategy {
		protected TutorialPlayerStrategy() {
			super(false);
		}

		@Override
		public AbstractMove move(Game game) {
			switch (game.getNumberOfMoves()) {
			case 1:
				return new PlaceNewPieceMove(game.getWhitePlayer(),
						Piece.WHITE4, 0, new Cell(2, 2), null);
			case 3:
				return new PlaceNewPieceMove(game.getWhitePlayer(),
						Piece.WHITE4, 1, new Cell(2, 0), null);
			case 5:
				return new PlaceNewPieceMove(game.getWhitePlayer(),
						Piece.WHITE3, 0, new Cell(0, 1), null);
			case 7:
				return new PlaceNewPieceMove(game.getWhitePlayer(),
						Piece.WHITE3, 1, new Cell(1, 0), null);
			}
			// game should be covered completely by the above moves
			return null;
		}
	}

	public static class BaseTutorial {
		String header;
		String descr;
	}

	public static class TutorialPage extends BaseTutorial {
		enum Type {
			DIALOG, SLIDE_UP
		}

		Type type;
		int backgroundResId;
	}

	Map<Integer, List<TutorialPage>> getTutorialPagesByMoveNum() {
		if (tutorialPagesByMoveNum != null) {
			return tutorialPagesByMoveNum;
		}
		tutorialPagesByMoveNum = new HashMap<Integer, List<TutorialPage>>();

		/** ----------------- First Move */
		List<TutorialPage> pages = new ArrayList<TutorialGameStrategy.TutorialPage>();
		TutorialPage page = new TutorialPage();
		page.type = Type.DIALOG;
		page.header = "Welcome to Tic-Stack-Toe!";
		page.descr = "A game of strategy and memory.";
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.DIALOG;
		page.header = "Each player starts with some nesting stacks of pieces.";
		page.descr = "Two stacks of three for the Junior 3x3 game.\n"
				+ "Three stacks of four for the full-sized 4x4 games.";
		page.backgroundResId = R.drawable.tut_0_point_stacks;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "Players place their pieces on the board to try to form a line to win.";
		page.backgroundResId = R.drawable.tut_0_board;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "Your move first.";
		page.descr = "Drag one of your pieces to the middle tile.";
		page.backgroundResId = R.drawable.tut_0_move;
		pages.add(page);
		tutorialPagesByMoveNum.put(0, pages);

		/** ----------------- Second Move */
		pages = new ArrayList<TutorialGameStrategy.TutorialPage>();

		/*
		 * Where to put this tutorial snippet? page = new TutorialPage();
		 * page.type = Type.SLIDE_UP; page.header = ""; page.descr =
		 * "Pieces from the stack can be placed over smaller pieces on the board in the Junior and Normal game. "
		 * +
		 * "In strict mode, you can only play a piece from the stack over an existing board piece if your opponent has three in a row including that tile."
		 * ; page.backgroundResId = R.drawable.tut_2_move; page.nextEnabled =
		 * true; pages.add(page);
		 */
		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "Except in a strict game, pieces dragged from the stacks can be placed over smaller pieces on the board.";
		page.backgroundResId = 0;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "In a strict game, this is only allowed if your opponent already has all but one of their pieces in line.";
		page.backgroundResId = 0;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "Drag the other large piece from the player stack to upper right corner.";
		page.backgroundResId = R.drawable.tut_2_move;
		pages.add(page);
		tutorialPagesByMoveNum.put(2, pages);

		/** ----------------- Third Move */
		pages = new ArrayList<TutorialGameStrategy.TutorialPage>();
		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "You can move a piece already on the board to another empty space or place it over an existing smaller piece.";
		page.backgroundResId = 0;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "It is possible to expose a win for your opponent this way!";
		page.backgroundResId = 0;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "No peaking?";
		page.descr = "In a strict game, there is no peaking- once you touch a piece on the board you have to move it.";
		page.backgroundResId = 0;
		pages.add(page);

		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "Drag the upper right piece to the lower middle tile to block your opponent.";
		page.backgroundResId = R.drawable.tut_4_move;
		pages.add(page);
		tutorialPagesByMoveNum.put(4, pages);

		/** ----------------- Fourth Move */
		pages = new ArrayList<TutorialGameStrategy.TutorialPage>();
		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "";
		page.descr = "Place a new piece from your stacks into the middle right tile.";
		page.backgroundResId = R.drawable.tut_6_move;
		pages.add(page);
		tutorialPagesByMoveNum.put(6, pages);

		/** ----------------- Fifth Move */
		pages = new ArrayList<TutorialGameStrategy.TutorialPage>();
		page = new TutorialPage();
		page.type = Type.SLIDE_UP;
		page.header = "Go for the Win!";
		page.descr = "Pick up the big lower middle piece, and place it over your opponent's middle left piece, getting three in a row!";
		page.backgroundResId = R.drawable.tut_8_move;
		pages.add(page);
		tutorialPagesByMoveNum.put(8, pages);

		return tutorialPagesByMoveNum;
	}

	public TutorialGameStrategy(GameContext context) {
		super(context);
	}

	public TutorialGameStrategy(GameContext context,
			TutorialMatchInfo tutorialMatchInfo) {
		super(context, tutorialMatchInfo);
	}

	public TutorialGameStrategy(GameContext context, boolean useSaveState) {
		super(context);
		Player blackPlayer = HumanStrategy.createPlayer("You", true);
		Player whitePlayer = createWhitePlayer("Opponent");
		Game game = new Game(GameType.JUNIOR, GameMode.TUTORIAL, blackPlayer,
				whitePlayer, blackPlayer);
		setGame(game);
		setMatchInfo(new TutorialMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, "You", "Opponent",
				System.currentTimeMillis(), game, new ScoreCard(0, 0, 0)));
		if (!useSaveState)
			return;

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		long lastPaused = preferences.getLong("tutorial_paused_time", 0);
		if (System.currentTimeMillis() - lastPaused > TimeUnit.DAYS.toMillis(1))
			return;
		int moveNum = preferences.getInt("tutorial_move", -1);
		if (moveNum <= 0)
			return;

		// reconstruct the tutorial game state, up to the moveNum
		game.placePlayerPiece(0, new Cell(1, 1));
		AbstractMove move = whitePlayer.getStrategy().move(getGame());
		move.applyToGame(game);
		if (moveNum <= 2)
			return;

		game.placePlayerPiece(1, new Cell(0, 2));
		move = whitePlayer.getStrategy().move(getGame());
		move.applyToGame(game);
		if (moveNum <= 4)
			return;

		game.movePiece(new Cell(0, 2), new Cell(2, 1));
		move = whitePlayer.getStrategy().move(getGame());
		move.applyToGame(game);
		if (moveNum <= 6)
			return;

		game.placePlayerPiece(0, new Cell(1, 2));
		move = whitePlayer.getStrategy().move(getGame());
		move.applyToGame(game);
		if (moveNum <= 8)
			return;

		game.movePiece(new Cell(2, 1), new Cell(1, 0));
	}

	public void startGame() {
		startNewGame(true, "You", "Opponent", GameType.JUNIOR, new ScoreCard(0,
				0, 0));
	}

	@Override
	public void viewCreated(final GameFragment gameFragment,
			LayoutInflater inflater, ViewGroup container,
			final FrameLayout frame) {
		tutorialView = inflater.inflate(R.layout.tutorial, container, false);
		frame.addView(tutorialView);

		dialogTitle = (TextView) tutorialView
				.findViewById(R.id.tutorial_dialog_title);
		dialogText = (TextView) tutorialView
				.findViewById(R.id.tutorial_dialog_text);
		slideUpTitle = (TextView) tutorialView
				.findViewById(R.id.tutorial_bottom_title);
		slideUpText = (TextView) tutorialView
				.findViewById(R.id.tutorial_bottom_text);
		dialog = tutorialView.findViewById(R.id.tutorial_dialog);
		slideUp = tutorialView.findViewById(R.id.tutorial_slideup);
		tutorial_bottom = tutorialView.findViewById(R.id.tutorial_bottom);
		min_max_tutorial = (ImageButton) tutorialView
				.findViewById(R.id.min_max_tutorial);
		min_max_tutorial.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (tutorial_bottom.getVisibility() == View.VISIBLE) {
					tutorial_bottom.setVisibility(View.GONE);
					min_max_tutorial
							.setImageResource(R.drawable.maximize_tutorial_arrow);
				} else {
					tutorial_bottom.setVisibility(View.VISIBLE);
					min_max_tutorial
							.setImageResource(R.drawable.minimize_tutorial_arrow);
				}
			}
		});

		dialog.setVisibility(View.GONE);
		slideUp.setVisibility(View.GONE);

		View skip = tutorialView.findViewById(R.id.skip);
		skip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				skipTutorial();
			}

		});
		dialogSkip = tutorialView.findViewById(R.id.dialog_skip);
		dialogSkip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				skipTutorial();
			}

		});
		dialogNext = (Button) tutorialView.findViewById(R.id.dialog_next);
		dialogNext.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				pageIndex++;
				processTutorial();
			}
		});
		next = tutorialView.findViewById(R.id.next);
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				pageIndex++;
				processTutorial();
			}
		});

	}

	protected TutorialMatchInfo createNewMatchInfo(String blackName,
			String whiteName, final Game game, ScoreCard score) {
		return new TutorialMatchInfo(TurnBasedMatch.MATCH_STATUS_ACTIVE,
				TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN, blackName, whiteName,
				System.currentTimeMillis(), game, score);
	}

	public RankInfo getRankInfo() {
		return null;
	}

	protected GameMode getGameMode() {
		return GameMode.TUTORIAL;
	}

	public static Uri getTutorialImageUri() {
		return Uri.parse("android.resource://com.oakonell.ticstacktoe/"
				+ R.drawable.dice_icon_14730);
	}

	public static Player staticCreateWhitePlayer(String name) {
		PlayerStrategy strategy = new TutorialPlayerStrategy();
		Uri imageUri = getTutorialImageUri();

		return new Player(name, imageUri, strategy);
	}

	@Override
	protected Player createWhitePlayer(String whiteName) {
		return staticCreateWhitePlayer(whiteName);
	}

	public void attemptHumanMove(final AbstractMove move,
			final OnHumanMove onHumanMove) {
		switch (getGame().getNumberOfMoves()) {
		case 0:
			if (move.getTargetCell().getX() != 1
					|| move.getTargetCell().getY() != 1) {
				onHumanMove.onInvalid(new InvalidMoveException(
						"Drag to the center tile!", R.string.about_title));
				return;
			}
			break;
		case 2:
			if (!(move instanceof PlaceNewPieceMove)
					|| move.getTargetCell().getX() != 0
					|| move.getTargetCell().getY() != 2
					|| move.getPlayedPiece() != Piece.BLACK4) {
				onHumanMove.onInvalid(new InvalidMoveException(
						"Drag to the upper right tile!", R.string.about_title));
				return;
			}
			break;
		case 4:
			if (!(move instanceof ExistingPieceMove)
					|| move.getTargetCell().getX() != 2
					|| move.getTargetCell().getY() != 1
					|| move.getPlayedPiece() != Piece.BLACK4
					|| ((ExistingPieceMove) move).getSource().getX() != 0) {
				onHumanMove.onInvalid(new InvalidMoveException(
						"Drag the uper right to the lower middle!",
						R.string.about_title));
				return;
			}
			break;
		case 6:
			if (!(move instanceof PlaceNewPieceMove)
					|| move.getTargetCell().getX() != 1
					|| move.getTargetCell().getY() != 2
					|| move.getPlayedPiece() != Piece.BLACK3) {
				onHumanMove.onInvalid(new InvalidMoveException(
						"Drag the uper right to the lower middle!",
						R.string.about_title));
				return;
			}
			break;
		case 8:
			if (!(move instanceof ExistingPieceMove)
					|| move.getTargetCell().getX() != 1
					|| move.getTargetCell().getY() != 0
					|| move.getPlayedPiece() != Piece.BLACK4
					|| ((ExistingPieceMove) move).getSource().getX() != 2) {
				onHumanMove
						.onInvalid(new InvalidMoveException(
								"Drag the lower middle to the left edge, covering the opponent's piece!",
								R.string.about_title));
				return;
			}
			break;
		}
		tutorialView.setBackgroundDrawable(new ColorDrawable(
				android.graphics.Color.TRANSPARENT));

		super.attemptHumanMove(move, onHumanMove);
	}

	protected void acceptNonHumanPlayerMove(final PlayerStrategy currentStrategy) {
		if (slideUp.getVisibility() == View.VISIBLE) {
			Animation slideDown = AnimationUtils.loadAnimation(getContext(),
					R.anim.tutorial_slide_down);
			slideDown.setAnimationListener(new BaseAnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					slideUp.setVisibility(View.GONE);
					acceptAIMove(currentStrategy);
				}

			});
			slideUp.startAnimation(slideDown);
		} else if (dialog.getVisibility() == View.VISIBLE) {
			Animation fadeOut = AnimationUtils.loadAnimation(getContext(),
					R.anim.tutorial_fade_out);
			fadeOut.setAnimationListener(new BaseAnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					dialog.setVisibility(View.GONE);
					acceptAIMove(currentStrategy);
				}

			});
			dialog.startAnimation(fadeOut);
		}

	}

	private void acceptAIMove(final PlayerStrategy currentStrategy) {
		AbstractMove move = currentStrategy.move(getGame());
		State state = applyNonHumanMove(move);
		updateGame();
		getGameFragment().animateMove(state.getLastMove(), state);
	}

	private void processTutorial() {
		tutorial_bottom.setVisibility(View.VISIBLE);
		min_max_tutorial.setImageResource(R.drawable.minimize_tutorial_arrow);

		List<TutorialPage> pages = getTutorialPagesByMoveNum().get(
				getGame().getNumberOfMoves());
		final TutorialPage page = pages.get(pageIndex);
		boolean isLastPage = pageIndex == pages.size() - 1;
		int nextVisibility = isLastPage ? View.GONE : View.VISIBLE;
		if (page.type == Type.DIALOG) {
			dialogNext.setVisibility(nextVisibility);
			dialogTitle.setText(page.header);
			dialogText.setText(page.descr);
		} else {
			next.setVisibility(nextVisibility);
			min_max_tutorial.setVisibility(isLastPage ? View.VISIBLE
					: View.GONE);
			if (page.header.length() == 0) {
				LayoutParams layoutParams = slideUpTitle.getLayoutParams();
				layoutParams.height = 0;
				slideUpTitle.setLayoutParams(layoutParams);
				slideUpTitle.requestLayout();
			} else {
				LayoutParams layoutParams = slideUpTitle.getLayoutParams();
				layoutParams.height = LayoutParams.WRAP_CONTENT;
				slideUpTitle.setLayoutParams(layoutParams);
				slideUpTitle.requestLayout();
				slideUpTitle.setText(page.header);
			}
			slideUpText.setText(page.descr);
		}
		getGameFragment().disableDragging(!isLastPage);

		final Runnable changeBackground = new Runnable() {
			@Override
			public void run() {
				if (page.backgroundResId != 0) {
					tutorialView.setBackgroundResource(page.backgroundResId);
				} else {
					tutorialView.setBackgroundDrawable(new ColorDrawable(
							android.graphics.Color.TRANSPARENT));
				}
			}
		};

		if (page.type == Type.DIALOG) {
			if (dialog.getVisibility() == View.GONE) {
				Animation anim = AnimationUtils.loadAnimation(getContext(),
						R.anim.fade_in);
				dialog.setVisibility(View.VISIBLE);
				anim.setAnimationListener(new BaseAnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						changeBackground.run();
					}
				});
				dialog.startAnimation(anim);
			} else {
				changeBackground.run();
			}
			if (slideUp.getVisibility() == View.VISIBLE) {
				Animation anim = AnimationUtils.loadAnimation(getContext(),
						R.anim.tutorial_slide_down);
				slideUp.setVisibility(View.GONE);
				slideUp.startAnimation(anim);
			}
		} else if (page.type == Type.SLIDE_UP) {
			if (slideUp.getVisibility() == View.GONE) {
				Animation anim = AnimationUtils.loadAnimation(getContext(),
						R.anim.tutorial_slide_up);
				slideUp.setVisibility(View.VISIBLE);
				anim.setAnimationListener(new BaseAnimationListener() {
					@Override
					public void onAnimationEnd(Animation animation) {
						changeBackground.run();
					}
				});
				slideUp.startAnimation(anim);
			} else {
				changeBackground.run();
			}
			if (dialog.getVisibility() == View.VISIBLE) {
				Animation anim = AnimationUtils.loadAnimation(getContext(),
						R.anim.fade_out);
				dialog.setVisibility(View.GONE);
				dialog.startAnimation(anim);
			}
		}
	}

	@Override
	protected void acceptHumanMove(final PlayerStrategy currentStrategy) {
		getGameFragment().acceptHumanMove();
		pageIndex = 0;
		processTutorial();
	}

	@Override
	public void promptToPlayAgain(String winner, String title) {
		pageIndex = 0;
		Animation anim = AnimationUtils.loadAnimation(getContext(),
				R.anim.tutorial_slide_down);
		slideUp.setVisibility(View.GONE);
		anim.setAnimationListener(new BaseAnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				dialog.setVisibility(View.VISIBLE);
				processPostNotes();
			}
		});
		slideUp.startAnimation(anim);

		dialogNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pageIndex++;
				processPostNotes();
			}
		});

	}

	private void processPostNotes() {
		BaseTutorial page = getPostNotes().get(pageIndex);
		if (pageIndex == getPostNotes().size() - 1) {
			dialogSkip.setVisibility(View.GONE);
			dialogNext.setText("OK");
			dialogNext.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					TicStackToe application = ((TicStackToe) getGameContext()
							.getSherlockActivity().getApplication());
					Achievements achievements = application.getAchievements();
					achievements.setFinishedTutorial(getGameContext(),
							getGame(), getGame().getBoard().getState());
					markTutorialFinished();
					leaveGame();
				}
			});
		}
		dialogTitle.setText(page.header);
		dialogText.setText(page.descr);
	}

	List<BaseTutorial> postNotes;

	private View dialogSkip;

	private List<BaseTutorial> getPostNotes() {
		if (postNotes != null) {
			return postNotes;
		}
		postNotes = new ArrayList<BaseTutorial>();
		BaseTutorial page = new BaseTutorial();
		page.header = "Congratulations!";
		page.descr = "You won this match, and have finished the tutorial.";
		postNotes.add(page);

		page = new BaseTutorial();
		page.header = "Play modes";
		page.descr = "You can play against\n"
				+ "  * Friends on the same device\n"
				+ "  * AIs of various difficulties\n"
				+ "  * Friends or Anonymous strangers over the network\n" //
				+ "     . Either real-time\n"
				+ "     . Or Turn-based at your liesure";
		postNotes.add(page);

		page = new BaseTutorial();
		page.header = "Ranked Play";
		page.descr = "You can choose to play ranked games against an AI or someone over the network."
				+ "The ELO ranking system is used, with a starting rank of 1200.";
		postNotes.add(page);

		return postNotes;
	}

	protected void gameFinished() {

	}

	public boolean shouldHideAd() {
		return true;
	}

	private void skipTutorial() {
		markTutorialFinished();
		leaveGame();
	}

	private void markTutorialFinished() {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		Editor edit = preferences.edit();
		edit.putBoolean(MenuFragment.SAW_TUTORIAL, true);
		edit.remove("tutorial_move");
		edit.remove("tutorial_paused_time");
		edit.commit();
	}

	private static class BaseAnimationListener implements AnimationListener {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

	}

	protected void saveToDB() {
		// tutorial isn't actually saved to the db
		// it is saved as a preference
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		Editor edit = preferences.edit();
		edit.putInt("tutorial_move", getGame().getNumberOfMoves());
		edit.putLong("tutorial_paused_time", System.currentTimeMillis());
		edit.commit();
	}

	protected void insertMatch() {
		saveToDB();
		showAndStartGame();
	}

	protected void writeDetailsToBundle(Bundle bundle) {
		bundle.putInt("tutorial_move", getGame().getNumberOfMoves());
	}

	@Override
	public boolean warnToLeave() {
		return true;
	}

}
