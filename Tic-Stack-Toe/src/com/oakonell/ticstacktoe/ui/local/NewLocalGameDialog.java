package com.oakonell.ticstacktoe.ui.local;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;
import com.oakonell.ticstacktoe.ui.menu.GameTypeSpinnerHelper;
import com.oakonell.ticstacktoe.ui.menu.TypeDropDownItem;
import com.oakonell.utils.StringUtils;

public class NewLocalGameDialog extends SherlockDialogFragment {
	private String blackName;
	private String whiteName;

	public interface LocalGameModeListener {
		void chosenMode(GameType type, String blackName, String whiteName);

		void cancel();
	}

	private LocalGameModeListener listener;

	public void initialize(LocalGameModeListener listener) {
		this.listener = listener;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		View view = inflater.inflate(R.layout.dialog_local_game, container,
				false);

//		getDialog().setTitle(R.string.choose_local_game_mode_title);
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

		final Spinner typeSpinner = (Spinner) view.findViewById(R.id.game_type);
		GameTypeSpinnerHelper.populateSpinner(getActivity(), typeSpinner);
		final TextView typeDescr = (TextView) view.findViewById(R.id.game_type_descr);
		GameTypeSpinnerHelper.setOnChange(getActivity(), typeSpinner, typeDescr);

		Button start = (Button) view.findViewById(R.id.start);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				dismiss();

				final TypeDropDownItem typeItem = (TypeDropDownItem) typeSpinner
						.getSelectedItem();

				if (!validate(blackNameText, whiteNameText)) {
					return;
				}
				blackName = blackNameText.getText().toString();
				whiteName = whiteNameText.getText().toString();
				writeNamesToPreferences();

				Handler handler = new Handler();
				imm.hideSoftInputFromWindow(v.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS, new ResultReceiver(
								handler) {

							@Override
							protected void onReceiveResult(int resultCode,
									Bundle resultData) {
								if (wasLaunched)
									return;
								wasLaunched = true;
								listener.chosenMode(typeItem.type, blackName,
										whiteName);
							}

						});

				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (wasLaunched)
							return;
						wasLaunched = true;
						listener.chosenMode(typeItem.type, blackName, whiteName);
					}
				}, 500);
			}
		});

		return view;

	}

	boolean wasLaunched;

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

	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		listener.cancel();
	}

}
