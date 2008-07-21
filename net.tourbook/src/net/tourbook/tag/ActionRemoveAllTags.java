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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEditorInput;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

public class ActionRemoveAllTags extends Action {

	private ISelectedTours	fTourProvider;

	public ActionRemoveAllTags(final ISelectedTours tourProvider) {

		super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {

				// get tours which tour type should be changed
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				final HashMap<Long, TourTag> removedTags = new HashMap<Long, TourTag>();

				// update tours which are opened in an editor
				final ArrayList<TourData> toursInEditor = updateEditors(selectedTours, removedTags);

				// get all tours which are not opened in an editor
				final ArrayList<TourData> saveTours = (ArrayList<TourData>) selectedTours.clone();
				saveTours.removeAll(toursInEditor);

				// remove tag in all tours (without tours from an editor)
				for (final TourData tourData : saveTours) {

					updateTourData(tourData, removedTags);

					TourDatabase.saveTour(tourData);
				}

				TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);

				TourManager.firePropertyChange(TourManager.TOUR_TAGS_CHANGED, new ChangedTags(removedTags,
						selectedTours,
						false));
			}

		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Update the tour type in tours which are opened in a tour editor
	 * 
	 * @param selectedTours
	 *            contains the tours where the tour type should be changed
	 * @param removedTags
	 * @return Returns the tours which are opened in a tour editor
	 */
	private ArrayList<TourData> updateEditors(	final ArrayList<TourData> selectedTours,
												final HashMap<Long, TourTag> removedTags) {

		final ArrayList<IEditorPart> editorParts = UI.getOpenedEditors();

		// list for tours which are updated in the editor
		final ArrayList<TourData> updatedTours = new ArrayList<TourData>();

		// check if a tour is in an editor
		for (final IEditorPart editorPart : editorParts) {
			if (editorPart instanceof TourEditor) {

				final IEditorInput editorInput = editorPart.getEditorInput();

				if (editorInput instanceof TourEditorInput) {

					final TourEditor tourEditor = (TourEditor) editorPart;
					final long editorTourId = ((TourEditorInput) editorInput).getTourId();

					for (final TourData tourData : selectedTours) {
						if (editorTourId == tourData.getTourId()) {

							/*
							 * a tour editor was found containing the current tour
							 */

							final TourData editorTourData = tourEditor.getTourChart().getTourData();

							updateTourData(editorTourData, removedTags);

							tourEditor.setTourPropertyIsModified();

							// keep updated tours
							updatedTours.add(tourData);
						}
					}
				}
			}
		}

		// show info that at least one tour is opened in a tour editor
		if (fTourProvider.isFromTourEditor() == false && updatedTours.size() > 0) {

			/*
			 * don't show the message when the tour is from a tour editor
			 */

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.action_tag_remove_all_dlg_title,
					Messages.action_tag_remove_all_dlg_message);
		}

		return updatedTours;
	}

	private void updateTourData(final TourData tourData, final HashMap<Long, TourTag> removedTags) {
		
		// get all tag's which will be removed
		final Set<TourTag> tourTags = tourData.getTourTags();
		
		for (final TourTag tourTag : tourTags) {
			removedTags.put(tourTag.getTagId(), tourTag);
		}

		// remove all tour tags
		tourTags.clear();
	}
}
