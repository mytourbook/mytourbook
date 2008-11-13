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

package net.tourbook.ui.views.tourDataEditor;

import net.tourbook.Messages;
import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateTourMarker extends Action {

	private TourDataEditorView	fTourDataEditorView;

	public ActionCreateTourMarker(final TourDataEditorView tourDataEditorView) {

		super(Messages.tourCatalog_view_action_create_marker);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_new));

		this.fTourDataEditorView = tourDataEditorView;
	}

	/**
	 * Creates a new marker
	 * 
	 * @param tourData
	 * @return
	 */
	private TourMarker createTourMarker(final TourData tourData) {

		final StructuredSelection selection = (StructuredSelection) fTourDataEditorView.getSliceViewer().getSelection();
		final Object firstElement = selection.getFirstElement();

		if (firstElement instanceof TimeSlice) {

			// create a new marker
			final int serieIndex = ((TimeSlice) firstElement).serieIndex;
			final int[] distSerie = tourData.getMetricDistanceSerie();

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_CUSTOM);
			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setTime(tourData.timeSerie[serieIndex]);
			tourMarker.setLabel(Messages.TourData_Label_new_marker);
			tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			if (distSerie != null) {
				tourMarker.setDistance(distSerie[serieIndex]);
			}

			return tourMarker;
		}

		return null;
	}

	@Override
	public void run() {

		final TourData tourData = fTourDataEditorView.getTourData();

		final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

		markerDialog.create();
		markerDialog.addTourMarker(createTourMarker(tourData));

		if (markerDialog.open() == Window.OK) {

//			fTourDataEditorView.updateViewer();
//			fTourDataEditorView.setTourDirty();

			final TourEvent propertyData = new TourEvent(tourData);
			TourManager.fireEvent(TourEventId.TOUR_CHANGED, propertyData);

		}

		// markers in tourData could be modified even when the Cancel button is pressed
		fTourDataEditorView.updateMarkerMap();
	}

}
