package com.oakonell.ticstacktoe.ui;

import android.annotation.TargetApi;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.utils.Utils;

public class WoodenAlertDialog extends SherlockDialogFragment {
	private String title;
	private String message;
	android.content.DialogInterface.OnClickListener onClickListener;
	OnCancelListener onCancelListener;
	private boolean shouldRotate;

	protected void initialize(String title, String message,
			android.content.DialogInterface.OnClickListener onClickListener,
			OnCancelListener onCancelListener, boolean shouldRotate) {
		this.title = title;
		this.message = message;
		this.onClickListener = onClickListener;
		this.onCancelListener = onCancelListener;
		this.shouldRotate = shouldRotate;
	}

	@Override
	public final View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));
		final View view = inflater.inflate(R.layout.alert, container, false);
		getDialog().setCancelable(false);

		TextView messageView = (TextView) view.findViewById(R.id.message);
		messageView.setText(message);
		messageView.setMovementMethod(new ScrollingMovementMethod());
		
		setTitle(view, title);

		Button okButton = (Button) view.findViewById(R.id.ok);
		okButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onClickListener != null) {
					onClickListener.onClick(WoodenAlertDialog.this.getDialog(),
							0);
				}
				dismiss();
			}
		});
		if (shouldRotate && canRotate()) {
			rotateView(view);
		}
		return view;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void rotateView(View view) {
		view.setRotation(180);
	}

	private boolean canRotate() {
		return Utils.hasHoneycomb();
	}

	protected void setTitle(final View view, String title) {
		((TextView) view.findViewById(R.id.title)).setText(title);
		getDialog().setTitle(title);
	}

	public static WoodenAlertDialog show(Fragment fragment, String title,
			String message,
			android.content.DialogInterface.OnClickListener onClickListener,
			OnCancelListener onCancelListener) {
		return show(fragment, title, message, onClickListener,
				onCancelListener, false);
	}

	public static WoodenAlertDialog show(Fragment fragment, String title,
			String message,
			android.content.DialogInterface.OnClickListener onClickListener,
			OnCancelListener onCancelListener, boolean shouldRotate) {
		WoodenAlertDialog progress = new WoodenAlertDialog();
		progress.initialize(title, message, onClickListener, onCancelListener,
				shouldRotate);
		progress.setCancelable(false);

		progress.show(fragment.getChildFragmentManager(), "alert");

		return progress;
	}
}
