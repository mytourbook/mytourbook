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

public class SpeedTourType implements Comparable<Object>, Cloneable {

	/**
	 * Average speed for this tour type in km/h.
	 */
	public float	avgSpeed;

	public long		tourTypeId	= TourDatabase.ENTITY_IS_NOT_SAVED;

	public SpeedTourType() {}

	public SpeedTourType(final int value) {

		this.avgSpeed = value;

	}

	@Override
	public SpeedTourType clone() {

		SpeedTourType clonedObject = null;

		try {

			clonedObject = (SpeedTourType) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public int compareTo(final Object anotherObject) throws ClassCastException {

		final float anotherValue = ((SpeedTourType) anotherObject).avgSpeed;

		return Float.compare(avgSpeed, anotherValue);
	}

	@Override
	public String toString() {
		return "\nSpeedTourType [" //$NON-NLS-1$
		//
				+ ("avgSpeed=" + avgSpeed + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("tourTypeId=" + tourTypeId + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "]"; //$NON-NLS-1$
	}

}
