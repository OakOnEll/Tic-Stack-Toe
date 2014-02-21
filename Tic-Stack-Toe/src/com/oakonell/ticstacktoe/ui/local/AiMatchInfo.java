package com.oakonell.ticstacktoe.ui.local;

import com.oakonell.ticstacktoe.MainActivity;
import com.oakonell.ticstacktoe.model.Game;
import com.oakonell.ticstacktoe.model.Player;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.ui.game.SoundManager;

public class AiMatchInfo extends LocalMatchInfo {

	private int aiLevel;

	public AiMatchInfo(long id, int matchStatus, int turnStatus,
			String blackName, String whiteName, int aiLevel, long lastUpdated,
			String fileName) {
		super(id, matchStatus, turnStatus, blackName, whiteName, lastUpdated,
				fileName);
		this.aiLevel = aiLevel;
	}

	public AiMatchInfo(int matchStatus, int turnStatus, String blackName,
			String whiteName, int aiLevel, long currentTimeMillis, Game game) {
		super(matchStatus, turnStatus, blackName, whiteName, game);
		this.aiLevel = aiLevel;
	}

	@Override
	public void accept(LocalMatchVisitor visitor) {
		visitor.visitAi(this);
	}

	public int getWhiteAILevel() {
		return aiLevel;
	}

	@Override
	public AbstractLocalStrategy createStrategy(MainActivity activity,
			SoundManager soundManager) {
		AiGameStrategy listener = new AiGameStrategy(activity, soundManager,
				aiLevel, this);
		return listener;
	}

	@Override
	protected Player createWhitePlayerStrategy() {
		return AiPlayerStrategy.createWhitePlayer(getWhiteName(), false,
				aiLevel);
	}

}
