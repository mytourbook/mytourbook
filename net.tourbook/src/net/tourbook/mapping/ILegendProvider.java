package net.tourbook.mapping;

import org.eclipse.swt.graphics.Color;

public interface ILegendProvider {

	abstract LegendColor getLegendColor();

	abstract LegendConfig getLegendConfig();

	abstract int getTourColorId();

	/**
	 * @param legendValue
	 * @return Returns a color for the legend value, this {@link Color} must be disposed
	 */
	abstract Color getValueColor(int legendValue);

	abstract void setLegendColor(LegendColor newLegendColor);

}
