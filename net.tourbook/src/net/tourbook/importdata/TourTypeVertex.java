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
/**
 * @author Alfred Barten
 */
package net.tourbook.importdata;

import net.tourbook.common.util.StatusUtil;

public class TourTypeVertex implements Comparable<Object>, Cloneable {

	int		avgSpeed;
	long	tourTypeId;

	public TourTypeVertex() {}

	public TourTypeVertex(final int value) {

		this.avgSpeed = value;

	}

	@Override
	public TourTypeVertex clone() {

		TourTypeVertex clonedObject = null;

		try {

			clonedObject = (TourTypeVertex) super.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public int compareTo(final Object anotherObject) throws ClassCastException {

		final TourTypeVertex anotherRGBVertex = (TourTypeVertex) anotherObject;
		final int anotherValue = anotherRGBVertex.avgSpeed;

		return avgSpeed - anotherValue;
	}

	@Override
	public String toString() {
		return "TourTypeVertex ["
		//
				+ ("avgSpeed=" + avgSpeed + ", ")
				+ ("tourTypeId=" + tourTypeId + ", ")
				//
				+ "]";
	}

}
