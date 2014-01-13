package com.oakonell.ticstacktoe;

import android.app.Application;
import android.content.Intent;

import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;

public class TicStackToe extends Application {
	private Achievements achievements = new Achievements();
	private Leaderboards leaderboards = new Leaderboards();
	private Intent settingsIntent;

	public Achievements getAchievements() {
		return achievements;
	}

	public Leaderboards getLeaderboards() {
		return leaderboards;
	}

	private Info info;

	public void setDevelopInfo(Info info) {
		this.info = info;
	}

	public Info getDevelopInfo() {
		return info;
	}

	public Intent getSettingsIntent() {
		return settingsIntent;
	}

	public void setSettingsIntent(Intent settingsIntent) {
		this.settingsIntent = settingsIntent;
	}
}