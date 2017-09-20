package net.tourbook.ui.views.calendar;

import org.eclipse.jface.action.Action;

class WeekSummaryFormatAction extends Action {

	/**
	 * 
	 */
	private final CalendarView	_calendarView;
	WeekSummaryFormatter		_formatter;
	int							_forLine;

	WeekSummaryFormatAction(final CalendarView calendarView,
							final String text,
							final WeekSummaryFormatter formatter,
							final int forLine) {

		super(text, AS_RADIO_BUTTON);

		_calendarView = calendarView;
		_formatter = formatter;
		_forLine = forLine;
	}

	@Override
	public void run() {

		this._calendarView.getCalendarGraph().setWeekSummaryFormatter(_forLine, _formatter);

		for (int i = 0; i < this._calendarView.tourWeekSummaryFormatter.length; i++) {

			this._calendarView._actionSetWeekSummaryFormat[_forLine][i].setChecked(i == _formatter.index);
		}
	}
}
