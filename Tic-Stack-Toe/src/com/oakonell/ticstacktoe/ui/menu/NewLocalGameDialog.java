package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.utils.StringUtils;

public class NewLocalGameDialog extends SherlockDialogFragment {
	private String blackName;
	private String whiteName;

	public interface LocalGameModeListener {
		void chosenMode(GameType type, String blackName, String whiteName);
	}

	private LocalGameModeListener listener;

	public void initialize(LocalGameModeListener listener) {
		this.listener = listener;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_local_game, container,
				false);

		getDialog().setTitle(R.string.choose_local_game_mode_title);
		defaultNamesFromPreferences();

		ImageButton switchPlayers = (ImageButton) view
				.findViewById(R.id.switch_players);
		final EditText blackNameText = (EditText) view
				.findViewById(R.id.player_x_name);
		final EditText whiteNameText = (EditText) view
				.findViewById(R.id.player_o_name);

		blackNameText.setText(blackName);
		whiteNameText.setText(whiteName);

		OnFocusChangeListener onNameFocusChangeListener = new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					validate(blackNameText, whiteNameText);
				}
			}
		};
		blackNameText.setOnFocusChangeListener(onNameFocusChangeListener);
		blackNameText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				blackNameText.setError(null);
			}
		});
		whiteNameText.setOnFocusChangeListener(onNameFocusChangeListener);
		whiteNameText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				whiteNameText.setError(null);
			}
		});

		switchPlayers.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Editable temp = blackNameText.getText();
				blackNameText.setText(whiteNameText.getText());
				whiteNameText.setText(temp);
			}
		});

		List<TypeDropDownItem> types = new ArrayList<TypeDropDownItem>();
		types.add(new TypeDropDownItem(getResources().getString(
				R.string.type_junior), GameType.JUNIOR));
		types.add(new TypeDropDownItem(getResources().getString(
				R.string.type_easy), GameType.EASY));
		types.add(new TypeDropDownItem(getResources().getString(
				R.string.type_strict), GameType.REGULAR));

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		ArrayAdapter<TypeDropDownItem> typeAdapter = new ArrayAdapter<TypeDropDownItem>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item,
				types);
		typeSpinner.setAdapter(typeAdapter);

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);

				TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();

				if (!validate(blackNameText, whiteNameText)) {
					return;
				}
				blackName = blackNameText.getText().toString();
				whiteName = whiteNameText.getText().toString();
				writeNamesToPreferences();

				dismiss();
				listener.chosenMode(typeItem.type, blackName, whiteName);
			}
		});

		return view;

	}

	protected boolean validate(EditText blackNameText, EditText whiteNameText) {
		boolean isValid = true;
		String blackName = blackNameText.getText().toString();
		if (StringUtils.isEmpty(blackName)) {
			isValid = false;
			blackNameText.setError(getResources().getString(
					R.string.error_black_name));
		}
		String whiteName = whiteNameText.getText().toString();
		if (StringUtils.isEmpty(whiteName)) {
			isValid = false;
			whiteNameText.setError(getResources().getString(
					R.string.error_white_name));
		}

		if (blackName.equals(whiteName)) {
			isValid = false;
			whiteNameText.setError(getResources().getString(
					R.string.unique_error_o_name));
		}

		return isValid;
	}

	private static final String PREF_X_NAME = "x-name";
	private static final String PREF_O_NAME = "o-name";

	private void defaultNamesFromPreferences() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getSherlockActivity());
		// TODO store multiple names of the last players, and hook "search" into
		// the text entry
		blackName = sharedPrefs.getString(PREF_X_NAME, "");
		whiteName = sharedPrefs.getString(PREF_O_NAME, "");
	}

	private void writeNamesToPreferences() {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getSherlockActivity());
		Editor edit = sharedPrefs.edit();
		edit.putString(PREF_X_NAME, blackName);
		edit.putString(PREF_O_NAME, whiteName);
		edit.commit();
	}

}
