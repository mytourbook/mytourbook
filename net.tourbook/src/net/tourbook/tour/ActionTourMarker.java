/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.tourBook.MarkerDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class ActionTourMarker extends Action implements IMenuCreator {

	TourChart						fTourChart;

	private Menu					fMenu;

	private ActionOpenMarkerView	fActionOpenMarkerView;

	public ActionTourMarker(TourChart tourChart) {

		super(null, Action.AS_DROP_DOWN_MENU);

		fTourChart = tourChart;

		setToolTipText(Messages.Tour_Action_open_marker_editor);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_open_marker_editor));

//		fActionOpenMarkerView = new ActionOpenMarkerView(this);

		setMenuCreator(this);
	}

	private void addItem(Action action) {
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

		fMenu = new Menu(parent);

		// addItem(fActionOpenMarkerEditor);
		addItem(fActionOpenMarkerView);

		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public void run() {

		(new MarkerDialog(Display.getCurrent().getActiveShell(), fTourChart.fTourData, null)).open();

		// force the tour to be saved
		fTourChart.setTourDirty(true);

		// update chart
		fTourChart.updateMarkerLayer(true);

		// update marker list and other listener
		fTourChart.fireTourChartSelection();
	}

}
