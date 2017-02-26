/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

import java.util.ArrayList;

import net.tourbook.common.UI;

public class TourFilterProfile {

	private static int				_idCounter			= 0;

	int								profileId;

	/**
	 * Profile name
	 */
	String							name				= UI.EMPTY_STRING;

	ArrayList<TourFilterProperty>	filterProperties	= new ArrayList<>();

	public TourFilterProfile() {

		profileId = ++_idCounter;
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

		final TourFilterProfile other = (TourFilterProfile) obj;

		if (profileId != other.profileId) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;

		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + profileId;

		return result;
	}

	@Override
	public String toString() {
		return "TourFilterProfile [\n"
				+ ("profileId=" + profileId + ", \n")//
				+ ("name=" + name + ", \n")
				+ ("filterProperties=" + filterProperties + ", \n")
				+ "]";
	}

}
