package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;

public class GameTypeSpinnerHelper {

	public static void populateSpinner(Activity activity, Spinner typeSpinner) {
		List<TypeDropDownItem> types = new ArrayList<TypeDropDownItem>();
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_junior), GameType.JUNIOR));
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_easy), GameType.EASY));
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_strict), GameType.REGULAR));

		ArrayAdapter<TypeDropDownItem> typeAdapter = new ArrayAdapter<TypeDropDownItem>(
				activity, android.R.layout.simple_spinner_dropdown_item, types);
		typeSpinner.setAdapter(typeAdapter);

	}

}
