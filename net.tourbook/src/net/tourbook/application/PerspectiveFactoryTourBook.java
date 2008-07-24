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
package net.tourbook.application;

import net.tourbook.ui.views.TourChartView;
import net.tourbook.ui.views.TourMarkerView;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourBook implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID			= "net.tourbook.perspective.TourBook";	//$NON-NLS-1$

	private static final String	FOLDER_ID_CHART			= "chart";								//$NON-NLS-1$
	private static final String	FOLDER_ID_STATISTICS	= "stat";								//$NON-NLS-1$
	private static final String	FOLDER_ID_LIST			= "list";								//$NON-NLS-1$

	public void createInitialLayout(final IPageLayout layout) {

		//--------------------------------------------------------------------------------

		final IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,
				IPageLayout.LEFT,
				0.4f,
				IPageLayout.ID_EDITOR_AREA);

		listFolder.addView(TourBookView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout markerFolder = layout.createFolder(FOLDER_ID_STATISTICS,
				IPageLayout.BOTTOM,
				0.7f,
				FOLDER_ID_LIST);

		markerFolder.addView(TourMarkerView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout chartFolder = layout.createFolder(FOLDER_ID_CHART,
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		chartFolder.addView(TourChartView.ID);
	}

}
