/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEntry;
import net.tourbook.map25.ClusterAlgorithmItem;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.marker.ClusterAlgorithm;
import net.tourbook.map25.layer.marker.MarkerConfig;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutMap25_MarkerOptions extends ToolbarSlideout implements IColorSelectorListener {

// SET_FORMATTING_OFF

   private static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP   = net.tourbook.map3.Messages.TourTrack_Properties_Button_Default_Tooltip;
   private static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT           = net.tourbook.map3.Messages.TourTrack_Properties_Button_Default;

// SET_FORMATTING_ON

   private static final int        DEFAULT_COMBO_WIDTH = 30;

   private SelectionListener       _defaultSelectionListener;
   private MouseWheelListener      _defaultMouseWheelListener;
   private IPropertyChangeListener _defaultPropertyChangeListener;
   private FocusListener           _keepOpenListener;

   private PixelConverter          _pc;

   private Map25View               _map25View;

   private boolean                 _isUpdateUI;
   private GridDataFactory         _spinnerGridData;

   /*
    * UI controls
    */
   private Composite             _shellContainer;

   private ColorSelectorExtended _colorClusterSymbol_Outline;
   private ColorSelectorExtended _colorClusterSymbol_Fill;
   private ColorSelectorExtended _colorMarkerSymbol_Outline;
   private ColorSelectorExtended _colorMarkerSymbol_Fill;

   private Button                _btnSwapClusterSymbolColor;
   private Button                _btnSwapMarkerColor;
   private Button                _btnReset;

   private Button                _chkIsMarkerClustering;
   private Button                _chkIsShowTourMarker;
   private Button                _chkIsShowBookmark;

   private Combo                 _comboClusterAlgorithm;
   private Combo                 _comboClusterOrientation;
   private Combo                 _comboConfigName;
   private Combo                 _comboMarkerOrientation;

   private Label                 _lblConfigName;
   private Label                 _lblClusterGridSize;
   private Label                 _lblClusterOpacity;
   private Label                 _lblClusterAlgorithm;
   private Label                 _lblClusterOrientation;
   private Label                 _lblClusterSymbol;
   private Label                 _lblClusterSymbolSize;

   private Label                 _lblMarkerOpacity;
   private Label                 _lblMarkerOrientation;
   private Label                 _lblMarkerColor;
   private Label                 _lblMarkerSize;

   private Spinner               _spinnerClusterGrid_Size;
   private Spinner               _spinnerClusterFill_Opacity;
   private Spinner               _spinnerClusterOutline_Opacity;
   private Spinner               _spinnerClusterOutline_Size;
   private Spinner               _spinnerClusterSymbol_Size;
   private Spinner               _spinnerClusterSymbol_Weight;

   private Spinner               _spinnerMarkerFill_Opacity;
   private Spinner               _spinnerMarkerOutline_Opacity;
   private Spinner               _spinnerMarkerOutline_Size;
   private Spinner               _spinnerMarkerSymbol_Size;

   private Text                  _textConfigName;

   public SlideoutMap25_MarkerOptions(final Control ownerControl,
                                      final ToolBar toolBar,
                                      final IDialogSettings state,
                                      final Map25View map25View) {

      super(ownerControl, toolBar);

      _map25View = map25View;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);
      createActions();

      final Composite ui = createUI(parent);

      fillUI();
      fillUI_Config();
      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(UI.SHELL_MARGIN, UI.SHELL_MARGIN).applyTo(_shellContainer);
//      _shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         final Composite container = new Composite(_shellContainer, SWT.NO_FOCUS);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         {
            createUI_00_Title(container);

            createUI_10_Marker(container);

            final Group group = new Group(container, SWT.NONE);
            group.setText(Messages.Slideout_Map25MarkerOptions_Group_MarkerLayout);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
            GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
            {
               createUI_20_Marker_Point(group);
               createUI_30_Marker_Cluster(group);
            }

            createUI_99_ConfigName(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_00_Title(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Label: Title
             */
            final Label title = new Label(container, SWT.LEAD);
            title.setText(Messages.Slideout_Map25MarkerOptions_Label_Title);
            MTFont.setBannerFont(title);
            GridDataFactory.fillDefaults()
//                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(title);
         }
         {
            /*
             * Combo: Configuration
             */
            _comboConfigName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboConfigName.setVisibleItemCount(20);
            _comboConfigName.addFocusListener(_keepOpenListener);
            _comboConfigName.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectConfig();
               }
            });
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .applyTo(_comboConfigName);
         }
         {
            /*
             * Button: Reset
             */
            _btnReset = new Button(container, SWT.PUSH);
            _btnReset.setText(TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT);
            _btnReset.setToolTipText(TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP);
            _btnReset.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectConfig_Default(e);
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_btnReset);
         }
      }
   }

   private void createUI_10_Marker(final Composite parent) {

      {
         // checkbox: Show tour marker
         _chkIsShowTourMarker = new Button(parent, SWT.CHECK);
         _chkIsShowTourMarker.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowTourMarker);
         _chkIsShowTourMarker.addSelectionListener(_defaultSelectionListener);
//         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowTourMarker);
      }
      {
         // checkbox: Show map bookmark
         _chkIsShowBookmark = new Button(parent, SWT.CHECK);
         _chkIsShowBookmark.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsShowBookmarks);
         _chkIsShowBookmark.addSelectionListener(_defaultSelectionListener);
//         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsShowBookmark);
      }
   }

   private void createUI_20_Marker_Point(final Composite parent) {

      {
         // label: symbol
         _lblMarkerColor = new Label(parent, SWT.NONE);
         _lblMarkerColor.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor);
         _lblMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerColor_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblMarkerColor);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            // outline color
            _colorMarkerSymbol_Outline = new ColorSelectorExtended(container);
            _colorMarkerSymbol_Outline.addListener(_defaultPropertyChangeListener);
            _colorMarkerSymbol_Outline.addOpenListener(this);

            // fill color
            _colorMarkerSymbol_Fill = new ColorSelectorExtended(container);
            _colorMarkerSymbol_Fill.addListener(_defaultPropertyChangeListener);
            _colorMarkerSymbol_Fill.addOpenListener(this);

            // button: swap color
            _btnSwapMarkerColor = new Button(container, SWT.PUSH);
            _btnSwapMarkerColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapMarkerColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapMarkerColor.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSwapMarkerColor(e);
               }
            });
         }
      }
      {
         /*
          * Opacity
          */
         // label
         _lblMarkerOpacity = new Label(parent, SWT.NONE);
         _lblMarkerOpacity.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity);
         _lblMarkerOpacity.setToolTipText(NLS.bind(Messages.Slideout_Map25MarkerOptions_Label_MarkerOpacity_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblMarkerOpacity);

         /*
          * Symbol
          */
         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            {
               // spinner: outline
               _spinnerMarkerOutline_Opacity = new Spinner(container, SWT.BORDER);
               _spinnerMarkerOutline_Opacity.setMinimum(0);
               _spinnerMarkerOutline_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
               _spinnerMarkerOutline_Opacity.setIncrement(1);
               _spinnerMarkerOutline_Opacity.setPageIncrement(10);
               _spinnerMarkerOutline_Opacity.addSelectionListener(_defaultSelectionListener);
               _spinnerMarkerOutline_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerMarkerOutline_Opacity);
            }
            {
               // spinner: fill
               _spinnerMarkerFill_Opacity = new Spinner(container, SWT.BORDER);
               _spinnerMarkerFill_Opacity.setMinimum(0);
               _spinnerMarkerFill_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
               _spinnerMarkerFill_Opacity.setIncrement(1);
               _spinnerMarkerFill_Opacity.setPageIncrement(10);
               _spinnerMarkerFill_Opacity.addSelectionListener(_defaultSelectionListener);
               _spinnerMarkerFill_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerMarkerFill_Opacity);
            }
         }
      }
      {
         /*
          * Size
          */

         // label: size
         _lblMarkerSize = new Label(parent, SWT.NONE);
         _lblMarkerSize.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize);
         _lblMarkerSize.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_MarkerSize_Tooltip);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblMarkerSize);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            // outline size
            _spinnerMarkerOutline_Size = new Spinner(container, SWT.BORDER);
            _spinnerMarkerOutline_Size.setMinimum((int) Map25ConfigManager.MARKER_OUTLINE_SIZE_MIN);
            _spinnerMarkerOutline_Size.setMaximum((int) Map25ConfigManager.MARKER_OUTLINE_SIZE_MAX);
            _spinnerMarkerOutline_Size.setIncrement(1);
            _spinnerMarkerOutline_Size.setPageIncrement(10);
            _spinnerMarkerOutline_Size.addSelectionListener(_defaultSelectionListener);
            _spinnerMarkerOutline_Size.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerGridData.applyTo(_spinnerMarkerOutline_Size);

            // symbol size
            _spinnerMarkerSymbol_Size = new Spinner(container, SWT.BORDER);
            _spinnerMarkerSymbol_Size.setMinimum(Map25ConfigManager.MARKER_SYMBOL_SIZE_MIN);
            _spinnerMarkerSymbol_Size.setMaximum(Map25ConfigManager.MARKER_SYMBOL_SIZE_MAX);
            _spinnerMarkerSymbol_Size.setIncrement(1);
            _spinnerMarkerSymbol_Size.setPageIncrement(10);
            _spinnerMarkerSymbol_Size.addSelectionListener(_defaultSelectionListener);
            _spinnerMarkerSymbol_Size.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerGridData.applyTo(_spinnerMarkerSymbol_Size);
         }
      }
      {
         /*
          * Orientation: billboard/ground
          */
         {
            // label
            _lblMarkerOrientation = new Label(parent, SWT.NONE);
            _lblMarkerOrientation.setText(Messages.Slideout_Map25MarkerOptions_Label_MarkerOrientation);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblMarkerOrientation);
         }
         {
            // combo
            _comboMarkerOrientation = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
            _comboMarkerOrientation.setVisibleItemCount(20);
            _comboMarkerOrientation.addFocusListener(_keepOpenListener);
            _comboMarkerOrientation.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onModifyConfig();
               }
            });
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
                  .applyTo(_comboMarkerOrientation);
         }
      }
   }

   private void createUI_30_Marker_Cluster(final Composite parent) {

      final int clusterIndent = UI.FORM_FIRST_COLUMN_INDENT;

      {
         /*
          * Cluster
          */

         // checkbox: Is clustering
         _chkIsMarkerClustering = new Button(parent, SWT.CHECK);
         _chkIsMarkerClustering.setText(Messages.Slideout_Map25MarkerOptions_Checkbox_IsMarkerClustering);
         _chkIsMarkerClustering.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .indent(0, 10)
               .applyTo(_chkIsMarkerClustering);
      }
      {
         /*
          * Symbol color
          */
         {
            // label
            _lblClusterSymbol = new Label(parent, SWT.NONE);
            _lblClusterSymbol.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor);
            _lblClusterSymbol.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSymbolColor_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterSymbol);
         }

         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

            // foreground color
            _colorClusterSymbol_Outline = new ColorSelectorExtended(container);
            _colorClusterSymbol_Outline.addListener(_defaultPropertyChangeListener);
            _colorClusterSymbol_Outline.addOpenListener(this);

            // foreground color
            _colorClusterSymbol_Fill = new ColorSelectorExtended(container);
            _colorClusterSymbol_Fill.addListener(_defaultPropertyChangeListener);
            _colorClusterSymbol_Fill.addOpenListener(this);

            // button: swap color
            _btnSwapClusterSymbolColor = new Button(container, SWT.PUSH);
            _btnSwapClusterSymbolColor.setText(UI.SYMBOL_ARROW_LEFT_RIGHT);
            _btnSwapClusterSymbolColor.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_SwapColor_Tooltip);
            _btnSwapClusterSymbolColor.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSwapClusterColor(e);
               }
            });
         }
      }
      {
         /*
          * Opacity
          */
         // label
         _lblClusterOpacity = new Label(parent, SWT.NONE);
         _lblClusterOpacity.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOpacity);
         _lblClusterOpacity.setToolTipText(NLS.bind(Messages.Slideout_Map25MarkerOptions_Label_ClusterOpacity_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         GridDataFactory.fillDefaults()
               .indent(clusterIndent, 0)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_lblClusterOpacity);

         /*
          * Symbol
          */
         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            {
               // spinner: outline
               _spinnerClusterOutline_Opacity = new Spinner(container, SWT.BORDER);
               _spinnerClusterOutline_Opacity.setMinimum(0);
               _spinnerClusterOutline_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
               _spinnerClusterOutline_Opacity.setIncrement(1);
               _spinnerClusterOutline_Opacity.setPageIncrement(10);
               _spinnerClusterOutline_Opacity.addSelectionListener(_defaultSelectionListener);
               _spinnerClusterOutline_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterOutline_Opacity);
            }
            {
               // spinner: fill
               _spinnerClusterFill_Opacity = new Spinner(container, SWT.BORDER);
               _spinnerClusterFill_Opacity.setMinimum(0);
               _spinnerClusterFill_Opacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
               _spinnerClusterFill_Opacity.setIncrement(1);
               _spinnerClusterFill_Opacity.setPageIncrement(10);
               _spinnerClusterFill_Opacity.addSelectionListener(_defaultSelectionListener);
               _spinnerClusterFill_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterFill_Opacity);
            }
         }
      }
      {
         /*
          * Size
          */
         {
            // label
            _lblClusterSymbolSize = new Label(parent, SWT.NONE);
            _lblClusterSymbolSize.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSize);
            _lblClusterSymbolSize.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_ClusterSize_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterSymbolSize);
         }

         {
            final Composite container = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
            {

               // outline size
               _spinnerClusterOutline_Size = new Spinner(container, SWT.BORDER);
               _spinnerClusterOutline_Size.setMinimum(Map25ConfigManager.CLUSTER_OUTLINE_SIZE_MIN);
               _spinnerClusterOutline_Size.setMaximum(Map25ConfigManager.CLUSTER_OUTLINE_SIZE_MAX);
               _spinnerClusterOutline_Size.setIncrement(1);
               _spinnerClusterOutline_Size.setPageIncrement(10);
               _spinnerClusterOutline_Size.addSelectionListener(_defaultSelectionListener);
               _spinnerClusterOutline_Size.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterOutline_Size);

               // spinner: symbol size
               _spinnerClusterSymbol_Size = new Spinner(container, SWT.BORDER);
               _spinnerClusterSymbol_Size.setMinimum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_MIN);
               _spinnerClusterSymbol_Size.setMaximum(Map25ConfigManager.CLUSTER_SYMBOL_SIZE_MAX);
               _spinnerClusterSymbol_Size.setIncrement(1);
               _spinnerClusterSymbol_Size.setPageIncrement(10);
               _spinnerClusterSymbol_Size.addSelectionListener(_defaultSelectionListener);
               _spinnerClusterSymbol_Size.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterSymbol_Size);

               // spinner: symbol size weight
               _spinnerClusterSymbol_Weight = new Spinner(container, SWT.BORDER);
               _spinnerClusterSymbol_Weight.setMinimum(Map25ConfigManager.CLUSTER_SYMBOL_WEIGHT_MIN);
               _spinnerClusterSymbol_Weight.setMaximum(Map25ConfigManager.CLUSTER_SYMBOL_WEIGHT_MAX);
               _spinnerClusterSymbol_Weight.setIncrement(1);
               _spinnerClusterSymbol_Weight.setPageIncrement(10);
               _spinnerClusterSymbol_Weight.addSelectionListener(_defaultSelectionListener);
               _spinnerClusterSymbol_Weight.addMouseWheelListener(_defaultMouseWheelListener);
               _spinnerGridData.applyTo(_spinnerClusterSymbol_Weight);
            }
         }
      }
      {
         /*
          * Cluster orientation: billboard/ground
          */
         {
            // label
            _lblClusterOrientation = new Label(parent, SWT.NONE);
            _lblClusterOrientation.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterOrientation);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterOrientation);
         }
         {
            /*
             * Combo: Orientaion
             */
            _comboClusterOrientation = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
            _comboClusterOrientation.setVisibleItemCount(20);
            _comboClusterOrientation.addFocusListener(_keepOpenListener);
            _comboClusterOrientation.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onModifyConfig();
               }
            });
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
                  .applyTo(_comboClusterOrientation);
         }
      }
      {
         /*
          * Cluster placement: first marker/distance/grid
          */
         {
            // label
            _lblClusterAlgorithm = new Label(parent, SWT.NONE);
            _lblClusterAlgorithm.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterPlacement);
            _lblClusterAlgorithm.setToolTipText(
                  Messages.Slideout_Map25MarkerOptions_Label_ClusterPlacement_Tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterAlgorithm);
         }
         {
            /*
             * Combo: Placement
             */
            _comboClusterAlgorithm = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
            _comboClusterAlgorithm.setVisibleItemCount(20);
            _comboClusterAlgorithm.addFocusListener(_keepOpenListener);
            _comboClusterAlgorithm.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onModifyConfig();
               }
            });
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(DEFAULT_COMBO_WIDTH), SWT.DEFAULT)
                  .applyTo(_comboClusterAlgorithm);
         }
      }
      {
         /*
          * Cluster size
          */
         {
            // label
            _lblClusterGridSize = new Label(parent, SWT.NONE);
            _lblClusterGridSize.setText(Messages.Slideout_Map25MarkerOptions_Label_ClusterGridSize);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(clusterIndent, 0)
                  .applyTo(_lblClusterGridSize);

            // spinner
            _spinnerClusterGrid_Size = new Spinner(parent, SWT.BORDER);
            _spinnerClusterGrid_Size.setMinimum(Map25ConfigManager.CLUSTER_GRID_MIN_SIZE);
            _spinnerClusterGrid_Size.setMaximum(Map25ConfigManager.CLUSTER_GRID_MAX_SIZE);
            _spinnerClusterGrid_Size.setIncrement(1);
            _spinnerClusterGrid_Size.setPageIncrement(10);
            _spinnerClusterGrid_Size.addSelectionListener(_defaultSelectionListener);
            _spinnerClusterGrid_Size.addMouseWheelListener(_defaultMouseWheelListener);
         }
      }
   }

   private void createUI_99_ConfigName(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Config name
             */

            // Label
            _lblConfigName = new Label(container, SWT.NONE);
            _lblConfigName.setText(Messages.Slideout_Map25MarkerOptions_Label_Name);
            _lblConfigName.setToolTipText(Messages.Slideout_Map25MarkerOptions_Label_Name_Tooltip);
            UI.gridLayoutData_AlignFillCenter().applyTo(_lblConfigName);

            // Text
            _textConfigName = new Text(container, SWT.BORDER);
            _textConfigName.addModifyListener(modifyEvent -> onModifyName());
            UI.gridLayoutData_AlignFillCenter().grab(true, false).applyTo(_textConfigName);
         }
      }
   }

   private void enableControls() {

      final boolean isMapBookmark = _chkIsShowBookmark.getSelection();
      final boolean isTourMarker = _chkIsShowTourMarker.getSelection();

      final boolean isLayout = isMapBookmark || isTourMarker;
      final boolean isClustering = _chkIsMarkerClustering.getSelection() && isLayout;

      /*
       * Marker
       */
      _btnSwapMarkerColor.setEnabled(isLayout);

      _chkIsMarkerClustering.setEnabled(isLayout);

      _colorMarkerSymbol_Fill.setEnabled(isLayout);
      _colorMarkerSymbol_Outline.setEnabled(isLayout);

      _comboMarkerOrientation.setEnabled(isLayout);

      _lblMarkerOpacity.setEnabled(isLayout);
      _lblMarkerOrientation.setEnabled(isLayout);
      _lblMarkerColor.setEnabled(isLayout);
      _lblMarkerSize.setEnabled(isLayout);

      _spinnerMarkerFill_Opacity.setEnabled(isLayout);
      _spinnerMarkerOutline_Opacity.setEnabled(isLayout);
      _spinnerMarkerSymbol_Size.setEnabled(isLayout);
      _spinnerMarkerOutline_Size.setEnabled(isLayout);

      /*
       * Clustering
       */
      _btnSwapClusterSymbolColor.setEnabled(isClustering);

      _colorClusterSymbol_Outline.setEnabled(isClustering);
      _colorClusterSymbol_Fill.setEnabled(isClustering);

      _comboClusterAlgorithm.setEnabled(isClustering);
      _comboClusterOrientation.setEnabled(isClustering);

      _lblClusterGridSize.setEnabled(isClustering);
      _lblClusterAlgorithm.setEnabled(isClustering);
      _lblClusterOrientation.setEnabled(isClustering);
      _lblClusterOpacity.setEnabled(isClustering);
      _lblClusterSymbol.setEnabled(isClustering);
      _lblClusterSymbolSize.setEnabled(isClustering);

      _spinnerClusterGrid_Size.setEnabled(isClustering);
      _spinnerClusterFill_Opacity.setEnabled(isClustering);
      _spinnerClusterOutline_Opacity.setEnabled(isClustering);
      _spinnerClusterOutline_Size.setEnabled(isClustering);
      _spinnerClusterSymbol_Size.setEnabled(isClustering);
      _spinnerClusterSymbol_Weight.setEnabled(isClustering);
   }

   private void fillUI() {

      final boolean backupIsUpdateUI = _isUpdateUI;
      _isUpdateUI = true;
      {
         for (final ComboEntry comboItem : Map25ConfigManager.SYMBOL_ORIENTATION) {

            _comboClusterOrientation.add(comboItem.label);
            _comboMarkerOrientation.add(comboItem.label);
         }

         for (final ClusterAlgorithmItem item : Map25ConfigManager.ALL_CLUSTER_ALGORITHM) {
            _comboClusterAlgorithm.add(item.label);
         }
      }
      _isUpdateUI = backupIsUpdateUI;
   }

   private void fillUI_Config() {

      final boolean backupIsUpdateUI = _isUpdateUI;
      _isUpdateUI = true;
      {
         _comboConfigName.removeAll();

         for (final MarkerConfig config : Map25ConfigManager.getAllMarkerConfigs()) {
            _comboConfigName.add(config.name);
         }
      }
      _isUpdateUI = backupIsUpdateUI;
   }

   private int getClusterAlgorithmIndex(final Enum<ClusterAlgorithm> requestedAlgorithm) {

      final ClusterAlgorithmItem[] allClusterAlgorithm = Map25ConfigManager.ALL_CLUSTER_ALGORITHM;

      for (int algorithmIndex = 0; algorithmIndex < allClusterAlgorithm.length; algorithmIndex++) {

         final ClusterAlgorithm algorithm = allClusterAlgorithm[algorithmIndex].clusterAlgorithm;

         if (algorithm == requestedAlgorithm) {
            return algorithmIndex;
         }
      }

      // this should not occure
      return 0;
   }

   private int getOrientationIndex(final int orientationId) {

      final ComboEntry[] symbolOrientation = Map25ConfigManager.SYMBOL_ORIENTATION;

      for (int itemIndex = 0; itemIndex < symbolOrientation.length; itemIndex++) {

         final ComboEntry comboItem = symbolOrientation[itemIndex];

         if (comboItem.value == orientationId) {
            return itemIndex;
         }
      }

      return 0;
   }

   private Enum<ClusterAlgorithm> getSelectedClusterAlgorithm() {

      final int selectedIndex = Math.max(0, _comboClusterAlgorithm.getSelectionIndex());

      return Map25ConfigManager.ALL_CLUSTER_ALGORITHM[selectedIndex].clusterAlgorithm;
   }

   private int getSelectedOrientation(final Combo combo) {

      final int selectedIndex = Math.max(0, combo.getSelectionIndex());

      return Map25ConfigManager.SYMBOL_ORIENTATION[selectedIndex].value;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      // force spinner controls to have the same width
      _spinnerGridData = GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(3), SWT.DEFAULT);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onModifyConfig());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onModifyConfig();
      };

      _defaultPropertyChangeListener = propertyChangeEvent -> onModifyConfig();

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onModifyConfig() {

      saveState();

      enableControls();

      updateUI_Map();
   }

   private void onModifyName() {

      if (_isUpdateUI) {
         return;
      }

      // update text in the combo
      final int selectedIndex = _comboConfigName.getSelectionIndex();

      _comboConfigName.setItem(selectedIndex, _textConfigName.getText());

      saveState();
   }

   private void onSelectConfig() {

      final int selectedIndex = _comboConfigName.getSelectionIndex();
      final ArrayList<MarkerConfig> allConfigurations = Map25ConfigManager.getAllMarkerConfigs();

      final MarkerConfig selectedConfig = allConfigurations.get(selectedIndex);
      final MarkerConfig activeConfig = Map25ConfigManager.getActiveMarkerConfig();

      if (selectedConfig.equals(activeConfig)) {

         // config has not changed
         return;
      }

      // keep data from previous config
      saveState();

      Map25ConfigManager.setActiveMarkerConfig(selectedConfig);

      restoreState();

      enableControls();

      updateUI_Map();
   }

   private void onSelectConfig_Default(final SelectionEvent selectionEvent) {

      if (Util.isCtrlKeyPressed(selectionEvent)) {

         // reset All configurations

         Map25ConfigManager.resetAllMarkerConfigurations();

         fillUI_Config();

      } else {

         // reset active config

         Map25ConfigManager.resetActiveMarkerConfiguration();
      }

      restoreState();
      enableControls();

      updateUI_Map();
   }

   private void onSwapClusterColor(final SelectionEvent e) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.clusterOutline_Color;
      final RGB bgColor = config.clusterFill_Color;

      config.clusterOutline_Color = bgColor;
      config.clusterFill_Color = fgColor;

      restoreState();
      onModifyConfig();
   }

   private void onSwapMarkerColor(final SelectionEvent event) {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final RGB fgColor = config.markerOutline_Color;
      final RGB bgColor = config.markerFill_Color;

      config.markerOutline_Color = bgColor;
      config.markerFill_Color = fgColor;

      restoreState();
      onModifyConfig();
   }

// SET_FORMATTING_OFF

   /**
    * Restores state values from the tour marker configuration and update the UI.
    */
   public void restoreState() {

      _isUpdateUI = true;
      {
         final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

         // get active config AFTER getting the index because this could change the active config
         final int activeConfigIndex = Map25ConfigManager.getActiveMarkerConfigIndex();

         _comboConfigName  .select(activeConfigIndex);
         _textConfigName   .setText(config.name);

         /*
          * Marker
          */
         _chkIsShowBookmark               .setSelection(config.isShowMapBookmark);
         _chkIsShowTourMarker             .setSelection(config.isShowTourMarker);

         _comboMarkerOrientation          .select(getOrientationIndex(config.markerOrientation));

         _colorClusterSymbol_Outline      .setColorValue(config.clusterOutline_Color);
         _colorClusterSymbol_Fill         .setColorValue(config.clusterFill_Color);

         _spinnerMarkerSymbol_Size        .setSelection(config.markerSymbol_Size);
         _spinnerMarkerOutline_Size       .setSelection((int) (config.markerOutline_Size * 1));
         _spinnerMarkerFill_Opacity       .setSelection(UI.transformOpacity_WhenRestored(config.markerFill_Opacity));
         _spinnerMarkerOutline_Opacity    .setSelection(UI.transformOpacity_WhenRestored(config.markerOutline_Opacity));

         /*
          * Cluster
          */
         _chkIsMarkerClustering           .setSelection(config.isMarkerClustered);
         _comboClusterAlgorithm           .select(getClusterAlgorithmIndex(config.clusterAlgorithm));
         _comboClusterOrientation         .select(getOrientationIndex(config.clusterOrientation));

         _colorMarkerSymbol_Outline       .setColorValue(config.markerOutline_Color);
         _colorMarkerSymbol_Fill          .setColorValue(config.markerFill_Color);

         _spinnerClusterFill_Opacity      .setSelection(UI.transformOpacity_WhenRestored(config.clusterFill_Opacity));
         _spinnerClusterOutline_Opacity   .setSelection(UI.transformOpacity_WhenRestored(config.clusterOutline_Opacity));
         _spinnerClusterOutline_Size      .setSelection((int) (config.clusterOutline_Size * 1));
         _spinnerClusterGrid_Size         .setSelection(config.clusterGrid_Size);
         _spinnerClusterSymbol_Size       .setSelection(config.clusterSymbol_Size);
         _spinnerClusterSymbol_Weight     .setSelection(config.clusterSymbol_Weight);
      }
      _isUpdateUI = false;
   }

   private void saveState() {

      // update config

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      config.name = _textConfigName.getText();

      /*
       * Marker
       */
      config.isShowTourMarker       = _chkIsShowTourMarker.getSelection();
      config.isShowMapBookmark      = _chkIsShowBookmark.getSelection();

      config.markerOrientation      = getSelectedOrientation(_comboMarkerOrientation);
      config.markerSymbol_Size      = _spinnerMarkerSymbol_Size.getSelection();
      config.markerOutline_Size     = _spinnerMarkerOutline_Size.getSelection() / 1.0f;
      config.markerOutline_Color    = _colorMarkerSymbol_Outline.getColorValue();
      config.markerFill_Color       = _colorMarkerSymbol_Fill.getColorValue();
      config.markerOutline_Opacity  = UI.transformOpacity_WhenSaved(_spinnerMarkerOutline_Opacity.getSelection());
      config.markerFill_Opacity     = UI.transformOpacity_WhenSaved(_spinnerMarkerFill_Opacity.getSelection());

      /*
       * Cluster
       */
      config.isMarkerClustered      = _chkIsMarkerClustering.getSelection();
      config.clusterAlgorithm       = getSelectedClusterAlgorithm();
      config.clusterOrientation     = getSelectedOrientation(_comboClusterOrientation);

      config.clusterGrid_Size       = _spinnerClusterGrid_Size.getSelection();
      config.clusterSymbol_Size     = _spinnerClusterSymbol_Size.getSelection();
      config.clusterOutline_Size    = _spinnerClusterOutline_Size.getSelection() / 1.0f;
      config.clusterSymbol_Weight   = _spinnerClusterSymbol_Weight.getSelection();

      config.clusterFill_Color      = _colorClusterSymbol_Fill.getColorValue();
      config.clusterOutline_Color   = _colorClusterSymbol_Outline.getColorValue();
      config.clusterFill_Opacity    = UI.transformOpacity_WhenSaved(_spinnerClusterFill_Opacity.getSelection());
      config.clusterOutline_Opacity = UI.transformOpacity_WhenSaved(_spinnerClusterOutline_Opacity.getSelection());
   }

// SET_FORMATTING_ON

   private void updateUI_Map() {

      _map25View.getMapApp().onModifyMarkerConfig();

   }

}
