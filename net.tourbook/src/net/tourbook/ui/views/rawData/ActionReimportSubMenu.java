/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.importdata.RawDataManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * The action to delete a tour is displayed in a sub menu that it is not accidentally be run
 */
public class ActionReimportSubMenu extends Action implements IMenuCreator {

	private Menu								_menu;

	private ActionReimportEntireTour			_actionReimportEntireTour;
	private ActionReimportOnlyTimeSlices		_actionReimportOnlyTimeSlices;
	private ActionReimportOnlyAltitudeValues	_actionReimportOnlyAltitudeValues;

	private ITourViewer3						_tourViewer;

	private class ActionReimportEntireTour extends Action {

		public ActionReimportEntireTour() {
			setText(Messages.Import_Data_Action_Reimport_EntireTour);
		}

		@Override
		public void run() {
			RawDataManager.getInstance().actionReimportTour(RawDataManager.ReImport.Tour, _tourViewer);
		}

	}

	private class ActionReimportOnlyAltitudeValues extends Action {

		public ActionReimportOnlyAltitudeValues() {
			setText(Messages.Import_Data_Action_Reimport_OnlyAltitudeValues);
		}

		@Override
		public void run() {
			RawDataManager.getInstance().actionReimportTour(RawDataManager.ReImport.OnlyAltitudeValues, _tourViewer);
		}

	}

	private class ActionReimportOnlyTimeSlices extends Action {

		public ActionReimportOnlyTimeSlices() {
			setText(Messages.Import_Data_Action_Reimport_OnlyTimeSlices);
		}

		@Override
		public void run() {
			RawDataManager.getInstance().actionReimportTour(RawDataManager.ReImport.AllTimeSlices, _tourViewer);
		}

	}

	public ActionReimportSubMenu(final ITourViewer3 tourViewer) {

		super(Messages.Import_Data_Action_Reimport_Tour, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourViewer = tourViewer;

		_actionReimportEntireTour = new ActionReimportEntireTour();
		_actionReimportOnlyTimeSlices = new ActionReimportOnlyTimeSlices();
		_actionReimportOnlyAltitudeValues = new ActionReimportOnlyAltitudeValues();
	}

	public void dispose() {
		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void fillMenu(final Menu menu) {

		new ActionContributionItem(_actionReimportOnlyAltitudeValues).fill(menu, -1);
		new ActionContributionItem(_actionReimportOnlyTimeSlices).fill(menu, -1);
		new ActionContributionItem(_actionReimportEntireTour).fill(menu, -1);
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		dispose();

		_menu = new Menu(parent);

		// Add listener to repopulate the menu each time
		_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(final MenuEvent e) {

				// dispose old menu items
				for (final MenuItem menuItem : ((Menu) e.widget).getItems()) {
					menuItem.dispose();
				}

				fillMenu(_menu);
			}
		});

		return _menu;
	}

}
