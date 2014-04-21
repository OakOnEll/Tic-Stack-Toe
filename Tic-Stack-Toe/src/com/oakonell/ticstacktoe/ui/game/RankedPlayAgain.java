package com.oakonell.ticstacktoe.ui.game;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;

public abstract class RankedPlayAgain extends SherlockDialogFragment {
	private boolean isRanked;
	private String whiteName;
	private String blackName;
	private TextView originalRankText;
	private TextView newRankText;
	private TextView originalAiRankText;
	private TextView newAiRankText;
	private ProgressBar waiting;

	protected void initialize(boolean isRanked, String myName,
			String opponentName, boolean iAmBlack) {
		if (iAmBlack) {
			whiteName = opponentName;
			blackName = myName;
		} else {
			blackName = opponentName;
			whiteName = myName;
		}
		this.isRanked = isRanked;
	}

	protected boolean isRanked() {
		return isRanked;
	}

	protected void setTitle(final View view, String title) {
		((TextView) view.findViewById(R.id.title)).setText(title);
		getDialog().setTitle(title);
	}

	@Override
	public final View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));
		final View view = inflater.inflate(getMainLayoutResId(),
				container, false);
		getDialog().setCancelable(false);
		onCreateRankView(view);
		
		continueCreateView(container, view, savedInstanceState);
		
		return view;
	}

	protected void continueCreateView(ViewGroup container, View view,
			Bundle savedInstanceState) {
		
	}

	abstract protected int getMainLayoutResId() ;
	
	protected void onCreateRankView(final View view) {
		if (isRanked) {
			TextView blackNameText = (TextView) view
					.findViewById(R.id.player_name);
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
			waiting.setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.rank_layout).setVisibility(View.GONE);
		}
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
	}

}
