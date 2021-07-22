/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.ui.views.tourCatalog.RefTour_YearStatistic_View;
import net.tourbook.ui.views.tourCatalog.TourCatalogView;
import net.tourbook.ui.views.tourCatalog.TourCatalogView_ComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCatalogView_ReferenceTour;
import net.tourbook.ui.views.tourCatalog.TourCompareResultView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryCompareTours implements IPerspectiveFactory {

   public static final String  PERSPECTIVE_ID            = "net.tourbook.perspective.CompareTours"; //$NON-NLS-1$

   private static final String FOLDER_ID_COMPARE         = "comp";                                  //$NON-NLS-1$
   private static final String FOLDER_ID_LIST            = "list";                                  //$NON-NLS-1$
   private static final String FOLDER_ID_REF             = "ref";                                   //$NON-NLS-1$
   private static final String FOLDER_ID_YEAR_STATISTICS = "folderYearStatistics";                  //$NON-NLS-1$

   @Override
   public void createInitialLayout(final IPageLayout layout) {

      layout.setEditorAreaVisible(false);

      layout.addShowViewShortcut(TourCompareResultView.ID);
      layout.addShowViewShortcut(TourCatalogView.ID);
      layout.addShowViewShortcut(TourCatalogView_ComparedTour.ID);
      layout.addShowViewShortcut(TourCatalogView_ReferenceTour.ID);
      layout.addShowViewShortcut(RefTour_YearStatistic_View.ID);

// SET_FORMATTING_OFF

      //--------------------------------------------------------------------------------
      // Left area
      //--------------------------------------------------------------------------------

      final IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,

            IPageLayout.LEFT,       0.4f,    IPageLayout.ID_EDITOR_AREA);

      listFolder.addView(TourCompareResultView.ID);
      listFolder.addPlaceholder(TourCatalogView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout folderYearStat = layout.createFolder(FOLDER_ID_YEAR_STATISTICS,

            IPageLayout.BOTTOM,     0.7f,    FOLDER_ID_LIST);

      folderYearStat.addView(RefTour_YearStatistic_View.ID);

      //--------------------------------------------------------------------------------
      // Right area
      //--------------------------------------------------------------------------------

      final IFolderLayout refFolder = layout.createFolder(FOLDER_ID_REF,

            IPageLayout.TOP,        0.7f,    IPageLayout.ID_EDITOR_AREA);

      refFolder.addView(TourCatalogView_ReferenceTour.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout compFolder = layout.createFolder(FOLDER_ID_COMPARE,

            IPageLayout.BOTTOM,     0.5f,    FOLDER_ID_REF);

      compFolder.addView(TourCatalogView_ComparedTour.ID);

// SET_FORMATTING_ON
   }

}
