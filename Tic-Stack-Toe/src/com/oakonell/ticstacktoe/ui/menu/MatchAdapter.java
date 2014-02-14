package com.oakonell.ticstacktoe.ui.menu;

import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.ImageHelper;

public class MatchAdapter extends ArrayAdapter<MatchInfo> {

	private Activity context;
	private MenuFragment fragment;
	private ImageManager imgManager;
	private View labelView;
	private List<MatchInfo> objects;

	public MatchAdapter(Activity context, MenuFragment fragment,
			List<MatchInfo> objects, View labelView) {
		super(context, R.layout.match_layout, objects);
		this.context = context;
		this.imgManager = ImageManager.create(context);
		this.fragment = fragment;
		this.labelView = labelView;
		this.objects = objects;
		if (objects.size() > 0) {
			labelView.setVisibility(View.VISIBLE);
		} else {
			labelView.setVisibility(View.GONE);
		}
	}

	private static class ViewHolder {
		ImageView image;
		TextView name;
		TextView subtitle;
		ImageView itemMenu;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			view = inflater.inflate(R.layout.match_layout, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = (TextView) view.findViewById(R.id.opponent_name);
			viewHolder.image = (ImageView) view
					.findViewById(R.id.opponnet_image);
			viewHolder.subtitle = (TextView) view.findViewById(R.id.subtitle);
			viewHolder.itemMenu = (ImageView) view.findViewById(R.id.item_menu);
			view.setTag(viewHolder);
		}
		ViewHolder holder = (ViewHolder) view.getTag();
		final MatchInfo item = getItem(position);

		holder.subtitle.setText(item.getSubtext(context));

		ImageHelper
				.displayImage(imgManager, holder.image,
						item.getOpponentIconImageUri(),
						R.drawable.silhouette_icon_4520);

		holder.name.setText(item.getText(context));

		final List<MatchMenuItem> menus = item.getMenuItems();
		if (menus.size() == 0) {
			holder.itemMenu.setVisibility(View.GONE);
		} else {
			holder.itemMenu.setVisibility(View.VISIBLE);
		}
		holder.itemMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// show a popup menu
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				// builder.setTitle("Modify Match");
				CharSequence[] menuitems = new String[menus.size()];
				int i = 0;
				for (MatchMenuItem each : menus) {
					menuitems[i++] = each.text;
				}

				builder.setItems(menuitems,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								// The 'which' argument contains the index
								// position of the selected item
								menus.get(which).execute.execute(fragment,
										objects);
								notifyDataSetChanged();
							}
						});
				builder.setInverseBackgroundForced(true);
				builder.create();
				AlertDialog dialog = builder.create();
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				WindowManager.LayoutParams wmlp = dialog.getWindow()
						.getAttributes();

				wmlp.gravity = Gravity.TOP | Gravity.RIGHT;
				int pos[] = new int[2];
				v.getLocationInWindow(pos);
				wmlp.x = pos[0];
				wmlp.y = pos[1];

				dialog.show();
			}
		});

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				item.onClick(fragment);
			}
		});

		return view;
	}

	interface ItemExecute {
		void execute(MenuFragment fragment, List<MatchInfo> objects);
	}

	static class MatchMenuItem {
		String text;
		ItemExecute execute;
	}

	@Override
	public void notifyDataSetChanged() {
		Collections.sort(objects, MatchInfo.Factory.getComparator());
		super.notifyDataSetChanged();
		if (getCount() == 0) {
			labelView.setVisibility(View.GONE);
		} else {
			labelView.setVisibility(View.VISIBLE);
		}
	}

}
