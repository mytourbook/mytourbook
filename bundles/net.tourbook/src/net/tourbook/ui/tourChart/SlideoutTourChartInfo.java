/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
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
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartInfo extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final String     APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP  = net.tourbook.common.Messages.App_Theme_BackgroundColor_Dark_Tooltip;
   private static final String     APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP = net.tourbook.common.Messages.App_Theme_BackgroundColor_Light_Tooltip;

   private final IPreferenceStore  _prefStore                               = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener _defaultChangePropertyListener;
   private SelectionListener       _defaultSelectionListener;
   private MouseWheelListener      _defaultMouseWheelListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _defaultChangePropertyListener = propertyChangeEvent -> onChangeUI();
   }

   private PixelConverter        _pc;

   private ActionOpenPrefDialog  _actionPrefDialog;
   private ActionResetToDefaults _actionRestoreDefaults;

   /*
    * UI controls
    */
   private TourChart             _tourChart;

   private Button                _chkShowInfoTitle;
   private Button                _chkShowInfoTooltip;
   private Button                _chkShowInfoTourSeparator;
   private Button                _chkSegmentAlternateColor;

   private Label                 _lblTooltipDelay;

   private Spinner               _spinnerTooltipDelay;

   private ColorSelectorExtended _colorSegmentAlternateColor_Dark;
   private ColorSelectorExtended _colorSegmentAlternateColor_Light;

   public SlideoutTourChartInfo(final Control ownerControl,
                                final ToolBar toolBar,
                                final TourChart tourChart) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   protected boolean closeShellAfterHidden() {

      /*
       * Close the tooltip that the state is saved.
       */

      return true;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {

      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(Messages.Tour_Action_EditChartPreferences, PrefPageAppearanceTourChart.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_tourChart.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);
      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TourInfoOptions_Label_Title);

      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);
      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show tour title
             */
            _chkShowInfoTitle = new Button(container, SWT.CHECK);
            _chkShowInfoTitle.setText(Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourTitle);
            _chkShowInfoTitle.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowInfoTitle);
         }
         {
            /*
             * Show tour separator
             */
            _chkShowInfoTourSeparator = new Button(container, SWT.CHECK);
            _chkShowInfoTourSeparator.setText(Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourSeparator);
            _chkShowInfoTourSeparator.setToolTipText(Messages.Slideout_TourInfoOptions_Checkbox_IsShowTourSeparator_Tooltip);
            _chkShowInfoTourSeparator.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowInfoTourSeparator);
         }
         {
            /*
             * Segments with alternate colors
             */
            _chkSegmentAlternateColor = new Button(container, SWT.CHECK);
            _chkSegmentAlternateColor.setText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor);
            _chkSegmentAlternateColor.setToolTipText(Messages.Pref_Graphs_Checkbox_SegmentAlternateColor_Tooltip);
            _chkSegmentAlternateColor.addSelectionListener(_defaultSelectionListener);

            final Composite colorContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
            {
               // light color
               _colorSegmentAlternateColor_Light = new ColorSelectorExtended(colorContainer);
               _colorSegmentAlternateColor_Light.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP);
               _colorSegmentAlternateColor_Light.addListener(_defaultChangePropertyListener);
               _colorSegmentAlternateColor_Light.addOpenListener(this);

               // dark color
               _colorSegmentAlternateColor_Dark = new ColorSelectorExtended(colorContainer);
               _colorSegmentAlternateColor_Dark.getButton().setToolTipText(APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP);
               _colorSegmentAlternateColor_Dark.addListener(_defaultChangePropertyListener);
               _colorSegmentAlternateColor_Dark.addOpenListener(this);
            }
         }
         {
            /*
             * Show info tooltip
             */
            _chkShowInfoTooltip = new Button(container, SWT.CHECK);
            _chkShowInfoTooltip.setText(Messages.Slideout_TourInfoOptions_Checkbox_IsShowInfoTooltip);
            _chkShowInfoTooltip.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowInfoTooltip);
         }
         {
            /*
             * Tooltip delay
             */

            // Label
            _lblTooltipDelay = new Label(container, SWT.NONE);
            _lblTooltipDelay.setText(Messages.Slideout_TourInfoOptions_Label_TooltipDelay);
            _lblTooltipDelay.setToolTipText(Messages.Slideout_TourInfoOptions_Label_TooltipDelay_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(_pc.convertWidthInCharsToPixels(3), 0)
                  .applyTo(_lblTooltipDelay);

            // Spinner
            _spinnerTooltipDelay = new Spinner(container, SWT.BORDER);
            _spinnerTooltipDelay.setMinimum(0);
            _spinnerTooltipDelay.setMaximum(1000);
            _spinnerTooltipDelay.setPageIncrement(50);
            _spinnerTooltipDelay.addSelectionListener(_defaultSelectionListener);
            _spinnerTooltipDelay.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void enableControls() {

      final boolean isShowInfoTooltip = _chkShowInfoTooltip.getSelection();
      final boolean isShowSegmentAlternateColor = _chkSegmentAlternateColor.getSelection();

      _lblTooltipDelay.setEnabled(isShowInfoTooltip);
      _spinnerTooltipDelay.setEnabled(isShowInfoTooltip);

      _colorSegmentAlternateColor_Light.setEnabled(isShowSegmentAlternateColor);
      _colorSegmentAlternateColor_Dark.setEnabled(isShowSegmentAlternateColor);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   private void onChangeUI() {

      saveState();

      // update chart with new settings
      _tourChart.updateUI_TourTitleInfo();

      enableControls();
   }

   @Override
   public void resetToDefaults() {

      /*
       * Update UI with defaults from pref store
       */
      _chkShowInfoTitle.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TITLE_VISIBLE));
      _chkShowInfoTooltip.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE));
      _chkShowInfoTourSeparator.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE));

      _spinnerTooltipDelay.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_TOUR_INFO_TOOLTIP_DELAY));

      // segment alternate color
      _chkSegmentAlternateColor.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
      _colorSegmentAlternateColor_Light.setColorValue(
            PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));
      _colorSegmentAlternateColor_Dark.setColorValue(
            PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK));

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      _chkShowInfoTitle.setSelection(tcc.isShowInfoTitle);
      _chkShowInfoTooltip.setSelection(tcc.isShowInfoTooltip);
      _chkShowInfoTourSeparator.setSelection(tcc.isShowInfoTourSeparator);

      _spinnerTooltipDelay.setSelection(tcc.tourInfoTooltipDelay);

      // segment alternate color
      _chkSegmentAlternateColor.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR));
      _colorSegmentAlternateColor_Light.setColorValue(
            PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR));
      _colorSegmentAlternateColor_Dark.setColorValue(
            PreferenceConverter.getColor(_prefStore, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK));
   }

   private void saveState() {

      final boolean isShowInfoTitle = _chkShowInfoTitle.getSelection();
      final boolean isShowInfoTooltip = _chkShowInfoTooltip.getSelection();
      final boolean isShowInfoTourSeparator = _chkShowInfoTourSeparator.getSelection();
      final int tooltipDelay = _spinnerTooltipDelay.getSelection();

      /*
       * Update pref store
       */
      _prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TITLE_VISIBLE, isShowInfoTitle);
      _prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE, isShowInfoTooltip);
      _prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE, isShowInfoTourSeparator);
      _prefStore.setValue(ITourbookPreferences.GRAPH_TOUR_INFO_TOOLTIP_DELAY, tooltipDelay);

      // segment alternate color
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR, _chkSegmentAlternateColor.getSelection());
      PreferenceConverter.setValue(_prefStore,
            ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR,
            _colorSegmentAlternateColor_Light.getColorValue());
      PreferenceConverter.setValue(_prefStore,
            ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK,
            _colorSegmentAlternateColor_Dark.getColorValue());

      /*
       * Update chart config
       */
      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();
      tcc.isShowInfoTitle = isShowInfoTitle;
      tcc.isShowInfoTooltip = isShowInfoTooltip;
      tcc.isShowInfoTourSeparator = isShowInfoTourSeparator;
      tcc.tourInfoTooltipDelay = tooltipDelay;
   }

}
