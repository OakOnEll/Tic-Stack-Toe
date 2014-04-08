package com.oakonell.ticstacktoe;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.Games;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.GameMode;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.State;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.ui.local.AiGameStrategy;
import com.oakonell.ticstacktoe.ui.network.AbstractNetworkedGameStrategy;

public class Achievements {
	private final String TAG = Achievements.class.getName();

	private List<Achievement> endGameAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> beatAiAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> beatFriendAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> inGameAchievements = new ArrayList<Achievements.Achievement>();

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

		public void unlock(GameContext helper) {
			boolean isSignedIn = helper.getGameHelper().isSignedIn();
			if (isSignedIn) {
				Games.Achievements.unlock(
						helper.getGameHelper().getApiClient(), helper
								.getContext().getString(achievementId));
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
		private final String name;

		IncrementalAchievement(int achievementId, String name) {
			this.achievementId = achievementId;
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
		return false;
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
