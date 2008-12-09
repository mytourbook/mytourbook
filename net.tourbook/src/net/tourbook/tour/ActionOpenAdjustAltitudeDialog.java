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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionOpenAdjustAltitudeDialog extends Action {

	private ITourProvider	fTourProvider;
	private boolean			fIsSaveTour;

	public ActionOpenAdjustAltitudeDialog(final ITourProvider tourProvider, final boolean isSaveTour) {

		fTourProvider = tourProvider;
		fIsSaveTour = isSaveTour;

		setText(Messages.app_action_edit_adjust_altitude);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_adjust_altitude_disabled));

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

		final DialogAdjustAltitude dialog = new DialogAdjustAltitude(Display.getCurrent().getActiveShell(), tourData);
		if (dialog.open() == Window.OK) {

			if (fIsSaveTour) {
				TourManager.saveModifiedTours(selectedTours);
			} else {

				/*
				 * don't save the tour, just update the tour data editor
				 */
				final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
				if (tourDataEditor != null) {

					tourDataEditor.updateUI(tourData, true);

					final ArrayList<TourData> modifiedTours = new ArrayList<TourData>();
					modifiedTours.add(tourData);
					final TourEvent propertyData = new TourEvent(modifiedTours);

					TourManager.fireEvent(TourEventId.TOUR_CHANGED, propertyData);
				}
			}

		} else {

			dialog.restoreOriginalAltitudeValues();
		}
	}
}
