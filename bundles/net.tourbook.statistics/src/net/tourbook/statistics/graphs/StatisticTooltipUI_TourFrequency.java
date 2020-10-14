/******************************************************  *************************
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

import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.Util;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.database.TourDatabase;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.Messages;

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

public class StatisticTooltipUI_TourFrequency {

   private static final String APP_ACTION_CLOSE_TOOLTIP          = net.tourbook.common.Messages.App_Action_Close_Tooltip;
   private static final String IMAGE_APP_CLOSE                   = net.tourbook.common.Messages.Image__App_Close;

   private static final String NUMBER_BETWEEN_FIRST_AND_LAST_BAR = "%d-%d";                                              //$NON-NLS-1$
   private static final String NUMBER_FIRST_BAR                  = "<%d";                                                //$NON-NLS-1$
   private static final String NUMBER_LAST_BAR                   = ">%d";                                                //$NON-NLS-1$
   private static final String TEXT_BETWEEN_FIRST_AND_LAST_BAR   = "%s-%s";                                              //$NON-NLS-1$
   private static final String TEXT_FIRST_BAR                    = "<%s";                                                //$NON-NLS-1$
   private static final String TEXT_LAST_BAR                     = ">%s";                                                //$NON-NLS-1$

   private static final String TITLE_FORMAT                      = "%s %s";                                              //$NON-NLS-1$

   private static final int    VERTICAL_LINE_SPACE               = 8;

   private static final int    SHELL_MARGIN                      = 5;

   /*
    * Tooltip context
    */
   private IToolTipProvider            _toolTipProvider;
   private TourStatisticData_Frequency _statisticData_Frequency;

   private String                      _summaryColumn_HeaderTitle;
   private String                      _toolTip_SubTitle;
   private String                      _toolTip_Title;
   private int                         _serieIndex;
   private int                         _valueIndex;

   private boolean                     _isTourTypeImageAvailable;
   private long                        _tourTypeId;
   private String                      _tourTypeName;

   private boolean                     _isShowPercentage;
   private boolean                     _isShowSummary;

   /*
    * Actions
    */
   private ActionCloseTooltip _actionCloseTooltip;

   /*
    * UI resources
    */
   private Color              _bgColor;
   private Color              _fgColor;

   private Font               FONT_BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private FrequencyStatistic _frequencyStatistic;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Label     _lblColumnHeader_TourType;
   private Label     _lblSubTitle;
   private Label     _lblTitle;
   private CLabel    _lblTourType_Image;

   private Label     _lblDataLabel;

   private Label     _lblDataValue;
   private Label     _lblDataValue_Unit;
   private Label     _lblDataValue_Summary;
   private Label     _lblDataValue_Summary_Unit;
   private Label     _lblDataValue_Percentage;

   private Label     _lblNumberOfTours;
   private Label     _lblNumberOfTours_Percentage;
   private Label     _lblNumberOfTours_Summary;

   private Label     _lblTotal_Label;
   private Label     _lblTotalValue;
   private Label     _lblTotalValue_Unit;
   private Label     _lblTotalValue_Summary;
   private Label     _lblTotalValue_Summary_Unit;
   private Label     _lblTotalValue_Percentage;

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

      _actionCloseTooltip = new ActionCloseTooltip();
   }

   /**
    * @param parent
    * @param toolTipProvider
    * @param statisticData_Frequency
    * @param frequencyStatistic
    * @param serieIndex
    * @param valueIndex
    * @param tourTypeId
    * @param toolTip_Title
    * @param toolTip_SubTitle
    * @param summaryColumn_HeaderTitle
    * @param isShowSummary
    * @param isShowPercentage
    */
   void createContentArea(final Composite parent,
                          final IToolTipProvider toolTipProvider,
                          final TourStatisticData_Frequency statisticData_Frequency,
                          final FrequencyStatistic frequencyStatistic,
                          final int serieIndex,
                          final int valueIndex,
                          final long tourTypeId,
                          final String toolTip_Title,
                          final String toolTip_SubTitle,
                          final String summaryColumn_HeaderTitle,
                          final boolean isShowSummary,
                          final boolean isShowPercentage) {

      _toolTipProvider = toolTipProvider;
      _statisticData_Frequency = statisticData_Frequency;
      _frequencyStatistic = frequencyStatistic;

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
      _tourTypeId = tourTypeId;
      _tourTypeName = TourDatabase.getTourTypeName(_tourTypeId);
      _isTourTypeImageAvailable = _tourTypeId >= 0;

      initUI(parent);

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
//            container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
            {

               createUI_18_ColumnHeader(container);

               createUI_Spacer_Columns(container, numColumns);
               createUI_30_BarData(container);

               createUI_Spacer_Columns(container, numColumns);
               createUI_32_NumTours(container);
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

//      tbm.add(_actionPrefDialog);

      /**
       * The close action is ALWAYS visible, sometimes there is a bug that the tooltip do not
       * automatically close when hovering out.
       */
      tbm.add(_actionCloseTooltip);

      tbm.update(true);
   }

   private void createUI_18_ColumnHeader(final Composite parent) {

      final int columnSpacing = 20;

      {
         // column 1+2
         _lblColumnHeader_TourType = createUI_Label(parent, UI.EMPTY_STRING);
         _lblColumnHeader_TourType.setFont(FONT_BOLD);
         GridDataFactory.fillDefaults()
               .span(2, 1)
//               .align(SWT.END, SWT.FILL)
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
            GridDataFactory.fillDefaults().indent(columnSpacing, 0).applyTo(lblPercentage);
         }
      }

      if (_isShowSummary) {

         {
            // column 5: Total
            final Label lblTotal = createUI_Label(parent, _summaryColumn_HeaderTitle, SWT.TRAIL);
            lblTotal.setFont(FONT_BOLD);
            GridDataFactory.fillDefaults()
                  .indent(columnSpacing, 0)
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

   private void createUI_30_BarData(final Composite container) {

      {
         /*
          * Data
          */
         _lblDataLabel = createUI_Label(container, UI.EMPTY_STRING);

         _lblDataValue = createUI_LabelValue(container, SWT.TRAIL);
         _lblDataValue_Unit = createUI_LabelValue(container, SWT.TRAIL);

         if (_isShowPercentage) {
            _lblDataValue_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDataValue_Summary = createUI_LabelValue(container, SWT.TRAIL);
            _lblDataValue_Summary_Unit = createUI_LabelValue(container, SWT.TRAIL);
         }
      }
      {
         /*
          * Total
          */
         _lblTotal_Label = createUI_Label(container, UI.EMPTY_STRING);

         _lblTotalValue = createUI_LabelValue(container, SWT.TRAIL);
         _lblTotalValue_Unit = createUI_LabelValue(container, SWT.TRAIL);

         if (_isShowPercentage) {
            _lblTotalValue_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblTotalValue_Summary = createUI_LabelValue(container, SWT.TRAIL);
            _lblTotalValue_Summary_Unit = createUI_LabelValue(container, SWT.TRAIL);
         }
      }
   }

   private void createUI_32_NumTours(final Composite container) {

      {
         /*
          * Number of tours
          */
         createUI_Label(container, Messages.Statistic_Tooltip_Label_NumberOfTours);

         _lblNumberOfTours = createUI_LabelValue(container, SWT.TRAIL);
         createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblNumberOfTours_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblNumberOfTours_Summary = createUI_LabelValue(container, SWT.TRAIL);
            createUI_LabelValue(container, SWT.LEAD);
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

   private void initUI(final Composite parent) {

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

      _lblColumnHeader_TourType.setText(_tourTypeName);

      if (_toolTip_SubTitle != null) {
         _lblSubTitle.setText(_toolTip_SubTitle);
      }

      _lblTotal_Label.setText(Messages.Statistic_Tooltip_Label_Total);

      switch (_frequencyStatistic) {
      case DISTANCE:
         updateUI_Statistic_Distance();
         break;

      case DURATION_TIME:
         updateUI_Statistic_DurationTime();
         break;

      case ELEVATION:
         updateUI_Statistic_Elevation();
         break;

      }

// SET_FORMATTING_OFF

//      final float numTours                               = _statisticDate_Frequency.numTours_High_Resorted    [_serieIndex][_valueIndex] + 0.5f;
//
//      final float numTours_Summary                       = computeSummary(_statisticDate_Frequency.numTours_High_Resorted,      _valueIndex) + 0.5f;
//
//      final float numTours_Percentage                    = numTours_Summary    == 0 ? 0 : numTours    / numTours_Summary      * 100;
//
//
//
//      _lblNumberOfTours                   .setText(Integer.toString((int) (numTours + 0.5)));
//
//      if (_isShowSummary) {
//
//
//         _lblNumberOfTours_Summary           .setText(Integer.toString((int) (numTours_Summary + 0.5)));
//      }
//
//      if (_isShowPercentage) {
//
//
//         _lblNumberOfTours_Percentage        .setText(Integer.toString((int) (numTours_Percentage + 0.5)));
//      }

// SET_FORMATTING_ON

   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }

   private void updateUI_Statistic_Distance() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

      String dataValueText;
      final int[] allGroupedValues = statData.statGroupedValues_Distance;

      if (_valueIndex == 0) {

         // first bar - duration <
         dataValueText = String.format(NUMBER_FIRST_BAR, allGroupedValues[_valueIndex]);

      } else if (_valueIndex == allGroupedValues.length - 1) {

         // last bar - duration >
         dataValueText = String.format(NUMBER_LAST_BAR, allGroupedValues[_valueIndex - 1]);

      } else {

         // between first and last bar - duration ... - ...
         dataValueText = String.format(NUMBER_BETWEEN_FIRST_AND_LAST_BAR, allGroupedValues[_valueIndex - 1], allGroupedValues[_valueIndex]);
      }

      final String unit = UI.UNIT_LABEL_DISTANCE;

      _lblTitle.setText(String.format(TITLE_FORMAT, dataValueText, unit));

      _lblDataLabel.setText(Messages.Statistic_Tooltip_Label_Distance);

      _lblDataValue.setText(dataValueText);
      _lblDataValue_Unit.setText(unit);

      // total
      _lblTotalValue.setText(Integer.toString(statData.statDistance_Sum_High[_serieIndex][_valueIndex]));
      _lblTotalValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statDistance_NumTours_High[_serieIndex][_valueIndex]));
   }

   private void updateUI_Statistic_DurationTime() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

      final int[] allGroupedValues = statData.statGroupedValues_DurationTime;
      String dataValueText;

      if (_valueIndex == 0) {

         // first bar - duration <
         dataValueText = String.format(TEXT_FIRST_BAR, Util.formatValue(allGroupedValues[_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE));

      } else if (_valueIndex == allGroupedValues.length - 1) {

         // last bar - duration >

         dataValueText = String.format(TEXT_LAST_BAR, Util.formatValue(allGroupedValues[_valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE));

      } else {

         // between first and last bar - duration ...-...

         dataValueText = String.format(TEXT_BETWEEN_FIRST_AND_LAST_BAR,
               Util.formatValue(allGroupedValues[_valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
               Util.formatValue(allGroupedValues[_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE));
      }

      final String unit = Messages.Tour_Tooltip_Label_Hour;

      _lblTitle.setText(String.format(TITLE_FORMAT, dataValueText, unit));

      _lblDataLabel.setText(Messages.Statistic_Tooltip_Label_DurationTime);

      _lblDataValue.setText(dataValueText);
      _lblDataValue_Unit.setText(unit);

      // total
      _lblTotalValue.setText(Util.formatValue(statData.statDurationTime_Sum_High[_serieIndex][_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE));
      _lblTotalValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statDurationTime_NumTours_High[_serieIndex][_valueIndex]));
   }

   private void updateUI_Statistic_Elevation() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

      final int[] allGroupedValues = statData.statGroupedValues_Elevation;
      String dataValueText;

      if (_valueIndex == 0) {

         // first bar - duration <

         dataValueText = String.format(NUMBER_FIRST_BAR, allGroupedValues[_valueIndex]);

      } else if (_valueIndex == allGroupedValues.length - 1) {

         // last bar - duration >

         dataValueText = String.format(NUMBER_LAST_BAR, allGroupedValues[_valueIndex - 1]);

      } else {

         // between first and last bar - duration ... - ...

         dataValueText = String.format(NUMBER_BETWEEN_FIRST_AND_LAST_BAR, allGroupedValues[_valueIndex - 1], allGroupedValues[_valueIndex]);
      }

      final String unit = UI.UNIT_LABEL_ALTITUDE;

      _lblTitle.setText(String.format(TITLE_FORMAT, dataValueText, unit));

      _lblDataLabel.setText(Messages.Statistic_Tooltip_Label_Elevation);

      _lblDataValue.setText(dataValueText);
      _lblDataValue_Unit.setText(unit);

      // total
      _lblTotalValue.setText(Util.formatValue(statData.statElevation_Sum_High[_serieIndex][_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE));
      _lblTotalValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statElevation_NumTours_High[_serieIndex][_valueIndex]));
   }
}
