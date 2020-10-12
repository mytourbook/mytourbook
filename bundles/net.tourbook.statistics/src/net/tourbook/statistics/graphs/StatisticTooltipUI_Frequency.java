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
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormatter_Number_1_0;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
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

public class StatisticTooltipUI_Frequency {

   private static final String          APP_ACTION_CLOSE_TOOLTIP = net.tourbook.common.Messages.App_Action_Close_Tooltip;
   private static final String          IMAGE_APP_CLOSE          = net.tourbook.common.Messages.Image__App_Close;

   private static final int             VERTICAL_LINE_SPACE      = 8;

   private static final int             SHELL_MARGIN             = 5;

   private static final IValueFormatter VALUE_FORMATTER_1_0      = new ValueFormatter_Number_1_0();

   /*
    * Tooltip context
    */
   private String                      _summaryColumn_HeaderTitle;
   private IToolTipProvider            _toolTipProvider;
   private TourStatisticData_Frequency _statisticDate_Frequency;
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
   private Label     _lblTitle;
   private CLabel    _lblTourType_Image;

   private Label     _lblData;
   private Label     _lblDataValue;
   private Label     _lblDataValue_Percentage;
   private Label     _lblDataValue_Summary;

   private Label     _lblTotal;
   private Label     _lblTotalValue;
   private Label     _lblTotalValue_Percentage;
   private Label     _lblTotalValue_Summary;

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

      final Integer selectedTabFolder = new Integer(1);

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_toolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID,
            selectedTabFolder);

      _actionCloseTooltip = new ActionCloseTooltip();
   }

   /**
    * @param parent
    * @param toolTipProvider
    * @param statisticData_Frequency
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
                          final TourStatisticData_Frequency statisticData_Frequency,
                          final int serieIndex,
                          final int valueIndex,
                          final String toolTip_Title,
                          final String summaryColumn_HeaderTitle,
                          final boolean isShowSummary,
                          final boolean isShowPercentage) {

      _toolTipProvider = toolTipProvider;
      _statisticDate_Frequency = statisticData_Frequency;
      _serieIndex = serieIndex;
      _valueIndex = valueIndex;

      _toolTip_Title = toolTip_Title;
      _summaryColumn_HeaderTitle = summaryColumn_HeaderTitle;

      _isShowPercentage = isShowPercentage;
      _isShowSummary = isShowSummary;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      // setup tour type
//      _tourTypeId = _statisticDate_Frequency.typeIds_Resorted[serieIndex][valueIndex];
//      _tourTypeName = TourDatabase.getTourTypeName(_tourTypeId);
//      _isTourTypeImageAvailable = _tourTypeId >= 0;

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
         _lblData = createUI_Label(container, Messages.Statistic_Tooltip_Label_NumberOfTours);

         _lblDataValue = createUI_LabelValue(container, SWT.TRAIL);
         createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblDataValue_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblDataValue_Summary = createUI_LabelValue(container, SWT.TRAIL);
            createUI_LabelValue(container, SWT.LEAD);
         }
      }
      {
         /*
          * Total
          */
         _lblTotal = createUI_Label(container, Messages.Statistic_Tooltip_Label_NumberOfTours);

         _lblTotalValue = createUI_LabelValue(container, SWT.TRAIL);
         createUI_LabelValue(container, SWT.LEAD);

         if (_isShowPercentage) {
            _lblTotalValue_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblTotalValue_Summary = createUI_LabelValue(container, SWT.TRAIL);
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
//      if (_lblTourType_Image != null && _lblTourType_Image.isDisposed() == false) {
//
//         if (_tourTypeId < 0) {
//            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
//         } else {
//            _lblTourType_Image.setImage(TourTypeImage.getTourTypeImage(_tourTypeId));
//         }
//         _lblTourType_Image.setToolTipText(_tourTypeName);
//      }
//
//      _lblTitle.setText(_toolTip_Title);
//      _lblColumnHeader_TourType.setText(_tourTypeName);
//
//      _lblData.setText(Messages.Statistic_Tooltip_Label_DurationTime);
//      _lblTotal.setText(Messages.Statistic_Tooltip_Label_Total);

      if (_valueIndex == 0) {

         // first bar

         // Duration <%sh * %d
         _lblDataValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_First,
               Util.formatValue(_statisticDate_Frequency.statTime_Units[_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
               _statisticDate_Frequency.statTimeCounter_High[_serieIndex][_valueIndex]));

         // Total %sh
         _lblTotalValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_Total,
               Util.formatValue(_statisticDate_Frequency.statTimeSum_High[_serieIndex][_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)));

      } else if (_valueIndex == _statisticDate_Frequency.statTime_Units.length - 1) {

         // last bar

         // Duration >%sh * %d
         _lblDataValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_Last,
               Util.formatValue(_statisticDate_Frequency.statTime_Units[_valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
               _statisticDate_Frequency.statTimeCounter_High[_serieIndex][_valueIndex]));

         // Total %sh
         _lblTotalValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_Total,
               Util.formatValue(_statisticDate_Frequency.statTimeSum_High[_serieIndex][_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)));

      } else {

         // between first and last bar

         // Duration %s-%sh * %d
         _lblDataValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_Between,
               Util.formatValue(_statisticDate_Frequency.statTime_Units[_valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
               Util.formatValue(_statisticDate_Frequency.statTime_Units[_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
               _statisticDate_Frequency.statTimeCounter_High[_serieIndex][_valueIndex]));

         // Total %sh
         _lblTotalValue.setText(String.format(Messages.Statistic_Tooltip_Label_DurationTime_Total,
               Util.formatValue(_statisticDate_Frequency.statTimeSum_High[_serieIndex][_valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)));
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
}
