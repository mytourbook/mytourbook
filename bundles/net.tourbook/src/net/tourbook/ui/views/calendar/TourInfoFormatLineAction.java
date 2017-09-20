package net.tourbook.ui.views.calendar;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

class TourInfoFormatLineAction extends Action implements IMenuCreator {

	/**
	 * 
	 */
	private final CalendarView calendarView;
	int		line;
	Menu	formatMenu;

	TourInfoFormatLineAction(CalendarView calendarView, final String text, final int line) {

		super(text, AS_DROP_DOWN_MENU);
		this.calendarView = calendarView;
		this.line = line;

		setMenuCreator(this);
	}

	@Override
	public void dispose() {
		if (formatMenu != null) {
			formatMenu.dispose();
			formatMenu = null;
		}
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		formatMenu = new Menu(parent);

		for (int i = 0; i < this.calendarView._tourInfoFormatter.length; i++) {
			final ActionContributionItem item = new ActionContributionItem(this.calendarView._actionSetTourInfoFormat[line][i]);
			item.fill(formatMenu, -1);
		}

		return formatMenu;
	}

	@Override
	public void run() {
		//
	}

}