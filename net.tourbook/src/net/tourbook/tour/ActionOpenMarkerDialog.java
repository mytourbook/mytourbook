/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionOpenMarkerDialog extends Action {

	private ITourProvider	fTourProvider;
	private boolean			fIsSaveTour;

	private TourMarker		fSelectedTourMarker;

	/**
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved when the marker dialog is closed
	 */
	public ActionOpenMarkerDialog(final ITourProvider tourProvider, final boolean isSaveTour) {

		fTourProvider = tourProvider;
		fIsSaveTour = isSaveTour;

		setText(Messages.app_action_edit_tour_marker);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_disabled));

		setEnabled(false);
	}

	@Override
	public void run() {

		final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

		// check if one tour is selected
		if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
			return;
		}

		final TourData tourData = selectedTours.get(0);

		final DialogMarker markerDialog = new DialogMarker(
				Display.getCurrent().getActiveShell(),
				tourData,
				fSelectedTourMarker);

		if (markerDialog.open() == Window.OK) {

			if (fIsSaveTour) {
				TourManager.saveModifiedTours(selectedTours);
			} else {

				/*
				 * don't save the tour, just update the tour data editor
				 */
				final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
				if (tourDataEditor != null) {

					tourDataEditor.updateUI(tourData, true);

					TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(tourData));
				}
			}
		}
	}

	public void setSelectedMarker(final TourMarker tourMarker) {
		fSelectedTourMarker = tourMarker;
	}

}
