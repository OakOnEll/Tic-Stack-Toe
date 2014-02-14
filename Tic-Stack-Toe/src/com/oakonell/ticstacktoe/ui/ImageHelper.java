package com.oakonell.ticstacktoe.ui;

import android.net.Uri;
import android.widget.ImageView;

import com.google.android.gms.common.images.ImageManager;

public class ImageHelper {
	public static void displayImage(ImageManager imgManager, ImageView image,
			Uri iconImageUri, int defaultResource) {
		if (iconImageUri == null
				|| iconImageUri.getEncodedSchemeSpecificPart().contains(
						"gms.games")) {
			imgManager.loadImage(image, iconImageUri, defaultResource);
		} else {
			image.setImageURI(iconImageUri);
		}
	}
}
