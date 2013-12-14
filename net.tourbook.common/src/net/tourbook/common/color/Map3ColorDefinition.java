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

import java.util.ArrayList;

import net.tourbook.common.preferences.ICommonPreferences;

/**
 * Contains all colors for one graph type (e.g. altitude) but only one is currently active.
 */
public class Map3ColorDefinition {

	private MapColorId					_colorId;
	private String						_visibleName;

	/**
	 * Contains all color profiles for one {@link MapColorId}.
	 */
	private ArrayList<Map3ColorProfile>	_colorProfiles	= new ArrayList<Map3ColorProfile>();

	private Map3ColorProfile			_activeMapColor;
	private Map3ColorProfile			_defaultMapColor;
	private Map3ColorProfile			_newMapColor;

	/**
	 * Sets the color for the default, current and changes
	 * 
	 * @param colorId
	 * @param visibleName
	 * @param defaultMapColor
	 */
	protected Map3ColorDefinition(	final MapColorId colorId,
									final String visibleName,
									final Map3ColorProfile defaultMapColor) {

		_colorId = colorId;
		_visibleName = visibleName;
		_defaultMapColor = defaultMapColor;

		_colorProfiles.add(defaultMapColor);
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final Map3ColorDefinition other = (Map3ColorDefinition) obj;
		if (_colorId == null) {
			if (other._colorId != null) {
				return false;
			}
		} else if (!_colorId.equals(other._colorId)) {
			return false;
		}

		return true;
	}

	public MapColorId getColorId() {
		return _colorId;
	}

	public ArrayList<Map3ColorProfile> getColorProfiles() {
		return _colorProfiles;
	}

	public Map3ColorProfile getDefaultMapColor() {
		return _defaultMapColor;
	}

	public String getGraphPrefName() {
		return ICommonPreferences.GRAPH_COLORS + _colorId + "."; //$NON-NLS-1$
	}

	public MapColorId getImageId() {
		return _colorId;
	}

	public Map3ColorProfile getMapColor() {
		return _activeMapColor;
	}

	public Map3ColorProfile getNewMapColor() {
		return _newMapColor;
	}

	public String getVisibleName() {
		return _visibleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_colorId == null) ? 0 : _colorId.hashCode());
		return result;
	}

	public void setColorId(final MapColorId colorId) {
		_colorId = colorId;
	}

	public void setMapColor(final Map3ColorProfile mapColor) {
		_activeMapColor = mapColor;
	}

	public void setNewMapColor(final Map3ColorProfile newMapColor) {
		_newMapColor = newMapColor;
	}

	public void setVisibleName(final String visibleName) {
		_visibleName = visibleName;
	}

}
