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

/**
 * Provides colors to draw a tour or legend.
 */
public class Map3GradientColorProvider extends MapGradientColorProvider implements IGradientColors {

	private MapColorId				_mapColorId;

	private Map3ColorProfile		_map3ColorProfile;

	private MapLegendImageConfig	_mapLegendImageConfig	= new MapLegendImageConfig();

	public Map3GradientColorProvider(final MapColorId mapColorId) {

		_mapColorId = mapColorId;
		_map3ColorProfile = new Map3ColorProfile(mapColorId);
	}

	@Override
	public int getColorValue(final float graphValue) {

		// TODO Auto-generated method stub
		return 0;
	}

	public MapColorId getMapColorId() {
		return _mapColorId;
	}

	@Override
	public MapColorProfile getMapColorProfile() {
		return _map3ColorProfile;
	}

	public MapLegendImageConfig getMapLegendImageConfig() {
		return _mapLegendImageConfig;
	}

	@Override
	public void setColorProfile(final MapColorProfile mapColorProfile) {

	}

	/**
	 * Set legend values from a dataserie
	 * 
	 * @param legendHeight
	 * @param dataSerie
	 * @param legendProvider
	 * @param unitText
	 */
	@Override
	public void setMapConfigValues(	final int legendHeight,
									final float minValue,
									final float maxValue,
									final String unitText,
									final LegendUnitFormat unitFormat) {

		// TODO Auto-generated method stub

	}

}
