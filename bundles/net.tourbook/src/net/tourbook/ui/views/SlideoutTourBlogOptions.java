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

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

   private GridDataFactory              _firstColumnLayoutData;

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

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
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

      _chkDrawMarkerWithDefaultColor.setSelection(    TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT);
      _chkShowHiddenMarker.setSelection(              TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT);
      _chkShowTourMarkers.setSelection(               TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT);
      _chkShowTourNutrition.setSelection(             TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT);
      _chkShowTourSummary.setSelection(               TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT);
      _chkShowTourTags.setSelection(                  TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT);
      _chkShowTourWeather.setSelection(               TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT);

      _spinnerFontSize.setSelection(                  WEB.STATE_BODY_FONT_SIZE_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI();

      enableControls();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _chkDrawMarkerWithDefaultColor.setSelection(    Util.getStateBoolean(_state, TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR_DEFAULT));
      _chkShowHiddenMarker.setSelection(              Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER_DEFAULT));
      _chkShowTourMarkers.setSelection(               Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               TourBlogView.STATE_IS_SHOW_TOUR_MARKERS_DEFAULT));
      _chkShowTourNutrition.setSelection(             Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION_DEFAULT));
      _chkShowTourSummary.setSelection(               Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY_DEFAULT));
      _chkShowTourTags.setSelection(                  Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  TourBlogView.STATE_IS_SHOW_TOUR_TAGS_DEFAULT));
      _chkShowTourWeather.setSelection(               Util.getStateBoolean(_state, TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               TourBlogView.STATE_IS_SHOW_TOUR_WEATHER_DEFAULT));

      _spinnerFontSize.setSelection(                  Util.getStateInt(_state_WEB, WEB.STATE_BODY_FONT_SIZE, WEB.STATE_BODY_FONT_SIZE_DEFAULT));

// SET_FORMATTING_ON

      enableControls();
   }

   private void saveState() {

// SET_FORMATTING_OFF

      _state.put(TourBlogView.STATE_IS_DRAW_MARKER_WITH_DEFAULT_COLOR,  _chkDrawMarkerWithDefaultColor.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_HIDDEN_MARKER,              _chkShowHiddenMarker.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_MARKERS,               _chkShowTourMarkers.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_NUTRITION,             _chkShowTourNutrition.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_SUMMARY,               _chkShowTourSummary.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_TAGS,                  _chkShowTourTags.getSelection());
      _state.put(TourBlogView.STATE_IS_SHOW_TOUR_WEATHER,               _chkShowTourWeather.getSelection());

      _state_WEB.put(WEB.STATE_BODY_FONT_SIZE,                          _spinnerFontSize.getSelection());

// SET_FORMATTING_ON
   }

}
