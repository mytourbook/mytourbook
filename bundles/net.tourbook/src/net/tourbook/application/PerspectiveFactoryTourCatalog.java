/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import net.tourbook.map2.view.Map2View;
import net.tourbook.map25.Map25View;
import net.tourbook.map3.view.Map3View;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.views.tourCatalog.TourCatalogView;
import net.tourbook.ui.views.tourCatalog.TourCatalogViewComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogViewReferenceTour;
import net.tourbook.ui.views.tourCatalog.YearStatisticView;
import net.tourbook.ui.views.tourCatalog.geo.GeoCompareView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class PerspectiveFactoryTourCatalog implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID					= "net.tourbook.perspective.TourCatalog";	//$NON-NLS-1$

	private static final String	FOLDER_ID_COMPARED_TOUR_CHART	= "comparedTourChart";						//$NON-NLS-1$
	private static final String	FOLDER_ID_GEO_COMPARE_TOOL		= "geoCompareTool";							//$NON-NLS-1$
	private static final String	FOLDER_ID_REFERENCE_TOURS		= "referenceTours";							//$NON-NLS-1$
	private static final String	FOLDER_ID_REFERENCE_TOUR_CHART	= "referenceTourChart";						//$NON-NLS-1$
	private static final String	FOLDER_ID_STAT					= "stat";									//$NON-NLS-1$
	private static final String	FOLDER_ID_TOUR_CHART			= "tourChart";								//$NON-NLS-1$
	private static final String	FOLDER_ID_TOUR_MAPS				= "tourMaps";								//$NON-NLS-1$

	@Override
	public void createInitialLayout(final IPageLayout layout) {

		layout.setEditorAreaVisible(false);

// SET_FORMATTING_OFF
		
		//--------------------------------------------------------------------------------
		// Left area
		//--------------------------------------------------------------------------------

		final IFolderLayout folderReferenceTours = layout.createFolder(FOLDER_ID_REFERENCE_TOURS,
				
				IPageLayout.LEFT, 0.4f, IPageLayout.ID_EDITOR_AREA);

		folderReferenceTours.addView(TourCatalogView.ID);
		
		//--------------------------------------------------------------------------------
		
		final IFolderLayout folderYearStat = layout.createFolder(FOLDER_ID_STAT,
				
				IPageLayout.BOTTOM, 0.7f, FOLDER_ID_REFERENCE_TOURS);
		
		folderYearStat.addView(YearStatisticView.ID);

		//--------------------------------------------------------------------------------

		final IPlaceholderFolderLayout folderGeoCompareTool = layout.createPlaceholderFolder(FOLDER_ID_GEO_COMPARE_TOOL,
				
				IPageLayout.BOTTOM, 0.5f, FOLDER_ID_REFERENCE_TOURS);

		folderGeoCompareTool.addPlaceholder(GeoCompareView.ID);

		//--------------------------------------------------------------------------------
		// Right area
		//--------------------------------------------------------------------------------
		
		final IFolderLayout refChartFolder = layout.createFolder(FOLDER_ID_REFERENCE_TOUR_CHART,
				
				IPageLayout.TOP, 0.5f, IPageLayout.ID_EDITOR_AREA);
		
		refChartFolder.addView(TourCatalogViewReferenceTour.ID);
		
		//--------------------------------------------------------------------------------
		
		final IFolderLayout folderComparedTourChart = layout.createFolder(FOLDER_ID_COMPARED_TOUR_CHART,
				
				IPageLayout.BOTTOM, 0.5f, FOLDER_ID_REFERENCE_TOUR_CHART);

		folderComparedTourChart.addView(TourCatalogViewComparedTour.ID);

		//--------------------------------------------------------------------------------
		
		final IPlaceholderFolderLayout folderMaps = layout.createPlaceholderFolder(FOLDER_ID_TOUR_MAPS,
				
				IPageLayout.TOP, 0.5f, FOLDER_ID_REFERENCE_TOUR_CHART);
		
		folderMaps.addPlaceholder(Map2View.ID);
		folderMaps.addPlaceholder(Map25View.ID);
		folderMaps.addPlaceholder(Map3View.ID);
		
		
		//--------------------------------------------------------------------------------
		
		final IPlaceholderFolderLayout folderTourChart = layout.createPlaceholderFolder(FOLDER_ID_TOUR_CHART,
				
				IPageLayout.BOTTOM, 0.5f, FOLDER_ID_COMPARED_TOUR_CHART);
		
		folderTourChart.addPlaceholder(TourChartView.ID);
		
		
// SET_FORMATTING_ON
	}

}
