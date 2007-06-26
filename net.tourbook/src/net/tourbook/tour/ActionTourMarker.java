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
import net.tourbook.ui.views.TourMarkerView;
import net.tourbook.ui.views.tourBook.MarkerDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionTourMarker extends Action implements IMenuCreator {

	private TourChart				fTourChart;

	private Menu					fMenu;

	private ActionOpenMarkerView	fActionOpenMarkerView;


	private class ActionOpenMarkerView extends Action {

		public ActionOpenMarkerView() {
			setText(Messages.Tour_Action_open_marker_view);
		}

		public void run() {

			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

			if (window != null) {
				try {

					/*
					 * the opened view can't be activated otherwise the fire event wouldn't work
					 */
					window.getActivePage().showView(
							TourMarkerView.ID,
							null,
							IWorkbenchPage.VIEW_VISIBLE);

					fTourChart.fireTourChartSelection();

				} catch (PartInitException e) {
					MessageDialog.openError(window.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
							+ e.getMessage());
				}
			}
		}
	}

	public ActionTourMarker(TourChart tourChart) {

		super(null, Action.AS_DROP_DOWN_MENU);

		fTourChart = tourChart;

		setToolTipText(Messages.Tour_Action_open_marker_editor);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_open_marker_editor));

		// fActionOpenMarkerEditor = new ActionOpenMarkerEditor();
		fActionOpenMarkerView = new ActionOpenMarkerView();

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

	public void run() {
		
		(new MarkerDialog(Display.getCurrent().getActiveShell(), fTourChart.fTourData, null))
				.open();

		// force the tour to be saved
		fTourChart.setTourDirty();

		// update chart
		fTourChart.updateMarkerLayer(true);

		// update marker list and other listener
		fTourChart.fireTourChartSelection();
	}

}
