package com.oakonell.ticstacktoe.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;

public class WoodenProgressDialog extends SherlockDialogFragment {
	private String title;
	private String message;

	protected void initialize(String title, String message) {
		this.title = title;
		this.message = message;
	}

	@Override
	public final View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));
		final View view = inflater.inflate(R.layout.progress, container, false);
		getDialog().setCancelable(false);

		TextView messageView = (TextView) view.findViewById(R.id.message);
		messageView.setText(message);

		setTitle(view, title);
		return view;
	}

	protected void setTitle(final View view, String title) {
		((TextView) view.findViewById(R.id.title)).setText(title);
		getDialog().setTitle(title);
	}

	public static WoodenProgressDialog show(Fragment fragment, String title,
			String message) {
		WoodenProgressDialog progress = new WoodenProgressDialog();
		progress.initialize(title, message);
		progress.setCancelable(false);
		progress.show(fragment.getChildFragmentManager(), "progress");

		return progress;
	}
}
