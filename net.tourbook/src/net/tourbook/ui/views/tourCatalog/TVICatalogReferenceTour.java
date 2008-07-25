/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TVICatalogTourItem;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

/**
 * TTI (TreeViewerItem) is used in the tree viewer {@link TourCatalogView}, it contains tree items
 * for reference tours
 */
public class TVICatalogReferenceTour extends TVICatalogTourItem {

	String	label;
	long	refId;

	int		yearMapMinValue	= Integer.MIN_VALUE;
	int		yearMapMaxValue;
	int		tourCounter;

	public TVICatalogReferenceTour(final TVICatalogRootItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVICatalogReferenceTour)) {
			return false;
		}
		final TVICatalogReferenceTour other = (TVICatalogReferenceTour) obj;
		if (refId != other.refId) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		/**
		 * derby does not support expression in "GROUP BY" statements, this is a workaround found
		 * here: http://mail-archives.apache.org/mod_mbox/db-derby-dev/200605.mbox/%3C7415300
		 * .1147889647479.JavaMail.jira@brutus%3E <code>
		 * 	String subSQLString = "(SELECT YEAR(tourDate)\n"
		 * 		+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + "\n")
		 * 		+ (" WHERE "
		 * 				+ TourDatabase.TABLE_TOUR_REFERENCE
		 * 				+ "_generatedId="
		 * 				+ refId + "\n")
		 * 		+ ")";
		 * 
		 * 	String sqlString = "SELECT years FROM \n"
		 * 		+ subSQLString
		 * 		+ (" REFYEARS(years) GROUP BY years");
		 *</code>
		 */

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT"); //$NON-NLS-1$
		sb.append(" startYear,"); //$NON-NLS-1$
		sb.append(" SUM(1)"); //$NON-NLS-1$

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_COMPARED); //$NON-NLS-1$
		sb.append(" WHERE refTourId=?"); //$NON-NLS-1$
		sb.append(" GROUP BY startYear"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, refId);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVICatalogYearItem yearItem = new TVICatalogYearItem(this);
				children.add(yearItem);

				yearItem.refId = refId;
				yearItem.year = result.getInt(1);
				yearItem.tourCounter = result.getInt(2);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (refId ^ (refId >>> 32));
		return result;
	}

	@Override
	public void remove() {

		final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();

		// remove all children
		if (unfetchedChildren != null) {
			unfetchedChildren.clear();
		}

		// remove this ref item from the parent item
		final ArrayList<TreeViewerItem> unfetchedParentChildren = getParentItem().getUnfetchedChildren();
		if (unfetchedParentChildren != null) {
			unfetchedParentChildren.remove(this);
		}
	}

}
