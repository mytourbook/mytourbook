/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.tag.tour.filter;

import gnu.trove.set.hash.TLongHashSet;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

public class TourTagFilterProfile implements Cloneable {

	private static int	_idCounter		= 0;

	int					profileId;

	/**
	 * Profile name
	 */
	String				name			= Messages.Tour_Filter_Default_ProfileName;

	TLongHashSet		tagFilterIds	= new TLongHashSet();

	public TourTagFilterProfile() {

		profileId = ++_idCounter;
	}

	@Override
	protected TourTagFilterProfile clone() {

		TourTagFilterProfile clonedObject = null;

		try {

			clonedObject = (TourTagFilterProfile) super.clone();

			clonedObject.profileId = ++_idCounter;

			// create a unique name
			clonedObject.name = name + UI.SPACE + Integer.toString(clonedObject.profileId);

			clonedObject.tagFilterIds = new TLongHashSet(tagFilterIds.toArray());

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
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

		final TourTagFilterProfile other = (TourTagFilterProfile) obj;

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

}
