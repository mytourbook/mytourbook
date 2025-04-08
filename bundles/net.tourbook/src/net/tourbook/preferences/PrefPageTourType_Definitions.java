/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.GraphColorItem;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IGradientColorProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeBorder;
import net.tourbook.tourType.TourTypeColor;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.tourType.TourTypeImageConfig;
import net.tourbook.tourType.TourTypeLayout;
import net.tourbook.tourType.TourTypeManager;
import net.tourbook.tourType.TourTypeManager.TourTypeBorderData;
import net.tourbook.tourType.TourTypeManager.TourTypeColorData;
import net.tourbook.tourType.TourTypeManager.TourTypeLayoutData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageTourType_Definitions extends PreferencePage implements IWorkbenchPreferencePage, IColorTreeViewer {

   public static final String                 ID                     = "net.tourbook.preferences.PrefPageTourType_Definitions";    //$NON-NLS-1$

   private static final String                COLOR_UNIQUE_ID_PREFIX = "crId";                                                     //$NON-NLS-1$

   private static final String[]              SORT_PROPERTY          = new String[] { "this property is needed for sorting !!!" }; //$NON-NLS-1$

   private final IPreferenceStore             _prefStore             = TourbookPlugin.getPrefStore();

   private GraphColorPainter                  _graphColorPainter;

   private ColorDefinition                    _expandedItem;

   private TourTypeColorDefinition            _selectedTourTypeColorDef;
   private GraphColorItem                     _selectedGraphColor;
   private List<TourType>                     _allDbTourTypes;

   /**
    * This is the model of the tour type viewer.
    */
   private ArrayList<TourTypeColorDefinition> _allTourTypeColorDefinitions;

   private boolean                            _isModified;
   private boolean                            _isLayoutModified;

   private boolean                            _isRecreateTourTypeImages;
   private boolean                            _isNavigationKeyPressed;

   private boolean                            _isInUpdateUI;
   private boolean                            _isTourTypeModified;
   private boolean                            _canModifyAnything     = true;
   private boolean                            _isDefaultTourType;

   /*
    * UI controls
    */
   private TreeViewer            _tourTypeViewer;

   private Button                _btnAdd;
   private Button                _btnDelete;
   private Button                _btnTourType_Save;
   private Button                _btnTourType_Cancel;

   private Button                _chkIsDefaultTourType;

   private ColorSelectorExtended _colorSelector;

   private Combo                 _comboFillColor1;
   private Combo                 _comboFillColor2;
   private Combo                 _comboFillLayout;
   private Combo                 _comboBorderColor;
   private Combo                 _comboBorderLayout;

   private Label                 _lblImportCategory;
   private Label                 _lblImportSubCategory;
   private Label                 _lblName;

   private Spinner               _spinnerBorder;
   private Spinner               _spinnerImageScale;

   private Text                  _txtImportCategory;
   private Text                  _txtImportSubCategory;
   private Text                  _txtName;

   private class ColorDefinitionContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {

         if (parentElement instanceof ColorDefinition) {

            return ((ColorDefinition) parentElement).getGraphColorItems();
         }

         return null;
      }

      @Override
      public Object[] getElements(final Object inputElement) {

         return _allTourTypeColorDefinitions.toArray(new Object[_allTourTypeColorDefinitions.size()]);
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

   private static final class TourTypeComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 instanceof final TourTypeColorDefinition ttc1
               && e2 instanceof final TourTypeColorDefinition ttc2) {

            return ttc1.compareTo(ttc2);
         }

         return 0;
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force sorting when the name has changed
         return true;
      }
   }

   public class TourTypeComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      /*
       * Ensure that a tour is NOT modified because changing the tour type needs an app restart
       * because the tour type images are DISPOSED
       */
      if (_canModifyAnything == false) {

         final Label label = new Label(parent, SWT.WRAP);
         label.setText(Messages.Pref_TourTypes_Label_TourIsDirty);
         GridDataFactory.fillDefaults().applyTo(label);

         return label;
      }

      initUI(parent);

      final Composite ui = createUI(parent);

      fillUI();

      // read tour types from the database
      _allDbTourTypes = TourDatabase.getAllTourTypes();

      /*
       * create color definitions for all tour types
       */
      _allTourTypeColorDefinitions = new ArrayList<>();

      if (_allDbTourTypes != null) {

         for (final TourType tourType : _allDbTourTypes) {

            final long typeId = tourType.getTypeId();

            // create a unique name for each tour type
            final Object colorId = typeId == TourDatabase.ENTITY_IS_NOT_SAVED
                  ? COLOR_UNIQUE_ID_PREFIX + tourType.getCreateId()
                  : typeId;

            final String colorName = "tourtype." + colorId; //$NON-NLS-1$

            final TourTypeColorDefinition colorDefinition = new TourTypeColorDefinition(
                  tourType,
                  colorName,
                  tourType.getName(),
                  tourType.getRGB_Gradient_Bright(),
                  tourType.getRGB_Gradient_Dark(),
                  tourType.getRGB_Line_LightTheme(),
                  tourType.getRGB_Line_DarkTheme(),
                  tourType.getRGB_Text_LightTheme(),
                  tourType.getRGB_Text_DarkTheme()

            );

            _allTourTypeColorDefinitions.add(colorDefinition);

            createGraphColorItems(colorDefinition);
         }
      }

      restoreState();
      enableControls();

      /*
       * MUST be run async otherwise the background color is NOT themed !!!
       */
      parent.getDisplay().asyncExec(() -> {

         _tourTypeViewer.setInput(this);

         setFocusToViewer();
      });

      return ui;
   }

   /**
    * Create the different color names (childs) for the color definition.
    */
   private void createGraphColorItems(final ColorDefinition colorDefinition) {

      // use the first 4 color, the mapping color is not used in tour types
      final int graphNamesLength = GraphColorManager.colorNames.length - 1;

      final GraphColorItem[] graphColors = new GraphColorItem[graphNamesLength];

      for (int nameIndex = 0; nameIndex < graphNamesLength; nameIndex++) {

         graphColors[nameIndex] = new GraphColorItem(
               colorDefinition,
               GraphColorManager.colorNames[nameIndex][0],
               GraphColorManager.colorNames[nameIndex][1],
               false);
      }

      colorDefinition.setColorItems(graphColors);
   }

   private Composite createUI(final Composite parent) {

      final Label label = new Label(parent, SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);
      label.setText(Messages.Pref_TourTypes_Title);

      // container
      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_10_ColorViewer(container);
         createUI_20_Actions(container);
         createUI_30_SelectedTourType(container);
         createUI_50_ImageLayout(container);
      }

      // must be set after the viewer is created
      _graphColorPainter = new GraphColorPainter(this);

      return container;
   }

   private void createUI_10_ColorViewer(final Composite parent) {

      final Display display = parent.getDisplay();

      /*
       * create tree layout
       */
      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(150, 100)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * create viewer
       */
      final Tree tree = new Tree(
            layoutContainer,
            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourTypeViewer = new TreeViewer(tree);
      defineAllColumns(treeLayout, tree);

      _tourTypeViewer.setContentProvider(new ColorDefinitionContentProvider());

      _tourTypeViewer.setComparator(new TourTypeComparator());
      _tourTypeViewer.setComparer(new TourTypeComparer());

      _tourTypeViewer.setUseHashlookup(true);

      _tourTypeViewer.getTree().addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         _isNavigationKeyPressed = false;

         switch (keyEvent.keyCode) {

         case SWT.ARROW_UP:
         case SWT.ARROW_DOWN:
            _isNavigationKeyPressed = true;
            break;

         case SWT.DEL:

            if (_btnDelete.isEnabled()) {
               onTourType_Delete();
            }

            break;
         }
      }));

      _tourTypeViewer.addSelectionChangedListener(selectionChangedEvent -> {

         final boolean isNavigationKeyPressed = _isNavigationKeyPressed;

         if (_isNavigationKeyPressed) {

            // don't expand when navigation key is pressed

            _isNavigationKeyPressed = false;

         } else {

            // expand/collapse tree item

            final Object selectedItem = _tourTypeViewer.getStructuredSelection().getFirstElement();

            if (selectedItem instanceof final ColorDefinition colorDefinition) {

               // expand/collapse current item

               if (_tourTypeViewer.getExpandedState(colorDefinition)) {

                  // item is expanded -> collapse

                  _tourTypeViewer.collapseToLevel(colorDefinition, 1);

               } else {

                  // item is collapsed -> expand

                  if (_expandedItem != null) {
                     _tourTypeViewer.collapseToLevel(_expandedItem, 1);
                  }
                  _tourTypeViewer.expandToLevel(colorDefinition, 1);
                  _expandedItem = colorDefinition;

                  // expanding the triangle, the layout is correctly done but not with double click
                  layoutContainer.layout(true, true);
               }
            }
         }

         onTourTypeViewer_Selection(isNavigationKeyPressed);
      });

      _tourTypeViewer.addTreeListener(new ITreeViewerListener() {

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

               /*
                * run not in the treeExpand method, this is blocked by the viewer with the message:
                * Ignored reentrant call while viewer is busy
                */
               display.asyncExec(() -> {

                  if (_expandedItem != null) {
                     _tourTypeViewer.collapseToLevel(_expandedItem, 1);
                  }

                  _tourTypeViewer.expandToLevel(treeItem, 1);
                  _expandedItem = treeItem;
               });
            }
         }
      });
   }

   private void createUI_20_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Color selector
             */
            _colorSelector = new ColorSelectorExtended(container);
            _colorSelector.addListener(event -> onTourType_ModifyColor(event));
            setButtonLayoutData(_colorSelector.getButton());
         }
         {
            /*
             * Add
             */
            _btnAdd = new Button(container, SWT.NONE);
            _btnAdd.setText(Messages.Pref_TourTypes_Button_add);
            _btnAdd.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
               onTourType_Add();
               enableControls();
            }));
            setButtonLayoutData(_btnAdd);
         }
         {
            /*
             * Delete
             */
            _btnDelete = new Button(container, SWT.NONE);
            _btnDelete.setText(Messages.Pref_TourTypes_Button_delete);
            _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {
               onTourType_Delete();
               enableControls();
            }));
            setButtonLayoutData(_btnDelete);
         }
         {
            /*
             * Button: Save/update
             */
            _btnTourType_Save = new Button(container, SWT.NONE);
            _btnTourType_Save.setText(Messages.App_Action_Save);
            _btnTourType_Save.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onTourTypeOptions_Save()));

            setButtonLayoutData(_btnTourType_Save);

            // align at the bottom
            final GridData gd = (GridData) _btnTourType_Save.getLayoutData();
            gd.verticalAlignment = SWT.BOTTOM;
            gd.grabExcessVerticalSpace = true;
         }
         {
            /*
             * Button: Cancel
             */
            _btnTourType_Cancel = new Button(container, SWT.NONE);
            _btnTourType_Cancel.setText(Messages.App_Action_Cancel);
            _btnTourType_Cancel.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onTourTypeOptions_Cancel()));
            setButtonLayoutData(_btnTourType_Cancel);
         }
      }
   }

   private void createUI_30_SelectedTourType(final Composite parent) {

      final ModifyListener modifyListener = modifyEvent -> onTourType_Modify();
      final SelectionListener widgetSelectedAdapter = SelectionListener.widgetSelectedAdapter(selectionEvent -> onTourType_Modify());

      final String tooltipCategory = Messages.Pref_TourTypes_Label_ImportCategory_Tooltip;
      final String tooltipSubCategory = Messages.Pref_TourTypes_Label_ImportSubCategory_Tooltip;

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_TourTypes_Group_SelectedTourType);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(group);
      GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(group);
      {
         {
            /*
             * Import category
             */
            _lblName = new Label(group, SWT.NONE);
            _lblName.setText(Messages.Pref_TourTypes_Label_Name);

            _txtName = new Text(group, SWT.BORDER);
            _txtName.addModifyListener(modifyListener);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtName);
         }
         {
            /*
             * Default tour type
             */
            _chkIsDefaultTourType = new Button(group, SWT.CHECK);
            _chkIsDefaultTourType.setText(Messages.Pref_TourTypes_Checkbox_DefaultTourType);
            _chkIsDefaultTourType.setToolTipText(Messages.Pref_TourTypes_Checkbox_DefaultTourType_Tooltip);
            _chkIsDefaultTourType.addSelectionListener(widgetSelectedAdapter);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_chkIsDefaultTourType);

         }
         {
            /*
             * Import category
             */
            _lblImportCategory = new Label(group, SWT.NONE);
            _lblImportCategory.setText(Messages.Pref_TourTypes_Label_ImportCategory);
            _lblImportCategory.setToolTipText(tooltipCategory);

            _txtImportCategory = new Text(group, SWT.BORDER);
            _txtImportCategory.setToolTipText(tooltipCategory);
            _txtImportCategory.addModifyListener(modifyListener);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtImportCategory);
         }
         {
            /*
             * Import sub category
             */
            _lblImportSubCategory = new Label(group, SWT.NONE);
            _lblImportSubCategory.setText(Messages.Pref_TourTypes_Label_ImportSubCategory);
            _lblImportSubCategory.setToolTipText(tooltipSubCategory);

            _txtImportSubCategory = new Text(group, SWT.BORDER);
            _txtImportSubCategory.setToolTipText(tooltipSubCategory);
            _txtImportSubCategory.addModifyListener(modifyListener);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtImportSubCategory);
         }
      }
   }

   private void createUI_50_ImageLayout(final Composite parent) {

      final SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> {
         onSelectImageLayout();
      });

      final MouseWheelListener mouseWheelListener_Spinner = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onSelectImageLayout();
      };

      final MouseWheelListener mouseWheelListener_Scaling = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
         onSelectImageLayout();
      };

      final MouseWheelListener mouseWheelListener_Combo = mouseEvent -> {
         onSelectImageLayout();
      };

      final GridDataFactory gridData_AlignVerticalCenter = GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_TourTypes_Group_CommonLayout);
      GridDataFactory.fillDefaults().grab(true, false).indent(0, 20).span(2, 1).applyTo(group);
      GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(group);
      {
         {
            /*
             * Image layout
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Pref_TourTypes_Label_ImageLayout);
            gridData_AlignVerticalCenter.applyTo(label);

            final Composite containerImage = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerImage);
            GridLayoutFactory.fillDefaults().numColumns(3).applyTo(containerImage);
            {
               // combo fill layout
               _comboFillLayout = new Combo(containerImage, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboFillLayout.setVisibleItemCount(20);
               _comboFillLayout.addSelectionListener(selectionListener);
               _comboFillLayout.addMouseWheelListener(mouseWheelListener_Combo);

               // combo color 1
               _comboFillColor1 = new Combo(containerImage, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboFillColor1.setVisibleItemCount(20);
               _comboFillColor1.addSelectionListener(selectionListener);
               _comboFillColor1.addMouseWheelListener(mouseWheelListener_Combo);

               // combo color 2
               _comboFillColor2 = new Combo(containerImage, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboFillColor2.setVisibleItemCount(20);
               _comboFillColor2.addSelectionListener(selectionListener);
               _comboFillColor2.addMouseWheelListener(mouseWheelListener_Combo);
            }
         }
         {
            /*
             * Image scale
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Pref_TourTypes_Label_ImageScaling);
            gridData_AlignVerticalCenter.applyTo(label);

            final Composite containerScale = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerScale);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerScale);
            {
               // spinner
               _spinnerImageScale = new Spinner(containerScale, SWT.BORDER);
               _spinnerImageScale.setMinimum(10);
               _spinnerImageScale.setMaximum(200);
               _spinnerImageScale.setPageIncrement(10);
               _spinnerImageScale.addSelectionListener(selectionListener);
               _spinnerImageScale.addMouseWheelListener(mouseWheelListener_Scaling);

               // %
               UI.createLabel(containerScale, UI.SYMBOL_PERCENTAGE);
            }
         }
         {
            /*
             * Border layout
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Pref_TourTypes_Label_BorderLayout);
            gridData_AlignVerticalCenter.applyTo(label);

            final Composite containerBorder = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerBorder);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerBorder);
            {
               // combo
               _comboBorderLayout = new Combo(containerBorder, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboBorderLayout.setVisibleItemCount(20);
               _comboBorderLayout.addSelectionListener(selectionListener);
               _comboBorderLayout.addMouseWheelListener(mouseWheelListener_Combo);

               // combo color
               _comboBorderColor = new Combo(containerBorder, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboBorderColor.setVisibleItemCount(20);
               _comboBorderColor.addSelectionListener(selectionListener);
               _comboBorderColor.addMouseWheelListener(mouseWheelListener_Combo);
            }
         }
         {
            /*
             * Border width
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Pref_TourTypes_Label_BorderWidth);
            gridData_AlignVerticalCenter.applyTo(label);

            // spinner
            _spinnerBorder = new Spinner(group, SWT.BORDER);
            _spinnerBorder.setMinimum(0);
            _spinnerBorder.setMaximum(10);
            _spinnerBorder.addSelectionListener(selectionListener);
            _spinnerBorder.addMouseWheelListener(mouseWheelListener_Spinner);
         }
      }

   }

   /**
    * Create columns
    */
   private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

      defineColumn_10_TourTypeImage(treeLayout);
      defineColumn_20_UpdatedTourTypeImage(treeLayout);
      defineColumn_30_ColorDefinition(treeLayout, tree);

      defineColumn_39_DefaultTourType(treeLayout);
      defineColumn_40_Category(treeLayout);
      defineColumn_50_SubCategory(treeLayout);
   }

   private void defineColumn_10_TourTypeImage(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.LEAD);

      final TreeColumn tc = tvc.getColumn();
      tc.setText(Messages.Pref_TourTypes_Column_TourType);

      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               cell.setText(((TourTypeColorDefinition) (element)).getVisibleName());

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());

               cell.setImage(tourTypeImage);

            } else if (element instanceof GraphColorItem) {

               cell.setText(((GraphColorItem) (element)).getName());
               cell.setImage(null);

            } else {

               cell.setText(UI.EMPTY_STRING);
               cell.setImage(null);
            }
         }
      });

      treeLayout.setColumnData(tc, new ColumnWeightData(20, true));
   }

   /**
    * Color definition with fully updated tour type image
    */
   private void defineColumn_20_UpdatedTourTypeImage(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.LEAD);
      final TreeColumn tc = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage_New(tourType.getTypeId());

               cell.setImage(tourTypeImage);

            } else {

               cell.setImage(null);
            }
         }
      });
      treeLayout.setColumnData(tc, new ColumnPixelData(convertHorizontalDLUsToPixels(16), true));
   }

   /**
    * All colors for a definition
    */
   private void defineColumn_30_ColorDefinition(final TreeColumnLayout treeLayout, final Tree tree) {

      final int numHorizontalImages = 6;

      final int itemHeight = tree.getItemHeight();
      final int oneColorWidth = itemHeight + GraphColorPainter.GRAPH_COLOR_SPACING;

      final int colorImageWidth = (oneColorWidth * numHorizontalImages) - 10;

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.TRAIL);
      final TreeColumn tc = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final Color backgroundColor = _tourTypeViewer.getTree().getBackground();

            if (element instanceof ColorDefinition) {

               final Image image = _graphColorPainter.drawColorDefinitionImage(
                     (ColorDefinition) element,
                     numHorizontalImages,
                     _isRecreateTourTypeImages,
                     backgroundColor);

               cell.setImage(image);

            } else if (element instanceof GraphColorItem) {

               final Image image = _graphColorPainter.drawGraphColorImage(
                     (GraphColorItem) element,
                     numHorizontalImages,
                     _isRecreateTourTypeImages,
                     backgroundColor);

               cell.setImage(image);

            } else {

               cell.setImage(null);
            }
         }
      });

      treeLayout.setColumnData(tc, new ColumnPixelData(colorImageWidth, true));
   }

   private void defineColumn_39_DefaultTourType(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.CENTER);

      final TreeColumn tc = tvc.getColumn();
      tc.setText(Messages.Pref_TourTypes_Column_Default);
      tc.setToolTipText(Messages.Pref_TourTypes_Column_Default_Tooltip);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();

               final long tourTypeID = tourType.getTypeId();
               final long prefTourTypeDefaultID = _prefStore.getLong(ITourbookPreferences.TOUR_TYPE_IMPORT_DEFAUL_ID);

               final boolean isDefaultTourType = prefTourTypeDefaultID == tourTypeID;

               cell.setText(isDefaultTourType ? UI.SYMBOL_HEAVY_CHECK_MARK : UI.EMPTY_STRING);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      treeLayout.setColumnData(tc, new ColumnPixelData(convertWidthInCharsToPixels(8), true));
   }

   private void defineColumn_40_Category(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.LEAD);

      final TreeColumn tc = tvc.getColumn();
      tc.setText(Messages.Pref_TourTypes_Column_Category);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final String importCategory = tourType.getImportCategory();

               cell.setText(importCategory != null ? importCategory : UI.EMPTY_STRING);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      treeLayout.setColumnData(tc, new ColumnWeightData(10, true));
   }

   private void defineColumn_50_SubCategory(final TreeColumnLayout treeLayout) {

      final TreeViewerColumn tvc = new TreeViewerColumn(_tourTypeViewer, SWT.LEAD);

      final TreeColumn tc = tvc.getColumn();
      tc.setText(Messages.Pref_TourTypes_Column_SubCategory);

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TourTypeColorDefinition) {

               final TourType tourType = ((TourTypeColorDefinition) element).getTourType();
               final String importSubCategory = tourType.getImportSubCategory();

               cell.setText(importSubCategory != null ? importSubCategory : UI.EMPTY_STRING);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      treeLayout.setColumnData(tc, new ColumnWeightData(10, true));
   }

   @Override
   public void dispose() {

      if (_graphColorPainter != null) {
         _graphColorPainter.disposeAllResources();
      }

      TourTypeImage.disposeRecreatedImages();

      super.dispose();
   }

   private void enableControls() {

      final Object selectedItem = _tourTypeViewer.getStructuredSelection().getFirstElement();

      boolean canDeleteColor = false;
      boolean canEditTourType = false;
      boolean isGraphSelected = false;
      final boolean isModified = _isTourTypeModified;
      final boolean isNotModified = isModified == false;

      boolean canEditDefault = false;

      if (selectedItem instanceof GraphColorItem) {

         isGraphSelected = true;
         canDeleteColor = true;

      } else if (selectedItem instanceof final TourTypeColorDefinition tourTypeColorDef) {

         canEditTourType = true;
         canDeleteColor = true;

         if (tourTypeColorDef.getTourType().getTypeId() != TourDatabase.ENTITY_IS_NOT_SAVED) {

            // only a saved tour type can be set as default
            canEditDefault = true;
         }
      }

// SET_FORMATTING_OFF

      _btnAdd                    .setEnabled(isNotModified);
      _btnDelete                 .setEnabled(isNotModified && canDeleteColor);
      _btnTourType_Cancel        .setEnabled(isModified);
      _btnTourType_Save          .setEnabled(isModified);

      _chkIsDefaultTourType      .setEnabled(canEditDefault);

      _lblImportCategory         .setEnabled(canEditTourType);
      _lblImportSubCategory      .setEnabled(canEditTourType);
      _lblName                   .setEnabled(canEditTourType);

      _txtImportCategory         .setEnabled(canEditTourType);
      _txtImportSubCategory      .setEnabled(canEditTourType);
      _txtName                   .setEnabled(canEditTourType);

      _tourTypeViewer.getTree()  .setEnabled(isNotModified);

      _colorSelector             .setEnabled(isNotModified && isGraphSelected);

// SET_FORMATTING_ON
   }

   private void enableLayoutControls() {

      final TourTypeLayoutData layoutData = getSelectedTourTypeLayoutData();

      _comboFillColor1.setEnabled(layoutData.isColor1);
      _comboFillColor2.setEnabled(layoutData.isColor2);
   }

   private void fillUI() {

      /*
       * Image layout
       */
      for (final TourTypeLayoutData data : TourTypeManager.getAllTourTypeLayoutData()) {
         _comboFillLayout.add(data.label);
      }
      for (final TourTypeColorData data : TourTypeManager.getAllTourTypeColorData()) {
         _comboFillColor1.add(data.label);
      }
      for (final TourTypeColorData data : TourTypeManager.getAllTourTypeColorData()) {
         _comboFillColor2.add(data.label);
      }

      /*
       * Border layout
       */
      for (final TourTypeBorderData data : TourTypeManager.getAllTourTypeBorderData()) {
         _comboBorderLayout.add(data.label);
      }
      for (final TourTypeColorData data : TourTypeManager.getAllTourTypeColorData()) {
         _comboBorderColor.add(data.label);
      }
   }

   private void fireModifyEvent() {

      if (_isModified) {

         _isModified = false;

         TourTypeManager.saveState();

         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED, Math.random());

         if (_isLayoutModified) {

            _isLayoutModified = false;

            // show restart info
            final MessageDialog messageDialog = new MessageDialog(
                  getShell(),
                  Messages.Pref_TourTypes_Dialog_Restart_Title,
                  null,
                  Messages.Pref_TourTypes_Dialog_Restart_Message_2,
                  MessageDialog.INFORMATION,
                  new String[] { Messages.App_Action_RestartApp, IDialogConstants.NO_LABEL },
                  1);

            if (messageDialog.open() == Window.OK) {
               getShell().getDisplay().asyncExec(() -> PlatformUI.getWorkbench().restart());
            }
         }
      }
   }

   @Override
   public IGradientColorProvider getMapLegendColorProvider() {
      return null;
   }

   /**
    * @return Returns the selected color definitions in the color viewer
    */
   private List<TourTypeColorDefinition> getSelectedColorDefinitions() {

      final List<TourTypeColorDefinition> allSelectedColorDefinitions = new ArrayList<>();

      final ITreeSelection allSelectedItems = _tourTypeViewer.getStructuredSelection();

      for (final Object selectedItem : allSelectedItems) {

         if (selectedItem instanceof final GraphColorItem graphColor) {

            final ColorDefinition colorDef = graphColor.getColorDefinition();

            if (colorDef instanceof final TourTypeColorDefinition tourTypeColorDef) {

               allSelectedColorDefinitions.add(tourTypeColorDef);
            }

         } else if (selectedItem instanceof final TourTypeColorDefinition tourTypeColorDef) {

            allSelectedColorDefinitions.add(tourTypeColorDef);
         }
      }

      return allSelectedColorDefinitions;
   }

   private TourTypeBorder getSelectedTourTypeBorderLayout() {

      final int selectedIndex = _comboBorderLayout.getSelectionIndex();

      if (selectedIndex < 0) {
         return TourTypeManager.DEFAULT_BORDER_LAYOUT;
      }

      return TourTypeManager.getAllTourTypeBorderData()[selectedIndex].tourTypeBorder;
   }

   private TourTypeColor getSelectedTourTypeColor(final Combo comboColor) {

      final int selectedIndex = comboColor.getSelectionIndex();

      if (selectedIndex < 0) {
         return TourTypeManager.DEFAULT_BORDER_COLOR;
      }

      return TourTypeManager.getAllTourTypeColorData()[selectedIndex].tourTypeColor;
   }

   private TourTypeLayout getSelectedTourTypeLayout() {

      final int selectedIndex = _comboFillLayout.getSelectionIndex();

      if (selectedIndex < 0) {
         return TourTypeManager.DEFAULT_IMAGE_LAYOUT;
      }

      return TourTypeManager.getAllTourTypeLayoutData()[selectedIndex].tourTypeLayout;
   }

   private TourTypeLayoutData getSelectedTourTypeLayoutData() {

      final TourTypeLayoutData[] allTourTypeLayoutData = TourTypeManager.getAllTourTypeLayoutData();

      final int selectedIndex = _comboFillLayout.getSelectionIndex();

      if (selectedIndex < 0) {
         return allTourTypeLayoutData[0];
      }

      return allTourTypeLayoutData[selectedIndex];
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourTypeViewer;
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      /*
       * Ensure that a tour is NOT modified because changing the tour type needs an app restart
       * because the tour type images are DISPOSED
       */
      if (TourManager.isTourEditorModified(false)) {

         _canModifyAnything = false;

         noDefaultAndApplyButton();
      }

   }

   private void initUI(final Composite parent) {

   }

   @Override
   public boolean okToLeave() {

      if (_canModifyAnything) {

         fireModifyEvent();
      }

      return super.okToLeave();
   }

   private void onSelectImageLayout() {

      final TourTypeImageConfig imageConfig = TourTypeManager.getImageConfig();

      imageConfig.imageColor1 = getSelectedTourTypeColor(_comboFillColor1);
      imageConfig.imageColor2 = getSelectedTourTypeColor(_comboFillColor2);
      imageConfig.imageLayout = getSelectedTourTypeLayout();
      imageConfig.imageScaling = _spinnerImageScale.getSelection();

      imageConfig.borderColor = getSelectedTourTypeColor(_comboBorderColor);
      imageConfig.borderLayout = getSelectedTourTypeBorderLayout();
      imageConfig.borderWidth = _spinnerBorder.getSelection();

      // set tour type images dirty
      TourTypeImage.setTourTypeImagesDirty();

      _isRecreateTourTypeImages = true;
      {
         _tourTypeViewer.refresh(true);

         // do a redraw in the tree viewer, the color images will be recreated in the label provider
         _tourTypeViewer.getTree().redraw();
      }
      _isRecreateTourTypeImages = false;

      _isModified = true;
      _isLayoutModified = true;

      enableLayoutControls();
   }

   private void onTourType_Add() {

      // ask for the tour type name
      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypes_Dlg_new_tour_type_title,
            Messages.Pref_TourTypes_Dlg_new_tour_type_msg,
            UI.EMPTY_STRING,
            null);

      if (inputDialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      final String tourTypeName = inputDialog.getValue();

      // create new tour type
      final TourType newTourType = new TourType(tourTypeName);

      /*
       * Create a dummy definition to get the default colors
       */
      final TourTypeColorDefinition dummyColorDef = new TourTypeColorDefinition(
            newTourType,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING);

      newTourType.setColor_Gradient_Bright(dummyColorDef.getGradientBright_Default());
      newTourType.setColor_Gradient_Dark(dummyColorDef.getGradientDark_Default());

      newTourType.setColor_Line(dummyColorDef.getLineColor_Default_Light(), dummyColorDef.getLineColor_Default_Dark());
      newTourType.setColor_Text(dummyColorDef.getTextColor_Default_Light(), dummyColorDef.getTextColor_Default_Dark());

      // add new entity to db
      final TourType savedTourType = tourType_Save(newTourType);

      if (savedTourType != null) {

         /*
          * Create a color definition WITH THE SAVED tour type, this fixes a VEEEEEEEEEEERY long
          * existing bug that a new tour type is initially not displayed correctly in the color
          * definition image.
          */
         // create the same color definition but with the correct id's
         final TourTypeColorDefinition newColorDefinition = new TourTypeColorDefinition(
               savedTourType,
               "tourType." + savedTourType.getTypeId(), //$NON-NLS-1$
               tourTypeName);

         // overwrite tour type object
         newColorDefinition.setTourType(savedTourType);

         createGraphColorItems(newColorDefinition);

         // update model
         _allTourTypeColorDefinitions.add(newColorDefinition);

         // update internal tour type list
         _allDbTourTypes.add(savedTourType);
         Collections.sort(_allDbTourTypes);

         // update UI
         _tourTypeViewer.add(this, newColorDefinition);

         _tourTypeViewer.setSelection(new StructuredSelection(newColorDefinition), true);

         _tourTypeViewer.collapseAll();
         _tourTypeViewer.expandToLevel(newColorDefinition, 1);

         _isModified = true;

         setFocusToViewer();
      }
   }

   private void onTourType_Delete() {

      final List<TourTypeColorDefinition> allSelectedColorDefinitions = getSelectedColorDefinitions();
      final List<TourType> allSelectedTourTypes = new ArrayList<>();

      allSelectedColorDefinitions.stream()
            .forEach(colorDefinition -> allSelectedTourTypes.add(colorDefinition.getTourType()));

      final List<String> allTourTypeNames = new ArrayList<>();

      allSelectedTourTypes.stream()
            .forEach(tourType -> allTourTypeNames.add(tourType.getName()));

      final String allTourTypeNamesJoined = StringUtils
            .join(allTourTypeNames.stream().toArray(String[]::new), UI.COMMA_SPACE);

      // confirm deletion
      final MessageDialog dialog = new MessageDialog(
            getShell(),
            Messages.Pref_TourTypes_Dlg_delete_tour_type_title,
            null,
            NLS.bind(Messages.Pref_TourTypes_Dlg_delete_tour_type_msg, allTourTypeNamesJoined),
            MessageDialog.QUESTION,
            new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL },
            1);

      if (dialog.open() != Window.OK) {

         setFocusToViewer();
         return;
      }

      BusyIndicator.showWhile(getShell().getDisplay(), () -> {

         for (final TourTypeColorDefinition selectedColorDefinition : allSelectedColorDefinitions) {

            final TourType selectedTourType = selectedColorDefinition.getTourType();

            // remove entity from the db
            if (tourType_Delete(selectedTourType)) {

               // update model
               _allDbTourTypes.remove(selectedTourType);
               _allTourTypeColorDefinitions.remove(selectedColorDefinition);

               // update UI
               _tourTypeViewer.remove(selectedColorDefinition);

// a tour type image cannot be deleted otherwise an image dispose exception can occur
//             TourTypeImage.deleteTourTypeImage(selectedTourType.getTypeId());

               _isModified = true;
            }
         }
      });

      setFocusToViewer();
   }

   private void onTourType_Modify() {

      if (_isInUpdateUI) {
         return;
      }

      _isTourTypeModified = true;

      enableControls();
   }

   /**
    * This is called when the color in the color selector is modified
    *
    * @param event
    */
   private void onTourType_ModifyColor(final PropertyChangeEvent event) {

      final RGB oldRGB = (RGB) event.getOldValue();
      final RGB newRGB = (RGB) event.getNewValue();

      if (_selectedGraphColor == null || oldRGB.equals(newRGB)) {
         return;
      }

      // color has changed

      // update model
      _selectedGraphColor.setRGB(newRGB);

      final TourTypeColorDefinition selectedColorDef = (TourTypeColorDefinition) _selectedGraphColor.getColorDefinition();

      /*
       * update tour type in the db
       */
      final TourType oldTourType = selectedColorDef.getTourType();

      oldTourType.setColor_Gradient_Bright(selectedColorDef.getGradientBright_New());
      oldTourType.setColor_Gradient_Dark(selectedColorDef.getGradientDark_New());

      oldTourType.setColor_Line(selectedColorDef.getLineColor_New_Light(), selectedColorDef.getLineColor_New_Dark());
      oldTourType.setColor_Text(selectedColorDef.getTextColor_New_Light(), selectedColorDef.getTextColor_New_Dark());

      final TourType savedTourType = tourType_Save(oldTourType);

      selectedColorDef.setTourType(savedTourType);

      // replace tour type with new one
      _allDbTourTypes.remove(oldTourType);
      _allDbTourTypes.add(savedTourType);
      Collections.sort(_allDbTourTypes);

      /*
       * Update UI
       */
      // invalidate old color/image from the graph and color definition to force the recreation
      _graphColorPainter.invalidateResources(
            _selectedGraphColor.getColorId(),
            selectedColorDef.getColorDefinitionId());

      // update UI
      TourTypeImage.setTourTypeImagesDirty();

      /*
       * update the tree viewer, the color images will be recreated in the label provider
       */
      _tourTypeViewer.update(_selectedGraphColor, null);
      _tourTypeViewer.update(selectedColorDef, null);

      // without a repaint the color def image is not updated
      _tourTypeViewer.getTree().redraw();

      _isModified = true;
   }

   private void onTourTypeOptions_Cancel() {

      _isTourTypeModified = false;

      updateUIFromModel();

      enableControls();
   }

   private void onTourTypeOptions_Save() {

      final TourTypeColorDefinition tourTypeColorDef = _selectedTourTypeColorDef;
      final TourType modifiedTourType = tourTypeColorDef.getTourType();

      final String tourTypeName = _txtName.getText().trim();

      modifiedTourType.setName(tourTypeName);
      modifiedTourType.setImportCategory(_txtImportCategory.getText());
      modifiedTourType.setImportSubCategory(_txtImportSubCategory.getText());

      tourTypeColorDef.setVisibleName(tourTypeName);

      // update entity in the db
      final TourType savedTourType = tourType_Save(modifiedTourType);

      if (savedTourType != null) {

         // update model
         tourTypeColorDef.setTourType(savedTourType);

         // replace tour type with new one
         _allDbTourTypes.remove(modifiedTourType);
         _allDbTourTypes.add(savedTourType);
         Collections.sort(_allDbTourTypes);

         // set default tour type
         final boolean isDefaultTourType_Modified = _chkIsDefaultTourType.getSelection();
         final long tourTypeID = savedTourType.getTypeId();
         final boolean isTourTypeSaved = tourTypeID != TourDatabase.ENTITY_IS_NOT_SAVED;

         // allow to set or remove the default tour type

         if (isDefaultTourType_Modified && isTourTypeSaved) {

            _prefStore.setValue(ITourbookPreferences.TOUR_TYPE_IMPORT_DEFAUL_ID, tourTypeID);

         } else if (_isDefaultTourType && isDefaultTourType_Modified == false) {

            _prefStore.setValue(ITourbookPreferences.TOUR_TYPE_IMPORT_DEFAUL_ID, -1);
         }

         // update viewer, resort types when necessary
         _tourTypeViewer.update(tourTypeColorDef, SORT_PROPERTY);

         _isModified = true;
      }

      _isTourTypeModified = false;

      enableControls();

      setFocusToViewer();
   }

   /**
    * Is called when a color in the color viewer is selected.
    *
    * @param isNavigationKeyPressed
    */
   private void onTourTypeViewer_Selection(final boolean isNavigationKeyPressed) {

      _selectedTourTypeColorDef = null;
      _selectedGraphColor = null;

      final Object selectedItem = _tourTypeViewer.getStructuredSelection().getFirstElement();

      if (selectedItem instanceof final GraphColorItem graphColor) {

         _selectedGraphColor = graphColor;

         _colorSelector.setColorValue(graphColor.getRGB());

         if (isNavigationKeyPressed == false) {

            // open color dialog only when not navigated with the keyboard

            /*
             * Run async that the UI do display the selected color in the color button when the
             * color dialog is opened
             */
            _tourTypeViewer.getTree().getDisplay().asyncExec(() -> {

               // open color selection dialog

               _colorSelector.open();
            });
         }

         final ColorDefinition graphColorDefinition = graphColor.getColorDefinition();

         if (graphColorDefinition instanceof final TourTypeColorDefinition tourTypeColorDef) {

            _selectedTourTypeColorDef = tourTypeColorDef;
         }

      } else if (selectedItem instanceof final TourTypeColorDefinition tourTypeColorDef) {

         _selectedTourTypeColorDef = tourTypeColorDef;
      }

      updateUIFromModel();
      enableControls();

      setFocusToViewer();
   }

   @Override
   public boolean performCancel() {

      if (_canModifyAnything) {

         fireModifyEvent();
      }

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      if (_canModifyAnything) {

         _spinnerImageScale.setSelection(TourTypeManager.DEFAULT_IMAGE_SCALING);

         _comboBorderColor.select(TourTypeManager.getTourTypeColorIndex(TourTypeManager.DEFAULT_BORDER_COLOR));
         _comboBorderLayout.select(TourTypeManager.getTourTypeBorderIndex(TourTypeManager.DEFAULT_BORDER_LAYOUT));
         _spinnerBorder.setSelection(TourTypeManager.DEFAULT_BORDER_WIDTH);

         _comboFillColor1.select(TourTypeManager.getTourTypeColorIndex(TourTypeManager.DEFAULT_IMAGE_COLOR1));
         _comboFillColor2.select(TourTypeManager.getTourTypeColorIndex(TourTypeManager.DEFAULT_IMAGE_COLOR2));
         _comboFillLayout.select(TourTypeManager.getTourTypeLayoutIndex(TourTypeManager.DEFAULT_IMAGE_LAYOUT));

         onSelectImageLayout();
      }

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      if (_canModifyAnything) {

         fireModifyEvent();
      }

      return super.performOk();
   }

   private void restoreState() {

      final TourTypeImageConfig imageConfig = TourTypeManager.getImageConfig();

      _spinnerImageScale.setSelection(imageConfig.imageScaling);

      _comboBorderColor.select(TourTypeManager.getTourTypeColorIndex(imageConfig.borderColor));
      _comboBorderLayout.select(TourTypeManager.getTourTypeBorderIndex(imageConfig.borderLayout));
      _spinnerBorder.setSelection(imageConfig.borderWidth);

      _comboFillColor1.select(TourTypeManager.getTourTypeColorIndex(imageConfig.imageColor1));
      _comboFillColor2.select(TourTypeManager.getTourTypeColorIndex(imageConfig.imageColor2));
      _comboFillLayout.select(TourTypeManager.getTourTypeLayoutIndex(imageConfig.imageLayout));

      enableLayoutControls();
   }

   private void setFocusToViewer() {

      // set focus back to the tree
      _tourTypeViewer.getTree().setFocus();
   }

   private boolean tourType_Delete(final TourType tourType) {

      if (tourType_Delete_10_FromTourData(tourType)) {
         if (tourType_Delete_20_FromDb(tourType)) {
            return true;
         }
      }

      return false;
   }

   private boolean tourType_Delete_10_FromTourData(final TourType tourType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();

      if (em != null) {

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT tourData" //$NON-NLS-1$
               + " FROM TourData AS tourData" //$NON-NLS-1$
               + " WHERE tourData.tourType.typeId=" + tourType.getTypeId()); //$NON-NLS-1$

         final List<?> tourDataList = query.getResultList();
         if (tourDataList.size() > 0) {

            final EntityTransaction ts = em.getTransaction();

            try {

               ts.begin();

               // remove tour type from all tour data
               for (final Object listItem : tourDataList) {

                  if (listItem instanceof TourData) {

                     final TourData tourData = (TourData) listItem;

                     tourData.setTourType(null);
                     em.merge(tourData);
                  }
               }

               ts.commit();

            } catch (final Exception e) {
               StatusUtil.showStatus(e);
            } finally {
               if (ts.isActive()) {
                  ts.rollback();
               }
            }
         }

         returnResult = true;
         em.close();
      }

      return returnResult;
   }

   private boolean tourType_Delete_20_FromDb(final TourType tourType) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourType tourTypeEntity = em.find(TourType.class, tourType.getTypeId());

         if (tourTypeEntity != null) {

            ts.begin();

            em.remove(tourTypeEntity);

            ts.commit();
         }

      } catch (final Exception e) {
         StatusUtil.showStatus(e);
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            returnResult = true;
         }
         em.close();
      }

      return returnResult;
   }

   private TourType tourType_Save(final TourType tourType) {

      return TourDatabase.saveEntity(
            tourType,
            tourType.getTypeId(),
            TourType.class);
   }

   private void updateUIFromModel() {

      final TourTypeColorDefinition tourTypeColorDef = _selectedTourTypeColorDef;

      if (tourTypeColorDef == null) {
         return;
      }

      final TourType tourType = tourTypeColorDef.getTourType();

      final String importCategory = tourType.getImportCategory();
      final String importSubCategory = tourType.getImportSubCategory();

      _isDefaultTourType = false;

      final long tourTypeID = tourType.getTypeId();

      if (tourTypeID != TourDatabase.ENTITY_IS_NOT_SAVED) {

         final long prefDefaultID = _prefStore.getLong(ITourbookPreferences.TOUR_TYPE_IMPORT_DEFAUL_ID);

         _isDefaultTourType = prefDefaultID == tourTypeID;
      }

      _isInUpdateUI = true;

// SET_FORMATTING_OFF

      _txtName                .setText(tourType.getName());
      _txtImportCategory      .setText(importCategory    == null ? UI.EMPTY_STRING : importCategory);
      _txtImportSubCategory   .setText(importSubCategory == null ? UI.EMPTY_STRING : importSubCategory);

      _chkIsDefaultTourType   .setSelection(_isDefaultTourType);

// SET_FORMATTING_ON

      _isInUpdateUI = false;
   }
}
