/*******************************************************************************
 * Copyright (C) 2020, 2023 Wolfgang Schramm and Contributors
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

import net.tourbook.OtherMessages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormatter_Number_1_0;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
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

public class StatisticTooltipUI_CategorizedData {

   private static final String          NUMBERS_UNIT        = net.tourbook.statistics.Messages.NUMBERS_UNIT;

   private static final int             VERTICAL_LINE_SPACE = 8;
   private static final int             SHELL_MARGIN        = 5;

   private static final IValueFormatter VALUE_FORMATTER_1_0 = new ValueFormatter_Number_1_0();

   private static final int             _columnSpacing      = 20;
   private static final GridDataFactory _columnGridData     = GridDataFactory.fillDefaults().grab(true, false).indent(_columnSpacing, 0);

   /*
    * Tooltip context
    */
   private String                   _summaryColumn_HeaderTitle;
   private IToolTipProvider         _toolTipProvider;
   private TourStatisticData_Common _tourData_Common;
   private String                   _toolTip_Title;
   private String                   _toolTip_SubTitle;
   private int                      _serieIndex;
   private int                      _valueIndex;

   private boolean                  _isTourTypeImageAvailable;
   private long                     _tourTypeId;
   private String                   _tourTypeName;

   private boolean                  _isShowPercentage;
   private boolean                  _isShowSummary;

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

   private Label     _lblColumnHeader_TourType;
   private Label     _lblSubTitle;
   private Label     _lblTitle;
   private CLabel    _lblTourType_Image;

   private Label     _lblDeviceTime_Elapsed;
   private Label     _lblDeviceTime_Elapsed_Unit;
   private Label     _lblDeviceTime_Elapsed_Summary;
   private Label     _lblDeviceTime_Elapsed_Summary_Unit;
   private Label     _lblDeviceTime_Elapsed_Percentage;

   private Label     _lblDeviceTime_Recorded;
   private Label     _lblDeviceTime_Recorded_Unit;
   private Label     _lblDeviceTime_Recorded_Summary;
   private Label     _lblDeviceTime_Recorded_Summary_Unit;
   private Label     _lblDeviceTime_Recorded_Percentage;

   private Label     _lblDeviceTime_Paused;
   private Label     _lblDeviceTime_Paused_Unit;
   private Label     _lblDeviceTime_Paused_Summary;
   private Label     _lblDeviceTime_Paused_Summary_Unit;
   private Label     _lblDeviceTime_Paused_Percentage;

   private Label     _lblComputedTime_Moving;
   private Label     _lblComputedTime_Moving_Unit;
   private Label     _lblComputedTime_Moving_Summary;
   private Label     _lblComputedTime_Moving_Summary_Unit;
   private Label     _lblComputedTime_Moving_Percentage;

   private Label     _lblComputedTime_Break;
   private Label     _lblComputedTime_Break_Unit;
   private Label     _lblComputedTime_Break_Summary;
   private Label     _lblComputedTime_Break_Summary_Unit;
   private Label     _lblComputedTime_Break_Percentage;

   private Label     _lblDistance;
   private Label     _lblDistance_Unit;
   private Label     _lblDistance_Summary;
   private Label     _lblDistance_Summary_Unit;
   private Label     _lblDistance_Percentage;

   private Label     _lblElevationUp;
   private Label     _lblElevationUp_Unit;
   private Label     _lblElevationUp_Summary;
   private Label     _lblElevationUp_Summary_Unit;
   private Label     _lblElevationUp_Percentage;

   private Label     _lblElevationDown;
   private Label     _lblElevationDown_Unit;
   private Label     _lblElevationDown_Summary;
   private Label     _lblElevationDown_Summary_Unit;
   private Label     _lblElevationDown_Percentage;

   private Label     _lblNumberOfTours;
   private Label     _lblNumberOfTours_Percentage;
   private Label     _lblNumberOfTours_Summary;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, IAction.AS_PUSH_BUTTON);

         setToolTipText(OtherMessages.APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         _toolTipProvider.hideToolTip();
      }
   }

   private float computeSummary(final float[][] allDataSeries, final int valueIndex) {

      float summary = 0;

      for (final float[] dataSerie : allDataSeries) {
         summary += dataSerie[valueIndex];
      }

      return summary;
   }

   private int computeSummary(final int[][] allDataSeries, final int valueIndex) {

      int summary = 0;

      for (final int[] dataSerie : allDataSeries) {
         summary += dataSerie[valueIndex];
      }

      return summary;
   }

   private void createActions() {

      final Integer selectedTabFolder = Integer.valueOf(1);

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_toolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID,
            selectedTabFolder);

      _actionCloseTooltip = new ActionCloseTooltip();
   }

   /**
    * @param parent
    * @param toolTipProvider
    * @param tourData_Month
    * @param serieIndex
    * @param valueIndex
    * @param toolTip_Title
    * @param toolTip_SubTitle
    * @param summaryColumn_HeaderTitle
    * @param isShowSummary
    * @param isShowPercentage
    */
   void createContentArea(final Composite parent,
                          final IToolTipProvider toolTipProvider,
                          final TourStatisticData_Common tourData_Month,
                          final int serieIndex,
                          final int valueIndex,
                          final String toolTip_Title,
                          final String toolTip_SubTitle,
                          final String summaryColumn_HeaderTitle,
                          final boolean isShowSummary,
                          final boolean isShowPercentage) {

      _toolTipProvider = toolTipProvider;
      _tourData_Common = tourData_Month;
      _serieIndex = serieIndex;
      _valueIndex = valueIndex;

      _toolTip_Title = toolTip_Title;
      _toolTip_SubTitle = toolTip_SubTitle;
      _summaryColumn_HeaderTitle = summaryColumn_HeaderTitle;

      _isShowPercentage = isShowPercentage;
      _isShowSummary = isShowSummary;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      // setup tour type
      _tourTypeId = _tourData_Common.typeIds_Resorted[serieIndex][valueIndex];
      _tourTypeName = TourDatabase.getTourTypeName(_tourTypeId);
      _isTourTypeImageAvailable = _tourTypeId >= 0;

      initUI();

      createActions();
      createUI(parent);

      updateUI();
      updateUI_Layout();

      enableControls();
   }

   private void createUI(final Composite parent) {

      final Point defaultSpacing = LayoutConstants.getSpacing();

      int numColumns = 3;

      if (_isShowPercentage) {
         numColumns++;
      }
      if (_isShowSummary) {
         numColumns += 2;
      }

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
                  .numColumns(numColumns)

                  // remove vertical spacing
                  .spacing(defaultSpacing.x, 0)
                  .applyTo(container);
//            container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
            {

               createUI_18_ColumnHeader(container);

               createUI_Spacer_Columns(container, numColumns);
               createUI_20_Time(container);

               createUI_Spacer_Columns(container, numColumns);
               createUI_22_Other(container);

               createUI_Spacer_Columns(container, numColumns);
               createUI_24_NumTours(container);
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

      if (_toolTip_SubTitle != null) {

         _lblSubTitle = new Label(parent, SWT.LEAD | SWT.WRAP);
         _lblSubTitle.setForeground(_fgColor);
         _lblSubTitle.setBackground(_bgColor);
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

      {
         // column 1+2
         _lblColumnHeader_TourType = createUI_Label(parent, UI.EMPTY_STRING);
         _lblColumnHeader_TourType.setFont(FONT_BOLD);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_lblColumnHeader_TourType);
      }
      {
         // column 3: unit
         createUI_Label(parent, UI.EMPTY_STRING);
      }
      if (_isShowPercentage) {

         {
            // column 4: %
            final Label lblPercentage = createUI_Label(parent, Messages.Statistic_Tooltip_Label_ColumnHeader_Percentage, SWT.TRAIL);
            lblPercentage.setToolTipText(Messages.Statistic_Tooltip_Label_ColumnHeader_Percentage_Tooltip);
            lblPercentage.setFont(FONT_BOLD);
            GridDataFactory.fillDefaults().indent(_columnSpacing, 0).applyTo(lblPercentage);
         }
      }

      if (_isShowSummary) {

         {
            // column 5: Summary
            final Label lblTotal = createUI_Label(parent, _summaryColumn_HeaderTitle, SWT.LEAD);
            lblTotal.setFont(FONT_BOLD);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .indent(_columnSpacing, 0)
                  .applyTo(lblTotal);
         }
         {
            // column 6: Summary unit
            final Label lblSummaryUnit = createUI_Label(parent, UI.EMPTY_STRING);

            if (_isShowSummary == false) {
               lblSummaryUnit.setVisible(false);
            }
         }
      }
   }

   private void createUI_20_Time(final Composite parent) {

      {
         /*
          * Device time: Elapsed
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_ElapsedTime);

         _lblDeviceTime_Elapsed = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Elapsed_Unit = createUI_LabelValue(parent, SWT.LEAD);

         if (_isShowPercentage) {
            _lblDeviceTime_Elapsed_Percentage = createUI_LabelValue(parent, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDeviceTime_Elapsed_Summary = createUI_LabelValue(parent, SWT.TRAIL);
            _lblDeviceTime_Elapsed_Summary_Unit = createUI_LabelValue(parent, SWT.LEAD);
            _columnGridData.applyTo(_lblDeviceTime_Elapsed_Summary);
         }
      }
      {
         /*
          * Device time: Recorded
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_RecordedTime);

         _lblDeviceTime_Recorded = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Recorded_Unit = createUI_LabelValue(parent, SWT.LEAD);

         if (_isShowPercentage) {
            _lblDeviceTime_Recorded_Percentage = createUI_LabelValue(parent, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDeviceTime_Recorded_Summary = createUI_LabelValue(parent, SWT.TRAIL);
            _lblDeviceTime_Recorded_Summary_Unit = createUI_LabelValue(parent, SWT.LEAD);
            _columnGridData.applyTo(_lblDeviceTime_Recorded_Summary);
         }
      }
      {
         /*
          * Device time: Paused
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_PausedTime);

         _lblDeviceTime_Paused = createUI_LabelValue(parent, SWT.TRAIL);
         _lblDeviceTime_Paused_Unit = createUI_LabelValue(parent, SWT.LEAD);

         if (_isShowPercentage) {
            _lblDeviceTime_Paused_Percentage = createUI_LabelValue(parent, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDeviceTime_Paused_Summary = createUI_LabelValue(parent, SWT.TRAIL);
            _lblDeviceTime_Paused_Summary_Unit = createUI_LabelValue(parent, SWT.LEAD);
            _columnGridData.applyTo(_lblDeviceTime_Paused_Summary);
         }
      }
      {
         /*
          * Computed time: Moving
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_MovingTime);

         _lblComputedTime_Moving = createUI_LabelValue(parent, SWT.TRAIL);
         _lblComputedTime_Moving_Unit = createUI_LabelValue(parent, SWT.LEAD);

         if (_isShowPercentage) {
            _lblComputedTime_Moving_Percentage = createUI_LabelValue(parent, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblComputedTime_Moving_Summary = createUI_LabelValue(parent, SWT.TRAIL);
            _lblComputedTime_Moving_Summary_Unit = createUI_LabelValue(parent, SWT.LEAD);
            _columnGridData.applyTo(_lblComputedTime_Moving_Summary);
         }
      }
      {
         /*
          * Computed time: Break
          */
         createUI_Label(parent, Messages.Tour_Tooltip_Label_BreakTime);

         _lblComputedTime_Break = createUI_LabelValue(parent, SWT.TRAIL);
         _lblComputedTime_Break_Unit = createUI_LabelValue(parent, SWT.LEAD);

         if (_isShowPercentage) {
            _lblComputedTime_Break_Percentage = createUI_LabelValue(parent, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblComputedTime_Break_Summary = createUI_LabelValue(parent, SWT.TRAIL);
            _lblComputedTime_Break_Summary_Unit = createUI_LabelValue(parent, SWT.LEAD);
            _columnGridData.applyTo(_lblComputedTime_Break_Summary);
         }
      }
   }

   private void createUI_22_Other(final Composite container) {

      {
         /*
          * Distance
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_Distance);

         _lblDistance = createUI_LabelValue(container, SWT.TRAIL);
         _lblDistance_Unit = createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblDistance_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDistance_Summary = createUI_LabelValue(container, SWT.TRAIL);
            _lblDistance_Summary_Unit = createUI_LabelValue(container, SWT.LEAD);
            _columnGridData.applyTo(_lblDistance_Summary);
         }
      }
      {
         /*
          * Elevation gain
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeUp);

         _lblElevationUp = createUI_LabelValue(container, SWT.TRAIL);
         _lblElevationUp_Unit = createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblElevationUp_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblElevationUp_Summary = createUI_LabelValue(container, SWT.TRAIL);
            _lblElevationUp_Summary_Unit = createUI_LabelValue(container, SWT.LEAD);
            _columnGridData.applyTo(_lblElevationUp_Summary);
         }
      }
      {
         /*
          * Elevation loss
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeDown);

         _lblElevationDown = createUI_LabelValue(container, SWT.TRAIL);
         _lblElevationDown_Unit = createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblElevationDown_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblElevationDown_Summary = createUI_LabelValue(container, SWT.TRAIL);
            _lblElevationDown_Summary_Unit = createUI_LabelValue(container, SWT.LEAD);
            _columnGridData.applyTo(_lblElevationDown_Summary);
         }
      }
   }

   private void createUI_24_NumTours(final Composite container) {

      {
         /*
          * Number of tours
          */
         createUI_Label(container, Messages.Statistic_Tooltip_Label_NumberOfTours);

         _lblNumberOfTours = createUI_LabelValue(container, SWT.TRAIL);
         createUI_Label(container, NUMBERS_UNIT, SWT.LEAD);

         if (_isShowPercentage) {
            _lblNumberOfTours_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblNumberOfTours_Summary = createUI_LabelValue(container, SWT.TRAIL);
            createUI_Label(container, NUMBERS_UNIT, SWT.LEAD);
         }
      }
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
    * @param numColumns
    */
   private void createUI_Spacer_Columns(final Composite container, final int numColumns) {

      // spacer
      final Label label = createUI_Label(container, null);
      GridDataFactory.fillDefaults()
            .span(numColumns, 1)
            .hint(1, VERTICAL_LINE_SPACE)
            .applyTo(label);
   }

   public void dispose() {

   }

   private void enableControls() {

   }

   private void initUI() {

   }

   private void updateUI() {

      // tour type image
      if (_lblTourType_Image != null && _lblTourType_Image.isDisposed() == false) {

         if (_tourTypeId < 0) {
            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
         } else {
            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(_tourTypeId));
         }
         _lblTourType_Image.setToolTipText(_tourTypeName);
      }

      _lblTitle.setText(_toolTip_Title);
      _lblColumnHeader_TourType.setText(_tourTypeName);

      if (_toolTip_SubTitle != null) {
         _lblSubTitle.setText(_toolTip_SubTitle);
      }

// SET_FORMATTING_OFF

      final long deviceTime_Elapsed                      = _tourData_Common.elapsedTime_Resorted    [_serieIndex][_valueIndex];
      final long deviceTime_Recorded                     = _tourData_Common.recordedTime_Resorted   [_serieIndex][_valueIndex];
      final long deviceTime_Paused                       = _tourData_Common.pausedTime_Resorted     [_serieIndex][_valueIndex];
      final long computedTime_Moving                     = _tourData_Common.movingTime_Resorted     [_serieIndex][_valueIndex];
      final long computedTime_Break                      = deviceTime_Elapsed - computedTime_Moving;

      final long deviceTime_Elapsed_Summary              = computeSummary(_tourData_Common.elapsedTime_Resorted,    _valueIndex);
      final long deviceTime_Recorded_Summary             = computeSummary(_tourData_Common.recordedTime_Resorted,   _valueIndex);
      final long deviceTime_Paused_Summary               = computeSummary(_tourData_Common.pausedTime_Resorted,     _valueIndex);
      final long computedTime_Moving_Summary             = computeSummary(_tourData_Common.movingTime_Resorted,     _valueIndex);
      final long computedTime_Break_Summary              = deviceTime_Elapsed_Summary - computedTime_Moving_Summary;

      final float deviceTime_Elapsed_Percentage          = deviceTime_Elapsed_Summary  == 0 ? 0 : (float) deviceTime_Elapsed  / deviceTime_Elapsed_Summary  * 100;
      final float deviceTime_Recorded_Percentage         = deviceTime_Recorded_Summary == 0 ? 0 : (float) deviceTime_Recorded / deviceTime_Recorded_Summary * 100;
      final float deviceTime_Paused_Percentage           = deviceTime_Paused_Summary   == 0 ? 0 : (float) deviceTime_Paused   / deviceTime_Paused_Summary   * 100;
      final float computedTime_Moving_Percentage         = computedTime_Moving_Summary == 0 ? 0 : (float) computedTime_Moving / computedTime_Moving_Summary * 100;
      final float computedTime_Break_Percentage          = computedTime_Break_Summary  == 0 ? 0 : (float) computedTime_Break  / computedTime_Break_Summary  * 100;

      final float distance                               = _tourData_Common.distance_High_Resorted       [_serieIndex][_valueIndex];
      final float numTours                               = _tourData_Common.numTours_High_Resorted       [_serieIndex][_valueIndex];

      final float elevationUp                            = _tourData_Common.elevationUp_High_Resorted    [_serieIndex][_valueIndex];
      final float elevationDown                          = _tourData_Common.elevationDown_High_Resorted  [_serieIndex][_valueIndex];

      final float distance_Summary                       = computeSummary(_tourData_Common.distance_High_Resorted,      _valueIndex);
      final float numTours_Summary                       = computeSummary(_tourData_Common.numTours_High_Resorted,      _valueIndex);

      final float elevationUp_Summary                    = computeSummary(_tourData_Common.elevationUp_High_Resorted,   _valueIndex);
      final float elevationDown_Summary                  = computeSummary(_tourData_Common.elevationDown_High_Resorted, _valueIndex);

      final float distance_Percentage                    = distance_Summary    == 0 ? 0 : distance    / distance_Summary      * 100;
      final float numTours_Percentage                    = numTours_Summary    == 0 ? 0 : numTours    / numTours_Summary      * 100;

      final float elevationUp_Percentage                 = elevationUp_Summary   == 0 ? 0 : elevationUp   / elevationUp_Summary     * 100;
      final float elevationDown_Percentage               = elevationDown_Summary == 0 ? 0 : elevationDown / elevationDown_Summary   * 100;

      final String deviceTime_Elapsed_Percentage_Text    = deviceTime_Elapsed_Percentage  == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(deviceTime_Elapsed_Percentage);
      final String deviceTime_Recorded_Percentage_Text   = deviceTime_Recorded_Percentage == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(deviceTime_Recorded_Percentage);
      final String deviceTime_Paused_Percentage_Text     = deviceTime_Paused_Percentage   == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(deviceTime_Paused_Percentage);
      final String computedTime_Moving_Percentage_Text   = computedTime_Moving_Percentage == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(computedTime_Moving_Percentage);
      final String computedTime_Break_Percentage_Text    = computedTime_Break_Percentage  == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(computedTime_Break_Percentage);

      final String distance_Percentage_Text              = distance_Percentage            == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(distance_Percentage);

      final String elevationUp_Percentage_Text           = elevationUp_Percentage         == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(elevationUp_Percentage);
      final String elevationDown_Percentage_Text         = elevationDown_Percentage       == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(elevationDown_Percentage);

      final String deviceTime_Elapsed_UnitText           = deviceTime_Elapsed             == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String deviceTime_Recorded_UnitText          = deviceTime_Recorded            == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String deviceTime_Paused_UnitText            = deviceTime_Paused              == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String computedTime_Moving_UnitText          = computedTime_Moving            == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String computedTime_Break_UnitText           = computedTime_Break             == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;

      final String deviceTime_Elapsed_Summary_UnitText   = deviceTime_Elapsed_Summary     == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String deviceTime_Recorded_Summary_UnitText  = deviceTime_Recorded_Summary    == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String deviceTime_Paused_Summary_UnitText    = deviceTime_Paused_Summary      == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String computedTime_Moving_Summary_UnitText  = computedTime_Moving_Summary    == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;
      final String computedTime_Break_Summary_UnitText   = computedTime_Break_Summary     == 0 ? UI.EMPTY_STRING : Messages.Tour_Tooltip_Label_Hour;

      _lblDeviceTime_Elapsed                 .setText(FormatManager.formatElapsedTime_Summary   (deviceTime_Elapsed));
      _lblDeviceTime_Elapsed_Unit            .setText(deviceTime_Elapsed_UnitText);

      _lblDeviceTime_Recorded                .setText(FormatManager.formatRecordedTime_Summary  (deviceTime_Recorded));
      _lblDeviceTime_Recorded_Unit           .setText(deviceTime_Recorded_UnitText);

      _lblDeviceTime_Paused                  .setText(FormatManager.formatPausedTime_Summary    (deviceTime_Paused));
      _lblDeviceTime_Paused_Unit             .setText(deviceTime_Paused_UnitText);

      _lblComputedTime_Moving                .setText(FormatManager.formatMovingTime_Summary    (computedTime_Moving));
      _lblComputedTime_Moving_Unit           .setText(computedTime_Moving_UnitText);

      _lblComputedTime_Break                 .setText(FormatManager.formatBreakTime_Summary     (computedTime_Break));
      _lblComputedTime_Break_Unit            .setText(computedTime_Break_UnitText);

      _lblDistance                           .setText(distance       == 0 ? UI.EMPTY_STRING : FormatManager.formatDistance_Summary  (distance / 1000.0));
      _lblDistance_Unit                      .setText(distance       == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_DISTANCE);

      _lblElevationUp                        .setText(elevationUp    == 0 ? UI.EMPTY_STRING : FormatManager.formatElevation_Summary (elevationUp));
      _lblElevationUp_Unit                   .setText(elevationUp    == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_ELEVATION);
      _lblElevationDown                      .setText(elevationDown  == 0 ? UI.EMPTY_STRING : FormatManager.formatElevation_Summary (elevationDown));
      _lblElevationDown_Unit                 .setText(elevationDown  == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_ELEVATION);

      _lblNumberOfTours                      .setText(Integer.toString((int) (numTours + 0.5)));

      if (_isShowSummary) {

         _lblDeviceTime_Elapsed_Summary      .setText(FormatManager.formatElapsedTime_Summary   (deviceTime_Elapsed_Summary));
         _lblDeviceTime_Elapsed_Summary_Unit .setText(deviceTime_Elapsed_Summary_UnitText);
         _lblDeviceTime_Recorded_Summary     .setText(FormatManager.formatRecordedTime_Summary  (deviceTime_Recorded_Summary));
         _lblDeviceTime_Recorded_Summary_Unit.setText(deviceTime_Recorded_Summary_UnitText);
         _lblDeviceTime_Paused_Summary       .setText(FormatManager.formatPausedTime_Summary    (deviceTime_Paused_Summary));
         _lblDeviceTime_Paused_Summary_Unit  .setText(deviceTime_Paused_Summary_UnitText);

         _lblComputedTime_Moving_Summary     .setText(FormatManager.formatMovingTime_Summary    (computedTime_Moving_Summary));
         _lblComputedTime_Moving_Summary_Unit.setText(computedTime_Moving_Summary_UnitText);
         _lblComputedTime_Break_Summary      .setText(FormatManager.formatBreakTime_Summary     (computedTime_Break_Summary));
         _lblComputedTime_Break_Summary_Unit .setText(computedTime_Break_Summary_UnitText);

         _lblDistance_Summary                .setText(distance_Summary        == 0 ? UI.EMPTY_STRING : FormatManager.formatDistance_Summary  (distance_Summary / 1000.0));
         _lblDistance_Summary_Unit           .setText(distance_Summary        == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_DISTANCE);

         _lblElevationUp_Summary             .setText(elevationUp_Summary     == 0 ? UI.EMPTY_STRING : FormatManager.formatElevation_Summary (elevationUp_Summary));
         _lblElevationUp_Summary_Unit        .setText(elevationUp_Summary     == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_ELEVATION);
         _lblElevationDown_Summary           .setText(elevationDown_Summary   == 0 ? UI.EMPTY_STRING : FormatManager.formatElevation_Summary (elevationDown_Summary));
         _lblElevationDown_Summary_Unit      .setText(elevationDown_Summary   == 0 ? UI.EMPTY_STRING : UI.UNIT_LABEL_ELEVATION);

         _lblNumberOfTours_Summary           .setText(Integer.toString((int) (numTours_Summary + 0.5)));
      }

      if (_isShowPercentage) {

         _lblDeviceTime_Elapsed_Percentage   .setText(deviceTime_Elapsed_Percentage_Text);
         _lblDeviceTime_Recorded_Percentage  .setText(deviceTime_Recorded_Percentage_Text);
         _lblDeviceTime_Paused_Percentage    .setText(deviceTime_Paused_Percentage_Text);

         _lblComputedTime_Moving_Percentage  .setText(computedTime_Moving_Percentage_Text);
         _lblComputedTime_Break_Percentage   .setText(computedTime_Break_Percentage_Text);

         _lblDistance_Percentage             .setText(distance_Percentage_Text);

         _lblElevationUp_Percentage          .setText(elevationUp_Percentage_Text);
         _lblElevationDown_Percentage        .setText(elevationDown_Percentage_Text);

         _lblNumberOfTours_Percentage        .setText(Integer.toString((int) (numTours_Percentage + 0.5)));
      }

// SET_FORMATTING_ON

   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }
}
