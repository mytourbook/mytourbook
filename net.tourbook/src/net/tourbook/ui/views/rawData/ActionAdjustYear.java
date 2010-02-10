/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
 
import net.tourbook.Messages;
import net.tourbook.importdata.RawDataManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.joda.time.DateTime;

public class ActionAdjustYear extends Action implements IMenuCreator {

	private Menu		_menu;

	private ActionYear	_actionYear_10;
	private ActionYear	_actionYear_9;
	private ActionYear	_actionYear_8;
	private ActionYear	_actionYear_7;
	private ActionYear	_actionYear_6;
	private ActionYear	_actionYear_5;
	private ActionYear	_actionYear_4;
	private ActionYear	_actionYear_3;
	private ActionYear	_actionYear_2;
	private ActionYear	_actionYearLast;
	private ActionYear	_actionYearThis;
	private ActionYear	_actionYearNext;

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

		final int thisYear = new DateTime().getYear();

		_actionYear_10 = new ActionYear(thisYear - 10);
		_actionYear_9 = new ActionYear(thisYear - 9);
		_actionYear_8 = new ActionYear(thisYear - 8);
		_actionYear_7 = new ActionYear(thisYear - 7);
		_actionYear_6 = new ActionYear(thisYear - 6);
		_actionYear_5 = new ActionYear(thisYear - 5);
		_actionYear_4 = new ActionYear(thisYear - 4);
		_actionYear_3 = new ActionYear(thisYear - 3);
		_actionYear_2 = new ActionYear(thisYear - 2);
		_actionYearLast = new ActionYear(thisYear - 1);
		_actionYearThis = new ActionYear(thisYear);
		_actionYearNext = new ActionYear(thisYear + 1);

		// set current year as default
		_actionYearThis.setChecked(true);
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		_menu = new Menu(parent);

		addActionToMenu(_actionYear_10);
		addActionToMenu(_actionYear_9);
		addActionToMenu(_actionYear_8);
		addActionToMenu(_actionYear_7);
		addActionToMenu(_actionYear_6);
		addActionToMenu(_actionYear_5);
		addActionToMenu(_actionYear_4);
		addActionToMenu(_actionYear_3);
		addActionToMenu(_actionYear_2);
		addActionToMenu(_actionYearLast);
		addActionToMenu(_actionYearThis);
		addActionToMenu(_actionYearNext);

		return _menu;
	}

}
