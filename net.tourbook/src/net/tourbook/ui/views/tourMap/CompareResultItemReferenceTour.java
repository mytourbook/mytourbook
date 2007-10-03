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
 * contains tree viewer items (TVI) for reference tours
 */
public class CompareResultItemReferenceTour extends TreeViewerItem {

	String										label;

	long										tourId;

	TourReference								refTour;

	/**
	 * keeps the tourId's for all compared tours which have already been stored in the db
	 */
	private HashMap<Long, StoredComparedTour>	fStoredComparedTours;

	private class StoredComparedTour {

		long			comparedId;

		private int		startIndex;
		private int		endIndex;

		private float	tourSpeed;

		public StoredComparedTour(long compareId, int startIndex, int endIndex, float speed) {
			this.comparedId = compareId;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.tourSpeed = speed;
		}
	}

	public CompareResultItemReferenceTour(CompareResultItemRoot parentItem, String label,
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

		fStoredComparedTours = new HashMap<Long, StoredComparedTour>();

		CompareResultItemComparedTour[] comparedTours = TourCompareManager.getInstance().getComparedTours();

		long refId = refTour.getRefId();

		String sqlString = "SELECT tourId, comparedId, startIndex, endIndex, tourSpeed  \n" //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("WHERE refTourId=" + refId); //$NON-NLS-1$

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				fStoredComparedTours.put(result.getLong(1),
						new StoredComparedTour(result.getLong(2),
								result.getInt(3),
								result.getInt(4),
								result.getFloat(5)));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		// create children for one reference tour
		for (CompareResultItemComparedTour compTour : comparedTours) {

			if (compTour.refTour.getRefId() == refId) {

				// compared tour belongs to the reference tour

				// keep the ref tour as the parent
				compTour.setParentItem(this);

				/*
				 * set the status if the compared tour is already stored in the database and set the
				 * id for the compared tour
				 */
				Long comparedTourId = compTour.comparedTourData.getTourId();
				boolean isStoredForRefTour = fStoredComparedTours.containsKey(comparedTourId);

				if (isStoredForRefTour) {
					final StoredComparedTour storedComparedTour = fStoredComparedTours.get(comparedTourId);
					compTour.compId = storedComparedTour.comparedId;
					compTour.dbStartIndex = storedComparedTour.startIndex;
					compTour.dbEndIndex = storedComparedTour.endIndex;
					compTour.dbSpeed = storedComparedTour.tourSpeed;
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
