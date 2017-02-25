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

import java.time.LocalDateTime;
import java.time.MonthDay;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Spinner;

public class TourFilterProperty {

	TourFilterFieldConfig	fieldConfig		= TourFilterManager.getFieldConfig(TourFilterFieldId.TOUR_DATE);

	/**
	 * Selected operator for the property field
	 */
	TourFilterFieldOperator	fieldOperator	= TourFilterFieldOperator.EQUALS;

	/*
	 * Field data for the different operators and field types
	 */
	LocalDateTime			dateTime1		= LocalDateTime.now().withDayOfMonth(1).withMonth(1);
	LocalDateTime			dateTime2		= LocalDateTime.now();

	MonthDay				monthDay1		= MonthDay.of(1, 1);
	MonthDay				monthDay2		= MonthDay.now();

	/*
	 * UI controls, not all of them are used, depending on the selected field type and operator
	 */
	Combo					comboFieldName;
	Combo					comboFieldOperator;

	Composite				fieldDetailOuterContainer;

	DateTime				uiDateTime1;
	DateTime				uiDateTime2;

	Combo					uiComboMonth1;
	Combo					uiComboMonth2;
	Spinner					uiSpinnerDay1;
	Spinner					uiSpinnerDay2;

	public TourFilterProperty() {}

	void disposeFieldInnerContainer() {

		for (final Control control : fieldDetailOuterContainer.getChildren()) {
			control.dispose();
		}

		uiDateTime1 = null;
		uiDateTime2 = null;

		uiComboMonth1 = null;
		uiComboMonth2 = null;
		uiSpinnerDay1 = null;
		uiSpinnerDay2 = null;
	}

	@Override
	public String toString() {

		return "\nTourFilterProperty [" //

				+ ("fieldConfig=" + fieldConfig + ", ")
				+ ("fieldOperator=" + fieldOperator + ", ")

				+ "]";
	}
}
