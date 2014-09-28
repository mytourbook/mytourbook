/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionSetGeoPositionAccuracy extends Action implements IMenuCreator {

	static final int					DISABLED_ACCURACY	= -1;
	static final int					DEFAULT_ACCURACY	= 3;
	static final int					MAX_ACCURACY		= 6;

	private int							_selectedAccuracy	= DEFAULT_ACCURACY;

	private TourMarkerAllView			_tourMarkerAllView;

	private ArrayList<ActionAccuracy>	_sccuracyActions;

	private Menu						_menu;

	private class ActionAccuracy extends Action {

		private static final String	OSX_SPACER_STRING	= UI.SPACE1;

		private int					__positionAccuracy;

		public ActionAccuracy(final int accuracy, final String label) {

			// add space before the label otherwise OSX will not display the menu item,
			super(OSX_SPACER_STRING + NLS.bind(label, Integer.toString(accuracy)), AS_RADIO_BUTTON);

			__positionAccuracy = accuracy;
		}

		@Override
		public void run() {

			setAccuracy(__positionAccuracy);

			_tourMarkerAllView.actionSetGeoPositionAccuracy(_selectedAccuracy);
		}
	}

	public ActionSetGeoPositionAccuracy(final TourMarkerAllView tourMarkerAllView) {

		super(Messages.Action_TourMarkerAllView_SetGeoPositionAccuracy, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourMarkerAllView = tourMarkerAllView;

		_sccuracyActions = new ArrayList<ActionAccuracy>();

		for (int actionIndex = DISABLED_ACCURACY; actionIndex < MAX_ACCURACY; actionIndex++) {

			final ActionAccuracy action;

			if (actionIndex == DISABLED_ACCURACY) {

				// action: disable accuracy

				action = new ActionAccuracy(-1, Messages.Action_TourMarkerAllView_DisableGeoPositionAccuracy);

			} else {

				action = new ActionAccuracy(actionIndex, Messages.Action_TourMarkerAllView_GeoPositionAccuracy);
			}

			_sccuracyActions.add(action);
		}
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

	public int getDimLevel() {
		return _selectedAccuracy;
	}

	public Menu getMenu(final Control parent) {
		return null;
	}

	public Menu getMenu(final Menu parent) {

		_menu = new Menu(parent);

		for (final ActionAccuracy action : _sccuracyActions) {
			addActionToMenu(action);
		}

		return _menu;
	}

	/**
	 * Set the geo position accuracy.
	 * 
	 * @param accuracy
	 */
	public void setAccuracy(final int accuracy) {

		_selectedAccuracy = accuracy;

		/*
		 * check selected accuracy and uncheck others
		 */
		for (final ActionAccuracy dimAction : _sccuracyActions) {

			final int actionAccurayValue = dimAction.__positionAccuracy;

			if (actionAccurayValue == _selectedAccuracy) {

				dimAction.setChecked(true);

			} else {

				if (dimAction.isChecked()) {
					dimAction.setChecked(false);
				}
			}
		}
	}

}
