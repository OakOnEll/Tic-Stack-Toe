package com.oakonell.ticstacktoe.ui.local;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.ChatHelper;
import com.oakonell.ticstacktoe.GameStrategy;
import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.AbstractMove;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.ScoreCard;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler.OnLocalMatchUpdateListener;
import com.oakonell.ticstacktoe.ui.game.AbstractGameFragment;
import com.oakonell.ticstacktoe.ui.game.GameFragment;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public abstract class AbstractLocalStrategy extends GameStrategy {
	protected LocalMatchInfo matchInfo;

	public AbstractLocalStrategy(MainActivity mainActivity,
			SoundManager soundManager) {
		super(mainActivity, soundManager);

	}

	public AbstractLocalStrategy(MainActivity mainActivity,
			LocalMatchInfo localMatchInfo, SoundManager soundManager) {
		super(mainActivity, soundManager);
		this.matchInfo = localMatchInfo;
	}

	@Override
	public void leaveRoom() {
		saveToDB();
	}

	private void saveToDB() {
		DatabaseHandler db = new DatabaseHandler(getMainActivity());
		db.updateMatch(matchInfo, new OnLocalMatchUpdateListener() {
			@Override
			public void onUpdateSuccess(LocalMatchInfo matchInfo) {

			}

			@Override
			public void onUpdateFailure() {
				getMainActivity().getGameHelper().showAlert(
						"Error updating match to DB");
			}
		});
	}

	@Override
	public void sendMove(Game game, AbstractMove lastMove, ScoreCard score) {
		// TODO Auto-generated method stub
		// write the game to the DB, or wait until leave...??
		if (game.getBoard().getState().isOver()) {
			matchInfo.setMatchStatus(TurnBasedMatch.MATCH_STATUS_COMPLETE);
			saveToDB();
			return;
		}
		matchInfo
				.setTurnStatus(matchInfo.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN ? TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN
						: TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
	}

	@Override
	public void backFromWaitingRoom() {
		// do nothing
	}

	@Override
	public boolean warnToLeave() {
		return false;
	}

	public void leaveGame() {
		getMainActivity().getGameFragment().leaveGame();
		getMainActivity().getMenuFragment().leaveRoom();
	}

	@Override
	public void promptToPlayAgain(String winner, String title) {
		OnClickListener cancelListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				leaveGame();
			}

		};
		OnClickListener playAgainListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				playAgain();
			}

		};

		AlertDialog.Builder builder = new AlertDialog.Builder(getMainActivity());
		builder.setTitle(title);
		builder.setMessage(R.string.play_again);
		builder.setCancelable(false);

		builder.setNegativeButton(R.string.no, cancelListener);
		builder.setPositiveButton(R.string.yes, playAgainListener);

		AlertDialog dialog = builder.create();

		dialog.show();

	}

	abstract protected void playAgain();

	@Override
	public void onResume(MainActivity activity) {
		// do nothing
	}

	@Override
	public void onFragmentResume() {
		// do nothing
	}

	@Override
	public void onSignInSuccess(MainActivity activity) {
		// nothing to do, no dependency on being signed in
	}

	@Override
	public void onSignInFailed(MainActivity mainActivity) {
		// no worries
	}

	@Override
	public void showSettings(AbstractGameFragment fragment) {
		fragment.showFullSettingsPreference();
	}


	public void showFromMenu() {
		final ScoreCard score = new ScoreCard(0, 0, 0);
		GameFragment gameFragment = new GameFragment();
		gameFragment.startGame(matchInfo.readGame(getMainActivity()), score, null,
				true);

		FragmentManager manager = getMainActivity().getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.replace(R.id.main_frame, gameFragment,
				MainActivity.FRAG_TAG_GAME);
		transaction.addToBackStack(null);
		transaction.commit();

	}

}
