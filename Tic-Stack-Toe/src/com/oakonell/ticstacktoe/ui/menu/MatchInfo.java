package com.oakonell.ticstacktoe.ui.menu;

import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;

public interface MatchInfo {
	public CharSequence getText(Context context);

	public CharSequence getSubtext(Context context);

	public long getUpdatedTimestamp();

	public Uri getOpponentIconImageUri();

	public List<MatchMenuItem> getMenuItems();

	public void onClick(MenuFragment fragment);

	public class Factory {
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
	}

}
