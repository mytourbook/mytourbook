/*******************************************************************************
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
import java.util.List;
import java.util.Map;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.views.tagging.TVITaggingView_TagCategory;
import net.tourbook.ui.views.tagging.TVITaggingView_Tour;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

public class EquipmentView extends ViewPart implements ITourProvider, ITourViewer, ITreeViewer {

   public static final String            ID                           = "net.tourbook.equipment.EquipmentView.ID"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                   = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common            = CommonActivator.getPrefStore();
   private static final IDialogSettings  _state                       = TourbookPlugin.getState(ID);

   private static final int              TAG_VIEW_LAYOUT_FLAT         = 0;
   private static final int              TAG_VIEW_LAYOUT_HIERARCHICAL = 10;

   private IPropertyChangeListener       _prefChangeListener;
   private IPropertyChangeListener       _prefChangeListener_Common;

   private int                           _tagViewLayout               = TAG_VIEW_LAYOUT_HIERARCHICAL;

   private ActionDeleteEquipment         _actionDeleteEquipment;
   private ActionEditEquipment           _actionEditEquipment;
   private ActionNewEquipment            _actionNewEquipment;

   private TreeViewer                    _equipViewer;
   private ColumnManager                 _columnManager;
   private TVIEquipRoot                  _rootItem;

   private PixelConverter                _pc;

   private MenuManager                   _viewerMenuManager;
   private Menu                          _treeContextMenu;
   private IContextMenuProvider          _viewerContextMenuProvider   = new TreeContextMenuProvider();

   private boolean                       _isSelectedWithKeyboard;

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;

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

//         if (obj1 instanceof final TVITaggingView_Year yearItem1 && obj2 instanceof final TVITaggingView_Year yearItem2) {
//
//            return yearItem1.compareTo(yearItem2);
//         }
//
//         if (obj1 instanceof final TVITaggingView_Month monthItem1 && obj2 instanceof final TVITaggingView_Month monthItem2) {
//
//            return monthItem1.compareTo(monthItem2);
//         }
//
//         if (obj1 instanceof final TVITaggingView_TagCategory iItem1 && obj2 instanceof final TVITaggingView_TagCategory item2) {
//
//            return iItem1.getTourTagCategory().getCategoryName().compareTo(item2.getTourTagCategory().getCategoryName());
//         }

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

//         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {
//
//            _equipViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//
//            _equipViewer.refresh();
//
//            /*
//             * the tree must be redrawn because the styled text does not show with the new color
//             */
//            _equipViewer.getTree().redraw();
//
//         } else if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
//
//            // reselect current sensor that the sensor chart (when opened) is reloaded
//
//            final StructuredSelection selection = getViewerSelection();
//
//            _equipViewer.setSelection(selection, true);
//
//            final Table table = _equipViewer.getTable();
//            table.showSelection();
//         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

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

   private void createActions() {

   // SET_FORMATTING_OFF

      _actionEditEquipment       = new ActionEditEquipment();
      _actionNewEquipment        = new ActionNewEquipment();
      _actionDeleteEquipment     = new ActionDeleteEquipment();

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

      addPrefListener();

      reloadViewer();

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
      _equipViewer = new TreeViewer(tree);
      _columnManager.createColumns(_equipViewer);

      _equipViewer.setContentProvider(new EquipmentContentProvider());
      _equipViewer.setComparator(new EquipmentComparator());
      _equipViewer.setComparer(new EquipmentComparer());
//      _equipViewer.setFilters(new TagFilter());

      _equipViewer.setUseHashlookup(true);

//      _equipViewer.addSelectionChangedListener(selectionChangedEvent -> onTagViewer_Selection(selectionChangedEvent));
//      _equipViewer.addDoubleClickListener(doubleClickEvent -> onTagViewer_DoubleClick());
//
      tree.addListener(SWT.MouseDoubleClick, event -> onAction_EditEquipment());
//      tree.addListener(SWT.MouseDown, event -> onTagTree_MouseDown(event));
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

      final Tree tree = (Tree) _equipViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   /**
    * Create the viewer context menu
    *
    * @return
    */
   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _equipViewer.getControl();

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

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof final TVIEquipmentView_Equipment tviEquipment) {
               cell.setText(tviEquipment.getEquipment().getName());
            }
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
            }
         }
      });
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

//      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
//
//      getSite().getPage().removePostSelectionListener(_postSelectionListener);

      super.dispose();
   }

   private void enableActions() {

      final StructuredSelection selection = (StructuredSelection) _equipViewer.getSelection();
      final int numTreeItems = _equipViewer.getTree().getItemCount();

      /*
       * Count number of selected tours/tags/categories
       */
      int numTours = 0;
      int numEquipments = 0;
      int numCategorys = 0;
      int numItems = 0;
      int numOtherItems = 0;

      TVITaggingView_Tour firstTour = null;

      for (final Object treeItem : selection) {

         if (treeItem instanceof final TVITaggingView_Tour tourItem) {

            if (numTours == 0) {
               firstTour = tourItem;
            }

            numTours++;

         } else if (treeItem instanceof TVIEquipmentView_Equipment) {

            numEquipments++;

         } else if (treeItem instanceof TVITaggingView_TagCategory) {

            numCategorys++;

         } else {

            numOtherItems++;
         }

         numItems++;
      }

      final boolean isTourSelected = numTours > 0;
      final boolean isEquipmentSelected = numEquipments > 0 && numTours == 0 && numCategorys == 0 && numOtherItems == 0;
      final boolean isCategorySelected = numCategorys == 1 && numTours == 0 && numEquipments == 0 && numOtherItems == 0;
      final boolean isOneTour = numTours == 1;
      final boolean isItemsAvailable = numTreeItems > 0;

      _actionDeleteEquipment.setEnabled(isEquipmentSelected);
      _actionEditEquipment.setEnabled(isEquipmentSelected);

   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionNewEquipment);

      // update that actions are fully created otherwise action enable will fail
      tbm.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionNewEquipment);
      menuMgr.add(_actionEditEquipment);
      menuMgr.add(_actionDeleteEquipment);

      enableActions();
   }

   @Override
   public ColumnManager getColumnManager() {
      return null;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {
      return null;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return null;
   }

   @Override
   public ColumnViewer getViewer() {
      return null;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

   }

   /**
    * Load all tree items that category items do show the number of items
    */
   private void loadAllTreeItems() {

      // get all tag viewer items

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

         loadAllTreeItems(childItem);
      }

      // SET_FORMATTING_ON
   }

   private void onAction_DeleteEquipment() {

      final ITreeSelection structuredSelection = _equipViewer.getStructuredSelection();
      final List<?> allSelection = structuredSelection.toList();

      final Map<Long, Equipment> allEquipments = EquipmentManager.getAllEquipment_ByID();

      final List<Equipment> allSelectedEquipments = new ArrayList<>();

      for (final Object object : allSelection) {

         if (object instanceof final TVIEquipmentView_Equipment tviEquipment) {

            allSelectedEquipments.add(allEquipments.get(tviEquipment.getEquipment().getEquipmentId()));
         }
      }

      if (allSelectedEquipments.size() > 0) {

         // delete equipments

         EquipmentManager.equipment_Delete(allSelectedEquipments);
      }
   }

   private void onAction_EditEquipment() {

      final ITreeSelection structuredSelection = _equipViewer.getStructuredSelection();
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

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _equipViewer.getExpandedElements();
         final ISelection selection = _equipViewer.getSelection();

         _equipViewer.getTree().dispose();

         createUI_10_EqipmentViewer(_viewerContainer);
         _viewerContainer.layout();

         reloadViewer_SetContent();

         _equipViewer.setExpandedElements(expandedElements);
         _equipViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _equipViewer;
   }

   @Override
   public void reloadViewer() {

      final Tree tree = _equipViewer.getTree();

      tree.setRedraw(false);
      {
         final Object[] expandedElements = _equipViewer.getExpandedElements();
         final ISelection selection = _equipViewer.getSelection();

         reloadViewer_SetContent();

         _equipViewer.setExpandedElements(expandedElements);
         _equipViewer.setSelection(selection);
      }
      tree.setRedraw(true);
   }

   private void reloadViewer_SetContent() {

      final boolean isTreeLayoutHierarchical = _tagViewLayout == TAG_VIEW_LAYOUT_HIERARCHICAL;

      _rootItem = new TVIEquipRoot(_equipViewer, isTreeLayoutHierarchical);

      // first: load all tree items
      loadAllTreeItems();

      // second: update viewer
      _equipViewer.setInput(_rootItem);
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

   }

   @Override
   public void setFocus() {

      _equipViewer.getTree().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {

   }

   private void updateOtherViews() {

      // remove old tags from cached tours
      EquipmentManager.clearCachedValues();

//    TagMenuManager.updateRecentTagNames();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
//    TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
   }

}
