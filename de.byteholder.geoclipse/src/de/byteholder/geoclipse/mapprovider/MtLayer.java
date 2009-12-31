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

import org.geotools.data.ows.Layer;

import de.byteholder.gpx.GeoPosition;

public class MtLayer implements Comparable<MtLayer>, Cloneable {

	private Layer		fMtGeoLayer;

	private GeoPosition	fLowerGeoPosition;
	private GeoPosition	fUpperGeoPosition;

	/**
	 * is <code>true</code> when this layer is displayed in the map
	 */
	private boolean		fIsDisplayedInMap	= false;

	/**
	 * position of the layer in the layer list
	 */
	private int			fPositionIndex		= -1;

	public MtLayer(final Layer mtGeoLayer, final GeoPosition lowerGeoPosition, final GeoPosition upperGeoPosition) {
		fMtGeoLayer = mtGeoLayer;
		fLowerGeoPosition = lowerGeoPosition;
		fUpperGeoPosition = upperGeoPosition;
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

		if (fPositionIndex == -1) {

			// sort by name when position is not set
			return fMtGeoLayer.compareTo(otherMtLayer.getGeoLayer());

		} else {

			// sort by position 
			return fPositionIndex - otherMtLayer.fPositionIndex;
		}
	}

	/**
	 * @return Returns the geolayer, the name of the geolayer {@link Layer#getName()} cannot be
	 *         <code>null</code> this is already validated
	 */
	public Layer getGeoLayer() {
		return fMtGeoLayer;
	}

	public GeoPosition getLowerGeoPosition() {
		return fLowerGeoPosition;
	}

	/**
	 * @return Returns the position of the layer within all layers, it returns -1 when the position
	 *         is not set
	 */
	public int getPositionIndex() {
		return fPositionIndex;
	}

	public GeoPosition getUpperGeoPosition() {
		return fUpperGeoPosition;
	}

	public boolean isDisplayedInMap() {
		return fIsDisplayedInMap;
	}

	public void setIsDisplayedInMap(final boolean fIsDisplayed) {
		fIsDisplayedInMap = fIsDisplayed;
	}

	public void setPositionIndex(final int positionIndex) {
		fPositionIndex = positionIndex;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();
		sb.append("p:"); //$NON-NLS-1$
		sb.append(fPositionIndex);
		sb.append(" v:"); //$NON-NLS-1$
		sb.append(fIsDisplayedInMap);
		sb.append(" "); //$NON-NLS-1$
		sb.append(fMtGeoLayer.getName());

		return sb.toString();
	}
}
