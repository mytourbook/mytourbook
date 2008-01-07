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
import net.tourbook.tour.TreeViewerItem;

/**
 * TTI (TreeViewerItem) is used in the tree viewer TourMapView, it contains tree items for reference
 * tours
 */
public class TourCatalogItemYear extends TreeViewerItem {

	long	refId;
	int		year;

	/**
	 * @param parentItem
	 * @param refId
	 * @param year
	 */
	public TourCatalogItemYear(TreeViewerItem parentItem, long refId, int year) {

		this.setParentItem(parentItem);

		this.refId = refId;
		this.year = year;
	}

	@Override
	protected void fetchChildren() {

		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		String sqlString = "SELECT " //$NON-NLS-1$
				+ "tourDate, " //$NON-NLS-1$
				+ "tourSpeed, " //$NON-NLS-1$
				+ "comparedId, " //$NON-NLS-1$
				+ "tourId , " //$NON-NLS-1$
				+ "startIndex, " //$NON-NLS-1$
				+ "endIndex, " //$NON-NLS-1$
				+ "startYear \n" //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("WHERE refTourId=" + refId) //$NON-NLS-1$
				+ " AND " //$NON-NLS-1$
				+ ("startYear=" + year) //$NON-NLS-1$
				+ " ORDER BY tourDate"; //$NON-NLS-1$

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				children.add(new TourCatalogItemComparedTour(this,
						result.getDate(1),
						result.getFloat(2),
						result.getLong(3),
						result.getLong(4),
						result.getInt(5),
						result.getInt(6),
						refId));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void remove() {

		// remove all children
		getUnfetchedChildren().clear();

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

	TourCatalogItemReferenceTour getRefItem() {
		return (TourCatalogItemReferenceTour) getParentItem();
	}

}
