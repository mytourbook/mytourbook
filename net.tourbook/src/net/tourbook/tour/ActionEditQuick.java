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
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class ActionEditQuick extends Action {

	private ISelectedTours	fTourProvider;
	private TourData		fTourData;

	public ActionEditQuick(ISelectedTours tourProvider) {

		setText(Messages.app_action_quick_edit);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_disabled));

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		// get tours which tour type should be changed
		ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

		if (selectedTours == null || selectedTours.size() == 0) {
			return;
		}

		fTourData = selectedTours.get(0);

		final QuickEditDialog dialog = new QuickEditDialog(Display.getCurrent().getActiveShell(), fTourData);
		if (dialog.open() == Window.OK) {

			ArrayList<TourData> toursInEditor = updateEditors(selectedTours);

			// remove all tours where the tour is opened in an editor
			ArrayList<TourData> savedTours = (ArrayList<TourData>) selectedTours.clone();
			savedTours.removeAll(toursInEditor);

			// update all tours (without tours from an editor) with the new tour type
			for (TourData tourData : savedTours) {

				// save the tour
				TourDatabase.saveTour(tourData);
			}

			TourManager.getInstance().firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);
		}
	}

	/**
	 * Update the tour type in tours which are opened in a tour editor
	 * 
	 * @param selectedTours
	 *        contains the tours where the tour type should be changed
	 * @return
	 * @return Returns the tours which are opened in a tour editor
	 */
	private ArrayList<TourData> updateEditors(ArrayList<TourData> selectedTours) {

		ArrayList<IEditorPart> editorParts = UI.getOpenedEditors();

		// list for tours which are updated in the editor
		ArrayList<TourData> updatedTours = new ArrayList<TourData>();

		// check if a tour is in an editor
		for (IEditorPart editorPart : editorParts) {
			if (editorPart instanceof TourEditor) {

				IEditorInput editorInput = editorPart.getEditorInput();

				if (editorInput instanceof TourEditorInput) {

					TourEditor tourEditor = (TourEditor) editorPart;
					long editorTourId = ((TourEditorInput) editorInput).getTourId();

					for (TourData tourData : selectedTours) {
						if (editorTourId == tourData.getTourId()) {

							/*
							 * a tour editor was found containing the current tour
							 */

							TourData editorTourData = tourEditor.getTourChart().getTourData();

							editorTourData.setTourTitle(fTourData.getTourTitle());
							editorTourData.setTourDescription(fTourData.getTourDescription());
							editorTourData.setTourStartPlace(fTourData.getTourStartPlace());
							editorTourData.setTourEndPlace(fTourData.getTourEndPlace());

							tourEditor.setTourPropertyIsModified();

							// keep updated tours
							updatedTours.add(tourData);
						}
					}
				}
			}
		}

		return updatedTours;
	}
}
