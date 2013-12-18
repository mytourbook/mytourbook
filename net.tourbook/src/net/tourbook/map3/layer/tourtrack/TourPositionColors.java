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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.Path;

import java.awt.Color;
import java.util.ArrayList;

import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapColorId;
import net.tourbook.data.TourData;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapUtils;
import net.tourbook.map3.layer.ColorCacheAWT;

class TourPositionColors implements Path.PositionColors {

	private final ColorCacheAWT	_awtColorCache	= new ColorCacheAWT();

	private IMapColorProvider	_colorProvider	= MapColorProvider.getMap3ColorProvider(MapColorId.Altitude);

	public Color getColor(final Position position, final int ordinal) {

		/**
		 * This returns a dummy color, it is just a placeholder because a Path.PositionColors must
		 * be set in the Path THAT a position color is used :-(
		 */

		return Color.CYAN;
	}

	public Color getDiscreteColor(final int colorValue) {
		return _awtColorCache.get(colorValue);
	}

	public Color getGradientColor(final float graphValue, final Integer positionIndex, final int tourTrackPickIndex) {

		int colorValue = -1;

		if (_colorProvider instanceof IGradientColors) {

			final IGradientColors gradientColorProvider = (IGradientColors) _colorProvider;

			colorValue = gradientColorProvider.getColorValue(graphValue);
		}

		if (colorValue == -1) {
			// set ugly default value, this case should not happen
			return Color.MAGENTA;
		}

		return _awtColorCache.get(colorValue);
	}

	public void setColorProvider(final IMapColorProvider legendProvider) {

		_colorProvider = legendProvider;

		_awtColorCache.clear();
	}

	/**
	 * Update colors in the color provider.
	 * 
	 * @param allTours
	 */
	public void updateColorProvider(final ArrayList<TourData> allTours) {

		if (_colorProvider instanceof IGradientColors) {

			final IGradientColors colorProvider = (IGradientColors) _colorProvider;

			MapUtils.updateLegendMinMaxValues(allTours, colorProvider, 300);
		}
	}

	void updateColors(final double trackOpacity) {

		_awtColorCache.setOpacity(trackOpacity);
	}

}
