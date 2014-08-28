package com.oakonell.ticstacktoe.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.oakonell.ticstacktoe.BuildConfig;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.utils.BillingHelper;
import com.oakonell.utils.Utils;
import com.oakonell.utils.activity.GenericAboutActivity;
import com.oakonell.utils.preference.CommonPreferences;
import com.oakonell.utils.preference.PrefsActivity;

public class SettingsActivity extends PrefsActivity {
	private static final String TAG = "SettingsActivity";

	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;

	protected BillingHelper mHelper;

	@Override
	protected void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		// Pass on the activity result to the helper for handling
		if (request == RC_REQUEST) {
			if (mHelper != null
					&& mHelper.handleActivityResult(request, response, data)) {
				Log.d(TAG, "onActivityResult handled by IABUtil.");
			}
			Log.d(TAG,
					"onActivityResult for billing request was not handled by IABUtil.");
			return;
		}

	}

	@Override
	public void onCreate(Bundle aSavedState) {
		super.onCreate(aSavedState);
		if (Utils.hasHoneycomb()) {
			addPreV11Resources();
		}
		getWindow().setBackgroundDrawableResource(R.drawable.background);
	}

	@Override
	protected int[] getPreV11PreferenceResources() {
		if (BuildConfig.DEBUG) {
			return new int[] { R.xml.prefs_account, R.xml.prefs_develop,
					R.xml.prefs_about };
		}
		return new int[] { R.xml.prefs_account, R.xml.prefs_about };
	}

	@Override
	protected PreferenceConfigurer getPreV11PreferenceConfigurer() {
		if (BuildConfig.DEBUG) {
			return configureMultiple(new AccountPrefConfigurer(this,
					getPrefFinder()), new DevelopPrefConfigurer(this,
					getPrefFinder()), new CommonPreferences(this,
					getPrefFinder(), GenericAboutActivity.class),
					new PremiumConfigurer(this, getPrefFinder()));
		}
		return configureMultiple(new AccountPrefConfigurer(this,
				getPrefFinder()), new CommonPreferences(this, getPrefFinder(),
				GenericAboutActivity.class), new PremiumConfigurer(this,
				getPrefFinder()));
	}

	protected boolean isValidFragment(String fragmentName) {
		return true;
	}

}
