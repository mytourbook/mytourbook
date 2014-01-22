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

import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.data.TourData;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map.MapUtils;
import net.tourbook.map3.layer.ColorCacheAWT;

class TourPositionColors implements Path.PositionColors {

	private final ColorCacheAWT	_awtColorCache	= new ColorCacheAWT();

	private IMapColorProvider	_colorProvider	= MapColorProvider.getActiveMap3ColorProvider(MapGraphId.Altitude);

	public Color getColor(final Position position, final int ordinal) {

		/**
		 * This returns a dummy color, it is just a placeholder because a Path.PositionColors must
		 * be set in the Path THAT a position color is used :-(
		 */

		return Color.CYAN;
	}

	public Color getDiscreteColor(final int colorValue) {
		return _awtColorCache.getColorRGBA(colorValue);
	}

	public Color getGradientColor(final float graphValue, final Integer positionIndex, final int tourTrackPickIndex) {

		if (_colorProvider instanceof IGradientColorProvider) {

			final IGradientColorProvider gradientColorProvider = (IGradientColorProvider) _colorProvider;

			final int rgbaValue = gradientColorProvider.getRGBValue(ColorProviderConfig.MAP3_TOUR, graphValue);

			return _awtColorCache.getColorRGBA(rgbaValue);
		}

		// set ugly default value, this case should not happen
		return Color.MAGENTA;
	}

	public void setColorProvider(final IMapColorProvider legendProvider) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tsetColorProvider() " + legendProvider));
//		// TODO remove SYSTEM.OUT.PRINTLN

		_colorProvider = legendProvider;

		_awtColorCache.clear();
	}

	/**
	 * Update colors in the color provider.
	 * 
	 * @param allTours
	 */
	public void updateColorProvider(final ArrayList<TourData> allTours) {

		if (_colorProvider instanceof IGradientColorProvider) {

			final IGradientColorProvider colorProvider = (IGradientColorProvider) _colorProvider;

			MapUtils.configureColorProvider(allTours, colorProvider, ColorProviderConfig.MAP3_TOUR, 300);
		}
	}

	void updateColors(final double trackOpacity) {

		_awtColorCache.setTrackOpacity(trackOpacity);
	}

}
