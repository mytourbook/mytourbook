package net.tourbook.ui.views.calendar;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.swt.graphics.RGB;

abstract class WeekFormatter {

	WeekFormatterID			id;

	private ColorDefinition	_colorDefinition;
	private String			_name;

	IValueFormatter			_valueFormatter;

	WeekFormatter(final WeekFormatterID id) {

		this.id = id;
	}

	WeekFormatter(final WeekFormatterID id, final String name, final String colorName) {

		this.id = id;
		_name = name;

		_colorDefinition = new GraphColorManager().getGraphColorDefinition(colorName);
	}

	abstract String format(CalendarTourData data, ValueFormat valueFormat, boolean isShowValueUnit);

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

	/**
	 * @return Return the default format for this formatter
	 */
	public abstract ValueFormat getDefaultFormat();

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
}
