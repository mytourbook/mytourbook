/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourType;

class AutomatedImportConfig implements Cloneable, Comparable<AutomatedImportConfig> {

	String					name			= UI.EMPTY_STRING;
	String					backupFolder	= UI.EMPTY_STRING;
	String					deviceFolder	= UI.EMPTY_STRING;

	Enum<TourTypeConfig>	tourTypeConfig	= TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED;

	TourType				oneTourType;
	ArrayList<SpeedVertex>	speedVertices	= new ArrayList<>();

	private int				_id;

	private static int		_idCreator;

	AutomatedImportConfig() {

		_id = ++_idCreator;
	}

	@Override
	protected AutomatedImportConfig clone() {

		AutomatedImportConfig clonedObject = null;

		try {

			clonedObject = (AutomatedImportConfig) super.clone();

			clonedObject._id = ++_idCreator;

			clonedObject.speedVertices = new ArrayList<>();
			for (final SpeedVertex speedVertex : speedVertices) {
				clonedObject.speedVertices.add(speedVertex.clone());
			}

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public int compareTo(final AutomatedImportConfig otherConfig) {

		return name.compareTo(otherConfig.name);
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

		final AutomatedImportConfig other = (AutomatedImportConfig) obj;

		if (_id != other._id) {
			return false;
		}

		return true;
	}

	public int getCreateId() {
		return _id;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + _id;

		return result;
	}

	@Override
	public String toString() {
		return "AutomatedImportConfig ["
		//
				+ ("name=" + name + ", ")
//				+ ("isSetTourType=" + isSetTourType + ", ")
				+ ("speedVertices=" + speedVertices)

				+ "]";
	}
}
