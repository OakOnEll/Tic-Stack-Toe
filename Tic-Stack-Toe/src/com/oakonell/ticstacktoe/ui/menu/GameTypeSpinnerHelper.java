package com.oakonell.ticstacktoe.ui.menu;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.model.GameType;

public class GameTypeSpinnerHelper {

	public static void populateSpinner(Activity activity, Spinner typeSpinner) {
		List<TypeDropDownItem> types = new ArrayList<TypeDropDownItem>();
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_junior), GameType.JUNIOR));
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_normal), GameType.NORMAL));
		types.add(new TypeDropDownItem(activity.getResources().getString(
				R.string.type_strict), GameType.STRICT));

		ArrayAdapter<TypeDropDownItem> typeAdapter = new ArrayAdapter<TypeDropDownItem>(
				activity, android.R.layout.simple_spinner_dropdown_item, types);
		typeSpinner.setAdapter(typeAdapter);
	}

	public static void populateDescription(FragmentActivity fragmentActivity,
			TextView typeDescr, GameType type) {
		int descrId = getTypeDescriptionStringResource(type);
//		typeDescr.setText(descrId);
	}

	public static int getTypeDescriptionStringResource(GameType type) {
		int descrId;
		if (type.isJunior()) {
			descrId = R.string.type_junior_short_descr;
		} else if (type.isNormal()) {
			descrId = R.string.type_normal_short_descr;
		} else if (type.isStrict()) {
			descrId = R.string.type_strict_short_descr;
		} else {
			throw new RuntimeException("Unsupported type " + type);
		}
		return descrId;
	}

	public static void setOnChange(final FragmentActivity activity,
			final Spinner typeSpinner, final TextView typeDescr) {

		typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
					long id) {
				GameType type = ((TypeDropDownItem) typeSpinner
						.getSelectedItem()).type;
				GameTypeSpinnerHelper.populateDescription(activity, typeDescr,
						type);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}

		});
		GameType type = ((TypeDropDownItem) typeSpinner.getSelectedItem()).type;
		GameTypeSpinnerHelper.populateDescription(activity, typeDescr, type);
	}

	public static CharSequence getTypeName(FragmentActivity activity,
			GameType type) {
		int descrId;
		if (type.isJunior()) {
			descrId = R.string.type_junior_name;
		} else if (type.isNormal()) {
			descrId = R.string.type_normal_name;
		} else if (type.isStrict()) {
			descrId = R.string.type_strict_name;
		} else {
			throw new RuntimeException("Unsupported type " + type);
		}
		return activity.getString(descrId);
	}

}
