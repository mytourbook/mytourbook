/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class ActionSetMarkerLabelPositionMenu extends Action implements IMenuCreator {

	private Menu								_menu;

	private TourChart							_tourChart;
	private TourMarker							_tourMarker;

	private ArrayList<ActionSetMarkerPosition>	_allActions	= new ArrayList<ActionSetMarkerPosition>();

	private class ActionSetMarkerPosition extends Action {

		private int	labelPosId;

		public ActionSetMarkerPosition(final String labelPosition, final int labelPosId) {

			super(labelPosition, AS_CHECK_BOX);

			this.labelPosId = labelPosId;
		}

		@Override
		public void run() {
			_tourChart.actionSetMarkerLabelPosition(_tourMarker, labelPosId);
		}
	}

	public ActionSetMarkerLabelPositionMenu(final TourChart tourChart) {

		super(Messages.Tour_Action_Marker_SetLabelPosition, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_tourChart = tourChart;

		createActions();
	}

	/**
	 * Adds all tour types to the menu manager
	 * 
	 * @param menuMgr
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise {@link TourData} from
	 *            the tour provider is only modified
	 */
	public static void fillMenu(final IMenuManager menuMgr, final ITourProvider tourProvider, final boolean isSaveTour) {

	}

	private void addActionToMenu(final Action action, final Menu menu) {

		new ActionContributionItem(action).fill(menu, -1);
	}

	private void createActions() {

		final String[] labelPositions = TourMarker.LABEL_POSITIONS;

		for (int labelPosId = 0; labelPosId < labelPositions.length; labelPosId++) {

			final String labelPosition = labelPositions[labelPosId];

			_allActions.add(new ActionSetMarkerPosition(labelPosition, labelPosId));
		}
	}

	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}
	}

	private void fillMenu(final Menu menu) {

		final int currentLabelPosition = _tourMarker.getLabelPosition();

		for (final ActionSetMarkerPosition action : _allActions) {

			final boolean isCurrentPosition = action.labelPosId == currentLabelPosition;
			action.setChecked(isCurrentPosition);
			action.setEnabled(!isCurrentPosition);

			addActionToMenu(action, menu);
		}
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

	public void setTourMarker(final TourMarker tourMarker) {
		_tourMarker = tourMarker;
	}

}
