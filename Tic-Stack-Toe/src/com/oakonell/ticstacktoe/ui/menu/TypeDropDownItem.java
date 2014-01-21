package com.oakonell.ticstacktoe.ui.menu;

import com.oakonell.ticstacktoe.model.GameType;

public class TypeDropDownItem {
	public final String text;
	public final GameType type;

	public TypeDropDownItem(String string, GameType type) {
		this.type = type;
		this.text = string;
	}

	@Override
	public String toString() {
		return text;
	}
}