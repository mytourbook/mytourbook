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

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

class ActionSetTourType extends Action {

	private TourType		fTourType;
	private ITourProvider	fTourProvider;

	private boolean			fIsSaveTour;

	/**
	 * @param tourType
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the {@link TourData}
	 *            from the tour provider is only updated
	 */
	public ActionSetTourType(final TourType tourType, final ITourProvider tourProvider, final boolean isSaveTour) {

		super(tourType.getName(), AS_CHECK_BOX);

		final Image tourTypeImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
		setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));

		fTourType = tourType;
		fTourProvider = tourProvider;
		fIsSaveTour = isSaveTour;
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {

			public void run() {

				final ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();
				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				// add the tag in all tours (without tours which are opened in an editor)
				for (final TourData tourData : selectedTours) {

					// set tour type
					tourData.setTourType(fTourType);
				}

				if (fIsSaveTour) {

					// save all tours with the removed tags
					TourManager.saveModifiedTours(selectedTours);

				} else {

					// tours are not saved but the tour provider must be notified

					if (fTourProvider instanceof ITourProvider2) {
						((ITourProvider2) fTourProvider).toursAreModified(selectedTours);
					} else {
						TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(selectedTours));
					}
				}
			}
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

}
