/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import net.tourbook.statistic.StatisticView;
import net.tourbook.tour.TourLogView;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.ui.views.TourPausesView;
import net.tourbook.ui.views.TourWaypointView;
import net.tourbook.ui.views.calendar.CalendarView;
import net.tourbook.ui.views.collateTours.CollatedToursView;
import net.tourbook.ui.views.geoCompare.GeoCompareView;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.ui.views.tagging.TourTags_View;
import net.tourbook.ui.views.tourBook.TourBookView;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;
import net.tourbook.ui.views.tourMarker.TourMarkerView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PerspectiveFactoryTourBook implements IPerspectiveFactory {

   static final String         PERSPECTIVE_ID   = "net.tourbook.perspective.TourBook"; //$NON-NLS-1$

   private static final String FOLDER_ID_MAP    = "map";                               //$NON-NLS-1$
   private static final String FOLDER_ID_CHART  = "chart";                             //$NON-NLS-1$
   private static final String FOLDER_ID_MARKER = "marker";                            //$NON-NLS-1$
   private static final String FOLDER_ID_LIST   = "list";                              //$NON-NLS-1$

   @Override
   public void createInitialLayout(final IPageLayout layout) {

      /*
       * the sequence is VERY important how the folders are created !!!
       */

      layout.setEditorAreaVisible(false);

      //--------------------------------------------------------------------------------

      final IFolderLayout mapFolder = layout.createFolder(FOLDER_ID_MAP,
            IPageLayout.RIGHT,
            0.4f,
            IPageLayout.ID_EDITOR_AREA);

      mapFolder.addView(Map2View.ID);
      mapFolder.addPlaceholder(IExternalIds.VIEW_ID_NET_TOURBOOK_MAP3_MAP3_VIEW_ID);
      mapFolder.addView(TourDataEditorView.ID);
      mapFolder.addPlaceholder(TourLogView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout chartFolder = layout.createFolder(FOLDER_ID_CHART,
            IPageLayout.BOTTOM,
            0.5f,
            FOLDER_ID_MAP);

      chartFolder.addView(TourChartView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout listFolder = layout.createFolder(FOLDER_ID_LIST,
            IPageLayout.TOP,
            0.6f,
            IPageLayout.ID_EDITOR_AREA);

      listFolder.addView(RawDataView.ID);
      listFolder.addView(TourBookView.ID);
      listFolder.addView(CalendarView.ID);
      listFolder.addPlaceholder(CollatedToursView.ID);
      listFolder.addView(StatisticView.ID);
      listFolder.addPlaceholder(PicDirView.ID);

      //--------------------------------------------------------------------------------

      final IFolderLayout markerFolder = layout.createFolder(FOLDER_ID_MARKER,
            IPageLayout.BOTTOM,
            0.6f,
            FOLDER_ID_LIST);

      markerFolder.addView(TourMarkerView.ID);
      markerFolder.addView(TourTags_View.ID);
      markerFolder.addView(TourWaypointView.ID);
      markerFolder.addView(TourPausesView.ID);
      markerFolder.addPlaceholder(GeoCompareView.ID);
   }
}
