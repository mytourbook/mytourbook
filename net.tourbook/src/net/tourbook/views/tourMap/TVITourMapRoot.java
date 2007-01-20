/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITourMapRoot extends TreeViewerItem {

	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are reference tours
		 */
		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		String sqlString = "SELECT label, refId, " //$NON-NLS-1$
				+ (TourDatabase.TABLE_TOUR_DATA + "_tourId	\n") //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_REFERENCE + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ " ORDER BY label"; //$NON-NLS-1$

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				children.add(new TVTITourMapReferenceTour(
						this,
						result.getString(1),
						result.getLong(2),
						result.getLong(3)));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void remove() {}
}
