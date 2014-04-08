package com.oakonell.ticstacktoe.ui.local;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.game.RankedPlayAgain;

public class RankedAIPlayAgainFragment extends RankedPlayAgain {
	private RankedAIPlayAgainListener listener;
	private Button playAgain;
	private String winnerName;

	public interface RankedAIPlayAgainListener {
		void cancel();

		void playAgain();
	}

	public void initialize(RankedAIPlayAgainListener listener,
			String blackName, String whiteName, String winner, boolean isRanked) {
		initialize(isRanked, blackName, whiteName, true);
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

		onCreateRankView(view);

		return view;
	}

	public void updateRanks(short oldRank, short newRank, short oldAiRank,
			short newAiRank) {
		super.updateRanks(oldRank, newRank, oldAiRank, newAiRank);
		playAgain.setEnabled(true);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		listener.cancel();
	}

}
