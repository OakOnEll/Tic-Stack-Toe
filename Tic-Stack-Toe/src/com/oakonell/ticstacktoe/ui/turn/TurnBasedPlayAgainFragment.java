package com.oakonell.ticstacktoe.ui.turn;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.R;

public class TurnBasedPlayAgainFragment extends SherlockDialogFragment {
	private String opponentName;
	private String winnerName;

	private ProgressBar waiting;
	private ImageView opponentPlayAgainImageView;
	private TextView opponentPlayAgainText;
	private TextView opponentPlayAgainTitle;

	private Button positiveButton;
	private Button negativeButton;

	private TurnListener listener;
	private Runnable configure;

	public void initialize(TurnListener listener, String opponentName,
			String winner) {
		this.opponentName = opponentName;
		this.listener = listener;
		this.winnerName = winner;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.turn_play_again_dialog,
				container, false);
		getDialog().setCancelable(false);
		getDialog().setTitle(winnerName + " Won!");

		opponentPlayAgainText = (TextView) view.findViewById(R.id.text);
		waiting = (ProgressBar) view.findViewById(R.id.waiting);
		opponentPlayAgainImageView = (ImageView) view.findViewById(R.id.image);
		opponentPlayAgainTitle = (TextView) view.findViewById(R.id.title);
		negativeButton = (Button) view.findViewById(R.id.negative);
		positiveButton = (Button) view.findViewById(R.id.positive);

		configure.run();

		return view;
	}

	public void displayAcceptInvite(final String invitationId) {
		configure = new Runnable() {
			@Override
			public void run() {
				configure = null;


				positiveButton.setText("Accept");
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
						listener.acceptInvite(invitationId);
					}
				});
				negativeButton.setVisibility(View.VISIBLE);
				negativeButton.setText("Not right now.");
				negativeButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						close();
					}
				});

				opponentPlayAgainTitle.setText(opponentName
						+ " has invited you to a rematch.");
				opponentPlayAgainText.setText("Accept the invitation?");
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


				positiveButton.setText("Cancel");
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
				negativeButton.setText("Not right now.");
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
				negativeButton.setText("Not right now.");
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

}
