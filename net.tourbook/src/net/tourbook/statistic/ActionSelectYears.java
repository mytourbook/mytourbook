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
package net.tourbook.statistic;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.ui.views.tourCatalog.TourCatalogViewYearStatistic;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionSelectYears extends Action implements IMenuCreator {

	private Menu							_menu;

	private ArrayList<ActionYear>			_yearActions		= new ArrayList<ActionYear>();

	private TourCatalogViewYearStatistic	_yearStatistic;

	private int								_selectedYear;

	private int								_selectionCounter	= 0;

	private class ActionYear extends Action {

		private int	__actionYear;

		public ActionYear(final int year) {

			super(Integer.toString(year), AS_RADIO_BUTTON);

			__actionYear = year;
		}

		@Override
		public void run() {

			if (_selectionCounter == 0) {

				setChecked(false);

			} else if (_selectionCounter == 1) {

				/*
				 * there is somewhere a bug that the first selection does not work
				 */

				setChecked(true);

				_selectedYear = __actionYear;
				_yearStatistic.onExecuteSelectNumberOfYears(__actionYear);

			} else {

				if (isChecked()) {

					// ignore uncheck event

					_selectedYear = __actionYear;
					_yearStatistic.onExecuteSelectNumberOfYears(__actionYear);
				}
			}

			_selectionCounter++;
		}
	}

	public ActionSelectYears(final TourCatalogViewYearStatistic yearStatistic) {

		super(Messages.tourCatalog_view_action_number_of_years, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		_yearStatistic = yearStatistic;

		// create action for each year
		for (int yearIndex = 0; yearIndex < 20; yearIndex++) {
			_yearActions.add(new ActionYear(yearIndex + 1));
		}

		// set as default
		_yearActions.get(0).setChecked(true);
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

		for (final ActionYear yearAction : _yearActions) {
			addActionToMenu(yearAction);
		}

		return _menu;
	}

	public int getSelectedYear() {
		return _selectedYear;
	}

	public void setNumberOfYears(final int selectedYear) {

		_selectedYear = selectedYear;

		// update check status for the menu items
		for (final ActionYear yearAction : _yearActions) {
			if (yearAction.__actionYear == selectedYear) {
				yearAction.setChecked(true);
			} else {
				yearAction.setChecked(false);
			}
		}
	}
}
