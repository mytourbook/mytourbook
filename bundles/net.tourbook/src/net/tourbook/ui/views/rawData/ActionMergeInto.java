/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Merge a tour into another tour
 */
public class ActionMergeInto extends Action {

	private TourData	_fromTourData;
	private TourData	_intoTourData;

	private RawDataView	_rawDataView;

	public ActionMergeInto(final TourData mergeFromTour, final TourData mergeIntoTour, final RawDataView rawDataView) {

		super(UI.EMPTY_STRING, AS_CHECK_BOX);

		_fromTourData = mergeFromTour;
		_intoTourData = mergeIntoTour;
		_rawDataView = rawDataView;

		/*
		 * set menu text
		 */
		final long start = mergeIntoTour.getTourStartTimeMS();

		final StringBuilder sb = new StringBuilder().append(UI.EMPTY_STRING)//
				.append(UI.getFormatterDateShort().format(start))
				.append(UI.DASH_WITH_DOUBLE_SPACE)
				.append(UI.getFormatterTimeShort().format(start))
				.append(UI.DASH_WITH_DOUBLE_SPACE)
				.append(mergeIntoTour.getDeviceName());

		setText(sb.toString());

		// show database icon
		setImageDescriptor(ImageDescriptor.createFromImage(_rawDataView.getStateImage_Db(_intoTourData)));

		// check menu item when the from tour is merge into the into tour
		final Long mergeIntoTourId = mergeFromTour.getMergeTargetTourId();
		if (mergeIntoTourId != null && mergeIntoTourId.equals(mergeIntoTour.getTourId())) {
			setChecked(true);
		}
	}

	@Override
	public void run() {
		_rawDataView.actionMergeTours(_fromTourData, _intoTourData);
	}

}
