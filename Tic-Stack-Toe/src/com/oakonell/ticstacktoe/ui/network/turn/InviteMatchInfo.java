package com.oakonell.ticstacktoe.ui.network.turn;

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
import com.oakonell.ticstacktoe.ui.menu.MatchInfo;
import com.oakonell.ticstacktoe.ui.menu.MenuFragment;

public class InviteMatchInfo implements MatchInfo {
	private final String opponentName;
	private final Uri opponentPicUri;
	private final long created;
	private final String inviteId;
	private final boolean isTurnBased;

	private GamesClient client;

	public InviteMatchInfo(GamesClient client, Invitation invite) {
		this.client = client;
		created = invite.getCreationTimestamp();
		inviteId = invite.getInvitationId();

		Player player = invite.getInviter().getPlayer();
		opponentName = player.getDisplayName();
		opponentPicUri = player.getIconImageUri();
		isTurnBased = invite.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED;
	}

	public CharSequence getOpponentName() {
		return opponentName;
	}

	@Override
	public Uri getIconImageUri() {
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
		MatchMenuItem dismiss = new MatchMenuItem("Decline", new ItemExecute() {
			@Override
			public void execute(MenuFragment fragment, List<MatchInfo> matches) {
				client.declineTurnBasedInvitation(inviteId);
				matches.remove(InviteMatchInfo.this);
			}
		});
		result.add(dismiss);
		return result;
	}

	@Override
	public void onClick(MenuFragment fragment) {
		if (isTurnBased) {
			fragment.acceptTurnBasedInvitation(inviteId);
		} else {
			fragment.acceptInviteToRoom(inviteId);
		}
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
		if (isTurnBased) {
			return "Invited by " + getOpponentName();
		} else {
			return "Real-time Invited by " + getOpponentName();
		}
	}

	@Override
	public long getUpdatedTimestamp() {
		return created;
	}

}
