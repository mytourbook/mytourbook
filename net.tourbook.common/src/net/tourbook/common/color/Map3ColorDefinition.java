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

/**
 * Contains all colors for one graph type (e.g. altitude) but only one is currently active.
 */
public class Map3ColorDefinition implements Comparable<Map3ColorDefinition> {

	private MapGraphId					_graphId;
	private String						_visibleName;

	/**
	 * Contains all color profiles for one {@link MapGraphId}.
	 */
	private ArrayList<Map3ColorProfile>	_colorProfiles	= new ArrayList<Map3ColorProfile>();

	public Map3ColorDefinition(final MapGraphId graphId) {

		_graphId = graphId;
	}

	/**
	 * Sets the color for the default, current and changes
	 * 
	 * @param graphId
	 * @param visibleName
	 * @param colorProfile
	 */
	protected Map3ColorDefinition(	final MapGraphId graphId,
									final String visibleName,
									final Map3ColorProfile colorProfile) {

		this(graphId);

		_visibleName = visibleName;

		_colorProfiles.add(colorProfile);
	}

	public void addProfile(final Map3ColorProfile colorProfile) {

		_colorProfiles.add(colorProfile);
	}

	@Override
	public int compareTo(final Map3ColorDefinition otherDef) {

		// sort by name
		return _visibleName.compareTo(otherDef._visibleName);
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
		if (_graphId == null) {
			if (other._graphId != null) {
				return false;
			}
		} else if (!_graphId.equals(other._graphId)) {
			return false;
		}

		return true;
	}

	public ArrayList<Map3ColorProfile> getColorProfiles() {
		return _colorProfiles;
	}

	public MapGraphId getGraphId() {
		return _graphId;
	}

	public MapGraphId getImageId() {
		return _graphId;
	}

	public String getVisibleName() {
		return _visibleName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_graphId == null) ? 0 : _graphId.hashCode());
		return result;
	}

	public void setColorProfiles(final ArrayList<Map3ColorProfile> colorProfiles) {

		_colorProfiles.clear();
		_colorProfiles.addAll(colorProfiles);
	}

	public void setGraphId(final MapGraphId graphId) {
		_graphId = graphId;
	}

	public void setVisibleName(final String visibleName) {
		_visibleName = visibleName;
	}

	@Override
	public String toString() {
		return String.format("Map3ColorDefinition [_graphId=%s]", _graphId);
	}

}
