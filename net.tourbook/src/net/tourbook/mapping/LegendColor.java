/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.mapping;

public class LegendColor {

	public static final int			BRIGHTNESS_DEFAULT		= 0;
	public static final int			BRIGHTNESS_DIMMING		= 1;
	public static final int			BRIGHTNESS_LIGHTNING	= 2;

	public static final String[]	BrightnessLabels		= new String[] { "Keep Color", "Dim Color", "Lighten Color" };

	public ValueColor[]				valueColors				= new ValueColor[] {
			new ValueColor(10, 255, 0, 0),
			new ValueColor(50, 100, 100, 0),
			new ValueColor(100, 0, 255, 0),
			new ValueColor(150, 0, 100, 100),
			new ValueColor(190, 0, 0, 255)					};
	/**
	 * min and max value is painted black when {@link #minBrightnessFactor}==100, a value below 100
	 * will dim the color
	 */
	public int						minBrightness			= BRIGHTNESS_DEFAULT;
	public int						minBrightnessFactor		= 100;

	public int						maxBrightness			= BRIGHTNESS_DEFAULT;
	public int						maxBrightnessFactor		= 100;

	/**
	 * Creates a copy for this {@link LegendColor}
	 * 
	 * @return
	 */
	public LegendColor getCopy() {

		LegendColor copy = new LegendColor();

		copy.valueColors = new ValueColor[valueColors.length];
		for (int colorIndex = 0; colorIndex < valueColors.length; colorIndex++) {
			copy.valueColors[colorIndex] = new ValueColor(valueColors[colorIndex]);
		}

		copy.minBrightness = minBrightness;
		copy.minBrightnessFactor = minBrightnessFactor;

		copy.maxBrightness = maxBrightness;
		copy.maxBrightnessFactor = maxBrightnessFactor;

		return copy;
	}

	@Override
	public String toString() {

		StringBuffer buffer = new StringBuffer();

		for (ValueColor valueColor : valueColors) {
			buffer.append(valueColor.toString());
			buffer.append('\n');
		}

		buffer.append('\n');

		buffer.append("minBrightness\t");
		buffer.append(minBrightness);
		buffer.append('\n');

		buffer.append("minBrightnessFactor\t");
		buffer.append(minBrightnessFactor);
		buffer.append('\n');

		buffer.append("maxBrightness\t");
		buffer.append(maxBrightness);
		buffer.append('\n');

		buffer.append("maxBrightnessFactor\t");
		buffer.append(maxBrightnessFactor);
		buffer.append('\n');

		return buffer.toString();
	}

}
