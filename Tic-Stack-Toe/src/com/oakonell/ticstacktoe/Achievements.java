package com.oakonell.ticstacktoe;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;
import com.google.android.gms.games.Games;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.State;

public class Achievements {
	private final String TAG = Achievements.class.getName();

	private List<Achievement> endGameAchievements = new ArrayList<Achievements.Achievement>();
	private List<Achievement> inGameAchievements = new ArrayList<Achievements.Achievement>();

	public Achievements() {
		// inGameAchievements.add(dejaVu);
		// endGameAchievements.add(customCount);
	}

	private interface Achievement {
		void push(GameHelper gameHelper, Context context);

		void testAndSet(GameHelper gameHelper, Context context, Game game,
				State outcome);

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

		public void push(GameHelper helper, Context context) {
			if (value) {
				Games.Achievements.unlock(helper.getApiClient(),
						context.getString(achievementId));
				value = false;
			}
		}

		public void unlock(GameHelper helper, Context context) {
			boolean isSignedIn = helper.isSignedIn();
			if (isSignedIn) {
				Games.Achievements.unlock(helper.getApiClient(),
						context.getString(achievementId));
			}
			if (!helper.isSignedIn() || BuildConfig.DEBUG) {
				if (!value || BuildConfig.DEBUG) {
					Toast.makeText(
							context,
							context.getString(R.string.offline_achievement_label)
									+ " " + context.getString(stringId),
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

		public void push(GameHelper helper, Context context) {
			if (count > 0) {
				Games.Achievements.increment(helper.getApiClient(),
						context.getString(achievementId), count);
				count = 0;
			}
		}

		public void increment(GameHelper helper, Context context) {
			if (helper.isSignedIn()) {
				Games.Achievements.increment(helper.getApiClient(),
						context.getString(achievementId), 1);
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

	public void pushToGoogle(GameHelper helper, Context context) {
		if (!helper.isSignedIn())
			return;

		for (Achievement each : endGameAchievements) {
			each.push(helper, context);
		}
		for (Achievement each : inGameAchievements) {
			each.push(helper, context);
		}
	}

	public void testAndSetForInGameAchievements(GameHelper gameHelper,
			Context context, Game game, State outcome) {
		testAndSetAchievements(gameHelper, context, game, outcome,
				inGameAchievements, "In Game");
	}

	public void testAndSetForGameEndAchievements(GameHelper gameHelper,
			Context context, Game game, State outcome) {
		testAndSetAchievements(gameHelper, context, game, outcome,
				endGameAchievements, "End Game");
	}

	private void testAndSetAchievements(GameHelper gameHelper, Context context,
			Game game, State outcome, List<Achievement> achievements,
			String type) {
		for (Achievement each : achievements) {
			try {
				each.testAndSet(gameHelper, context, game, outcome);
			} catch (RuntimeException e) {
				String text = "Error testing " + type + " achievement "
						+ each.getName();
				if (BuildConfig.DEBUG) {
					Toast.makeText(context, text + ": " + e.getMessage(),
							Toast.LENGTH_LONG).show();
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
