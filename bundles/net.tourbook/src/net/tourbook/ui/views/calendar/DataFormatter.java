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
package net.tourbook.ui.views.calendar;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.swt.graphics.RGB;

abstract class DataFormatter {

	FormatterID				id;
	private String			_name;

	IValueFormatter			valueFormatter;
	private ColorDefinition	_colorDefinition;

	DataFormatter(final FormatterID id) {

		this.id = id;
	}

	DataFormatter(final FormatterID id, final String name, final String colorName) {

		this.id = id;
		_name = name;

		_colorDefinition = new GraphColorManager().getGraphColorDefinition(colorName);
	}

	abstract String format(CalendarTourData data, ValueFormat valueFormat, boolean isShowValueUnit);

	/**
	 * @return Return the default format for this formatter or <code>null</code> when a formatter is
	 *         not available.
	 */
	public abstract ValueFormat getDefaultFormat();

	RGB getGraphColor(final CalendarColor calendarColor) {

		switch (calendarColor) {

		case BRIGHT:
			return _colorDefinition.getGradientBright_Active();

		case DARK:
			return _colorDefinition.getGradientDark_Active();

		case LINE:
			return _colorDefinition.getLineColor_Active();

		case TEXT:
		default:
			return _colorDefinition.getTextColor_Active();
		}
	}

	String getText() {

		if (null != _name) {
			return _name;
		} else {
			return _colorDefinition.getVisibleName();
		}
	}

	/**
	 * @return Returns <code>null</code> when a format is not available.
	 */
	public abstract ValueFormat[] getValueFormats();

	abstract void setValueFormat(ValueFormat valueFormat);

	@Override
	public String toString() {

		return "\n"

				+ "DataFormatter ["

				+ "id=" + id + ", "
				+ "_name=" + _name + ", "
				+ "_valueFormatter=" + valueFormatter
				+ "_colorDefinition=" + _colorDefinition + ", "

				+ "]";
	}
}
