/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 * TTI (TreeViewerItem) is used in the tree viewer {@link TourCatalogView}, it contains tree items
 * for reference tours
 */
public class TVICatalogYearItem extends TVICatalogItem {

	long	refId;
	int		year;
	int		tourCounter;	// number of tours

	/**
	 * @param parentItem
	 * @param refId
	 * @param year
	 */
	public TVICatalogYearItem(final TreeViewerItem parentItem) {
		this.setParentItem(parentItem);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVICatalogYearItem)) {
			return false;
		}

		final TVICatalogYearItem other = (TVICatalogYearItem) obj;

		if (refId != other.refId) {
			return false;
		}
		if (year != other.year) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT"); //$NON-NLS-1$

		sb.append(" TourCompared.comparedId,"); // 		1 //$NON-NLS-1$
		sb.append(" TourCompared.tourId,"); //			2 //$NON-NLS-1$
		sb.append(" TourCompared.tourDate,"); //		3 //$NON-NLS-1$
		sb.append(" TourCompared.avgPulse,"); //		4 //$NON-NLS-1$
		sb.append(" TourCompared.tourSpeed,"); //		5 //$NON-NLS-1$
		sb.append(" TourCompared.startIndex,"); //		6 //$NON-NLS-1$
		sb.append(" TourCompared.endIndex,"); //		7 //$NON-NLS-1$

		sb.append(" TourData.tourTitle,"); //			8		 //$NON-NLS-1$
		sb.append(" TourData.tourType_typeId,"); //		9 //$NON-NLS-1$

		sb.append(" jTdataTtag.TourTag_tagId"); //		10 //$NON-NLS-1$

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_COMPARED + " TourCompared"); //$NON-NLS-1$ //$NON-NLS-2$

		// get data for a tour
		sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData ON "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" TourCompared.tourId = TourData.tourId"); //$NON-NLS-1$

		// get tag id's
		sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(" ON TourData.tourId = jTdataTtag.TourData_tourId"); //$NON-NLS-1$

		sb.append(" WHERE TourCompared.refTourId=? AND TourCompared.startYear=?"); //$NON-NLS-1$
		sb.append(" ORDER BY TourCompared.tourDate"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, refId);
			statement.setInt(2, year);

			final ResultSet result = statement.executeQuery();

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			while (result.next()) {

				final long tourId = result.getLong(2);
				final Object resultTagId = result.getObject(10);

				if (tourId == lastTourId) {

					// get tags from outer join

					if (resultTagId instanceof Long) {
						tagIds.add((Long) resultTagId);
					}

				} else {

					// new tour is in the resultset

					final TVICatalogComparedTour tourItem = new TVICatalogComparedTour(this);
					children.add(tourItem);

					tourItem.refId = refId;

					tourItem.compareId = result.getLong(1);
					tourItem.setTourId(tourId);

					final Date tourDate = result.getDate(3);

					tourItem.avgPulse = result.getFloat(4);
					tourItem.tourSpeed = result.getFloat(5);

					tourItem.startIndex = result.getInt(6);
					tourItem.endIndex = result.getInt(7);

					tourItem.tourTitle = result.getString(8);
					final Object tourTypeId = result.getObject(9);

					// tour date
					if (tourDate != null) {
						tourItem.tourDate = tourDate.toLocalDate();
					}

					// tour type
					tourItem.tourTypeId = (tourTypeId == null ? //
							TourDatabase.ENTITY_IS_NOT_SAVED
							: (Long) tourTypeId);

					// tour tags
					if (resultTagId instanceof Long) {

						if (tourItem.tagIds != null) {
							tourItem.tagIds.clear();
						}

						tourItem.tagIds = tagIds = new ArrayList<Long>();
						tagIds.add((Long) resultTagId);
					}
				}

				lastTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	TVICatalogRefTourItem getRefItem() {
		return (TVICatalogRefTourItem) getParentItem();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (refId ^ (refId >>> 32));
		result = prime * result + year;
		return result;
	}

	void remove() {

		// remove all children
		getUnfetchedChildren().clear();

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

}
