package de.byteholder.geoclipse.swt;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class MapLegend {

	private Image	legendImage;

	/**
	 * top/left position for the legend in the map
	 */
	private Point	fLegendPosition;

	public Image getImage() {
		return legendImage;
	}

	public Point getLegendPosition() {
		return fLegendPosition;
	}

	public void setImage(Image image) {
		legendImage = image;
	}

	/**
	 * Set top/left position where the legend is painted into the map
	 * 
	 * @param legendPosition
	 */
	public void setLegendPosition(Point legendPosition) {
		fLegendPosition = legendPosition;
	}

}
