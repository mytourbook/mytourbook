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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

public class TVIWizardCompareMonth extends TVIWizardCompareItem {

	int	tourYear;
	int	tourMonth;

	TVIWizardCompareMonth(final TVIWizardCompareItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the month item, these are tour items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT"); //$NON-NLS-1$

		sb.append(" tourId,"); //				1 //$NON-NLS-1$
		sb.append(" startYear,"); //			2 //$NON-NLS-1$
		sb.append(" startMonth,"); //			3 //$NON-NLS-1$
		sb.append(" startDay,"); //				4 //$NON-NLS-1$
		sb.append(" tourType_typeId,"); //		5	 //$NON-NLS-1$
		sb.append(" tourDistance,"); //			6 //$NON-NLS-1$
		sb.append(" tourRecordingTime,"); //	7		 //$NON-NLS-1$
		sb.append(" tourAltUp"); //				8 //$NON-NLS-1$

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$
		sb.append(" WHERE startYear=? AND startMonth=?"); //$NON-NLS-1$
		sb.append(" ORDER BY startDay, startHour, startMinute"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setInt(1, tourYear);
			statement.setInt(2, tourMonth);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				// new tour is in the resultset
				final TVIWizardCompareTour tourItem = new TVIWizardCompareTour(this);
				children.add(tourItem);

				tourItem.tourId = result.getLong(1);

				final int dbYear = result.getInt(2);
				final int dbMonth = result.getInt(3);
				final int dbDay = result.getInt(4);
				tourItem.treeColumn = Integer.toString(dbDay);

				tourItem.tourYear = dbYear;
				tourItem.tourMonth = dbMonth;
				tourItem.tourDay = dbDay;

//				fCalendar.set(dbYear, dbMonth - 1, dbDay);
//				tourItem.tourDate = fCalendar.getTimeInMillis();

				final Object tourTypeId = result.getObject(5);
				tourItem.tourTypeId = (tourTypeId == null ? //
						TourDatabase.ENTITY_IS_NOT_SAVED
						: (Long) tourTypeId);

				tourItem.colDistance = result.getLong(6);
				tourItem.colRecordingTime = result.getLong(7);
				tourItem.colAltitudeUp = result.getLong(8);

			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}

}
