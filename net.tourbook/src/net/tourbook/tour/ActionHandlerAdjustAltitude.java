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

import net.tourbook.database.TourDatabase;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerAdjustAltitude extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// open the dialog to adjust the altitude

		IEditorPart editorPart = HandlerUtil.getActiveEditorChecked(event);

		TourEditor tourEditor;
		TourChart tourChart;

		if (editorPart instanceof TourEditor) {
			tourEditor = ((TourEditor) editorPart);
			tourChart = tourEditor.getTourChart();
		} else {
			return null;
		}

		AdjustAltitudeDialog dialog;

		if (tourChart == null) {
			return null;
		}

		dialog = new AdjustAltitudeDialog(tourChart.getShell(), tourChart);
		dialog.create();
		dialog.init();

		if (dialog.open() == Window.OK) {
			tourEditor.setTourDirty();
		} else {
			dialog.restoreOriginalAltitudeValues();
		}
		tourChart.updateTourChart(true);

		/*
		 * when in the tour chart view this tour is visible, another tour is selected and then again
		 * this tour, the displayed data are from the cache which where changed from this dialog,
		 * but the tour chart view should show the tour from the database
		 */
		TourManager.getInstance().removeTourFromCache(tourChart.getTourData().getTourId());

		TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED);

		return null;
	}
}
