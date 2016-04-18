/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 */
public class ActionCadenceSubMenu extends Action implements IMenuCreator {

	private Menu			_menu;

	private ActionSetRpm	_actionSetRpm;
	private ActionSetSpm	_actionSetSpm;

	private ITourProvider	_tourProvider;

	private class ActionSetRpm extends Action {

		public ActionSetRpm() {

			super(Messages.Action_Cadence_Set_Rpm, AS_CHECK_BOX);
		}

		@Override
		public void run() {
			setCadenceMultiplier(1.0f);
		}
	}

	private class ActionSetSpm extends Action {

		public ActionSetSpm() {
			super(Messages.Action_Cadence_Set_Spm, AS_CHECK_BOX);
		}

		@Override
		public void run() {
			setCadenceMultiplier(2.0f);
		}
	}

	public ActionCadenceSubMenu(final ITourProvider tourViewer) {

		super(Messages.Action_Cadence_Set, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourProvider = tourViewer;

		_actionSetRpm = new ActionSetRpm();
		_actionSetSpm = new ActionSetSpm();
	}

	@Override
	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void enableActions() {

		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

		int numSpm = 0;
		int numRpm = 0;

		for (final TourData tourData : selectedTours) {

			if (tourData.isCadenceSpm()) {
				numSpm++;
			} else {
				numRpm++;
			}
		}

		if (numSpm > 0 && numRpm > 0) {
			_actionSetRpm.setChecked(false);
			_actionSetSpm.setChecked(false);
		} else if (numRpm > 0) {
			_actionSetRpm.setChecked(true);
			_actionSetSpm.setChecked(false);
		} else if (numSpm > 0) {
			_actionSetRpm.setChecked(false);
			_actionSetSpm.setChecked(true);
		}
	}

	private void fillMenu(final Menu menu) {

		new ActionContributionItem(_actionSetSpm).fill(menu, -1);
		new ActionContributionItem(_actionSetRpm).fill(menu, -1);
	}

	@Override
	public Menu getMenu(final Control parent) {
		return null;
	}

	@Override
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

				enableActions();
			}
		});

		return _menu;
	}

	private void setCadenceMultiplier(final float cadenceMultiplier) {

//		 * 1.0f = Revolutions per minute (RPM)
//		 * 2.0f = Steps per minute (SPM)

		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
		final ArrayList<TourData> modifiedTours = new ArrayList<>();

		for (final TourData tourData : selectedTours) {

			if (tourData.getCadenceMultiplier() != cadenceMultiplier) {

				// cadence multiplier is not the same

				tourData.setCadenceMultiplier(cadenceMultiplier);

				modifiedTours.add(tourData);
			}
		}

		if (modifiedTours.size() > 0) {
			TourManager.saveModifiedTours(modifiedTours);
		}
	}

}
