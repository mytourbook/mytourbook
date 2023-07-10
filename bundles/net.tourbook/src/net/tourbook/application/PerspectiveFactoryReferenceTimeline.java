/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import net.tourbook.photo.PicDirView;
import net.tourbook.statistic.StatisticView;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.views.calendar.CalendarView;
import net.tourbook.ui.views.collateTours.CollatedToursView;
import net.tourbook.ui.views.geoCompare.GeoCompareView;
import net.tourbook.ui.views.referenceTour.ElevationCompareResultView;
import net.tourbook.ui.views.referenceTour.ComparedTourChartView;
import net.tourbook.ui.views.referenceTour.ReferenceTourChartView;
import net.tourbook.ui.views.referenceTour.ReferenceTimelineView;
import net.tourbook.ui.views.referenceTour.ReferenceTourView;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryReferenceTimeline implements IPerspectiveFactory {

   public static final String  PERSPECTIVE_ID                     = "net.tourbook.perspective.TourCatalog"; //$NON-NLS-1$

   private static final String FOLDER_ID_GEO_COMPARE_TOOL         = "folderGeoCompareTool";                 //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_CHART               = "folderTourChart";                      //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_CHART_COMPARED_TOUR = "folderTourChart_ComparedTour";         //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_CHART_REF_Tour      = "folderTourChart_RefTour";              //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_COMPARISON_TIMELINE = "folderTourComparisonTimeline";         //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_DIRECTORIES         = "folderTourDirectories";                //$NON-NLS-1$
   private static final String FOLDER_ID_TOUR_MAPS                = "folderTourMaps";                       //$NON-NLS-1$

   @Override
   public void createInitialLayout(final IPageLayout layout) {

      layout.setEditorAreaVisible(false);

      layout.addShowViewShortcut(GeoCompareView.ID);
      layout.addShowViewShortcut(ElevationCompareResultView.ID);
      layout.addShowViewShortcut(ReferenceTourView.ID);
      layout.addShowViewShortcut(ComparedTourChartView.ID);
      layout.addShowViewShortcut(ReferenceTourChartView.ID);
      layout.addShowViewShortcut(ReferenceTimelineView.ID);

// SET_FORMATTING_OFF

      //--------------------------------------------------------------------------------
      // Left area
      //--------------------------------------------------------------------------------

      final IFolderLayout folderTourDirectory = layout.createFolder(FOLDER_ID_TOUR_DIRECTORIES,

            IPageLayout.LEFT, 0.4f, IPageLayout.ID_EDITOR_AREA);

      folderTourDirectory.addView(ReferenceTourView.ID);
      folderTourDirectory.addPlaceholder(ElevationCompareResultView.ID);
      folderTourDirectory.addPlaceholder(TourBookView.ID);
      folderTourDirectory.addPlaceholder(CalendarView.ID);
      folderTourDirectory.addPlaceholder(StatisticView.ID);
      folderTourDirectory.addPlaceholder(CollatedToursView.ID);
      folderTourDirectory.addPlaceholder(PicDirView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout folderYearStat = layout.createFolder(FOLDER_ID_TOUR_COMPARISON_TIMELINE,

            IPageLayout.BOTTOM, 0.7f, FOLDER_ID_TOUR_DIRECTORIES);

      folderYearStat.addView(ReferenceTimelineView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout folderGeoCompareTool = layout.createFolder(FOLDER_ID_GEO_COMPARE_TOOL,

            IPageLayout.BOTTOM, 0.5f, FOLDER_ID_TOUR_DIRECTORIES);

      folderGeoCompareTool.addView(GeoCompareView.ID);


      //--------------------------------------------------------------------------------
      // Right area
      //--------------------------------------------------------------------------------

      final IFolderLayout refChartFolder = layout.createFolder(FOLDER_ID_TOUR_CHART_REF_Tour,

            IPageLayout.TOP, 0.5f, IPageLayout.ID_EDITOR_AREA);

      refChartFolder.addView(ReferenceTourChartView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout folderComparedTourChart = layout.createFolder(FOLDER_ID_TOUR_CHART_COMPARED_TOUR,

            IPageLayout.BOTTOM, 0.5f, FOLDER_ID_TOUR_CHART_REF_Tour);

      folderComparedTourChart.addView(ComparedTourChartView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout folderMaps = layout.createFolder(FOLDER_ID_TOUR_MAPS,

            IPageLayout.TOP, 0.5f, FOLDER_ID_TOUR_CHART_REF_Tour);

      folderMaps.addView(Map2View.ID);
      folderMaps.addPlaceholder(Map25View.ID);
      folderMaps.addPlaceholder(Map3View.ID);


      //--------------------------------------------------------------------------------

      final IFolderLayout folderTourChart = layout.createFolder(FOLDER_ID_TOUR_CHART,

            IPageLayout.BOTTOM, 0.5f, FOLDER_ID_TOUR_CHART_COMPARED_TOUR);

      folderTourChart.addView(TourChartView.ID);

// SET_FORMATTING_ON
   }

}
