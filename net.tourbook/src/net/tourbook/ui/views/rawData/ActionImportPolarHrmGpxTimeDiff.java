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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.importdata.RawDataManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.byteholder.geoclipse.map.UI;

public class ActionImportPolarHrmGpxTimeDiff extends Action implements IMenuCreator {

	private static final String		PREFIX_PLUS			= "+ ";

	private Menu					_menu;

	private ArrayList<ActionHour>	_actionHours		= new ArrayList<ActionHour>();

	private class ActionHour extends Action {

		private int	_minutes;

		public ActionHour(final String prefix, final int minutes) {

			super(prefix + Integer.toString(minutes / 60), AS_RADIO_BUTTON);

			_minutes = minutes;
		}

		@Override
		public void run() {
			RawDataManager.getInstance().setImportPolarHrmGpxTimeDiff(_minutes);
		}

	}

	public ActionImportPolarHrmGpxTimeDiff(final RawDataView rawDataView) {

		super(Messages.Import_Data_Action_AdjustPolarHrmGpxTimeDiff, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		for (int hour = -12; hour < 13; hour++) {


			final String prefix = hour > 0 ? PREFIX_PLUS : UI.EMPTY_STRING;

			_actionHours.add(new ActionHour(prefix, hour * 60));
		}

		// set default
		_actionHours.get(0).setChecked(true);
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

		for (final ActionHour actionHour : _actionHours) {
			addActionToMenu(actionHour);
		}

		return _menu;
	}

	public void setTimeDiff(final int timeDiff) {

		// uncheck all
		for (final ActionHour actionHour : _actionHours) {
			actionHour.setChecked(false);
		}

		for (final ActionHour actionHour : _actionHours) {
			if (actionHour._minutes == timeDiff) {
				actionHour.setChecked(true);
				return;
			}
		}

		// set default
		_actionHours.get(0).setChecked(true);
	}

}
