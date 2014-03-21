package com.oakonell.ticstacktoe.ui.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.model.solver.AiPlayerStrategy;
import com.oakonell.ticstacktoe.query.AIRanksRetreiver;
import com.oakonell.ticstacktoe.query.AIRanksRetreiver.OnRanksRetrieved;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.TypeDropDownItem;

public class NewAIGameDialog extends SherlockDialogFragment {

	public interface LocalAIGameModeListener {
		void chosenMode(GameType type, String aiName, int level);

		void cancel();
	}

	private LocalAIGameModeListener listener;
	private DatabaseHandler handler;

	public void initialize(LocalAIGameModeListener listener) {
		this.listener = listener;
	}

	public static class AiDropDownItem {
		private final int level;
		private String text;

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

		final List<AiDropDownItem> aiLevels = new ArrayList<NewAIGameDialog.AiDropDownItem>();
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_random), AiPlayerStrategy.RANDOM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_easy), AiPlayerStrategy.EASY_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_medium), AiPlayerStrategy.MEDIUM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_hard), AiPlayerStrategy.HARD_AI));

		final Spinner aiLevelSpinner = (Spinner) view
				.findViewById(R.id.ai_level);
		final ArrayAdapter<AiDropDownItem> aiLevelAdapter = new ArrayAdapter<AiDropDownItem>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item,
				aiLevels);
		aiLevelSpinner.setAdapter(aiLevelAdapter);
		aiLevelSpinner.setSelection(1);

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		GameTypeSpinnerHelper.populateSpinner(getActivity(), typeSpinner);

		handler = new DatabaseHandler(getActivity());
		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				GameType type = ((TypeDropDownItem) typeSpinner
						.getSelectedItem()).type;
				retrieveRanks(type, aiLevels, aiLevelAdapter);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		GameType type = ((TypeDropDownItem) typeSpinner.getSelectedItem()).type;
		retrieveRanks(type, aiLevels, aiLevelAdapter);

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AiDropDownItem selectedItem = (AiDropDownItem) aiLevelSpinner
						.getSelectedItem();

				TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();

				String whiteName = selectedItem.text + " AI";

				dismiss();
				listener.chosenMode(typeItem.type, whiteName,
						selectedItem.level);
			}
		});
		return view;
	}

	private void retrieveRanks(GameType type,
			final List<AiDropDownItem> aiLevels,
			final ArrayAdapter<AiDropDownItem> aiLevelAdapter) {
		AIRanksRetreiver.retrieveRanks(handler, type, new OnRanksRetrieved() {
			@Override
			public void onSuccess(Map<AILevel, Integer> ranks) {
				for (AiDropDownItem each : aiLevels) {
					int rank = 0;
					int label = 0;
					switch (each.level) {
					case AiPlayerStrategy.RANDOM_AI:
						label = R.string.ai_random;
						rank = ranks.get(AILevel.RANDOM_AI);
						break;
					case AiPlayerStrategy.EASY_AI:
						label = R.string.ai_easy;
						rank = ranks.get(AILevel.EASY_AI);
						break;
					case AiPlayerStrategy.MEDIUM_AI:
						label = R.string.ai_medium;
						rank = ranks.get(AILevel.MEDIUM_AI);
						break;
					case AiPlayerStrategy.HARD_AI:
						label = R.string.ai_hard;
						rank = ranks.get(AILevel.HARD_AI);
						break;
					}
					each.text = getResources().getString(label) + ": " + rank;
				}
				aiLevelAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		listener.cancel();
	}

}
