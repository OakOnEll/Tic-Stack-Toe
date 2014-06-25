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
			if (iconImageUri != null) {
				if (iconImageUri.equals(image.getTag())) {
					return;
				}
				image.setTag(iconImageUri);
			} else {
				Integer defResInt = defaultResource;
				if (defResInt.equals(image.getTag())) {
					return;
				}
				image.setTag(defResInt);
			}
			imgManager.loadImage(image, iconImageUri, defaultResource);
		} else {
			if (iconImageUri.equals(image.getTag())) {
				return;
			}
			image.setTag(iconImageUri);
			image.setImageURI(iconImageUri);
		}
	}
}
