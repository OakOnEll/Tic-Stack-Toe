package com.oakonell.ticstacktoe.ui.network.turn;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.game.RankedPlayAgain;

public class TurnBasedPlayAgainFragment extends RankedPlayAgain {
	private String opponentName;
	private String winnerName;

	private ProgressBar waiting;
	private ImageView opponentPlayAgainImageView;
	private TextView opponentPlayAgainText;
	private TextView opponentPlayAgainTitle;

	private Button positiveButton;
	private Button negativeButton;

	private TurnBasedMatchGameStrategy listener;
	private Runnable configure;

	public void initialize(TurnBasedMatchGameStrategy listener, String myName,
			String opponentName, String winner, boolean isRanked,
			boolean iAmBlack) {
		initialize(isRanked, myName, opponentName, iAmBlack);
		this.opponentName = opponentName;
		this.listener = listener;
		this.winnerName = winner;
	}

	@Override
	protected void continueCreateView(ViewGroup container, View view,
			Bundle savedInstanceState) {
		setTitle(view, getActivity().getString(R.string.player_won, winnerName));

		opponentPlayAgainText = (TextView) view.findViewById(R.id.text);
		waiting = (ProgressBar) view.findViewById(R.id.waiting);
		opponentPlayAgainImageView = (ImageView) view.findViewById(R.id.image);
		opponentPlayAgainTitle = (TextView) view.findViewById(R.id.title);
		negativeButton = (Button) view.findViewById(R.id.negative);
		positiveButton = (Button) view.findViewById(R.id.positive);

		configure.run();
	}

	public void displayAcceptInvite(final String invitationId) {
		configure = new Runnable() {
			@Override
			public void run() {
				configure = null;

				positiveButton.setText(getString(R.string.accept));
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
						listener.acceptInvite(invitationId);
					}
				});
				negativeButton.setVisibility(View.VISIBLE);
				negativeButton.setText(getString(R.string.not_right_now));
				negativeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
					}
				});

				opponentPlayAgainTitle.setText(getString(
						R.string.opponent_invited_to_rematch, opponentName));
				opponentPlayAgainText
						.setText(getString(R.string.accept_invitation));
				opponentPlayAgainImageView
						.setImageResource(R.drawable.check_icon_835);
				waiting.setVisibility(View.GONE);
			}
		};
		if (getView() != null) {
			configure.run();
		}
	}

	public void displayWaitingForInvite() {
		configure = new Runnable() {
			@Override
			public void run() {
				configure = null;

				positiveButton.setText(getString(R.string.cancel));
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
					}
				});
				negativeButton.setVisibility(View.GONE);

				opponentPlayAgainTitle.setText(opponentName
						+ " has started a rematch, waiting for the invite.");
				opponentPlayAgainText.setText("Waiting...");
				opponentPlayAgainImageView.setImageDrawable(null);
				waiting.setVisibility(View.VISIBLE);
			}
		};
		if (getView() != null) {
			configure.run();
		}
	}

	public void displayGoToRematch(final TurnBasedMatch match) {
		configure = new Runnable() {
			@Override
			public void run() {
				configure = null;

				positiveButton.setText("Open Rematch");
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
						listener.updateMatch(match);
					}
				});
				negativeButton.setVisibility(View.VISIBLE);
				negativeButton.setText(getString(R.string.not_right_now));
				negativeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
					}
				});

				opponentPlayAgainTitle.setText("There is a rematch against "
						+ opponentName + ".");
				opponentPlayAgainText.setText("Open rematch?");
				opponentPlayAgainImageView
						.setImageResource(R.drawable.check_icon_835);
				waiting.setVisibility(View.VISIBLE);
			}
		};
		if (getView() != null) {
			configure.run();
		}

	}

	public void displayAskRematch() {
		configure = new Runnable() {
			@Override
			public void run() {
				configure = null;

				positiveButton.setText("Rematch");
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
						listener.rematch();
					}

				});
				negativeButton.setVisibility(View.VISIBLE);
				negativeButton.setText(getString(R.string.not_right_now));
				negativeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
					}
				});

				opponentPlayAgainTitle.setText("Start a rematch against "
						+ opponentName + "?");
				opponentPlayAgainText.setText("Start rematch?");
				opponentPlayAgainImageView.setImageDrawable(null);
				waiting.setVisibility(View.VISIBLE);
			}
		};
		if (getView() != null) {
			configure.run();
		}

	}

	private void close() {
		dismiss();
		listener.playAgainClosed();
	}

	@Override
	protected int getMainLayoutResId() {
		return R.layout.turn_play_again_dialog;
	}

}
