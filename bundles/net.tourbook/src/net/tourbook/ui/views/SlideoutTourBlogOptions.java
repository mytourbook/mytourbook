/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.web.WEB;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for the tour blog options
 */
class SlideoutTourBlogOptions extends ToolbarSlideout implements IActionResetToDefault {

   private static final IDialogSettings       _state_WEB      = WEB.getState();

   private static IDialogSettings             _state;

// SET_FORMATTING_OFF

   private static final TimeFormatComboData[] _allTimeFormats =

      new TimeFormatComboData[] {

         new TimeFormatComboData(TimeFormat.TIME_SMALL,        TimeTools.Formatter_Time_S.format(LocalTime.now())),
         new TimeFormatComboData(TimeFormat.TIME_MEDIUM,       TimeTools.Formatter_Time_M.format(LocalTime.now())),

         new TimeFormatComboData(TimeFormat.DATE_TIME_SMALL,   TimeTools.Formatter_DateTime_S.format(LocalDateTime.now())),
         new TimeFormatComboData(TimeFormat.DATE_TIME_MEDIUM,  TimeTools.Formatter_DateTime_M.format(LocalDateTime.now())),
      };

// SET_FORMATTING_ON

   private ActionResetToDefaults              _actionRestoreDefaults;

   private TourBlogView                       _tourBlogView;

   private MouseWheelListener                 _defaultMouseWheelListener;
   private SelectionListener                  _defaultSelectionListener;
   private FocusListener                      _keepOpenListener;

   private GridDataFactory                    _firstColumnLayoutData;
   private GridDataFactory                    _secondColumnLayoutData;

   private List<String>                       _allSortedFontNames;

   private PixelConverter                     _pc;

   /*
    * UI controls
    */
   private Button  _chkDrawMarkerWithDefaultColor;
   private Button  _chkShowHiddenMarker;
   private Button  _chkShowMarkerTime;
   private Button  _chkShowTourMarkers;
   private Button  _chkShowTourNutrition;
   private Button  _chkShowTourSummary;
   private Button  _chkShowTourTags;
   private Button  _chkShowTourWeather;

   private Combo   _comboFonts;
   private Combo   _comboTimeFormat;

   private Spinner _spinnerFontSize;

   static enum TimeFormat {

      TIME_SMALL, //
      TIME_MEDIUM, //

      DATE_TIME_SMALL, //
      DATE_TIME_MEDIUM, //
   }

   private static class TimeFormatComboData {

      String     label;
      TimeFormat timeFormat;

      private TimeFormatComboData(final TimeFormat timeFormat, final String label) {

         this.timeFormat = timeFormat;
         this.label = label;
      }
   }

   /**
    * @param ownerControl
    * @param toolBar
    * @param tourBlogView
    * @param tourBlogState
    */
   SlideoutTourBlogOptions(final Control ownerControl,
                           final ToolBar toolBar,
                           final TourBlogView tourBlogView,
                           final IDialogSettings tourBlogState) {

      super(ownerControl, toolBar);

      _tourBlogView = tourBlogView;
      _state = tourBlogState;
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      fillUI();

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         createUI_10_Header(shellContainer);
         createUI_20_Options(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourBlogOptions_Label_Title);
            MTFont.setBannerFont(label);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(label);
         }
         {
            /*
             * Actionbar
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Display tour summary
             */

            _chkShowTourSummary = new Button(container, SWT.CHECK);
            _chkShowTourSummary.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourSummary);
            _chkShowTourSummary.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkShowTourSummary);
         }
         {
            /*
             * Display tour weather
             */

            _chkShowTourWeather = new Button(container, SWT.CHECK);
            _chkShowTourWeather.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourWeather);
            _chkShowTourWeather.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkShowTourWeather);
         }
         {
            /*
             * Display the tour nutrition
             */

            _chkShowTourNutrition = new Button(container, SWT.CHECK);
            _chkShowTourNutrition.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourNutrition);
            _chkShowTourNutrition.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkShowTourNutrition);

         }
         {
            /*
             * Display the tour tags
             */

            _chkShowTourTags = new Button(container, SWT.CHECK);
            _chkShowTourTags.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourTags);
            _chkShowTourTags.setToolTipText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourTags_Tooltip);
            _chkShowTourTags.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkShowTourTags);
         }
         {
            /*
             * Display the tour markers
             */

            _chkShowTourMarkers = new Button(container, SWT.CHECK);
            _chkShowTourMarkers.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowTourMarkers);
            _chkShowTourMarkers.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkShowTourMarkers);

            {
               /*
                * Show hidden marker
                */

               _chkShowHiddenMarker = new Button(container, SWT.CHECK);
               _chkShowHiddenMarker.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
               _chkShowHiddenMarker.addSelectionListener(_defaultSelectionListener);
               _firstColumnLayoutData.span(3, 1).applyTo(_chkShowHiddenMarker);
            }
            {
               /*
                * Draw marker with default color
                */

               _chkDrawMarkerWithDefaultColor = new Button(container, SWT.CHECK);
               _chkDrawMarkerWithDefaultColor.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor);
               _chkDrawMarkerWithDefaultColor.setToolTipText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor_Tooltip);
               _chkDrawMarkerWithDefaultColor.addSelectionListener(_defaultSelectionListener);
               _firstColumnLayoutData.span(3, 1).applyTo(_chkDrawMarkerWithDefaultColor);
            }
            {
               /*
                * Display marker time
                */

               _chkShowMarkerTime = new Button(container, SWT.CHECK);
               _chkShowMarkerTime.setText(Messages.Slideout_TourBlogOptions_Checkbox_ShowMarkerTime);
               _chkShowMarkerTime.addSelectionListener(_defaultSelectionListener);
               _firstColumnLayoutData.span(3, 1).applyTo(_chkShowMarkerTime);
            }
            {
               /*
                * Time format
                */
               _comboTimeFormat = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboTimeFormat.setVisibleItemCount(20);
               _comboTimeFormat.addSelectionListener(_defaultSelectionListener);
               _comboTimeFormat.addFocusListener(_keepOpenListener);
               _secondColumnLayoutData.span(3, 1).applyTo(_comboTimeFormat);
            }
         }
         {
            /*
             * Font
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourBlogOptions_Label_Font);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            _comboFonts = new Combo(container, SWT.READ_ONLY);
            _comboFonts.addSelectionListener(_defaultSelectionListener);
            _comboFonts.addFocusListener(_keepOpenListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_comboFonts);
         }
         {
            /*
             * Font size
             */

            // label
            Label label = new Label(container, SWT.NONE);
            label.setText(OtherMessages.APP_WEB_LABEL_DEFAULT_FONT_SIZE);
            label.setToolTipText(OtherMessages.APP_WEB_LABEL_DEFAULT_FONT_SIZE_TOOLTIP);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);

            // spinner
            _spinnerFontSize = new Spinner(container, SWT.BORDER);
            _spinnerFontSize.setMinimum(WEB.STATE_BODY_FONT_SIZE_MIN);
            _spinnerFontSize.setMaximum(WEB.STATE_BODY_FONT_SIZE_MAX);
            _spinnerFontSize.setToolTipText(OtherMessages.APP_WEB_LABEL_DEFAULT_FONT_SIZE_TOOLTIP);
            _spinnerFontSize.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerFontSize.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_spinnerFontSize);

            // px
            label = new Label(container, SWT.NONE);
            label.setText(Messages.App_Unit_Px);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
         }
      }
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isShowTourMarkers  = _chkShowTourMarkers.getSelection();
      final boolean isShowMarkerTime   = _chkShowMarkerTime.getSelection();

      _chkDrawMarkerWithDefaultColor   .setEnabled(isShowTourMarkers);
      _chkShowHiddenMarker             .setEnabled(isShowTourMarkers);
      _chkShowMarkerTime               .setEnabled(isShowTourMarkers);

      _comboTimeFormat                 .setEnabled(isShowTourMarkers && isShowMarkerTime);

// SET_FORMATTING_ON
   }

   private void fillUI() {

      final FontData[] allScalabelFonts = Display.getDefault().getFontList(null, true);
      final FontData[] allFixedFonts = Display.getDefault().getFontList(null, false);

      final Set<String> allFontNames = new HashSet<>();

      for (final FontData fontData : allFixedFonts) {
         allFontNames.add(fontData.getName() + UI.SPACE + Messages.Slideout_TourBlogOptions_Info_FontFixed);
      }

      for (final FontData fontData : allScalabelFonts) {
         allFontNames.add(fontData.getName());
      }

      _allSortedFontNames = new ArrayList<>(allFontNames);
      java.util.Collections.sort(_allSortedFontNames);

      for (final String fontName : _allSortedFontNames) {
         _comboFonts.add(fontName);
      }

      for (final TimeFormatComboData data : _allTimeFormats) {
         _comboTimeFormat.add(data.label);
      }
   }

   private int getFontIndex(final String fontName) {

      for (int fontIndex = 0; fontIndex < _allSortedFontNames.size(); fontIndex++) {

         final String availableFontName = _allSortedFontNames.get(fontIndex);

         if (availableFontName.equalsIgnoreCase(fontName)) {

            return fontIndex;
         }
      }

      final String defaultFontName = WEB.STATE_BODY_FONT_DEFAULT;

      for (int fontIndex = 0; fontIndex < _allSortedFontNames.size(); fontIndex++) {

         final String availableFontName = _allSortedFontNames.get(fontIndex);

         if (availableFontName.equalsIgnoreCase(defaultFontName)) {

            return fontIndex;
         }
      }

      return 0;
   }

   private String getSelectedFont() {

      final int selectionIndex = _comboFonts.getSelectionIndex();

      if (selectionIndex >= 0) {

         return _allSortedFontNames.get(selectionIndex);
      }

      return WEB.STATE_BODY_FONT_DEFAULT;
   }

   private TimeFormat getSelectedTimeFormat() {

      final int selectedIndex = _comboTimeFormat.getSelectionIndex();

      if (selectedIndex < 0) {
         return TimeFormat.TIME_SMALL;
      }

      final TimeFormatComboData selectedData = _allTimeFormats[selectedIndex];

      return selectedData.timeFormat;
   }

   private int getTimeFormatIndex(final TimeFormat requestedData) {

      final TimeFormatComboData[] allData = _allTimeFormats;

      for (int dataIndex = 0; dataIndex < allData.length; dataIndex++) {

         final TimeFormatComboData data = allData[dataIndex];

         if (data.timeFormat.equals(requestedData)) {
            return dataIndex;
         }
      }

      // this should not happen
      return 0;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {

            setIsAnotherDialogOpened(false);
         }
      };

      final int firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

      _firstColumnLayoutData = GridDataFactory.fillDefaults()
            .indent(firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);

      final int secondColumnIndent = _pc.convertWidthInCharsToPixels(6);

      _secondColumnLayoutData = GridDataFactory.fillDefaults()
            .indent(secondColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      _tourBlogView.updateUI();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkShowHiddenMarker             .setSelection(TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT);
      _chkShowMarkerTime               .setSelection(TourBlogView.STATE_IS_SHOW_MARKER_TIME_DEFAULT);
      _chkDrawMarkerWithDefaultColor   .setSelection(TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT);
      _chkShowTourMarkers              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT);
      _chkShowTourNutrition            .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT);
      _chkShowTourSummary              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT);
      _chkShowTourTags                 .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT);
      _chkShowTourWeather              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT);

      _comboFonts                      .select(getFontIndex(WEB.STATE_BODY_FONT_DEFAULT));
      _spinnerFontSize                 .setSelection(WEB.STATE_BODY_FONT_SIZE_DEFAULT);

      _comboTimeFormat                 .select(getTimeFormatIndex(TourBlogView.STATE_TIME_FORMAT_DEFAULT));

// SET_FORMATTING_ON

      onChangeUI();

      enableControls();
   }

   private void restoreState() {

      final TimeFormat timeFormat = (TimeFormat) Util.getStateEnum(_state, TourBlogView.STATE_TIME_FORMAT, TourBlogView.STATE_TIME_FORMAT_DEFAULT);

// SET_FORMATTING_OFF

      _chkShowHiddenMarker             .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT));
      _chkShowMarkerTime               .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_MARKER_TIME,                 TourBlogView.STATE_IS_SHOW_MARKER_TIME_DEFAULT));
      _chkDrawMarkerWithDefaultColor   .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT));
      _chkShowTourMarkers              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT));
      _chkShowTourNutrition            .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT));
      _chkShowTourSummary              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT));
      _chkShowTourTags                 .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT));
      _chkShowTourWeather              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT));

      _comboFonts                      .select(getFontIndex(Util.getStateString  (_state_WEB, WEB.STATE_BODY_FONT,       WEB.STATE_BODY_FONT_DEFAULT)));
      _spinnerFontSize                 .setSelection(Util.getStateInt            (_state_WEB, WEB.STATE_BODY_FONT_SIZE,  WEB.STATE_BODY_FONT_SIZE_DEFAULT));

      _comboTimeFormat                 .select(getTimeFormatIndex( timeFormat));

// SET_FORMATTING_ON

      enableControls();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  _chkDrawMarkerWithDefaultColor   .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              _chkShowHiddenMarker             .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_MARKER_TIME,                _chkShowMarkerTime               .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               _chkShowTourMarkers              .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             _chkShowTourNutrition            .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               _chkShowTourSummary              .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  _chkShowTourTags                 .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               _chkShowTourWeather              .getSelection());

      _state_WEB.put(WEB.STATE_BODY_FONT,                               getSelectedFont());
      _state_WEB.put(WEB.STATE_BODY_FONT_SIZE,                          _spinnerFontSize                 .getSelection());

      Util.setStateEnum(_state, TourBlogView.STATE_TIME_FORMAT, getSelectedTimeFormat());

// SET_FORMATTING_ON
   }
}
