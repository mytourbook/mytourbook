/****************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

public class EquipmentView extends ViewPart implements ITourProvider, ITourViewer, ITreeViewer {

   public static final String  ID                                     = "net.tourbook.equipment.EquipmentView.ID"; //$NON-NLS-1$

   /**
    * The expanded equipment items have these structure:
    * <p>
    * 1. Type<br>
    * 2. id/year/month<br>
    * <br>
    * 3. Type<br>
    * 4. id/year/month<br>
    * ...
    */
   private static final String STATE_EXPANDED_ITEMS                   = "STATE_EXPANDED_ITEMS";                    //$NON-NLS-1$
   private static final String STATE_IS_ON_SELECT_EXPAND_COLLAPSE     = "STATE_IS_ON_SELECT_EXPAND_COLLAPSE";      //$NON-NLS-1$
   private static final String STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";  //$NON-NLS-1$

   private static final int    STATE_ITEM_TYPE_SEPARATOR              = -1;

//   private static final int                    STATE_ITEM_TYPE_CATEGORY                 = 1;
   private static final int STATE_ITEM_TYPE_EQUIPMENT = 2;
//   private static final int                    STATE_ITEM_TYPE_YEAR                     = 3;
//   private static final int                    STATE_ITEM_TYPE_MONTH                    = 4;

   private static final IPreferenceStore       _prefStore                               = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore       _prefStore_Common                        = CommonActivator.getPrefStore();
   private static final IDialogSettings        _state                                   = TourbookPlugin.getState(ID);

   private IPropertyChangeListener             _prefChangeListener;
   private IPropertyChangeListener             _prefChangeListener_Common;
   private ITourEventListener                  _tourEventListener;

   private PostSelectionProvider               _postSelectionProvider;

   private TreeViewer                          _equipmentViewer;
   private ColumnManager                       _columnManager;
   private TVIEquipmentView_EquipRoot          _rootItem;

   private MenuManager                         _viewerMenuManager;
   private Menu                                _treeContextMenu;
   private IContextMenuProvider                _viewerContextMenuProvider               = new TreeContextMenuProvider();

   private ActionDeleteEquipment               _actionDeleteEquipment;
   private ActionEditEquipment                 _actionEditEquipment;
   private ActionNewEquipment                  _actionNewEquipment;
   private ActionRefreshView                   _action_RefreshView;

   private Action_CollapseAll_WithoutSelection _actionCollapseAll_WithoutSelection;
   private ActionCollapseOthers                _actionCollapseOthers;
   private ActionExpandSelection               _actionExpandSelection;
   private Action_OnMouseSelect_ExpandCollapse _actionOnMouseSelect_ExpandCollapse;
   private Action_SingleExpand_CollapseOthers  _actionSingleExpand_CollapseOthers;

   private boolean                             _isBehaviour_SingleExpand_CollapseOthers = true;
   private boolean                             _isBehaviour_OnSelect_ExpandCollapse     = true;
   private boolean                             _isInCollapseAll;
   private boolean                             _isInExpandingSelection;
   private int                                 _expandRunnableCounter;
   private long                                _lastExpandSelectionTime;

   private boolean                             _isMouseContextMenu;
   private boolean                             _isSelectedWithKeyboard;

   private PixelConverter                      _pc;

   private Color                               _colorContentCategory;
   private Color                               _colorContentSubCategory;
   private Color                               _colorDateCategory;
   private Color                               _colorDateSubCategory;
   private Color                               _colorTour;

   /*
    * UI resources
    */
   private final Image _imgEquipment         = TourbookPlugin.getImage(Images.Equipment);
   private final Image _imgEquipment_All     = TourbookPlugin.getImage(Images.Equipment_All);
   private final Image _imgEquipment_Part    = TourbookPlugin.getImage(Images.Equipment_Part);
   private final Image _imgEquipment_Service = TourbookPlugin.getImage(Images.Equipment_Service);

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;

   private class Action_CollapseAll_WithoutSelection extends ActionCollapseAll {

      public Action_CollapseAll_WithoutSelection() {
         super(EquipmentView.this);
      }

      @Override
      public void run() {

         _isInCollapseAll = true;
         {
            super.run();
         }
         _isInCollapseAll = false;
      }
   }

   private class Action_OnMouseSelect_ExpandCollapse extends Action {

      public Action_OnMouseSelect_ExpandCollapse() {
         super(Messages.Tour_Tags_Action_OnMouseSelect_ExpandCollapse, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_OnMouseSelect_ExpandCollapse();
      }
   }

   private class Action_SingleExpand_CollapseOthers extends Action {

      public Action_SingleExpand_CollapseOthers() {
         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_SingleExpandCollapseOthers();
      }
   }

   private class ActionDeleteEquipment extends Action {

      ActionDeleteEquipment() {

         super(Messages.Action_Equipment_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
         onAction_DeleteEquipment();
      }
   }

   private class ActionEditEquipment extends Action {

      public ActionEditEquipment() {

         super(Messages.Action_Equipment_Edit, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Edit));
      }

      @Override
      public void run() {
         onAction_EditEquipment();
      }
   }

   private class ActionNewEquipment extends Action {

      public ActionNewEquipment() {

         super(Messages.Action_Equipment_New, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_New));
      }

      @Override
      public void run() {
         onAction_NewEquipment();
      }
   }

   /**
    * Comparator is sorting the tree items
    */
   private final class EquipmentComparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof final TVIEquipmentView_Equipment equip1 && obj2 instanceof final TVIEquipmentView_Equipment equip2) {

            // sort equipment by name

            return equip1.getEquipment().getName().compareTo(equip2.getEquipment().getName());

         }

         return 0;
      }
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <p>
    * A comparer is necessary to set and restore the expanded elements AND to reselect elements
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    */
   private class EquipmentComparer implements IElementComparer {

      @Override
      public boolean equals(final Object o1, final Object o2) {

         if (o1 == o2) {

            return true;

         } else if (o1 instanceof final TVIEquipmentView_Equipment item1
               && o2 instanceof final TVIEquipmentView_Equipment item2) {

            return item1.getEquipment().getEquipmentId() == item2.getEquipment().getEquipmentId();
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }

   }

   private final class EquipmentContentProvider implements ITreeContentProvider {

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {
         return ((TreeViewerItem) element).hasChildren();
      }
   }

   private class StateSegment {

      private long __itemType;
      private long __itemData;

      public StateSegment(final long itemType, final long itemData) {

         __itemType = itemType;
         __itemData = itemData;
      }
   }

   private class TreeContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_treeContextMenu != null) {
            _treeContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _treeContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _treeContextMenu = createUI_22_CreateViewerContextMenu();

         return _treeContextMenu;
      }
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _equipmentViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _equipmentViewer.refresh();

            /*
             * The tree must be redrawn because the styled text does not show with the new color
             */
            _equipmentViewer.getTree().redraw();

         } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            updateColors();

            _equipmentViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _equipmentViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not show with the new color
             */
            _equipmentViewer.getTree().redraw();

//       } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
//
//            // reselect current sensor that the sensor chart (when opened) is reloaded
//
//            final StructuredSelection selection = getViewerSelection();
//
//            _equipmentViewer.setSelection(selection, true);
//
//            final Table table = _equipmentViewer.getTable();
//            table.showSelection();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

//         final String property = propertyChangeEvent.getProperty();
//
//         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {
//
//            // measurement system has changed
//
//            _columnManager.saveState(_state);
//            _columnManager.clearColumns();
//
//            defineAllColumns();
//
//            _equipViewer = (TableViewer) recreateViewer(_equipViewer);
//         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (workbenchPart == EquipmentView.this) {
            return;
         }

         if (tourEventId == TourEventId.EQUIPMENT_STRUCTURE_CHANGED
               || tourEventId == TourEventId.TOUR_CHANGED) {

            reloadViewer();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionDeleteEquipment                 = new ActionDeleteEquipment();
      _actionEditEquipment                   = new ActionEditEquipment();
      _actionNewEquipment                    = new ActionNewEquipment();
      _action_RefreshView                    = new ActionRefreshView(this);

      _actionCollapseAll_WithoutSelection    = new Action_CollapseAll_WithoutSelection();
      _actionCollapseOthers                  = new ActionCollapseOthers(this);
      _actionExpandSelection                 = new ActionExpandSelection(this, true);
      _actionOnMouseSelect_ExpandCollapse    = new Action_OnMouseSelect_ExpandCollapse();
      _actionSingleExpand_CollapseOthers     = new Action_SingleExpand_CollapseOthers();

// SET_FORMATTING_ON
   }

   private void createMenuManager() {

//      _tagMenuManager = new TagMenuManager(this, true);
//      _tourTypeMenuManager = new TourTypeMenuManager(this);

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> {

//         _tourInfoToolTip.hideToolTip();

         fillContextMenu(menuManager);
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      initUI(parent);

      createMenuManager();

      // define all columns
      _columnManager = new ColumnManager(this, _state);
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns();

      createActions();
      fillActionBars();

      createUI(parent);

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      addPrefListener();
      addTourEventListener();

      restoreState();

      updateColors();
      reloadViewer();

      restoreState_Viewer();

      enableActions();
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_EqipmentViewer(_viewerContainer);
      }
   }

   private void createUI_10_EqipmentViewer(final Composite parent) {

      /*
       * Create tree
       */
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.FULL_SELECTION | SWT.MULTI);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * Create viewer
       */
      _equipmentViewer = new TreeViewer(tree);
      _columnManager.createColumns(_equipmentViewer);

      _equipmentViewer.setContentProvider(new EquipmentContentProvider());
      _equipmentViewer.setComparator(new EquipmentComparator());
      _equipmentViewer.setComparer(new EquipmentComparer());
//      _equipViewer.setFilters(new TagFilter());

      _equipmentViewer.setUseHashlookup(true);

      _equipmentViewer.addSelectionChangedListener(selectionChangedEvent -> onEquipmentViewer_Selection(selectionChangedEvent));
//      _equipViewer.addDoubleClickListener(doubleClickEvent -> onTagViewer_DoubleClick());
//
      tree.addListener(SWT.MouseDoubleClick, event -> onAction_EditEquipment());
      tree.addListener(SWT.MouseDown, event -> onEquipmentTree_MouseDown(event));
//
      tree.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> {

         _isSelectedWithKeyboard = true;
//
         enableActions();

         switch (keyEvent.keyCode) {

         case SWT.DEL:

            // delete equipment only when the delete button is enabled
            if (_actionDeleteEquipment.isEnabled()) {

               onAction_DeleteEquipment();

//            } else if (_actionDeleteTagCategory.isEnabled()) {
//
//               onAction_DeleteTagCategory();
            }

            break;

         case SWT.F2:
            onAction_EditEquipment();
            break;
         }
      }));

      /*
       * The context menu must be created AFTER the viewer is created which is also done after the
       * measurement system has changed, if not, the context menu is not displayed because it
       * belongs to the old viewer
       */
      createUI_20_ContextMenu();
//      createUI_30_ColumnImages(tree);
//
//      fillToolBar();
//
//      // set tour info tooltip provider
//      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_equipViewer);
//      _tourInfoToolTip.setTooltipUIProvider(new TaggingView_TooltipUIProvider(this));
   }

   /**
    * Setup the viewer context menu
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _equipmentViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   /**
    * Create the viewer context menu
    *
    * @return
    */
   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _equipmentViewer.getControl();

      // add the context menu to the tree viewer

      final Menu treeContextMenu = _viewerMenuManager.createContextMenu(tree);

//      treeContextMenu.addMenuListener(new MenuAdapter() {
//         @Override
//         public void menuHidden(final MenuEvent e) {
//            _tagMenuManager.onHideMenu();
//         }
//
//         @Override
//         public void menuShown(final MenuEvent menuEvent) {
//            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
//         }
//      });

      return treeContextMenu;
   }

   /**
    * Defines all columns for the table viewer in the column manager
    */
   private void defineAllColumns() {

      defineColumn_1stColumn();

      defineColumn_Brand();
      defineColumn_Model();

      defineColumn_Time_Date();
      defineColumn_Time_Date_Built();
      defineColumn_Time_Date_FirstUse();
      defineColumn_Time_Date_Retired();
   }

   /**
    * Column: Equipment & Category
    */
   private void defineColumn_1stColumn() {

      final TreeColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_AND_CATEGORY.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Object getData(final ViewerCell cell) {

//            if (_isToolTipInTag == false) {
//               return null;
//            }
//
//            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) cell.getElement();
//
//            if (viewItem instanceof TVIEquipmentView_Equipment || viewItem instanceof TVITaggingView_TagCategory) {
//
//               // return tag/category to show it's notes fields in the tooltip
//
//               return viewItem;
//            }

            return null;
         }

         @Override
         public Long getTourId(final ViewerCell cell) {

//            if (_isToolTipInTag == false) {
//               return null;
//            }
//
//            final Object element = cell.getElement();
//            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) element;
//
//            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {
//               return tourItem.tourId;
//            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVIEquipmentView_Item viewItem = (TVIEquipmentView_Item) element;

            final long numTours = viewItem.numTours;

            // hide number of tours
//            if (_tagFilterType == TagFilterType.TAGS_WITHOUT_TOURS) {
//               numTours = 0;
//            }

            final StyledString styledString = new StyledString();

            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {

               /*
                * Tour
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.TOUR_STYLER);

//               cell.setImage(TourTypeImage.getTourTypeImage(tourItem.tourTypeId));

               setCellColor(cell, element);

            } else if (viewItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

               /*
                * Equipment
                */

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.CONTENT_SUB_CATEGORY_STYLER);

               if (numTours > 0) {
                  styledString.append(UI.SPACE3 + numTours, net.tourbook.ui.UI.TOTAL_STYLER);
               }

               cell.setImage(_imgEquipment_All);

            } else {

               styledString.append(viewItem.firstColumn, net.tourbook.ui.UI.TOUR_STYLER);
            }

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
         }
      });
   }

   /**
    * Column: Brand
    */
   private void defineColumn_Brand() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_BRAND.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               cell.setText(tviEquipment.getEquipment().getBrand());
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Model
    */
   private void defineColumn_Model() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_MODEL.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               cell.setText(tviEquipment.getEquipment().getModel());
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Date
    */
   private void defineColumn_Time_Date() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               final LocalDate date = tviEquipment.getEquipment().getDate();

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Build date
    */
   private void defineColumn_Time_Date_Built() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_BUILT.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               final LocalDate date = tviEquipment.getEquipment().getDateBuilt();

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: First use date
    */
   private void defineColumn_Time_Date_FirstUse() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_FIRST_USE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               final LocalDate date = tviEquipment.getEquipment().getDateFirstUse();

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Retired date
    */
   private void defineColumn_Time_Date_Retired() {

      final ColumnDefinition colDef = TreeColumnFactory.EQUIPMENT_DATE_RETIRED.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {

               final LocalDate date = tviEquipment.getEquipment().getDateRetired();

               cell.setText(TimeTools.Formatter_Date_S.format(date));
               setCellColor(cell, element);
            }
         }
      });
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _imgEquipment.dispose();
      _imgEquipment_All.dispose();
      _imgEquipment_Part.dispose();
      _imgEquipment_Service.dispose();

      super.dispose();
   }

   private void enableActions() {

      final StructuredSelection selection = (StructuredSelection) _equipmentViewer.getSelection();
//      final int numTreeItems = _equipmentViewer.getTree().getItemCount();

      /*
       * Count number of selected tours/tags/categories
       */
      int numTours = 0;
      int numEquipments = 0;
//      int numItems = 0;
//      int numOtherItems = 0;

      TVIEquipmentView_Tour firstTour = null;

      for (final Object treeItem : selection) {

         if (treeItem instanceof final TVIEquipmentView_Tour tourItem) {

            if (numTours == 0) {
               firstTour = tourItem;
            }

            numTours++;

         } else if (treeItem instanceof TVIEquipmentView_Equipment) {

            numEquipments++;

         } else {

//            numOtherItems++;
         }

//         numItems++;
      }

//      final boolean isTourSelected = numTours > 0;
      final boolean isEquipmentSelected = numEquipments > 0;//&& numTours == 0 && numOtherItems == 0;
//      final boolean isOneTour = numTours == 1;
//      final boolean isItemsAvailable = numTreeItems > 0;

      _actionDeleteEquipment.setEnabled(isEquipmentSelected);
      _actionEditEquipment.setEnabled(isEquipmentSelected);
   }

   private void expandCollapseItem(final TreeViewerItem treeItem) {

      if (_equipmentViewer.getExpandedState(treeItem)) {

         _equipmentViewer.collapseToLevel(treeItem, 1);

      } else {

         _equipmentViewer.expandToLevel(treeItem, 1);
      }
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionNewEquipment);
      tbm.add(_actionExpandSelection);
      tbm.add(_actionCollapseAll_WithoutSelection);
      tbm.add(_action_RefreshView);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionCollapseOthers);
      menuMgr.add(_actionExpandSelection);
      menuMgr.add(_actionCollapseAll_WithoutSelection);
      menuMgr.add(_actionOnMouseSelect_ExpandCollapse);
      menuMgr.add(_actionSingleExpand_CollapseOthers);

      menuMgr.add(new Separator());

      menuMgr.add(_actionNewEquipment);
      menuMgr.add(_actionEditEquipment);
      menuMgr.add(_actionDeleteEquipment);

      enableActions();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   /**
    * Recursive !!!
    * <p>
    * Fetch children of a equipment item and collect tour id's.
    *
    * @param equipmentItem
    * @param allTourIds
    * @param checkedTourIds
    */
   private void getEquipmentChildren(final TVIEquipmentView_Item equipmentItem,
                                     final List<Long> allTourIds,
                                     final Set<Long> checkedTourIds) {

      // iterate over all equipment children

      for (final TreeViewerItem viewerItem : equipmentItem.getFetchedChildren()) {

         if (viewerItem instanceof final TVIEquipmentView_Tour tourItem) {

            final long tourId = tourItem.tourId;

            if (checkedTourIds.add(tourId)) {
               allTourIds.add(tourId);
            }

         } else if (viewerItem instanceof final TVIEquipmentView_Item viewItem) {

            getEquipmentChildren(viewItem, allTourIds, checkedTourIds);
         }
      }
   }

   private List<Long> getSelectedTourIDs() {

      final List<Long> allTourIds = new ArrayList<>();
      final Set<Long> checkedTourIds = new HashSet<>();

      final Object[] selection = ((IStructuredSelection) _equipmentViewer.getSelection()).toArray();

      for (final Object selectedItem : selection) {

         if (selectedItem instanceof final TVIEquipmentView_Tour tourItem) {

            final long tourId = tourItem.tourId;

            if (checkedTourIds.add(tourId)) {
               allTourIds.add(tourId);
            }

         } else if (selectedItem instanceof final TVIEquipmentView_Tour viewItem) {

            getEquipmentChildren(viewItem, allTourIds, checkedTourIds);
         }
      }

      return allTourIds;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tour id's
      final List<Long> tourIds = getSelectedTourIDs();

      /*
       * Show busyindicator when multiple tours needs to be retrieved from the database
       */
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      if (tourIds.size() > 1) {

         BusyIndicator.showWhile(Display.getCurrent(), () -> TourManager.getInstance().getTourData(selectedTourData, tourIds));

      } else {

         TourManager.getInstance().getTourData(selectedTourData, tourIds);
      }

      return selectedTourData;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _equipmentViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _equipmentViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

   }

   /**
    * Load all tree items that expandable items do show the number of items
    */
   private void loadAllTreeItems() {

      // get all equipment viewer items

      final List<TreeViewerItem> allRootItems = _rootItem.getFetchedChildren();

      for (final TreeViewerItem rootItem : allRootItems) {

         // is recursive !!!
         loadAllTreeItems(rootItem);
      }
   }

   /**
    * !!! RECURSIVE !!!
    * <p>
    * Traverses all tag viewer items
    *
    * @param parentItem
    */
   private void loadAllTreeItems(final TreeViewerItem parentItem) {

      final ArrayList<TreeViewerItem> allFetchedChildren = parentItem.getFetchedChildren();

      for (final TreeViewerItem childItem : allFetchedChildren) {

         // skip tour items, they do not have further children
         if (childItem instanceof TVIEquipmentView_Tour) {
            continue;
         }

         loadAllTreeItems(childItem);
      }

      /*
       * Collect number of ...
       */
      final int numAllTagCategories = 0;
      int numAllTags = 0;

      int numTags_NoTours = 0;

      int numTours_InTourItems = 0;
      final int numTours_InTagSubCats = 0;

      for (final TreeViewerItem childItem : allFetchedChildren) {

         if (childItem instanceof TVIEquipmentView_Tour) {

            numTours_InTourItems++;

//         } else if (childItem instanceof TVITaggingView_Year
//               || childItem instanceof TVITaggingView_Month) {
//
//            // collect number of tours in the tag sub categories
//
//            numTours_InTagSubCats += ((TVIEquipmentView_Item) childItem).numTours;
//
//         } else if (childItem instanceof TVITaggingView_TagCategory) {
//
//            numAllTagCategories++;

         } else if (childItem instanceof TVIEquipmentView_Equipment) {

            numAllTags++;

         }
      }

      if (numTours_InTourItems == 0 && numTours_InTagSubCats == 0) {

         numTags_NoTours++;
      }

// SET_FORMATTING_OFF

      /*
       * Update number of tours in parent item and up to the tag item
       */
      if (parentItem instanceof final TVIEquipmentView_Equipment tagItem) {

         tagItem.numTours           += numTours_InTourItems;
         tagItem.numTags_NoTours    += numTags_NoTours;

//      } else if (parentItem instanceof final TVITaggingView_Year yearItem) {
//
//         yearItem.numTours          += numTours_InTourItems;
//         yearItem.numTags_NoTours   += numTags_NoTours;
//
//         final TreeViewerItem yearParent = yearItem.getParentItem();
//         if (yearParent instanceof final TVIEquipmentView_Equipment tagItem) {
//
//            tagItem.numTours           += numTours_InTourItems;
//            tagItem.numTags_NoTours    += numTags_NoTours;
//         }
//
//      } else if (parentItem instanceof final TVITaggingView_Month monthItem) {
//
//         monthItem.numTours            += numTours_InTourItems;
//         monthItem.numTags_NoTours     += numTags_NoTours;
//
//         final TreeViewerItem monthParent = monthItem.getParentItem();
//         if (monthParent instanceof final TVITaggingView_Year yearItem) {
//
//            yearItem.numTours          += numTours_InTourItems;
//            yearItem.numTags_NoTours   += numTags_NoTours;
//
//            final TreeViewerItem yearParent = yearItem.getParentItem();
//            if (yearParent instanceof final TVIEquipmentView_Equipment tagItem) {
//
//               tagItem.numTours           += numTours_InTourItems;
//               tagItem.numTags_NoTours    += numTags_NoTours;
//            }
//         }
//
//      } else if (parentItem instanceof final TVITaggingView_TagCategory categoryItem) {
//
//         long allNumChild_Tours           = 0;
//         long allNumChild_TagsNoTours     = 0;
//
//         for (final TreeViewerItem treeViewerItem : allFetchedChildren) {
//
//            if (treeViewerItem instanceof final TVIEquipmentView_Item viewItem) {
//
//               allNumChild_Tours          += viewItem.numTours;
//               allNumChild_TagsNoTours    += viewItem.numTags_NoTours;
//            }
//         }
//
//         categoryItem.numTagCategories    += numAllTagCategories;
//         categoryItem.numTags             += numAllTags;
//
//         categoryItem.numTours            += allNumChild_Tours;
//         categoryItem.numTags_NoTours     += allNumChild_TagsNoTours;
      }

// SET_FORMATTING_ON
   }

   /**
    * !!! RECURSIVE !!!
    * <p>
    * Traverses all equipment viewer items
    *
    * @param parentItem
    */
   private void loadAllTreeItems2(final TreeViewerItem parentItem) {

      final List<TreeViewerItem> allFetchedChildren = parentItem.getFetchedChildren();

      for (final TreeViewerItem childItem : allFetchedChildren) {

         loadAllTreeItems2(childItem);
      }
   }

   private void onAction_DeleteEquipment() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      final Map<Long, Equipment> allAvailableEquipment = EquipmentManager.getAllEquipment_ByID();

      final List<Equipment> allSelectedEquipment = new ArrayList<>();

      for (final Object object : allSelection) {

         if (object instanceof final TVIEquipmentView_Equipment tviEquipment) {

            allSelectedEquipment.add(allAvailableEquipment.get(tviEquipment.getEquipment().getEquipmentId()));
         }
      }

      if (allSelectedEquipment.size() > 0) {

         // delete equipments

         EquipmentManager.equipment_Delete(allSelectedEquipment);
      }
   }

   private void onAction_EditEquipment() {

      final ITreeSelection structuredSelection = _equipmentViewer.getStructuredSelection();
      final Object firstElement = structuredSelection.getFirstElement();

      if (firstElement instanceof final TVIEquipmentView_Equipment equipmentItem) {

         final Equipment equipment = equipmentItem.getEquipment();

         final DialogEquipment dialogEquipment = new DialogEquipment(_parent.getShell(), equipment);

         if (dialogEquipment.open() != Window.OK) {
            return;
         }

         final Equipment equipmentInDialog = dialogEquipment.getEquipment();

         // update model
         equipment.updateFromOther(equipmentInDialog);

         TourDatabase.saveEntity(equipment, equipment.getEquipmentId(), Equipment.class);

         // update UI
         reloadViewer();

         updateOtherViews();
      }
   }

   private void onAction_NewEquipment() {

      final DialogEquipment dialogEquipment = new DialogEquipment(_parent.getShell(), null);

      if (dialogEquipment.open() != Window.OK) {
         return;
      }

      final Equipment equipment = dialogEquipment.getEquipment();

      // update model
      TourDatabase.saveEntity(equipment, equipment.getEquipmentId(), Equipment.class);

      // update UI
      reloadViewer();

      updateOtherViews();
   }

   private void onAction_OnMouseSelect_ExpandCollapse() {

      _isBehaviour_OnSelect_ExpandCollapse = _actionOnMouseSelect_ExpandCollapse.isChecked();
   }

   private void onAction_SingleExpandCollapseOthers() {

      _isBehaviour_SingleExpand_CollapseOthers = _actionSingleExpand_CollapseOthers.isChecked();
   }

   private void onEquipmentTree_MouseDown(final Event event) {

      _isMouseContextMenu = event.button == 3;
   }

   private void onEquipmentViewer_Selection(final SelectionChangedEvent event) {

      if (_isMouseContextMenu) {
         return;
      }

      final IStructuredSelection selectedTours = (IStructuredSelection) (event.getSelection());
      final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

      if (selectedItem instanceof final TVIEquipmentView_Tour tourItem && selectedTours.size() == 1) {

         // one tour is selected

         _postSelectionProvider.setSelection(new SelectionTourId(tourItem.tourId));

      } else if (false

            || selectedItem instanceof TVIEquipmentView_Equipment

//            || selectedItem instanceof TVIEquipmentView_Equipment
//            || selectedItem instanceof TVITaggingView_TagCategory
//            || selectedItem instanceof TVITaggingView_Year
//            || selectedItem instanceof TVITaggingView_Month

      ) {

         // category is selected, expand/collapse category items

         if (_isSelectedWithKeyboard == false) {

            // do not expand/collapse when keyboard is used -> unusable

            onSelect_CategoryItem((TreeSelection) event.getSelection());
         }

      } else {

         // multiple tours are selected

         final ArrayList<Long> tourIds = new ArrayList<>();

         for (final Object viewItem : selectedTours) {

            if (viewItem instanceof final TVIEquipmentView_Tour tourItem) {
               tourIds.add(tourItem.tourId);
            }
         }

         if (tourIds.size() > 0) {
            _postSelectionProvider.setSelection(new SelectionTourIds(tourIds));
         }
      }

      // reset state
      _isSelectedWithKeyboard = false;

      enableActions();
   }

   private void onSelect_CategoryItem(final TreeSelection treeSelection) {

      if (_isInExpandingSelection) {

         // prevent endless loops
         return;
      }

      final TreePath[] selectedTreePaths = treeSelection.getPaths();
      if (selectedTreePaths.length == 0) {
         return;
      }

      final TreePath selectedTreePath = selectedTreePaths[0];
      if (selectedTreePath == null) {
         return;
      }

      onSelect_CategoryItem_10_AutoExpandCollapse(treeSelection);
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded children are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    */
   private void onSelect_CategoryItem_10_AutoExpandCollapse(final ITreeSelection treeSelection) {

      if (_isInCollapseAll) {

         // prevent auto expand
         return;
      }

      if (_isBehaviour_SingleExpand_CollapseOthers) {

         /*
          * run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         Display.getCurrent().asyncExec(new Runnable() {

            private long           __expandRunnableCounter = ++_expandRunnableCounter;

            private ITreeSelection __treeSelection         = treeSelection;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (_equipmentViewer.getTree().isDisposed()) {
                  return;
               }

               /*
                * With Linux the selection event is fired twice when a subcategory, e.g. month is
                * selected which causes an endless loop !!!
                */
               final long now = System.currentTimeMillis();
               final long timeDiff = now - _lastExpandSelectionTime;
               if (timeDiff < 200) {
                  return;
               }

               onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(__treeSelection);
            }
         });

      } else {

         if (_isBehaviour_OnSelect_ExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard

            final TreePath selectedTreePath = treeSelection.getPaths()[0];

            expandCollapseItem((TreeViewerItem) selectedTreePath.getLastSegment());
         }
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param treeSelection
    */
   private void onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(final ITreeSelection treeSelection) {

      /*
       * Create expanded elements from the tree selection
       */
      final TreePath selectedTreePath = treeSelection.getPaths()[0];
      final int numSegments = selectedTreePath.getSegmentCount();

      final Object[] expandedElements = new Object[numSegments];

      for (int segmentIndex = 0; segmentIndex < numSegments; segmentIndex++) {
         expandedElements[segmentIndex] = selectedTreePath.getSegment(segmentIndex);
      }

      _isInExpandingSelection = true;
      {
         final Tree tree = _equipmentViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _equipmentViewer.getExpandedState(selectedTreePath);

            /*
             * collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _equipmentViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _equipmentViewer.setExpandedState(treePath, false);
            }

            /*
             * expand and select selected folder
             */
            _equipmentViewer.setExpandedElements(expandedElements);
            _equipmentViewer.setSelection(treeSelection, true);

            if (_isBehaviour_OnSelect_ExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _equipmentViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * Set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * Win behaviour: When an item is set to top which was collapsed before, it will be
             * expanded
             */
            if (topItem.isDisposed() == false) {
               tree.setTopItem(topItem);
            }
         }
         tree.setRedraw(true);
      }
      _isInExpandingSelection = false;
      _lastExpandSelectionTime = System.currentTimeMillis();
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _equipmentViewer.getExpandedElements();
         final ISelection selection = _equipmentViewer.getSelection();

         _equipmentViewer.getTree().dispose();

         createUI_10_EqipmentViewer(_viewerContainer);
         _viewerContainer.layout();

         reloadViewer_SetContent();

         _equipmentViewer.setExpandedElements(expandedElements);
         _equipmentViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _equipmentViewer;
   }

   @Override
   public void reloadViewer() {

      final Tree tree = _equipmentViewer.getTree();

      tree.setRedraw(false);
      {
         final Object[] expandedElements = _equipmentViewer.getExpandedElements();
         final ITreeSelection selection = _equipmentViewer.getStructuredSelection();

         reloadViewer_SetContent();

         _equipmentViewer.setExpandedElements(expandedElements);
         _equipmentViewer.setSelection(selection);
      }
      tree.setRedraw(true);
   }

   private void reloadViewer_SetContent() {

      _rootItem = new TVIEquipmentView_EquipRoot(_equipmentViewer);

      // first: load all tree items
      loadAllTreeItems();

      // second: update viewer
      _equipmentViewer.setInput(_rootItem);
   }

   private void restoreState() {

      // on mouse select -> expand/collapse
      _isBehaviour_OnSelect_ExpandCollapse = Util.getStateBoolean(_state, STATE_IS_ON_SELECT_EXPAND_COLLAPSE, true);
      _actionOnMouseSelect_ExpandCollapse.setChecked(_isBehaviour_OnSelect_ExpandCollapse);

      // single expand -> collapse others
      _isBehaviour_SingleExpand_CollapseOthers = Util.getStateBoolean(_state, STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, true);
      _actionSingleExpand_CollapseOthers.setChecked(_isBehaviour_SingleExpand_CollapseOthers);
   }

   /**
    * Restore viewer state after the viewer is loaded.
    */
   private void restoreState_Viewer() {

      /*
       * Expanded equipment categories
       */
      final long[] allStateItems = Util.getStateLongArray(_state, STATE_EXPANDED_ITEMS, null);
      if (allStateItems != null) {

         final List<TreePath> viewerTreePaths = new ArrayList<>();

         final List<StateSegment[]> allStateSegments = restoreState_Viewer_GetSegments(allStateItems);
         for (final StateSegment[] stateSegments : allStateSegments) {

            final List<Object> pathSegments = new ArrayList<>();

            // start tree items with the root and go deeper with every segment
            List<TreeViewerItem> treeItems = _rootItem.getFetchedChildren();

            for (final StateSegment stateSegment : stateSegments) {

               /*
                * This is somehow recursive as it goes deeper into the child tree items until there
                * are no children
                */
               treeItems = restoreState_Viewer_ExpandItem(pathSegments, treeItems, stateSegment);
            }

            if (pathSegments.size() > 0) {
               viewerTreePaths.add(new TreePath(pathSegments.toArray()));
            }
         }

         if (viewerTreePaths.size() > 0) {
            _equipmentViewer.setExpandedTreePaths(viewerTreePaths.toArray(new TreePath[viewerTreePaths.size()]));
         }
      }
   }

   /**
    * @param pathSegments
    * @param treeItems
    * @param stateSegment
    *
    * @return Returns children when it could be expanded otherwise <code>null</code>.
    */
   private List<TreeViewerItem> restoreState_Viewer_ExpandItem(final List<Object> pathSegments,
                                                               final List<TreeViewerItem> treeItems,
                                                               final StateSegment stateSegment) {

      if (treeItems == null) {
         return null;
      }

      final long stateData = stateSegment.__itemData;

//      if (stateSegment.__itemType == STATE_ITEM_TYPE_CATEGORY) {
//
//         for (final TreeViewerItem treeItem : treeItems) {
//
//            if (treeItem instanceof final TVITaggingView_TagCategory categoryItem) {
//
//               final long viewerCatId = categoryItem.getTourTagCategory().getCategoryId();
//
//               if (viewerCatId == stateData) {
//
//                  pathSegments.add(treeItem);
//
//                  return categoryItem.getFetchedChildren();
//               }
//            }
//         }
//
//      } else

      if (stateSegment.__itemType == STATE_ITEM_TYPE_EQUIPMENT) {

         for (final TreeViewerItem treeItem : treeItems) {

            if (treeItem instanceof final TVIEquipmentView_Equipment equipmentItem) {

               final long viewerEquipmentId = equipmentItem.getEquipment().getEquipmentId();

               if (viewerEquipmentId == stateData) {

                  pathSegments.add(treeItem);

                  return equipmentItem.getFetchedChildren();
               }
            }
         }

//      } else if (stateSegment.__itemType == STATE_ITEM_TYPE_YEAR) {
//
//         for (final TreeViewerItem treeItem : treeItems) {
//
//            if (treeItem instanceof final TVITaggingView_Year yearItem) {
//
//               final long viewerYear = yearItem.getYear();
//
//               if (viewerYear == stateData) {
//
//                  pathSegments.add(treeItem);
//
//                  return yearItem.getFetchedChildren();
//               }
//            }
//         }
//
//      } else if (stateSegment.__itemType == STATE_ITEM_TYPE_MONTH) {
//
//         for (final TreeViewerItem treeItem : treeItems) {
//
//            if (treeItem instanceof final TVITaggingView_Month monthItem) {
//
//               final long viewerYear = monthItem.getMonth();
//
//               if (viewerYear == stateData) {
//
//                  pathSegments.add(treeItem);
//
//                  return monthItem.getFetchedChildren();
//               }
//            }
//         }
      }

      return null;
   }

   /**
    * Convert state structure into a 'segment' structure.
    */
   private List<StateSegment[]> restoreState_Viewer_GetSegments(final long[] expandedItems) {

      final List<StateSegment[]> allTreePathSegments = new ArrayList<>();
      final List<StateSegment> currentSegments = new ArrayList<>();

      for (int itemIndex = 0; itemIndex < expandedItems.length;) {

         // ensure array bounds
         if (itemIndex + 1 >= expandedItems.length) {
            // this should not happen when data are not corrupted
            break;
         }

         final long itemType = expandedItems[itemIndex++];
         final long itemData = expandedItems[itemIndex++];

         if (itemType == STATE_ITEM_TYPE_SEPARATOR) {

            // a new tree path starts

            if (currentSegments.size() > 0) {

               // keep current tree path segments

               allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));

               // start a new path
               currentSegments.clear();
            }

         } else {

            // a new segment is available

            if (false
//                  || itemType == STATE_ITEM_TYPE_CATEGORY
                  || itemType == STATE_ITEM_TYPE_EQUIPMENT
//                  || itemType == STATE_ITEM_TYPE_YEAR
//                  || itemType == STATE_ITEM_TYPE_MONTH
            ) {

               currentSegments.add(new StateSegment(itemType, itemData));
            }
         }
      }

      if (currentSegments.size() > 0) {
         allTreePathSegments.add(currentSegments.toArray(new StateSegment[currentSegments.size()]));
      }

      return allTreePathSegments;
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpand_CollapseOthers.isChecked());
      _state.put(STATE_IS_ON_SELECT_EXPAND_COLLAPSE, _actionOnMouseSelect_ExpandCollapse.isChecked());

      saveState_ExpandedItems();
   }

   /**
    * Save state for expanded tree items.
    */
   private void saveState_ExpandedItems() {

      final Object[] visibleExpanded = _equipmentViewer.getVisibleExpandedElements();

      if (visibleExpanded.length == 0) {
         Util.setState(_state, STATE_EXPANDED_ITEMS, new long[0]);
         return;
      }

      final LongArrayList expandedItems = new LongArrayList();

      final TreePath[] expandedOpenedTreePaths = net.tourbook.common.UI.getExpandedOpenedItems(
            visibleExpanded,
            _equipmentViewer.getExpandedTreePaths());

      for (final TreePath expandedPath : expandedOpenedTreePaths) {

         // start a new path, always set it twice to have a even structure
         expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);
         expandedItems.add(STATE_ITEM_TYPE_SEPARATOR);

         for (int segmentIndex = 0; segmentIndex < expandedPath.getSegmentCount(); segmentIndex++) {

            final Object segment = expandedPath.getSegment(segmentIndex);

//            if (segment instanceof final TVITaggingView_TagCategory categoryItem) {
//
//               expandedItems.add(STATE_ITEM_TYPE_CATEGORY);
//               expandedItems.add(categoryItem.getTourTagCategory().getCategoryId());
//
//            } else
            if (segment instanceof final TVIEquipmentView_Equipment equipmentItem) {

               expandedItems.add(STATE_ITEM_TYPE_EQUIPMENT);
               expandedItems.add((equipmentItem).getEquipment().getEquipmentId());

//            } else if (segment instanceof final TVITaggingView_Year yeatItem) {
//
//               expandedItems.add(STATE_ITEM_TYPE_YEAR);
//               expandedItems.add(yeatItem.getYear());
//
//            } else if (segment instanceof final TVITaggingView_Month monthItem) {
//
//               expandedItems.add(STATE_ITEM_TYPE_MONTH);
//               expandedItems.add(monthItem.getMonth());
            }
         }
      }

      Util.setState(_state, STATE_EXPANDED_ITEMS, expandedItems.toArray());
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

      // set color

      if (element instanceof TVIEquipmentView_Equipment) {

         cell.setForeground(_colorContentSubCategory);

//      } else if (element instanceof TVITaggingView_Year) {
//
//         cell.setForeground(_colorDateCategory);
//
//      } else if (element instanceof TVITaggingView_Month) {
//
//         cell.setForeground(_colorDateSubCategory);

      } else if (element instanceof TVIEquipmentView_Tour) {

         cell.setForeground(_colorTour);
      }
   }

   @Override
   public void setFocus() {

      _equipmentViewer.getTree().setFocus();
   }

   private void updateColors() {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

// SET_FORMATTING_OFF

      _colorContentCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_CATEGORY);
      _colorContentSubCategory   = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_SUB_CATEGORY);
      _colorDateCategory         = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_CATEGORY);
      _colorDateSubCategory      = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_DATE_SUB_CATEGORY);
      _colorTour                 = colorRegistry.get(net.tourbook.ui.UI.VIEW_COLOR_TOUR);

// SET_FORMATTING_ON
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   private void updateOtherViews() {

      // remove old equipment from cached tours
      EquipmentManager.clearCachedValues();

//    TagMenuManager.updateRecentTagNames();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
//    TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
   }
}
