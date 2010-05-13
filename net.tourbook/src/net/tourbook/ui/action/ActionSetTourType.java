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
package net.tourbook.ui.action;

import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

class ActionSetTourType extends Action {

	private TourType		_tourType;
	private ITourProvider	_tourProvider;

	private boolean			_isSaveTour;

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

		_tourType = tourType;
		_tourProvider = tourProvider;
		_isSaveTour = isSaveTour;
	}

	@Override
	public void run() {
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				TourTypeMenuManager.setTourTypeIntoTour(_tourType, _tourProvider, _isSaveTour);
			}
		});
	}
}
