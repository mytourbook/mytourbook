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

import java.util.Arrays;

/**
 * Configuration for a tour filter field
 */
public class TourFilterFieldConfig {

	/**
	 * Visible field name
	 */
	final String					name;

	/**
	 * Every field can be identified by its unique id
	 */
	final TourFilterFieldId			fieldId;

	final TourFilterFieldType		fieldType;

	/**
	 * Contains all operators which are allowed for this field
	 */
	final TourFilterFieldOperator[]	fieldOperators;

	int								minValue;
	int								maxValue;
	int								pageIncrement;

	/**
	 * Number of digits
	 */
	int								numDigits;

	FieldValueProvider				fieldValueProvider;

	String							unitLabel;

	/**
	 * @param name
	 * @param fieldId
	 * @param fieldType
	 * @param fieldOperators
	 */
	public TourFilterFieldConfig(	final String name,
									final TourFilterFieldId fieldId,
									final TourFilterFieldType fieldType,
									final TourFilterFieldOperator[] fieldOperators) {

		this.name = name;

		this.fieldId = fieldId;
		this.fieldType = fieldType;

		this.fieldOperators = fieldOperators;
	}

	/**
	 * @param name
	 * @param fieldId
	 * @param fieldType
	 * @param filterOperators
	 * @param minValue
	 * @param maxValue
	 * @param pageIncrement
	 */
	public TourFilterFieldConfig(	final String name,
									final TourFilterFieldId fieldId,
									final TourFilterFieldType fieldType,
									final TourFilterFieldOperator[] filterOperators,
									final int minValue,
									final int maxValue,
									final int pageIncrement) {

		this(name, fieldId, fieldType, filterOperators);

		this.minValue = minValue;
		this.maxValue = maxValue;
		this.pageIncrement = pageIncrement;

	}

	/**
	 * @param name
	 * @param fieldId
	 * @param fieldType
	 * @param filterOperators
	 * @param minValue
	 * @param maxValue
	 * @param pageIncrement
	 * @param numDigits
	 * @param fieldValueProvider
	 */
	public TourFilterFieldConfig(	final String name,
									final TourFilterFieldId fieldId,
									final TourFilterFieldType fieldType,
									final TourFilterFieldOperator[] filterOperators,
									final int minValue,
									final int maxValue,
									final int pageIncrement,
									final int numDigits,
									final FieldValueProvider fieldValueProvider) {

		this(name, fieldId, fieldType, filterOperators);

		this.minValue = minValue;
		this.maxValue = maxValue;
		this.pageIncrement = pageIncrement;
		this.numDigits = numDigits;
		this.fieldValueProvider = fieldValueProvider;
	}

	@Override
	public String toString() {
		return "TourFilterFieldConfig [\n" //$NON-NLS-1$
				+ ("name=" + name + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("fieldId=" + fieldId + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("fieldType=" + fieldType + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("fieldOperators=" + Arrays.toString(fieldOperators)) //$NON-NLS-1$
				+ "\n]\n"; //$NON-NLS-1$
	}

}
