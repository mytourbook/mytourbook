/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

	public static final String[]	BRIGHTNESS_LABELS		= new String[] //
															{
			Messages.legend_color_keep_color,
			Messages.legend_color_dim_color,
			Messages.legend_color_lighten_color			};

	public static final int			BRIGHTNESS_DEFAULT		= 0;
	public static final int			BRIGHTNESS_DIMMING		= 1;
	public static final int			BRIGHTNESS_LIGHTNING	= 2;

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

	public boolean					isMaxValueOverwrite		= false;
	public int						overwriteMaxValue;

	public boolean					isMinValueOverwrite		= false;
	public int						overwriteMinValue;

	public LegendColor() {}

	public LegendColor(	final ValueColor[] valueColors,
						final int minBrightness,
						final int minBrightnessFactor,
						final int maxBrightness,
						final int maxBrightnessFactor) {

		this.valueColors = valueColors;
		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

	public LegendColor(	final ValueColor[] valueColors,
						final int minBrightness,
						final int minBrightnessFactor,
						final int maxBrightness,
						final int maxBrightnessFactor,
						final boolean isMinOverwrite,
						final int minOverwrite,
						final boolean isMaxOverwrite,
						final int maxOverwrite) {

		this.valueColors = valueColors;
		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;

		this.isMinValueOverwrite = isMinOverwrite;
		this.overwriteMinValue = minOverwrite;
		this.isMaxValueOverwrite = isMaxOverwrite;
		this.overwriteMaxValue = maxOverwrite;
	}

	/**
	 * create a string which is a java contructor for the {@link LegendColor}
	 * 
	 * <pre>
	 * 
	 * new LegendColor(new ValueColor[] {
	 * 		new ValueColor(10, 161, 85, 0),
	 * 		new ValueColor(50, 232, 169, 0),
	 * 		new ValueColor(100, 96, 218, 0),
	 * 		new ValueColor(150, 107, 193, 255),
	 * 		new ValueColor(190, 206, 247, 255) }, LegendColor.BRIGHTNESS_DIMMING, 15, LegendColor.BRIGHTNESS_LIGHTNING, 100)
	 * 
	 * </pre>
	 */
	public String createConstructorString() {

		final StringBuilder buffer = new StringBuilder();

		buffer.append('\n');
		buffer.append('\n');
		buffer.append("new LegendColor(new ValueColor[] {"); //$NON-NLS-1$
		buffer.append('\n');

		int index = 0;
		for (final ValueColor valueColor : valueColors) {
			buffer.append(valueColor.toString());

			if (index++ != valueColors.length - 1) {
				buffer.append(',');
			}
			buffer.append('\n');
		}
		buffer.append("}, "); //$NON-NLS-1$
		buffer.append('\n');

		buffer.append("LegendColor."); //$NON-NLS-1$
		buffer.append(getBrightnessJavaText(minBrightness));
		buffer.append(",\n"); //$NON-NLS-1$

		buffer.append(minBrightnessFactor);
		buffer.append(",\n"); //$NON-NLS-1$

		buffer.append("LegendColor."); //$NON-NLS-1$
		buffer.append(getBrightnessJavaText(maxBrightness));
		buffer.append(",\n"); //$NON-NLS-1$

		buffer.append(maxBrightnessFactor);
		buffer.append(")\n"); //$NON-NLS-1$

		return buffer.toString();
	}

	private String getBrightnessJavaText(final int brightness) {

		switch (brightness) {
		case BRIGHTNESS_DEFAULT:
			return "BRIGHTNESS_DEFAULT"; //$NON-NLS-1$
		case BRIGHTNESS_DIMMING:
			return "BRIGHTNESS_DIMMING"; //$NON-NLS-1$
		case BRIGHTNESS_LIGHTNING:
			return "BRIGHTNESS_LIGHTNING"; //$NON-NLS-1$

		default:
			break;
		}

		return ""; //$NON-NLS-1$
	}

	/**
	 * Creates a copy for this {@link LegendColor}
	 * 
	 * @return
	 */
	public LegendColor getCopy() {

		final LegendColor copy = new LegendColor();

		copy.valueColors = new ValueColor[valueColors.length];
		for (int colorIndex = 0; colorIndex < valueColors.length; colorIndex++) {
			copy.valueColors[colorIndex] = new ValueColor(valueColors[colorIndex]);
		}

		copy.minBrightness = minBrightness;
		copy.minBrightnessFactor = minBrightnessFactor;

		copy.maxBrightness = maxBrightness;
		copy.maxBrightnessFactor = maxBrightnessFactor;

		copy.isMinValueOverwrite = isMinValueOverwrite;
		copy.overwriteMinValue = overwriteMinValue;

		copy.isMaxValueOverwrite = isMaxValueOverwrite;
		copy.overwriteMaxValue = overwriteMaxValue;

		return copy;
	}

}
