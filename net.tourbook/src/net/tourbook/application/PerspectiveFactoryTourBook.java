/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.mapping.TourMapView;
import net.tourbook.statistic.TourStatisticsView;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.views.TourMarkerView;
import net.tourbook.ui.views.TourWaypointView;
import net.tourbook.ui.views.calendar.CalendarView;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourBook.TourBookView;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourBook implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID		= "net.tourbook.perspective.TourBook";	//$NON-NLS-1$

	private static final String	FOLDER_ID_MAP		= "map";								//$NON-NLS-1$
	private static final String	FOLDER_ID_CHART		= "chart";								//$NON-NLS-1$
	private static final String	FOLDER_ID_MARKER	= "marker";							//$NON-NLS-1$
	private static final String	FOLDER_ID_LIST		= "list";								//$NON-NLS-1$

	public void createInitialLayout(final IPageLayout layout) {

		/*
		 * the sequence is VERY important how the folders are created !!!
		 */

		layout.setEditorAreaVisible(false);

		//--------------------------------------------------------------------------------

		final IFolderLayout mapFolder = layout.createFolder(//
				FOLDER_ID_MAP,
				IPageLayout.RIGHT,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		mapFolder.addView(TourMapView.ID);
		mapFolder.addView(TourDataEditorView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout chartFolder = layout.createFolder(//
				FOLDER_ID_CHART,
				IPageLayout.BOTTOM,
				0.5f,
				FOLDER_ID_MAP);

		chartFolder.addView(TourChartView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout listFolder = layout.createFolder(
				FOLDER_ID_LIST,
				IPageLayout.TOP,
				0.6f,
				IPageLayout.ID_EDITOR_AREA);

		listFolder.addView(RawDataView.ID);
		listFolder.addView(TourBookView.ID);
		listFolder.addView(CalendarView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout markerFolder = layout.createFolder(
				FOLDER_ID_MARKER,
				IPageLayout.BOTTOM,
				0.6f,
				FOLDER_ID_LIST);

		markerFolder.addView(TourStatisticsView.ID);
		markerFolder.addView(TourMarkerView.ID);
		markerFolder.addView(TourWaypointView.ID);
	}
}
