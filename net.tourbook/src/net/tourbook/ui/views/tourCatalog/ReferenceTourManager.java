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
/**
 * 
 */
package net.tourbook.ui.views.tourCatalog;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.TourChartConfiguration;

/**
 *
 */
public class ReferenceTourManager {

	private static ReferenceTourManager				_instance			= null;

	private final HashMap<Long, TourCompareConfig>	_compareConfigCache	= new HashMap<Long, TourCompareConfig>();

	private ReferenceTourManager() {}

	public static ReferenceTourManager getInstance() {
		if (_instance == null) {
			_instance = new ReferenceTourManager();
		}
		return _instance;
	}

	/**
	 * @return Returns an array with all reference tours
	 */
	public Object[] getAllReferenceTours() {

		List<?> referenceTours = null;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			referenceTours = em.createQuery("SELECT refTour FROM TourReference AS refTour").getResultList(); //$NON-NLS-1$

			em.close();
		}

		return referenceTours.toArray();
	}

	/**
	 * Returns a {@link TourCompareConfig} or <code>null</code> when the reference tour cannot be
	 * loaded from the database
	 * 
	 * @param refId
	 *            Reference Id
	 * @return
	 */
	public TourCompareConfig getTourCompareConfig(final long refId) {

		TourCompareConfig compareConfig = _compareConfigCache.get(refId);

		if (compareConfig != null) {
			return compareConfig;
		}

		// load the reference tour from the database
		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final TourReference refTour = em.find(TourReference.class, refId);
		em.close();

		if (refTour == null) {
			return null;
		} else {

			/*
			 * create a new reference tour configuration
			 */

			final TourData refTourData = refTour.getTourData();

			final TourChartConfiguration refTourChartConfig = TourManager.createTourChartConfiguration();
			final TourChartConfiguration compTourchartConfig = TourManager.createTourChartConfiguration();

			final ChartDataModel chartDataModel = TourManager.getInstance().createChartDataModel(
					refTourData,
					refTourChartConfig);

			compareConfig = new TourCompareConfig(
					refTour,
					chartDataModel,
					refTourData.getTourId(),
					refTourChartConfig,
					compTourchartConfig);

			// keep ref config in the cache
			_compareConfigCache.put(refId, compareConfig);
		}

		return compareConfig;
	}
}
