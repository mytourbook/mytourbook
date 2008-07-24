/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
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

		public ActionYear(final int year) {
			super(Integer.toString(year), AS_RADIO_BUTTON);

			fYear = year;
		}

		@Override
		public void run() {
			RawDataManager.getInstance().setImportYear(fYear);
		}

	}

	public ActionAdjustYear(final RawDataView rawDataView) {

		super(Messages.import_data_action_adjust_imported_year, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fCalendar.setTime(new Date());
		final int thisYear = fCalendar.get(Calendar.YEAR);

		fActionYearLast = new ActionYear(thisYear - 1);
		fActionYearThis = new ActionYear(thisYear);
		fActionYearNext = new ActionYear(thisYear + 1);

		// set current year as default
		fActionYearThis.setChecked(true);
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		fMenu = new Menu(parent);

		addActionToMenu(fActionYearLast);
		addActionToMenu(fActionYearThis);
		addActionToMenu(fActionYearNext);

		return fMenu;
	}

}
