package com.oakonell.ticstacktoe.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;
import android.os.Parcelable;
import android.widget.Toast;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarController.UndoListener;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter;
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;
import com.oakonell.ticstacktoe.ui.network.turn.InviteMatchInfo;

public class DismissHelper {
	private static final int DISMISS_BAR_DURATION = 3000;

	public static void dismiss(final MenuFragment fragment,
			final MatchInfo matchInfo, final List<MatchInfo> matches,
			final MatchAdapter adapter) {
		final int index = matches.indexOf(matchInfo);
		matches.remove(matchInfo);

		final AtomicBoolean delete = new AtomicBoolean(true);

		UndoBarController.UndoBar undoBar = new UndoBarController.UndoBar(
				fragment.getActivity());
		undoBar.message(fragment.getString(R.string.dismissed_match,
				matchInfo.getText(fragment.getActivity())));
		undoBar.listener(new UndoListener() {
			@Override
			public void onUndo(Parcelable token) {
				// cancel the delete
				delete.set(false);
				Toast.makeText(fragment.getActivity(),
						fragment.getString(R.string.match_restored),
						Toast.LENGTH_SHORT).show();
				matches.add(index, matchInfo);
				adapter.notifyDataSetChanged();
			}
		});
		undoBar.duration(DISMISS_BAR_DURATION);
		undoBar.show(true);

		// schedule the delete...
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!delete.get()) {
					// was canceled
					return;
				}
				// Toast.makeText(fragment.getActivity(), "Actually dismissing",
				// Toast.LENGTH_SHORT).show();
				matchInfo.dismiss(fragment);
			}
		}, DISMISS_BAR_DURATION + 200);
	}

	public static void dismissWithOlder(final MenuFragment fragment,
			MatchInfo info, final List<MatchInfo> matches,
			final MatchAdapter adapter) {
		// create a map that keeps the order of index
		final Map<MatchInfo, Integer> matchesToDismiss = new LinkedHashMap<MatchInfo, Integer>();

		// accumulate matches to be dismissed
		// mark to be removed from the view... but only
		// actually delete on a delay
		int size = matches.size();
		for (int i = 0; i < size; i++) {
			MatchInfo each = matches.get(i);
			if (each.getUpdatedTimestamp() >= info.getUpdatedTimestamp()) {
				matchesToDismiss.put(each, i);
			}
		}
		matches.removeAll(matchesToDismiss.keySet());

		final AtomicBoolean delete = new AtomicBoolean(true);

		UndoBarController.UndoBar undoBar = new UndoBarController.UndoBar(
				fragment.getActivity());
		if (matchesToDismiss.size() > 1) {
			undoBar.message(fragment.getString(
					R.string.dismissed_completed_matches,
					matchesToDismiss.size() + ""));
		} else {
			MatchInfo match = matchesToDismiss.entrySet().iterator().next()
					.getKey();
			undoBar.message(fragment.getString(R.string.dismissed_match,
					match.getText(fragment.getActivity())));
		}
		undoBar.listener(new UndoListener() {
			@Override
			public void onUndo(Parcelable token) {
				// cancel the delete
				delete.set(false);
				for (Entry<MatchInfo, Integer> entry : matchesToDismiss
						.entrySet()) {
					matches.add(entry.getValue(), entry.getKey());
				}
				if (matchesToDismiss.size() > 1) {
					Toast.makeText(fragment.getActivity(),
							fragment.getString(R.string.matches_restored),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(fragment.getActivity(),
							fragment.getString(R.string.match_restored),
							Toast.LENGTH_SHORT).show();
				}
				adapter.notifyDataSetChanged();
			}
		});
		undoBar.duration(DISMISS_BAR_DURATION);
		undoBar.show(true);

		// schedule the delete...
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!delete.get()) {
					// was canceled
					return;
				}
				// Toast.makeText(fragment.getActivity(), "Actually dismissing",
				// Toast.LENGTH_SHORT).show();
				for (MatchInfo each : matchesToDismiss.keySet()) {
					each.dismiss(fragment);
				}
			}
		}, DISMISS_BAR_DURATION + 200);
	}

	public static void decline(final MenuFragment fragment,
			final InviteMatchInfo inviteMatchInfo,
			final List<MatchInfo> matches, final MatchAdapter adapter) {
		final int index = matches.indexOf(inviteMatchInfo);
		matches.remove(inviteMatchInfo);

		final AtomicBoolean delete = new AtomicBoolean(true);

		UndoBarController.UndoBar undoBar = new UndoBarController.UndoBar(
				fragment.getActivity());
		undoBar.message(fragment.getString(R.string.declined_invite,
				inviteMatchInfo.getOpponentName()));
		undoBar.listener(new UndoListener() {
			@Override
			public void onUndo(Parcelable token) {
				// cancel the delete
				delete.set(false);
				Toast.makeText(fragment.getActivity(),
						fragment.getString(R.string.invite_restored),
						Toast.LENGTH_SHORT).show();
				matches.add(index, inviteMatchInfo);
				adapter.notifyDataSetChanged();
			}
		});
		undoBar.duration(DISMISS_BAR_DURATION);
		undoBar.show(true);

		// schedule the delete...
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!delete.get()) {
					// was canceled
					return;
				}
				// Toast.makeText(fragment.getActivity(), "Actually declining",
				// Toast.LENGTH_SHORT).show();
				inviteMatchInfo.decline(fragment, matches);
			}
		}, DISMISS_BAR_DURATION + 200);

	}

}
