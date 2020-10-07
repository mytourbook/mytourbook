/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class StatisticTooltipUI_Summary {

   private static final String IMAGE_APP_CLOSE          = net.tourbook.common.Messages.Image__App_Close;
   private static final String APP_ACTION_CLOSE_TOOLTIP = net.tourbook.common.Messages.App_Action_Close_Tooltip;

   private static final int    VERTICAL_LINE_SPACE      = 8;

   private static final int    SHELL_MARGIN             = 5;

   /*
    * Tooltip context
    */
   private IToolTipProvider _toolTipProvider;
   private TourData_Month   _tourData_Month;
   private String           _toolTipTitle;
   private int              _tourType_SerieIndex;
   private int              _valueIndex;

   private boolean          _isTourTypeImageAvailable;
   private long             _tourTypeId;
   private String           _tourTypeName;

   /*
    * Actions
    */
   private ActionCloseTooltip             _actionCloseTooltip;
   private Action_ToolTip_EditPreferences _actionPrefDialog;

   /*
    * UI resources
    */
   private Color _bgColor;
   private Color _fgColor;

   private Font  FONT_BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Label     _lblTitle;
   private CLabel    _lblTourType_Image;

   private Label     _lblDeviceTime_Elapsed;
   private Label     _lblDeviceTime_Elapsed_Total;
   private Label     _lblDeviceTime_Paused;
   private Label     _lblDeviceTime_Paused_Total;
   private Label     _lblDeviceTime_Recorded;
   private Label     _lblDeviceTime_Recorded_Total;

   private Label     _lblComputedTime_Break;
   private Label     _lblComputedTime_Break_Total;
   private Label     _lblComputedTime_Moving;
   private Label     _lblComputedTime_Moving_Total;

   private Label     _lblDistance;
   private Label     _lblDistance_Total;
   private Label     _lblDistance_Unit;
   private Label     _lblElevationUp;
   private Label     _lblElevationUp_Total;
   private Label     _lblElevationUp_Unit;
   private Label     _lblNumberOfTours;
   private Label     _lblNumberOfTours_Total;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, Action.AS_PUSH_BUTTON);

         setToolTipText(APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getImageDescriptor(IMAGE_APP_CLOSE));
      }

      @Override
      public void run() {
         _toolTipProvider.hideToolTip();
      }
   }

   private float computeTotals(final float[][] allDataSeries, final int valueIndex) {

      float summary = 0;

      for (final float[] dataSerie : allDataSeries) {
         summary += dataSerie[valueIndex];
      }

      return summary;
   }

   private int computeTotals(final int[][] allDataSeries, final int valueIndex) {

      int summary = 0;

      for (final int[] dataSerie : allDataSeries) {
         summary += dataSerie[valueIndex];
      }

      return summary;
   }

   private void createActions() {

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_toolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID);

      _actionCloseTooltip = new ActionCloseTooltip();
   }

   void createContentArea(final Composite parent,
                          final IToolTipProvider toolTipProvider,
                          final TourData_Month tourData_Month,
                          final String toolTipTitle,
                          final int serieIndex,
                          final int valueIndex) {

      _toolTipProvider = toolTipProvider;
      _tourData_Month = tourData_Month;
      _toolTipTitle = toolTipTitle;
      _tourType_SerieIndex = serieIndex;
      _valueIndex = valueIndex;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      // setup tour type
      _tourTypeId = _tourData_Month.typeIds[serieIndex][valueIndex];
      _tourTypeName = TourDatabase.getTourTypeName(_tourTypeId);
      _isTourTypeImageAvailable = _tourTypeId >= 0;

      createActions();
      createUI(parent);

      updateUI();
      updateUI_Layout();

      enableControls();
   }

   private void createUI(final Composite parent) {

      final Point defaultSpacing = LayoutConstants.getSpacing();

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(_fgColor);
      shellContainer.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
//      shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         _ttContainer.setForeground(_fgColor);
         _ttContainer.setBackground(_bgColor);
         GridLayoutFactory
               .fillDefaults() //
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(_ttContainer);
//         _ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_10_Header(_ttContainer);

            final Composite container = new Composite(_ttContainer, SWT.NONE);
            container.setForeground(_fgColor);
            container.setBackground(_bgColor);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults()
                  .numColumns(4)

                  // remove vertical spacing
                  .spacing(defaultSpacing.x, 0)
                  .applyTo(container);
//            container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
            {

               createUI_18_ColumnHeader(container);

               createUI_Spacer_4_Columns(container);
               createUI_20_Time(container);

               createUI_Spacer_4_Columns(container);
               createUI_22_Distance_Elevation(container);

               createUI_Spacer_4_Columns(container);
               createUI_24_Tours(container);
            }
         }
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final int numColumns = _isTourTypeImageAvailable ? 3 : 2;

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(numColumns)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         /*
          * Tour type image
          */
         if (_isTourTypeImageAvailable) {

            _lblTourType_Image = new CLabel(container, SWT.NONE);
            GridDataFactory.swtDefaults()
                  .align(SWT.BEGINNING, SWT.BEGINNING)
                  .applyTo(_lblTourType_Image);
            _lblTourType_Image.setForeground(_fgColor);
            _lblTourType_Image.setBackground(_bgColor);
         }

         /*
          * Title
          */
         _lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
         _lblTitle.setForeground(_fgColor);
         _lblTitle.setBackground(_bgColor);
         MTFont.setBannerFont(_lblTitle);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblTitle);

         /*
          * Action toolbar in the top right corner
          */
         createUI_12_Header_Toolbar(container);
      }
   }

   private void createUI_12_Header_Toolbar(final Composite container) {

      /*
       * Create toolbar
       */
      final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
      GridDataFactory.fillDefaults().applyTo(toolbar);
      toolbar.setForeground(_fgColor);
      toolbar.setBackground(_bgColor);
//      toolbar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      /*
       * Fill toolbar
       */

      tbm.add(_actionPrefDialog);

      /**
       * The close action is ALWAYS visible, sometimes there is a bug that the tooltip do not
       * automatically close when hovering out.
       */
      tbm.add(_actionCloseTooltip);

      tbm.update(true);
   }

   private void createUI_18_ColumnHeader(final Composite parent) {

      /*
       * 1st row
       */

      // column 1
      createUI_Label(parent, UI.EMPTY_STRING);

      // column 2
      final Label lblBar = createUI_Label(parent, "Hovered", SWT.TRAIL);
      lblBar.setFont(FONT_BOLD);

      // column 3
      final Label lblTotal = createUI_Label(parent, "Month", SWT.TRAIL);
      lblTotal.setFont(FONT_BOLD);
      GridDataFactory.fillDefaults().indent(30, 0).applyTo(lblTotal);

      // column 4
      createUI_Label(parent, UI.EMPTY_STRING);

      /*
       * 2nd row
       */

      // column 1
      createUI_Label(parent, UI.EMPTY_STRING);

      // column 2
      final Label lblBar2 = createUI_Label(parent, "Tour Type", SWT.TRAIL);
      lblBar2.setFont(FONT_BOLD);

      // column 3
      final Label lblTotal2 = createUI_Label(parent, "Total", SWT.TRAIL);
      lblTotal2.setFont(FONT_BOLD);
      GridDataFactory.fillDefaults().indent(30, 0).applyTo(lblTotal2);

      // column 4
      createUI_Label(parent, UI.EMPTY_STRING);
   }

   private void createUI_20_Time(final Composite parent) {

      {
         /*
          * Device time: Elapsed
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_ElapsedTime);

         _lblDeviceTime_Elapsed = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Elapsed_Total = createUI_LabelValue(parent, SWT.TRAIL);
         final Label lblDeviceTime_Elapsed_Hour = createUI_Label(parent, Messages.Tour_Tooltip_Label_Hour);

         // force this column to take the rest of the space
         GridDataFactory.fillDefaults().grab(true, false).applyTo(lblDeviceTime_Elapsed_Hour);
      }

      {
         /*
          * Device time: Recorded
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_RecordedTime);

         _lblDeviceTime_Recorded = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Recorded_Total = createUI_LabelValue(parent, SWT.TRAIL);
         createUI_Label(parent, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Device time: Paused
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_PausedTime);

         _lblDeviceTime_Paused = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Paused_Total = createUI_LabelValue(parent, SWT.TRAIL);
         createUI_Label(parent, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Computed time: Moving
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_MovingTime);

         _lblComputedTime_Moving = createUI_LabelValue(parent, SWT.TRAIL);
         _lblComputedTime_Moving_Total = createUI_LabelValue(parent, SWT.TRAIL);
         createUI_Label(parent, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Computed time: Break
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_BreakTime);

         _lblComputedTime_Break = createUI_LabelValue(parent, SWT.TRAIL);
         _lblComputedTime_Break_Total = createUI_LabelValue(parent, SWT.TRAIL);
         createUI_Label(parent, Messages.Tour_Tooltip_Label_Hour);
      }

   }

   private void createUI_22_Distance_Elevation(final Composite container) {

      {
         /*
          * Distance
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_Distance);

         _lblDistance = createUI_LabelValue(container, SWT.TRAIL);
         _lblDistance_Total = createUI_LabelValue(container, SWT.TRAIL);
         _lblDistance_Unit = createUI_LabelValue(container, SWT.LEAD);
      }

      {
         /*
          * Elevation up
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeUp);

         _lblElevationUp = createUI_LabelValue(container, SWT.TRAIL);
         _lblElevationUp_Total = createUI_LabelValue(container, SWT.TRAIL);
         _lblElevationUp_Unit = createUI_LabelValue(container, SWT.LEAD);
      }
   }

   private void createUI_24_Tours(final Composite container) {

      /*
       * Number of tours
       */
      createUI_Label(container, Messages.Statistic_Tooltip_Label_NumberOfTours);

      _lblNumberOfTours = createUI_LabelValue(container, SWT.TRAIL);
      _lblNumberOfTours_Total = createUI_LabelValue(container, SWT.TRAIL);
      createUI_LabelValue(container, SWT.LEAD);

   }

   private Label createUI_Label(final Composite parent, final String labelText) {

      final Label label = new Label(parent, SWT.NONE);

      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Label createUI_Label(final Composite parent, final String labelText, final int style) {

      final Label label = new Label(parent, style);

      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      GridDataFactory.fillDefaults().applyTo(label);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Label createUI_LabelValue(final Composite parent, final int style) {

      final Label label = new Label(parent, style);

      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      GridDataFactory.fillDefaults().applyTo(label);

      return label;
   }

   public Composite createUI_NoData(final Composite parent) {

      final Display display = parent.getDisplay();

      final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(fgColor);
      shellContainer.setBackground(bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {

         final Composite container = new Composite(shellContainer, SWT.NONE);
         container.setForeground(fgColor);
         container.setBackground(bgColor);
         GridLayoutFactory
               .fillDefaults()//
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(container);
         {
            final Label label = new Label(container, SWT.NONE);
//            label.setText(_noTourTooltip);
            label.setForeground(fgColor);
            label.setBackground(bgColor);
         }
      }

      return shellContainer;
   }

   /**
    * Spacer for 4 columns
    *
    * @param container
    */
   private void createUI_Spacer_4_Columns(final Composite container) {

      // spacer
      final Label label = createUI_Label(container, null);
      GridDataFactory.fillDefaults()
            .span(4, 1)
            .hint(1, VERTICAL_LINE_SPACE)
            .applyTo(label);
   }

   public void dispose() {

   }

   private void enableControls() {

   }

   private void updateUI() {

// SET_FORMATTING_OFF

      final long deviceTime_Elapsed          = _tourData_Month.elapsedTime[_tourType_SerieIndex][_valueIndex];
      final long deviceTime_Elapsed_Total    = computeTotals(_tourData_Month.elapsedTime,_valueIndex);
      final long deviceTime_Recorded         = _tourData_Month.recordedTime[_tourType_SerieIndex][_valueIndex];
      final long deviceTime_Recorded_Total   = computeTotals(_tourData_Month.recordedTime,_valueIndex);
      final long deviceTime_Paused           = _tourData_Month.pausedTime[_tourType_SerieIndex][_valueIndex];
      final long deviceTime_Paused_Total     = computeTotals(_tourData_Month.pausedTime,_valueIndex);
      final long computedTime_Moving         = _tourData_Month.movingTime[_tourType_SerieIndex][_valueIndex];
      final long computedTime_Moving_Total   = computeTotals(_tourData_Month.movingTime,_valueIndex);
      final long computedTime_Break          = deviceTime_Elapsed - computedTime_Moving;
      final long computedTime_Break_Total    = deviceTime_Elapsed_Total - computedTime_Moving_Total;

      final float distance                   = _tourData_Month.distanceHigh[_tourType_SerieIndex][_valueIndex];
      final float distance_Total             = computeTotals(_tourData_Month.distanceHigh,_valueIndex);
      final float elevationUp                = _tourData_Month.elevationUp_High[_tourType_SerieIndex][_valueIndex];
      final float elevationUp_Total          = computeTotals(_tourData_Month.elevationUp_High,_valueIndex);

      final int numTours                     = (int) (_tourData_Month.numToursHigh[_tourType_SerieIndex][_valueIndex] + 0.5f);
      final int numTours_Total               = (int) (computeTotals(_tourData_Month.numToursHigh,_valueIndex) + 0.5f);

// SET_FORMATTING_ON

      // tour type image
      if (_lblTourType_Image != null && _lblTourType_Image.isDisposed() == false) {

         if (_tourTypeId < 0) {
            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
         } else {
            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(_tourTypeId));
         }
         _lblTourType_Image.setToolTipText(_tourTypeName);
      }

      _lblTitle.setText(_toolTipTitle);

// SET_FORMATTING_OFF

      _lblDeviceTime_Elapsed        .setText(FormatManager.formatElapsedTime(deviceTime_Elapsed));
      _lblDeviceTime_Elapsed_Total  .setText(FormatManager.formatElapsedTime(deviceTime_Elapsed_Total));
      _lblDeviceTime_Recorded       .setText(FormatManager.formatMovingTime(deviceTime_Recorded));
      _lblDeviceTime_Recorded_Total .setText(FormatManager.formatMovingTime(deviceTime_Recorded_Total));
      _lblDeviceTime_Paused         .setText(FormatManager.formatPausedTime(deviceTime_Paused));
      _lblDeviceTime_Paused_Total   .setText(FormatManager.formatPausedTime(deviceTime_Paused_Total));
      _lblComputedTime_Moving       .setText(FormatManager.formatMovingTime(computedTime_Moving));
      _lblComputedTime_Moving_Total .setText(FormatManager.formatMovingTime(computedTime_Moving_Total));
      _lblComputedTime_Break        .setText(FormatManager.formatPausedTime(computedTime_Break));
      _lblComputedTime_Break_Total  .setText(FormatManager.formatPausedTime(computedTime_Break_Total));

      _lblDistance                  .setText(FormatManager.formatDistance(distance / 1000.0));
      _lblDistance_Total            .setText(FormatManager.formatDistance(distance_Total / 1000.0));
      _lblDistance_Unit             .setText(UI.UNIT_LABEL_DISTANCE);

      _lblElevationUp               .setText(Integer.toString((int) (elevationUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
      _lblElevationUp_Total         .setText(Integer.toString((int) (elevationUp_Total / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
      _lblElevationUp_Unit          .setText(UI.UNIT_LABEL_ALTITUDE);

      _lblNumberOfTours             .setText(Integer.toString(numTours));
      _lblNumberOfTours_Total       .setText(Integer.toString(numTours_Total));

// SET_FORMATTING_ON
   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }
}
