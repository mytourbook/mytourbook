/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
import net.tourbook.photo.PhotosAndToursView;
import net.tourbook.photo.PicDirView;
import net.tourbook.photo.TourPhotosView;
import net.tourbook.ui.tourChart.TourChartView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryPhoto implements IPerspectiveFactory {

	public static final String	PERSPECTIVE_ID			= "net.tourbook.perspective.MergePhotos";	//$NON-NLS-1$

	private static final String	FOLDER_ID_PHOTO_MERGE	= "FOLDER_ID_PHOTO_MERGE";					//$NON-NLS-1$
	private static final String	FOLDER_ID_TOUR_PHOTO	= "FOLDER_ID_TOUR_PHOTO";					//$NON-NLS-1$
	private static final String	FOLDER_ID_MAP			= "FOLDER_ID_MAP";							//$NON-NLS-1$
	private static final String	FOLDER_ID_TOUR_CHART	= "FOLDER_ID_TOUR_CHART";					//$NON-NLS-1$

	public void createInitialLayout(final IPageLayout layout) {

		layout.setEditorAreaVisible(false);

		//--------------------------------------------------------------------------------

		final IFolderLayout photoMergeFolder = layout.createFolder(FOLDER_ID_PHOTO_MERGE,//
				IPageLayout.RIGHT,
				0.3f,
				IPageLayout.ID_EDITOR_AREA);

		photoMergeFolder.addView(PicDirView.ID);
		photoMergeFolder.addView(PhotosAndToursView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout tourPhotoFolder = layout.createFolder(FOLDER_ID_TOUR_PHOTO,//
				IPageLayout.BOTTOM,
				0.85f,
				FOLDER_ID_PHOTO_MERGE);

		tourPhotoFolder.addView(TourPhotosView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout tourChartFolder = layout.createFolder(FOLDER_ID_TOUR_CHART, //
				IPageLayout.BOTTOM,
				0.7f,
				FOLDER_ID_PHOTO_MERGE);

		tourChartFolder.addView(TourChartView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout mapFolder = layout.createFolder(FOLDER_ID_MAP, //
				IPageLayout.RIGHT,
				0.5f,
				FOLDER_ID_PHOTO_MERGE);

		mapFolder.addView(TourMapView.ID);
	}

}
