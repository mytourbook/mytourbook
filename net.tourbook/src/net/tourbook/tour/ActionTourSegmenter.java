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

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.views.TourSegmenterView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionTourSegmenter extends Action {

	private TourChart	fTourChart;

	public ActionTourSegmenter(TourChart tourChart) {

		super(null, AS_PUSH_BUTTON);

		fTourChart = tourChart;

		setToolTipText("Tour Segmenter");
		setImageDescriptor(TourbookPlugin.getImageDescriptor("tour-segmenter.gif"));

	}

	public void run() {

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {
				window.getActivePage().showView(
						TourSegmenterView.ID,
						null,
						IWorkbenchPage.VIEW_VISIBLE);

				fTourChart.fireSelectionTourChart();

			} catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:"
						+ e.getMessage());
			}
		}
	}
}
