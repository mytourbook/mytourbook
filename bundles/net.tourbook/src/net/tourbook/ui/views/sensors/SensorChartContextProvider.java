/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import java.util.ArrayList;

import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;

/**
 * provides the fill menu methods for the chart context menu
 */
public class SensorChartContextProvider implements IChartContextProvider, ITourProvider {

   private final Chart                 _chart;
   private final ITourProvider         _tourProvider;

   private final ActionEditQuick       _actionEditQuick;
   private final ActionEditTour        _actionEditTour;
   private final ActionOpenTour        _actionOpenTour;
   private final ActionSetTourTypeMenu _actionSetTourType;

   public SensorChartContextProvider(final Chart chart, final ITourProvider tourProvider) {

      _chart = chart;
      _tourProvider = tourProvider;

      _actionEditQuick = new ActionEditQuick(this);
      _actionEditTour = new ActionEditTour(this);
      _actionOpenTour = new ActionOpenTour(this);

      _actionSetTourType = new ActionSetTourTypeMenu(this);
   }

   private void enableActions(final boolean isTourHovered) {

      _actionEditQuick.setEnabled(isTourHovered);
      _actionEditTour.setEnabled(isTourHovered);
      _actionOpenTour.setEnabled(isTourHovered);

      _actionSetTourType.setEnabled(isTourHovered);
      TourTypeMenuManager.enableRecentTourTypeActions(isTourHovered, TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   @Override
   public void fillBarChartContextMenu(final IMenuManager menuMgr,
                                       final int hoveredBarSerieIndex,
                                       final int hoveredBarValueIndex) {

      menuMgr.add(_actionEditQuick);
      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionOpenTour);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionSetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

      enableActions(hoveredBarSerieIndex != -1);
   }

   @Override
   public void fillContextMenu(final IMenuManager menuMgr,
                               final int mouseDownDevPositionX,
                               final int mouseDownDevPositionY) {}

   @Override
   public void fillXSliderContextMenu(final IMenuManager menuMgr,
                                      final ChartXSlider leftSlider,
                                      final ChartXSlider rightSlider) {}

   @Override
   public Chart getChart() {
      return _chart;
   }

   @Override
   public ChartXSlider getLeftSlider() {
      return null;
   }

   @Override
   public ChartXSlider getRightSlider() {
      return null;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> allSelectedTours = _tourProvider.getSelectedTours();
      if (allSelectedTours != null && allSelectedTours.size() > 0) {

         return allSelectedTours;
      }

      return null;
   }

   @Override
   public void onHideContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {}

   @Override
   public void onShowContextMenu(final MenuEvent menuEvent, final Control menuParentControl) {}

   @Override
   public boolean showOnlySliderContextMenu() {
      return false;
   }
}
