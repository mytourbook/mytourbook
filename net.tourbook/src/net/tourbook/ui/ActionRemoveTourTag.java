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

package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionRemoveTourTag extends Action {

	private ISelectedTours	fTourProvider;

	public ActionRemoveTourTag(final ISelectedTours tourProvider) {

		super(Messages.app_action_remove_tour_tag, AS_PUSH_BUTTON);

		fTourProvider = tourProvider;
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {

			@SuppressWarnings("unchecked")
			public void run() {

				// get tours which tour type should be changed
				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null) {
					return;
				}

				// update tours which are opened in an editor
//				final ArrayList<TourData> toursInEditor = updateEditors(selectedTours);

				// get all tours which are not opened in an editor
				final ArrayList<TourData> saveTours = (ArrayList<TourData>) selectedTours.clone();
//				saveTours.removeAll(toursInEditor);

				// add tour tag in all tours (without tours from an editor)
				for (final TourData tourData : saveTours) {

					// remove all tour tags
					tourData.getTourTags().clear();
					TourDatabase.saveTour(tourData);
				}

				TourManager.getInstance().firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, selectedTours);
			}

		};
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

}
