/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import net.tourbook.views.TourMarkerView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionTourMarker extends Action {

	private TourChart	fTourChart;

	public ActionTourMarker(TourChart chartPanel) {

		super(null, AS_PUSH_BUTTON);

		setToolTipText(Messages.Tour_Action_open_marker_editor);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Tour_Image_open_marker_editor));

		fTourChart = chartPanel;
	}

	public void run() {

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {

				/*
				 * the opened view can't be activated otherwise the fire event
				 * wouldn't not work
				 */
				window.getActivePage().showView(
						TourMarkerView.ID,
						null,
						IWorkbenchPage.VIEW_VISIBLE);

				fTourChart.fireSelectionTourChart();

			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
						+ e.getMessage());
			}
		}
	}

}
