package com.oakonell.ticstacktoe.settings;

import android.app.Activity;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.oakonell.ticstacktoe.TicStackToe;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil.Info;
import com.oakonell.utils.preference.PrefsActivity.PreferenceConfigurer;
import com.oakonell.utils.preference.PrefsActivity.PreferenceFinder;

public class DevelopPrefConfigurer implements PreferenceConfigurer {
	private PreferenceFinder finder;
	private Activity activity;

	DevelopPrefConfigurer(Activity activity, PreferenceFinder finder) {
		this.finder = finder;
		this.activity = activity;
	}

	@Override
	public void configure() {
		Preference deleteRanks = finder.findPreference("delete_ranks");
		deleteRanks
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						TicStackToe app = (TicStackToe) activity
								.getApplication();
						Info info = app.getDevelopInfo();
						if (info == null) {
							Toast.makeText(activity, "Not connected",
									Toast.LENGTH_SHORT).show();
							;
							return true;
						}

						DevelopmentUtil.deleteRanks(activity, info);
						return true;
					}
				});

		Preference resetAchievements = finder
				.findPreference("reset_achievements");
		resetAchievements
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						TicStackToe app = (TicStackToe) activity
								.getApplication();
						Info info = app.getDevelopInfo();
						if (info == null) {
							Toast.makeText(activity, "Not connected",
									Toast.LENGTH_SHORT).show();
							;
							return true;
						}

						DevelopmentUtil.resetAchievements(activity, info);
						return true;
					}
				});

		Preference resetLeaderboards = finder
				.findPreference("reset_leaderboard");
		resetLeaderboards
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						TicStackToe app = (TicStackToe) activity
								.getApplication();
						Info info = app.getDevelopInfo();
						if (info == null) {
							Toast.makeText(activity, "Not connected",
									Toast.LENGTH_SHORT).show();
							return true;
						}

						DevelopmentUtil.resetLeaderboards(activity, info);
						return true;
					}
				});



	}

}
