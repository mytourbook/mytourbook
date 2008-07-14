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

	public TVICatalogReferenceTour(	final TVICatalogRootItem parentItem,
									final String label,
									final long refId,
									final long tourId) {

		this.setParentItem(parentItem);

		this.label = label;
		this.refId = refId;

		setTourId(tourId);
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

		final String sqlString = "SELECT startYear\n" //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE refTourId=" + refId + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" GROUP BY startYear"); //$NON-NLS-1$

		// System.out.println(sqlString);
		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				children.add(new TVICatalogYearItem(this, refId, result.getInt(1)));
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

		if (getUnfetchedChildren() != null) {
			// remove all children
			getUnfetchedChildren().clear();
		}

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

}
