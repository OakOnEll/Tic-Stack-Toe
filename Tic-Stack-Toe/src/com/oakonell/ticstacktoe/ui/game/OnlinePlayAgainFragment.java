package com.oakonell.ticstacktoe.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.RoomListener.PlayAgainState;

public class OnlinePlayAgainFragment extends SherlockDialogFragment {
	private String opponentName;
	private String title;

	private ProgressBar opponentPlayAgainProgress;
	private ImageView opponentPlayAgainImageView;
	private TextView opponentPlayAgainText;

	private Button playAgainButton;

	private AbstractGameFragment gameFragment;
	boolean willPlayAgain;
	private Button notPlayAgainButton;

	public void initialize(AbstractGameFragment fragment, String opponentName,
			String title) {
		this.opponentName = opponentName;
		this.title = title;
		this.gameFragment = fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.play_again_dialog,
				container, false);
		getDialog().setTitle(title);
		getDialog().setCancelable(false);
		opponentPlayAgainText = (TextView) view
				.findViewById(R.id.opponent_wants_to_play_again_text);
		opponentPlayAgainText.setText(getResources().getString(
				R.string.waiting_for_opponent_to_decide_to_play_again,
				opponentName));

		opponentPlayAgainProgress = (ProgressBar) view
				.findViewById(R.id.opponent_wants_to_play_again_progress);
		opponentPlayAgainImageView = (ImageView) view
				.findViewById(R.id.opponent_wants_to_play_again);

		notPlayAgainButton = (Button) view.findViewById(R.id.not_play_again);
		notPlayAgainButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// send a message,
				// TODO show a progress while sending..
				Runnable success = new Runnable() {
					@Override
					public void run() {
						dismiss();
						gameFragment.playAgainClosed();
						// and leave immediately
						gameFragment.leaveGame();
					}
				};
				Runnable error = new Runnable() {
					@Override
					public void run() {
						dismiss();
						gameFragment.playAgainClosed();
						// TODO show error message?
						gameFragment.leaveGame();
					}
				};
				gameFragment.getMainActivity().getRoomListener()
						.sendNotPlayAgain(success, error);
			}

		});
		playAgainButton = (Button) view.findViewById(R.id.play_again);
		playAgainButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Runnable success = new Runnable() {
					@Override
					public void run() {
						willPlayAgain = true;
						if (gameFragment.getMainActivity().getRoomListener()
								.getOpponentPlayAgainState() == PlayAgainState.PLAY_AGAIN) {
							gameFragment.playAgain();
							gameFragment.playAgainClosed();
							dismiss();
						} else {
							playAgainButton.setEnabled(true);
						}
						// else wait:
						// change dialog to waiting for other player
						// hide yes button, but leave no button as an option
						TextView playAgainText = (TextView) view
								.findViewById(R.id.play_again_text);
						playAgainText.setVisibility(View.GONE);
						playAgainText.setText(getResources().getString(R.string.play_again_waiting, opponentName));
						playAgainButton.setVisibility(View.GONE);
						notPlayAgainButton.setText(R.string.exit_waiting_play_again);
					}
				};
				Runnable error = new Runnable() {
					@Override
					public void run() {
						// TODO how to handle an error?
						throw new RuntimeException(
								"Error sending 'play again' message");
					}
				};
				gameFragment.getMainActivity().getRoomListener()
						.sendPlayAgain(success, error);
			}

		});

		PlayAgainState opponentPlayAgainState = gameFragment.getMainActivity()
				.getRoomListener().getOpponentPlayAgainState();
		if (opponentPlayAgainState != PlayAgainState.WAITING) {
			updateOpponentPlayAgain(opponentPlayAgainState == PlayAgainState.PLAY_AGAIN);
		}

		return view;
	}

	public void updateOpponentPlayAgain(boolean willPlay) {
		opponentPlayAgainProgress.setVisibility(View.INVISIBLE);
		if (willPlay) {
			opponentPlayAgainText.setText(getResources().getString(
					R.string.opponent_wants_to_play_again, opponentName));
			opponentPlayAgainImageView
					.setImageResource(R.drawable.check_icon_835);
		} else {
			opponentPlayAgainText.setText(getResources()
					.getString(R.string.opponent_does_not_want_to_play_again,
							opponentName));
			opponentPlayAgainImageView
					.setImageResource(R.drawable.cancel_icon_18932);
			TextView playAgainText = (TextView) getView()
					.findViewById(R.id.play_again_text);
			playAgainText.setVisibility(View.GONE);

			playAgainButton.setVisibility(View.GONE);
			notPlayAgainButton.setText(R.string.opponent_left_exit_play_again);
		}
	}

	public void opponentWillPlayAgain() {
		updateOpponentPlayAgain(true);
		// if I already chose to play again, go ahead, otherwise wait till I
		// choose
		if (willPlayAgain) {
			dismiss();
			gameFragment.playAgainClosed();
			gameFragment.playAgain();
		}
	}

	public void opponentWillNotPlayAgain() {
		updateOpponentPlayAgain(false);
		// leave up the dialog so the user can see and react, but only press the
		// NO button
	}
}
