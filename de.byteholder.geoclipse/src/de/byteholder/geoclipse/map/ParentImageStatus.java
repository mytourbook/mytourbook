package de.byteholder.geoclipse.map;

import org.eclipse.swt.graphics.ImageData;

public class ParentImageStatus {

	/**
	 * Tile image of the parent
	 */
	ImageData[]	tileImageData;

	/**
	 * Is <code>true</code> when all child images are set into the parent image
	 */
	boolean		isImageFinal;

	boolean		isSaveImage;

	public ParentImageStatus(final ImageData[] imageData, final boolean isFinal, final boolean isSaveParentImage) {
		tileImageData = imageData;
		isImageFinal = isFinal;
		isSaveImage = isSaveParentImage;
	}

}
