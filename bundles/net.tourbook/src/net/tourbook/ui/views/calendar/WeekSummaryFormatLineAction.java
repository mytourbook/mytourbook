package net.tourbook.ui.views.calendar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

class WeekSummaryFormatLineAction extends Action implements IMenuCreator {

	/**
	 * 
	 */
	private final CalendarView calendarView;
	int		line;
	Menu	summaryMenu;

	WeekSummaryFormatLineAction(CalendarView calendarView, final String text, final int line) {

		super(text, AS_DROP_DOWN_MENU);
		this.calendarView = calendarView;
		this.line = line;

		setMenuCreator(this);
	}

	@Override
	public void dispose() {
		if (summaryMenu != null) {
			summaryMenu.dispose();
			summaryMenu = null;
		}
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		summaryMenu = new Menu(parent);

		for (int i = 0; i < this.calendarView.tourWeekSummaryFormatter.length; i++) {
			final ActionContributionItem item = new ActionContributionItem(this.calendarView._actionSetWeekSummaryFormat[line][i]);
			item.fill(summaryMenu, -1);
		}

		return summaryMenu;
	}

	@Override
	public void run() {
		//
	}

}