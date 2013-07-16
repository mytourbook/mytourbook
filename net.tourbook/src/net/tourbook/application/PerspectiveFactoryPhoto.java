/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
import net.tourbook.photo.PicDirView;
import net.tourbook.photo.TourPhotoLinkView;
import net.tourbook.photo.TourPhotosView;
import net.tourbook.ui.tourChart.TourChartView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryPhoto implements IPerspectiveFactory {

	public static final String	PERSPECTIVE_ID			= "net.tourbook.perspective.TourPhotoLinks";	//$NON-NLS-1$

	private static final String	FOLDER_MAP				= "FOLDER_MAP";								//$NON-NLS-1$
	private static final String	FOLDER_PHOTO_GALLERY	= "FOLDER_PHOTO_GALLERY";						//$NON-NLS-1$
	private static final String	FOLDER_TOUR_CHART		= "FOLDER_TOUR_CHART";							//$NON-NLS-1$
	private static final String	FOLDER_TOUR_PHOTO		= "FOLDER_TOUR_PHOTO";							//$NON-NLS-1$
	private static final String	FOLDER_TOUR_PHOTO_LINKS	= "FOLDER_TOUR_PHOTO_LINKS";					//$NON-NLS-1$

	public void createInitialLayout(final IPageLayout layout) {

		layout.setEditorAreaVisible(false);

		//--------------------------------------------------------------------------------

		final IFolderLayout folderTourPhoto = layout.createFolder(FOLDER_TOUR_PHOTO,//
				IPageLayout.BOTTOM,
				0.85f,
				IPageLayout.ID_EDITOR_AREA);

		folderTourPhoto.addView(TourPhotosView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout folderPicDir = layout.createFolder(FOLDER_PHOTO_GALLERY,//
				IPageLayout.RIGHT,
				0.5f,
				IPageLayout.ID_EDITOR_AREA);

		folderPicDir.addView(PicDirView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout folderMap = layout.createFolder(FOLDER_MAP, //
				IPageLayout.RIGHT,
				0.5f,
				FOLDER_PHOTO_GALLERY);

		folderMap.addView(Map2View.ID);
		folderMap.addPlaceholder(IExternalIds.VIEW_ID_NET_TOURBOOK_MAP3_MAP3_VIEW);

		//--------------------------------------------------------------------------------

		final IFolderLayout tourChartFolder = layout.createFolder(FOLDER_TOUR_CHART, //
				IPageLayout.BOTTOM,
				0.6f,
				FOLDER_MAP);

		tourChartFolder.addView(TourChartView.ID);

		//--------------------------------------------------------------------------------

		final IFolderLayout folderLink = layout.createFolder(FOLDER_TOUR_PHOTO_LINKS,//
				IPageLayout.BOTTOM,
				0.5f,
				FOLDER_PHOTO_GALLERY);

		folderLink.addView(TourPhotoLinkView.ID);
		folderLink.addPlaceholder(IExternalIds.VIEW_ID_NET_TOURBOOK_MAP3_MAP3_LAYER_VIEW);
	}

}
