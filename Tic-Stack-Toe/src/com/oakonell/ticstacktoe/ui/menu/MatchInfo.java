package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;

public interface MatchInfo {
	public CharSequence getText(Context context);

	public CharSequence getSubtext(Context context);

	public CharSequence getUpdatedText(Context context);

	public Uri getIconImageUri();

	public long getUpdatedTimestamp();

	public int getMatchStatus();

	public void dismiss(MenuFragment fragment, List<MatchInfo> matches);

	public List<MatchMenuItem> getMenuItems(Context context);

	public void onClick(MenuFragment fragment);

	public class MatchUtils {
		private static Comparator<MatchInfo> comparator = new Comparator<MatchInfo>() {
			@Override
			public int compare(MatchInfo lhs, MatchInfo rhs) {
				return (int) (lhs.getUpdatedTimestamp() - rhs
						.getUpdatedTimestamp());
			}
		};

		public static int getVariant(GameType type, boolean isRanked) {
			if (isRanked) {
				return type.getVariant() + 100;
			}
			return type.getVariant();
		}

		public static boolean isRanked(int variant) {
			return variant > 100;
		}

		public static GameType getType(int variant) {
			if (variant > 100) {
				return GameType.fromVariant(variant - 100);
			}
			return GameType.fromVariant(variant);
		}

		public static CharSequence getTimeSince(Context context, long time) {
			return DateUtils.getRelativeDateTimeString(context, time,
					DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
					DateUtils.FORMAT_ABBREV_RELATIVE);
		}

		public static Comparator<MatchInfo> getComparator() {
			return comparator;
		}

		public static void addDismissThisAndOlder(List<MatchMenuItem> result,
				final MatchInfo info) {
			if (info.getMatchStatus() != TurnBasedMatch.MATCH_STATUS_COMPLETE
					&& info.getMatchStatus() != TurnBasedMatch.MATCH_STATUS_EXPIRED) {
				return;
			}
			MatchMenuItem dismissOlder = new MatchMenuItem(
					"Dismiss this and older", new ItemExecute() {
						@Override
						public void execute(final MenuFragment fragment,
								List<MatchInfo> matches) {
							List<MatchInfo> myMatches = new ArrayList<MatchInfo>(
									matches);
							for (MatchInfo each : myMatches) {
								if (each.getUpdatedTimestamp() >= info
										.getUpdatedTimestamp()) {
									each.dismiss(fragment, matches);
								}
							}
						}
					});
			result.add(dismissOlder);

		}
	}

}
