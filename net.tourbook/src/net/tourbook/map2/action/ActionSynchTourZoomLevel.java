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

package net.tourbook.map2.action;

import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.map2.view.TourPainterConfiguration;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionSynchTourZoomLevel extends Action implements IMenuCreator {

	private Menu			_menu;

	private ActionZoomLevel	_actionZoomLevel_0;
	private ActionZoomLevel	_actionZoomLevel_1;
	private ActionZoomLevel	_actionZoomLevel_2;
	private ActionZoomLevel	_actionZoomLevel_3;

	private int				_zoomLevel	= 0;

	private class ActionZoomLevel extends Action {

		private static final String	OSX_SPACER_STRING	= " ";	//$NON-NLS-1$
		private int					_actionZoomLevel;

		public ActionZoomLevel(final int zoomLevel, final String label) {

			// add space before the label otherwise OSX will not display the menu item,
			super(OSX_SPACER_STRING + NLS.bind(label, Integer.toString(zoomLevel)), AS_RADIO_BUTTON);

			_actionZoomLevel = zoomLevel;
		}

		@Override
		public void run() {
			_zoomLevel = _actionZoomLevel;
			TourPainterConfiguration.getInstance().setSynchTourZoomLevel(_actionZoomLevel);
		}
	}

	public ActionSynchTourZoomLevel(final Map2View osmView) {

		super(Messages.map_action_zoom_level_centered_tour, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		_actionZoomLevel_0 = new ActionZoomLevel(0, Messages.map_action_zoom_level_default);
		_actionZoomLevel_1 = new ActionZoomLevel(-1, Messages.map_action_zoom_level_x_value);
		_actionZoomLevel_2 = new ActionZoomLevel(-2, Messages.map_action_zoom_level_x_value);
		_actionZoomLevel_3 = new ActionZoomLevel(-3, Messages.map_action_zoom_level_x_value);

		// set current year as default
		_actionZoomLevel_0.setChecked(true);
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

		addActionToMenu(_actionZoomLevel_0);
		addActionToMenu(_actionZoomLevel_1);
		addActionToMenu(_actionZoomLevel_2);
		addActionToMenu(_actionZoomLevel_3);

		return _menu;
	}

	public int getZoomLevel() {
		return _zoomLevel;
	}

	public void setZoomLevel(final int zoomLevel) {

		_zoomLevel = zoomLevel;
		TourPainterConfiguration.getInstance().setSynchTourZoomLevel(zoomLevel);

		_actionZoomLevel_0.setChecked(false);
		_actionZoomLevel_1.setChecked(false);
		_actionZoomLevel_2.setChecked(false);
		_actionZoomLevel_3.setChecked(false);

		switch (zoomLevel) {
		case -1:
			_actionZoomLevel_1.setChecked(true);
			break;
		case -2:
			_actionZoomLevel_2.setChecked(true);
			break;
		case -3:
			_actionZoomLevel_3.setChecked(true);
			break;

		case 0:
		default:
			_actionZoomLevel_0.setChecked(true);
			break;
		}
	}

}
