package com.oakonell.ticstacktoe.ui.network.turn;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.googleapi.GameHelper;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
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

	private GameHelper helper;
	private int variant;

	public InviteMatchInfo(GameHelper gameHelper, Invitation invite) {
		this.helper = gameHelper;
		created = invite.getCreationTimestamp();
		inviteId = invite.getInvitationId();

		Player player = invite.getInviter().getPlayer();
		opponentName = player.getDisplayName();
		opponentPicUri = player.getIconImageUri();
		isTurnBased = invite.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED;
		variant = invite.getVariant();
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
				if (isTurnBased) {
					Games.TurnBasedMultiplayer.declineInvitation(
							helper.getApiClient(), inviteId);
				} else {
					Games.RealTimeMultiplayer.declineInvitation(
							helper.getApiClient(), inviteId);
				}
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
		CharSequence subtext;
		boolean isRanked = MatchUtils.isRanked(variant);
		GameType type = MatchUtils.getType(variant);

		CharSequence timeSpanString = MatchUtils.getTimeSince(context,
				getLastUpdatedTimestamp());
		subtext = "Invited to " + (isRanked ? "Ranked " : "")
				+ GameTypeSpinnerHelper.getTypeName(context, type) + " "
				+ timeSpanString;

		return subtext;
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

	@Override
	public int getMatchStatus() {
		return TurnBasedMatch.MATCH_TURN_STATUS_INVITED;
	}

	@Override
	public void dismiss(MenuFragment fragment, List<MatchInfo> matches) {
		// doesn't support dismiss... instead it is a decline?
	}

}
