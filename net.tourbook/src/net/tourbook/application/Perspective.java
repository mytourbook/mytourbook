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

package net.tourbook.application;

import net.tourbook.views.TourChartAnalyzerView;
import net.tourbook.views.rawData.RawDataView;
import net.tourbook.views.tourBook.TourBookView;
import net.tourbook.views.tourMap.TourMapView;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;


public class Perspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {

		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		float ratio = 0.5f;
		
		layout.addPlaceholder(RawDataView.ID, IPageLayout.TOP, ratio, editorArea);
		layout.addPlaceholder(TourChartAnalyzerView.ID, IPageLayout.TOP, ratio, editorArea);
		layout.addPlaceholder(TourBookView.ID, IPageLayout.TOP, ratio, editorArea);
		layout.addPlaceholder(TourMapView.ID, IPageLayout.TOP, ratio, editorArea);
	}

	
}
