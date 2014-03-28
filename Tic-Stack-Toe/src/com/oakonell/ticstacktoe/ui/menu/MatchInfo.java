package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;

public interface MatchInfo {
	public CharSequence getText(Context context);

	public CharSequence getSubtext(Context context);

	public Uri getIconImageUri();

	public long getUpdatedTimestamp();

	public int getMatchStatus();

	public void dismiss(MenuFragment fragment, List<MatchInfo> matches);

	public List<MatchMenuItem> getMenuItems();

	public void onClick(MenuFragment fragment);

	public class MatchUtils {
		private static Comparator<MatchInfo> comparator = new Comparator<MatchInfo>() {
			@Override
			public int compare(MatchInfo lhs, MatchInfo rhs) {
				return (int) (lhs.getUpdatedTimestamp() - rhs
						.getUpdatedTimestamp());
			}
		};

		public static Comparator<MatchInfo> getComparator() {
			return comparator;
		}

		public static void addDismissThisAndOlder(List<MatchMenuItem> result,
				final MatchInfo info) {
			if (info.getMatchStatus() != TurnBasedMatch.MATCH_STATUS_COMPLETE && info.getMatchStatus() != TurnBasedMatch.MATCH_STATUS_EXPIRED) {
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
