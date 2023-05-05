/******************************************************  *************************
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.IValueFormatter;
import net.tourbook.common.formatter.ValueFormatter_Number_1_0;
import net.tourbook.common.formatter.ValueFormatter_Time_HH;
import net.tourbook.common.formatter.ValueFormatter_Time_HHMM;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.statistic.DurationTime;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.IPreferenceStore;
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

   private static final String NUMBERS_UNIT             = net.tourbook.statistics.Messages.NUMBERS_UNIT;

   // Dashes:  – — …
   private static final String           NUMBER_BETWEEN_FIRST_AND_LAST_BAR = "%d – %d";                      //$NON-NLS-1$
   private static final String           NUMBER_FIRST_BAR                  = "<%d";                          //$NON-NLS-1$
   private static final String           NUMBER_LAST_BAR                   = ">%d";                          //$NON-NLS-1$
   private static final String           TEXT_BETWEEN_FIRST_AND_LAST_BAR   = "%s – %s";                      //$NON-NLS-1$
   private static final String           TEXT_FIRST_BAR                    = "<%s";                          //$NON-NLS-1$
   private static final String           TEXT_LAST_BAR                     = ">%s";                          //$NON-NLS-1$

   private static final String           TITLE_FORMAT                      = "%s %s";                        //$NON-NLS-1$

   private static final int              VERTICAL_LINE_SPACE               = 8;
   private static final int              SHELL_MARGIN                      = 5;

   private static final IPreferenceStore _prefStore                        = TourbookPlugin.getPrefStore();

   private static final IValueFormatter  VALUE_FORMATTER_1_0               = new ValueFormatter_Number_1_0();

   private static IValueFormatter        _valueFormatter_Time_HH           = new ValueFormatter_Time_HH();
   private static IValueFormatter        _valueFormatter_Time_HHMM         = new ValueFormatter_Time_HHMM();

   private static final int              _columnSpacing                    = 10;

   /*
    * Tooltip context
    */
   private IToolTipProvider            _toolTipProvider;
   private TourStatisticData_Frequency _statisticData_Frequency;

   private String                      _toolTip_SubTitle;
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
   private Color              _bgColor;
   private Color              _fgColor;

   private Font               FONT_BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   private FrequencyStatistic _frequencyStatistic;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Label     _lblSubTitle;
   private Label     _lblTitle;
   private CLabel    _lblTourType_Image;

   private Label     _lblColumnHeader_Summary;
   private Label     _lblColumnHeader_TourType;

   private Label     _lblDataLabel;

   private Label     _lblDataValue;
   private Label     _lblDataValue_Unit;
   private Label     _lblDataValue_Summary;
   private Label     _lblDataValue_Summary_Unit;
   private Label     _lblDataValue_Percentage;

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

   private int computeSummary(final int[][] allDataSeries, final int valueIndex) {

      int summary = 0;

      for (final int[] dataSerie : allDataSeries) {
         summary += dataSerie[valueIndex];
      }

      return summary;
   }

   private void createActions() {

      _actionCloseTooltip = new ActionCloseTooltip();

      _actionPrefDialog = new Action_ToolTip_EditPreferences(_toolTipProvider,
            Messages.Tour_Tooltip_Action_EditFormatPreferences,
            PrefPageAppearanceDisplayFormat.ID,

            // set index for the tab folder which should be selected when dialog is opened and applied
            // in net.tourbook.preferences.PrefPageAppearanceDisplayFormat.applyData(Object)
            Integer.valueOf(1));
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
                          final String toolTip_SubTitle,
                          final boolean isShowSummary,
                          final boolean isShowPercentage) {

      _toolTipProvider = toolTipProvider;
      _statisticData_Frequency = statisticData_Frequency;
      _frequencyStatistic = frequencyStatistic;

      _serieIndex = serieIndex;
      _valueIndex = valueIndex;

      _toolTip_SubTitle = toolTip_SubTitle;

      _isShowPercentage = isShowPercentage;
      _isShowSummary = isShowSummary;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      // setup tour type
      _tourTypeId = tourTypeId;
      _tourTypeName = TourDatabase.getTourTypeName(_tourTypeId);
      _isTourTypeImageAvailable = _tourTypeId >= 0;

      initUI();

      createActions();
      createUI(parent);

      updateUI();
      updateUI_Layout();

      enableControls();
   }

   private String createText_DurationTime(final long durationTimeValue, final DurationTime durationTimeType) {

      switch (durationTimeType) {
      case RECORDED:
         return FormatManager.formatRecordedTime_Summary(durationTimeValue);
      case ELAPSED:
         return FormatManager.formatElapsedTime_Summary(durationTimeValue);
      case PAUSED:
         return FormatManager.formatPausedTime_Summary(durationTimeValue);

      case MOVING:
         return FormatManager.formatMovingTime_Summary(durationTimeValue);
      case BREAK:
         return FormatManager.formatBreakTime_Summary(durationTimeValue);
      }

      return UI.EMPTY_STRING;
   }

   private String createText_DurationTimeLabel(final DurationTime durationTimeType) {

      switch (durationTimeType) {

      case ELAPSED:
         return Messages.Tour_Tooltip_Label_ElapsedTime;
      case RECORDED:
         return Messages.Tour_Tooltip_Label_RecordedTime;
      case PAUSED:
         return Messages.Tour_Tooltip_Label_PausedTime;

      case MOVING:
         return Messages.Tour_Tooltip_Label_MovingTime;
      case BREAK:
         return Messages.Tour_Tooltip_Label_BreakTime;
      }

      return UI.EMPTY_STRING;
   }

   private String createText_Title(final TourStatisticData_Frequency statData) {

      final int durationTimeInterval = _prefStore.getInt(ITourbookPreferences.STAT_DURATION_INTERVAL);

      final int[] allGroupValues = statData.statDurationTime_GroupValues;
      String titleText;
      if (_valueIndex == 0) {

         // first bar - duration <
         titleText = String.format(TEXT_FIRST_BAR, createText_TitleTime(allGroupValues[_valueIndex], durationTimeInterval));

      } else if (_valueIndex == allGroupValues.length - 1) {

         // last bar - duration >

         titleText = String.format(TEXT_LAST_BAR, createText_TitleTime(allGroupValues[_valueIndex - 1], durationTimeInterval));

      } else {

         // between first and last bar - duration ...-...

         titleText = String.format(TEXT_BETWEEN_FIRST_AND_LAST_BAR,
               createText_TitleTime(allGroupValues[_valueIndex - 1], durationTimeInterval),
               createText_TitleTime(allGroupValues[_valueIndex], durationTimeInterval));
      }
      return titleText;
   }

   private String createText_TitleTime(final int durationTime_Value, final int durationTime_Interval) {

      if (durationTime_Interval % 60 == 0) {
         return _valueFormatter_Time_HH.printLong(durationTime_Value, false, false);
      } else {
         // show minutes when interval is not 1 hour
         return _valueFormatter_Time_HHMM.printLong(durationTime_Value, false, false);
      }
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
//            container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
            {

               createUI_18_ColumnHeader(container);

               createUI_Spacer_Row(container, numColumns);
               createUI_30_BarData(container);

//               createUI_Spacer_Row(container, numColumns);
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
            GridDataFactory.fillDefaults().indent(_columnSpacing, 0).applyTo(lblPercentage);
         }
      }

      if (_isShowSummary) {

         {
            // column 5+6: Total
            _lblColumnHeader_Summary = createUI_Label(parent, UI.EMPTY_STRING, SWT.TRAIL);
            _lblColumnHeader_Summary.setFont(FONT_BOLD);
            GridDataFactory.fillDefaults()
                  .indent(_columnSpacing, 0)
                  .span(2, 1)
                  .applyTo(_lblColumnHeader_Summary);
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
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .grab(true, false)
                  .applyTo(_lblDataValue_Summary);

            _lblDataValue_Summary_Unit = createUI_LabelValue(container, SWT.TRAIL);
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
         createUI_Label(container, NUMBERS_UNIT, SWT.LEAD);

         if (_isShowPercentage) {
            _lblNumberOfTours_Percentage = createUI_LabelValue(container, SWT.TRAIL);
         }

         if (_isShowSummary) {
            _lblNumberOfTours_Summary = createUI_LabelValue(container, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .grab(true, false)
                  .applyTo(_lblNumberOfTours_Summary);

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
   private void createUI_Spacer_Row(final Composite container, final int numColumns) {

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

      _lblColumnHeader_TourType.setText(_tourTypeName);

      if (_toolTip_SubTitle != null) {
         _lblSubTitle.setText(_toolTip_SubTitle);
      }

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
   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }

   private void updateUI_Statistic_Distance() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

// SET_FORMATTING_OFF

      final int distance                     = statData.statDistance_SumValues_High    [_serieIndex][_valueIndex];
      final float numTours                   = statData.statDistance_NumTours_High     [_serieIndex][_valueIndex] ;

      final float distance_Summary           = computeSummary(statData.statDistance_SumValues_High,   _valueIndex);
      final float numTours_Summary           = computeSummary(statData.statDistance_NumTours_High,    _valueIndex);

      final float distance_Percentage        = distance_Summary      == 0 ? 0 : distance / distance_Summary * 100;
      final float numTours_Percentage        = numTours_Summary      == 0 ? 0 : numTours / numTours_Summary * 100;

      final String distance_Percentage_Text  = distance_Percentage   == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(distance_Percentage);

// SET_FORMATTING_ON

      String groupText;
      final int[] allGroupValues = statData.statDistance_GroupValues;

      if (_valueIndex == 0) {

         // first bar - duration <
         groupText = String.format(NUMBER_FIRST_BAR, allGroupValues[_valueIndex]);

      } else if (_valueIndex == allGroupValues.length - 1) {

         // last bar - duration >
         groupText = String.format(NUMBER_LAST_BAR, allGroupValues[_valueIndex - 1]);

      } else {

         // between first and last bar - duration ... - ...
         groupText = String.format(NUMBER_BETWEEN_FIRST_AND_LAST_BAR,
               allGroupValues[_valueIndex - 1],
               allGroupValues[_valueIndex]);
      }

      final String unit = UI.UNIT_LABEL_DISTANCE;
      final String title = String.format(TITLE_FORMAT, groupText, unit);

      _lblTitle.setText(title);

      _lblDataLabel.setText(Messages.Statistic_Tooltip_Label_Distance);

      _lblDataValue.setText(FormatManager.formatDistance_Summary(distance));
      _lblDataValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statDistance_NumTours_High[_serieIndex][_valueIndex]));

      if (_isShowSummary) {

         _lblColumnHeader_Summary.setText(title);

         _lblDataValue_Summary.setText(distance_Summary == 0 ? UI.EMPTY_STRING : FormatManager.formatDistance_Summary(distance_Summary));
         _lblDataValue_Summary_Unit.setText(unit);

         _lblNumberOfTours_Summary.setText(Integer.toString((int) (numTours_Summary + 0.5)));
      }

      if (_isShowPercentage) {

         _lblDataValue_Percentage.setText(distance_Percentage_Text);
         _lblNumberOfTours_Percentage.setText(Integer.toString((int) (numTours_Percentage + 0.5)));
      }
   }

   private void updateUI_Statistic_DurationTime() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

   // SET_FORMATTING_OFF

      final int durationTime                    = statData.statDurationTime_SumValues_High   [_serieIndex][_valueIndex];
      final float numTours                      = statData.statDurationTime_NumTours_High    [_serieIndex][_valueIndex] ;

      final float durationTime_Summary          = computeSummary(statData.statDurationTime_SumValues_High,   _valueIndex);
      final float numTours_Summary              = computeSummary(statData.statDurationTime_NumTours_High,    _valueIndex);

      final float durationTime_Percentage       = durationTime_Summary     == 0 ? 0 : durationTime / durationTime_Summary * 100;
      final float numTours_Percentage           = numTours_Summary         == 0 ? 0 : numTours / numTours_Summary * 100;

      final String durationTime_Percentage_Text = durationTime_Percentage  == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(durationTime_Percentage);

// SET_FORMATTING_ON

      final DurationTime durationTimeType = (DurationTime) net.tourbook.common.util.Util.getEnumValue(
            _prefStore.getString(ITourbookPreferences.STAT_FREQUENCY_DURATION_TIME),
            DurationTime.MOVING);

      final String unit = Messages.Tour_Tooltip_Label_Hour;
      final String title = String.format(TITLE_FORMAT, createText_Title(statData), unit);

      _lblTitle.setText(title);

      _lblDataLabel.setText(createText_DurationTimeLabel(durationTimeType));

      _lblDataValue.setText(createText_DurationTime(durationTime, durationTimeType));
      _lblDataValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statDurationTime_NumTours_High[_serieIndex][_valueIndex]));

      if (_isShowSummary) {

         _lblColumnHeader_Summary.setText(title);

         _lblDataValue_Summary.setText(durationTime_Summary == 0
               ? UI.EMPTY_STRING
               : createText_DurationTime((long) durationTime_Summary, durationTimeType));

         _lblDataValue_Summary_Unit.setText(unit);

         _lblNumberOfTours_Summary.setText(Integer.toString((int) (numTours_Summary + 0.5)));
      }

      if (_isShowPercentage) {

         _lblDataValue_Percentage.setText(durationTime_Percentage_Text);
         _lblNumberOfTours_Percentage.setText(Integer.toString((int) (numTours_Percentage + 0.5)));
      }
   }

   private void updateUI_Statistic_Elevation() {

      final TourStatisticData_Frequency statData = _statisticData_Frequency;

   // SET_FORMATTING_OFF

      final int elevationUp                           = statData.statElevation_SumValues_High    [_serieIndex][_valueIndex];
      final float numTours                            = statData.statElevation_NumTours_High     [_serieIndex][_valueIndex] ;

      final float elevationUp_Summary                 = computeSummary(statData.statElevation_SumValues_High,   _valueIndex);
      final float numTours_Summary                    = computeSummary(statData.statElevation_NumTours_High,    _valueIndex);

      final float elevationUp_Percentage              = elevationUp_Summary      == 0 ? 0 : elevationUp / elevationUp_Summary * 100;
      final float numTours_Percentage                 = numTours_Summary         == 0 ? 0 : numTours / numTours_Summary * 100;

      final String elevationUp_Percentage_Text        = elevationUp_Percentage   == 0  ? UI.EMPTY_STRING : VALUE_FORMATTER_1_0.printDouble(elevationUp_Percentage);

// SET_FORMATTING_ON

      final int[] allGrouped_ElevationUp = statData.statElevation_GroupValues;
      String grouped_ElevationUp;

      if (_valueIndex == 0) {

         // first bar - duration <

         grouped_ElevationUp = String.format(NUMBER_FIRST_BAR, allGrouped_ElevationUp[_valueIndex]);

      } else if (_valueIndex == allGrouped_ElevationUp.length - 1) {

         // last bar - duration >

         grouped_ElevationUp = String.format(NUMBER_LAST_BAR, allGrouped_ElevationUp[_valueIndex - 1]);

      } else {

         // between first and last bar - duration ... - ...

         grouped_ElevationUp = String.format(NUMBER_BETWEEN_FIRST_AND_LAST_BAR,
               allGrouped_ElevationUp[_valueIndex - 1],
               allGrouped_ElevationUp[_valueIndex]);
      }

      final String unit = UI.UNIT_LABEL_ELEVATION;
      final String title = String.format(TITLE_FORMAT, grouped_ElevationUp, unit);

      _lblTitle.setText(title);

      _lblDataLabel.setText(Messages.Statistic_Tooltip_Label_Elevation);

      _lblDataValue.setText(FormatManager.formatElevation_Summary(elevationUp));
      _lblDataValue_Unit.setText(unit);

      _lblNumberOfTours.setText(Integer.toString(statData.statElevation_NumTours_High[_serieIndex][_valueIndex]));

      if (_isShowSummary) {

         _lblColumnHeader_Summary.setText(title);

         _lblDataValue_Summary.setText(elevationUp_Summary == 0
               ? UI.EMPTY_STRING
               : FormatManager.formatElevation_Summary(elevationUp_Summary));
         _lblDataValue_Summary_Unit.setText(unit);

         _lblNumberOfTours_Summary.setText(Integer.toString((int) (numTours_Summary + 0.5)));
      }

      if (_isShowPercentage) {

         _lblDataValue_Percentage.setText(elevationUp_Percentage_Text);
         _lblNumberOfTours_Percentage.setText(Integer.toString((int) (numTours_Percentage + 0.5)));
      }
   }
}
