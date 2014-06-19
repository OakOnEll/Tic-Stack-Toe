package com.oakonell.ticstacktoe.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper.OnIabSetupFinishedListener;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabHelper.QueryInventoryFinishedListener;
import com.oakonell.ticstacktoe.googleapi.inappbill.IabResult;
import com.oakonell.ticstacktoe.googleapi.inappbill.Inventory;
import com.oakonell.ticstacktoe.googleapi.inappbill.Purchase;

public class BillingHelper {
	public static final String SKU_PREMIUM = "remove_ads";
	private static final String TAG = "BillingHelper";

	private IabHelper mHelper;
	// TODO this should be unique per user!
	private String premium_payload = "premium...";

	private Activity activity;

	public BillingHelper(Activity activity) {
		Log.d(TAG, "Creating IAB helper.");
		this.activity = activity;
		mHelper = new IabHelper(activity, getPublicKey());
		mHelper.enableDebugLogging(true);

	}

	public void purchaseUpgrade(final Activity activity, int request,
			final IabHelper.OnIabPurchaseFinishedListener listener) {

		// Callback for when a purchase is finished
		IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
			public void onIabPurchaseFinished(IabResult result,
					Purchase purchase) {
				Log.d(TAG, "Purchase finished: " + result + ", purchase: "
						+ purchase);

				// if we were disposed of in the meantime, quit.
				if (mHelper == null)
					return;

				if (result.isFailure()
						&& result.getResponse() != IabHelper.IABHELPER_USER_CANCELLED) {
					complain("Error purchasing: " + result);
					// setWaitScreen(false);
					return;
				}
				if (purchase == null) {
					return;
				}
				
				if (!verifyDeveloperPayload(purchase)) {
					complain("Error purchasing. Authenticity verification failed.");
					// setWaitScreen(false);
					return;
				}

				Log.d(TAG, "Purchase successful.");

				if (purchase.getSku().equals(BillingHelper.SKU_PREMIUM)) {
					// bought the premium upgrade!
					Log.d(TAG,
							"Purchase is premium upgrade. Congratulating user.");
					alert("Thank you for upgrading to premium!");

					SharedPreferences sharedPrefs = PreferenceManager
							.getDefaultSharedPreferences(activity);
					Editor edit = sharedPrefs.edit();
					edit.putBoolean(
							activity.getString(R.string.pref_premium_key), true);
					edit.apply();

				} else {
					// unknown purchase
					Log.d(TAG, "Unknown purchase was returned... ignoring");
				}

				if (listener != null) {
					listener.onIabPurchaseFinished(result, purchase);
				}
			}
		};

		mHelper.launchPurchaseFlow(activity, SKU_PREMIUM, request,
				mPurchaseFinishedListener, premium_payload);

	}

	public boolean handleActivityResult(int request, int response, Intent data) {
		return mHelper.handleActivityResult(request, response, data);
	}

	public void dispose() {
		mHelper.dispose();
	}

	public void startSetup(OnIabSetupFinishedListener onIabSetupFinishedListener) {
		mHelper.startSetup(onIabSetupFinishedListener);
	}

	public void queryInventoryAsync(
			final QueryInventoryFinishedListener listener) {

		IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {

			public void onQueryInventoryFinished(IabResult result,
					Inventory inventory) {
				Log.d(TAG, "Query inventory finished.");

				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null)
					return;

				// Is it a failure?
				if (result.isFailure()) {
					complain("Failed to query inventory: " + result);
					return;
				}

				Log.d(TAG, "Query inventory was successful.");

				/*
				 * Check for items we own. Notice that for each purchase, we
				 * check the developer payload to see if it's correct! See
				 * verifyDeveloperPayload().
				 */

				// Do we have the premium upgrade?
				Purchase premiumPurchase = inventory
						.getPurchase(BillingHelper.SKU_PREMIUM);
				if (premiumPurchase != null
						&& !verifyDeveloperPayload(premiumPurchase)) {
					complain("Invalid premium purchase!");
				}
				Log.d(TAG,
						"Initial inventory query finished; calling secondary listener.");
				if (listener != null) {
					listener.onQueryInventoryFinished(result, inventory);
				}
			}
		};

		mHelper.queryInventoryAsync(mGotInventoryListener);
	}

	/** Verifies the developer payload of a purchase. */
	boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();

		/*
		 * TODO: verify that the developer payload of the purchase is correct.
		 * It will be the same one that you sent when initiating the purchase.
		 * 
		 * WARNING: Locally generating a random string when starting a purchase
		 * and verifying it here might seem like a good approach, but this will
		 * fail in the case where the user purchases an item on one device and
		 * then uses your app on a different device, because on the other device
		 * you will not have access to the random string you originally
		 * generated.
		 * 
		 * So a good developer payload has these characteristics:
		 * 
		 * 1. If two different users purchase an item, the payload is different
		 * between them, so that one user's purchase can't be replayed to
		 * another user.
		 * 
		 * 2. The payload must be such that you can verify it even when the app
		 * wasn't the one who initiated the purchase flow (so that items
		 * purchased by the user on one device work on other devices owned by
		 * the user).
		 * 
		 * Using your own server to store and verify developer payloads across
		 * app installations is recommended.
		 */

		return true;
	}

	void complain(String message) {
		Log.e(TAG, "****  Error: " + message);
		alert("Error: " + message);
	}

	void alert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(TAG, "Showing alert dialog: " + message);
		bld.create().show();
	}

	public static String getPublicKey() {
		return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyPlmnk70AzLTBT59JoASBbg+reJVHYU3BB7qypm5Er8leaj"
				+ "MmQOiPdPeU1eLCyr0bx09LSEZpN3K3HgwIU161w2mL1VPp3cMUDgFixuzN+eP6tsDSxjW1nws6NB3TNsJ235OdRRR1MbU4CzdU74OCcWsF2Skw"
				+ "tOLE9aUAcPUJe/6cOp8FZEIyx/JiiofOXcUuSrAtpy9JY9JS1P74Vu0wVhZ7oSxVUomAMYFIoJnwwst1gQZEICf+/vj6URl4SLXNcgcKCU8hcC9vUQq7u"
				+ "aOn1lulNadJ5MfzPp7LntRg96EpA2Tuoz5MROGlHtCeevSi1d4Qf7PVE+UImdTrE04mwIDAQAB";
	}

}
