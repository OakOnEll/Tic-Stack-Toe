package com.oakonell.ticstacktoe.ui.menu;

import java.util.List;

import android.app.Activity;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.oakonell.ticstacktoe.R;

public class MatchAdapter extends ArrayAdapter<TurnBasedMatch> {

	private Activity context;
	private ImageManager imgManager;

	public MatchAdapter(Activity context, List<TurnBasedMatch> objects) {
		super(context, R.layout.match_layout, objects);
		this.context = context;
		imgManager = ImageManager.create(context);
	}

	private static class ViewHolder {
		ImageView image;
		TextView name;
		TextView subtitle;

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
			view.setTag(viewHolder);
		}
		ViewHolder holder = (ViewHolder) view.getTag();
		TurnBasedMatch item = getItem(position);

		CharSequence timeSpanString = DateUtils.getRelativeTimeSpanString(
				getContext(), item.getLastUpdatedTimestamp(), true);
		holder.subtitle.setText(timeSpanString);

		Participant opponnet = item.getParticipants().get(0);
		imgManager.loadImage(holder.image, opponnet.getIconImageUri());
		holder.name.setText(opponnet.getDisplayName());

		return view;
	}

}
