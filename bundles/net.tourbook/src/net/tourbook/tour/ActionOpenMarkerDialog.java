/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionOpenMarkerDialog extends Action {

	private ITourProvider	_tourProvider;
	private TourMarker		_tourMarker;
	private boolean			_isSaveTour;

	/**
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved when the marker dialog is closed
	 */
	public ActionOpenMarkerDialog(final ITourProvider tourProvider, final boolean isSaveTour) {

		_tourProvider = tourProvider;
		_isSaveTour = isSaveTour;

		setText(Messages.app_action_edit_tour_marker);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_disabled));

		setEnabled(false);
	}

	public static void doAction(final ITourProvider tourProvider,
								final boolean isSaveTour,
								final TourMarker selectedTourMarker) {

		final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

		// check if one tour is selected
		if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
			return;
		}

		final TourData tourData = selectedTours.get(0);

		if (tourData.isManualTour()) {
			// a manually created tour do not have time slices -> no markers
			return;
		}

		final DialogMarker markerDialog = new DialogMarker(
				Display.getCurrent().getActiveShell(),
				tourData,
				selectedTourMarker);

		if (markerDialog.open() == Window.OK) {

			if (isSaveTour) {

				TourManager.saveModifiedTours(selectedTours);

			} else {

				/*
				 * don't save the tour, just update the tour data editor
				 */
				final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
				if (tourDataEditor != null) {
					tourDataEditor.updateUI(tourData, true);
					fireTourChangeEvent(tourData);
				}

//				fireTourChangeEvent(tourData);
			}

		} else {

//			fireTourChangeEvent(tourData);
		}
	}

	/**
	 * This event must be event when the dialog is canceled because the original tour marker are
	 * replaced with the backedup markers.
	 * <p>
	 * Views which contain the original {@link TourMarker}'s need to know that the list has changed
	 * otherwise marker actions do fail, e.g. Set Marker Hidden in tour chart view.
	 * 
	 * @param tourData
	 */
	private static void fireTourChangeEvent(final TourData tourData) {

		TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(tourData));
	}

	@Override
	public void run() {
		doAction(_tourProvider, _isSaveTour, _tourMarker);
	}

	public void setTourMarker(final TourMarker tourMarker) {
		_tourMarker = tourMarker;
	}

}
