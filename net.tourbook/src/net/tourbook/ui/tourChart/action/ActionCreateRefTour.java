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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperty;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

/**
 * add a new reference tour to all reference tours
 */
public class ActionCreateRefTour extends Action {

	private TourChart	fTourChart;

	public ActionCreateRefTour(final TourChart tourChart) {

		setText(Messages.tourCatalog_view_action_create_reference_tour);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_map_ref_tour_new));

		fTourChart = tourChart;
	}

	@Override
	public void run() {

		if (UI.isTourEditorModified()) {
			return;
		}

		final SelectionChartInfo chartInfo = fTourChart.getChartInfo();
		final TourData tourData = fTourChart.getTourData();

		// get the reference tour name
		final InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
				Messages.tourCatalog_view_dlg_add_reference_tour_title,
				Messages.tourCatalog_view_dlg_add_reference_tour_msg,
				"", //$NON-NLS-1$
				null);

		if (dialog.open() != Window.OK) {
			return;
		}

		// create new tour reference
		final TourReference newTourReference = new TourReference(dialog.getValue(),
				tourData,
				chartInfo.leftSliderValuesIndex,
				chartInfo.rightSliderValuesIndex);

		// add the tour reference into the tour data 
		tourData.getTourReferences().add(newTourReference);

		// save tour
		final ArrayList<TourData> modifiedTours = new ArrayList<TourData>();
		modifiedTours.add(tourData);
		TourManager.saveModifiedTours(modifiedTours);

		// update tour catalog view
		TourManager.firePropertyChange(TourProperty.REFERENCE_TOUR_IS_CREATED);
	}
}
