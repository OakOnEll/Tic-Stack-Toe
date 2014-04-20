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

import com.commonsware.cwac.merge.MergeAdapter;
import com.google.android.gms.common.images.ImageManager;
import com.oakonell.ticstacktoe.R;
import com.oakonell.ticstacktoe.ui.ImageHelper;

public class MatchAdapter extends ArrayAdapter<MatchInfo> {
	private final Activity context;
	private final MenuFragment fragment;
	private final ImageManager imgManager;
	private final View labelView;
	private final View bottomView;
	private final List<MatchInfo> objects;
	private final MergeAdapter menuAdapter;

	public MatchAdapter(Activity context, MenuFragment fragment,
			List<MatchInfo> objects, MergeAdapter menuAdapter, View labelView,
			View listBottomView) {
		super(context, R.layout.match_layout, objects);
		this.context = context;
		this.imgManager = ImageManager.create(context);
		this.fragment = fragment;
		this.labelView = labelView;
		this.bottomView = listBottomView;
		this.objects = objects;
		this.menuAdapter = menuAdapter;
		if (objects.size() > 0) {
			labelView.setVisibility(View.VISIBLE);
			listBottomView.setVisibility(View.VISIBLE);
			menuAdapter.setActive(labelView, true);
		} else {
			labelView.setVisibility(View.GONE);
			listBottomView.setVisibility(View.GONE);
			menuAdapter.setActive(labelView, false);
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
		ViewHolder holder;
		if (view == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			view = inflater.inflate(R.layout.match_layout, null);
			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.opponent_name);
			holder.image = (ImageView) view.findViewById(R.id.opponnet_image);
			holder.subtitle = (TextView) view.findViewById(R.id.subtitle);
			holder.itemMenu = (ImageView) view.findViewById(R.id.item_menu);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		final MatchInfo item = getItem(position);

		holder.subtitle.setText(item.getSubtext(context));

		ImageHelper.displayImage(imgManager, holder.image,
				item.getIconImageUri(), R.drawable.silhouette_icon_4520);

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
				showPopupMenu(menus, v);
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

	public interface ItemExecute {
		void execute(MenuFragment fragment, List<MatchInfo> objects);
	}

	public static class MatchMenuItem {
		final String text;
		final ItemExecute execute;

		public MatchMenuItem(String text, ItemExecute execute) {
			this.text = text;
			this.execute = execute;
		}
	}

	private void showPopupMenu(final List<MatchMenuItem> menus,
			View originatingView) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		// builder.setTitle("Modify Match");
		CharSequence[] menuitems = new String[menus.size()];
		int i = 0;
		for (MatchMenuItem each : menus) {
			menuitems[i++] = each.text;
		}

		builder.setItems(menuitems, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// The 'which' argument contains the index
				// position of the selected item
				menus.get(which).execute.execute(fragment, objects);
				notifyDataSetChanged();
			}
		});
		builder.setInverseBackgroundForced(true);
		builder.create();
		AlertDialog dialog = builder.create();
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();

		wmlp.gravity = Gravity.TOP | Gravity.RIGHT;
		int pos[] = new int[2];
		originatingView.getLocationInWindow(pos);
		wmlp.x = pos[0];
		wmlp.y = pos[1];

		dialog.show();
	}

	@Override
	public void notifyDataSetChanged() {
		Collections.sort(objects, MatchInfo.MatchUtils.getComparator());
		super.notifyDataSetChanged();
		if (getCount() == 0) {
			labelView.setVisibility(View.GONE);
			bottomView.setVisibility(View.GONE);
			menuAdapter.setActive(labelView, false);
		} else {
			labelView.setVisibility(View.VISIBLE);
			bottomView.setVisibility(View.VISIBLE);
			menuAdapter.setActive(labelView, true);
		}
	}

}
