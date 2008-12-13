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

package net.tourbook.ui.views.rawData;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.data.TourData;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Merge a tour into another tour
 */
public class ActionMergeInto extends Action {

	private TourData	fFromTourData;
	private TourData	fIntoTourData;
	private RawDataView	fRawDataView;

	public ActionMergeInto(final TourData mergeFromTour, final TourData mergeIntoTour, final RawDataView rawDataView) {

		fFromTourData = mergeFromTour;
		fIntoTourData = mergeIntoTour;
		fRawDataView = rawDataView;

		/*
		 * set menu text
		 */
		final Calendar calendar = GregorianCalendar.getInstance();
		calendar.set(mergeIntoTour.getStartYear(),
				mergeIntoTour.getStartMonth() - 1,
				mergeIntoTour.getStartDay(),
				mergeIntoTour.getStartHour(),
				mergeIntoTour.getStartMinute());

		final StringBuilder sb = new StringBuilder().append(UI.EMPTY_STRING)//
				.append(UI.getFormatterDateShort().format(calendar.getTime()))
				.append(UI.DASH_WITH_DOUBLE_SPACE)
				.append(UI.getFormatterTimeShort().format(calendar.getTime()))
				.append(UI.DASH_WITH_DOUBLE_SPACE)
				.append(mergeIntoTour.getDeviceName());

		setText(sb.toString());

		// show database icon
		setImageDescriptor(ImageDescriptor.createFromImage(fRawDataView.getDbImage(fIntoTourData)));
	}

	@Override
	public void run() {
		fRawDataView.actionMergedIntoTour(fFromTourData, fIntoTourData);
	}

}
