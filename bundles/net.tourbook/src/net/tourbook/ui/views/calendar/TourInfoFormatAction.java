package net.tourbook.ui.views.calendar;

import net.tourbook.ui.views.calendar.CalendarView.TourInfoFormatter;

import org.eclipse.jface.action.Action;

class TourInfoFormatAction extends Action {

	/**
	 * 
	 */
	private final CalendarView calendarView;
	TourInfoFormatter	formatter;
	int					forLine;

	TourInfoFormatAction(CalendarView calendarView, final String text, final TourInfoFormatter formatter, final int forLine) {

		super(text, AS_RADIO_BUTTON);
		this.calendarView = calendarView;
		this.formatter = formatter;
		this.forLine = forLine;
	}

	@Override
	public void run() {
		this.calendarView.getCalendarGraph().setTourInfoFormatter(forLine, formatter);
		for (int i = 0; i < this.calendarView.tourInfoFormatter.length; i++) {
			this.calendarView._actionSetTourInfoFormat[forLine][i].setChecked(i == formatter.index);
		}
	}
}