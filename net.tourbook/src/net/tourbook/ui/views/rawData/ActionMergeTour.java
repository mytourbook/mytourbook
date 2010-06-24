/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.rawData;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class ActionMergeTour extends Action {

	private ITourProvider	_tourProvider;

	private TourData		_mergeTargetTour;
	private TourData		_mergeSourceTour;

	public ActionMergeTour(final ITourProvider tourProvider) {

		setText(Messages.app_action_merge_tour);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image__merge_tours));

		_tourProvider = tourProvider;
	}

	/**
	 * @return {@link TourData} for the selected tour or <code>null</code> when the selected tour is
	 *         not valid
	 */
	private boolean getSelectedTour() {

		// get selected tour, make sure only one tour is selected
		final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
		if (selectedTours == null || selectedTours.size() != 1) {
			return false;
		}

		final TourData targetTour = selectedTours.get(0);

		final Long mergeFromTourId = targetTour.getMergeSourceTourId();
		if (mergeFromTourId == null) {

			// check if the tour can be merged

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_tour_message);

			return false;

		} else if ((_mergeSourceTour = TourManager.getInstance().getTourData(mergeFromTourId)) == null) {

			// check if merge from tour is available

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					NLS.bind(
							Messages.merge_tour_dlg_invalid_tour_data_message,
							mergeFromTourId,
							TourManager.getTourTitle(targetTour)));

			// remove invalid merge tour id
			targetTour.setMergeSourceTourId(null);
			TourManager.saveModifiedTour(targetTour);

			return false;

		} else if (targetTour.timeSerie == null
				|| targetTour.timeSerie.length == 0
				|| _mergeSourceTour.timeSerie == null
				|| _mergeSourceTour.timeSerie.length == 0) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.merge_tour_dlg_invalid_tour_title,
					Messages.merge_tour_dlg_invalid_serie_data_message);

			return false;
		}

		_mergeTargetTour = targetTour;

		return true;
	}

	@Override
	public void run() {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		if (getSelectedTour()) {
			new DialogMergeTours(Display.getCurrent().getActiveShell(), _mergeSourceTour, _mergeTargetTour).open();
		}
	}

}
