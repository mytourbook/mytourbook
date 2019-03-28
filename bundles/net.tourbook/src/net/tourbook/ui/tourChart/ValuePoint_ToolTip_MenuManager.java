/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.tooltip.Pinned_ToolTip_PinLocation;
import net.tourbook.data.TourData;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

public class ValuePoint_ToolTip_MenuManager {

   static final String                 STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS = "ValuePoint_ToolTip_VisibleGraphs";     //$NON-NLS-1$
   static final String                 STATE_VALUE_POINT_TOOLTIP_ORIENTATION    = "ValuePoint_ToolTip_Orientation";       //$NON-NLS-1$

   static ValuePoint_ToolTip_Orientation DEFAULT_ORIENTATION                      = ValuePoint_ToolTip_Orientation.Horizontal;

   static final long                   VALUE_ID_ALTIMETER                       = 1 << 1;
   static final long                   VALUE_ID_ALTITUDE                        = 1 << 2;
   static final long                   VALUE_ID_CADENCE                         = 1 << 3;
   static final long                   VALUE_ID_DISTANCE                        = 1 << 4;
   static final long                   VALUE_ID_GRADIENT                        = 1 << 5;
   static final long                   VALUE_ID_PACE                            = 1 << 6;
   static final long                   VALUE_ID_POWER                           = 1 << 7;
   static final long                   VALUE_ID_PULSE                           = 1 << 8;
   static final long                   VALUE_ID_SPEED                           = 1 << 9;
   static final long                   VALUE_ID_TEMPERATURE                     = 1 << 10;
   static final long                   VALUE_ID_TIME_DURATION                   = 1 << 11;
   static final long                   VALUE_ID_TIME_OF_DAY                     = 1 << 12;
   static final long                   VALUE_ID_TIME_SLICES                     = 1 << 13;
   static final long                   VALUE_ID_CHART_ZOOM_FACTOR               = 1 << 14;
   static final long                   VALUE_ID_GEARS                           = 1 << 15;
   static final long                   VALUE_ID_TOUR_COMPARE_RESULT             = 1 << 16;
   static final long                   VALUE_ID_RUN_DYN_STANCE_TIME             = 1 << 17;
   static final long                   VALUE_ID_RUN_DYN_STANCE_TIME_BALANCED    = 1 << 18;
   static final long                   VALUE_ID_RUN_DYN_STEP_LENGTH             = 1 << 19;
   static final long                   VALUE_ID_RUN_DYN_VERTICAL_OSCILLATION    = 1 << 20;
   static final long                   VALUE_ID_RUN_DYN_VERTICAL_RATIO          = 1 << 21;

   static final long                   DEFAULT_GRAPHS                           =

         VALUE_ID_TIME_SLICES
               | VALUE_ID_TIME_DURATION
               | VALUE_ID_DISTANCE

   ;

   private long                        _allVisibleValueIds;

   private IDialogSettings             _state;
   private TourData                    _tourData;

   private ValuePoint_ToolTip_UI         _valuePointToolTipUI;

   private Menu                        _menu                                    = null;

   private boolean                     _isHorizontal;

   /**
    * Parent of this tool item is the parent for the tooltip menu.
    */
   private ToolItem                    _menuParentItem;

   private ActionHideToolTip           _actionHideToolTip;
   private ActionSetDefaults           _actionSetDefaults;
   private ActionCloseTTContextMenu    _actionCloseTTContextMenu;

   private ActionOrientation           _actionHorizontalOrientation;
   private ActionOrientation           _actionVerticalOrientation;

   private ActionValueItem             _actionValue_Altimeter;
   private ActionValueItem             _actionValue_Altitude;
   private ActionValueItem             _actionValue_Cadence;
   private ActionValueItem             _actionValue_ChartZoomFactor;
   private ActionValueItem             _actionValue_Distance;
   private ActionValueItem             _actionValue_Gears;
   private ActionValueItem             _actionValue_Gradient;
   private ActionValueItem             _actionValue_Header;
   private ActionValueItem             _actionValue_Pace;
   private ActionValueItem             _actionValue_Power;
   private ActionValueItem             _actionValue_Pulse;
   private ActionValueItem             _actionValue_Speed;
   private ActionValueItem             _actionValue_Temperature;
   private ActionValueItem             _actionValue_TimeDuration;
   private ActionValueItem             _actionValue_TimeOfDay;
   private ActionValueItem             _actionValue_TimeSlices;
   private ActionValueItem             _actionValue_TourCompareResult;

   private ActionValueItem             _actionValue_RunDyn_StanceTime;
   private ActionValueItem             _actionValue_RunDyn_StanceTimeBalance;
   private ActionValueItem             _actionValue_RunDyn_StepLength;
   private ActionValueItem             _actionValue_RunDyn_VerticalOscillation;
   private ActionValueItem             _actionValue_RunDyn_VerticalRatio;

   private Action                      _actionPinLocation_Header;
   private ActionPinLocation           _actionPinLocation_Screen;
   private ActionPinLocation           _actionPinLocation_TopRight;
   private ActionPinLocation           _actionPinLocation_TopLeft;
   private ActionPinLocation           _actionPinLocation_BottomLeft;
   private ActionPinLocation           _actionPinLocation_BottomRight;
   private ActionPinLocation           _actionPinLocation_MouseXPosition;

   private final class ActionCloseTTContextMenu extends Action {

      public ActionCloseTTContextMenu() {
         super(Messages.Tooltip_ValuePoint_Action_CloseContextMenu);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Cancel));
      }

      @Override
      public void run() {
         _menu.setVisible(false);
      }
   }

   private class ActionHideToolTip extends Action {

      public ActionHideToolTip() {
         setText(Messages.Action_ToolTip_Hide);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Cancel));
      }

      @Override
      public void run() {
         _valuePointToolTipUI.actionHideToolTip();
      }
   }

   private final class ActionOrientation extends Action {

      private ValuePoint_ToolTip_Orientation _orientation;

      public ActionOrientation(final ValuePoint_ToolTip_Orientation orientation) {

         if (orientation == ValuePoint_ToolTip_Orientation.Horizontal) {
            setText(Messages.Tooltip_ValuePoint_Action_Orientation_Horizontal);
         } else {
            setText(Messages.Tooltip_ValuePoint_Action_Orientation_Vertical);
         }

         _orientation = orientation;
      }

      @Override
      public void run() {

         _state.put(STATE_VALUE_POINT_TOOLTIP_ORIENTATION, _orientation.name());

         _valuePointToolTipUI.actionOrientation(_orientation, true);
      }
   }

   private class ActionPinLocation extends Action {

      public Pinned_ToolTip_PinLocation _locationId;

      public ActionPinLocation(final Pinned_ToolTip_PinLocation locationId, final String text) {
         setText(text);
         _locationId = locationId;
      }

      @Override
      public void run() {

         _valuePointToolTipUI.setPinLocation(_locationId);

         // reopen tooltip menu with the new location
         if (_locationId == Pinned_ToolTip_PinLocation.TopLeft
               || _locationId == Pinned_ToolTip_PinLocation.TopRight
               || _locationId == Pinned_ToolTip_PinLocation.BottomLeft
               || _locationId == Pinned_ToolTip_PinLocation.BottomRight
         //
         ) {
// this is very annoying
//            openToolTipMenu10Reopen();
         }
      }
   }

   private class ActionSetDefaults extends Action {

      public ActionSetDefaults() {
         setText(Messages.Action_ToolTip_SetDefaults);
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__undo_edit));
      }

      @Override
      public void run() {

         /*
          * set defaults into the state
          */
         final ValuePoint_ToolTip_Orientation orientation = ValuePoint_ToolTip_MenuManager.DEFAULT_ORIENTATION;
         _state.put(STATE_VALUE_POINT_TOOLTIP_ORIENTATION, orientation.name());

         _allVisibleValueIds = DEFAULT_GRAPHS;
         _state.put(STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS, _allVisibleValueIds);

         // update tooltip with default values
         _valuePointToolTipUI.actionSetDefaults(_allVisibleValueIds, orientation);
      }
   }

   private class ActionValueItem extends Action {

      private long            _graphId;

      private ImageDescriptor _graphImage;
      private ImageDescriptor _graphImageDisabled;

      public ActionValueItem(final long graphId,
                             final String name,
                             final String graphImageName,
                             final String graphImageNameDisabled) {

         super(name, AS_CHECK_BOX);

         _graphId = graphId;

         if (graphImageName != null) {
            _graphImage = TourbookPlugin.getImageDescriptor(graphImageName);
         }
         if (graphImageNameDisabled != null) {
            _graphImageDisabled = TourbookPlugin.getImageDescriptor(graphImageNameDisabled);
         }
      }

      @Override
      public void run() {
         actionValueItem(_graphId, isChecked());
      }

      private void setState(final boolean isChecked, final boolean isEnabled) {

         setEnabled(isEnabled);

         // show checked state only when also enabled
         setChecked(isChecked && isEnabled);

         setImageDescriptor(isEnabled ? _graphImage : _graphImageDisabled);
      }
   }

   public ValuePoint_ToolTip_MenuManager(final ValuePoint_ToolTip_UI valuePointToolTipUI, final IDialogSettings state) {

      _valuePointToolTipUI = valuePointToolTipUI;
      _state = state;

      createActions();
   }

   /**
    * Show/hide value and reopen the tool tip with the new or hidden value.
    *
    * @param graphId
    *           Graph id which should be displayed/hidden.
    * @param isChecked
    */
   private void actionValueItem(final long graphId, final boolean isChecked) {

      final long currentVisibleValues = _allVisibleValueIds;

      if (isChecked) {

         // display additional graph

         _allVisibleValueIds = currentVisibleValues | graphId;

      } else {

         // remove graph

         /**
          * <pre>
          * a = 0011
          * b = 0110
          * a|b = 0111
          * a&b = 0010
          * a^b = 0101
          * ~a&b|a&~b = 0101
          * ~a = 1100
          * </pre>
          */
         _allVisibleValueIds = (~currentVisibleValues & graphId) | (currentVisibleValues & ~graphId);
      }

      _state.put(STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS, _allVisibleValueIds);

      // update tooltip with new/removed graphs
      final ToolItem toolItem = _valuePointToolTipUI.actionVisibleValues(_allVisibleValueIds);

      // reopen context menu
      openToolTipMenu10Reopen(toolItem);
   }

   private void addItem(final Action action) {
      final ActionContributionItem item = new ActionContributionItem(action);
      item.fill(_menu, -1);
   }

   private void createActions() {

      _actionHideToolTip = new ActionHideToolTip();
      _actionSetDefaults = new ActionSetDefaults();
      _actionCloseTTContextMenu = new ActionCloseTTContextMenu();
      _actionHorizontalOrientation = new ActionOrientation(ValuePoint_ToolTip_Orientation.Horizontal);
      _actionVerticalOrientation = new ActionOrientation(ValuePoint_ToolTip_Orientation.Vertical);

      createPinActions();
      createGraphActions();
   }

   private void createGraphActions() {

      _actionValue_Header = new ActionValueItem(//
            -1,
            Messages.Tooltip_ValuePoint_Action_Value_Header,
            null,
            null);

      _actionValue_TimeSlices = new ActionValueItem(//
            VALUE_ID_TIME_SLICES,
            Messages.Tooltip_ValuePoint_Action_Value_TimeSlices,
            null,
            null);

      _actionValue_ChartZoomFactor = new ActionValueItem(//
            VALUE_ID_CHART_ZOOM_FACTOR,
            Messages.Tooltip_ValuePoint_Action_Value_ChartZoomFactor,
            null,
            null);

      _actionValue_TimeDuration = new ActionValueItem(//
            VALUE_ID_TIME_DURATION,
            Messages.Tooltip_ValuePoint_Action_Value_TimeDuration,
            null,
            null);

      _actionValue_TimeOfDay = new ActionValueItem(//
            VALUE_ID_TIME_OF_DAY,
            Messages.Tooltip_ValuePoint_Action_Value_TimeOfDay,
            null,
            null);

      _actionValue_Distance = new ActionValueItem(
            VALUE_ID_DISTANCE,
            Messages.Tooltip_ValuePoint_Action_Value_Distance,
            null,
            null);

      _actionValue_Altitude = new ActionValueItem(
            VALUE_ID_ALTITUDE,
            Messages.Tooltip_ValuePoint_Action_Value_Altitude,
            Messages.Image__graph_altitude,
            Messages.Image__graph_altitude_disabled);

      _actionValue_Altimeter = new ActionValueItem(
            VALUE_ID_ALTIMETER,
            Messages.Tooltip_ValuePoint_Action_Value_Altimeter,
            Messages.Image__graph_altimeter,
            Messages.Image__graph_altimeter_disabled);

      _actionValue_Cadence = new ActionValueItem(
            VALUE_ID_CADENCE,
            Messages.Tooltip_ValuePoint_Action_Value_Cadence,
            Messages.Image__graph_cadence,
            Messages.Image__graph_cadence_disabled);

      _actionValue_Gears = new ActionValueItem(
            VALUE_ID_GEARS,
            Messages.Tooltip_ValuePoint_Action_Value_Gears,
            Messages.Image__Graph_Gears,
            Messages.Image__Graph_Gears_disabled);

      _actionValue_Gradient = new ActionValueItem(
            VALUE_ID_GRADIENT,
            Messages.Tooltip_ValuePoint_Action_Value_Gradient,
            Messages.Image__graph_gradient,
            Messages.Image__graph_gradient_disabled);

      _actionValue_Pace = new ActionValueItem(
            VALUE_ID_PACE,
            Messages.Tooltip_ValuePoint_Action_Value_Pace,
            Messages.Image__graph_pace,
            Messages.Image__graph_pace_disabled);

      _actionValue_Power = new ActionValueItem(
            VALUE_ID_POWER,
            Messages.Tooltip_ValuePoint_Action_Value_Power,
            Messages.Image__graph_power,
            Messages.Image__graph_power_disabled);

      _actionValue_Pulse = new ActionValueItem(
            VALUE_ID_PULSE,
            Messages.Tooltip_ValuePoint_Action_Value_Pulse,
            Messages.Image__graph_heartbeat,
            Messages.Image__graph_heartbeat_disabled);

      _actionValue_Speed = new ActionValueItem(
            VALUE_ID_SPEED,
            Messages.Tooltip_ValuePoint_Action_Value_Speed,
            Messages.Image__graph_speed,
            Messages.Image__graph_speed_disabled);

      _actionValue_Temperature = new ActionValueItem(
            VALUE_ID_TEMPERATURE,
            Messages.Tooltip_ValuePoint_Action_Value_Temperature,
            Messages.Image__graph_temperature,
            Messages.Image__graph_temperature_disabled);

      _actionValue_TourCompareResult = new ActionValueItem(
            VALUE_ID_TOUR_COMPARE_RESULT,
            Messages.Tooltip_ValuePoint_Action_Value_TourCompareResult,
            Messages.Image__graph_tour_compare,
            Messages.Image__graph_tour_compare_disabled);

      _actionValue_RunDyn_StanceTime = new ActionValueItem(
            VALUE_ID_RUN_DYN_STANCE_TIME,
            Messages.Tooltip_ValuePoint_Action_Value_RunDyn_StanceTime,
            Messages.Image__Graph_RunDyn_StanceTime,
            Messages.Image__Graph_RunDyn_StanceTime_Disabled);

      _actionValue_RunDyn_StanceTimeBalance = new ActionValueItem(
            VALUE_ID_RUN_DYN_STANCE_TIME_BALANCED,
            Messages.Tooltip_ValuePoint_Action_Value_RunDyn_StanceTimeBalance,
            Messages.Image__Graph_RunDyn_StanceTimeBalance,
            Messages.Image__Graph_RunDyn_StanceTimeBalance_Disabled);

      _actionValue_RunDyn_StepLength = new ActionValueItem(
            VALUE_ID_RUN_DYN_STEP_LENGTH,
            Messages.Tooltip_ValuePoint_Action_Value_RunDyn_StepLength,
            Messages.Image__Graph_RunDyn_StepLength,
            Messages.Image__Graph_RunDyn_StepLength_Disabled);

      _actionValue_RunDyn_VerticalOscillation = new ActionValueItem(
            VALUE_ID_RUN_DYN_VERTICAL_OSCILLATION,
            Messages.Tooltip_ValuePoint_Action_Value_RunDyn_VerticalOscillation,
            Messages.Image__Graph_RunDyn_VerticalOscillation,
            Messages.Image__Graph_RunDyn_VerticalOscillation_Disabled);

      _actionValue_RunDyn_VerticalRatio = new ActionValueItem(
            VALUE_ID_RUN_DYN_VERTICAL_RATIO,
            Messages.Tooltip_ValuePoint_Action_Value_RunDyn_VerticalRatio,
            Messages.Image__Graph_RunDyn_VerticalRatio,
            Messages.Image__Graph_RunDyn_VerticalRatio_Disabled);
   }

   private void createPinActions() {

      _actionPinLocation_Header = new Action(Messages.Tooltip_ValuePoint_Action_PinLocation_Header) {};

      _actionPinLocation_Screen = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.Screen,
            Messages.Tooltip_ValuePoint_Action_PinLocation_Screen);

      _actionPinLocation_TopLeft = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.TopLeft,
            Messages.Tooltip_ValuePoint_Action_PinLocation_TopLeft);

      _actionPinLocation_TopRight = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.TopRight,
            Messages.Tooltip_ValuePoint_Action_PinLocation_TopRight);

      _actionPinLocation_BottomLeft = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.BottomLeft,
            Messages.Tooltip_ValuePoint_Action_PinLocation_BottomLeft);

      _actionPinLocation_BottomRight = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.BottomRight,
            Messages.Tooltip_ValuePoint_Action_PinLocation_BottomRight);

      _actionPinLocation_MouseXPosition = new ActionPinLocation(
            Pinned_ToolTip_PinLocation.MouseXPosition,
            Messages.Tooltip_ValuePoint_Action_PinLocation_MouseXPosition);
   }

   void dispose() {
      if (_menu != null) {
         _menu.dispose();
         _menu = null;
      }
   }

   private void enableActions() {

      final Pinned_ToolTip_PinLocation pinnedLocation = _valuePointToolTipUI.getPinnedLocation();

      _actionPinLocation_Header.setEnabled(false);

      _actionPinLocation_Screen.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.Screen);
      _actionPinLocation_TopLeft.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.TopLeft);
      _actionPinLocation_TopRight.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.TopRight);
      _actionPinLocation_BottomLeft.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.BottomLeft);
      _actionPinLocation_BottomRight.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.BottomRight);
      _actionPinLocation_MouseXPosition.setChecked(pinnedLocation == Pinned_ToolTip_PinLocation.MouseXPosition);

      _actionValue_Header.setEnabled(false);

      _actionValue_Altimeter.setState(
            (_allVisibleValueIds & VALUE_ID_ALTIMETER) > 0,
            _tourData.getAltimeterSerie() != null);

      _actionValue_Altitude.setState( //
            (_allVisibleValueIds & VALUE_ID_ALTITUDE) > 0,
            _tourData.getAltitudeSerie() != null);

      _actionValue_Cadence.setState( //
            (_allVisibleValueIds & VALUE_ID_CADENCE) > 0,
            _tourData.getCadenceSerie() != null);

      _actionValue_ChartZoomFactor.setState( //
            (_allVisibleValueIds & VALUE_ID_CHART_ZOOM_FACTOR) > 0,
            true);

      _actionValue_Distance.setState( //
            (_allVisibleValueIds & VALUE_ID_DISTANCE) > 0,
            _tourData.distanceSerie != null);

      _actionValue_Gears.setState( //
            (_allVisibleValueIds & VALUE_ID_GEARS) > 0,
            _tourData.getGears() != null);

      _actionValue_Gradient.setState( //
            (_allVisibleValueIds & VALUE_ID_GRADIENT) > 0,
            _tourData.getGradientSerie() != null);

      _actionValue_Pace.setState( //
            (_allVisibleValueIds & VALUE_ID_PACE) > 0,
            _tourData.getPaceSerie() != null);

      _actionValue_Power.setState( //
            (_allVisibleValueIds & VALUE_ID_POWER) > 0,
            _tourData.getPowerSerie() != null);

      _actionValue_Pulse.setState( //
            (_allVisibleValueIds & VALUE_ID_PULSE) > 0,
            _tourData.pulseSerie != null);

      _actionValue_Speed.setState( //
            (_allVisibleValueIds & VALUE_ID_SPEED) > 0,
            _tourData.getSpeedSerie() != null);

      _actionValue_Temperature.setState( //
            (_allVisibleValueIds & VALUE_ID_TEMPERATURE) > 0,
            _tourData.temperatureSerie != null);

      _actionValue_TimeDuration.setState( //
            (_allVisibleValueIds & VALUE_ID_TIME_DURATION) > 0,
            _tourData.timeSerie != null);

      _actionValue_TimeOfDay.setState( //
            (_allVisibleValueIds & VALUE_ID_TIME_OF_DAY) > 0,
            _tourData.timeSerie != null);

      _actionValue_TimeSlices.setState( //
            (_allVisibleValueIds & VALUE_ID_TIME_SLICES) > 0,
            true);

      _actionValue_TourCompareResult.setState( //
            (_allVisibleValueIds & VALUE_ID_TOUR_COMPARE_RESULT) > 0,
            _tourData.tourCompareSerie != null && _tourData.tourCompareSerie.length > 0);

      _actionValue_RunDyn_StanceTime.setState( //
            (_allVisibleValueIds & VALUE_ID_RUN_DYN_STANCE_TIME) > 0,
            _tourData.getRunDyn_StanceTime() != null);

      _actionValue_RunDyn_StanceTimeBalance.setState( //
            (_allVisibleValueIds & VALUE_ID_RUN_DYN_STANCE_TIME_BALANCED) > 0,
            _tourData.getRunDyn_StanceTimeBalance() != null);

      _actionValue_RunDyn_StepLength.setState( //
            (_allVisibleValueIds & VALUE_ID_RUN_DYN_STEP_LENGTH) > 0,
            _tourData.getRunDyn_StepLength() != null);

      _actionValue_RunDyn_VerticalOscillation.setState( //
            (_allVisibleValueIds & VALUE_ID_RUN_DYN_VERTICAL_OSCILLATION) > 0,
            _tourData.getRunDyn_VerticalOscillation() != null);

      _actionValue_RunDyn_VerticalRatio.setState( //
            (_allVisibleValueIds & VALUE_ID_RUN_DYN_VERTICAL_RATIO) > 0,
            _tourData.getRunDyn_VerticalRatio() != null);
   }

   private Menu getMenu(final Control parent) {

      // recreate menu each time
      if (_menu != null) {
         _menu.dispose();
      }

      // !!! actions must be checked before they are added otherwise they are not checked
      enableActions();

      _menu = new Menu(parent);

//      (new Separator()).fill(_menu, -1);
      addItem(_actionValue_Header);

      addItem(_actionValue_TimeSlices);
      addItem(_actionValue_TimeDuration);
      addItem(_actionValue_TimeOfDay);
      addItem(_actionValue_Distance);
      addItem(_actionValue_Altitude);
      addItem(_actionValue_Pulse);
      addItem(_actionValue_Speed);
      addItem(_actionValue_Pace);
      addItem(_actionValue_Power);
      addItem(_actionValue_Temperature);
      addItem(_actionValue_Gradient);
      addItem(_actionValue_Altimeter);
      addItem(_actionValue_Cadence);
      addItem(_actionValue_Gears);
      addItem(_actionValue_RunDyn_StanceTime);
      addItem(_actionValue_RunDyn_StanceTimeBalance);
      addItem(_actionValue_RunDyn_StepLength);
      addItem(_actionValue_RunDyn_VerticalOscillation);
      addItem(_actionValue_RunDyn_VerticalRatio);
      addItem(_actionValue_TourCompareResult);
      addItem(_actionValue_ChartZoomFactor);
      addItem(_actionCloseTTContextMenu);

      (new Separator()).fill(_menu, -1);
      addItem(_actionPinLocation_Header);
      addItem(_actionPinLocation_MouseXPosition);
      addItem(_actionPinLocation_TopLeft);
      addItem(_actionPinLocation_TopRight);
      addItem(_actionPinLocation_BottomLeft);
      addItem(_actionPinLocation_BottomRight);
      addItem(_actionPinLocation_Screen);

      (new Separator()).fill(_menu, -1);

      // show the other orientation
      if (_isHorizontal) {
         addItem(_actionVerticalOrientation);
      } else {
         addItem(_actionHorizontalOrientation);
      }
      addItem(_actionSetDefaults);
      addItem(_actionHideToolTip);

      return _menu;
   }

   void hideContextMenu() {

      if (_menu != null && !_menu.isDisposed()) {
         _menu.setVisible(false);
      }
   }

   /**
    * Open tooltip context menu.
    *
    * @param event
    * @param tourData
    * @param allVisibleValueIds
    * @param isHorizontal
    */
   void openToolTipMenu(final Event event,
                        final TourData tourData,
                        final long allVisibleValueIds,
                        final boolean isHorizontal) {

      _tourData = tourData;

      _allVisibleValueIds = allVisibleValueIds;
      _isHorizontal = isHorizontal;

      // open and position drop down menu below the action button
      final Widget item = event.widget;
      if (item instanceof ToolItem) {
         openToolTipMenu10Reopen((ToolItem) item);
      }
   }

   private void openToolTipMenu10Reopen(final ToolItem toolItem) {

      _menuParentItem = toolItem;

      final ToolBar menuParentControl = _menuParentItem.getParent();
      final Rectangle toolItemBounds = _menuParentItem.getBounds();

      Point topLeft = new Point(toolItemBounds.x, toolItemBounds.y + toolItemBounds.height);
      topLeft = menuParentControl.toDisplay(topLeft);

      final Menu menu = getMenu(menuParentControl);
      menu.setLocation(topLeft.x, topLeft.y);
      menu.setVisible(true);
   }
}
