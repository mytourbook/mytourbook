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

	public MapColorProfile() {
		super();
	}

	public int getMaxBrightness() {
		return maxBrightness;
	}

	public int getMaxBrightnessFactor() {
		return maxBrightnessFactor;
	}

	public int getMinBrightness() {
		return minBrightness;
	}

	public int getMinBrightnessFactor() {
		return minBrightnessFactor;
	}

	public void setMaxBrightness(final int maxBrightness) {
		this.maxBrightness = maxBrightness;
	}

	public void setMaxBrightnessFactor(final int maxBrightnessFactor) {
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

	public void setMinBrightness(final int minBrightness) {
		this.minBrightness = minBrightness;
	}

	public void setMinBrightnessFactor(final int minBrightnessFactor) {
		this.minBrightnessFactor = minBrightnessFactor;
	}

	public String toString_dis() {

		return String.format("\n" //$NON-NLS-1$
				+ "MapColorProfile\n" //$NON-NLS-1$
				+ "   minBrightness			=%s\n" //$NON-NLS-1$
				+ "   minBrightnessFactor	=%s\n" //$NON-NLS-1$
				+ "   maxBrightness			=%s\n" //$NON-NLS-1$
				+ "   maxBrightnessFactor	=%s\n", //$NON-NLS-1$
				minBrightness,
				minBrightnessFactor,
				maxBrightness,
				maxBrightnessFactor);
	}
}
