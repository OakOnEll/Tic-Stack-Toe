package com.oakonell.ticstacktoe.model;

import java.util.List;

import android.net.Uri;
import android.widget.ImageView;

import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.Board.PieceStack;

public class Player {
	private final String name;
	private final Uri iconImageUri;
	private final PlayerStrategy strategy;
	private Player opponent;
	private Game game;

	public Player(String name, Uri iconImageUri, PlayerStrategy strategy) {
		this.name = name;
		this.iconImageUri = iconImageUri;
		this.strategy = strategy;
	}

	public void setOpponent(Player opponent) {
		this.opponent = opponent;
	}

	public String getName() {
		return name;
	}

	public Uri getIconImageUri() {
		return iconImageUri;
	}

	public boolean isBlack() {
		return strategy.isBlack();
	}

	public Player opponent() {
		return opponent;
	}

	public PlayerStrategy getStrategy() {
		return strategy;
	}

	public void updatePlayerImage(ImageManager imgManager, ImageView image) {
		int defaultResource = strategy.isBlack() ? R.color.abs__background_holo_dark
				: R.color.abs__background_holo_light;
		if (iconImageUri == null
				|| iconImageUri.getEncodedSchemeSpecificPart().contains(
						"gms.games")) {
			imgManager.loadImage(image, iconImageUri, defaultResource);
		} else {
			image.setImageURI(iconImageUri);

		}
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public List<PieceStack> getPlayerPieces() {
		return isBlack() ? game.getBlackPlayerPieces() : game
				.getWhitePlayerPieces();
	}

	public String toString() {
		return getName() + "(" + (isBlack() ? "Black" : "White") + ")";
	}
}
