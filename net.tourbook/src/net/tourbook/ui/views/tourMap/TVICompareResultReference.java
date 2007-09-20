/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

/**
 * TTI (TreeViewerItem) is used in the tree viewer TourMapView, it contains tree items for reference
 * tours
 */
public class TVICompareResultReference extends TreeViewerItem {

	String						label;

	long						tourId;

	TourReference				refTour;

	/**
	 * keeps the tourId's for all compared tours which have already been stored in the db
	 */
	private HashMap<Long, Long>	storedComparedTours;

	public TVICompareResultReference(TVICompareResultRoot parentItem, String label,
			TourReference refTour, long tourId) {

		this.setParentItem(parentItem);

		this.label = label;
		this.refTour = refTour;
		this.tourId = tourId;
	}

	@Override
	protected void fetchChildren() {

		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		storedComparedTours = new HashMap<Long, Long>();

		TVICompareResult[] comparedTours = TourCompareManager.getInstance().getComparedTours();

		long refId = refTour.getRefId();

		String sqlString = "SELECT tourId, comparedId  \n" //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("WHERE refTourId=" + refId); //$NON-NLS-1$

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				storedComparedTours.put(result.getLong(1), result.getLong(2));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		// create children for one reference tour
		for (TVICompareResult compTour : comparedTours) {

			if (compTour.refTour.getRefId() == refId) {

				// compared tour belongs to the reference tour

				// keep the ref tour as the parent
				compTour.setParentItem(this);

				/*
				 * set the status if the compared tour is already stored in the database and set the
				 * id for the compared tour
				 */
				Long compTourId = compTour.compTour.getTourId();
				boolean isStoredForRefTour = storedComparedTours.containsKey(compTourId);

				if (isStoredForRefTour) {
					compTour.compId = storedComparedTours.get(compTourId);
				} else {
					compTour.compId = -1;
				}

				children.add(compTour);
			}
		}

	}

	@Override
	public void remove() {}

}
