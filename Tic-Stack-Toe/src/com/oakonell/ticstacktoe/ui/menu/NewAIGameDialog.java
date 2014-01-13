package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;

public class NewAIGameDialog extends SherlockDialogFragment {

	public interface LocalAIGameModeListener {
		void chosenMode(int size, String aiName, int level);
	}

	private LocalAIGameModeListener listener;

	public void initialize(LocalAIGameModeListener listener) {
		this.listener = listener;
	}

	public static class AiDropDownItem {
		private final String text;
		private final int level;

		public AiDropDownItem(String string, int level) {
			this.level = level;
			this.text = string;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater
				.inflate(R.layout.dialog_local_ai, container, false);

		getDialog().setTitle(R.string.choose_ai_mode_title);

		List<AiDropDownItem> aiLevels = new ArrayList<NewAIGameDialog.AiDropDownItem>();
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_random), -1));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_easy), 1));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_medium), 2));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_hard), 3));

		final Spinner aiLevelSpinner = (Spinner) view
				.findViewById(R.id.ai_level);
		ArrayAdapter<AiDropDownItem> aiLevelAdapter = new ArrayAdapter<AiDropDownItem>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item,
				aiLevels);
		aiLevelSpinner.setAdapter(aiLevelAdapter);
		aiLevelSpinner.setSelection(1);

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AiDropDownItem selectedItem = (AiDropDownItem) aiLevelSpinner
						.getSelectedItem();

				String whiteName = selectedItem.text + " AI";

				dismiss();
				// TODO get size
				int size = 4;
				listener.chosenMode(size, whiteName, selectedItem.level);
			}
		});
		return view;
	}

}
