package com.oakonell.ticstacktoe.settings;

import android.os.Bundle;

import com.oakonell.ticstacktoe.BuildConfig;
import com.oakonell.ticstacktoe.R;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.GenericAboutActivity;
import com.oakonell.utils.preference.CommonPreferences;
import com.oakonell.utils.preference.PrefsActivity;

public class SettingsActivity extends PrefsActivity {

	@Override
	public void onCreate(Bundle aSavedState) {
		super.onCreate(aSavedState);
		if (Utils.hasHoneycomb()) {
			addPreV11Resources();
		}
		getListView().setBackgroundResource(R.drawable.wood_10_);
		
	}

	@Override
	protected int[] getPreV11PreferenceResources() {
		if (BuildConfig.DEBUG) {
			return new int[] { R.xml.prefs_account, R.xml.prefs_develop,
					R.xml.prefs_about };
		}
		return new int[] { R.xml.prefs_account,
				R.xml.prefs_about };
	}

	@Override
	protected PreferenceConfigurer getPreV11PreferenceConfigurer() {
		if (BuildConfig.DEBUG) {
			return configureMultiple(new AccountPrefConfigurer(this,
					getPrefFinder()), new DevelopPrefConfigurer(this,
					getPrefFinder()), new CommonPreferences(this,
					getPrefFinder(), GenericAboutActivity.class));
		}
		return configureMultiple(new AccountPrefConfigurer(this,
				getPrefFinder()),
				new CommonPreferences(this, getPrefFinder(),
						GenericAboutActivity.class));
	}

	protected boolean isValidFragment(String fragmentName) {
		return true;
	}

}
