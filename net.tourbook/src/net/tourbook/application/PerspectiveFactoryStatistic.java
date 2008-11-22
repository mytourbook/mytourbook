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

import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.views.TourStatisticsView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryStatistic implements IPerspectiveFactory {

	static final String			PERSPECTIVE_ID		= "net.tourbook.perspective.Statistic"; //$NON-NLS-1$

	private static final String	FOLDER_ID_BOTTOM	= "bottom";							//$NON-NLS-1$
	private static final String	FOLDER_ID_TOP		= "top";								//$NON-NLS-1$

	public void createInitialLayout(final IPageLayout layout) {

		layout.setEditorAreaVisible(false);

		//--------------------------------------------------------------------------------

		final IFolderLayout topFolder = layout.createFolder(FOLDER_ID_TOP,
				IPageLayout.TOP,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		topFolder.addView(TourStatisticsView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout bottomFolder = layout.createFolder(FOLDER_ID_BOTTOM,
				IPageLayout.LEFT,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		bottomFolder.addView(TourChartView.ID);

//		IPlaceholderFolderLayout rightFolder = layout.createPlaceholderFolder("right",
//				IPageLayout.RIGHT,
//				0.5f,
//				IPageLayout.ID_EDITOR_AREA);
//
//		rightFolder.addPlaceholder(TourChartView.ID);

	}
}
