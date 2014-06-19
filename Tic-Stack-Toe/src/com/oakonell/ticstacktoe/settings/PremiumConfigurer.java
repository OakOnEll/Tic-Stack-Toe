package com.oakonell.ticstacktoe.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper.OnConsumeFinishedListener;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper.OnIabPurchaseFinishedListener;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabResult;
import com.oakonell.ticstacktoe.googleapi.inappbill.Purchase;
import com.oakonell.ticstacktoe.utils.BillingHelper;
import com.oakonell.ticstacktoe.utils.DevelopmentUtil;
import com.oakonell.utils.preference.PrefsActivity.PreferenceConfigurer;
import com.oakonell.utils.preference.PrefsActivity.PreferenceFinder;

public class PremiumConfigurer implements PreferenceConfigurer {
	private static final String LogTag = "PremiumConfigurer";
	private PreferenceFinder finder;
	private SettingsActivity activity;

	PremiumConfigurer(Activity activity, PreferenceFinder finder) {
		this.finder = finder;
		this.activity = (SettingsActivity) activity;
	}

	@Override
	public void configure() {
		Preference premium = finder.findPreference(activity
				.getString(R.string.pref_premium_key));
		boolean isPremium = premium.getSharedPreferences().getBoolean(
				activity.getString(R.string.pref_premium_key), false);

		if (!isPremium) {
			premium.setTitle(activity
					.getString(R.string.pref__upgrade_to_premium));

			premium.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					upgradeToPremium();
					return true;
				}
			});

		} else {
			premium.setTitle(activity.getString(R.string.pref_is_premium));
			premium.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					return true;
				}
			});
		}

		Preference downgrade_from_premium = finder
				.findPreference("downgrade_from_premium");
		if (isPremium) {
			downgrade_from_premium.setEnabled(true);
			downgrade_from_premium
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
							try {
								DevelopmentUtil.downgradeFromPremium(activity,
										new OnConsumeFinishedListener() {
											@Override
											public void onConsumeFinished(
													Purchase purchase,
													IabResult result) {
												configure();
											}
										});
							} catch (Exception e) {
								complain(activity, "Exception downgrading..."
										+ e.getMessage());
							}
							return true;
						}
					});
		} else {
			downgrade_from_premium.setEnabled(false);
		}
	}

	private void upgradeToPremium() {
		activity.mHelper = new BillingHelper(activity);

		activity.mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(LogTag, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					String message = "Problem setting up in-app billing: "
							+ result;
					complain(activity, message);

					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				// if (mHelper == null) {
				// return;
				// }

				// IAB is fully set up. Now, let's get an inventory of stuff we
				// own.
				Log.d(LogTag, "Billing Setup successful. Upgrading to premium.");
				activity.mHelper.purchaseUpgrade(activity,
						SettingsActivity.RC_REQUEST,
						new OnIabPurchaseFinishedListener() {

							@Override
							public void onIabPurchaseFinished(IabResult result,
									Purchase info) {
								configure();
							}
						});
			}

		});

	}

	private static void complain(final Activity activity, String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(LogTag, "Showing alert dialog: " + message);
		bld.create().show();
	}

}
