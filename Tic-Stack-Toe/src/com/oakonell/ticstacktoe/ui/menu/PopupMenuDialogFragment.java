package com.oakonell.ticstacktoe.ui.menu;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;

public class PopupMenuDialogFragment extends SherlockDialogFragment {
	private String[] items;
	private OnItemSelected onItemSelected;
	private View originatingView;

	public interface OnItemSelected {
		void onSelected(int position);
	}

	public void initialize(View originatingView, String[] items,
			OnItemSelected onItemSelected) {
		this.originatingView = originatingView;
		this.items = items;
		this.onItemSelected = onItemSelected;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));

		Window window = getDialog().getWindow();

		// set "origin" to top left corner, so to speak
		window.setGravity(Gravity.TOP | Gravity.LEFT);

		// get the originating view's position
		int pos[] = new int[2];
		originatingView.getLocationInWindow(pos);

		// after that, setting values for x and y works "naturally"
		WindowManager.LayoutParams params = window.getAttributes();
		final float scale = getActivity().getResources().getDisplayMetrics().density;
		int pixelWidth = (int) (100 * scale + 0.5f);
		params.x = pos[0];
		params.y = pos[1];
		params.width = pixelWidth;
		window.setAttributes(params);

		View popupView = inflater.inflate(R.layout.popup, container, true);
		ListView listView = (ListView) popupView.findViewById(R.id.list);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.popup_item, R.id.text, items) {
			@Override
			public View getView(final int position, View convertView,
					ViewGroup parent) {
				View view = super.getView(position, convertView, parent);

				view.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						getDialog().dismiss();
						onItemSelected.onSelected(position);
					}
				});
				return view;
			}
		};
		listView.setAdapter(adapter);

		return popupView;
	}
}
