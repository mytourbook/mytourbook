/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class ActionSynchTourZoomLevel extends Action implements IMenuCreator {

	private Menu			fMenu;

	private ActionZoomLevel	fActionZoomLevel_0;
	private ActionZoomLevel	fActionZoomLevel_1;
	private ActionZoomLevel	fActionZoomLevel_2;
	private ActionZoomLevel	fActionZoomLevel_3;

	private int				fZoomLevel	= 0;

	private class ActionZoomLevel extends Action {

		private int	fActionZoomLevel;

		public ActionZoomLevel(int zoomLevel, String label) {

			super(NLS.bind(label, Integer.toString(zoomLevel)), AS_RADIO_BUTTON);

			fActionZoomLevel = zoomLevel;
		}

		@Override
		public void run() {
			fZoomLevel = fActionZoomLevel;
			PaintManager.getInstance().setSynchTourZoomLevel(fActionZoomLevel);
		}

	}

	public ActionSynchTourZoomLevel(OSMView osmView) {

		super(Messages.map_action_zoom_level_centered_tour, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fActionZoomLevel_0 = new ActionZoomLevel(0, Messages.map_action_zoom_level_default);
		fActionZoomLevel_1 = new ActionZoomLevel(-1, Messages.map_action_zoom_level_x_value);
		fActionZoomLevel_2 = new ActionZoomLevel(-2, Messages.map_action_zoom_level_x_value);
		fActionZoomLevel_3 = new ActionZoomLevel(-3, Messages.map_action_zoom_level_x_value);

		// set current year as default
		fActionZoomLevel_0.setChecked(true);
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

		addActionToMenu(fActionZoomLevel_0);
		addActionToMenu(fActionZoomLevel_1);
		addActionToMenu(fActionZoomLevel_2);
		addActionToMenu(fActionZoomLevel_3);

		return fMenu;
	}

	public int getZoomLevel() {
		return fZoomLevel;
	}

	public void setZoomLevel(Integer zoomLevel) {

		fZoomLevel = zoomLevel;
		PaintManager.getInstance().setSynchTourZoomLevel(zoomLevel);

		fActionZoomLevel_0.setChecked(false);
		fActionZoomLevel_1.setChecked(false);
		fActionZoomLevel_2.setChecked(false);
		fActionZoomLevel_3.setChecked(false);

		switch (zoomLevel) {
		case -1:
			fActionZoomLevel_1.setChecked(true);
			break;
		case -2:
			fActionZoomLevel_2.setChecked(true);
			break;
		case -3:
			fActionZoomLevel_3.setChecked(true);
			break;

		case 0:
		default:
			fActionZoomLevel_0.setChecked(true);
			break;
		}
	}

}
