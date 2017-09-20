package net.tourbook.ui.views.calendar;

import net.tourbook.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;

class NumberOfToursPerDayAction extends Action {

		/**
		 * 
		 */
		private final CalendarView calendarView;
		private int _numberOfTours;

		NumberOfToursPerDayAction(CalendarView calendarView, final int numberOfTours) {

			super(null, AS_RADIO_BUTTON);
			this.calendarView = calendarView;

			_numberOfTours = numberOfTours;

			if (0 == numberOfTours) {
				setText(Messages.Calendar_View_Action_DisplayTours_All);
			} else if (1 == numberOfTours) {
				setText(Messages.Calendar_View_Action_DisplayTours_1ByDay);
			} else {
				setText(NLS.bind(Messages.Calendar_View_Action_DisplayTours_ByDay, numberOfTours));
			}
		}

		@Override
		public void run() {

			this.calendarView._calendarGraph.setNumberOfToursPerDay(_numberOfTours);

			for (int j = 0; j < 5; j++) {
				this.calendarView._actionSetNumberOfToursPerDay[j].setChecked((j == _numberOfTours));
			}
//			if (null != _setTourSizeDynamic) {
//				_setTourSizeDynamic.setEnabled(numberOfTours != 0);
//			}
		};
	}