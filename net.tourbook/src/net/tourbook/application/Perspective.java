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

package net.tourbook.application;

import net.tourbook.ui.views.TourChartAnalyzerView;
import net.tourbook.ui.views.TourMarkerView;
import net.tourbook.ui.views.TourSegmenterView;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tourBook.TourBookView;
import net.tourbook.ui.views.tourMap.TourMapView;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class Perspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		IPlaceholderFolderLayout placeHolderLeft = layout.createPlaceholderFolder(
				"left", IPageLayout.LEFT, (float) 0.3, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderLeft.addPlaceholder(TourChartAnalyzerView.ID);
		placeHolderLeft.addPlaceholder(TourSegmenterView.ID);
		placeHolderLeft.addPlaceholder(TourMarkerView.ID);

		IPlaceholderFolderLayout placeHolderTop = layout.createPlaceholderFolder(
				"top", IPageLayout.TOP, (float) 0.6, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		placeHolderTop.addPlaceholder(RawDataView.ID);
		placeHolderTop.addPlaceholder(TourBookView.ID);
		placeHolderTop.addPlaceholder(TourMapView.ID);
		
	}

}
