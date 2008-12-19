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
package net.tourbook.ui.action;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.rawData.DialogMergeTours;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionMergeTour extends Action {

	private ITourProvider	fTourProvider;

	private TourData		fMergeIntoTour;

	private TourData		fMergeFromTour;

	public ActionMergeTour(final ITourProvider tourProvider) {

		setText(Messages.app_action_merge_tour);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image__merge_tours));

		fTourProvider = tourProvider;
	}

	/**
	 * @return {@link TourData} for the selected tour or <code>null</code> when the selected tour is
	 *         not valid
	 */
	private boolean getSelectedTour() {

		if (UI.isTourEditorModified()) {
			return false;
		}

		// get selected tour, make sure only one tour is selected
		final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
		if (selectedTours == null || selectedTours.size() != 1) {
			return false;
		}

		final TourData intoTourData = selectedTours.get(0);

		final Long mergeFromTourId = intoTourData.getMergeFromTourId();
		if (mergeFromTourId == null) {

			// check if the tour can be merged

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_tour_message);

			return false;

		} else if ((fMergeFromTour = TourManager.getInstance().getTourData(mergeFromTourId)) == null) {

			// check if merge from tour is available

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					NLS.bind(Messages.merge_tour_dlg_invalid_tour_data_message,
							mergeFromTourId,
							TourManager.getTourTitle(intoTourData)));

			// remove invalid merge tour id
			intoTourData.setMergeFromTourId(null);
			TourManager.saveModifiedTour(intoTourData);

			return false;

		} else if (intoTourData.altitudeSerie == null
				|| intoTourData.altitudeSerie.length == 0
				|| intoTourData.timeSerie == null
				|| intoTourData.timeSerie.length == 0
				|| fMergeFromTour.altitudeSerie == null
				|| fMergeFromTour.altitudeSerie.length == 0
				|| fMergeFromTour.timeSerie == null
				|| fMergeFromTour.timeSerie.length == 0) {

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_serie_data_message);

			return false;
		}

		fMergeIntoTour = intoTourData;

		return true;
	}

	@Override
	public void run() {

		if (getSelectedTour()) {
			new DialogMergeTours(Display.getCurrent().getActiveShell(), fMergeFromTour, fMergeIntoTour).open();
		}
	}

}
