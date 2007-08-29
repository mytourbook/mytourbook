package net.tourbook.ui.views.rawData;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.importdata.RawDataManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionAdjustYear extends Action implements IMenuCreator {

	public Calendar		fCalendar	= GregorianCalendar.getInstance();
	private Menu		fMenu;

	private ActionYear	fActionYearLast;
	private ActionYear	fActionYearThis;
	private ActionYear	fActionYearNext;

	private class ActionYear extends Action {

		private int	fYear;

		public ActionYear(int year) {
			super(Integer.toString(year), AS_RADIO_BUTTON);

			fYear = year;
		}

		@Override
		public void run() {
			RawDataManager.getInstance().setImportYear(fYear);
		}

	}

	public ActionAdjustYear(RawDataView rawDataView) {

		super("Adjust Imported Year", AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fCalendar.setTime(new Date());
		int thisYear = fCalendar.get(Calendar.YEAR);

		fActionYearLast = new ActionYear(thisYear - 1);
		fActionYearThis = new ActionYear(thisYear);
		fActionYearNext = new ActionYear(thisYear + 1);

		// set current year as default
		fActionYearThis.setChecked(true);
	}

	private void addActionToMenu(Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {
		return null;
	}

	public Menu getMenu(Menu parent) {

		fMenu = new Menu(parent);

		addActionToMenu(fActionYearLast);
		addActionToMenu(fActionYearThis);
		addActionToMenu(fActionYearNext);

		return fMenu;
	}

}
