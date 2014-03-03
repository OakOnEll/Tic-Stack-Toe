package com.oakonell.ticstacktoe.ui.game;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;

public class GameStatDialogFragment extends SherlockDialogFragment {
	private GameFragment parent;
	private ScoreCard score;
	private Game game;

	private ImageManager imgManager;

	public void initialize(GameFragment parent, Game game, ScoreCard score) {
		this.parent = parent;
		this.score = score;
		this.game = game;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.game_stats_dialog, container,
				false);
		getDialog().setTitle(R.string.game_stats_title);
		imgManager = ImageManager.create(parent.getActivity());

		ImageView playerBlack = (ImageView) view.findViewById(R.id.player_x);
		game.getBlackPlayer().updatePlayerImage(imgManager, playerBlack);

		ImageView playerWhite = (ImageView) view.findViewById(R.id.player_o);
		game.getWhitePlayer().updatePlayerImage(imgManager, playerWhite);

		TextView blackNameText = (TextView) view.findViewById(R.id.x_name);
		blackNameText.setText(game.getBlackPlayer().getName());

		TextView whiteNameText = (TextView) view.findViewById(R.id.o_name);
		whiteNameText.setText(game.getWhitePlayer().getName());

		TextView whiteWinsText = (TextView) view.findViewById(R.id.o_wins);
		TextView whiteLossesText = (TextView) view.findViewById(R.id.o_losses);
		whiteWinsText.setText(score.getWhiteWins() + "");
		whiteLossesText.setText(score.getBlackWins() + "");

		TextView blackWinsText = (TextView) view.findViewById(R.id.x_wins);
		TextView blackLossesText = (TextView) view.findViewById(R.id.x_losses);
		blackWinsText.setText(score.getBlackWins() + "");
		blackLossesText.setText(score.getWhiteWins() + "");

		TextView numDraws = (TextView) view.findViewById(R.id.num_draws);
		numDraws.setText(parent.getResources().getQuantityString(
				R.plurals.num_draws_with_label, score.getDraws(),
				score.getDraws()));

		View ok = view.findViewById(R.id.ok_button);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				getDialog().dismiss();
				parent.gameStatsClosed();
			}
		});

		return view;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		dialog.getWindow().getAttributes().windowAnimations = R.style.game_stats_dialog_Window;

		return dialog;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		parent.gameStatsClosed();
	}
}
