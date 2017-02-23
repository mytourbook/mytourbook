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

import org.eclipse.swt.widgets.Combo;

public class TourFilterProperty {

	TourFilterField			filterField		= TourFilterField.TOUR_DATE;
	TourFilterFieldOperator	fieldOperator	= TourFilterFieldOperator.EQUALS;

	Combo					comboFilterField;
	Combo					comboFieldOperator;

	public TourFilterProperty() {}

	@Override
	public String toString() {

		return "\nTourFilterProperty [" //

				+ ("filterField=" + filterField + ", ")
				+ ("fieldOperator=" + fieldOperator)

				+ "]";
	}
}
