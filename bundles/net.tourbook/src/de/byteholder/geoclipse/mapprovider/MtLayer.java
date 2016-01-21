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
package de.byteholder.geoclipse.mapprovider;

import net.tourbook.common.map.GeoPosition;

import org.geotools.data.ows.Layer;


public class MtLayer implements Comparable<MtLayer>, Cloneable {

	private Layer		_mtGeoLayer;

	private GeoPosition	_lowerGeoPosition;
	private GeoPosition	_upperGeoPosition;

	/**
	 * is <code>true</code> when this layer is displayed in the map
	 */
	private boolean		_isDisplayedInMap	= false;

	/**
	 * position of the layer in the layer list
	 */
	private int			_positionIndex		= -1;

	public MtLayer(final Layer mtGeoLayer, final GeoPosition lowerGeoPosition, final GeoPosition upperGeoPosition) {
		_mtGeoLayer = mtGeoLayer;
		_lowerGeoPosition = lowerGeoPosition;
		_upperGeoPosition = upperGeoPosition;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final MtLayer mtLayer = (MtLayer) super.clone();

		/*
		 * use all shallow copies
		 */

		return mtLayer;
	}

	public int compareTo(final MtLayer otherMtLayer) {

		if (_positionIndex == -1) {

			// sort by name when position is not set
			return _mtGeoLayer.compareTo(otherMtLayer.getGeoLayer());

		} else {

			// sort by position 
			return _positionIndex - otherMtLayer._positionIndex;
		}
	}

	/**
	 * @return Returns the geolayer, the name of the geolayer {@link Layer#getName()} cannot be
	 *         <code>null</code> this is already validated
	 */
	public Layer getGeoLayer() {
		return _mtGeoLayer;
	}

	public GeoPosition getLowerGeoPosition() {
		return _lowerGeoPosition;
	}

	/**
	 * @return Returns the position of the layer within all layers, it returns -1 when the position
	 *         is not set
	 */
	public int getPositionIndex() {
		return _positionIndex;
	}

	public GeoPosition getUpperGeoPosition() {
		return _upperGeoPosition;
	}

	public boolean isDisplayedInMap() {
		return _isDisplayedInMap;
	}

	public void setIsDisplayedInMap(final boolean isDisplayed) {
		_isDisplayedInMap = isDisplayed;
	}

	public void setPositionIndex(final int positionIndex) {
		_positionIndex = positionIndex;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		sb.append("p:"); //$NON-NLS-1$
		sb.append(_positionIndex);
		sb.append(" v:"); //$NON-NLS-1$
		sb.append(_isDisplayedInMap);
		sb.append(" "); //$NON-NLS-1$
		sb.append(_mtGeoLayer.getName());

		return sb.toString();
	}
}
