package com.oakonell.ticstacktoe;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.achievement.Achievements.UpdateAchievementResult;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Board;
import com.oakonell.ticstacktoe.model.Board.PieceStack;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.model.solver.AIMoveHelper;
import com.oakonell.ticstacktoe.model.solver.RandomAI;
import com.oakonell.ticstacktoe.ui.local.AiGameStrategy;
import com.oakonell.ticstacktoe.ui.network.AbstractNetworkedGameStrategy;

public class Achievements {
	private static final String TAG = Achievements.class.getName();

	private List<Achievement> endGameAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> beatAiAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> beatFriendAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> inGameAchievements = new ArrayList<Achievements.Achievement>();

	private static class PlayedNumGames extends IncrementalAchievement {
		PlayedNumGames(int achievementId, int label_id, String name) {
			super(achievementId, label_id, name);
		}

		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			// count any game types as a play
			increment(gameHelper);
		}
	}

	private static class ThanksAchievement extends BooleanAchievement {
		ThanksAchievement() {
			super(R.string.achievement_thanks,
					R.string.achievement_thanks_label, "Thanks!");
		}

		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			// This does not apply to pass 'n play, because you could "cheat" to
			// get it
			if (game.getMode() == GameMode.PASS_N_PLAY) {
				return;
			}
			// don't count AI matches against the random AI! 
			if (game.getMode() == GameMode.AI
					&& (game.getWhitePlayer().getStrategy() instanceof RandomAI)) {
				return;
			}

			// If the human/current player won, it w
			if (!outcome.getWinner().getStrategy().isHuman()) {
				return;
			}

			// If the winner was not the last player to move... it was an
			// uncovered win
			if (outcome.getWinner().isBlack() != outcome.getLastMove()
					.getPlayer().isBlack()) {
				unlock(gameHelper);
			}
		}
	}

	private static class TheGiftAchievement extends BooleanAchievement {
		TheGiftAchievement() {
			super(R.string.achievement_the_gift,
					R.string.achievement_the_gift_label, "The Gift");
		}

		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			// This does not apply to pass 'n play, because you could "cheat" to
			// get it
			if (game.getMode() == GameMode.PASS_N_PLAY) {
				return;
			}
			// don't count AI matches against the random AI! 
			if (game.getMode() == GameMode.AI
					&& (game.getWhitePlayer().getStrategy() instanceof RandomAI)) {
				return;
			}

			if (outcome.getWinner().getStrategy().isHuman()) {
				return;
			}
			// If the winner was not the last player to move... it was an
			// uncovered win
			if (outcome.getWinner().isBlack() != outcome.getLastMove()
					.getPlayer().isBlack()) {
				unlock(gameHelper);
			}
		}
	}

	private static class TwoBirdsAchievement extends BooleanAchievement {
		TwoBirdsAchievement() {
			super(R.string.achievement_two_birds_with_one_stone,
					R.string.achievement_two_birds_with_one_stone_label,
					"Two Birds with One Stone");
		}

		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			// This does not apply to pass 'n play, because you could "cheat" to
			// get it
			if (game.getMode() == GameMode.PASS_N_PLAY) {
				return;
			}
			// don't count AI matches against the random AI!
			if (game.getMode() == GameMode.AI
					&& (game.getWhitePlayer().getStrategy() instanceof RandomAI)) {
				return;
			}

			if (!outcome.getWinner().getStrategy().isHuman()) {
				return;
			}

			if (outcome.getWins().size() > 1) {
				unlock(gameHelper);
			}
		}

	}

	// TODO how to determine if a fork is present?
	// private static class ForkedAchievement extends BooleanAchievement {
	// protected ForkedAchievement() {
	// super(R.string.achievement_forked_win,
	// R.string.achievement_forked_win_label, "Forked Win");
	// }
	//
	// public void testAndSet(GameContext gameHelper, Game game, State outcome)
	// {
	// // TODO
	// Log.i("Achievements", "entering forked win test");
	//
	// // This does not apply to pass 'n play, because you could "cheat" to
	// // get it
	// if (game.getMode() == GameMode.PASS_N_PLAY) {
	// return;
	// }
	// // don't count AI matches against the random AI!
	// if (game.getMode() == GameMode.AI
	// && (game.getWhitePlayer().getStrategy() instanceof RandomAI)) {
	// return;
	// }
	//
	// // Since we got here, the game is not over.
	// // If we undo the last move and find a winning move for that player
	// // then they missed a win, and get this achievement
	// AbstractMove lastMove = outcome.getLastMove();
	// if (lastMove == null)
	// return;
	// Player player = lastMove.getPlayer();
	// if (!player.getStrategy().isHuman()) {
	// return;
	// }
	//
	// // create a copy of the game board and pieces
	// GameType type = game.getType();
	// Board copy = game.getBoard().copy();
	// List<PieceStack> blackPlayerPieces = new ArrayList<Board.PieceStack>();
	// List<PieceStack> whitePlayerPieces = new ArrayList<Board.PieceStack>();
	// for (PieceStack each : game.getBlackPlayerPieces()) {
	// blackPlayerPieces.add(each.copy());
	// }
	// for (PieceStack each : game.getWhitePlayerPieces()) {
	// whitePlayerPieces.add(each.copy());
	// }
	//
	// boolean shouldUnlock = false;
	//
	//
	//
	// // // undo from the copy
	// // lastMove.undo(copy, outcome, blackPlayerPieces,
	// // whitePlayerPieces);
	// //
	// // // get the player that made the move
	// // List<AbstractMove> validMoves = AIMoveHelper.getValidMoves(type,
	// // blackPlayerPieces, whitePlayerPieces, copy, player);
	// // // Attempt all the valid moves- if it results in a win for him,
	// // then
	// // // the player missed a winning move
	// // for (AbstractMove each : validMoves) {
	// // State result = each.applyTo(type, copy, blackPlayerPieces,
	// // whitePlayerPieces);
	// // try {
	// // if (result.isOver() && result.getWinner().equals(player)) {
	// // shouldUnlock = true;
	// // break;
	// // }
	// // } finally {
	// // each.undo(copy, outcome, blackPlayerPieces,
	// // whitePlayerPieces);
	// // }
	// // }
	// if (shouldUnlock) {
	// unlock(gameHelper);
	// }
	// }
	// }

	private static class MissedOpportunityAchievement extends
			BooleanAchievement {

		MissedOpportunityAchievement() {
			super(R.string.achievement_missed_opportunity,
					R.string.achievement_missed_opportunity_label,
					"Missed Opportunity");
		}

		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			// This does not apply to pass 'n play, because you could "cheat" to
			// get it
			if (game.getMode() == GameMode.PASS_N_PLAY) {
				return;
			}
			// don't count AI matches against the random AI!
			if (game.getMode() == GameMode.AI
					&& (game.getWhitePlayer().getStrategy() instanceof RandomAI)) {
				return;
			}

			// Since we got here, the game is not over.
			// If we undo the last move and find a winning move for that player
			// then they missed a win, and get this achievement
			AbstractMove lastMove = outcome.getLastMove();
			if (lastMove == null)
				return;
			Player player = lastMove.getPlayer();
			if (!player.getStrategy().isHuman()) {
				return;
			}

			// create a copy of the game board and pieces
			GameType type = game.getType();
			Board copy = game.getBoard().copy();
			List<PieceStack> blackPlayerPieces = new ArrayList<Board.PieceStack>();
			List<PieceStack> whitePlayerPieces = new ArrayList<Board.PieceStack>();
			for (PieceStack each : game.getBlackPlayerPieces()) {
				blackPlayerPieces.add(each.copy());
			}
			for (PieceStack each : game.getWhitePlayerPieces()) {
				whitePlayerPieces.add(each.copy());
			}
			// undo from the copy
			lastMove.undo(copy, outcome, blackPlayerPieces, whitePlayerPieces);

			// get the player that made the move
			boolean shouldUnlock = false;
			List<AbstractMove> validMoves = AIMoveHelper.getValidMoves(type,
					blackPlayerPieces, whitePlayerPieces, copy, player);
			// Attempt all the valid moves- if it results in a win for him, then
			// the player missed a winning move
			for (AbstractMove each : validMoves) {
				State result = each.applyTo(type, copy, blackPlayerPieces,
						whitePlayerPieces);
				try {
					if (result.isOver() && result.getWinner().equals(player)) {
						shouldUnlock = true;
						break;
					}
				} finally {
					each.undo(copy, outcome, blackPlayerPieces,
							whitePlayerPieces);
				}
			}
			if (shouldUnlock) {
				unlock(gameHelper);
			}
		}

	}

	private static abstract class BeatAiAchievement extends BooleanAchievement {
		private AILevel level;

		protected BeatAiAchievement(AILevel level, int achievementId,
				int stringId, String name) {
			super(achievementId, stringId, name);
			this.level = level;
		}

		abstract protected boolean checkType(GameType type);

		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			if (game.getMode() != GameMode.AI)
				return;
			if (!checkType(game.getType()))
				return;
			AiGameStrategy gameStrategy = (AiGameStrategy) gameHelper
					.getGameStrategy();
			if (gameStrategy.getAILevel() != level)
				return;
			if (!outcome.getWinner().isBlack())
				return;
			unlock(gameHelper);
		}
	}

	private static abstract class BeatAFriendAchievement extends
			BooleanAchievement {

		protected BeatAFriendAchievement(int achievementId, int stringId,
				String name) {
			super(achievementId, stringId, name);
		}

		abstract protected boolean checkType(GameType type);

		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			if (!(game.getMode() == GameMode.ONLINE || game.getMode() == GameMode.TURN_BASED))
				return;
			if (!checkType(game.getType()))
				return;
			AbstractNetworkedGameStrategy gameStrategy = (AbstractNetworkedGameStrategy) gameHelper
					.getGameStrategy();
			if (outcome.getWinner().isBlack() != gameStrategy.iAmBlackPlayer())
				return;
			unlock(gameHelper);
		}
	}

	Achievement finishedTutorial = new BooleanAchievement(
			R.string.achievement_finished_the_tutorial,
			R.string.achievement_finished_the_tutorial_label,
			"Finished the Tutorial") {
		@Override
		public void testAndSet(GameContext gameHelper, Game game, State outcome) {
			unlock(gameHelper);
		}
	};

	Achievement beatEasyJunior = new BeatAiAchievement(AILevel.EASY_AI,
			R.string.achievement_beat_the_easy_junior_bot,
			R.string.achievement_beat_the_easy_junior_bot_label,
			"Beat easy junior") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isJunior();
		}
	};

	Achievement beatMediumJunior = new BeatAiAchievement(AILevel.MEDIUM_AI,
			R.string.achievement_beat_the_medium_junior_bot,
			R.string.achievement_beat_the_medium_junior_bot_label,
			"Beat medium junior") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isJunior();
		}
	};
	Achievement beatHardJunior = new BeatAiAchievement(AILevel.HARD_AI,
			R.string.achievement_beta_the_hard_junior_bot,
			R.string.achievement_beta_the_hard_junior_bot_label,
			"Beat hard junior") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isJunior();
		}
	};

	// normal
	Achievement beatEasyNormal = new BeatAiAchievement(AILevel.EASY_AI,
			R.string.achievement_beat_the_easy_normal_bot,
			R.string.achievement_beat_the_easy_normal_bot_label,
			"Beat easy normal") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isNormal();
		}
	};

	Achievement beatMediumNormal = new BeatAiAchievement(AILevel.MEDIUM_AI,
			R.string.achievement_beat_the_medium_normal_bot,
			R.string.achievement_beat_the_medium_normal_bot_label,
			"Beat medium normal") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isNormal();
		}
	};
	Achievement beatHardNormal = new BeatAiAchievement(AILevel.HARD_AI,
			R.string.achievement_beat_the_hard_normal_bot,
			R.string.achievement_beat_the_hard_normal_bot_label,
			"Beat hard normal") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isNormal();
		}
	};

	// strict
	Achievement beatEasyStrict = new BeatAiAchievement(AILevel.EASY_AI,
			R.string.achievement_beat_the_easy_strict_bot,
			R.string.achievement_beat_the_easy_strict_bot_label,
			"Beat easy strict") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isStrict();
		}
	};

	Achievement beatMediumStrict = new BeatAiAchievement(AILevel.MEDIUM_AI,
			R.string.achievement_beat_the_medium_strict_bot,
			R.string.achievement_beat_the_medium_strict_bot_label,
			"Beat medium strict") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isStrict();
		}
	};
	Achievement beatHardStrict = new BeatAiAchievement(AILevel.HARD_AI,
			R.string.achievement_beat_the_hard_strict_bot,
			R.string.achievement_beat_the_hard_strict_bot_label,
			"Beat hard strict") {

		@Override
		protected boolean checkType(GameType type) {
			return type.isStrict();
		}
	};

	Achievement beatFriendJunior = new BeatAFriendAchievement(
			R.string.achievement_beat_a_friend_at_junior,
			R.string.achievement_beat_a_friend_at_junior_label,
			"Beat a Friend at Junior") {
		@Override
		protected boolean checkType(GameType type) {
			return type.isJunior();
		}
	};
	Achievement beatFriendNormal = new BeatAFriendAchievement(
			R.string.achievement_beat_a_friend_at_normal,
			R.string.achievement_beat_a_friend_at_normal_label,
			"Beat a Friend at Normal") {
		@Override
		protected boolean checkType(GameType type) {
			return type.isNormal();
		}
	};
	Achievement beatFriendStrict = new BeatAFriendAchievement(
			R.string.achievement_beat_a_friend_at_strict,
			R.string.achievement_beat_a_friend_at_strict_label,
			"Beat a Friend at Strict") {
		@Override
		protected boolean checkType(GameType type) {
			return type.isStrict();
		}
	};

	public Achievements() {
		// inGameAchievements.add(dejaVu);
		beatAiAchievements.add(beatEasyJunior);
		beatAiAchievements.add(beatMediumJunior);
		beatAiAchievements.add(beatHardJunior);
		beatAiAchievements.add(beatEasyNormal);
		beatAiAchievements.add(beatMediumNormal);
		beatAiAchievements.add(beatHardNormal);
		beatAiAchievements.add(beatEasyStrict);
		beatAiAchievements.add(beatMediumStrict);
		beatAiAchievements.add(beatHardStrict);
		//
		beatFriendAchievements.add(beatFriendJunior);
		beatFriendAchievements.add(beatFriendNormal);
		beatFriendAchievements.add(beatFriendStrict);

		endGameAchievements.add(new TwoBirdsAchievement());
		endGameAchievements.add(new PlayedNumGames(
				R.string.achievement_play_ten_matches,
				R.string.achievement_play_ten_matches_label,
				"Getting your feet wet"));
		endGameAchievements.add(new PlayedNumGames(
				R.string.achievement_play_100_matches,
				R.string.achievement_play_100_matches_label, "Dedicated"));
		endGameAchievements.add(new ThanksAchievement());
		endGameAchievements.add(new TheGiftAchievement());

		// inGameAchievements.add(new ForkedAchievement());
		inGameAchievements.add(new MissedOpportunityAchievement());
	}

	private interface Achievement {
		void push(GameContext gameHelper);

		void testAndSet(GameContext gameHelper, Game game, State outcome);

		boolean isPending();

		String getName();
	}

	private abstract static class BooleanAchievement implements Achievement {
		private boolean value = false;
		private final int achievementId;
		private final int stringId;
		private final String name;

		BooleanAchievement(int achievementId, int stringId, String name) {
			this.achievementId = achievementId;
			this.stringId = stringId;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isPending() {
			return value;
		}

		public void push(GameContext helper) {
			if (value) {
				Games.Achievements.unlock(
						helper.getGameHelper().getApiClient(), helper
								.getContext().getString(achievementId));
				value = false;
			}
		}

		public void unlock(final GameContext helper) {
			boolean isSignedIn = helper.getGameHelper().isSignedIn();
			if (isSignedIn) {
				PendingResult<UpdateAchievementResult> result = Games.Achievements
						.unlockImmediate(helper.getGameHelper().getApiClient(),
								helper.getContext().getString(achievementId));
				result.setResultCallback(new ResultCallback<UpdateAchievementResult>() {
					@Override
					public void onResult(
							UpdateAchievementResult achievementResult) {
						if (!achievementResult.getStatus().isSuccess())
							return;
						if (achievementResult.getStatus().getStatusCode() == GamesStatusCodes.STATUS_ACHIEVEMENT_UNLOCKED) {
							Tracker myTracker = EasyTracker.getTracker();
							Log.i(TAG, "Unlocked achievement " + getName());
							myTracker.sendEvent(
									helper.getContext().getString(
											R.string.an_achievement_unlocked),
									getName(), "", 0L);

						}
					}
				});
				// Games.Achievements.unlock(
				// helper.getGameHelper().getApiClient(), helper
				// .getContext().getString(achievementId));
			}
			if (!helper.getGameHelper().isSignedIn() || BuildConfig.DEBUG) {
				if (!value || BuildConfig.DEBUG) {
					Toast.makeText(
							helper.getContext(),
							helper.getContext().getString(
									R.string.offline_achievement_label)
									+ " "
									+ helper.getContext().getString(stringId),
							Toast.LENGTH_LONG).show();
				}
				value = true;
			}
		}
	}

	private static abstract class IncrementalAchievement implements Achievement {
		private int count = 0;
		private final int achievementId;
		private final int stringId;
		private final String name;

		IncrementalAchievement(int achievementId, int stringId, String name) {
			this.achievementId = achievementId;
			this.stringId = stringId;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isPending() {
			return count > 0;
		}

		public void push(GameContext helper) {
			if (count > 0) {
				Games.Achievements.increment(helper.getGameHelper()
						.getApiClient(),
						helper.getContext().getString(achievementId), count);
				count = 0;
			}
		}

		public void increment(GameContext helper) {
			if (helper.getGameHelper().isSignedIn()) {
				Games.Achievements.increment(helper.getGameHelper()
						.getApiClient(),
						helper.getContext().getString(achievementId), 1);
			} else {
				count++;
			}
		}
	}

	public boolean hasPending() {
		for (Achievement each : endGameAchievements) {
			if (each.isPending())
				return true;
		}
		for (Achievement each : inGameAchievements) {
			if (each.isPending())
				return true;
		}
		for (Achievement each : beatAiAchievements) {
			if (each.isPending())
				return true;
		}
		for (Achievement each : beatFriendAchievements) {
			if (each.isPending())
				return true;
		}

		return finishedTutorial.isPending();
	}

	public void pushToGoogle(GameContext helper) {
		if (!helper.getGameHelper().isSignedIn())
			return;

		for (Achievement each : endGameAchievements) {
			each.push(helper);
		}
		for (Achievement each : inGameAchievements) {
			each.push(helper);
		}
		for (Achievement each : beatAiAchievements) {
			each.push(helper);
		}
		for (Achievement each : beatFriendAchievements) {
			each.push(helper);
		}

		finishedTutorial.push(helper);
	}

	public void testAndSetForInGameAchievements(GameContext gameHelper,
			Game game, State outcome) {
		testAndSetAchievements(gameHelper, game, outcome, inGameAchievements,
				"In Game");
	}

	public void testAndSetForGameEndAchievements(GameContext gameHelper,
			Game game, State outcome) {
		testAndSetAchievements(gameHelper, game, outcome, endGameAchievements,
				"End Game");
	}

	public void testAndSetForBeatAiAchievements(GameContext gameHelper,
			Game game, State outcome) {
		testAndSetAchievements(gameHelper, game, outcome, beatAiAchievements,
				"Beat AI");
	}

	public void testAndSetForBeatAFriendAchievements(GameContext gameHelper,
			Game game, State outcome) {
		testAndSetAchievements(gameHelper, game, outcome,
				beatFriendAchievements, "Beat a Friend");
	}

	public void setFinishedTutorial(GameContext gameHelper, Game game,
			State outcome) {
		finishedTutorial.testAndSet(gameHelper, game, outcome);
	}

	private void testAndSetAchievements(GameContext gameHelper, Game game,
			State outcome, List<Achievement> achievements, String type) {
		for (Achievement each : achievements) {
			try {
				each.testAndSet(gameHelper, game, outcome);
			} catch (RuntimeException e) {
				String text = "Error testing " + type + " achievement "
						+ each.getName();
				if (BuildConfig.DEBUG) {
					Toast.makeText(gameHelper.getContext(),
							text + ": " + e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
				Tracker myTracker = EasyTracker.getTracker();
				myTracker.sendException(text, e, false);
				// don't crash game due to faulty implementation of achievement,
				// just log it
				Log.e(TAG, text + ": " + e.getMessage());
			}
		}
	}
}
