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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.formatter.ValueFormatSet;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
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
import net.tourbook.ui.views.geoCompare.GeoCompareManager;
import net.tourbook.ui.views.geoCompare.GeoCompareView;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnViewer;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * Tour map -> Tour catalog -> Reference tour
 */
public class ReferenceTourView extends ViewPart implements

      ITourViewer,
      ITourProvider,
      IReferenceTourProvider,
      ITreeViewer {

   public static final String                  ID                                       = "net.tourbook.views.tourCatalog.TourCatalogView"; //$NON-NLS-1$

   public static final int                     COLUMN_LABEL                             = 0;
   public static final int                     COLUMN_SPEED                             = 1;

   private static final String                 MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID       = "tour.catalog.active.ref.id";                     //$NON-NLS-1$
   private static final String                 MEMENTO_TOUR_CATALOG_LINK_TOUR           = "tour.catalog.link.tour";                         //$NON-NLS-1$
   private static final String                 STATE_IS_ON_SELECT_EXPAND_COLLAPSE       = "STATE_IS_ON_SELECT_EXPAND_COLLAPSE";             //$NON-NLS-1$
   private static final String                 STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS   = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";         //$NON-NLS-1$
   private static final String                 STATE_IS_USE_FAST_APP_TOUR_FILTER        = "STATE_IS_USE_FAST_APP_TOUR_FILTER";              //$NON-NLS-1$

   private static final IPreferenceStore       _prefStore                               = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore       _prefStore_Common                        = CommonActivator.getPrefStore();
   private static final IDialogSettings        _state                                   = TourbookPlugin.getState(ID);

   private PostSelectionProvider               _postSelectionProvider;

   private ISelectionListener                  _postSelectionListener;
   private IPartListener2                      _partListener;
   private IPropertyChangeListener             _prefChangeListener;
   private IPropertyChangeListener             _prefChangeListener_Common;
   private ITourEventListener                  _tourEventListener;

   private TreeViewer                          _tourViewer;
   private ColumnManager                       _columnManager;
   private TreeColumnDefinition                _colDef_TourTypeImage;

   /**
    * Index of the column with the image, index can be changed when the columns are reordered with
    * the mouse or the column manager
    */
   private int                                 _columnIndex_TourTypeImage               = -1;
   private int                                 _columnWidth_TourTypeImage;

   private TVIRefTour_RootItem                 _rootItem;

   /**
    * Tour item which is selected by the link tour action
    */
   private TVIRefTour_ComparedTour             _linkedTour;

   /**
    * ref id which is currently selected in the tour viewer
    */
   private long                                _activeRefId;

   /**
    * flag if actions are added to the toolbar
    */
   private boolean                             _isToolbarCreated;

   private boolean                             _isToolTipInRefTour;
   private boolean                             _isToolTipInTitle;
   private boolean                             _isToolTipInTags;

   private boolean                             _isMouseContextMenu;
   private boolean                             _isSelectedWithKeyboard;
   private boolean                             _isBehaviour_SingleExpand_CollapseOthers = true;
   private boolean                             _isBehaviour_OnSelect_ExpandCollapse     = true;
   private boolean                             _isInCollapseAll;
   private boolean                             _isInExpandingSelection;
   private boolean                             _isInRestore;
   private boolean                             _isPartVisible;
   private int                                 _expandRunnableCounter;

   private TreeViewerTourInfoToolTip           _tourInfoToolTip;
   private TourDoubleClickState                _tourDoubleClickState                    = new TourDoubleClickState();

   private TagMenuManager                      _tagMenuManager;
   private MenuManager                         _viewerMenuManager;
   private IContextMenuProvider                _viewerContextMenuProvider               = new TreeContextMenuProvider();

   private ActionAppTourFilter                 _action_AppTourFilter;
   private ActionLinkTour                      _action_LinkTour;
   private ActionRefreshView                   _action_RefreshView;
   private Action_ViewLayout                   _action_ToggleRefTourLayout;

   private ActionCollapseAll_WithoutSelection  _actionContext_CollapseAll;
   private ActionCollapseOthers                _actionContext_CollapseOthers;
   private ActionCompareByElevation_AllTours   _actionContext_Compare_AllTours;
   private ActionCompareByElevation_WithWizard _actionContext_Compare_WithWizard;
   private ActionEditQuick                     _actionContext_EditQuick;
   private ActionEditTour                      _actionContext_EditTour;
   private ActionExpandSelection               _actionContext_ExpandSelection;
   private ActionGeoCompare                    _actionContext_GeoCompare;
   private ActionOnMouseSelect_ExpandCollapse  _actionContext_OnMouseSelect_ExpandCollapse;
   private ActionOpenTour                      _actionContext_OpenTour;
   private ActionRemoveComparedTours           _actionContext_RemoveComparedTours;
   private ActionRenameRefTour                 _actionContext_RenameRefTour;
   private ActionSetTourTypeMenu               _actionContext_SetTourType;
   private ActionSingleExpand_CollapseOthers   _actionContext_SingleExpand_CollapseOthers;

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

   private class ActionCollapseAll_WithoutSelection extends ActionCollapseAll {

      public ActionCollapseAll_WithoutSelection(final ReferenceTourView referenceTourView) {
         super(referenceTourView);
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

   private class ActionGeoCompare extends Action {

      public ActionGeoCompare() {

         super(Messages.Tour_Action_GeoCompare, AS_PUSH_BUTTON);

         setToolTipText(Messages.Tour_Action_GeoCompare_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_GeoCompare_Tool));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourCompare_GeoCompare_Tool_Disabled));
      }

      @Override
      public void run() {

         onAction_GeoCompare();
      }
   }

   private class ActionOnMouseSelect_ExpandCollapse extends Action {

      public ActionOnMouseSelect_ExpandCollapse() {

         super(Messages.Tour_Tags_Action_OnMouseSelect_ExpandCollapse, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _isBehaviour_OnSelect_ExpandCollapse = _actionContext_OnMouseSelect_ExpandCollapse.isChecked();
      }
   }

   private class ActionSingleExpand_CollapseOthers extends Action {

      public ActionSingleExpand_CollapseOthers() {

         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         _isBehaviour_SingleExpand_CollapseOthers = _actionContext_SingleExpand_CollapseOthers.isChecked();
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

   public ReferenceTourView() {}

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
   private static void getComparedTours(final ArrayList<TVIRefTour_ComparedTour> comparedTours,
                                        final TreeViewerItem parentItem,
                                        final ArrayList<ElevationCompareResult> removedComparedTours) {

      final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

      if (unfetchedChildren != null) {

         // children are available

         for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

            if (tourTreeItem instanceof TVIRefTour_ComparedTour) {

               final TVIRefTour_ComparedTour ttiCompResult = (TVIRefTour_ComparedTour) tourTreeItem;
               final long ttiCompId = ttiCompResult.getCompareId();

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
         public void partActivated(final IWorkbenchPartReference partRef) {

            _isPartVisible = true;
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {

            _isPartVisible = false;
         }

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

            if (_isPartVisible == false) {

               // prevent to open this view with a selection event

               return;
            }

            // update the view when a new tour reference was created
            if (selection instanceof SelectionPersistedCompareResults) {

               final SelectionPersistedCompareResults selectionPersisted =
                     (SelectionPersistedCompareResults) selection;

               final ArrayList<TVIElevationCompareResult_ComparedTour> persistedCompareResults =
                     selectionPersisted.persistedCompareResults;

               if (persistedCompareResults.size() > 0) {
                  updateTourViewer(persistedCompareResults);
               }

            } else if (selection instanceof SelectionRemovedComparedTours) {

               final SelectionRemovedComparedTours removedCompTours = (SelectionRemovedComparedTours) selection;

               /*
                * find/remove the removed compared tours in the viewer
                */

               final ArrayList<TVIRefTour_ComparedTour> comparedTours = new ArrayList<>();

               getComparedTours(comparedTours, _rootItem, removedCompTours.removedComparedTours);

               // remove compared tour from the data model
               for (final TVIRefTour_ComparedTour comparedTour : comparedTours) {
                  comparedTour.remove();
               }

               // remove compared tour from the tree viewer
               _tourViewer.remove(comparedTours.toArray());
               reloadViewer();

            } else if (selection instanceof StructuredSelection) {

               final StructuredSelection structuredSelection = (StructuredSelection) selection;

               final Object firstElement = structuredSelection.getFirstElement();

               if (firstElement instanceof TVIRefTour_ComparedTour) {

                  // select the compared tour in the tour viewer

                  final TVIRefTour_ComparedTour linkedTour = (TVIRefTour_ComparedTour) firstElement;

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

            if (part == ReferenceTourView.this) {
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
                  final ArrayList<TVIRefTour_ComparedTour> comparedTours = new ArrayList<>();

                  getComparedTours(comparedTours, _rootItem, allCompareItems);

                  if (comparedTours.size() > 0) {

                     final TVIRefTour_ComparedTour comparedTour = comparedTours.get(0);

                     // update entity
                     comparedTour.setStartIndex(compareTourProperty.startIndex);
                     comparedTour.setEndIndex(compareTourProperty.endIndex);

                     comparedTour.setAvgAltimeter(compareTourProperty.avgAltimeter);
                     comparedTour.setAvgPulse(compareTourProperty.avgPulse);
                     comparedTour.setMaxPulse(compareTourProperty.maxPulse);
                     comparedTour.setTourDeviceTime_Elapsed(compareTourProperty.tourDeviceTime_Elapsed);

                     comparedTour.setTourSpeed(compareTourProperty.speed);
                     comparedTour.setTourPace(compareTourProperty.pace);

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

// SET_FORMATTING_OFF

      _action_AppTourFilter                        = new ActionAppTourFilter();
      _action_LinkTour                             = new ActionLinkTour(this);
      _action_RefreshView                          = new ActionRefreshView(this);
      _action_ToggleRefTourLayout                  = new Action_ViewLayout();

      _actionContext_CollapseAll                   = new ActionCollapseAll_WithoutSelection(this);
      _actionContext_CollapseOthers                = new ActionCollapseOthers(this);
      _actionContext_Compare_AllTours              = new ActionCompareByElevation_AllTours(this);
      _actionContext_Compare_WithWizard            = new ActionCompareByElevation_WithWizard(this);
      _actionContext_EditQuick                     = new ActionEditQuick(this);
      _actionContext_EditTour                      = new ActionEditTour(this);
      _actionContext_ExpandSelection               = new ActionExpandSelection(this);
      _actionContext_GeoCompare                    = new ActionGeoCompare();
      _actionContext_OnMouseSelect_ExpandCollapse  = new ActionOnMouseSelect_ExpandCollapse();
      _actionContext_OpenTour                      = new ActionOpenTour(this);
      _actionContext_RemoveComparedTours           = new ActionRemoveComparedTours(this);
      _actionContext_RenameRefTour                 = new ActionRenameRefTour(this);
      _actionContext_SetTourType                   = new ActionSetTourTypeMenu(this);
      _actionContext_SingleExpand_CollapseOthers   = new ActionSingleExpand_CollapseOthers();

// SET_FORMATTING_ON
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

      _rootItem = new TVIRefTour_RootItem();

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

      tree.addListener(SWT.MouseDown, event -> {

         _isMouseContextMenu = event.button == 3;
      });

      tree.addKeyListener(keyPressedAdapter(keyEvent -> {

         _isSelectedWithKeyboard = true;
      }));

      _tourViewer = new TreeViewer(tree);
      _columnManager.createColumns(_tourViewer);

      _tourViewer.setContentProvider(new TourContentProvider());
      _tourViewer.setUseHashlookup(true);

      _tourViewer.addSelectionChangedListener(selectionChangedEvent -> onTourViewer_Selection(selectionChangedEvent));

      _tourViewer.addDoubleClickListener(doubleClickEvent -> {

         final IStructuredSelection selection = (IStructuredSelection) doubleClickEvent.getSelection();

         final Object tourItem = selection.getFirstElement();

         /*
          * get tour id
          */
         long tourId = -1;
         if (tourItem instanceof TVIRefTour_ComparedTour) {
            tourId = ((TVIRefTour_ComparedTour) tourItem).getTourId();
         }

         if (tourId != -1) {
            TourManager.getInstance().tourDoubleClickAction(ReferenceTourView.this, _tourDoubleClickState);
         } else {
            // expand/collapse current item
            if (_tourViewer.getExpandedState(tourItem)) {
               _tourViewer.collapseToLevel(tourItem, 1);
            } else {
               _tourViewer.expandToLevel(tourItem, 1);
            }
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
      defineColumn_HasGeoData();
      defineColumn_TourType();
      defineColumn_Title();
      defineColumn_Tags();
      defineColumn_Time_ElapsedTime();
      defineColumn_AvgSpeed();
      defineColumn_AvgPace();
      defineColumn_AvgAltimeter();
      defineColumn_AvgPulse();
      defineColumn_MaxPulse();
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

            if ((element instanceof TVIRefTour_RefTourItem)) {

               // ref tour item

               return ((TVIRefTour_RefTourItem) element).getTourId();

            } else if (element instanceof TVIRefTour_ComparedTour) {

               // compared tour item

               return ((TVIRefTour_ComparedTour) element).getTourId();

            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if ((element instanceof TVIRefTour_RefTourItem)) {

               // ref tour item

               final TVIRefTour_RefTourItem refItem = (TVIRefTour_RefTourItem) element;

               final StyledString styledString = new StyledString();
               styledString.append(refItem.label, net.tourbook.ui.UI.TAG_STYLER);

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

            } else if (element instanceof TVIRefTour_YearItem) {

               // year item

               final TVIRefTour_YearItem yearItem = (TVIRefTour_YearItem) element;
               final StyledString styledString = new StyledString();
               styledString.append(Integer.toString(yearItem.year), net.tourbook.ui.UI.TAG_SUB_STYLER);
               styledString.append("   " + yearItem.numTours, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

            } else if (element instanceof TVIRefTour_ComparedTour) {

               // compared tour item

               final LocalDate tourDate = ((TVIRefTour_ComparedTour) element).tourDate;

               cell.setText(tourDate.format(TimeTools.Formatter_Date_S));
            }
         }
      });
   }

   /**
    * Column: Avg vertical speed (VAM average ascent speed)
    */
   private void defineColumn_AvgAltimeter() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "motionAltimeter", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnCategory(OtherMessages.COLUMN_FACTORY_CATEGORY_MOTION);

      colDef.setIsDefaultColumn();
      colDef.setColumnHeaderText(UI.UNIT_LABEL_ALTIMETER);
      colDef.setColumnUnit(UI.UNIT_LABEL_ALTIMETER);
      colDef.setColumnHeaderToolTipText(OtherMessages.COLUMN_FACTORY_MOTION_ALTIMETER_TOOLTIP);
      colDef.setColumnLabel(OtherMessages.COLUMN_FACTORY_MOTION_ALTIMETER);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setValueFormats(
            ValueFormatSet.Number,
            ValueFormat.NUMBER_1_0,
            _columnManager);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final TVIRefTour_ComparedTour compareItem = (TVIRefTour_ComparedTour) element;

               final double value = compareItem.avgAltimeter;

               colDef.printDetailValue(cell, value);
            }
         }
      });
   }

   /**
    * Column: Average pace
    */
   private void defineColumn_AvgPace() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final double value = ((TVIRefTour_ComparedTour) element).avgPace * UI.UNIT_VALUE_DISTANCE;

               if (value == 0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(UI.format_mm_ss((long) value));
               }
            }
         }
      });
   }

   /**
    * Column: Avg pulse
    */
   private void defineColumn_AvgPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final float value = ((TVIRefTour_ComparedTour) element).avgPulse;

               colDef.printDoubleValue(cell, value, element instanceof TVIRefTour_ComparedTour);
            }
         }
      });
   }

   /**
    * Column: Average speed
    */
   private void defineColumn_AvgSpeed() {

      final TreeColumnDefinition colDef = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final double value = ((TVIRefTour_ComparedTour) element).avgSpeed / UI.UNIT_VALUE_DISTANCE;

               colDef.printDoubleValue(cell, value, element instanceof TVIRefTour_ComparedTour);
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
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_RefTourItem) {

               final int numberOfTours = ((TVIRefTour_RefTourItem) element).numTours;
               if (numberOfTours > 0) {
                  cell.setText(Integer.toString(numberOfTours));
               }
            }
         }
      });
   }

   /**
    * Column: Has geo data
    */
   private void defineColumn_HasGeoData() {

      final TreeColumnDefinition colDef = TreeColumnFactory.DATA_HAS_GEO_DATA.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_RefTourItem) {

               final boolean hasGeoData = ((TVIRefTour_RefTourItem) element).hasGeoData;
               if (hasGeoData) {
                  cell.setText(UI.SYMBOL_FULL_BLOCK);
               }
            }
         }
      });
   }

   /**
    * Column: Max pulse
    */
   private void defineColumn_MaxPulse() {

      final TreeColumnDefinition colDef = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager, _pc);

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new SelectionCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final float value = ((TVIRefTour_ComparedTour) element).maxPulse;

               colDef.printDoubleValue(cell, value, element instanceof TVIRefTour_ComparedTour);
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
            if (element instanceof TVIRefTour_ComparedTour) {
               return ((TVIRefTour_ComparedTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {
               cell.setText(TourDatabase.getTagNames(((TVIRefTour_ComparedTour) element).tagIds));
            }
         }
      });
   }

   /**
    * column: Elapsed time (hh:mm:ss)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TreeColumnDefinition colDef = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager, _pc);

      colDef.setLabelProvider(new SelectionCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {

               final long value = ((TVIRefTour_ComparedTour) element).tourDeviceTime_Elapsed;

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
            if (element instanceof TVIRefTour_ComparedTour) {
               return ((TVIRefTour_ComparedTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVIRefTour_ComparedTour) {
               cell.setText(((TVIRefTour_ComparedTour) element).tourTitle);
            }
         }
      });
   }

   /**
    * Column: Tour type
    */
   private void defineColumn_TourType() {

      _colDef_TourTypeImage = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
      _colDef_TourTypeImage.setIsDefaultColumn();
      _colDef_TourTypeImage.setLabelProvider(new SelectionCellLabelProvider() {

         // !!! When using cell.setImage() then it is not centered !!!
         // !!! Set dummy label provider, otherwise an error occures !!!
         @Override
         public void update(final ViewerCell cell) {}
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

      TVIRefTour_ComparedTour firstTourItem = null;

      // count number of items
      for (final Object treeItem : selection) {

         if (treeItem instanceof TVIRefTour_RefTourItem) {
            numRefItems++;
         } else if (treeItem instanceof TVIRefTour_ComparedTour) {
            if (numTourItems == 0) {
               firstTourItem = (TVIRefTour_ComparedTour) treeItem;
            }
            numTourItems++;
         } else if (treeItem instanceof TVIRefTour_YearItem) {
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

// SET_FORMATTING_OFF

      _tourDoubleClickState.canEditTour         = isEditableTour;
      _tourDoubleClickState.canOpenTour         = isEditableTour;
      _tourDoubleClickState.canQuickEditTour    = isEditableTour;
      _tourDoubleClickState.canEditMarker       = isEditableTour;
      _tourDoubleClickState.canAdjustAltitude   = isEditableTour;

      _actionContext_GeoCompare           .setEnabled(isOneRefTour);
      _actionContext_Compare_AllTours     .setEnabled(isRefItemSelected);
      _actionContext_Compare_WithWizard   .setEnabled(isRefItemSelected);

      _actionContext_RemoveComparedTours  .setEnabled(canRemoveTours);
      _actionContext_RenameRefTour        .setEnabled(isOneRefTour);

      _actionContext_EditQuick            .setEnabled(isEditableTour);
      _actionContext_EditTour             .setEnabled(isEditableTour);
      _actionContext_OpenTour             .setEnabled(isEditableTour);

      _actionContext_SetTourType          .setEnabled(isTourSelected && tourTypes.size() > 0);

      _actionContext_CollapseOthers       .setEnabled(isOnly1SelectedItem && firstElementHasChildren);
      _actionContext_ExpandSelection      .setEnabled(canExpandSelection);

      _tagMenuManager.enableTagActions(isTourSelected, isOneTour, firstTourItem == null ? null : firstTourItem.tagIds);

// SET_FORMATTING_ON

      TourTypeMenuManager.enableRecentTourTypeActions(
            isTourSelected,
            isOneTour
                  ? firstTourItem.tourTypeId
                  : TourDatabase.ENTITY_IS_NOT_SAVED);
   }

   private void expandCollapseItem(final TreeViewerItem treeItem) {

      if (_tourViewer.getExpandedState(treeItem)) {

         _tourViewer.collapseToLevel(treeItem, 1);

      } else {

         _tourViewer.expandToLevel(treeItem, 1);
      }
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
      if (firstItem instanceof TVIRefTour_RefTourItem) {

         // remove the reference tours and it's children
         _actionContext_RemoveComparedTours.setText(Messages.Elevation_Compare_Action_RemoveReferenceTours);

      } else {

         // remove compared tours - &Remove Compared Tours...
         _actionContext_RemoveComparedTours.setText(Messages.RefTour_Action_DeleteTours);
      }

      /*
       * Fill context menu
       */
      menuMgr.add(_actionContext_CollapseOthers);
      menuMgr.add(_actionContext_ExpandSelection);
      menuMgr.add(_actionContext_CollapseAll);
      menuMgr.add(_actionContext_OnMouseSelect_ExpandCollapse);
      menuMgr.add(_actionContext_SingleExpand_CollapseOthers);

      menuMgr.add(new Separator());
      menuMgr.add(_actionContext_GeoCompare);
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
   private TVIRefTour_RefTourItem getFirstSelectedRefTour() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVIRefTour_RefTourItem) {

            return ((TVIRefTour_RefTourItem) treeItem);
         }
      }

      return null;
   }

   /**
    * @return Returns the first tour which is selected or <code>null</code> when a tour is not
    *         selected.
    */
   @SuppressWarnings("unused")
   private TVIRefTour_ComparedTour getFirstSelectedTour() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVIRefTour_ComparedTour) {

            return ((TVIRefTour_ComparedTour) treeItem);
         }
      }

      return null;
   }

   /**
    * @return Returns the first year which is selected or <code>null</code> when a year is not
    *         selected.
    */
   @SuppressWarnings("unused")
   private TVIRefTour_YearItem getFirstSelectedYear() {

      final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVIRefTour_YearItem) {

            return ((TVIRefTour_YearItem) treeItem);
         }
      }

      return null;
   }

   private ArrayList<Long> getSelectedRefTourIds() {

      final ArrayList<Long> selectedReferenceTour = new ArrayList<>();

      // loop: all selected items
      final IStructuredSelection selectedItems = ((IStructuredSelection) _tourViewer.getSelection());
      for (final Object treeItem : selectedItems) {

         if (treeItem instanceof TVIRefTour_RefTourItem) {

            selectedReferenceTour.add(((TVIRefTour_RefTourItem) treeItem).refId);
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

      // loop: all selected items
      for (final Object treeItem : selectedTours) {

         if (treeItem instanceof TVIRefTour_ComparedTour) {

            final TVIRefTour_ComparedTour tourItem = ((TVIRefTour_ComparedTour) treeItem);

            final TourData tourData = TourManager.getInstance().getTourData(tourItem.getTourId());
            if (tourData != null) {
               selectedTourData.add(tourData);
            }

         } else if (treeItem instanceof TVIRefTour_RefTourItem) {

            final TVIRefTour_RefTourItem refItem = (TVIRefTour_RefTourItem) treeItem;

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

   private void onAction_GeoCompare() {

      if (GeoCompareManager.isGeoComparingOn() == false) {

         // turn it on

         GeoCompareManager.setGeoComparing(true, ReferenceTourView.this);
      }

      final Object firstItem = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();
      if ((firstItem instanceof TVIRefTour_RefTourItem) == false) {
         return;
      }

      final TVIRefTour_RefTourItem refTourItem = (TVIRefTour_RefTourItem) firstItem;
      final long refId = refTourItem.refId;

      // load reference tour from the database
      final TourReference referenceTour = ReferenceTourManager.getReferenceTour(refId);
      if (referenceTour == null) {
         return;
      }

      final TourData refTour_TourData = referenceTour.getTourData();

      if (refTour_TourData.hasGeoData() == false) {

         MessageDialog.openInformation(
               _viewerContainer.getShell(),
               Messages.RefTour_Dialog_NoGeoInRefTour_Title,
               NLS.bind(Messages.RefTour_Dialog_NoGeoInRefTour_Message, referenceTour.getLabel()));

         return;
      }

      // show and get geo compare view
      final IViewPart geoCompareViewPart = Util.showView(GeoCompareView.ID, true);

      if (geoCompareViewPart instanceof GeoCompareView) {

         final GeoCompareView geoCompareView = (GeoCompareView) geoCompareViewPart;

//issue: do not final start or show final geo compared tours

         geoCompareView.compareNativeRefTour(refId);
      }
   }

   private void onAction_ToggleViewLayout() {

      switch (ElevationCompareManager.getReferenceTour_ViewLayout()) {

      case ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITHOUT_YEAR_CATEGORIES:

         ElevationCompareManager.setReferenceTour_ViewLayout(ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES);
         break;

      case ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES:

         ElevationCompareManager.setReferenceTour_ViewLayout(ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITHOUT_YEAR_CATEGORIES);
         break;
      }

      // keep current vertical position
      final Tree tree = _tourViewer.getTree();
      final TreeItem topItem = tree.getTopItem();

      final Object itemData = topItem.getData();
      TVIRefTour_RefTourItem topRefTourItem = null;
      if (itemData instanceof TVIRefTour_RefTourItem) {
         topRefTourItem = (TVIRefTour_RefTourItem) itemData;
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

            if (treeItem instanceof TVIRefTour_RefTourItem) {

               final TVIRefTour_RefTourItem refTourItem = (TVIRefTour_RefTourItem) treeItem;

               if (refTourItem.refId == topRefId) {

                  // loop: all tree items
                  for (final TreeItem recreatedTreeItem : recreatedTree.getItems()) {

                     final Object treeItemData = recreatedTreeItem.getData();

                     if (treeItemData instanceof TVIRefTour_RefTourItem) {

                        final TVIRefTour_RefTourItem treeItemRefTour = (TVIRefTour_RefTourItem) treeItemData;

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

   private void onPaint_TreeViewer(final Event event) {

      // paint images at the correct column

      final int columnIndex = event.index;

      if (columnIndex == _columnIndex_TourTypeImage) {

         onPaint_TreeViewer_TourTypeImage(event);
      }
   }

   private void onPaint_TreeViewer_TourTypeImage(final Event event) {

      final Object itemData = event.item.getData();

      if (itemData instanceof TVIRefTour_ComparedTour) {

         final TVIRefTour_ComparedTour tviItem = (TVIRefTour_ComparedTour) itemData;
         final long tourTypeId = tviItem.tourTypeId;

         final Image image = TourTypeImage.getTourTypeImage(tourTypeId);
         if (image != null) {

            UI.paintImageCentered(event, image, _columnWidth_TourTypeImage);
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

      onSelect_CategoryItem_10_AutoExpandCollapse(treeSelection, selectedTreePath);
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded childrens are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onSelect_CategoryItem_10_AutoExpandCollapse(final ITreeSelection treeSelection,
                                                            final TreePath selectedTreePath) {

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
            private TreePath       __selectedTreePath      = selectedTreePath;

            @Override
            public void run() {

               // check if a newer expand event occured
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (_tourViewer.getTree().isDisposed()) {
                  return;
               }

               onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(
                     __treeSelection,
                     __selectedTreePath);
            }
         });

      } else {

         if (_isBehaviour_OnSelect_ExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard
            expandCollapseItem((TreeViewerItem) selectedTreePath.getLastSegment());
         }
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onSelect_CategoryItem_20_AutoExpandCollapse_Runnable(final ITreeSelection treeSelection,
                                                                     final TreePath selectedTreePath) {
      _isInExpandingSelection = true;
      {
         final Tree tree = _tourViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _tourViewer.getExpandedState(selectedTreePath);

            /*
             * Collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _tourViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _tourViewer.setExpandedState(treePath, false);
            }

            /*
             * Expand and select selected folder
             */
            _tourViewer.setExpandedTreePaths(selectedTreePath);
            _tourViewer.setSelection(treeSelection, true);

            if (_isBehaviour_OnSelect_ExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _tourViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * Set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * win behaviour: when an item is set to top which was collapsed bevore, it will be
             * expanded
             */
            if (topItem.isDisposed() == false) {
               tree.setTopItem(topItem);
            }
         }
         tree.setRedraw(true);
      }
      _isInExpandingSelection = false;
   }

   /**
    * Selection changes in the tour map viewer
    *
    * @param selectionChangedEvent
    */
   private void onTourViewer_Selection(final SelectionChangedEvent selectionChangedEvent) {

      if (_isInRestore || _isMouseContextMenu) {
         return;
      }

      /*
       * First activate this view, otherwise the selection is not fired. This happened very
       * often which is the reason why this workaround is implemented
       */
      UI.activateView(this, ID);

      onTourViewer_Selection_FireSelection(selectionChangedEvent);
   }

   private void onTourViewer_Selection_FireSelection(final SelectionChangedEvent selectionChangedEvent) {

      final TreeSelection treeSelection = (TreeSelection) selectionChangedEvent.getSelection();

      boolean isCategorySelected = false;

      // show the reference tour chart
      final Object item = treeSelection.getFirstElement();

      if (item instanceof TVIRefTour_RefTourItem) {

         // reference tour is selected

         final TVIRefTour_RefTourItem refItem = (TVIRefTour_RefTourItem) item;

         _activeRefId = refItem.refId;

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new SelectionReferenceTourView(refItem));

         isCategorySelected = true;

      } else if (item instanceof TVIRefTour_YearItem) {

         // year item is selected

         final TVIRefTour_YearItem yearItem = (TVIRefTour_YearItem) item;

         _activeRefId = yearItem.refId;

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new SelectionReferenceTourView(yearItem));

         isCategorySelected = true;

      } else if (item instanceof TVIRefTour_ComparedTour) {

         // compared tour is selected

         final TVIRefTour_ComparedTour compItem = (TVIRefTour_ComparedTour) item;

         _activeRefId = compItem.getRefId();

         // fire selection for the selected tour catalog item
         _postSelectionProvider.setSelection(new StructuredSelection(compItem));
      }

      // category is selected, expand/collapse category items

      if (_isSelectedWithKeyboard == false) {

         // do not expand/collapse when keyboard is used -> unusable

         if (isCategorySelected) {

            onSelect_CategoryItem(treeSelection);
         }
      }

      // reset state
      _isSelectedWithKeyboard = false;
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

         _tourViewer.setInput(_rootItem = new TVIRefTour_RootItem());

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

         _tourViewer.setInput(_rootItem = new TVIRefTour_RootItem());

         _tourViewer.setExpandedElements(expandedElements);
         _tourViewer.setSelection(selection, true);
      }
      tree.setRedraw(true);
   }

   private void restoreState() {

      _action_LinkTour.setChecked(_state.getBoolean(MEMENTO_TOUR_CATALOG_LINK_TOUR));
      _action_AppTourFilter.setChecked(Util.getStateBoolean(_state, STATE_IS_USE_FAST_APP_TOUR_FILTER, false));

      // on mouse select -> expand/collapse
      _isBehaviour_OnSelect_ExpandCollapse = Util.getStateBoolean(_state, STATE_IS_ON_SELECT_EXPAND_COLLAPSE, true);
      _actionContext_OnMouseSelect_ExpandCollapse.setChecked(_isBehaviour_OnSelect_ExpandCollapse);

      // single expand -> collapse others
      _isBehaviour_SingleExpand_CollapseOthers = Util.getStateBoolean(_state, STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, true);
      _actionContext_SingleExpand_CollapseOthers.setChecked(_isBehaviour_SingleExpand_CollapseOthers);

      updateToolTipState();

      // select ref tour in tour viewer
      final long refId = Util.getStateLong(_state, MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID, -1);

      _isInRestore = true;
      {
         selectRefTour(refId);
      }
      _isInRestore = false;
   }

   @PersistState
   private void saveState() {

      _state.put(MEMENTO_TOUR_CATALOG_ACTIVE_REF_ID, Long.toString(_activeRefId));
      _state.put(MEMENTO_TOUR_CATALOG_LINK_TOUR, _action_LinkTour.isChecked());

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionContext_SingleExpand_CollapseOthers.isChecked());
      _state.put(STATE_IS_ON_SELECT_EXPAND_COLLAPSE, _actionContext_OnMouseSelect_ExpandCollapse.isChecked());
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
         if (refTourItem instanceof TVIRefTour_RefTourItem) {

            final TVIRefTour_RefTourItem tvtiRefTour = (TVIRefTour_RefTourItem) refTourItem;
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
   private void updateTourViewer(final ArrayList<TVIElevationCompareResult_ComparedTour> persistedCompareResults) {

      // ref id's which has new children
      final HashMap<Long, Long> viewRefIds = new HashMap<>();

      // get all ref tours which needs to be updated
      for (final TVIElevationCompareResult_ComparedTour compareResult : persistedCompareResults) {

         final TreeViewerItem parentItem = compareResult.getParentItem();

         if (parentItem instanceof TVIElevationCompareResult_ReferenceTour) {

            final long compResultRefId = ((TVIElevationCompareResult_ReferenceTour) parentItem).refTourItem.refId;

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
               final TVIRefTour_RefTourItem mapRefTour = (TVIRefTour_RefTourItem) rootChild;

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
            if (treeItem instanceof TVIRefTour_ComparedTour) {

               final TVIRefTour_ComparedTour tourItem = (TVIRefTour_ComparedTour) treeItem;
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

      if (ElevationCompareManager.getReferenceTour_ViewLayout() == ElevationCompareManager.REF_TOUR_VIEW_LAYOUT_WITH_YEAR_CATEGORIES) {

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
