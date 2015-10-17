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

import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.TourDatabase;

public class SpeedVertex implements Comparable<Object>, Cloneable {

	int		avgSpeed;
	long	tourTypeId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	public SpeedVertex() {}

	public SpeedVertex(final int value) {

		this.avgSpeed = value;

	}

	@Override
	public SpeedVertex clone() {

		SpeedVertex clonedObject = null;

		try {

			clonedObject = (SpeedVertex) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public int compareTo(final Object anotherObject) throws ClassCastException {

		final int anotherValue = ((SpeedVertex) anotherObject).avgSpeed;

		return avgSpeed - anotherValue;
	}

	@Override
	public String toString() {
		return "\nSpeedVertex ["
		//
				+ ("avgSpeed=" + avgSpeed + ", ")
				+ ("tourTypeId=" + tourTypeId + ", ")
				//
				+ "]";
	}

}
