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
package net.tourbook.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.ColorValue;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.common.color.Map2GradientColorProvider;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.MapUnits;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapColorProvider;
import net.tourbook.map2.view.DialogMap2ColorEditor;
import net.tourbook.map2.view.IMap2ColorUpdater;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceColors extends PreferencePage implements
      IWorkbenchPreferencePage,
      IColorTreeViewer,
      IMap2ColorUpdater {

   public static final String ID = "net.tourbook.preferences.PrefPageChartColors"; //$NON-NLS-1$

   private static final char  NL = UI.NEW_LINE;

   /*
    * Legend is created with dummy values 0...200.
    */
   private static final int          LEGEND_MIN_VALUE           = 0;
   private static final int          LEGEND_MAX_VALUE           = 200;

   private static ColorValue[]       _legendImageColors         = new ColorValue[] {

         new ColorValue(10, 255, 0, 0),
         new ColorValue(50, 100, 100, 0),
         new ColorValue(100, 0, 255, 0),
         new ColorValue(150, 0, 100, 100),
         new ColorValue(190, 0, 0, 255) };

   private static final List<Float>  _legendImageUnitValues     = Arrays.asList(

         10f,
         50f,
         100f,
         150f,
         190f);

   private static final List<String> _legendImageUnitLabels     = Arrays.asList(

         Messages.Pref_ChartColors_unit_min,
         Messages.Pref_ChartColors_unit_low,
         Messages.Pref_ChartColors_unit_mid,
         Messages.Pref_ChartColors_unit_high,
         Messages.Pref_ChartColors_unit_max);

   private static final String       SYS_PROP__LOG_COLOR_VALUES = "logColorValues";                                      //$NON-NLS-1$
   private static boolean            _isLogging_ColorValues     = System.getProperty(SYS_PROP__LOG_COLOR_VALUES) != null;
   static {

      if (_isLogging_ColorValues) {
         Util.logSystemProperty_IsEnabled(TourManager.class, SYS_PROP__LOG_COLOR_VALUES, "Color values are logged"); //$NON-NLS-1$
      }
   }

   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

   private static final IDialogSettings  _state            = CommonActivator.getState(ID);

   private TreeViewer                    _colorViewer;
   private GraphColorItem                _selectedColor;
   private boolean                       _isColorChanged;

   private ColorDefinition               _expandedItem;
   private boolean                       _isNavigationKeyPressed;
   private boolean                       _isInTreeExpand;

   private IGradientColorProvider        _legendImageColorProvider;
   private DialogMap2ColorEditor         _dialogMappingColor;

   private GraphColorPainter             _graphColorPainter;

   /*
    * UI controls
    */
   private Button                _btnLegend;
   private Button                _chkLiveUpdate;

   private ColorSelectorExtended _colorSelector;

   /**
    * the color content provider has the following structure<br>
    *
    * <pre>
    * {@link ColorDefinition}
    *    {@link GraphColorItem}
    *    {@link GraphColorItem}
    *    ...
    *    {@link GraphColorItem}
    *
    *    ...
    *
    * {@link ColorDefinition}
    *    {@link GraphColorItem}
    *    {@link GraphColorItem}
    *    ...
    *    {@link GraphColorItem}
    * </pre>
    */
   private static class ColorContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {

         if (parentElement instanceof ColorDefinition) {
            final ColorDefinition graphDefinition = (ColorDefinition) parentElement;
            return graphDefinition.getGraphColorItems();
         }
         return null;
      }

      @Override
      public Object[] getElements(final Object inputElement) {

         if (inputElement instanceof PrefPageAppearanceColors) {
            return GraphColorManager.getAllColorDefinitions();
         }
         return null;
      }

      @Override
      public Object getParent(final Object element) {
         return null;
      }

      @Override
      public boolean hasChildren(final Object element) {

         if (element instanceof ColorDefinition) {
            return true;
         }
         return false;
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}

   }

   public static Map2GradientColorProvider createMap2ColorProvider() {

      final Map2GradientColorProvider colorProvider = new Map2GradientColorProvider(MapGraphId.Altitude);

      final Map2ColorProfile colorProfile = colorProvider.getColorProfile();
      colorProfile.setColorValues(_legendImageColors);

      // update legend configuations
      final MapUnits mapUnits = colorProvider.getMapUnits(ColorProviderConfig.MAP3_PROFILE);

      mapUnits.units = _legendImageUnitValues;
      mapUnits.unitLabels = _legendImageUnitLabels;
      mapUnits.unitText = UI.EMPTY_STRING;

      mapUnits.legendMinValue = LEGEND_MIN_VALUE;
      mapUnits.legendMaxValue = LEGEND_MAX_VALUE;

      return colorProvider;
   }

   public static boolean isLogging_ColorValues() {
      return _isLogging_ColorValues;
   }

   @Override
   public void applyMapColors(final Map2ColorProfile newMapColor) {

      updateColorsFromDialog(_selectedColor.getColorDefinition(), newMapColor);

      updateAndSaveColors();
   }

   /**
    * create color objects for every graph definition
    */
   private void createColorDefinitions() {

      final String[][] allColorNames = GraphColorManager.colorNames;
      final ColorDefinition[] allGraphColorDefinitions = GraphColorManager.getAllColorDefinitions();

      for (final ColorDefinition colorDefinition : allGraphColorDefinitions) {

         final ArrayList<GraphColorItem> allGraphColorItems = new ArrayList<>();

         final boolean isMapColorAvailable = colorDefinition.getMap2Color_Active() != null;

         for (final String[] colorName : allColorNames) {

            final String colorNameID = colorName[0];
            final String visibleColorName = colorName[1];

            if (colorNameID == GraphColorManager.PREF_COLOR_MAPPING) {

               if (isMapColorAvailable) {

                  // create map color
                  allGraphColorItems.add(new GraphColorItem(colorDefinition, colorNameID, visibleColorName, true));
               }

            } else {

               allGraphColorItems.add(new GraphColorItem(colorDefinition, colorNameID, visibleColorName, false));
            }
         }

         colorDefinition.setColorItems(allGraphColorItems.toArray(new GraphColorItem[allGraphColorItems.size()]));
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      createColorDefinitions();

      final Composite ui = createUI(parent);

      initializeLegend();

      restoreState();

      /*
       * MUST be run async otherwise the background color is NOT themed !!!
       */
      ui.getDisplay().asyncExec(() -> {

         _colorViewer.setInput(this);
      });

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         createUI_10_ColorViewer(container);
         createUI_20_ColorControl(container);
         createUI_99_LiveUpdate(container);
      }

      return container;
   }

   private void createUI_10_ColorViewer(final Composite parent) {

      /*
       * Create tree layout
       */
      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(400, 650)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * Create viewer
       */
      final Tree tree = new Tree(layoutContainer,
            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.BORDER
                  | SWT.MULTI
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(false);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _colorViewer = new TreeViewer(tree);
      defineAllColumns(treeLayout, tree);

      _colorViewer.setContentProvider(new ColorContentProvider());

      _graphColorPainter = new GraphColorPainter(this);

      _colorViewer.getTree().addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent keyEvent) {

            if (keyEvent.keyCode == SWT.ARROW_UP || keyEvent.keyCode == SWT.ARROW_DOWN) {
               _isNavigationKeyPressed = true;
            } else {
               _isNavigationKeyPressed = false;
            }
         }

         @Override
         public void keyReleased(final KeyEvent keyEvent) {}
      });

      _colorViewer.addSelectionChangedListener(selectionChangedEvent -> {

         if (_isInTreeExpand) {

            // prevent: !MESSAGE Ignored reentrant call while viewer is busy. This is only logged once per viewer instance, but similar calls will still be ignored.

            return;
         }

         final Object selection = ((IStructuredSelection) _colorViewer.getSelection()).getFirstElement();

         // don't expand when navigation key is pressed
         if (_isNavigationKeyPressed) {

            _isNavigationKeyPressed = false;

            return;
         }

         if (selection instanceof ColorDefinition) {

            // expand/collapse current item
            final ColorDefinition treeItem = (ColorDefinition) selection;

            if (_colorViewer.getExpandedState(treeItem)) {

               // item is expanded -> collapse

               _colorViewer.collapseToLevel(treeItem, 1);

            } else {

               // item is collapsed -> expand

               if (_expandedItem != null) {
                  _colorViewer.collapseToLevel(_expandedItem, 1);
               }
               _colorViewer.expandToLevel(treeItem, 1);
               _expandedItem = treeItem;

               // expanding the treeangle, the layout is correctly done but not with double click
               layoutContainer.layout(true, true);
            }

         } else if (selection instanceof GraphColorItem) {

            onSelect_ColorIn_ColorViewer();

            // run async that the UI do display the selected color in the color button
            _colorViewer.getTree().getDisplay().asyncExec(() -> {

               final GraphColorItem graphColor = (GraphColorItem) selection;

               if (graphColor.isMapColor()) {

                  // legend color is selected

                  onSelect_MappingColor();

               } else {

                  // open color selection dialog
                  _colorSelector.open();
               }
            });
         }
      });

      _colorViewer.addTreeListener(new ITreeViewerListener() {

         @Override
         public void treeCollapsed(final TreeExpansionEvent event) {

            if (event.getElement() instanceof ColorDefinition) {
               _expandedItem = null;
            }
         }

         @Override
         public void treeExpanded(final TreeExpansionEvent event) {

            final Object element = event.getElement();

            if (element instanceof ColorDefinition) {
               final ColorDefinition treeItem = (ColorDefinition) element;

               if (_expandedItem != null) {

                  _isInTreeExpand = true;
                  {
                     _colorViewer.collapseToLevel(_expandedItem, 1);
                  }
                  _isInTreeExpand = false;
               }

               _colorViewer.getTree().getDisplay().asyncExec(() -> {

                  _colorViewer.expandToLevel(treeItem, 1);
                  _expandedItem = treeItem;
               });
            }
         }
      });
   }

   /**
    * Create the color selection control.
    *
    * @param parent
    */
   private void createUI_20_ColorControl(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      // container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         /*
          * button: color selector:
          */
         _colorSelector = new ColorSelectorExtended(container);
         _colorSelector.setEnabled(false);
         _colorSelector.addListener(propertyChangeEvent -> {
            onSelect_ColorIn_ColorSelector(propertyChangeEvent);
            doLiveUpdate();
         });
         setButtonLayoutData(_colorSelector.getButton());

         /*
          * button: mapping color
          */
         _btnLegend = new Button(container, SWT.NONE);
         _btnLegend.setText(Messages.Pref_ChartColors_btn_legend);
         _btnLegend.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
            onSelect_MappingColor();
            doLiveUpdate();
         }));
         setButtonLayoutData(_btnLegend);

         _btnLegend.setEnabled(false);
      }
   }

   private void createUI_99_LiveUpdate(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Checkbox: live update
          */
         _chkLiveUpdate = new Button(container, SWT.CHECK);
         _chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
         _chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
         _chkLiveUpdate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> doLiveUpdate()));
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_chkLiveUpdate);
      }
   }

   /**
    * create columns
    */
   private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

      final int numHorizontalImages = 7;
      final int trailingOffset = 2;

      final int itemHeight = tree.getItemHeight();
      final int colorWidth = (itemHeight + GraphColorPainter.GRAPH_COLOR_SPACING) * numHorizontalImages
            + trailingOffset;

      {
         /*
          * 1. column: color item/color definition
          */
         final TreeViewerColumn tvc = new TreeViewerColumn(_colorViewer, SWT.LEAD);
         final TreeColumn tc = tvc.getColumn();
         tvc.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final Object element = cell.getElement();

               if (element instanceof ColorDefinition) {
                  cell.setText(((ColorDefinition) (element)).getVisibleName());
               } else if (element instanceof GraphColorItem) {
                  cell.setText(((GraphColorItem) (element)).getName());
               } else {
                  cell.setText(UI.EMPTY_STRING);
               }
            }
         });
         treeLayout.setColumnData(tc, new ColumnWeightData(1, true));
      }
      {
         /*
          * 2. column: color for definition/item
          */
         final TreeViewerColumn tvc = new TreeViewerColumn(_colorViewer, SWT.TRAIL);
         final TreeColumn tc = tvc.getColumn();
         tvc.setLabelProvider(new StyledCellLabelProvider() {
            @Override
            public void update(final ViewerCell cell) {

               final Object element = cell.getElement();

               final Color backgroundColor = _colorViewer.getTree().getBackground();

               if (element instanceof ColorDefinition) {

                  cell.setImage(_graphColorPainter.drawColorDefinitionImage(
                        (ColorDefinition) element,
                        numHorizontalImages,
                        false,
                        backgroundColor));

               } else if (element instanceof GraphColorItem) {

                  // draw map legend image horizontally

                  cell.setImage(_graphColorPainter.drawGraphColorImage(
                        (GraphColorItem) element,
                        numHorizontalImages,
                        false,
                        backgroundColor));

               } else {

                  cell.setImage(null);
               }
            }
         });
         treeLayout.setColumnData(tc, new ColumnPixelData(colorWidth, true));
      }
   }

   private void doLiveUpdate() {

      if (_chkLiveUpdate.getSelection()) {
         performApply();
      }
   }

   @Override
   public IGradientColorProvider getMapLegendColorProvider() {
      return _legendImageColorProvider;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _colorViewer;
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(_prefStore_Common);
   }

   /**
    * Setup legend and create a dummy color provider.
    */
   private void initializeLegend() {

      _legendImageColorProvider = createMap2ColorProvider();

      _dialogMappingColor = new DialogMap2ColorEditor(
            Display.getCurrent().getActiveShell(),
            _legendImageColorProvider,
            this);
   }

   private void logAllColorValues() {

      final StringBuilder sb = new StringBuilder();

      final ColorDefinition[] allGraphColorDefinitions = GraphColorManager.getAllColorDefinitions();

      // log color definition
      for (final ColorDefinition colorDefinition : allGraphColorDefinitions) {
         sb.append(colorDefinition.toString());
      }

      // log map2 colors
      for (final ColorDefinition colorDefinition : allGraphColorDefinitions) {

         final Map2ColorProfile map2Color_New = colorDefinition.getMap2Color_New();
         if (map2Color_New != null) {
            sb.append(map2Color_New);
         }
      }

      System.out.println(NL + NL

            + UI.timeStampNano()

            + " [" + getClass().getSimpleName() + "] ()" + NL //$NON-NLS-1$ //$NON-NLS-2$

            + sb.toString()

      );

   }

   @Override
   public boolean okToLeave() {

      if (_isLogging_ColorValues) {
         logAllColorValues();
      }

      return super.okToLeave();
   }

   /**
    * is called when the color in the color selector has changed
    *
    * @param event
    */
   private void onSelect_ColorIn_ColorSelector(final PropertyChangeEvent event) {

      final RGB oldValue = (RGB) event.getOldValue();
      final RGB newValue = (RGB) event.getNewValue();

      ColorDefinition colorDefinition = null;

      if (!oldValue.equals(newValue) && _selectedColor != null) {

         // color has changed

         // update the data model
         _selectedColor.setRGB(newValue);

         colorDefinition = _selectedColor.getColorDefinition();

         /*
          * dispose the old color/image from the graph
          */
         _graphColorPainter.invalidateResources(
               _selectedColor.getColorId(),
               colorDefinition.getColorDefinitionId());

         /*
          * update the tree viewer, the color images will then be recreated
          */
         _colorViewer.update(_selectedColor, null);
         _colorViewer.update(colorDefinition, null);

         _isColorChanged = true;
      }

      // log changes that it is easier to adjust the default values
      if (_selectedColor != null && _isLogging_ColorValues) {

         System.out.println(NL + NL

               + UI.timeStampNano()

               + " [" + getClass().getSimpleName() + "] ()" //$NON-NLS-1$ //$NON-NLS-2$
               + _selectedColor.getColorDefinition()

         );
      }
   }

   /**
    * is called when the color in the color viewer was selected
    */
   private void onSelect_ColorIn_ColorViewer() {

      final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

      _btnLegend.setEnabled(false);
      _colorSelector.setEnabled(false);

      if (selection.getFirstElement() instanceof GraphColorItem) {

         // graph color is selected

         final GraphColorItem graphColor = (GraphColorItem) selection.getFirstElement();

         // keep selected color
         _selectedColor = graphColor;

         if (graphColor.isMapColor()) {

            // legend color is selected

            _btnLegend.setEnabled(true);

         } else {

            // 'normal' color is selected

            // prepare color selector
            _colorSelector.setColorValue(graphColor.getRGB());
            _colorSelector.setEnabled(true);
         }

      } else {

         // color definition is selected

      }
   }

   /**
    * modify the colors of the legend
    */
   private void onSelect_MappingColor() {

      final ColorDefinition selectedColorDefinition = _selectedColor.getColorDefinition();

      // set the color which should be modified in the dialog
      _dialogMappingColor.setLegendColor(selectedColorDefinition);

      // new colors will be set with applyMapColors
      _dialogMappingColor.open();
   }

   @Override
   protected void performApply() {

      if (_isColorChanged) {
         updateAndSaveColors();
      }
   }

   @Override
   public boolean performCancel() {

      resetColors();
      _graphColorPainter.disposeAllResources();

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      final ColorDefinition[] graphColorDefinitions = GraphColorManager.getAllColorDefinitions();

      // update current colors
      for (final ColorDefinition graphDefinition : graphColorDefinitions) {

         graphDefinition.setGradientBright_New(graphDefinition.getGradientBright_Default());
         graphDefinition.setGradientDark_New(graphDefinition.getGradientDark_Default());

         graphDefinition.setLineColor_New_LightTheme(graphDefinition.getLineColor_Default_Light());
         graphDefinition.setLineColor_New_DarkTheme(graphDefinition.getLineColor_Default_Dark());

         graphDefinition.setTextColor_New_LightTheme(graphDefinition.getTextColor_Default_Light());
         graphDefinition.setTextColor_New_DarkTheme(graphDefinition.getTextColor_Default_Dark());

         final Map2ColorProfile defaultLegendColor = graphDefinition.getMap2Color_Default();
         if (defaultLegendColor != null) {
            graphDefinition.setMap2Color_New(defaultLegendColor.clone());
         }
      }

      _graphColorPainter.disposeAllResources();
      _colorViewer.refresh();

      _isColorChanged = true;

      // live update
      _chkLiveUpdate.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE));

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      if (_isColorChanged) {
         updateAndSaveColors();
      }

      _graphColorPainter.disposeAllResources();

      // live update
      _prefStore.setValue(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE, _chkLiveUpdate.getSelection());

      return super.performOk();
   }

   private void resetColors() {

      for (final ColorDefinition graphDefinition : GraphColorManager.getAllColorDefinitions()) {

         graphDefinition.setGradientBright_New(graphDefinition.getGradientBright_Active());
         graphDefinition.setGradientDark_New(graphDefinition.getGradientDark_Active());

         graphDefinition.setLineColor_New_LightTheme(graphDefinition.getLineColor_Active_Light());
         graphDefinition.setLineColor_New_DarkTheme(graphDefinition.getLineColor_Active_Dark());

         graphDefinition.setTextColor_New_LightTheme(graphDefinition.getTextColor_Active_Light());
         graphDefinition.setTextColor_New_DarkTheme(graphDefinition.getTextColor_Active_Dark());

         graphDefinition.setMap2Color_New(graphDefinition.getMap2Color_Active());
      }
   }

   private void restoreState() {

      // live update
      _chkLiveUpdate.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE));

      _colorSelector.restoreCustomColors(_state);
   }

   private void updateAndSaveColors() {

      GraphColorManager.saveColors();

      MapColorProvider.updateMap2Colors();

      _colorSelector.saveCustomColors(_state);

      // force to change the status
      _prefStore.setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
   }

   private void updateColorsFromDialog(final ColorDefinition selectedColorDefinition,
                                       final Map2ColorProfile newMapColor) {

      // set new legend color
      selectedColorDefinition.setMap2Color_New(newMapColor);

      /*
       * dispose old color and image for the graph
       */
      _graphColorPainter.invalidateResources(
            _selectedColor.getColorId(),
            selectedColorDefinition.getColorDefinitionId());

      /*
       * update the tree viewer, the color images will be recreated
       */
      _colorViewer.update(_selectedColor, null);
      _colorViewer.update(selectedColorDefinition, null);

      _isColorChanged = true;
   }

}
