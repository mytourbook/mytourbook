package net.tourbook.ui.views.calendar;

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;

import org.eclipse.swt.graphics.RGB;

abstract class WeekSummaryFormatter {

	/**
	 * 
	 */
	private final CalendarView	_calendarView;
	int							index;

	ColorDefinition				_colorDefinition;
	String						_name;

	WeekSummaryFormatter(final CalendarView calendarView, final String colorName) {

		_calendarView = calendarView;
		_colorDefinition = new GraphColorManager().getGraphColorDefinition(colorName);
	}

	WeekSummaryFormatter(final CalendarView calendarView, final String colorName, final String name) {

		this(calendarView, colorName);

		_name = name;
	}

	abstract String format(CalendarTourData data);

	RGB getColor() {

		if (_calendarView._useLineColorForWeekSummary) {

			return _colorDefinition.getLineColor_Active();

		} else {

			return _colorDefinition.getTextColor_Active();
			// return new RGB(64, 64, 64); // 0x404040
		}
	}

	String getText() {

		if (null != _name) {
			return _name;
		} else {
			return _colorDefinition.getVisibleName();
		}
	}
}
