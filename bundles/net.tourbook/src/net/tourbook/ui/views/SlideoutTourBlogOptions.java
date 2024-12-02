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

   private static final IDialogSettings _state_WEB = WEB.getState();
   private static IDialogSettings       _state;

   private ActionResetToDefaults        _actionRestoreDefaults;

   private TourBlogView                 _tourBlogView;

   private MouseWheelListener           _defaultMouseWheelListener;
   private SelectionListener            _defaultSelectionListener;
   private FocusListener                _keepOpenListener;

   private GridDataFactory              _firstColumnLayoutData;

   private List<String>                 _allSortedFontNames;

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private Button  _chkDrawMarkerWithDefaultColor;
   private Button  _chkShowHiddenMarker;
   private Button  _chkShowTourMarkers;
   private Button  _chkShowTourNutrition;
   private Button  _chkShowTourSummary;
   private Button  _chkShowTourTags;
   private Button  _chkShowTourWeather;

   private Combo   _comboFonts;

   private Spinner _spinnerFontSize;

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

   public void enableControls() {

      final boolean isShowTourMarkers = _chkShowTourMarkers.getSelection();

      _chkDrawMarkerWithDefaultColor.setEnabled(isShowTourMarkers);
      _chkShowHiddenMarker.setEnabled(isShowTourMarkers);
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
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      _tourBlogView.updateUI();
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkDrawMarkerWithDefaultColor   .setSelection(TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT);
      _chkShowHiddenMarker             .setSelection(TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT);
      _chkShowTourMarkers              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT);
      _chkShowTourNutrition            .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT);
      _chkShowTourSummary              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT);
      _chkShowTourTags                 .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT);
      _chkShowTourWeather              .setSelection(TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT);

      _comboFonts                      .select(getFontIndex(WEB.STATE_BODY_FONT_DEFAULT));
      _spinnerFontSize                 .setSelection(WEB.STATE_BODY_FONT_SIZE_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI();

      enableControls();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkDrawMarkerWithDefaultColor   .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT));
      _chkShowHiddenMarker             .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT));
      _chkShowTourMarkers              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT));
      _chkShowTourNutrition            .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT));
      _chkShowTourSummary              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT));
      _chkShowTourTags                 .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT));
      _chkShowTourWeather              .setSelection(Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT));

      _comboFonts                      .select(getFontIndex(Util.getStateString  (_state_WEB, WEB.STATE_BODY_FONT,       WEB.STATE_BODY_FONT_DEFAULT)));
      _spinnerFontSize                 .setSelection(Util.getStateInt            (_state_WEB, WEB.STATE_BODY_FONT_SIZE,  WEB.STATE_BODY_FONT_SIZE_DEFAULT));

// SET_FORMATTING_ON

      enableControls();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  _chkDrawMarkerWithDefaultColor   .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              _chkShowHiddenMarker             .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               _chkShowTourMarkers              .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             _chkShowTourNutrition            .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               _chkShowTourSummary              .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  _chkShowTourTags                 .getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               _chkShowTourWeather              .getSelection());

      _state_WEB.put(WEB.STATE_BODY_FONT,                               getSelectedFont());
      _state_WEB.put(WEB.STATE_BODY_FONT_SIZE,                          _spinnerFontSize                 .getSelection());

// SET_FORMATTING_ON
   }

}
