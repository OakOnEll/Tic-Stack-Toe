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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.model.db.DatabaseHandler;
import com.oakonell.ticstacktoe.model.solver.AILevel;
import com.oakonell.ticstacktoe.rank.AIRankHelper;
import com.oakonell.ticstacktoe.rank.AIRankHelper.OnRanksRetrieved;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.TypeDropDownItem;

public class NewAIGameDialog extends SherlockDialogFragment {

	public interface LocalAIGameModeListener {
		void chosenMode(GameType type, String aiName, AILevel level,
				boolean isRanked);

		void cancel();
	}

	private LocalAIGameModeListener listener;
	private DatabaseHandler handler;

	public void initialize(LocalAIGameModeListener listener) {
		this.listener = listener;
	}

	public static class AiDropDownItem {
		private final AILevel level;
		private String text;

		public AiDropDownItem(String string, AILevel level) {
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
				R.string.ai_random), AILevel.RANDOM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_easy), AILevel.EASY_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_medium), AILevel.MEDIUM_AI));
		aiLevels.add(new AiDropDownItem(getResources().getString(
				R.string.ai_hard), AILevel.HARD_AI));

		final Spinner aiLevelSpinner = (Spinner) view
				.findViewById(R.id.ai_level);
		final ArrayAdapter<AiDropDownItem> aiLevelAdapter = new ArrayAdapter<AiDropDownItem>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item,
				aiLevels);
		aiLevelSpinner.setAdapter(aiLevelAdapter);
		aiLevelSpinner.setSelection(1);

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		GameTypeSpinnerHelper.populateSpinner(getActivity(), typeSpinner);
		final CheckBox ranked = (CheckBox) view.findViewById(R.id.ranked_game);
		final TextView typeDescr = (TextView) view.findViewById(R.id.game_type_descr);
		
		ranked.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				GameType type = ((TypeDropDownItem) typeSpinner
						.getSelectedItem()).type;
				retrieveRanks(type, aiLevels, aiLevelAdapter, isChecked);
			}
		});

		handler = new DatabaseHandler(getActivity());
		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				GameType type = ((TypeDropDownItem) typeSpinner
						.getSelectedItem()).type;
				GameTypeSpinnerHelper.populateDescription(getActivity(), typeDescr, type);
				retrieveRanks(type, aiLevels, aiLevelAdapter,
						ranked.isChecked());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		GameType type = ((TypeDropDownItem) typeSpinner.getSelectedItem()).type;
		GameTypeSpinnerHelper.populateDescription(getActivity(), typeDescr, type);
		retrieveRanks(type, aiLevels, aiLevelAdapter, ranked.isChecked());

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AiDropDownItem selectedItem = (AiDropDownItem) aiLevelSpinner
						.getSelectedItem();

				TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();

				int label = 0;
				switch (selectedItem.level) {
				case RANDOM_AI:
					label = R.string.ai_random;
					break;
				case EASY_AI:
					label = R.string.ai_easy;
					break;
				case MEDIUM_AI:
					label = R.string.ai_medium;
					break;
				case HARD_AI:
					label = R.string.ai_hard;
					break;
				}

				String whiteName = getResources().getString(label) + " AI";

				dismiss();
				listener.chosenMode(typeItem.type, whiteName,
						selectedItem.level, ranked.isChecked());
			}
		});
		return view;
	}

	private void retrieveRanks(GameType type,
			final List<AiDropDownItem> aiLevels,
			final ArrayAdapter<AiDropDownItem> aiLevelAdapter,
			boolean includeRanks) {
		if (!includeRanks) {
			for (AiDropDownItem each : aiLevels) {
				int label = 0;
				switch (each.level) {
				case RANDOM_AI:
					label = R.string.ai_random;
					break;
				case EASY_AI:
					label = R.string.ai_easy;
					break;
				case MEDIUM_AI:
					label = R.string.ai_medium;
					break;
				case HARD_AI:
					label = R.string.ai_hard;
					break;
				}
				each.text = getResources().getString(label);
			}
			aiLevelAdapter.notifyDataSetChanged();
			return;
		}
		// TODO show a progress dialog and clear when success
		AIRankHelper.retrieveRanks(handler, type, new OnRanksRetrieved() {
			@Override
			public void onSuccess(Map<AILevel, Integer> ranks) {
				for (AiDropDownItem each : aiLevels) {
					int rank = 0;
					int label = 0;
					switch (each.level) {
					case RANDOM_AI:
						label = R.string.ai_random;
						rank = ranks.get(AILevel.RANDOM_AI);
						break;
					case EASY_AI:
						label = R.string.ai_easy;
						rank = ranks.get(AILevel.EASY_AI);
						break;
					case MEDIUM_AI:
						label = R.string.ai_medium;
						rank = ranks.get(AILevel.MEDIUM_AI);
						break;
					case HARD_AI:
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
