package net.tourbook.mapping;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;

public interface ILegendProvider {

	abstract LegendColor getLegendColor();

	abstract LegendConfig getLegendConfig();

	abstract int getTourColorId();

	/**
	 * @param legendValue
	 * @return Returns a color for the legend value, this {@link Color} must be disposed
	 */
	abstract Color getValueColor(int legendValue);

	/**
	 * Set the colors for the legend, the values will not be changed
	 * 
	 * @param newLegendColor
	 */
	abstract void setLegendColorColors(LegendColor newLegendColor);

	abstract void setLegendColorValues(Rectangle legendBounds, int[] dataSerie, String unitLabel);
}
