package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.text.format.DateUtils;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.ItemExecute;
import com.oakonell.ticstacktoe.ui.menu.MatchAdapter.MatchMenuItem;

public class InviteMatchInfo implements MatchInfo {
	private String opponentName;
	private Uri opponentPicUri;
	private long created;
	private String inviteId;

	public InviteMatchInfo(GamesClient client, Invitation invite) {

		created = invite.getCreationTimestamp();
		inviteId = invite.getInvitationId();

		Player player = invite.getInviter().getPlayer();
		opponentName = player.getDisplayName();
		opponentPicUri = player.getIconImageUri();
	}

	public CharSequence getOpponentName() {
		return opponentName;
	}

	@Override
	public Uri getOpponentIconImageUri() {
		return opponentPicUri;
	}

	public long getLastUpdatedTimestamp() {
		return created;
	}

	public String getInviteId() {
		return inviteId;
	}

	public List<MatchMenuItem> getMenuItems() {
		List<MatchMenuItem> result = new ArrayList<MatchMenuItem>();
		MatchMenuItem dismiss = new MatchMenuItem();
		dismiss.text = "Decline";
		dismiss.execute = new ItemExecute() {
			@Override
			public void execute(MenuFragment fragment, List<MatchInfo> matches) {
				GamesClient gamesClient = fragment.getMainActivity()
						.getGamesClient();
				gamesClient.declineTurnBasedInvitation(inviteId);
				matches.remove(InviteMatchInfo.this);
			}
		};
		result.add(dismiss);
		return result;
	}

	@Override
	public void onClick(MenuFragment fragment) {
		fragment.acceptTurnBasedInvitation(inviteId);
	}

	@Override
	public CharSequence getSubtext(Context context) {
		CharSequence timeSpanString = DateUtils.getRelativeDateTimeString(
				context, getLastUpdatedTimestamp(), DateUtils.MINUTE_IN_MILLIS,
				DateUtils.WEEK_IN_MILLIS, 0);
		return timeSpanString;
	}

	@Override
	public CharSequence getText(Context context) {
		return "Invited by " + getOpponentName();
	}

	@Override
	public long getUpdatedTimestamp() {
		return created;
	}

}