package net.tourbook.ui.views.calendar;

import org.eclipse.jface.action.Action;

class WeekSummaryFormatAction extends Action {

	/**
	 * 
	 */
	private final CalendarView calendarView;
	WeekSummaryFormatter	formatter;
	int						forLine;

	WeekSummaryFormatAction(CalendarView calendarView, final String text, final WeekSummaryFormatter formatter, final int forLine) {

		super(text, AS_RADIO_BUTTON);
		this.calendarView = calendarView;
		this.formatter = formatter;
		this.forLine = forLine;
	}

	@Override
	public void run() {
		this.calendarView._calendarGraph.setWeekSummaryFormatter(forLine, formatter);
		for (int i = 0; i < this.calendarView._tourWeekSummaryFormatter.length; i++) {
			this.calendarView._actionSetWeekSummaryFormat[forLine][i].setChecked(i == formatter.index);
		}
	}
}