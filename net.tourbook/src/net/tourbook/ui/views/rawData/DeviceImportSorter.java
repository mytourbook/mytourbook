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
/*
 * Author:	Wolfgang Schramm
 * Created: 07.06.2005
 *
 * 
 */

package net.tourbook.ui.views.rawData;

import net.tourbook.data.TourData;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class DeviceImportSorter extends ViewerSorter {

	private static final int	ASCENDING	= 0;
	private static final int	DESCENDING	= 1;

	private int					column;
	private int					direction;

	@Override
	public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

		final TourData tourData1 = ((TourData) obj1);
		final TourData tourData2 = ((TourData) obj2);

		int result = 0;

		// Determine which column and do the appropriate sort
		switch (column) {
		case RawDataView.COLUMN_DATE:

			result = compareDateTime(tourData1, tourData2);
			break;

		case RawDataView.COLUMN_TITLE:

			// sort by title
			result = tourData1.getTourTitle().compareTo(tourData2.getTourTitle());
			break;

		case RawDataView.COLUMN_FILE_NAME:

			// sort by file name
			if (tourData1.importRawDataFile == null||tourData2.importRawDataFile==null) {
				break;
			}
			
			result = tourData1.importRawDataFile.compareTo(tourData2.importRawDataFile);

			if (result == 0) {
				result = compareDateTime(tourData1, tourData2);
			}
			break;

		case RawDataView.COLUMN_DATA_FORMAT:

			// sort by import data format
			result = tourData1.getDeviceName().compareTo(tourData2.getDeviceName());

			if (result == 0) {
				result = compareDateTime(tourData1, tourData2);
			}
			break;
		}

		// If descending order, flip the direction
		if (direction == DESCENDING) {
			result = -result;
		}

		return result;
	}

	private int compareDateTime(final TourData tourData1, final TourData tourData2) {

		int result;

		// sort on date
		result = (tourData1.getStartYear() * 10000 + tourData1.getStartMonth() * 100 + tourData1.getStartDay())
				- (tourData2.getStartYear() * 10000 + tourData2.getStartMonth() * 100 + tourData2.getStartDay());

		// sort on time if date is the same
		if (result == 0) {
			result = (tourData1.getStartHour() * 100 + tourData1.getStartMinute())
					- (tourData2.getStartHour() * 100 + tourData2.getStartMinute());
		}
		return result;
	}

	/**
	 * Does the sort. If it's a different column from the previous sort, do an ascending sort. If
	 * it's the same column as the last sort, toggle the sort direction.
	 * 
	 * @param column
	 */
	public void doSort(final int column) {

		if (column == this.column) {
			// Same column as last sort; toggle the direction
			direction = 1 - direction;
		} else {
			// New column; do an ascending sort
			this.column = column;
			direction = ASCENDING;
		}
	}
}
