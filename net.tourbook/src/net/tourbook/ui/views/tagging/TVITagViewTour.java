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
package net.tourbook.ui.views.tagging;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.joda.time.DateTime;

public class TVITagViewTour extends TVITagViewItem {

	public static final String	SQL_TOUR_COLUMNS	= UI.EMPTY_STRING //
															//
															+ "startYear," //			0 //$NON-NLS-1$
															+ "startMonth," //			1 //$NON-NLS-1$
															+ "startDay," //			2 //$NON-NLS-1$
															//
															+ "tourTitle," //			3 //$NON-NLS-1$
															+ "tourType_typeId," //		4 //$NON-NLS-1$
															+ "deviceTimeInterval," //	5 //$NON-NLS-1$
															+ "startDistance," //		6 //$NON-NLS-1$
															//
															+ SQL_SUM_COLUMNS_TOUR; //	7

	long						tourId;

	DateTime					tourDate;

	int							tourYear;
	int							tourMonth;
	int							tourDay;

	String						tourTitle;
	long						tourTypeId;

	ArrayList<Long>				tagIds;

	public long					deviceStartDistance;
	public short				deviceTimeInterval;

	public TVITagViewTour(final TVITagViewItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj == this) {
			return true;
		}

//		if (obj instanceof TVITagViewTour) {
//			final TVITagViewTour tourItem = (TVITagViewTour) obj;
//			return tourId == tourItem.tourId && fParentItem == tourItem.fParentItem;
//		}

		return false;
	}

	@Override
	protected void fetchChildren() {}

	public void getTourColumnData(final ResultSet result, final Object resultTagId, final int startIndex)
			throws SQLException {

		tourYear = result.getInt(startIndex + 0);
		tourMonth = result.getInt(startIndex + 1);
		tourDay = result.getInt(startIndex + 2);

		tourDate = new DateTime(tourYear, tourMonth, tourDay, 0, 0, 0, 0);

		tourTitle = result.getString(startIndex + 3);

		final Object resultTourTypeId = result.getObject(startIndex + 4);
		tourTypeId = (resultTourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) resultTourTypeId);

		deviceTimeInterval = result.getShort(startIndex + 5);
		deviceStartDistance = result.getLong(startIndex + 6);

		readDefaultColumnData(result, startIndex + 7);

		if (resultTagId instanceof Long) {
			tagIds = new ArrayList<Long>();
			tagIds.add((Long) resultTagId);
		}
	}

	public long getTourId() {
		return tourId;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	protected void remove() {}

}
