/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation; either version 2 of the License, or (at your option) any later 
 * version.
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class ActionOpenMarkerView extends Action implements IEditorActionDelegate {

	/**
	 * 
	 */
	private final ActionTourMarker	fActionTourMarker;


	public ActionOpenMarkerView(ActionTourMarker actionTourMarker) {
		fActionTourMarker = actionTourMarker;
		fActionTourMarker.setText(Messages.Tour_Action_open_marker_view);
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		int a=0;
	}

	public void run(IAction action) {
		int a=0;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		int a=0;
	}

//	public void dispose() {}
//
//	public void init(IWorkbenchWindow window) {
//		fWindow = window;
//	}

//	public void run() {
//
//		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//
//		if (window != null) {
//			try {
//
//				/*
//				 * the opened view can't be activated otherwise the fire event wouldn't work
//				 */
//				window.getActivePage().showView(TourMarkerView.ID,
//						null,
//						IWorkbenchPage.VIEW_VISIBLE);
//				/*
//				 * the opened view can't be activated otherwise the fire event wouldn't work
//				 */
//				window.getActivePage().showView(TourMarkerView.ID,
//						null,
//						IWorkbenchPage.VIEW_VISIBLE);
//
//				this.fActionTourMarker.fTourChart.fireTourChartSelection();
//
//			} catch (PartInitException e) {
//				MessageDialog.openError(window.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
//						+ e.getMessage());
//			}
//		}
//	}

//	public void run(IAction action) {
//
//		try {
//
//			/*
//			 * the opened view can't be activated otherwise the fire event wouldn't work
//			 */
//			fWindow.getActivePage().showView(TourMarkerView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
//
//			this.fActionTourMarker.fTourChart.fireTourChartSelection();
//
//		} catch (PartInitException e) {
//			MessageDialog.openError(fWindow.getShell(), "Error", "Error opening view:" //$NON-NLS-1$ //$NON-NLS-2$
//					+ e.getMessage());
//		}
//
//	}
//
//	public void selectionChanged(IAction action, ISelection selection) {
//
//		if (selection != null) {
//
//		}
//	}
}
