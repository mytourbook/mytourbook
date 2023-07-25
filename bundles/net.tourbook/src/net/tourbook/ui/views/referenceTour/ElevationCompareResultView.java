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
package net.tourbook.ui.views.referenceTour;

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.formatter.ValueFormatSet;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.ui.SelectionCellLabelProvider;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ColumnProfile;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.IReferenceTourProvider;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTip_CustomSelection_CellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class ElevationCompareResultView extends ViewPart implements

      ITourViewer,
      ITourProvider,
      ITreeViewer,
      IReferenceTourProvider {

   public static final String            ID                                = "net.tourbook.views.tourCatalog.CompareResultView"; //$NON-NLS-1$

   private static String                 STATE_IS_USE_FAST_APP_TOUR_FILTER = "STATE_IS_USE_FAST_APP_TOUR_FILTER";                //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common                 = CommonActivator.getPrefStore();
   private static final IDialogSettings  _state                            = TourbookPlugin.getState(ID);

   private static final NumberFormat     _nf0                              = NumberFormat.getNumberInstance();

   static {

      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
   }

   private TVIElevationCompareResult_RootItem  _rootItem;

   private PostSelectionProvider               _postSelectionProvider;

   private ISelectionListener                  _postSelectionListener;
   private IPartListener2                      _partListener;
   private IPropertyChangeListener             _prefChangeListener;
   private IPropertyChangeListener             _prefChangeListener_Common;
   private ITourEventListener                  _tourPropertyListener;
   private ITourEventListener                  _compareTourPropertyListener;

   private boolean                             _isToolbarCreated;
   private boolean                             _isToolTipInTour;

   private CompareFilter                       _compareFilter             = CompareFilter.ALL_IS_DISPLAYED;

   private CheckboxTreeViewer                  _tourViewer;
   private ColumnManager                       _columnManager;
   private TreeColumnDefinition                _colDef_TourTypeImage;

   /**
    * Index of the column with the image, index can be changed when the columns are reordered with
    * the mouse or the column manager
    */
   private int                                 _columnIndex_TourTypeImage = -1;
   private int                                 _columnWidth_TourTypeImage;

   private SelectionRemovedComparedTours       _oldRemoveSelection        = null;

   private TagMenuManager                      _tagMenuManager;
   private MenuManager                         _viewerMenuManager;
   private IContextMenuProvider                _viewerContextMenuProvider = new TreeContextMenuProvider();

   private ActionAppTourFilter                 _actionAppTourFilter;
   private ActionCollapseAll                   _actionCollapseAll;
   private ActionElevationCompareFilter        _actionElevationCompareFilter;
   private ActionReRunComparision              _actionReRunComparision;

   private ActionCheckTours                    _actionContext_CheckTours;
   private ActionCompareByElevation_AllTours   _actionContext_Compare_AllTours;
   private ActionCompareByElevation_WithWizard _actionContext_Compare_WithWizard;
   private ActionEditQuick                     _actionContext_EditQuick;
   private ActionEditTour                      _actionContext_EditTour;
   private ActionOpenTour                      _actionContext_OpenTour;
   private ActionRemoveComparedTourSaveStatus  _actionContext_RemoveComparedTourSaveStatus;
   private ActionSetTourTypeMenu               _actionContext_SetTourType;
   private ActionSaveComparedTours             _actionContext_SaveComparedTours;
   private ActionUncheckTours                  _actionContext_UncheckTours;

   private TreeViewerTourInfoToolTip           _tourInfoToolTip;

   private PixelConverter                      _pc;

// SET_FORMATTING_OFF

   private Color    _titleColor = JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_SUB_CATEGORY);

// SET_FORMATTING_ON

   /*
    * UI resources
    */
   private Image _dbImage = TourbookPlugin.getImageDescriptor(Images.Saved_Tour).createImage(true);

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Menu      _treeContextMenu;

   private class ActionAppTourFilter extends Action {

      public ActionAppTourFilter() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Elevation_Compare_Action_AppTourFilter_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
      }
   }

   private class ActionElevationCompareFilter extends Action {

      public ActionElevationCompareFilter() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Elevation_Compare_Action_TourCompareFilter_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourElevationCompareFilter));
      }

      @Override
      public void run() {
         action_ElevationCompareFilter();
      }
   }

   private class ActionReRunComparision extends Action {

      public ActionReRunComparision() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.Elevation_Compare_Action_ReRunComparison_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Refresh));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Refresh_Disabled));
      }

      @Override
      public void run() {
         action_ReRunComparision();
      }
   }

   private enum CompareFilter {

      ALL_IS_DISPLAYED,

      /**
       * Only saved tours are displayed
       */
      SAVED,

      /**
       * Only not saved tours are displayed
       */
      NOT_SAVED
   }

   private class ResultContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

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

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public class ResultTreeViewer extends ContainerCheckedTreeViewer {

      public ResultTreeViewer(final Tree tree) {
         super(tree);
      }

      @Override
      protected Object[] getSortedChildren(final Object parentElementOrTreePath) {

         final Object[] sortedChildren = super.getSortedChildren(parentElementOrTreePath);

         /*
          * Keep sorted and filtered children which are used to navigate them programmatically
          */
         if (parentElementOrTreePath instanceof TVIElevationCompareResult_ReferenceTour) {

            final TVIElevationCompareResult_ReferenceTour comparedRefTour = (TVIElevationCompareResult_ReferenceTour) parentElementOrTreePath;

            comparedRefTour.sortedAndFilteredCompareResults = sortedChildren;
         }

         return sortedChildren;
      }

   }

   public class TourCompareFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_compareFilter == CompareFilter.ALL_IS_DISPLAYED) {

            // nothing is filtered
            return true;
         }

         // compare results are filtered

         if (element instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour compareResult = (TVIElevationCompareResult_ComparedTour) element;

            final boolean isResultSaved = compareResult.isSaved();

            if (_compareFilter == CompareFilter.SAVED && isResultSaved) {

               // saved results are displayed

               return true;

            } else if (_compareFilter == CompareFilter.NOT_SAVED && isResultSaved == false) {

               // not saved results are displayed

               return true;

            } else {

               return false;
            }
         }

         // all other items are not filtered, e.g. ref tour item
         return true;
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

   public ElevationCompareResultView() {}

   private void action_ElevationCompareFilter() {

      // toggle compare Filter

      if (_compareFilter == CompareFilter.ALL_IS_DISPLAYED) {

         _compareFilter = CompareFilter.SAVED;
         _actionElevationCompareFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourElevationCompareFilter_Saved));

      } else if (_compareFilter == CompareFilter.SAVED) {

         _compareFilter = CompareFilter.NOT_SAVED;
         _actionElevationCompareFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourElevationCompareFilter_NotSaved));

      } else {

         _compareFilter = CompareFilter.ALL_IS_DISPLAYED;
         _actionElevationCompareFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourElevationCompareFilter));
      }

      // prevent too many refreshes, this is visible when the scrollthumb is moved
      final Tree tree = _tourViewer.getTree();
      tree.setRedraw(false);
      {
         _tourViewer.refresh();
      }
      tree.setRedraw(true);
   }

   private void action_ReRunComparision() {

      final ArrayList<RefTourItem> selectedRefTourItems = ElevationCompareManager.getComparedReferenceTours();

      final ArrayList<Long> allTourIds = isUseFastAppFilter()

            ? TourDatabase.getAllTourIds_WithFastAppFilter()
            : TourDatabase.getAllTourIds();

      final Long[] allTourIdsAsArray = allTourIds.toArray(new Long[allTourIds.size()]);

      ElevationCompareManager.compareTours(selectedRefTourItems, allTourIdsAsArray);
   }

   private void addCompareTourPropertyListener() {

      _compareTourPropertyListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId propertyId, final Object propertyData) {

            if (propertyId == TourEventId.COMPARE_TOUR_CHANGED
                  && propertyData instanceof TourPropertyCompareTourChanged) {

               final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) propertyData;

               final long compareId = compareTourProperty.compareId;

               final ArrayList<ElevationCompareResult> compareIds = new ArrayList<>();

               compareIds.add(new ElevationCompareResult(

                     compareId,
                     compareTourProperty.tourId,
                     compareTourProperty.refTourId));

               if (compareId == -1) {

                  // compare result is not saved

                  final Object comparedTourItem = compareTourProperty.comparedTourItem;

                  if (comparedTourItem instanceof TVIElevationCompareResult_ComparedTour) {

                     final TVIElevationCompareResult_ComparedTour resultItem = (TVIElevationCompareResult_ComparedTour) comparedTourItem;

                     resultItem.movedSpeed = compareTourProperty.speed;

                     // update viewer
                     _tourViewer.update(comparedTourItem, null);
                  }

               } else {

                  // compare result is saved

                  // find compared tour in the viewer
                  final ArrayList<TVIElevationCompareResult_ComparedTour> comparedTours = new ArrayList<>();
                  getComparedTours(comparedTours, _rootItem, compareIds);

                  if (comparedTours.size() > 0) {

                     final TVIElevationCompareResult_ComparedTour compareTourItem = comparedTours.get(0);

                     if (compareTourProperty.isDataSaved) {

                        // compared tour was saved

                        compareTourItem.dbStartIndex = compareTourProperty.startIndex;
                        compareTourItem.dbEndIndex = compareTourProperty.endIndex;

                        compareTourItem.dbSpeed = compareTourProperty.speed;
                        compareTourItem.dbElapsedTime = compareTourProperty.tourDeviceTime_Elapsed;

                     } else {

                        compareTourItem.movedSpeed = compareTourProperty.speed;
                     }

                     // update viewer
                     _tourViewer.update(compareTourItem, null);
                  }
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_compareTourPropertyListener);
   }

   /**
    * set the part listener to save the view settings, the listeners are called before the controls
    * are disposed
    */
   private void addPartListeners() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == ElevationCompareResultView.this) {

               ElevationCompareManager.clearCompareResult();
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {

            /*
             * add the actions in the part open event so they are appended AFTER the actions which
             * are defined in the plugin.xml
             */
            fillToolbar();
         }

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };

      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _tourViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _titleColor = JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_CONTENT_SUB_CATEGORY);

               _tourViewer.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _tourViewer.getTree().redraw();
            } else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

               updateToolTipState();
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               // measurement system has changed

               _columnManager.saveState(_state);
               _columnManager.clearColumns();
               defineAllColumns(_viewerContainer);

               recreateViewer(null);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * Listen to post selections
    */
   private void addSelectionListeners() {

      _postSelectionListener = new ISelectionListener() {

         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            onSelectionChanged(part, selection);
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);

   }

   private void addTourEventListener() {

      _tourPropertyListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == ElevationCompareResultView.this) {
               return;
            }

            if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {
                  updateTourViewer(_rootItem, modifiedTours);
               }

            } else if (eventId == TourEventId.UPDATE_UI) {

               // ref tour is removed -> remove all compare results

               ElevationCompareManager.clearCompareResult();

               reloadViewer();

               enableActions();

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

               reloadViewer();
            }
         }
      };
      TourManager.getInstance().addTourEventListener(_tourPropertyListener);
   }

   private void createActions() {

      _actionAppTourFilter = new ActionAppTourFilter();
      _actionCollapseAll = new ActionCollapseAll(this);
      _actionElevationCompareFilter = new ActionElevationCompareFilter();
      _actionReRunComparision = new ActionReRunComparision();

      _actionContext_CheckTours = new ActionCheckTours(this);
      _actionContext_Compare_AllTours = new ActionCompareByElevation_AllTours(this);
      _actionContext_Compare_WithWizard = new ActionCompareByElevation_WithWizard(this);
      _actionContext_EditQuick = new ActionEditQuick(this);
      _actionContext_EditTour = new ActionEditTour(this);
      _actionContext_OpenTour = new ActionOpenTour(this);
      _actionContext_RemoveComparedTourSaveStatus = new ActionRemoveComparedTourSaveStatus(this);
      _actionContext_SaveComparedTours = new ActionSaveComparedTours(this);
      _actionContext_SetTourType = new ActionSetTourTypeMenu(this);
      _actionContext_UncheckTours = new ActionUncheckTours(this);
   }

   private void createMenuManager() {

      _tagMenuManager = new TagMenuManager(this, true);

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(menuManager -> fillContextMenu(menuManager));
   }

   @Override
   public void createPartControl(final Composite parent) {

      _pc = new PixelConverter(parent);
      createMenuManager();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);

      defineAllColumns(parent);

      createUI(parent);

      addPartListeners();
      addSelectionListeners();
      addCompareTourPropertyListener();
      addPrefListener();
      addTourEventListener();

      createActions();
      fillViewMenu();

      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      _tourViewer.setInput(_rootItem = new TVIElevationCompareResult_RootItem());

      restoreState();
      enableActions();

      updateToolTipState();
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_TourViewer(_viewerContainer);
      }
   }

   private void createUI_10_TourViewer(final Composite parent) {

      final Tree tree = new Tree(parent,

            SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.FULL_SELECTION
                  | SWT.CHECK);

      GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer = new ResultTreeViewer(tree);
      _columnManager.createColumns(_tourViewer);

      _tourViewer.setContentProvider(new ResultContentProvider());
      _tourViewer.setFilters(new TourCompareFilter());
      _tourViewer.setUseHashlookup(true);

      _tourViewer.addSelectionChangedListener(selectionChangedEvent -> onSelect(selectionChangedEvent));

      _tourViewer.addDoubleClickListener(doubleClickEvent -> {

         // expand/collapse current item

         final Object treeItem = ((IStructuredSelection) doubleClickEvent.getSelection()).getFirstElement();

         if (_tourViewer.getExpandedState(treeItem)) {
            _tourViewer.collapseToLevel(treeItem, 1);
         } else {
            _tourViewer.expandToLevel(treeItem, 1);
         }
      });

      _tourViewer.getTree().addKeyListener(keyPressedAdapter(keyEvent -> {

         if (keyEvent.keyCode == SWT.DEL) {
            removeComparedTourFromDb();
         }
      }));

      _tourViewer.addCheckStateListener(checkStateChangedEvent -> {

         if (checkStateChangedEvent.getElement() instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour compareResult = (TVIElevationCompareResult_ComparedTour) checkStateChangedEvent.getElement();

            if (checkStateChangedEvent.getChecked() && compareResult.isSaved()) {

               /*
                * uncheck elements which are already stored for the reftour, it would be better
                * to disable them, but this is not possible because this is a limitation by the
                * OS
                */
               _tourViewer.setChecked(compareResult, false);

            } else {

               enableActions_ContextMenu();
            }

         } else {

            // uncheck all other tree items
            _tourViewer.setChecked(checkStateChangedEvent.getElement(), false);
         }
      });

      createUI_15_ColumnImages(tree);

      createUI_20_ContextMenu();

      // set tour info tooltip provider
      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);
   }

   private void createUI_15_ColumnImages(final Tree tree) {

      boolean isColumnVisible = false;
      final ControlListener controlResizedAdapter = controlResizedAdapter(controlEvent -> onResize_SetWidthForImageColumn());

      // update column index which is needed for repainting
      final ColumnProfile activeProfile = _columnManager.getActiveProfile();
      _columnIndex_TourTypeImage = activeProfile.getColumnIndex(_colDef_TourTypeImage.getColumnId());

      // add column resize listener
      if (_columnIndex_TourTypeImage >= 0) {

         isColumnVisible = true;
         tree.getColumn(_columnIndex_TourTypeImage).addControlListener(controlResizedAdapter);
      }

      // add tree resize listener
      if (isColumnVisible) {

         /*
          * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
          * critical for performance that these methods be as efficient as possible.
          */
         final Listener treePaintListener = event -> {

            if (event.type == SWT.PaintItem) {

               onPaint_TreeViewer(event);
            }
         };

         tree.addControlListener(controlResizedAdapter);
         tree.addListener(SWT.PaintItem, treePaintListener);
      }
   }

   /**
    * Setup context menu for the viewer
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _tourViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

   /**
    * Creates context menu for the viewer
    *
    * @return
    */
   private Menu createUI_22_CreateViewerContextMenu() {

      final Tree tree = (Tree) _tourViewer.getControl();
      final Menu treeContextMenu = _viewerMenuManager.createContextMenu(tree);
      treeContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
         }
      });

      return treeContextMenu;
   }

   private void defineAllColumns(final Composite parent) {

      defineColumn_1st_ComparedTour();
      defineColumn_Tour_Type();
      defineColumn_Data_Diff();

      defineColumn_Elevation_ElevationGain();
      defineColumn_Elevation_ElevationLoss();

      defineColumn_Motion_SpeedComputed();
      defineColumn_Motion_SpeedSaved();
      defineColumn_Motion_SpeedMoved();
      defineColumn_Motion_PaceComputed();
      defineColumn_Motion_VerticalSpeed();
      defineColumn_Motion_Distance();

      defineColumn_Time_MovingTime();

      defineColumn_Data_TimeInterval();

      defineColumn_Tour_Title();
      defineColumn_Tour_Tags();
   }

   /**
    * tree column: reference tour/date
    */
   private void defineColumn_1st_ComparedTour() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "comparedTour", SWT.LEAD); //$NON-NLS-1$

      colDef.setIsDefaultColumn();
      colDef.setColumnLabel(Messages.Compare_Result_Column_tour);
      colDef.setColumnHeaderText(Messages.Compare_Result_Column_tour);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25) + 16);
      colDef.setCanModifyVisibility(false);
      colDef.setLabelProvider(new TourInfoToolTip_CustomSelection_CellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTour == false) {
               return null;
            }

            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ReferenceTour) {

               return ((TVIElevationCompareResult_ReferenceTour) element).tourId;

            } else if (element instanceof TVIElevationCompareResult_ComparedTour) {

               return ((TVIElevationCompareResult_ComparedTour) element).getComparedTourData().getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ReferenceTour) {

               final TVIElevationCompareResult_ReferenceTour refItem = (TVIElevationCompareResult_ReferenceTour) element;
               cell.setText(refItem.label);

            } else if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;
               cell.setText(TourManager.getTourDateShort(compareItem.getComparedTourData()));

               // display an image when a tour is saved
               if (compareItem.isSaved()) {
                  cell.setImage(_dbImage);
               } else {
                  cell.setImage(null);
               }
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * column: altitude difference
    */
   private void defineColumn_Data_Diff() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "diff", SWT.TRAIL); //$NON-NLS-1$

      colDef.setIsDefaultColumn();
      colDef.setColumnHeaderText(Messages.Compare_Result_Column_diff);
      colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_diff_tooltip);
      colDef.setColumnLabel(Messages.Compare_Result_Column_diff_label);
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;

               final float value = (compareItem.minAltitudeDiff * 100)
                     / (compareItem.normalizedEndIndex - compareItem.normalizedStartIndex);

               cell.setText(Integer.toString((int) value));

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: time interval
    */
   private void defineColumn_Data_TimeInterval() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               cell.setText(Integer.toString(((TVIElevationCompareResult_ComparedTour) element).timeInterval));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Elevation gain
    */
   private void defineColumn_Elevation_ElevationGain() {

      final ColumnDefinition colDef = TreeColumnFactory.ALTITUDE_ELEVATION_TOTAL_GAIN.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour item = (TVIElevationCompareResult_ComparedTour) element;

               final float value = item.elevationGain;
               if (value == 0) {

                  cell.setText(UI.EMPTY_STRING);

               } else {

                  cell.setText(_nf0.format(value));
               }
            }
         }
      });
   }

   /**
    * Column: Elevation loss
    */
   private void defineColumn_Elevation_ElevationLoss() {

      final ColumnDefinition colDef = TreeColumnFactory.ALTITUDE_ELEVATION_TOTAL_LOSS.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour item = (TVIElevationCompareResult_ComparedTour) element;

               final float value = item.elevationLoss;
               if (value == 0) {

                  cell.setText(UI.EMPTY_STRING);

               } else {

                  cell.setText(_nf0.format(value));
               }
            }
         }
      });
   }

   /**
    * column: distance
    */
   private void defineColumn_Motion_Distance() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;

               final float value = compareItem.compareDistance / (1000 * UI.UNIT_VALUE_DISTANCE);

               colDef.printDetailValue(cell, value);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Pace computed
    */
   private void defineColumn_Motion_PaceComputed() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "paceComputed", SWT.TRAIL); //$NON-NLS-1$

      colDef.setIsDefaultColumn();
      colDef.setColumnHeaderText(UI.UNIT_LABEL_PACE);
      colDef.setColumnUnit(UI.UNIT_LABEL_PACE);
      colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_Pace_Tooltip);
      colDef.setColumnLabel(Messages.Compare_Result_Column_Pace_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;
               final double value = compareItem.comparePace * UI.UNIT_VALUE_DISTANCE;

               if (value == 0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(UI.format_mm_ss((long) value));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: speed computed
    */
   private void defineColumn_Motion_SpeedComputed() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedComputed", SWT.TRAIL); //$NON-NLS-1$

      colDef.setIsDefaultColumn();
      colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
      colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
      colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_tooltip);
      colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setValueFormats(//
            ValueFormatSet.Number,
            ValueFormat.NUMBER_1_1,
            _columnManager);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;
               final double value = compareItem.compareSpeed / UI.UNIT_VALUE_DISTANCE;

               colDef.printDetailValue(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: speed moved
    */
   private void defineColumn_Motion_SpeedMoved() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedMoved", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
      colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
      colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_moved_tooltip);
      colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_moved_label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setValueFormats(//
            ValueFormatSet.Number,
            ValueFormat.NUMBER_1_1,
            _columnManager);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;

               final double value = compareItem.movedSpeed / UI.UNIT_VALUE_DISTANCE;

               colDef.printDetailValue(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: speed saved
    */
   private void defineColumn_Motion_SpeedSaved() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "speedSaved", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
      colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
      colDef.setColumnHeaderToolTipText(Messages.Compare_Result_Column_kmh_db_tooltip);
      colDef.setColumnLabel(Messages.Compare_Result_Column_kmh_db_label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setValueFormats(//
            ValueFormatSet.Number,
            ValueFormat.NUMBER_1_1,
            _columnManager);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;

               final double value = compareItem.dbSpeed / UI.UNIT_VALUE_DISTANCE;

               colDef.printDetailValue(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Vertical speed (VAM average ascent speed)
    */
   private void defineColumn_Motion_VerticalSpeed() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "motionAltimeter", SWT.TRAIL); //$NON-NLS-1$

      colDef.setIsDefaultColumn();
      colDef.setColumnHeaderText(UI.UNIT_LABEL_ALTIMETER);
      colDef.setColumnUnit(UI.UNIT_LABEL_ALTIMETER);
      colDef.setColumnHeaderToolTipText(OtherMessages.COLUMN_FACTORY_MOTION_ALTIMETER_TOOLTIP);
      colDef.setColumnLabel(OtherMessages.COLUMN_FACTORY_MOTION_ALTIMETER);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setValueFormats(//
            ValueFormatSet.Number,
            ValueFormat.NUMBER_1_0,
            _columnManager);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) element;

               final double value = compareItem.avgAltimeter;

               colDef.printDetailValue(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__COMPUTED_MOVING_TIME_NO_CATEGORY.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final long value = ((TVIElevationCompareResult_ComparedTour) element).compareMovingTime;

               colDef.printLongValue(cell, value, true);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * column: tags
    */
   private void defineColumn_Tour_Tags() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVIElevationCompareResult_ComparedTour) {

               final Set<TourTag> tourTags = ((TVIElevationCompareResult_ComparedTour) element).getComparedTourData().getTourTags();
               if (tourTags.isEmpty()) {

                  // the tags could have been removed, set empty field

                  cell.setText(UI.EMPTY_STRING);

               } else {

                  cell.setText(TourDatabase.getTagNames(tourTags));
                  setCellColor(cell, element);
               }
            }
         }
      });
   }

   /**
    * column: title
    */
   private void defineColumn_Tour_Title() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIElevationCompareResult_ComparedTour) {
               cell.setText(((TVIElevationCompareResult_ComparedTour) element).getComparedTourData().getTourTitle());
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour type
    */
   private void defineColumn_Tour_Type() {

      _colDef_TourTypeImage = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
      _colDef_TourTypeImage.setIsDefaultColumn();
      _colDef_TourTypeImage.setLabelProvider(new CellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occures !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getSite().getPage().removePartListener(_partListener);
      TourManager.getInstance().removeTourEventListener(_compareTourPropertyListener);
      TourManager.getInstance().removeTourEventListener(_tourPropertyListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      _dbImage.dispose();

      super.dispose();
   }

   private void enableActions() {

      final boolean canReRunComparision = _rootItem != null
            && _rootItem.getUnfetchedChildren() != null
            && _rootItem.getUnfetchedChildren().size() > 0;

      _actionReRunComparision.setEnabled(canReRunComparision);
   }

   private void enableActions_ContextMenu() {

      final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

      int numTourItems = 0;
      int numSavedTourItems = 0;
      int numUnsavedTourItems = 0;
      int numRefItems = 0;

      TVIElevationCompareResult_ComparedTour firstTourItem = null;
      TVIElevationCompareResult_ComparedTour firstCheckedItem = null;
      TVIElevationCompareResult_ComparedTour firstSelectedItem = null;

      /*
       * Count selected items
       */
      int selectedTours = 0;
      for (final Object treeItem : selection) {

         if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour comparedTourItem = (TVIElevationCompareResult_ComparedTour) treeItem;

            // count tours
            if (numTourItems == 0) {
               firstTourItem = comparedTourItem;
               firstSelectedItem = comparedTourItem;
            }
            numTourItems++;

            // count saved tours
            if (comparedTourItem.isSaved()) {
               numSavedTourItems++;
            } else {
               numUnsavedTourItems++;
            }

            selectedTours++;

         } else if (treeItem instanceof TVIElevationCompareResult_ReferenceTour) {

            numRefItems++;
         }
      }

      /*
       * Count checked items
       */
      int checkedTours = 0;
      for (final Object checkedElement : _tourViewer.getCheckedElements()) {

         if (checkedElement instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour comparedTourItem = (TVIElevationCompareResult_ComparedTour) checkedElement;

            // count tours
            if (numTourItems <= 1) {
               firstTourItem = comparedTourItem;
               firstCheckedItem = comparedTourItem;
            }
            numTourItems++;

            // count saved tours
            if (comparedTourItem.isSaved()) {
               numSavedTourItems++;
            } else {
               numUnsavedTourItems++;
            }

            checkedTours++;
         }
      }

      final boolean isRefItemSelected = numRefItems > 0;
      final boolean isTourSelected = numTourItems > 0 && numRefItems == 0;
      boolean isOneTour = numTourItems == 1 && numRefItems == 0;
      boolean isOneTourSelected = selectedTours == 1;

      // check if the same tour is selected and/or checked
      if (numTourItems == 2 && numRefItems == 0 && firstSelectedItem == firstCheckedItem) {

         isOneTour = true;
         isOneTourSelected = true;
      }

      _actionContext_CheckTours.setEnabled(numUnsavedTourItems > 0);
      _actionContext_UncheckTours.setEnabled(checkedTours > 0);

      // action: save compare result
      _actionContext_SaveComparedTours.setEnabled(numUnsavedTourItems > 0);

      // action: remove tour from saved compare result, currently only one tour item is supported
      _actionContext_RemoveComparedTourSaveStatus.setEnabled(numSavedTourItems > 0);

      _actionContext_Compare_AllTours.setEnabled(isRefItemSelected);
      _actionContext_Compare_WithWizard.setEnabled(isRefItemSelected);

      // actions: edit tour
      _actionContext_EditQuick.setEnabled(isOneTourSelected);
      _actionContext_EditTour.setEnabled(isOneTourSelected);
      _actionContext_OpenTour.setEnabled(isOneTourSelected);

      // action: tour type
      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
      _actionContext_SetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

      // tags: add/remove/remove all
      Set<TourTag> allExistingTags = null;
      long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

      if (isOneTour) {

         // one tour is selected

         allExistingTags = firstTourItem.getComparedTourData().getTourTags();

         final TourType tourType = firstTourItem.getComparedTourData().getTourType();
         existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
      }
      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, allExistingTags);

      TourTypeMenuManager.enableRecentTourTypeActions(isTourSelected, existingTourTypeId);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      final String compareTooltip = isUseFastAppFilter()
            ? Messages.Elevation_Compare_Action_IsUsingAppFilter_Tooltip
            : Messages.Elevation_Compare_Action_IsNotUsingAppFilter_Tooltip;

      _actionContext_Compare_AllTours.setToolTipText(compareTooltip);
      _actionContext_Compare_WithWizard.setToolTipText(compareTooltip);

      menuMgr.add(_actionContext_SaveComparedTours);
      menuMgr.add(_actionContext_RemoveComparedTourSaveStatus);
      menuMgr.add(_actionContext_CheckTours);
      menuMgr.add(_actionContext_UncheckTours);

      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_Compare_WithWizard);
      menuMgr.add(_actionContext_Compare_AllTours);

      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_EditQuick);
      menuMgr.add(_actionContext_EditTour);
      menuMgr.add(_actionContext_OpenTour);

      // tour tag actions
      _tagMenuManager.fillTagMenu(menuMgr, true);

      // tour type actions
      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_SetTourType);
      TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

      enableActions_ContextMenu();
   }

   private void fillToolbar() {

      // check if toolbar is created
      if (_isToolbarCreated) {
         return;
      }

      _isToolbarCreated = true;

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionElevationCompareFilter);
      tbm.add(_actionAppTourFilter);
      tbm.add(_actionReRunComparision);
      tbm.add(_actionCollapseAll);

      tbm.update(true);
   }

   private void fillViewMenu() {

      /*
       * fill view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   /**
    * Recursive method to walk down the tour tree items and find the compared tours
    *
    * @param parentItem
    * @param allRemovedComparedTours
    */
   private void getComparedTours(final ArrayList<TVIElevationCompareResult_ComparedTour> comparedTours,
                                 final TreeViewerItem parentItem,
                                 final ArrayList<ElevationCompareResult> allRemovedComparedTours) {

      final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

      if (unfetchedChildren != null) {

         // children are available

         for (final TreeViewerItem treeItem : unfetchedChildren) {

            if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

               final TVIElevationCompareResult_ComparedTour ttiCompResult = (TVIElevationCompareResult_ComparedTour) treeItem;
               final long compId = ttiCompResult.compareId;

               for (final ElevationCompareResult resultItem : allRemovedComparedTours) {

                  if (compId == resultItem.compareId) {
                     comparedTours.add(ttiCompResult);
                  }
               }

            } else {

               // this is a child which can be the parent for other children
               getComparedTours(comparedTours, treeItem, allRemovedComparedTours);
            }
         }
      }
   }

   private TVIElevationCompareResult_ComparedTour getSelectedComparedTour() {

      final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
      for (final Object treeItem : selection) {

         if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

            return (TVIElevationCompareResult_ComparedTour) treeItem;
         }
      }

      return null;
   }

   private TVIElevationCompareResult_ReferenceTour getSelectedRefTour() {

      final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
      for (final Object treeItem : selection) {

         if (treeItem instanceof TVIElevationCompareResult_ReferenceTour) {

            return (TVIElevationCompareResult_ReferenceTour) treeItem;
         }
      }

      return null;
   }

   private ArrayList<Long> getSelectedRefTourIds() {

      final ArrayList<Long> selectedReferenceTour = new ArrayList<>();

      // loop: all selected items
      final IStructuredSelection selectedItems = ((IStructuredSelection) _tourViewer.getSelection());
      for (final Object treeItem : selectedItems) {

         if (treeItem instanceof TVIElevationCompareResult_ReferenceTour) {

            selectedReferenceTour.add(((TVIElevationCompareResult_ReferenceTour) treeItem).refTourItem.refId);
         }
      }

      return selectedReferenceTour;
   }

   @Override
   public ArrayList<RefTourItem> getSelectedRefTourItems() {

      final ArrayList<Long> allSelectedRefTourIds = getSelectedRefTourIds();
      final ArrayList<RefTourItem> allSelectedRefTourItems = ElevationCompareManager.createRefTourItems(allSelectedRefTourIds);

      return allSelectedRefTourItems;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tours

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      // loop: all selected tours
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour compareItem = ((TVIElevationCompareResult_ComparedTour) treeItem);
            final TourData tourData = TourManager.getInstance().getTourData(compareItem.getTourId());
            if (tourData != null) {
               selectedTourData.add(tourData);
            }
         }
      }

      return selectedTourData;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer;
   }

   /**
    * @return Returns the tour viewer
    */
   @Override
   public CheckboxTreeViewer getViewer() {
      return _tourViewer;
   }

   @Override
   public boolean isUseFastAppFilter() {

      return _actionAppTourFilter.isChecked();
   }

   TVIElevationCompareResult_ComparedTour navigateTour(final boolean isNextTour) {

      final TVIElevationCompareResult_ComparedTour selectedCompareResult = getSelectedComparedTour();
      if (selectedCompareResult != null) {

         /*
          * A compared tour is selected
          */

         final TreeViewerItem parentItem = selectedCompareResult.getParentItem();
         if (parentItem instanceof TVIElevationCompareResult_ReferenceTour) {

            final TVIElevationCompareResult_ReferenceTour refTourItem = (TVIElevationCompareResult_ReferenceTour) parentItem;

            final Object[] allSortedAndFilteredCompareResults = refTourItem.sortedAndFilteredCompareResults;
            final int numChildren = allSortedAndFilteredCompareResults.length;

            for (int childIndex = 0; childIndex < numChildren; childIndex++) {

               final Object refTourChild = allSortedAndFilteredCompareResults[childIndex];

               if (refTourChild == selectedCompareResult) {

                  if (isNextTour) {

                     // navigate next

                     if (childIndex < numChildren - 1) {

                        // next tour is available

                        final Object nextRefTourChild = allSortedAndFilteredCompareResults[childIndex + 1];
                        if (nextRefTourChild instanceof TVIElevationCompareResult_ComparedTour) {
                           return (TVIElevationCompareResult_ComparedTour) nextRefTourChild;
                        }

                     } else {

                        // return first tour

                        final Object nextRefTourChild = allSortedAndFilteredCompareResults[0];
                        if (nextRefTourChild instanceof TVIElevationCompareResult_ComparedTour) {
                           return (TVIElevationCompareResult_ComparedTour) nextRefTourChild;
                        }
                     }

                  } else {

                     // navigate previous

                     if (childIndex > 0) {

                        // previous tour is available

                        final Object nextChild = allSortedAndFilteredCompareResults[childIndex - 1];
                        if (nextChild instanceof TVIElevationCompareResult_ComparedTour) {
                           return (TVIElevationCompareResult_ComparedTour) nextChild;
                        }

                     } else {

                        // return last tour

                        final Object prevChild = allSortedAndFilteredCompareResults[numChildren - 1];
                        if (prevChild instanceof TVIElevationCompareResult_ComparedTour) {
                           return (TVIElevationCompareResult_ComparedTour) prevChild;
                        }
                     }
                  }
               }
            }
         }

      } else {

         final TVIElevationCompareResult_ReferenceTour selectedRefTour = getSelectedRefTour();
         if (selectedRefTour != null) {

            /*
             * A ref tour is selected
             */

            final ArrayList<TreeViewerItem> allRefChildren = selectedRefTour.getFetchedChildren();

            if (allRefChildren.size() > 0) {

               // navigate to the first child and ignore direction

               _tourViewer.expandToLevel(selectedRefTour, 1);

               final Object[] allRefSortedAndFilteredChildren = selectedRefTour.sortedAndFilteredCompareResults;

               if (allRefSortedAndFilteredChildren.length > 0) {

                  // return first tour
                  final Object nextRefTourChild = allRefSortedAndFilteredChildren[0];

                  if (nextRefTourChild instanceof TVIElevationCompareResult_ComparedTour) {
                     return (TVIElevationCompareResult_ComparedTour) nextRefTourChild;
                  }
               }
            }
         }
      }

      return null;
   }

   private void onPaint_TreeViewer(final Event event) {

      // paint images at the correct column

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_TourTypeImage) {

         onPaint_TreeViewer_TourTypeImage(event);
      }
   }

   private void onPaint_TreeViewer_TourTypeImage(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TVIElevationCompareResult_ComparedTour) {

         final TVIElevationCompareResult_ComparedTour tviItem = (TVIElevationCompareResult_ComparedTour) itemData;

         final TourData comparedTourData = tviItem.getComparedTourData();
         final TourType tourType = comparedTourData.getTourType();
         if (tourType != null) {

            final Image image = TourTypeImage.getTourTypeImage(tourType.getTypeId());
            if (image != null) {

               UI.paintImageCentered(event, image, _columnWidth_TourTypeImage);
            }
         }
      }
   }

   private void onResize_SetWidthForImageColumn() {

      if (_colDef_TourTypeImage != null) {

         final TreeColumn treeColumn = _colDef_TourTypeImage.getTreeColumn();

         if (treeColumn != null && treeColumn.isDisposed() == false) {

            _columnWidth_TourTypeImage = treeColumn.getWidth();
         }
      }
   }

   private void onSelect(final SelectionChangedEvent event) {

      final IStructuredSelection selection = (IStructuredSelection) event.getSelection();

      final Object treeItem = selection.getFirstElement();

      if (treeItem instanceof TVIElevationCompareResult_ReferenceTour) {

         final TVIElevationCompareResult_ReferenceTour refItem = (TVIElevationCompareResult_ReferenceTour) treeItem;

         _postSelectionProvider.setSelection(new SelectionReferenceTourView(refItem.refTourItem.refId));

      } else if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

         final TVIElevationCompareResult_ComparedTour compareResultItem = (TVIElevationCompareResult_ComparedTour) treeItem;

         _postSelectionProvider.setSelection(new StructuredSelection(compareResultItem));
      }
   }

   private void onSelectionChanged(final IWorkbenchPart part, final ISelection selection) {

      if (part == ElevationCompareResultView.this) {
         return;
      }

      if (selection instanceof StructuredSelection) {

         final Object firstElement = ((StructuredSelection) selection).getFirstElement();

         if (firstElement instanceof TVIElevationCompareResult_ComparedTour) {

            _tourViewer.setSelection(selection, true);
         }

      } else if (selection instanceof SelectionRemovedComparedTours) {

         removeComparedToursFromViewer(selection);

      } else if (selection instanceof SelectionPersistedCompareResults) {

         final SelectionPersistedCompareResults selectionPersisted = (SelectionPersistedCompareResults) selection;

         final ArrayList<TVIElevationCompareResult_ComparedTour> persistedCompareResults = selectionPersisted.persistedCompareResults;

         if (persistedCompareResults.size() > 0) {

            final TVIElevationCompareResult_ComparedTour comparedTourItem = persistedCompareResults.get(0);

            // uncheck persisted tours
            _tourViewer.setChecked(comparedTourItem, false);

            // update changed item
            _tourViewer.update(comparedTourItem, null);

         }
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _tourViewer.getExpandedElements();
         final ISelection selection = _tourViewer.getSelection();

         _tourViewer.getTree().dispose();

         createUI_10_TourViewer(_viewerContainer);
         _viewerContainer.layout();

         _tourViewer.setInput(_rootItem = new TVIElevationCompareResult_RootItem());

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection);
      }
      _viewerContainer.setRedraw(true);

      return _tourViewer;
   }

   @Override
   public void reloadViewer() {

      final Tree tree = _tourViewer.getTree();
      tree.setRedraw(false);
      {
         final Object[] expandedElements = _tourViewer.getExpandedElements();
         final ISelection selection = _tourViewer.getSelection();

         _tourViewer.setInput(_rootItem = new TVIElevationCompareResult_RootItem());

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection);
      }
      tree.setRedraw(true);
   }

   /**
    * Remove compared tour from the database
    */
   void removeComparedTourFromDb() {

      final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();
      final SelectionRemovedComparedTours selectionRemovedCompareTours = new SelectionRemovedComparedTours();
      final ArrayList<ElevationCompareResult> removedComparedTours = selectionRemovedCompareTours.removedComparedTours;

      for (final Object selectedElement : selection) {

         if (selectedElement instanceof TVIElevationCompareResult_ComparedTour) {

            final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) selectedElement;

            if (ElevationCompareManager.removeComparedTourFromDb(compareItem.compareId)) {

               removedComparedTours.add(new ElevationCompareResult(

                     compareItem.compareId,
                     compareItem.tourId,
                     compareItem.refTour.refId));
            }
         }
      }

      if (removedComparedTours.size() > 0) {

         _postSelectionProvider.setSelection(selectionRemovedCompareTours);

         removeComparedToursFromViewer(selectionRemovedCompareTours);
      }
   }

   private void removeComparedToursFromViewer(final ISelection selection) {

      final SelectionRemovedComparedTours removedTourSelection = (SelectionRemovedComparedTours) selection;
      final ArrayList<ElevationCompareResult> allRemovedComparedTours = removedTourSelection.removedComparedTours;

      /*
       * return when there are no removed tours or when the selection has not changed
       */
      if (allRemovedComparedTours.isEmpty() || removedTourSelection == _oldRemoveSelection) {
         return;
      }

      _oldRemoveSelection = removedTourSelection;

      /*
       * Find/update the removed compared tours in the viewer
       */

      final ArrayList<TVIElevationCompareResult_ComparedTour> comparedTourItems = new ArrayList<>();
      getComparedTours(comparedTourItems, _rootItem, allRemovedComparedTours);

      // reset entity for the removed compared tours
      for (final TVIElevationCompareResult_ComparedTour removedTourItem : comparedTourItems) {

         removedTourItem.compareId = -1;

         removedTourItem.dbStartIndex = -1;
         removedTourItem.dbEndIndex = -1;
         removedTourItem.dbSpeed = 0;
         removedTourItem.dbElapsedTime = 0;

         removedTourItem.movedSpeed = 0;
      }

      // update viewer
      _tourViewer.update(comparedTourItems.toArray(), null);
   }

   private void restoreState() {

      _actionAppTourFilter.setChecked(Util.getStateBoolean(_state, STATE_IS_USE_FAST_APP_TOUR_FILTER, false));
   }

   /**
    * Persist the compared tours which are checked or selected
    */
   void saveCompareResults() {

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      if (em != null) {

         final EntityTransaction ts = em.getTransaction();

         try {

            final ArrayList<TVIElevationCompareResult_ComparedTour> allUpdatedItems = new ArrayList<>();

            final SelectionPersistedCompareResults selectionCompareResult = new SelectionPersistedCompareResults();
            final ArrayList<TVIElevationCompareResult_ComparedTour> allPersistedCompareResults = selectionCompareResult.persistedCompareResults;

            /*
             * Save checked items
             */
            for (final Object checkedItem : _tourViewer.getCheckedElements()) {
               if (checkedItem instanceof TVIElevationCompareResult_ComparedTour) {

                  final TVIElevationCompareResult_ComparedTour checkedCompareItem = (TVIElevationCompareResult_ComparedTour) checkedItem;
                  if (checkedCompareItem.isSaved() == false) {

                     ElevationCompareManager.saveComparedTourItem(checkedCompareItem, em, ts);

                     allPersistedCompareResults.add(checkedCompareItem);

                     allUpdatedItems.add(checkedCompareItem);
                  }
               }
            }

            /*
             * Save selected items which are not checked
             */
            final TreeSelection selection = (TreeSelection) _tourViewer.getSelection();
            for (final Object treeItem : selection) {

               if (treeItem instanceof TVIElevationCompareResult_ComparedTour) {

                  final TVIElevationCompareResult_ComparedTour selectedComparedItem = (TVIElevationCompareResult_ComparedTour) treeItem;
                  if (selectedComparedItem.isSaved() == false) {

                     ElevationCompareManager.saveComparedTourItem(selectedComparedItem, em, ts);

                     allPersistedCompareResults.add(selectedComparedItem);

                     allUpdatedItems.add(selectedComparedItem);
                  }
               }
            }

            // uncheck all
            _tourViewer.setCheckedElements(new Object[0]);

            // update persistent status
            _tourViewer.update(allUpdatedItems.toArray(), null);

            // fire post selection to update the tour catalog view
            _postSelectionProvider.setSelection(selectionCompareResult);

         } catch (final Exception e) {
            e.printStackTrace();
         } finally {
            if (ts.isActive()) {
               ts.rollback();
            }
            em.close();
         }
      }
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_USE_FAST_APP_TOUR_FILTER, _actionAppTourFilter.isChecked());

      _columnManager.saveState(_state);
   }

   private void setCellColor(final ViewerCell cell, final Object element) {

      if (element instanceof TVIElevationCompareResult_ReferenceTour) {

         cell.setForeground(_titleColor);

      } else if (element instanceof TVIElevationCompareResult_ComparedTour) {

         // show the saved tours with a different color

         if (((TVIElevationCompareResult_ComparedTour) (element)).isSaved()) {

            cell.setForeground(_titleColor);

         } else {

            // display text with default color

            cell.setForeground(ThemeUtil.getDefaultForegroundColor_Table());
         }
      }
   }

   @Override
   public void setFocus() {

      _tourViewer.getTree().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   private void updateToolTipState() {

      _isToolTipInTour = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCOMPARERESULT_TIME);
   }

   /**
    * !!!Recursive !!! update all tour items with new data
    *
    * @param rootItem
    * @param modifiedTours
    */
   private void updateTourViewer(final TreeViewerItem parentItem, final ArrayList<TourData> modifiedTours) {

      final ArrayList<TreeViewerItem> children = parentItem.getUnfetchedChildren();

      if (children == null) {
         return;
      }

      // loop: all children
      for (final Object object : children) {

         final TreeViewerItem treeItem = (TreeViewerItem) object;

         if (object instanceof TVIElevationCompareResult_ComparedTour) {

            // update compared items

            final TVIElevationCompareResult_ComparedTour compareItem = (TVIElevationCompareResult_ComparedTour) treeItem;
            final TourData comparedTourData = compareItem.getComparedTourData();
            final long tourItemId = comparedTourData.getTourId();

            for (final TourData modifiedTourData : modifiedTours) {

               if (modifiedTourData.getTourId().longValue() == tourItemId) {

                  comparedTourData.setTourType(modifiedTourData.getTourType());
                  comparedTourData.setTourTitle(modifiedTourData.getTourTitle());
                  comparedTourData.setTourTags(modifiedTourData.getTourTags());

                  // update item in the viewer
                  _tourViewer.update(compareItem, null);

                  break;
               }
            }

         } else {

            // update children

            updateTourViewer(treeItem, modifiedTours);
         }
      }
   }

   public void updateViewer() {

      // disable filter, show all compared tours
      _compareFilter = CompareFilter.ALL_IS_DISPLAYED;
      _actionElevationCompareFilter.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourElevationCompareFilter));

      reloadViewer();

      // expand 1st ref tour

      final ArrayList<TreeViewerItem> refTourItems = _rootItem.getFetchedChildren();

      if (refTourItems.size() > 0) {

         _tourViewer.expandToLevel(refTourItems.get(0), 1, true);
      }

      enableActions();
   }
}
