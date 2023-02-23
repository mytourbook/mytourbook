/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.LinkedHashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.Map2_Appearance;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2D map track options
 */
class SlideoutMap2TrackOptions extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private IDialogSettings               _state;
   //
   private IPropertyChangeListener       _defaultChangePropertyListener;
   private MouseWheelListener            _defaultMouseWheelListener;
   private SelectionListener             _defaultSelectionListener;
   private SelectionListener             _defaultTrackOptions_SelectionListener;
   private MouseWheelListener            _defaultTrackOptions_MouseWheelListener;
   private IPropertyChangeListener       _defaultTrackOptions_ChangePropertyListener;
   private SelectionListener             _defaultMapOptions_SelectionListener;
   private MouseWheelListener            _defaultMapOptions_MouseWheelListener;
   private IPropertyChangeListener       _defaultMapOptions_ChangePropertyListener;
   //
   private ActionResetToDefaults         _actionRestoreDefaults;
   //
   private PixelConverter                _pc;
   private int                           _firstColumnIndent;
   //
   private GridDataFactory               _firstColoumLayoutData;
   private GridDataFactory               _secondColoumLayoutData;
   private GridDataFactory               _spinnerLayoutData;
   //
   private Map2View                      _map2View;
   //
   /*
    * UI controls
    */
   private Composite             _parent;
   private CTabFolder            _tabFolder;
   //
   private Button                _chkIsCutOffLinesInPauses;
   private Button                _chkIsAntialiasPainting;
   private Button                _chkPaintWithBorder;
   private Button                _chkShowBreadcrumbs;
   private Button                _chkShowEnhancedWarning;
   private Button                _chkShowHoveredSelectedTour;
   private Button                _chkShowSlider_Location;
   private Button                _chkShowSlider_Path;
   private Button                _chkShowTourDirections;
   private Button                _chkShowTourDirectionsAlways;
   private Button                _chkTrackOpacity;
   //
   private Button                _rdoBorderColorDarker;
   private Button                _rdoBorderColorColor;
   private Button                _rdoPainting_Simple;
   private Button                _rdoPainting_Complex;
   private Button                _rdoSymbolLine;
   private Button                _rdoSymbolDot;
   private Button                _rdoSymbolSquare;
   //
   private Label                 _lblBorder_Color;
   private Label                 _lblBorder_Width;
   private Label                 _lblBreadcrumbItems;
   private Label                 _lblHoveredSelected_HoveredColor;
   private Label                 _lblHoveredSelected_HoveredAndSelectedColor;
   private Label                 _lblHoveredSelected_SelectedColor;
   private Label                 _lblSliderPath_Color;
   private Label                 _lblSliderPath_Segments;
   private Label                 _lblSliderPath_Width;
   private Label                 _lblTourDirection_DistanceBetweenMarkers;
   private Label                 _lblTourDirection_LineWidth;
   private Label                 _lblTourDirection_SymbolColor;
   private Label                 _lblTourDirection_SymbolSize;
   //
   private Spinner               _spinnerBorder_ColorDarker;
   private Spinner               _spinnerBorder_Width;
   private Spinner               _spinnerBreadcrumbItems;
   private Spinner               _spinnerHoveredSelected_HoveredOpacity;
   private Spinner               _spinnerHoveredSelected_HoveredAndSelectedOpacity;
   private Spinner               _spinnerHoveredSelected_SelectedOpacity;
   private Spinner               _spinnerLineWidth;
   private Spinner               _spinnerSliderPath_Opacity;
   private Spinner               _spinnerSliderPath_Segments;
   private Spinner               _spinnerSliderPath_LineWidth;
   private Spinner               _spinnerTourDirection_MarkerGap;
   private Spinner               _spinnerTourDirection_LineWidth;
   private Spinner               _spinnerTourDirection_SymbolSize;
   private Spinner               _spinnerTrackOpacity;
   //
   private ColorSelectorExtended _colorBorderColor;
   private ColorSelectorExtended _colorHoveredSelected_Hovered;
   private ColorSelectorExtended _colorHoveredSelected_Selected;
   private ColorSelectorExtended _colorHoveredSelected_HoveredAndSelected;
   private ColorSelectorExtended _colorSliderPathColor;
   private ColorSelectorExtended _colorTourDirection_SymbolColor;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map2View
    * @param map2State
    */
   SlideoutMap2TrackOptions(final Control ownerControl,
                            final ToolBar toolBar,
                            final Map2View map2View,
                            final IDialogSettings map2State) {

      super(ownerControl, toolBar);

      _map2View = map2View;
      _state = map2State;
   }

   @Override
   protected void beforeHideToolTip() {

      saveUIState();
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {
      setIsAnotherDialogOpened(isAnotherDialogOpened);
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
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         createUI_010_Header(shellContainer);

         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .extendedMargins(5, 5, 0, 5)
               .applyTo(container);
         {
            _tabFolder = new CTabFolder(container, SWT.TOP);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);
            GridLayoutFactory.fillDefaults().applyTo(_tabFolder);

            {
               final CTabItem tabPainting = new CTabItem(_tabFolder, SWT.NONE);
               tabPainting.setText(Messages.Slideout_Map_Options_Tab_Painting);
               tabPainting.setControl(createUI_100_Tab_Painting(_tabFolder));

               final CTabItem tabSelection = new CTabItem(_tabFolder, SWT.NONE);
               tabSelection.setText(Messages.Slideout_Map_Options_Tab_Selection);
               tabSelection.setControl(createUI_200_Tab_Selection(_tabFolder));

               final CTabItem tabSlider = new CTabItem(_tabFolder, SWT.NONE);
               tabSlider.setText(Messages.Slideout_Map_Options_Tab_Slider);
               tabSlider.setControl(createUI_300_Tab_Slider(_tabFolder));
            }
         }
      }

      return shellContainer;
   }

   private void createUI_010_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .extendedMargins(5, 5, 5, 0)
            .numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         {
            /*
             * Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_Map_TrackOptions_Label_Title);
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

   private Composite createUI_100_Tab_Painting(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         createUI_110_PaintTrack(container);
         createUI_120_PaintBorder(container);
         createUI_130_PaintingMethod(container);
      }

      return container;
   }

   private void createUI_110_PaintTrack(final Composite parent) {

      {
         /*
          * Plot tour with
          */

         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_map_layout_symbol);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

         // radio
         final Composite radioContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(radioContainer);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(radioContainer);
         {
            // Radio: Line
            _rdoSymbolLine = new Button(radioContainer, SWT.RADIO);
            _rdoSymbolLine.setText(Messages.pref_map_layout_symbol_line);
            _rdoSymbolLine.addSelectionListener(_defaultSelectionListener);

            // Radio: Dot
            _rdoSymbolDot = new Button(radioContainer, SWT.RADIO);
            _rdoSymbolDot.setText(Messages.pref_map_layout_symbol_dot);
            _rdoSymbolDot.addSelectionListener(_defaultSelectionListener);

            // Radio: Squares
            _rdoSymbolSquare = new Button(radioContainer, SWT.RADIO);
            _rdoSymbolSquare.setText(Messages.pref_map_layout_symbol_square);
            _rdoSymbolSquare.addSelectionListener(_defaultSelectionListener);
         }
         {
            // Checkbox: Cut off lines in pauses
            _chkIsCutOffLinesInPauses = new Button(radioContainer, SWT.CHECK);
            _chkIsCutOffLinesInPauses.setText(Messages.Slideout_Map_Options_Checkbox_CutOffLinesInPauses);
            _chkIsCutOffLinesInPauses.setToolTipText(Messages.Slideout_Map_Options_Checkbox_CutOffLinesInPauses_Tooltip);
            _chkIsCutOffLinesInPauses.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(3, 1)
                  .indent(_firstColumnIndent, 0)
                  .applyTo(_chkIsCutOffLinesInPauses);

         }
      }
      {
         /*
          * Line width
          */
         // label: line width
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_map_layout_symbol_width);

         // spinner: line width
         _spinnerLineWidth = new Spinner(parent, SWT.BORDER);
         _spinnerLineWidth.setMinimum(1);
         _spinnerLineWidth.setMaximum(50);
         _spinnerLineWidth.setPageIncrement(5);

         _spinnerLineWidth.addSelectionListener(_defaultSelectionListener);
         _spinnerLineWidth.addMouseWheelListener(_defaultMouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerLineWidth);
      }
      {
         /*
          * Tour track opacity
          */
         final String tooltipText = NLS.bind(
               Messages.Slideout_Map_Options_Checkbox_TrackOpacity_Tooltip,
               UI.TRANSFORM_OPACITY_MAX);

         {
            // checkbox
            _chkTrackOpacity = new Button(parent, SWT.CHECK);
            _chkTrackOpacity.setText(Messages.Slideout_Map_Options_Checkbox_TrackOpacity);
            _chkTrackOpacity.setToolTipText(tooltipText);
            _chkTrackOpacity.addSelectionListener(_defaultSelectionListener);
         }
         {
            // spinner
            _spinnerTrackOpacity = new Spinner(parent, SWT.BORDER);
            _spinnerTrackOpacity.setToolTipText(tooltipText);
            _spinnerTrackOpacity.setMinimum((int) (UI.TRANSFORM_OPACITY_MAX * 0.2f)); // ensure that the track is visible
            _spinnerTrackOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerTrackOpacity.setIncrement(1);
            _spinnerTrackOpacity.setPageIncrement(10);
            _spinnerTrackOpacity.addSelectionListener(_defaultSelectionListener);
            _spinnerTrackOpacity.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerTrackOpacity);
         }
      }
      {
         /*
          * Antialias painting
          */
         {
            // checkbox
            _chkIsAntialiasPainting = new Button(parent, SWT.CHECK);
            _chkIsAntialiasPainting.setText(Messages.Slideout_Map_Options_Checkbox_AntialiasPainting);
            _chkIsAntialiasPainting.setToolTipText(Messages.Slideout_Map_Options_Checkbox_AntialiasPainting_Tooltip);
            _chkIsAntialiasPainting.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkIsAntialiasPainting);
         }
      }
   }

   private void createUI_120_PaintBorder(final Composite parent) {

      {
         /*
          * Checkbox: paint with border
          */
         _chkPaintWithBorder = new Button(parent, SWT.CHECK);
         _chkPaintWithBorder.setText(Messages.pref_map_layout_PaintBorder);
         _chkPaintWithBorder.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkPaintWithBorder);
      }
      {
         /*
          * Border width
          */

         // label
         _lblBorder_Width = new Label(parent, SWT.NONE);
         _lblBorder_Width.setText(Messages.pref_map_layout_BorderWidth);
         _firstColoumLayoutData.applyTo(_lblBorder_Width);

         // spinner
         _spinnerBorder_Width = new Spinner(parent, SWT.BORDER);
         _spinnerBorder_Width.setMinimum(1);
         _spinnerBorder_Width.setMaximum(30);
         _spinnerBorder_Width.addSelectionListener(_defaultSelectionListener);
         _spinnerBorder_Width.addMouseWheelListener(_defaultMouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerBorder_Width);
      }

      {
         /*
          * Border color
          */

         // label
         _lblBorder_Color = new Label(parent, SWT.NONE);
         _lblBorder_Color.setText(Messages.Pref_MapLayout_Label_BorderColor);
         _firstColoumLayoutData
               .align(SWT.FILL, SWT.BEGINNING)
               .applyTo(_lblBorder_Color);

         final Composite containerBorderColor = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(containerBorderColor);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBorderColor);
         {
            // Radio: darker
            _rdoBorderColorDarker = new Button(containerBorderColor, SWT.RADIO);
            _rdoBorderColorDarker.setText(Messages.Pref_MapLayout_Checkbox_BorderColor_Darker);
            _rdoBorderColorDarker.addSelectionListener(_defaultSelectionListener);

            // spinner: border width
            _spinnerBorder_ColorDarker = new Spinner(containerBorderColor, SWT.BORDER);
            _spinnerBorder_ColorDarker.setMinimum(0);
            _spinnerBorder_ColorDarker.setMaximum(100);
            _spinnerBorder_ColorDarker.setPageIncrement(10);
            _spinnerBorder_ColorDarker.addSelectionListener(_defaultSelectionListener);
            _spinnerBorder_ColorDarker.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerBorder_ColorDarker);

            // Radio: color
            _rdoBorderColorColor = new Button(containerBorderColor, SWT.RADIO);
            _rdoBorderColorColor.setText(Messages.Pref_MapLayout_Checkbox_BorderColor_Color);
            _rdoBorderColorColor.addSelectionListener(_defaultSelectionListener);

            // border color
            _colorBorderColor = new ColorSelectorExtended(containerBorderColor);
            _colorBorderColor.addListener(_defaultChangePropertyListener);
            _colorBorderColor.addOpenListener(this);
         }
      }
   }

   private void createUI_130_PaintingMethod(final Composite parent) {

      {
         /*
          * Radio: Tour painting method
          */
         // label
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_MapLayout_Label_TourPaintMethod);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

         final Composite paintingContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(paintingContainer);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(paintingContainer);
         {
            // Radio: Simple
            _rdoPainting_Simple = new Button(paintingContainer, SWT.RADIO);
            _rdoPainting_Simple.setText(Messages.Pref_MapLayout_Label_TourPaintMethod_Simple);
            _rdoPainting_Simple.setToolTipText(Messages.Pref_MapLayout_Label_TourPaintMethod_Simple_Tooltip);
            _rdoPainting_Simple.addSelectionListener(_defaultSelectionListener);

            // Radio: Enhanced
            _rdoPainting_Complex = new Button(paintingContainer, SWT.RADIO);
            _rdoPainting_Complex.setText(Messages.Pref_MapLayout_Label_TourPaintMethod_Complex);
            _rdoPainting_Complex.setToolTipText(Messages.Pref_MapLayout_Label_TourPaintMethod_Complex_Tooltip);
            _rdoPainting_Complex.addSelectionListener(_defaultSelectionListener);

            // Checkbox: Show warning
            _chkShowEnhancedWarning = new Button(paintingContainer, SWT.CHECK);
            _chkShowEnhancedWarning.setText(Messages.Slideout_Map_Options_Checkbox_ShowEnhancedWarning);
            _chkShowEnhancedWarning.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowEnhancedWarning_Tooltip);
            _chkShowEnhancedWarning.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(_firstColumnIndent, 0)
                  .applyTo(_chkShowEnhancedWarning);
         }
      }
   }

   private Composite createUI_200_Tab_Selection(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         createUI_210_HoveredSelection(container);
         createUI_220_Breadcrumb(container);
         createUI_230_TourDirection(container);
      }

      return container;
   }

   private void createUI_210_HoveredSelection(final Composite parent) {

      {
         /*
          * Show hovered/selected tour
          */
         _chkShowHoveredSelectedTour = new Button(parent, SWT.CHECK);
         _chkShowHoveredSelectedTour.setText(Messages.Slideout_Map_Options_Checkbox_ShowHoveredSelectedTour);
         _chkShowHoveredSelectedTour.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowHoveredSelectedTour_Tooltip);
         _chkShowHoveredSelectedTour.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onChangeUI_ShowHoveredTour()));
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowHoveredSelectedTour);
      }
      {
         /*
          * Hovered color
          */

         final String tooltipText = NLS.bind(
               Messages.Slideout_Map_Options_Label_HoveredColor_Tooltip,
               UI.TRANSFORM_OPACITY_MAX);

         // label
         _lblHoveredSelected_HoveredColor = new Label(parent, SWT.NONE);
         _lblHoveredSelected_HoveredColor.setText(Messages.Slideout_Map_Options_Label_HoveredColor);
         _lblHoveredSelected_HoveredColor.setToolTipText(tooltipText);
         _firstColoumLayoutData.span(1, 1).applyTo(_lblHoveredSelected_HoveredColor);

         final Composite colorContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
         {
            // opacity
            _spinnerHoveredSelected_HoveredOpacity = new Spinner(colorContainer, SWT.BORDER);
            _spinnerHoveredSelected_HoveredOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerHoveredSelected_HoveredOpacity.setToolTipText(tooltipText);
            _spinnerHoveredSelected_HoveredOpacity.addSelectionListener(_defaultMapOptions_SelectionListener);
            _spinnerHoveredSelected_HoveredOpacity.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerHoveredSelected_HoveredOpacity);

            // color
            _colorHoveredSelected_Hovered = new ColorSelectorExtended(colorContainer);
            _colorHoveredSelected_Hovered.setToolTipText(tooltipText);
            _colorHoveredSelected_Hovered.addListener(_defaultMapOptions_ChangePropertyListener);
            _colorHoveredSelected_Hovered.addOpenListener(this);
         }
      }
      {
         /*
          * Selected color
          */

         final String tooltipText = NLS.bind(
               Messages.Slideout_Map_Options_Label_SelectedColor_Tooltip,
               UI.TRANSFORM_OPACITY_MAX);

         // label
         _lblHoveredSelected_SelectedColor = new Label(parent, SWT.NONE);
         _lblHoveredSelected_SelectedColor.setText(Messages.Slideout_Map_Options_Label_SelectedColor);
         _lblHoveredSelected_SelectedColor.setToolTipText(tooltipText);
         _firstColoumLayoutData.span(1, 1).applyTo(_lblHoveredSelected_SelectedColor);

         final Composite colorContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
         {
            // opacity
            _spinnerHoveredSelected_SelectedOpacity = new Spinner(colorContainer, SWT.BORDER);
            _spinnerHoveredSelected_SelectedOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerHoveredSelected_SelectedOpacity.setToolTipText(tooltipText);
            _spinnerHoveredSelected_SelectedOpacity.addSelectionListener(_defaultMapOptions_SelectionListener);
            _spinnerHoveredSelected_SelectedOpacity.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerHoveredSelected_SelectedOpacity);

            // color
            _colorHoveredSelected_Selected = new ColorSelectorExtended(colorContainer);
            _colorHoveredSelected_Selected.setToolTipText(tooltipText);
            _colorHoveredSelected_Selected.addListener(_defaultMapOptions_ChangePropertyListener);
            _colorHoveredSelected_Selected.addOpenListener(this);
         }
      }
      {
         /*
          * Hovered + selected color
          */

         final String tooltipText = NLS.bind(
               Messages.Slideout_Map_Options_Label_HoveredAndSelectedColor_Tooltip,
               UI.TRANSFORM_OPACITY_MAX);

         // label
         _lblHoveredSelected_HoveredAndSelectedColor = new Label(parent, SWT.NONE);
         _lblHoveredSelected_HoveredAndSelectedColor.setText(Messages.Slideout_Map_Options_Label_HoveredAndSelectedColor);
         _lblHoveredSelected_HoveredAndSelectedColor.setToolTipText(tooltipText);
         _firstColoumLayoutData.span(1, 1).applyTo(_lblHoveredSelected_HoveredAndSelectedColor);

         final Composite colorContainer = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
         {
            // opacity
            _spinnerHoveredSelected_HoveredAndSelectedOpacity = new Spinner(colorContainer, SWT.BORDER);
            _spinnerHoveredSelected_HoveredAndSelectedOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
            _spinnerHoveredSelected_HoveredAndSelectedOpacity.setToolTipText(tooltipText);
            _spinnerHoveredSelected_HoveredAndSelectedOpacity.addSelectionListener(_defaultMapOptions_SelectionListener);
            _spinnerHoveredSelected_HoveredAndSelectedOpacity.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerHoveredSelected_HoveredAndSelectedOpacity);

            // color
            _colorHoveredSelected_HoveredAndSelected = new ColorSelectorExtended(colorContainer);
            _colorHoveredSelected_HoveredAndSelected.setToolTipText(tooltipText);
            _colorHoveredSelected_HoveredAndSelected.addListener(_defaultMapOptions_ChangePropertyListener);
            _colorHoveredSelected_HoveredAndSelected.addOpenListener(this);
         }
      }
   }

   /**
    * Breadcrumb history
    *
    * @param parent
    */
   private void createUI_220_Breadcrumb(final Composite parent) {

      {
         /*
          * Show breadcrumbs
          */
         _chkShowBreadcrumbs = new Button(parent, SWT.CHECK);
         _chkShowBreadcrumbs.setText(Messages.Slideout_Map_Options_Checkbox_ShowBreadcrumbs);
         _chkShowBreadcrumbs.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowBreadcrumbs_Tooltip);
         _chkShowBreadcrumbs.addSelectionListener(_defaultMapOptions_SelectionListener);
         _firstColoumLayoutData.span(2, 1).applyTo(_chkShowBreadcrumbs);
      }
      {
         /*
          * Number of crumbs
          */

         // label
         _lblBreadcrumbItems = new Label(parent, SWT.NONE);
         _lblBreadcrumbItems.setText(Messages.Slideout_Map_Options_Label_BreadcrumbItems);
         _lblBreadcrumbItems.setToolTipText(Messages.Slideout_Map_Options_Label_BreadcrumbItems_Tooltip);
         _secondColoumLayoutData.span(1, 1).applyTo(_lblBreadcrumbItems);

         // number of crumbs
         _spinnerBreadcrumbItems = new Spinner(parent, SWT.BORDER);
         _spinnerBreadcrumbItems.setMinimum(0); // 0 will hide the bread crumb
         _spinnerBreadcrumbItems.setMaximum(99);
         _spinnerBreadcrumbItems.setToolTipText(Messages.Slideout_Map_Options_Label_BreadcrumbItems_Tooltip);
         _spinnerBreadcrumbItems.addSelectionListener(_defaultMapOptions_SelectionListener);
         _spinnerBreadcrumbItems.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerBreadcrumbItems);
      }
   }

   private void createUI_230_TourDirection(final Composite parent) {

      {
         /*
          * Show tour direction
          */
         _chkShowTourDirections = new Button(parent, SWT.CHECK);
         _chkShowTourDirections.setText(Messages.Slideout_Map_Options_Checkbox_ShowTourDirection);
         _chkShowTourDirections.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowTourDirection_Tooltip);
         _chkShowTourDirections.addSelectionListener(_defaultMapOptions_SelectionListener);
         _firstColoumLayoutData.span(2, 1).applyTo(_chkShowTourDirections);
      }
      {
         /*
          * Show tour direction always
          */
         _chkShowTourDirectionsAlways = new Button(parent, SWT.CHECK);
         _chkShowTourDirectionsAlways.setText(Messages.Slideout_Map_Options_Checkbox_ShowTourDirection_Always);
         _chkShowTourDirectionsAlways.setToolTipText(Messages.Slideout_Map_Options_Checkbox_ShowTourDirection_Always_Tooltip);
         _chkShowTourDirectionsAlways.addSelectionListener(_defaultMapOptions_SelectionListener);
         _secondColoumLayoutData.span(2, 1).applyTo(_chkShowTourDirectionsAlways);
      }

      {
         /*
          * Symbol size
          */

         // label
         _lblTourDirection_SymbolSize = new Label(parent, SWT.NONE);
         _lblTourDirection_SymbolSize.setText(Messages.Slideout_Map_Options_Label_TourDirection_SymbolSize);
         _secondColoumLayoutData.span(1, 1).applyTo(_lblTourDirection_SymbolSize);

         // spinner
         _spinnerTourDirection_SymbolSize = new Spinner(parent, SWT.BORDER);
         _spinnerTourDirection_SymbolSize.setMinimum(1);
         _spinnerTourDirection_SymbolSize.setMaximum(50);
         _spinnerTourDirection_SymbolSize.setPageIncrement(5);
         _spinnerTourDirection_SymbolSize.addSelectionListener(_defaultMapOptions_SelectionListener);
         _spinnerTourDirection_SymbolSize.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerTourDirection_SymbolSize);
      }
      {
         /*
          * Line width
          */

         // label
         _lblTourDirection_LineWidth = new Label(parent, SWT.NONE);
         _lblTourDirection_LineWidth.setText(Messages.Slideout_Map_Options_Label_TourDirection_LineWidth);
         _secondColoumLayoutData.span(1, 1).applyTo(_lblTourDirection_LineWidth);

         // spinner
         _spinnerTourDirection_LineWidth = new Spinner(parent, SWT.BORDER);
         _spinnerTourDirection_LineWidth.setMinimum(1);
         _spinnerTourDirection_LineWidth.setMaximum(50);
         _spinnerTourDirection_LineWidth.setPageIncrement(5);
         _spinnerTourDirection_LineWidth.addSelectionListener(_defaultMapOptions_SelectionListener);
         _spinnerTourDirection_LineWidth.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerTourDirection_LineWidth);
      }
      {
         /*
          * Distance between markers
          */

         // label
         _lblTourDirection_DistanceBetweenMarkers = new Label(parent, SWT.NONE);
         _lblTourDirection_DistanceBetweenMarkers.setText(Messages.Slideout_Map_Options_Label_TourDirection_DistanceBetweenMarkers);
         _secondColoumLayoutData.span(1, 1).applyTo(_lblTourDirection_DistanceBetweenMarkers);

         // spinner
         _spinnerTourDirection_MarkerGap = new Spinner(parent, SWT.BORDER);
         _spinnerTourDirection_MarkerGap.setMinimum(5);
         _spinnerTourDirection_MarkerGap.setMaximum(200);
         _spinnerTourDirection_MarkerGap.setPageIncrement(10);
         _spinnerTourDirection_MarkerGap.addSelectionListener(_defaultMapOptions_SelectionListener);
         _spinnerTourDirection_MarkerGap.addMouseWheelListener(_defaultMapOptions_MouseWheelListener);
         _spinnerLayoutData.applyTo(_spinnerTourDirection_MarkerGap);
      }
      {
         /*
          * Color
          */

         // label
         _lblTourDirection_SymbolColor = new Label(parent, SWT.NONE);
         _lblTourDirection_SymbolColor.setText(Messages.Slideout_Map_Options_Label_TourDirection_SymbolColor);
         _secondColoumLayoutData.span(1, 1).applyTo(_lblTourDirection_SymbolColor);

         // color
         _colorTourDirection_SymbolColor = new ColorSelectorExtended(parent);
         _colorTourDirection_SymbolColor.addListener(_defaultMapOptions_ChangePropertyListener);
         _colorTourDirection_SymbolColor.addOpenListener(this);

      }
   }

   private Composite createUI_300_Tab_Slider(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show chart slider
             */
            // checkbox
            _chkShowSlider_Location = new Button(container, SWT.CHECK);
            _chkShowSlider_Location.setText(Messages.Slideout_Map_Options_Checkbox_ChartSlider);
            _chkShowSlider_Location.addSelectionListener(_defaultTrackOptions_SelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowSlider_Location);
         }
         {
            /*
             * Show chart slider path
             */
            // checkbox
            _chkShowSlider_Path = new Button(container, SWT.CHECK);
            _chkShowSlider_Path.setText(Messages.Slideout_Map_Options_Checkbox_SliderPath);
            _chkShowSlider_Path.setToolTipText(Messages.Slideout_Map_Options_Checkbox_SliderPath_Tooltip);
            _chkShowSlider_Path.addSelectionListener(_defaultTrackOptions_SelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowSlider_Path);
         }

         {
            /*
             * Number of segments
             */

            // label
            _lblSliderPath_Segments = new Label(container, SWT.NONE);
            _lblSliderPath_Segments.setText(Messages.Slideout_Map_Options_Label_SliderPath_Segments);
            _firstColoumLayoutData.span(1, 1).applyTo(_lblSliderPath_Segments);

            // spinner
            _spinnerSliderPath_Segments = new Spinner(container, SWT.BORDER);
            _spinnerSliderPath_Segments.setMinimum(1);
            _spinnerSliderPath_Segments.setMaximum(10000);
            _spinnerSliderPath_Segments.setIncrement(1);
            _spinnerSliderPath_Segments.setPageIncrement(10);
            _spinnerSliderPath_Segments.addSelectionListener(_defaultTrackOptions_SelectionListener);
            _spinnerSliderPath_Segments.addMouseWheelListener(_defaultTrackOptions_MouseWheelListener);
         }
         {
            /*
             * Line width
             */

            // label
            _lblSliderPath_Width = new Label(container, SWT.NONE);
            _lblSliderPath_Width.setText(Messages.Slideout_Map_Options_Label_SliderPath_Width);
            _firstColoumLayoutData.applyTo(_lblSliderPath_Width);

            // spinner
            _spinnerSliderPath_LineWidth = new Spinner(container, SWT.BORDER);
            _spinnerSliderPath_LineWidth.setMinimum(1);
            _spinnerSliderPath_LineWidth.setMaximum(200);
            _spinnerSliderPath_LineWidth.setIncrement(1);
            _spinnerSliderPath_LineWidth.setPageIncrement(10);
            _spinnerSliderPath_LineWidth.addSelectionListener(_defaultTrackOptions_SelectionListener);
            _spinnerSliderPath_LineWidth.addMouseWheelListener(_defaultTrackOptions_MouseWheelListener);
            _spinnerLayoutData.applyTo(_spinnerSliderPath_LineWidth);
         }
         {
            /*
             * Color + opacity
             */

            final String tooltipText = NLS.bind(
                  Messages.Slideout_Map_Options_Label_SliderPath_Color_Tooltip,
                  UI.TRANSFORM_OPACITY_MAX);

            // label
            _lblSliderPath_Color = new Label(container, SWT.NONE);
            _lblSliderPath_Color.setText(Messages.Slideout_Map_Options_Label_SliderPath_Color);
            _firstColoumLayoutData.applyTo(_lblSliderPath_Color);

            final Composite colorContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
            {
               // color
               _colorSliderPathColor = new ColorSelectorExtended(colorContainer);
               _colorSliderPathColor.addListener(_defaultTrackOptions_ChangePropertyListener);
               _colorSliderPathColor.addOpenListener(this);

               // opacity
               _spinnerSliderPath_Opacity = new Spinner(colorContainer, SWT.BORDER);
               _spinnerSliderPath_Opacity.setToolTipText(tooltipText);
               _spinnerSliderPath_Opacity.setMinimum((int) (UI.TRANSFORM_OPACITY_MAX * 0.2f));
               _spinnerSliderPath_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
               _spinnerSliderPath_Opacity.setIncrement(1);
               _spinnerSliderPath_Opacity.setPageIncrement(10);
               _spinnerSliderPath_Opacity.addSelectionListener(_defaultTrackOptions_SelectionListener);
               _spinnerSliderPath_Opacity.addMouseWheelListener(_defaultTrackOptions_MouseWheelListener);
               _spinnerLayoutData.applyTo(_spinnerSliderPath_Opacity);
            }
         }
      }

      return container;
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isUseTrackOpacity              = _chkTrackOpacity.getSelection();
      final boolean isShowSliderPath               = _chkShowSlider_Path.getSelection();
      final boolean isPaintWithBorder              = _chkPaintWithBorder.getSelection();
      final boolean isCutOffLinesInPauses          = _rdoSymbolLine.getSelection();
      final boolean isEnhancedPaintingMode         = _rdoPainting_Complex.getSelection();

      final boolean isHoveredSelected              = _chkShowHoveredSelectedTour.getSelection();
      final boolean isShowTourDirection            = _chkShowTourDirections.getSelection() && isHoveredSelected;
      final boolean isShowBreadcrumbs              = _chkShowBreadcrumbs.getSelection() && isHoveredSelected;

      final boolean isBasicPaintingMode            = isEnhancedPaintingMode == false;
      final boolean isHoveredSelectedAndBasicMode  = isHoveredSelected && isBasicPaintingMode;

      _chkIsCutOffLinesInPauses     .setEnabled(isCutOffLinesInPauses);
      _spinnerTrackOpacity          .setEnabled(isUseTrackOpacity);

      // border
      _lblBorder_Color              .setEnabled(isPaintWithBorder);
      _lblBorder_Width              .setEnabled(isPaintWithBorder);
      _colorBorderColor             .setEnabled(isPaintWithBorder);
      _rdoBorderColorColor          .setEnabled(isPaintWithBorder);
      _rdoBorderColorDarker         .setEnabled(isPaintWithBorder);
      _spinnerBorder_ColorDarker    .setEnabled(isPaintWithBorder);
      _spinnerBorder_Width          .setEnabled(isPaintWithBorder);

      // slider path
      _colorSliderPathColor         .setEnabled(isShowSliderPath);
      _lblSliderPath_Color          .setEnabled(isShowSliderPath);
      _lblSliderPath_Width          .setEnabled(isShowSliderPath);
      _lblSliderPath_Segments       .setEnabled(isShowSliderPath);
      _spinnerSliderPath_Opacity    .setEnabled(isShowSliderPath);
      _spinnerSliderPath_Segments   .setEnabled(isShowSliderPath);
      _spinnerSliderPath_LineWidth  .setEnabled(isShowSliderPath);

      // painting method
      _chkShowEnhancedWarning       .setEnabled(isEnhancedPaintingMode);

      // hovered + selected
      _chkShowHoveredSelectedTour                        .setEnabled(isBasicPaintingMode);
      _lblHoveredSelected_HoveredColor                   .setEnabled(isHoveredSelectedAndBasicMode);
      _lblHoveredSelected_HoveredAndSelectedColor        .setEnabled(isHoveredSelectedAndBasicMode);
      _lblHoveredSelected_SelectedColor                  .setEnabled(isHoveredSelectedAndBasicMode);
      _colorHoveredSelected_Hovered                      .setEnabled(isHoveredSelectedAndBasicMode);
      _colorHoveredSelected_HoveredAndSelected           .setEnabled(isHoveredSelectedAndBasicMode);
      _colorHoveredSelected_Selected                     .setEnabled(isHoveredSelectedAndBasicMode);
      _spinnerHoveredSelected_HoveredOpacity             .setEnabled(isHoveredSelectedAndBasicMode);
      _spinnerHoveredSelected_HoveredAndSelectedOpacity  .setEnabled(isHoveredSelectedAndBasicMode);
      _spinnerHoveredSelected_SelectedOpacity            .setEnabled(isHoveredSelectedAndBasicMode);

      // breadcrumbs
      _chkShowBreadcrumbs                       .setEnabled(isHoveredSelectedAndBasicMode);
      _lblBreadcrumbItems                       .setEnabled(isShowBreadcrumbs && isHoveredSelectedAndBasicMode);
      _spinnerBreadcrumbItems                   .setEnabled(isShowBreadcrumbs && isHoveredSelectedAndBasicMode);

      // tour direction
      _chkShowTourDirections                    .setEnabled(isHoveredSelectedAndBasicMode);
      _chkShowTourDirectionsAlways              .setEnabled(isHoveredSelectedAndBasicMode &&  isShowTourDirection);
      _colorTourDirection_SymbolColor           .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _lblTourDirection_DistanceBetweenMarkers  .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _lblTourDirection_LineWidth               .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _lblTourDirection_SymbolColor             .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _lblTourDirection_SymbolSize              .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _spinnerTourDirection_MarkerGap           .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _spinnerTourDirection_LineWidth           .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);
      _spinnerTourDirection_SymbolSize          .setEnabled(isShowTourDirection && isHoveredSelectedAndBasicMode);

// SET_FORMATTING_ON
   }

   private int getBorderType() {

      final int borderType = _rdoBorderColorColor.getSelection()

            ? Map2_Appearance.BORDER_TYPE_COLOR
            : _rdoBorderColorDarker.getSelection()

                  ? Map2_Appearance.BORDER_TYPE_DARKER
                  : Map2_Appearance.DEFAULT_BORDER_TYPE;

      return borderType;
   }

   private String getPlotType() {

      final String plotType;

      if (_rdoSymbolDot.getSelection()) {
         plotType = Map2_Appearance.PLOT_TYPE_DOT;
      } else if (_rdoSymbolLine.getSelection()) {
         plotType = Map2_Appearance.PLOT_TYPE_LINE;
      } else if (_rdoSymbolSquare.getSelection()) {
         plotType = Map2_Appearance.PLOT_TYPE_SQUARE;
      } else {
         plotType = Map2_Appearance.DEFAULT_PLOT_TYPE;
      }

      return plotType;
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      // force width for the spinner controls
      _spinnerLayoutData = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT);

      _firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

      _firstColoumLayoutData = GridDataFactory.fillDefaults()
            .indent(_firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);

      _secondColoumLayoutData = GridDataFactory.fillDefaults()
            .indent(2 * _firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
      _defaultChangePropertyListener = propertyChangeEvent -> onChangeUI();

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      /*
       * Track options
       */
      _defaultTrackOptions_ChangePropertyListener = propertyChangeEvent -> onChangeUI_UpdateMap_TrackOptions();
      _defaultTrackOptions_SelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI_UpdateMap_TrackOptions());

      _defaultTrackOptions_MouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI_UpdateMap_TrackOptions();
      };

      /*
       * Map options
       */
      _defaultMapOptions_ChangePropertyListener = propertyChangeEvent -> onChangeUI_UpdateMap_MapOptions();
      _defaultMapOptions_SelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI_UpdateMap_MapOptions());

      _defaultMapOptions_MouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI_UpdateMap_MapOptions();
      };
   }

   private void onChangeUI() {

      saveState();
      enableControls();

      // fire one event for all modifications
      _prefStore.setValue(ITourbookPreferences.MAP2_OPTIONS_IS_MODIFIED, Math.random());
   }

   private void onChangeUI_ShowHoveredTour() {

      if (_chkShowHoveredSelectedTour.getSelection()) {

         // get current painting method
         final String prefPaintingMethod = _prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
         final boolean isEnhancedPainting = Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(prefPaintingMethod);

         if (isEnhancedPainting) {

            // show warning that enhanced painting mode is selected

            final LinkedHashMap<String, Integer> buttonLabelToIdMap = new LinkedHashMap<>();
            buttonLabelToIdMap.put(Messages.Slideout_Map2MapOptions_Action_SetTourPaintingModeBasic, IDialogConstants.OK_ID);
            buttonLabelToIdMap.put(Messages.App_Action_Cancel, IDialogConstants.CANCEL_ID);

            final MessageDialog dialog = new MessageDialog(

                  _parent.getShell(),

                  Messages.Slideout_Map2MapOptions_Dialog_EnhancePaintingWarning_Title,
                  null,

                  Messages.Slideout_Map2MapOptions_Dialog_EnhancePaintingWarning_Message,
                  MessageDialog.INFORMATION,

                  // default index
                  0,

                  Messages.Slideout_Map2MapOptions_Action_SetTourPaintingModeBasic,
                  Messages.App_Action_Cancel);

            setIsAnotherDialogOpened(true);
            {
               final int choice = dialog.open();

               if (choice == IDialogConstants.OK_ID) {

                  // update UI
                  _rdoPainting_Simple.setSelection(true);
                  _rdoPainting_Complex.setSelection(false);

                  // set painting method to basic
                  _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,
                        Map2_Appearance.TOUR_PAINT_METHOD_SIMPLE);
               }
            }
            setIsAnotherDialogOpened(false);
         }
      }

      onChangeUI_UpdateMap_MapOptions();
   }

   private void onChangeUI_UpdateMap_MapOptions() {

      saveState();
      enableControls();

      _map2View.restoreState_Map2_Options();
   }

   private void onChangeUI_UpdateMap_TrackOptions() {

      saveState();
      enableControls();

      _map2View.restoreState_Map2_TrackOptions(true);
   }

// SET_FORMATTING_OFF

   @Override
   public void resetToDefaults() {

      final int trackOpacity = UI.transformOpacity_WhenRestored(_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));

      _tabFolder.setSelection(0);

      // hovered/selected tour
      _chkShowHoveredSelectedTour.setSelection(                         Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT);
      _spinnerHoveredSelected_HoveredOpacity.setSelection(              Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT);
      _spinnerHoveredSelected_HoveredAndSelectedOpacity.setSelection(   Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT);
      _spinnerHoveredSelected_SelectedOpacity.setSelection(             Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT);
      _colorHoveredSelected_Hovered.setColorValue(                      Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT);
      _colorHoveredSelected_HoveredAndSelected.setColorValue(           Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT);
      _colorHoveredSelected_Selected.setColorValue(                     Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT);

      // breadcrumbs
      _chkShowBreadcrumbs.setSelection(               Map2View.STATE_IS_SHOW_BREADCRUMBS_DEFAULT);
      _spinnerBreadcrumbItems.setSelection(           Map2View.STATE_VISIBLE_BREADCRUMBS_DEFAULT);

      // tour direction
      _chkShowTourDirections.setSelection(            Map2View.STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT);
      _chkShowTourDirectionsAlways.setSelection(      Map2View.STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS_DEFAULT);
      _spinnerTourDirection_LineWidth.setSelection(   Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT);
      _spinnerTourDirection_MarkerGap.setSelection(   Map2View.STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT);
      _spinnerTourDirection_SymbolSize.setSelection(  Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT);
      _colorTourDirection_SymbolColor.setColorValue(  Map2View.STATE_TOUR_DIRECTION_RGB_DEFAULT);

      // slider location
      _chkShowSlider_Location.setSelection(           Map2View.STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT);

      // slider path
      _chkShowSlider_Path.setSelection(               Map2View.STATE_IS_SHOW_SLIDER_PATH_DEFAULT);
      _spinnerSliderPath_Opacity.setSelection(        Map2View.STATE_SLIDER_PATH_OPACITY_DEFAULT);
      _spinnerSliderPath_Segments.setSelection(       Map2View.STATE_SLIDER_PATH_SEGMENTS_DEFAULT);
      _spinnerSliderPath_LineWidth.setSelection(      Map2View.STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT);
      _colorSliderPathColor.setColorValue(            Map2View.STATE_SLIDER_PATH_COLOR_DEFAULT);

      // track opacity
      _chkTrackOpacity.setSelection(            _prefStore.getDefaultBoolean( ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
      _spinnerTrackOpacity.setSelection(        trackOpacity);

      // plot type
      updateUI_SetPlotType(                     _prefStore.getDefaultString(  ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE));
      _chkIsCutOffLinesInPauses.setSelection(   _prefStore.getDefaultBoolean( ITourbookPreferences.MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES));

      // line
      _chkIsAntialiasPainting.setSelection(     _prefStore.getDefaultBoolean( ITourbookPreferences.MAP_LAYOUT_IS_ANTIALIAS_PAINTING));
      _spinnerLineWidth.setSelection(           _prefStore.getDefaultInt(     ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));

      // border
      _chkPaintWithBorder.setSelection(         _prefStore.getDefaultBoolean( ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER));
      _spinnerBorder_Width.setSelection(        _prefStore.getDefaultInt(     ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));
      _spinnerBorder_ColorDarker.setSelection(  _prefStore.getDefaultInt(     ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE));
      updateUI_SetBorderType(                   _prefStore.getDefaultInt(     ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE));

      _colorBorderColor.setColorValue(PreferenceConverter.getDefaultColor(_prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR));

      // painting method
      final String paintingMethod =             _prefStore.getDefaultString(  ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD);
      _chkShowEnhancedWarning.setSelection(     _prefStore.getDefaultBoolean( ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING));
      _rdoPainting_Simple.setSelection(         Map2_Appearance.TOUR_PAINT_METHOD_SIMPLE.equals(paintingMethod));
      _rdoPainting_Complex.setSelection(        Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(paintingMethod));

      onChangeUI_UpdateMap_MapOptions();
      onChangeUI_UpdateMap_TrackOptions();

      onChangeUI();
   }


   private void restoreState() {

      final int hoveredOpacity               = UI.transformOpacity_WhenRestored(Util.getStateInt(_state, Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY,              Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY_DEFAULT));
      final int hoveredAndSelectedOpacity    = UI.transformOpacity_WhenRestored(Util.getStateInt(_state, Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY, Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY_DEFAULT));
      final int selectedOpacity              = UI.transformOpacity_WhenRestored(Util.getStateInt(_state, Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY,             Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY_DEFAULT));
      final int sliderPathOpacity            = UI.transformOpacity_WhenRestored(Util.getStateInt(_state, Map2View.STATE_SLIDER_PATH_OPACITY,                            Map2View.STATE_SLIDER_PATH_OPACITY_DEFAULT));

      final int trackOpacity                 = UI.transformOpacity_WhenRestored(_prefStore.getInt(      ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));

      _tabFolder.setSelection(                     Util.getStateInt(_state,         Map2View.STATE_TRACK_OPTIONS_SELECTED_TAB,   0));

      // hovered/selected tour
      _chkShowHoveredSelectedTour.setSelection(                         Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR,                     Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR_DEFAULT));
      _spinnerHoveredSelected_HoveredOpacity.setSelection(              hoveredOpacity);
      _spinnerHoveredSelected_HoveredAndSelectedOpacity.setSelection(   hoveredAndSelectedOpacity);
      _spinnerHoveredSelected_SelectedOpacity.setSelection(             selectedOpacity);
      _colorHoveredSelected_Hovered.setColorValue(                      Util.getStateRGB(_state,      Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB,                     Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB_DEFAULT));
      _colorHoveredSelected_HoveredAndSelected.setColorValue(           Util.getStateRGB(_state,      Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB,        Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB_DEFAULT));
      _colorHoveredSelected_Selected.setColorValue(                     Util.getStateRGB(_state,      Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB,                    Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB_DEFAULT));

      // breadcrumbs
      _chkShowBreadcrumbs.setSelection(                                 Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_BREADCRUMBS,                               Map2View.STATE_IS_SHOW_BREADCRUMBS_DEFAULT));
      _spinnerBreadcrumbItems.setSelection(                             Util.getStateInt(_state,      Map2View.STATE_VISIBLE_BREADCRUMBS,                               Map2View.STATE_VISIBLE_BREADCRUMBS_DEFAULT));

      // tour direction
      _chkShowTourDirections.setSelection(            Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_TOUR_DIRECTION,          Map2View.STATE_IS_SHOW_TOUR_DIRECTION_DEFAULT));
      _chkShowTourDirectionsAlways.setSelection(      Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS,   Map2View.STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS_DEFAULT));
      _spinnerTourDirection_LineWidth.setSelection(   Util.getStateInt(_state,      Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH,       Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH_DEFAULT));
      _spinnerTourDirection_MarkerGap.setSelection(   Util.getStateInt(_state,      Map2View.STATE_TOUR_DIRECTION_MARKER_GAP,       Map2View.STATE_TOUR_DIRECTION_MARKER_GAP_DEFAULT));
      _spinnerTourDirection_SymbolSize.setSelection(  Util.getStateInt(_state,      Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE,      Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE_DEFAULT));
      _colorTourDirection_SymbolColor.setColorValue(  Util.getStateRGB(_state,      Map2View.STATE_TOUR_DIRECTION_RGB,              Map2View.STATE_TOUR_DIRECTION_RGB_DEFAULT));

      // slider location
      _chkShowSlider_Location.setSelection(           Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_SLIDER_IN_MAP,  Map2View.STATE_IS_SHOW_SLIDER_IN_MAP_DEFAULT));

      // slider path
      _chkShowSlider_Path.setSelection(               Util.getStateBoolean(_state,  Map2View.STATE_IS_SHOW_SLIDER_PATH,    Map2View.STATE_IS_SHOW_SLIDER_PATH_DEFAULT));
      _spinnerSliderPath_Opacity.setSelection(        sliderPathOpacity);
      _spinnerSliderPath_Segments.setSelection(       Util.getStateInt(_state,      Map2View.STATE_SLIDER_PATH_SEGMENTS,   Map2View.STATE_SLIDER_PATH_SEGMENTS_DEFAULT));
      _spinnerSliderPath_LineWidth.setSelection(      Util.getStateInt(_state,      Map2View.STATE_SLIDER_PATH_LINE_WIDTH, Map2View.STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT));
      _colorSliderPathColor.setColorValue(            Util.getStateRGB(_state,      Map2View.STATE_SLIDER_PATH_COLOR,      Map2View.STATE_SLIDER_PATH_COLOR_DEFAULT));

      // track opacity
      _chkTrackOpacity.setSelection(            _prefStore.getBoolean(  ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
      _spinnerTrackOpacity.setSelection(        trackOpacity);

      // plot type
      updateUI_SetPlotType(                     _prefStore.getString(   ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE));
      _chkIsCutOffLinesInPauses.setSelection(   _prefStore.getBoolean(  ITourbookPreferences.MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES));

      // line
      _chkIsAntialiasPainting.setSelection(     _prefStore.getBoolean(  ITourbookPreferences.MAP_LAYOUT_IS_ANTIALIAS_PAINTING));
      _spinnerLineWidth.setSelection(           _prefStore.getInt(      ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH));

      // border
      _chkPaintWithBorder.setSelection(         _prefStore.getBoolean(  ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER));
      _spinnerBorder_Width.setSelection(        _prefStore.getInt(      ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH));
      _spinnerBorder_ColorDarker.setSelection(  _prefStore.getInt(      ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE));
      updateUI_SetBorderType(                   _prefStore.getInt(      ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE));

      _colorBorderColor.setColorValue(PreferenceConverter.getColor( _prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR));

      // painting method
      final boolean isComplex = Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX.equals(_prefStore.getString(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD));
      _chkShowEnhancedWarning.setSelection(     _prefStore.getBoolean(  ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING));
      _rdoPainting_Simple.setSelection(         isComplex == false);
      _rdoPainting_Complex.setSelection(        isComplex);
   }

   private void saveState() {

      // hovered/selected tour
      _state.put(Map2View.STATE_IS_SHOW_HOVERED_SELECTED_TOUR,                            _chkShowHoveredSelectedTour.getSelection());
      _state.put(Map2View.STATE_HOVERED_SELECTED__HOVERED_OPACITY,                        UI.transformOpacity_WhenSaved(_spinnerHoveredSelected_HoveredOpacity.getSelection()));
      _state.put(Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_OPACITY,           UI.transformOpacity_WhenSaved(_spinnerHoveredSelected_HoveredAndSelectedOpacity.getSelection()));
      _state.put(Map2View.STATE_HOVERED_SELECTED__SELECTED_OPACITY,                       UI.transformOpacity_WhenSaved(_spinnerHoveredSelected_SelectedOpacity.getSelection()));
      Util.setState(_state, Map2View.STATE_HOVERED_SELECTED__HOVERED_RGB,                 _colorHoveredSelected_Hovered.getColorValue());
      Util.setState(_state, Map2View.STATE_HOVERED_SELECTED__HOVERED_AND_SELECTED_RGB,    _colorHoveredSelected_HoveredAndSelected.getColorValue());
      Util.setState(_state, Map2View.STATE_HOVERED_SELECTED__SELECTED_RGB,                _colorHoveredSelected_Selected.getColorValue());

      // breadcrumbs
      _state.put(Map2View.STATE_IS_SHOW_BREADCRUMBS,                                      _chkShowBreadcrumbs.getSelection());
      _state.put(Map2View.STATE_VISIBLE_BREADCRUMBS,                                      _spinnerBreadcrumbItems.getSelection());

      // tour direction
      _state.put(Map2View.STATE_IS_SHOW_TOUR_DIRECTION,           _chkShowTourDirections.getSelection());
      _state.put(Map2View.STATE_IS_SHOW_TOUR_DIRECTION_ALWAYS,    _chkShowTourDirectionsAlways.getSelection());
      _state.put(Map2View.STATE_TOUR_DIRECTION_LINE_WIDTH,        _spinnerTourDirection_LineWidth.getSelection());
      _state.put(Map2View.STATE_TOUR_DIRECTION_MARKER_GAP,        _spinnerTourDirection_MarkerGap .getSelection());
      _state.put(Map2View.STATE_TOUR_DIRECTION_SYMBOL_SIZE,       _spinnerTourDirection_SymbolSize.getSelection());
      Util.setState(_state, Map2View.STATE_TOUR_DIRECTION_RGB,    _colorTourDirection_SymbolColor.getColorValue());

      // slider location
      _state.put(Map2View.STATE_IS_SHOW_SLIDER_IN_MAP,            _chkShowSlider_Location.getSelection());

      // slider path
      _state.put(Map2View.STATE_IS_SHOW_SLIDER_PATH,              _chkShowSlider_Path.getSelection());
      _state.put(Map2View.STATE_SLIDER_PATH_OPACITY,              UI.transformOpacity_WhenSaved(_spinnerSliderPath_Opacity.getSelection()));
      _state.put(Map2View.STATE_SLIDER_PATH_SEGMENTS,             _spinnerSliderPath_Segments.getSelection());
      _state.put(Map2View.STATE_SLIDER_PATH_LINE_WIDTH,           _spinnerSliderPath_LineWidth.getSelection());
      Util.setState(_state, Map2View.STATE_SLIDER_PATH_COLOR,     _colorSliderPathColor.getColorValue());

      // track opacity
      _prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,      _chkTrackOpacity.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,         UI.transformOpacity_WhenSaved(_spinnerTrackOpacity.getSelection()));

      // plot type
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE,                   getPlotType());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES,  _chkIsCutOffLinesInPauses.getSelection());

      // line
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_IS_ANTIALIAS_PAINTING,       _chkIsAntialiasPainting.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH,                _spinnerLineWidth.getSelection());

      // border
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER,           _chkPaintWithBorder.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH,                _spinnerBorder_Width.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_TYPE,                 getBorderType());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE,           _spinnerBorder_ColorDarker.getSelection());

      PreferenceConverter.setValue(_prefStore, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR, _colorBorderColor.getColorValue());

      // painting method
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING,   _chkShowEnhancedWarning.getSelection());
      _prefStore.setValue(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,           _rdoPainting_Complex.getSelection()
               ? Map2_Appearance.TOUR_PAINT_METHOD_COMPLEX
               : Map2_Appearance.TOUR_PAINT_METHOD_SIMPLE);
   }

   private void saveUIState() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _state.put(Map2View.STATE_TRACK_OPTIONS_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }

// SET_FORMATTING_ON

   private void updateUI_SetBorderType(int borderType) {

      if (borderType != Map2_Appearance.BORDER_TYPE_COLOR
            && borderType != Map2_Appearance.BORDER_TYPE_DARKER) {

         borderType = Map2_Appearance.DEFAULT_BORDER_TYPE;
      }

      _rdoBorderColorColor.setSelection(borderType == Map2_Appearance.BORDER_TYPE_COLOR);
      _rdoBorderColorDarker.setSelection(borderType == Map2_Appearance.BORDER_TYPE_DARKER);
   }

   private void updateUI_SetPlotType(String plotType) {

      if (plotType.equals(Map2_Appearance.PLOT_TYPE_DOT) == false
            && plotType.equals(Map2_Appearance.PLOT_TYPE_LINE) == false
            && plotType.equals(Map2_Appearance.PLOT_TYPE_SQUARE) == false) {

         plotType = Map2_Appearance.DEFAULT_PLOT_TYPE;
      }

      _rdoSymbolDot.setSelection(plotType.equals(Map2_Appearance.PLOT_TYPE_DOT));
      _rdoSymbolLine.setSelection(plotType.equals(Map2_Appearance.PLOT_TYPE_LINE));
      _rdoSymbolSquare.setSelection(plotType.equals(Map2_Appearance.PLOT_TYPE_SQUARE));
   }
}
