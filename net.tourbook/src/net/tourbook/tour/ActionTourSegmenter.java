/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.views.TourSegmenterView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionTourSegmenter extends Action {

	private TourChart	fTourChart;

	public ActionTourSegmenter(final TourChart tourChart) {

		super(null, AS_PUSH_BUTTON);

		fTourChart = tourChart;

		setToolTipText(Messages.Tour_Action_open_tour_segmenter_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__open_tour_segmenter));

	}

	@Override
	public void run() {

		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {
				window.getActivePage().showView(TourSegmenterView.ID, null, IWorkbenchPage.VIEW_VISIBLE);

				fTourChart.fireTourChartSelection();

			} catch (final PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
						+ e.getMessage());
			}
		}
	}
}
