package com.oakonell.ticstacktoe.ui.local;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;

public class RankedAIPlayAgainFragment extends SherlockDialogFragment {
	private RankedAIPlayAgainListener listener;
	private String winnerName;
	private String whiteName;
	private String blackName;
	private TextView originalRankText;
	private TextView newRankText;
	private TextView originalAiRankText;
	private TextView newAiRankText;
	private Button playAgain;
	private ProgressBar waiting;

	public interface RankedAIPlayAgainListener {
		void cancel();

		void playAgain();
	}

	public void initialize(RankedAIPlayAgainListener listener,
			String blackName, String whiteName, String winner) {
		this.blackName = blackName;
		this.whiteName = whiteName;
		this.listener = listener;
		this.winnerName = winner;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(
				R.layout.ranked_ai_play_again_fragment, container, false);
		getDialog().setCancelable(false);
		getDialog().setTitle(winnerName + " Won!");

		playAgain = (Button) view.findViewById(R.id.play_again);
		playAgain.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				listener.playAgain();
			}
		});
		playAgain.setEnabled(false);
		Button cancel = (Button) view.findViewById(R.id.cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				listener.cancel();
			}
		});

		TextView blackNameText = (TextView) view.findViewById(R.id.player_name);
		blackNameText.setText(blackName);
		TextView whiteNameText = (TextView) view.findViewById(R.id.ai_name);
		whiteNameText.setText(whiteName);

		originalRankText = (TextView) view.findViewById(R.id.original_rank);
		newRankText = (TextView) view.findViewById(R.id.new_rank);
		originalAiRankText = (TextView) view
				.findViewById(R.id.original_ai_rank);
		newAiRankText = (TextView) view.findViewById(R.id.new_ai_rank);

		originalRankText.setText("--");
		originalAiRankText.setText("--");
		newRankText.setText("--");
		newAiRankText.setText("--");

		waiting = (ProgressBar) view.findViewById(R.id.waiting);
		waiting.setVisibility(view.VISIBLE);
		return view;
	}

	public void updateRanks(short oldRank, short newRank, short oldAiRank,
			short newAiRank) {
		if (!isVisible()) {
			return;
		}
		originalRankText.setText(oldRank + "");
		originalAiRankText.setText(oldAiRank + "");
		newRankText.setText(newRank + "");
		newAiRankText.setText(newAiRank + "");
		waiting.setVisibility(View.GONE);
		playAgain.setEnabled(true);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		listener.cancel();
	}

}
