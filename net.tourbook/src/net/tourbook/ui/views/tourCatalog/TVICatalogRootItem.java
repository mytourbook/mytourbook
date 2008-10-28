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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVICatalogRootItem extends TVICatalogItem {

	private ITourViewer	fTourViewer;

	public TVICatalogRootItem(final ITourViewer tourViewer) {
		super();
		fTourViewer = tourViewer;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are reference tours
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT"); //$NON-NLS-1$

		sb.append(" label,"); //$NON-NLS-1$
		sb.append(" refId,"); //$NON-NLS-1$
		sb.append(" TourData_tourId"); //$NON-NLS-1$
//		sb.append(" SUM(1)");

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_REFERENCE); //$NON-NLS-1$
		sb.append(" ORDER BY label"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final TVICatalogReferenceTour refItem = new TVICatalogReferenceTour(this);
				children.add(refItem);

				refItem.label = result.getString(1);
				refItem.refId = result.getLong(2);
				refItem.setTourId(result.getLong(3));
//				refItem.tourCounter = result.getInt(1);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public ITourViewer getRootTourViewer() {
		return fTourViewer;
	}

	@Override
	protected void remove() {}
}
