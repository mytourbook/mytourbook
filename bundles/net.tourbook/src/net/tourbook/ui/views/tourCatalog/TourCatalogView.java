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
package net.tourbook.ui.views.tourCatalog;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
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
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.IReferenceTourProvider;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionCollapseOthers;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionExpandSelection;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionRefreshView;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;
import net.tourbook.ui.views.TreeViewerTourInfoToolTip;

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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class TourCatalogView extends ViewPart implements
      ITourViewer,
      ITourProvider,
      IReferenceTourProvider,
      ITreeViewer {

   public static final String     ID                                 = "net.tourbook.views.tourCatalog.TourCatalogView"; //$NON-NLS-1$

   public static final int        COLUMN_LABEL                       = 0;
   public static final int        COLUMN_SPEED                       = 1;

   private static final String    MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID = "tour.catalog.active.ref.id";                     //$NON-NLS-1$
   private static final String    MEMENTO_TOUR_CATALOG_LINK_TOUR     = "tour.catalog.link.tour";                         //$NON-NLS-1$
   private static final String    STATE_IS_USE_FAST_APP_TOUR_FILTER  = "STATE_IS_USE_FAST_APP_TOUR_FILTER";              //$NON-NLS-1$

   private final IPreferenceStore _prefStore                         = TourbookPlugin.getPrefStore();
   private final IPreferenceStore _prefStore_Common                  = CommonActivator.getPrefStore();
   private final IDialogSettings  _state                             = TourbookPlugin.getState(ID);

   private TVICatalogRootItem     _rootItem;

   private final NumberFormat     _nf1                               = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   private PostSelectionProvider               _postSelectionProvider;

   private ISelectionListener                  _postSelectionListener;
   private IPartListener2                      _partListener;
   private IPropertyChangeListener             _prefChangeListener;
   private IPropertyChangeListener             _prefChangeListener_Common;
   private ITourEventListener                  _tourEventListener;

   /**
    * tour item which is selected by the link tour action
    */
   protected TVICatalogComparedTour            _linkedTour;

   /**
    * ref id which is currently selected in the tour viewer
    */
   private long                                _activeRefId;

   /**
    * flag if actions are added to the toolbar
    */
   private boolean                             _isToolbarCreated;

   private TreeViewer                          _tourViewer;
   private ColumnManager                       _columnManager;

   private boolean                             _isToolTipInRefTour;
   private boolean                             _isToolTipInTitle;
   private boolean                             _isToolTipInTags;

   private TreeViewerTourInfoToolTip           _tourInfoToolTip;
   private TourDoubleClickState                _tourDoubleClickState      = new TourDoubleClickState();

   private TagMenuManager                      _tagMenuManager;
   private MenuManager                         _viewerMenuManager;
   private IContextMenuProvider                _viewerContextMenuProvider = new TreeContextMenuProvider();

   private ActionAppTourFilter                 _action_AppTourFilter;
   private ActionLinkTour                      _action_LinkTour;
   private ActionRefreshView                   _action_RefreshView;
   private Action_ViewLayout                   _action_ToggleRefTourLayout;

   private ActionCollapseAll                   _actionContext_CollapseAll;
   private ActionCollapseOthers                _actionContext_CollapseOthers;
   private ActionCompareByElevation_AllTours   _actionContext_Compare_AllTours;
   private ActionEditQuick                     _actionContext_EditQuick;
   private ActionEditTour                      _actionContext_EditTour;
   private ActionExpandSelection               _actionContext_ExpandSelection;
   private ActionOpenTour                      _actionContext_OpenTour;
   private ActionRemoveComparedTours           _actionContext_RemoveComparedTours;
   private ActionRenameRefTour                 _actionContext_RenameRefTour;
   private ActionSetTourTypeMenu               _actionContext_SetTourType;
   private ActionCompareByElevation_WithWizard _actionContext_Compare_WithWizard;

   private PixelConverter                      _pc;

   /*
    * UI controls
    */
   private Composite _viewerContainer;

   private Menu      _treeContextMenu;

   private class Action_ViewLayout extends Action {

      Action_ViewLayout() {

         super(null, AS_PUSH_BUTTON);

         setText(Messages.Elevation_Compare_Action_Layout_WithoutYearCategories_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Layout_Flat));
      }

      @Override
      public void run() {
         onAction_ToggleViewLayout();
      }
   }

   private class ActionAppTourFilter extends Action {

      public ActionAppTourFilter() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Elevation_Compare_Action_AppTourFilter_Tooltip);

         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Filter));
      }
   }

   class TourContentProvider implements ITreeContentProvider {

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

   public class TreeContextMenuProvider implements IContextMenuProvider {

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

   public TourCatalogView() {}

   /**
    * !!! Recursive !!!<br>
    * <br>
    * Find the compared tours in the tour map tree viewer<br>
    * <br>
    * !!! Recursive !!!<br>
    *
    * @param comparedTours
    * @param parentItem
    * @param removedComparedTours
    *           comp id's which should be found
    */
   private static void getComparedTours(final ArrayList<TVICatalogComparedTour> comparedTours,
                                        final TreeViewerItem parentItem,
                                        final ArrayList<ElevationCompareResult> removedComparedTours) {

      final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

      if (unfetchedChildren != null) {

         // children are available

         for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

            if (tourTreeItem instanceof TVICatalogComparedTour) {

               final TVICatalogComparedTour ttiCompResult = (TVICatalogComparedTour) tourTreeItem;
               final long ttiCompId = ttiCompResult.getCompId();

               for (final ElevationCompareResult compareResultItem : removedComparedTours) {

                  if (ttiCompId == compareResultItem.compareId) {
                     comparedTours.add(ttiCompResult);
                  }
               }

            } else {
               // this is a child which can be the parent for other children
               getComparedTours(comparedTours, tourTreeItem, removedComparedTours);
            }
         }
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

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

   private void addPostSelectionListener() {

      // this view part is a selection listener
      _postSelectionListener = new ISelectionListener() {

         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            // update the view when a new tour reference was created
            if (selection instanceof SelectionPersistedCompareResults) {

               final SelectionPersistedCompareResults selectionPersisted =
                     (SelectionPersistedCompareResults) selection;

               final ArrayList<TVICompareResultComparedTour> persistedCompareResults =
                     selectionPersisted.persistedCompareResults;

               if (persistedCompareResults.size() > 0) {
                  updateTourViewer(persistedCompareResults);
               }

            } else if (selection instanceof SelectionRemovedComparedTours) {

               final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

               /*
                * find/remove the removed compared tours in the viewer
                */

               final ArrayList<TVICatalogComparedTour> comparedTours = new ArrayList<>();

               getComparedTours(comparedTours, _rootItem, removedCompTours.removedComparedTours);

               // remove compared tour from the data model
               for (final TVICatalogComparedTour comparedTour : comparedTours) {
                  comparedTour.remove();
               }

               // remove compared tour from the tree viewer
               _tourViewer.remove(comparedTours.toArray());
               reloadViewer();

            } else if (selection instanceof StructuredSelection) {

               final StructuredSelection structuredSelection = (StructuredSelection) selection;

               final Object firstElement = structuredSelection.getFirstElement();

               if (firstElement instanceof TVICatalogComparedTour) {

                  // select the compared tour in the tour viewer

                  final TVICatalogComparedTour linkedTour = (TVICatalogComparedTour) firstElement;

                  // check if the linked tour is already set, prevent recursion
                  if (_linkedTour != linkedTour) {
                     _linkedTour = linkedTour;
                     selectLinkedTour();
                  }
               }
            }
         }
      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

               updateToolTipState();

            } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

               // update tourbook viewer
               _tourViewer.refresh();

            } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _tourViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _tourViewer.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _tourViewer.getTree().redraw();
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

               _tourViewer = (TreeViewer) recreateViewer(_tourViewer);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourCatalogView.this) {
               return;
            }

            if (eventId == TourEventId.COMPARE_TOUR_CHANGED
                  && eventData instanceof TourPropertyCompareTourChanged) {

               final TourPropertyCompareTourChanged compareTourProperty = (TourPropertyCompareTourChanged) eventData;

               // check if the compared tour was saved in the database
               if (compareTourProperty.isDataSaved) {

                  final ArrayList<ElevationCompareResult> allCompareItems = new ArrayList<>();
                  allCompareItems.add(new ElevationCompareResult(

                        compareTourProperty.compareId,
                        compareTourProperty.tourId,
                        compareTourProperty.refTourId));

                  // find the compared tour in the viewer
                  final ArrayList<TVICatalogComparedTour> comparedTours = new ArrayList<>();

                  getComparedTours(comparedTours, _rootItem, allCompareItems);

                  if (comparedTours.size() > 0) {

                     final TVICatalogComparedTour comparedTour = comparedTours.get(0);

                     // update entity
                     comparedTour.setStartIndex(compareTourProperty.startIndex);
                     comparedTour.setEndIndex(compareTourProperty.endIndex);

                     comparedTour.setAvgPulse(compareTourProperty.avgPulse);
                     comparedTour.setTourSpeed(compareTourProperty.speed);
                     comparedTour.setTourDeviceTime_Elapsed(compareTourProperty.tourDeviceTime_Elapsed);

                     // update the viewer
                     _tourViewer.update(comparedTour, null);
                  }
               }

            } else if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

               // get a clone of the modified tours because the tours are removed from the list
               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {
                  updateTourViewer(_rootItem, modifiedTours);
               }

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

               reloadViewer();

            } else if (eventId == TourEventId.REFERENCE_TOUR_IS_CREATED && eventData instanceof TourEvent) {

               reloadViewer();

               /*
                * Select newly created ref tour
                */
               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours.size() > 0) {

                  final TourData tourData = modifiedTours.get(0);
                  for (final TourReference refTour : tourData.getTourReferences()) {

                     selectRefTour(refTour.getRefId());

                     // LIMIT: only the first ref tour is selected
                     break;
                  }
               }

            }
         }
      };
      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _action_AppTourFilter = new ActionAppTourFilter();
      _action_LinkTour = new ActionLinkTour(this);
      _action_RefreshView = new ActionRefreshView(this);
      _action_ToggleRefTourLayout = new Action_ViewLayout();

      _actionContext_CollapseAll = new ActionCollapseAll(this);
      _actionContext_Compare_AllTours = new ActionCompareByElevation_AllTours(this);
      _actionContext_Compare_WithWizard = new ActionCompareByElevation_WithWizard(this);
      _actionContext_RemoveComparedTours = new ActionRemoveComparedTours(this);
      _actionContext_RenameRefTour = new ActionRenameRefTour(this);
      _actionContext_CollapseOthers = new ActionCollapseOthers(this);
      _actionContext_ExpandSelection = new ActionExpandSelection(this);
      _actionContext_EditQuick = new ActionEditQuick(this);
      _actionContext_EditTour = new ActionEditTour(this);
      _actionContext_OpenTour = new ActionOpenTour(this);
      _actionContext_SetTourType = new ActionSetTourTypeMenu(this);
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
      _columnManager.setIsCategoryAvailable(true);
      defineAllColumns(parent);

      createUI(parent);

      createActions();
      fillViewMenu();

      addPartListener();
      addPostSelectionListener();
      addTourEventListener();
      addPrefListener();

      // set selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      _rootItem = new TVICatalogRootItem();

      // delay loading, that the UI and app filters are initialized
      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            if (_tourViewer.getTree().isDisposed()) {
               return;
            }

            _tourViewer.setInput(this);

            restoreState();

            updateUI_ViewLayout();

            // move the horizontal scrollbar to the left border
            final ScrollBar horizontalBar = _tourViewer.getTree().getHorizontalBar();
            if (horizontalBar != null) {
               horizontalBar.setSelection(0);
            }
         }
      });
   }

   private void createUI(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
      {
         createUI_10_TourViewer(_viewerContainer);
      }
   }

   private void createUI_10_TourViewer(final Composite parent) {

      // tour tree
      final Tree tree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FLAT | SWT.MULTI | SWT.FULL_SELECTION);

      tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      tree.setHeaderVisible(true);
      tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      _tourViewer = new TreeViewer(tree);
      _columnManager.createColumns(_tourViewer);

      _tourViewer.setContentProvider(new TourContentProvider());
      _tourViewer.setUseHashlookup(true);

      _tourViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectionChanged(
            (IStructuredSelection) selectionChangedEvent.getSelection()));

      _tourViewer.addDoubleClickListener(doubleClickEvent -> {

         final IStructuredSelection selection = (IStructuredSelection) doubleClickEvent.getSelection();

         final Object tourItem = selection.getFirstElement();

         /*
          * get tour id
          */
         long tourId = -1;
         if (tourItem instanceof TVICatalogComparedTour) {
            tourId = ((TVICatalogComparedTour) tourItem).getTourId();
         }

         if (tourId != -1) {
            TourManager.getInstance().tourDoubleClickAction(TourCatalogView.this, _tourDoubleClickState);
         } else {
            // expand/collapse current item
            if (_tourViewer.getExpandedState(tourItem)) {
               _tourViewer.collapseToLevel(tourItem, 1);
            } else {
               _tourViewer.expandToLevel(tourItem, 1);
            }
         }
      });

      createUI_20_ContextMenu();

      // set tour info tooltip provider
      _tourInfoToolTip = new TreeViewerTourInfoToolTip(_tourViewer);
   }

   /**
    * create the views context menu
    */
   private void createUI_20_ContextMenu() {

      _treeContextMenu = createUI_22_CreateViewerContextMenu();

      final Tree tree = (Tree) _tourViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
   }

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

      defineColumn_1stColumn();
      defineColumn_Count();
      defineColumn_TourType();
      defineColumn_Title();
      defineColumn_Tags();
      defineColumn_Speed();
      defineColumn_Time_ElapsedTime();
      defineColumn_AvgPulse();
   }

   /**
    * first column: ref tour name/compare tour name /year
    */
   private void defineColumn_1stColumn() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_REFTOUR_TOUR.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInRefTour == false) {
               return null;
            }

            final Object element = cell.getElement();

            if ((element instanceof TVICatalogRefTourItem)) {

               // ref tour item

               return ((TVICatalogRefTourItem) element).getTourId();

            } else if (element instanceof TVICatalogComparedTour) {

               // compared tour item

               return ((TVICatalogComparedTour) element).getTourId();

            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if ((element instanceof TVICatalogRefTourItem)) {

               // ref tour item

               final TVICatalogRefTourItem refItem = (TVICatalogRefTourItem) element;

               final StyledString styledString = new StyledString();
               styledString.append(refItem.label, net.tourbook.ui.UI.TAG_STYLER);

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

            } else if (element instanceof TVICatalogYearItem) {

               // year item

               final TVICatalogYearItem yearItem = (TVICatalogYearItem) element;
               final StyledString styledString = new StyledString();
               styledString.append(Integer.toString(yearItem.year), net.tourbook.ui.UI.TAG_SUB_STYLER);
               styledString.append("   " + yearItem.numTours, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

            } else if (element instanceof TVICatalogComparedTour) {

               // compared tour item

               final LocalDate tourDate = ((TVICatalogComparedTour) element).tourDate;

               cell.setText(tourDate.format(TimeTools.Formatter_Date_S));
            }
         }
      });
   }

   /**
    * column: Avg pulse
    */
   private void defineColumn_AvgPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {

               final float value = ((TVICatalogComparedTour) element).getAvgPulse();

               colDef.printDoubleValue(cell, value, element instanceof TVICatalogComparedTour);
            }
         }
      });
   }

   /**
    * column: Count
    */
   private void defineColumn_Count() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_NUM_TOURS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVICatalogRefTourItem) {

               final int numberOfTours = ((TVICatalogRefTourItem) element).numTours;
               if (numberOfTours > 0) {
                  cell.setText(Integer.toString(numberOfTours));
               }
            }
         }
      });
   }

   /**
    * column: speed
    */
   private void defineColumn_Speed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {

               final double value = ((TVICatalogComparedTour) element).tourSpeed / UI.UNIT_VALUE_DISTANCE;

               colDef.printDoubleValue(cell, value, element instanceof TVICatalogComparedTour);
            }
         }
      });
   }

   /**
    * column: tags
    */
   private void defineColumn_Tags() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTags == false) {
               return null;
            }

            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {
               return ((TVICatalogComparedTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {
               cell.setText(TourDatabase.getTagNames(((TVICatalogComparedTour) element).tagIds));
            }
         }
      });
   }

   /**
    * column: Elapsed time (hh:mm:ss)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {

               final long value = ((TVICatalogComparedTour) element).tourDeviceTime_Elapsed;

               colDef.printLongValue(cell, value, true);
            }
         }
      });
   }

   /**
    * column: title
    */
   private void defineColumn_Title() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isToolTipInTitle == false) {
               return null;
            }

            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {
               return ((TVICatalogComparedTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {
               cell.setText(((TVICatalogComparedTour) element).tourTitle);
            }
         }
      });
   }

   /**
    * column: tour type
    */
   private void defineColumn_TourType() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVICatalogComparedTour) {
               cell.setImage(TourTypeImage.getTourTypeImage(((TVICatalogComparedTour) element).tourTypeId));
            }
         }
      });
   }

   @Override
   public void dispose() {

      getSite().getPage().removePostSelectionListener(_postSelectionListener);
      getViewSite().getPage().removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void enableActions() {

      final ITreeSelection selection = (ITreeSelection) _tourViewer.getSelection();

      int numRefItems = 0;
      int numYearItems = 0;
      int numTourItems = 0;

      TVICatalogComparedTour firstTourItem = null;

      // count number of items
      for (final Object treeItem : selection) {

         if (treeItem instanceof TVICatalogRefTourItem) {
            numRefItems++;
         } else if (treeItem instanceof TVICatalogComparedTour) {
            if (numTourItems == 0) {
               firstTourItem = (TVICatalogComparedTour) treeItem;
            }
            numTourItems++;
         } else if (treeItem instanceof TVICatalogYearItem) {
            numYearItems++;
         }
      }

      final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

      final boolean isTourSelected = numTourItems > 0;
      final boolean isRefItemSelected = numRefItems > 0;
      final boolean isOneTour = numTourItems == 1 && numRefItems == 0 && numYearItems == 0;
      final boolean isOneRefTour = numRefItems == 1 && numYearItems == 0 && numTourItems == 0;
      final boolean isEditableTour = isOneTour || isOneRefTour;

      final int numSelectedItems = selection.size();
      final boolean isOnly1SelectedItem = numSelectedItems == 1;
      final TreeViewerItem firstElement = (TreeViewerItem) selection.getFirstElement();
      final boolean firstElementHasChildren = firstElement == null ? false : firstElement.hasChildren();

      // enable remove button only, when one type of the item is selected
      final boolean canRemoveTours = numYearItems == 0 &&
            ((isRefItemSelected && numTourItems == 0)
                  || (numRefItems == 0 && numTourItems > 0));

      final boolean canExpandSelection = firstElement == null
            ? false
            : isOnly1SelectedItem
                  ? firstElementHasChildren
                  : true;

      _tourDoubleClickState.canEditTour = isEditableTour;
      _tourDoubleClickState.canOpenTour = isEditableTour;
      _tourDoubleClickState.canQuickEditTour = isEditableTour;
      _tourDoubleClickState.canEditMarker = isEditableTour;
      _tourDoubleClickState.canAdjustAltitude = isEditableTour;

      _actionContext_Compare_AllTours.setEnabled(isRefItemSelected);
      _actionContext_Compare_WithWizard.setEnabled(isRefItemSelected);

      _actionContext_RemoveComparedTours.setEnabled(canRemoveTours);
      _actionContext_RenameRefTour.setEnabled(numRefItems == 1 && numTourItems == 0 && numYearItems == 0);

      _actionContext_EditQuick.setEnabled(isEditableTour);
      _actionContext_EditTour.setEnabled(isEditableTour);
      _actionContext_OpenTour.setEnabled(isEditableTour);

      _actionContext_SetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

      _actionContext_CollapseOthers.setEnabled(isOnly1SelectedItem && firstElementHasChildren);
      _actionContext_ExpandSelection.setEnabled(canExpandSelection);

      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, firstTourItem == null ? null : firstTourItem.tagIds);

      TourTypeMenuManager.enableRecentTourTypeActions(
            isTourSelected,
            isOneTour
                  ? firstTourItem.tourTypeId
                  : TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      /*
       * Set tooltip for the compare actions
       */
      final String compareTooltip = isUseFastAppFilter()
            ? Messages.Elevation_Compare_Action_IsUsingAppFilter_Tooltip
            : Messages.Elevation_Compare_Action_IsNotUsingAppFilter_Tooltip;

      _actionContext_Compare_AllTours.setToolTipText(compareTooltip);
      _actionContext_Compare_WithWizard.setToolTipText(compareTooltip);

      /*
       * Set remove action text according to the selected items
       */
      final IStructuredSelection selection = (IStructuredSelection) _tourViewer.getSelection();
      final Object firstItem = selection.getFirstElement();
      if (firstItem instanceof TVICatalogRefTourItem) {

         // remove the reference tours and it's children
         _actionContext_RemoveComparedTours.setText(Messages.Elevation_Compare_Action_RemoveReferenceTours);

      } else {

         // remove compared tours - &Remove Compared Tours...
         _actionContext_RemoveComparedTours.setText(Messages.tourCatalog_view_action_delete_tours);
      }

      /*
       * Fill context menu
       */
      menuMgr.add(_actionContext_CollapseOthers);
      menuMgr.add(_actionContext_ExpandSelection);
      menuMgr.add(_actionContext_CollapseAll);

      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_Compare_WithWizard);
      menuMgr.add(_actionContext_Compare_AllTours);
      menuMgr.add(_actionContext_RenameRefTour);

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

      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_RemoveComparedTours);

      enableActions();
   }

   private void fillToolbar() {

      // check if toolbar is created
      if (_isToolbarCreated) {
         return;
      }

      _isToolbarCreated = true;

      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_action_AppTourFilter);
      tbm.add(_action_LinkTour);
      tbm.add(_actionContext_CollapseAll);
      tbm.add(_action_ToggleRefTourLayout);
      tbm.add(_action_RefreshView);

      tbm.update(true);
   }

   private void fillViewMenu() {

      /*
       * fill view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

   }

   void fireSelection(final ISelection selection) {
      _postSelectionProvider.setSelection(selection);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   /**
    * @return Returns the first ref tour which is selected or <code>null</code> when a ref tour is
    *         not selected.
    */
   @SuppressWarnings("unused")
   private TVICatalogRefTourItem getFirstSelectedRefTour() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVICatalogRefTourItem) {

            return ((TVICatalogRefTourItem) treeItem);
         }
      }

      return null;
   }

   /**
    * @return Returns the first tour which is selected or <code>null</code> when a tour is not
    *         selected.
    */
   @SuppressWarnings("unused")
   private TVICatalogComparedTour getFirstSelectedTour() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVICatalogComparedTour) {

            return ((TVICatalogComparedTour) treeItem);
         }
      }

      return null;
   }

   /**
    * @return Returns the first year which is selected or <code>null</code> when a year is not
    *         selected.
    */
   @SuppressWarnings("unused")
   private TVICatalogYearItem getFirstSelectedYear() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVICatalogYearItem) {

            return ((TVICatalogYearItem) treeItem);
         }
      }

      return null;
   }

   private ArrayList<Long> getSelectedRefTourIds() {

      final ArrayList<Long> selectedReferenceTour = new ArrayList<>();

      // loop: all selected items
      final IStructuredSelection selectedItems = ((IStructuredSelection) _tourViewer.getSelection());
      for (final Object treeItem : selectedItems) {

         if (treeItem instanceof TVICatalogRefTourItem) {

            selectedReferenceTour.add(((TVICatalogRefTourItem) treeItem).refId);
         }
      }

      return selectedReferenceTour;
   }

   @Override
   public ArrayList<RefTourItem> getSelectedRefTourItems() {

      final ArrayList<Long> allSelectedRefTourIds = getSelectedRefTourIds();
      final ArrayList<RefTourItem> allSelectedRefTourItems = TourCompareManager.createRefTourItems(allSelectedRefTourIds);

      return allSelectedRefTourItems;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // get selected tours

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());
      final ArrayList<TourData> selectedTourData = new ArrayList<>();

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVICatalogComparedTour) {

            final TVICatalogComparedTour tourItem = ((TVICatalogComparedTour) treeItem);

            final TourData tourData = TourManager.getInstance().getTourData(tourItem.getTourId());
            if (tourData != null) {
               selectedTourData.add(tourData);
            }

         } else if (treeItem instanceof TVICatalogRefTourItem) {

            final TVICatalogRefTourItem refItem = (TVICatalogRefTourItem) treeItem;

            final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
            if (tourData != null) {
               selectedTourData.add(tourData);
            }
         }
      }

      return selectedTourData;
   }

   public TreeViewer getTourViewer() {
      return _tourViewer;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tourViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tourViewer;
   }

   @Override
   public boolean isUseFastAppFilter() {

      return _action_AppTourFilter.isChecked();
   }

   private void onAction_ToggleViewLayout() {

      switch (TourCompareManager.getReferenceTour_ViewLayout()) {

      case TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITHOUT_YEAR_CATEGORIES:

         TourCompareManager.setReferenceTour_ViewLayout(TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES);
         break;

      case TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES:

         TourCompareManager.setReferenceTour_ViewLayout(TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITHOUT_YEAR_CATEGORIES);
         break;
      }

      // keep current vertical position
      final Tree tree = _tourViewer.getTree();
      final TreeItem topItem = tree.getTopItem();

      final Object itemData = topItem.getData();
      TVICatalogRefTourItem topRefTourItem = null;
      if (itemData instanceof TVICatalogRefTourItem) {
         topRefTourItem = (TVICatalogRefTourItem) itemData;
      }

      updateUI_ViewLayout();

      reloadViewer();

      // set to previous vertical position but only when it is a ref tour otherwise it gets complicated
      // -> compared tour can disappear but not the ref tours
      if (topRefTourItem != null) {

         final long topRefId = topRefTourItem.refId;

         final Tree recreatedTree = _tourViewer.getTree();

         /*
          * Find ref tour tree item which was lastly at the top
          */

         // loop: all ref tour items
         for (final TreeViewerItem treeItem : _rootItem.getChildren()) {

            if (treeItem instanceof TVICatalogRefTourItem) {

               final TVICatalogRefTourItem refTourItem = (TVICatalogRefTourItem) treeItem;

               if (refTourItem.refId == topRefId) {

                  // loop: all tree items
                  for (final TreeItem recreatedTreeItem : recreatedTree.getItems()) {

                     final Object treeItemData = recreatedTreeItem.getData();

                     if (treeItemData instanceof TVICatalogRefTourItem) {

                        final TVICatalogRefTourItem treeItemRefTour = (TVICatalogRefTourItem) treeItemData;

                        if (treeItemRefTour.refId == topRefId) {

                           recreatedTree.setTopItem(recreatedTreeItem);

                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Selection changes in the tour map viewer
    *
    * @param selection
    */
   private void onSelectionChanged(final IStructuredSelection selection) {

      // show the reference tour chart
      final Object item = selection.getFirstElement();

      if (item instanceof TVICatalogRefTourItem) {

         // reference tour is selected

         final TVICatalogRefTourItem refItem = (TVICatalogRefTourItem) item;

         _activeRefId = refItem.refId;

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new SelectionTourCatalogView(refItem));

      } else if (item instanceof TVICatalogYearItem) {

         // year item is selected

         final TVICatalogYearItem yearItem = (TVICatalogYearItem) item;

         _activeRefId = yearItem.refId;

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new SelectionTourCatalogView(yearItem));

      } else if (item instanceof TVICatalogComparedTour) {

         // compared tour is selected

         final TVICatalogComparedTour compItem = (TVICatalogComparedTour) item;

         _activeRefId = compItem.getRefId();

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new StructuredSelection(compItem));
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      final Object[] expandedElements = _tourViewer.getExpandedElements();
      final ISelection selection = _tourViewer.getSelection();

      _viewerContainer.setRedraw(false);
      {
         _tourViewer.getTree().dispose();

         createUI_10_TourViewer(_viewerContainer);
         _viewerContainer.layout();

         _tourViewer.setInput(_rootItem = new TVICatalogRootItem());

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

         _tourViewer.setInput(_rootItem = new TVICatalogRootItem());

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection, true);
      }
      tree.setRedraw(true);
   }

   private void restoreState() {

      _action_LinkTour.setChecked(_state.getBoolean(MEMENTO_TOUR_CATALOG_LINK_TOUR));
      _action_AppTourFilter.setChecked(Util.getStateBoolean(_state, STATE_IS_USE_FAST_APP_TOUR_FILTER, false));

      updateToolTipState();

      // select ref tour in tour viewer
      final long refId = Util.getStateLong(_state, MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID, -1);
      selectRefTour(refId);
   }

   @PersistState
   private void saveState() {

      _state.put(MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID, Long.toString(_activeRefId));
      _state.put(MEMENTO_TOUR_CATALOG_LINK_TOUR, _action_LinkTour.isChecked());

      _state.put(STATE_IS_USE_FAST_APP_TOUR_FILTER, _action_AppTourFilter.isChecked());

      _columnManager.saveState(_state);
   }

   /**
    * select the tour which was selected in the year chart
    */
   void selectLinkedTour() {
      if (_linkedTour != null && _action_LinkTour.isChecked()) {
         _tourViewer.setSelection(new StructuredSelection((_linkedTour)), true);
      }
   }

   /**
    * Select the reference tour in the tour viewer
    *
    * @param refId
    */
   private void selectRefTour(final long refId) {

      final Object[] refTourItems = _rootItem.getFetchedChildrenAsArray();

      // search ref tour
      for (final Object refTourItem : refTourItems) {
         if (refTourItem instanceof TVICatalogRefTourItem) {

            final TVICatalogRefTourItem tvtiRefTour = (TVICatalogRefTourItem) refTourItem;
            if (tvtiRefTour.refId == refId) {

               // select ref tour
               _tourViewer.setSelection(new StructuredSelection(tvtiRefTour), true);
               break;
            }
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

      _isToolTipInRefTour = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR);
      _isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE);
      _isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS);
   }

   /**
    * Update viewer with new saved compared tours
    *
    * @param persistedCompareResults
    */
   private void updateTourViewer(final ArrayList<TVICompareResultComparedTour> persistedCompareResults) {

      // ref id's which has new children
      final HashMap<Long, Long> viewRefIds = new HashMap<>();

      // get all ref tours which needs to be updated
      for (final TVICompareResultComparedTour compareResult : persistedCompareResults) {

         final TreeViewerItem parentItem = compareResult.getParentItem();

         if (parentItem instanceof TVICompareResultReferenceTour) {

            final long compResultRefId = ((TVICompareResultReferenceTour) parentItem).refTourItem.refId;

            viewRefIds.put(compResultRefId, compResultRefId);
         }
      }

      // clear selection
// have no idea why it was cleared but it prevents to update other views
//      persistedCompareResults.clear();

      // loop: all ref tours where children have been added
      for (final Long refId : viewRefIds.values()) {

         final ArrayList<TreeViewerItem> unfetchedChildren = _rootItem.getUnfetchedChildren();
         if (unfetchedChildren != null) {

            for (final TreeViewerItem rootChild : unfetchedChildren) {
               final TVICatalogRefTourItem mapRefTour = (TVICatalogRefTourItem) rootChild;

               if (mapRefTour.refId == refId) {

                  // reload the children for the reference tour
                  mapRefTour.fetchChildren();
                  _tourViewer.refresh(mapRefTour, true);

                  break;
               }
            }
         }
      }
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
         if (object instanceof TreeViewerItem) {

            final TreeViewerItem treeItem = (TreeViewerItem) object;
            if (treeItem instanceof TVICatalogComparedTour) {

               final TVICatalogComparedTour tourItem = (TVICatalogComparedTour) treeItem;
               final long tourItemId = tourItem.getTourId();

               for (final TourData modifiedTourData : modifiedTours) {
                  if (modifiedTourData.getTourId().longValue() == tourItemId) {

                     // update tree item

                     final TourType tourType = modifiedTourData.getTourType();
                     if (tourType != null) {
                        tourItem.tourTypeId = tourType.getTypeId();
                     }

                     // update item title
                     tourItem.tourTitle = modifiedTourData.getTourTitle();

                     // update item tags
                     final Set<TourTag> tourTags = modifiedTourData.getTourTags();
                     final ArrayList<Long> tagIds;

                     if (tourItem.tagIds != null) {
                        tourItem.tagIds.clear();
                     }

                     tourItem.tagIds = tagIds = new ArrayList<>();
                     for (final TourTag tourTag : tourTags) {
                        tagIds.add(tourTag.getTagId());
                     }

                     // update item in the viewer
                     _tourViewer.update(tourItem, null);

                     break;
                  }
               }

            } else {
               // update children
               updateTourViewer(treeItem, modifiedTours);
            }
         }
      }
   }

   private void updateUI_ViewLayout() {

      if (TourCompareManager.getReferenceTour_ViewLayout() == TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES) {

         // hierarchy is displayed -> show icon/tooltip for flat view

         _action_ToggleRefTourLayout.setToolTipText(Messages.Elevation_Compare_Action_Layout_WithoutYearCategories_Tooltip);

         _action_ToggleRefTourLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Layout_Flat));
         _action_ToggleRefTourLayout.setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Layout_Flat_Disabled));

      } else {

         // flat view is displayed -> show icon/tooltip for hierarchy view

         _action_ToggleRefTourLayout.setToolTipText(Messages.Elevation_Compare_Action_Layout_WithYearCategories_Tooltip);

         _action_ToggleRefTourLayout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Layout_Hierarchical));
         _action_ToggleRefTourLayout.setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.RefTour_Layout_Hierarchical_Disabled));
      }
   }

}
