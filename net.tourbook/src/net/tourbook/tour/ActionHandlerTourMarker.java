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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class ActionHandlerTourMarker extends AbstractHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		final IEditorPart editorPart = HandlerUtil.getActiveEditorChecked(event);

		TourEditor tourEditor;
		TourChart tourChart;

		if (editorPart instanceof TourEditor) {
			tourEditor = ((TourEditor) editorPart);
			tourChart = tourEditor.getTourChart();
		} else {
			return null;
		}

		(new MarkerDialog(Display.getCurrent().getActiveShell(), tourChart.getTourData(), null)).open();

		/*
		 * Currently the dialog works with the markers from the tour editor not with a backup, so
		 * changes in the dialog are made in the tourdata of the tour editor -> the tour will be
		 * dirty when this dialog was opened
		 */

		// force the tour to be saved
		tourChart.setTourDirty(true);

		// update chart
		tourChart.updateMarkerLayer(true);

		// update marker list and other listener
		TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED);

		return null;
	}

}
