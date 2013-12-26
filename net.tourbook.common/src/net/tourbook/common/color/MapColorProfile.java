/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

import net.tourbook.common.Messages;

public abstract class MapColorProfile {

	public static final String[]	BRIGHTNESS_LABELS			= new String[] //
																{
			Messages.legend_color_keep_color,
			Messages.legend_color_dim_color,
			Messages.legend_color_lighten_color				};

	public static final int			BRIGHTNESS_DEFAULT			= 0;
	public static final int			BRIGHTNESS_DIMMING			= 1;
	public static final int			BRIGHTNESS_LIGHTNING		= 2;

	public static final int			BRIGHTNESS_FACTOR_DEFAULT	= 100;

	/**
	 * min and max value is painted black when {@link #minBrightnessFactor}==100, a value below 100
	 * will dim the color
	 */
	protected int					minBrightness				= BRIGHTNESS_DEFAULT;
	protected int					maxBrightness				= BRIGHTNESS_DEFAULT;

	protected int					minBrightnessFactor			= BRIGHTNESS_FACTOR_DEFAULT;
	protected int					maxBrightnessFactor			= BRIGHTNESS_FACTOR_DEFAULT;

	protected boolean				isMinValueOverwrite			= false;
	protected boolean				isMaxValueOverwrite			= false;

	protected int					minValueOverwrite;
	protected int					maxValueOverwrite;

	public MapColorProfile() {
		super();
	}

	public int getMaxBrightness() {
		return maxBrightness;
	}

	public int getMaxBrightnessFactor() {
		return maxBrightnessFactor;
	}

	public int getMaxValueOverwrite() {
		return maxValueOverwrite;
	}

	public int getMinBrightness() {
		return minBrightness;
	}

	public int getMinBrightnessFactor() {
		return minBrightnessFactor;
	}

	public int getMinValueOverwrite() {
		return minValueOverwrite;
	}

	public boolean isMaxValueOverwrite() {
		return isMaxValueOverwrite;
	}

	public boolean isMinValueOverwrite() {
		return isMinValueOverwrite;
	}

	public void setIsMaxValueOverwrite(final boolean isMaxValueOverwrite) {
		this.isMaxValueOverwrite = isMaxValueOverwrite;
	}

	public void setIsMinValueOverwrite(final boolean isMinValueOverwrite) {
		this.isMinValueOverwrite = isMinValueOverwrite;
	}

	public void setMaxBrightness(final int maxBrightness) {
		this.maxBrightness = maxBrightness;
	}

	public void setMaxBrightnessFactor(final int maxBrightnessFactor) {
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

	public void setMaxValueOverwrite(final int overwriteMaxValue) {
		this.maxValueOverwrite = overwriteMaxValue;
	}

	public void setMinBrightness(final int minBrightness) {
		this.minBrightness = minBrightness;
	}

	public void setMinBrightnessFactor(final int minBrightnessFactor) {
		this.minBrightnessFactor = minBrightnessFactor;
	}

	public void setMinValueOverwrite(final int overwriteMinValue) {
		this.minValueOverwrite = overwriteMinValue;
	}

	public String toString_dis() {

		return String.format("\n" //$NON-NLS-1$
				+ "MapColorProfile\n" //$NON-NLS-1$
				+ "   minBrightness			=%s\n" //$NON-NLS-1$
				+ "   minBrightnessFactor	=%s\n" //$NON-NLS-1$
				+ "   maxBrightness			=%s\n" //$NON-NLS-1$
				+ "   maxBrightnessFactor	=%s\n" //$NON-NLS-1$
				+ "   isMaxValueOverwrite	=%s\n" //$NON-NLS-1$
				+ "   overwriteMaxValue		=%s\n" //$NON-NLS-1$
				+ "   isMinValueOverwrite	=%s\n" //$NON-NLS-1$
				+ "   overwriteMinValue		=%s\n", //$NON-NLS-1$
				minBrightness,
				minBrightnessFactor,
				maxBrightness,
				maxBrightnessFactor,
				isMaxValueOverwrite,
				maxValueOverwrite,
				isMinValueOverwrite,
				minValueOverwrite);
	}
}
