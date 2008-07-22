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
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ISelectedTours;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionRemoveAllTags extends Action {

	private ISelectedTours	fTourProvider;

	public ActionRemoveAllTags(final ISelectedTours tourProvider) {

		super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {

			public void run() {

				// get tours which tour type should be changed
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				if (TourManager.saveTourEditors(selectedTours)) {

					final HashMap<Long, TourTag> removedTags = new HashMap<Long, TourTag>();

					// remove tag in all tours (without tours from an editor)
					for (final TourData tourData : selectedTours) {

						// get all tag's which will be removed
						final Set<TourTag> tourTags = tourData.getTourTags();

						for (final TourTag tourTag : tourTags) {
							removedTags.put(tourTag.getTagId(), tourTag);
						}

						// remove all tour tags
						tourTags.clear();

						TourDatabase.saveTour(tourData);
					}

					TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);

					TourManager.firePropertyChange(TourManager.TOUR_TAGS_CHANGED, new ChangedTags(removedTags,
							selectedTours,
							false));
				}
			}

		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}
}
