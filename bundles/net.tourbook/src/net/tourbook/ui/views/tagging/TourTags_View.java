/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.commands.AppCommands;
import net.tourbook.commands.IRestorablePart;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.PrefPageTags;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class TourTags_View extends ViewPart implements ITreeViewer, ITourViewer, ISaveablePart, IRestorablePart {

   static public final String         ID                                        = "net.tourbook.ui.views.tagging.TourTags_View"; //$NON-NLS-1$

   private static final String        STATE_IS_HIERARCHICAL_LAYOUT              = "STATE_IS_HIERARCHICAL_LAYOUT";                //$NON-NLS-1$
   private static final String        STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS    = "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";      //$NON-NLS-1$
   private static final String        STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED = "STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED";   //$NON-NLS-1$
   private static final String        STATE_SORT_COLUMN_DIRECTION               = "STATE_SORT_COLUMN_DIRECTION";                 //$NON-NLS-1$
   private static final String        STATE_SORT_COLUMN_ID                      = "STATE_SORT_COLUMN_ID";                        //$NON-NLS-1$

   private static final String        COLUMN_ID                                 = "id";                                          //$NON-NLS-1$
   private static final String        COLUMN_NOTES                              = "notes";                                       //$NON-NLS-1$
   private static final String        COLUMN_TAGS                               = "tags";                                        //$NON-NLS-1$

   private final IDialogSettings      _state                                    = TourbookPlugin.getState("TourTagsView");       //$NON-NLS-1$

   private SelectionListener          _columnSortListener;
   private IPartListener2             _partListener;
   private ISelectionListener         _postSelectionListener;
   private ITourEventListener         _tourEventListener;

   private ContainerCheckedTreeViewer _tagViewer;
   private IContextMenuProvider       _tagViewerContextMenuProvider             = new TreeContextMenuProvider();
   private TagViewerComparator        _tagViewerComparator                      = new TagViewerComparator();
   private TagFilter                  _tagFilter                                = new TagFilter();
   private ColumnManager              _columnManager;
   private TVIPrefTagRoot             _rootItem;

   private int                        _lastSelectionHash;
   private int                        _hash_AllTourData;
   private HashSet<Long>              _allCheckedTagIds                         = new HashSet<>();
   private ArrayList<TourData>        _allSelectedTours                         = new ArrayList<>();
   private ArrayList<TourData>        _allTaggedTours                           = new ArrayList<>();

   private boolean                    _tagViewerItem_IsChecked;
   private boolean                    _tagViewerItem_IsKeyPressed;
   private Object                     _tagViewerItem_Data;

   private boolean                    _isBehaviourSingleExpandedOthersCollapse  = true;
   private boolean                    _isBehaviourAutoExpandCollapse            = true;
   private boolean                    _isExpandingSelection;
   private boolean                    _isHierarchicalLayout;
   private boolean                    _isInCollapseAll;
   private boolean                    _isInUIUpdate;
   private boolean                    _isShowOnlyCheckedTags;
   private boolean                    _isTagDirty;

//   private OpenDialogManager                  _openDlgMgr                               = new OpenDialogManager();

   private long                                _expandRunnableCounter;

   private Action_CollapseAll_WithoutSelection _actionCollapseAll;
   private ActionExpandAll                     _actionExpandAll;
   private Action_SingleExpand_CollapseOthers  _actionSingleExpandCollapseOthers;
   private ActionOpenPrefDialog                _action_PrefDialog;
   private ActionTagLayout                     _actionTagLayout;
   private ActionTagFilter                     _actionTagFilter;
//   private Action_TourTag_Options             _actionTourTagOptions;

   private PixelConverter _pc;
   private MenuManager    _viewerMenuManager;

   /*
    * Image resources
    */
   private Image _imgTag         = TourbookPlugin.getImageDescriptor(Messages.Image__tag).createImage();
   private Image _imgTagRoot     = TourbookPlugin.getImageDescriptor(Messages.Image__tag_root).createImage();
   private Image _imgTagCategory = TourbookPlugin.getImageDescriptor(Messages.Image__tag_category).createImage();

   /*
    * UI controls
    */
   private Composite _parent;
   private Composite _viewerContainer;

   private Menu      _treeContextMenu;

   private Label     _lblHeader;

   private class Action_CollapseAll_WithoutSelection extends ActionCollapseAll {

      public Action_CollapseAll_WithoutSelection(final ITreeViewer treeViewerProvider) {
         super(treeViewerProvider);
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

//   private class Action_TourTag_Options extends ActionToolbarSlideout {
//
//      private Slideout_TourTag_Options __slideoutTourTagOptions;
//
//      @Override
//      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {
//
//         __slideoutTourTagOptions = new Slideout_TourTag_Options(_parent, toolbar, TourTags_View.this, _state);
//
//         return __slideoutTourTagOptions;
//      }
//
//      @Override
//      protected void onBeforeOpenSlideout() {
//         closeOpenedDialogs(this);
//      }
//   }

   private class Action_SingleExpand_CollapseOthers extends Action {

      public Action_SingleExpand_CollapseOthers() {
         super(Messages.Tour_Tags_Action_SingleExpand_CollapseOthers, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         onAction_SingleExpandCollapseOthers();
      }
   }

   public class ActionTagFilter extends Action {

      public ActionTagFilter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagsChecked));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagsChecked_Disabled));
      }

      @Override
      public void run() {
         onAction_TagFilter();
      }
   }

   private class ActionTagLayout extends Action {

      ActionTagLayout() {

         super(Messages.action_tagView_flat_layout, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Flat));
      }

      @Override
      public void run() {
         onAction_TagLayout();
      }
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    * <br>
    * The comparator is necessary to set and restore the expanded elements <br>
    * <br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    */
   class TagComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {

            return true;

         } else if (a instanceof TVIPrefTag && b instanceof TVIPrefTag) {

            final TVIPrefTag tviTag1 = (TVIPrefTag) a;
            final TVIPrefTag tviTag2 = (TVIPrefTag) b;

            return tviTag1.getTourTag().getTagId() == tviTag2.getTourTag().getTagId();

         } else if (a instanceof TVIPrefTagCategory && b instanceof TVIPrefTagCategory) {

            final TVIPrefTagCategory tviTagCat1 = (TVIPrefTagCategory) a;
            final TVIPrefTagCategory tviTagCat2 = (TVIPrefTagCategory) b;

            return tviTagCat1.getTourTagCategory().getCategoryId() == tviTagCat2.getTourTagCategory().getCategoryId();
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }
   }

   public class TagFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (element instanceof TVIPrefTag) {

            final TVIPrefTag tviTag = (TVIPrefTag) element;

            final TourTag tourTag = tviTag.getTourTag();
            final long tagId = tourTag.getTagId();

            if (_allCheckedTagIds.contains(tagId)) {
               return true;
            }
         }

         return false;
      }
   }

   /**
    * Sort the tags and categories
    */
   private final class TagViewerComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_TAGS;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTag) {

            // sort tags by name

            final TourTag tourTag1 = ((TVIPrefTag) (obj1)).getTourTag();
            final TourTag tourTag2 = ((TVIPrefTag) (obj2)).getTourTag();

//            return tourTag1.getTagName().compareTo(tourTag2.getTagName());

            double rc = 0;

            // Determine which column and do the appropriate sort
            switch (__sortColumnId) {

            case COLUMN_NOTES:
               rc = tourTag1.getNotes().compareTo(tourTag2.getNotes());
               break;

            case COLUMN_ID:
               rc = tourTag1.getTagId() - tourTag2.getTagId();
               break;

            case COLUMN_TAGS:
            default:
               rc = tourTag1.getTagName().compareTo(tourTag2.getTagName());
            }

            if (rc == 0) {

               // subsort by map provider

               rc = tourTag1.getTagName().compareTo(tourTag2.getTagName());
            }

            // if descending order, flip the direction
            if (__sortDirection == DESCENDING) {
               rc = -rc;
            }

            /*
             * MUST return 1 or -1 otherwise long values are not sorted correctly.
             */
            return rc > 0 //
                  ? 1
                  : rc < 0 //
                        ? -1
                        : 0;

         } else if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTagCategory) {

            // sort category before tag

            return 1;

         } else if (obj2 instanceof TVIPrefTag && obj1 instanceof TVIPrefTagCategory) {

            // sort category before tag

            return -1;

         } else if (obj1 instanceof TVIPrefTagCategory && obj2 instanceof TVIPrefTagCategory) {

            // sort categories by name

            final TourTagCategory tourTagCat1 = ((TVIPrefTagCategory) (obj1)).getTourTagCategory();
            final TourTagCategory tourTagCat2 = ((TVIPrefTagCategory) (obj2)).getTourTagCategory();

            return tourTagCat1.getCategoryName().compareTo(tourTagCat2.getCategoryName());
         }

         return 0;
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {
         // sort when the name has changed
         return true;
      }

      void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort -> select next sorting

            switch (__sortDirection) {
            case ASCENDING:
               __sortDirection = DESCENDING;
               break;

            case DESCENDING:
            default:
               __sortDirection = ASCENDING;
               break;
            }

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }
   }

   private final class TagViewerContentProvider implements ITreeContentProvider {

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

         _treeContextMenu = createUI_24_CreateViewerContextMenu();

         return _treeContextMenu;
      }

   }

   public TourTags_View() {}

   private void addPartListener() {

      // set the part listener
      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourTags_View.this) {

               // update save icon
               final ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
               commandService.refreshElements(AppCommands.COMMAND_NET_TOURBOOK_TOUR_SAVE_TOUR, null);
            }
         }

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
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };

      // register the listener in the page
      getSite().getPage().addPartListener(_partListener);
   }

   /**
    * Listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

            if (part == TourTags_View.this) {
               return;
            }

            onSelectionChanged(selection);
         }
      };
      getSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourTags_View.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();

            } else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

               _hash_AllTourData = Integer.MIN_VALUE;

               recreateViewer(_tagViewer);
               updateUI_Tags_FromTourData(_allSelectedTours, true);

            } else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  // update modified tour

                  updateUI_Tags_FromTourData(modifiedTours, true);
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      _tagViewer.setCheckedElements(new Object[] {});

      _allSelectedTours.clear();
      _allTaggedTours.clear();
      _allCheckedTagIds.clear();

      updateUI_NumberOfTours();
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
//   private void closeOpenedDialogs(final IOpeningDialog openingDialog) {
//      _openDlgMgr.closeOpenedDialogs(openingDialog);
//   }

   private void createActions() {

      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new Action_CollapseAll_WithoutSelection(this);
      _action_PrefDialog = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
      _actionTagLayout = new ActionTagLayout();
      _actionTagFilter = new ActionTagFilter();
//      _actionTourTagOptions = new Action_TourTag_Options();
      _actionSingleExpandCollapseOthers = new Action_SingleExpand_CollapseOthers();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
            fillContextMenu(manager);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      //Listener registration. This is very important for enabling and disabling the tool bar level buttons
//      addListenerObject(this);

      initUI(parent);
      createMenuManager();
      restoreState_BeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      createActions();
      fillToolbar();

      addPartListener();
      addSelectionListener();
      addTourEventListener();

      // load tag viewer
      reloadViewer();

      restoreState();
      enableControls();

      updateUI_NumberOfTours();

      restoreSelection();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .spacing(0, 0)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_10_Header(container);

         _viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_TagViewer(_viewerContainer);
         }
      }
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
      {
         _lblHeader = new Label(container, SWT.NONE);
         _lblHeader.setText(UI.SPACE1);
         GridDataFactory.fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .grab(true, true)
               .applyTo(_lblHeader);
      }
   }

   private void createUI_20_TagViewer(final Composite parent) {

      /*
       * Create tree
       */
      final Tree tree = new Tree(parent,

            SWT.CHECK
                  | SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(true);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

      tree.addSelectionListener(new SelectionAdapter() {

         @Override
         public void widgetSelected(final SelectionEvent event) {

            /*
             * The tag treeviewer selection event can have another selection !!!
             */

            _tagViewerItem_IsChecked = event.detail == SWT.CHECK;

            if (_tagViewerItem_IsChecked) {

               /*
                * Item can be null when <ctrl>+A is pressed !!!
                */
               final Widget item = event.item;

               _tagViewerItem_Data = item.getData();
            }
         }
      });

      tree.addKeyListener(new KeyAdapter() {

         @Override
         public void keyPressed(final KeyEvent e) {
            _tagViewerItem_IsKeyPressed = true;
         }
      });

      /*
       * Create tag viewer
       */
      _tagViewer = new ContainerCheckedTreeViewer(tree);
      _columnManager.createColumns(_tagViewer);

      _tagViewer.setContentProvider(new TagViewerContentProvider());
      _tagViewer.setComparer(new TagComparer());
      _tagViewer.setComparator(_tagViewerComparator);
      _tagViewer.setUseHashlookup(true);

      _tagViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
//            onTagViewer_DoubleClick();
         }
      });

      /**
       * This check state provider is necessary to fix
       * <p>
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=170521
       * <p>
       * Refreshing ContainerCheckedTreeViewer does not restore checked/grayed states correctly.
       */
      _tagViewer.setCheckStateProvider(new ICheckStateProvider() {

         @Override
         public boolean isChecked(final Object element) {

            if (isGrayed(element)) {
               return true;
            }

            return false;
         }

         @Override
         public boolean isGrayed(final Object element) {
            return false;
         }
      });

      _tagViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onTag_Select(event);
         }
      });

      createUI_22_ContextMenu();
   }

   /**
    * Setup the viewer context menu
    */
   private void createUI_22_ContextMenu() {

      _treeContextMenu = createUI_24_CreateViewerContextMenu();

      final Tree tree = (Tree) _tagViewer.getControl();

      _columnManager.createHeaderContextMenu(tree, _tagViewerContextMenuProvider);
   }

   /**
    * Create the viewer context menu
    *
    * @return
    */
   private Menu createUI_24_CreateViewerContextMenu() {

      final Tree tree = _tagViewer.getTree();

      // add the context menu to the tree viewer

      final Menu treeContextMenu = _viewerMenuManager.createContextMenu(tree);
      treeContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
//            _tagMenuManager.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {
//            _tagMenuManager.onShowMenu(menuEvent, tree, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
         }
      });

      return treeContextMenu;
   }

   private void defineAllColumns() {

      defineColumn_10_Tags();
      defineColumn_20_Notes();
      defineColumn_99_ID();
   }

   private void defineColumn_10_Tags() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, COLUMN_TAGS, SWT.LEAD);

      colDef.setColumnLabel(Messages.Pref_TourTag_Column_TagsAndCategories);
      colDef.setColumnHeaderText(Messages.Pref_TourTag_Column_TagsAndCategories);
      colDef.setColumnHeaderToolTipText(Messages.Pref_TourTag_Column_TagsAndCategories);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(40));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final StyledString styledString = new StyledString();

            final Object element = cell.getElement();
            if (element instanceof TVIPrefTag) {

               final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

               styledString.append(tourTag.getTagName(), net.tourbook.ui.UI.TAG_STYLER);
               cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

//               cell.setBackground(background);

            } else if (element instanceof TVIPrefTagCategory) {

               final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
               final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

               cell.setImage(_imgTagCategory);

               styledString.append(tourTagCategory.getCategoryName(), net.tourbook.ui.UI.TAG_CATEGORY_STYLER);

               // get number of categories
               final int categoryCounter = tourTagCategory.getCategoryCounter();
               final int tagCounter = tourTagCategory.getTagCounter();
               if (categoryCounter == -1 && tagCounter == -1) {

//               styledString.append("  ...", StyledString.COUNTER_STYLER);

               } else {

                  String categoryString = UI.EMPTY_STRING;
                  if (categoryCounter > 0) {
                     categoryString = "/" + categoryCounter; //$NON-NLS-1$
                  }
                  styledString.append("   " + tagCounter + categoryString, StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
               }

            } else {
               styledString.append(element.toString());
            }

            cell.setText(styledString.getString());
            cell.setStyleRanges(styledString.getStyleRanges());
         }
      });
   }

   private void defineColumn_20_Notes() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, COLUMN_NOTES, SWT.LEAD);

      colDef.setColumnLabel(Messages.Pref_TourTag_Column_Notes);
      colDef.setColumnHeaderText(Messages.Pref_TourTag_Column_Notes);
      colDef.setColumnHeaderToolTipText(Messages.Pref_TourTag_Column_Notes);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            String notes = UI.EMPTY_STRING;

            final Object element = cell.getElement();
            if (element instanceof TVIPrefTag) {

               final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

               notes = tourTag.getNotes();

            } else if (element instanceof TVIPrefTagCategory) {

               final TourTagCategory tourTagCategory = ((TVIPrefTagCategory) element).getTourTagCategory();

               notes = tourTagCategory.getNotes();
            }

            final String shortedNotes = UI.shortenText(notes, 200, true);

            cell.setText(shortedNotes);
         }
      });
   }

   private void defineColumn_99_ID() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, COLUMN_ID, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Pref_TourTag_Column_ID);
      colDef.setColumnHeaderText(Messages.Pref_TourTag_Column_ID);
      colDef.setColumnHeaderToolTipText(Messages.Pref_TourTag_Column_ID);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVIPrefTag) {

               final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

               cell.setText(Long.toString(tourTag.getTagId()));

            } else if (element instanceof TVIPrefTagCategory) {

               final TourTagCategory tourTagCategory = ((TVIPrefTagCategory) element).getTourTagCategory();

               cell.setText(Long.toString(tourTagCategory.getCategoryId()));

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   @Override
   public void dispose() {

      final IWorkbenchPage page = getSite().getPage();

      page.removePartListener(_partListener);
      page.removePostSelectionListener(_postSelectionListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();

      super.dispose();
   }

   @Override
   public void doRestore() {

      _isTagDirty = false;

      enableControls();

      firePropertyChange(PROP_DIRTY);
   }

   @Override
   public void doSave(final IProgressMonitor monitor) {

      _isTagDirty = false;

      enableControls();

      firePropertyChange(PROP_DIRTY);
   }

   @Override
   public void doSaveAs() {}

   private void enableControls() {

      final boolean isTourAvailable = _allSelectedTours.size() > 0;

      _actionCollapseAll.setEnabled(isTourAvailable && _isHierarchicalLayout);
      _actionExpandAll.setEnabled(isTourAvailable && _isHierarchicalLayout);

      _actionTagFilter.setEnabled(isTourAvailable);
      _actionTagLayout.setEnabled(isTourAvailable);

//      _actionUndoChanges.setEnabled(isTourAvailable && _isTagDirty);

      _tagViewer.getTree().setEnabled(isTourAvailable);
   }

   private void expandCollapseFolder(final TVIPrefTagCategory treeItem) {

      if (_tagViewer.getExpandedState(treeItem)) {

         // collapse category

         _tagViewer.collapseToLevel(treeItem, 1);

      } else {

         // expand category

         _tagViewer.expandToLevel(treeItem, 1);
      }
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(new Separator());
      menuMgr.add(_actionSingleExpandCollapseOthers);
   }

   private void fillToolbar() {

      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = actionBars.getToolBarManager();

      tbm.add(_actionTagFilter);
      tbm.add(_actionTagLayout);
      tbm.add(_actionExpandAll);
      tbm.add(_actionCollapseAll);
      tbm.add(_action_PrefDialog);
//      tbm.add(_actionTourTagOptions);

      /*
       * fill toolbar view menu
       */
//      final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
//
//      menuMgr.add(_actionUndoChanges);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TreeColumn getSortColumn(final String sortColumnId) {

      final TreeColumn[] allColumns = _tagViewer.getTree().getColumns();

      for (final TreeColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   /**
    * Traverses all tag viewer items until a tag items is found Recursive !
    *
    * @param parentItems
    * @param tagItems
    * @param tagId
    * @return Returns <code>true</code> when the tag id is found
    */
   private boolean getTagItems(final ArrayList<TreeViewerItem> parentItems,
                               final ArrayList<TVIPrefTag> tagItems,
                               final long tagId) {

      for (final TreeViewerItem tvItem : parentItems) {

         if (tvItem instanceof TVIPrefTagCategory) {

            final TVIPrefTagCategory tagCategory = (TVIPrefTagCategory) tvItem;
            final ArrayList<TreeViewerItem> tagCategoryChildren = tagCategory.getFetchedChildren();

            if (tagCategoryChildren.size() > 0) {

               final boolean isTagFound = getTagItems(tagCategoryChildren, tagItems, tagId);

               if (isTagFound) {
                  return true;
               }
            }

         } else if (tvItem instanceof TVIPrefTag) {

            final TVIPrefTag tagItem = (TVIPrefTag) tvItem;

            if (tagId == tagItem.getTourTag().getTagId()) {

               tagItems.add(tagItem);

               return true;
            }
         }
      }

      return false;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tagViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tagViewer;
   }

   private void initUI(final Composite parent) {

      _parent = parent;
      _pc = new PixelConverter(parent);

      _columnSortListener = widgetSelectedAdapter(e -> onSelect_SortColumn(e));
   }

   @Override
   public boolean isDirty() {

      return _isTagDirty;
   }

   @Override
   public boolean isSaveAsAllowed() {
      return false;
   }

   @Override
   public boolean isSaveOnCloseNeeded() {
      return isDirty();
   }

   /**
    * Load all tag items that the categories do show the number of items
    */
   private void loadAllTagItems() {

      final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

      final Set<Long> tagIds = allTourTags.keySet();

      final ArrayList<TVIPrefTag> tagItems = new ArrayList<>(tagIds.size());

      if (tagIds.size() > 0) {

         // get all tag viewer items which should be checked

         final ArrayList<TreeViewerItem> rootItems = _rootItem.getFetchedChildren();

         for (final long tagId : tagIds) {

            // Is recursive !!!
            getTagItems(rootItems, tagItems, tagId);
         }
      }
   }

   private void onAction_SingleExpandCollapseOthers() {

      _isBehaviourSingleExpandedOthersCollapse = _actionSingleExpandCollapseOthers.isChecked();
   }

   private void onAction_TagFilter() {

      // toggle tag filter
      _isShowOnlyCheckedTags = !_isShowOnlyCheckedTags;

      _parent.setRedraw(false);
      {
         if (_isShowOnlyCheckedTags && _isHierarchicalLayout) {

            // tag viewer must not display a tree

            onAction_TagLayout();
            updateUI_TagFilter();

         } else {

            updateUI_TagFilter();

            enableControls();
         }
      }
      _parent.setRedraw(true);
   }

   private void onAction_TagLayout() {

      // toggle layout
      _isHierarchicalLayout = !_isHierarchicalLayout;

      _parent.setRedraw(false);
      {
         if (_isHierarchicalLayout && _isShowOnlyCheckedTags) {

            // the tree cannot show only checked tags -> show all tags in the tree but with ckecked tags

            _isShowOnlyCheckedTags = false;
            _actionTagFilter.setChecked(false);

            updateUI_TagFilter();
         }

         updateUI_TagLayoutAction();

         recreateViewer(_tagViewer);

         _parent.getDisplay().asyncExec(() -> {

            // tags in the tree hierarchie must be rechecked otherwise they are not checked
            updateUI_Tags_FromTourData(_allTaggedTours, true);
         });

         enableControls();
      }
      _parent.setRedraw(true);
   }


   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
//         final ISelection selectionBackup = getViewerSelection();

         // toggle sorting
         _tagViewerComparator.setSortColumn(e.widget);
         _tagViewer.refresh();

         // reselect selection
//         _tagViewer.setSelection(selectionBackup, true);
//         _tagViewer.getTable().showSelection();
      }
      _viewerContainer.setRedraw(true);
   }

   private void onSelectionChanged(final ISelection selection) {

      if (selection == null) {
         return;
      }

      final int selectionHash = selection.hashCode();
      if (_lastSelectionHash == selectionHash) {

         /*
          * Last selection has not changed, this can occure when the app lost the focus and got the
          * focus again.
          */
         return;
      }

      _lastSelectionHash = selectionHash;

      if (selection instanceof SelectionTourData) {

         final SelectionTourData tourDataSelection = (SelectionTourData) selection;

         final TourData tourData = tourDataSelection.getTourData();
         if (tourData != null) {
            updateUI_Tags_FromTourData(tourData);
         }

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId selectionTourId = (SelectionTourId) selection;
         final Long tourId = selectionTourId.getTourId();

         final TourData tourData = TourManager.getInstance().getTourData(tourId);

         updateUI_Tags_FromTourData(tourData);

      } else if (selection instanceof SelectionTourIds) {

         final ArrayList<Long> allTourIds = ((SelectionTourIds) selection).getTourIds();
         if (allTourIds != null) {

            final ArrayList<TourData> allTourData = new ArrayList<>();

            TourManager.loadTourData(allTourIds, allTourData, false);

            updateUI_Tags_FromTourData(allTourData, false);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();
      }
   }

   private void onTag_Select(final SelectionChangedEvent event) {

      if (_isInUIUpdate) {
         return;
      }

      if (_tagViewerItem_IsKeyPressed) {

         // ignore when selected with keyboard

         // reset state
         _tagViewerItem_IsKeyPressed = false;

         return;
      }

      Object selection;

      if (_tagViewerItem_IsChecked) {

         // a checkbox is checked

         setTagsDirty();

         selection = _tagViewerItem_Data;

      } else {

         selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
      }

      if (selection instanceof TVIPrefTag) {

         // tag is selected

         final TVIPrefTag tviTag = (TVIPrefTag) selection;

         // toggle tag
         if (_tagViewerItem_IsChecked == false) {

            // tag is selected and NOT the checkbox !!!

            setTagsDirty();

            // get new check state
            final boolean isChecked = !_tagViewer.getChecked(tviTag);

            // update model
            final long tagId = tviTag.getTourTag().getTagId();
            if (isChecked) {
               _allCheckedTagIds.add(tagId);
            } else {
               _allCheckedTagIds.remove(tagId);
            }

            // update UI
            _tagViewer.setChecked(tviTag, isChecked);
         }

      } else if (selection instanceof TVIPrefTagCategory) {

         // expand/collapse current item

         if (_tagViewerItem_IsChecked == false) {

            // category is selected and NOT the checkbox !!!

            final TreeSelection treeSelection = (TreeSelection) event.getSelection();

            onTag_SelectCategory(treeSelection);
         }
      }
   }

   private void onTag_SelectCategory(final TreeSelection treeSelection) {

      if (_isExpandingSelection) {

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

      final TVIPrefTagCategory tviFolder = (TVIPrefTagCategory) selectedTreePath.getLastSegment();

      onTag_SelectCategory_10_AutoExpandCollapse(treeSelection, selectedTreePath, tviFolder);
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded childrens are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    * @param selectedTreePath
    * @param tviFolder
    */
   private void onTag_SelectCategory_10_AutoExpandCollapse(final ITreeSelection treeSelection,
                                                           final TreePath selectedTreePath,
                                                           final TVIPrefTagCategory tviFolder) {

      if (_isInCollapseAll) {

         // prevent auto expand
         return;
      }

      if (_isBehaviourSingleExpandedOthersCollapse) {

         /*
          * run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         Display.getCurrent().asyncExec(new Runnable() {

            private long               __expandRunnableCounter = ++_expandRunnableCounter;

            private TVIPrefTagCategory __selectedFolderItem    = tviFolder;
            private ITreeSelection     __treeSelection         = treeSelection;
            private TreePath           __selectedTreePath      = selectedTreePath;

            @Override
            public void run() {

               // check if a newer expand event occured
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               onTag_SelectCategory_20_AutoExpandCollapse_Runnable(
                     __selectedFolderItem,
                     __treeSelection,
                     __selectedTreePath);
            }
         });

      } else {

         if (_isBehaviourAutoExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard
            expandCollapseFolder(tviFolder);
         }
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param selectedFolderItem
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onTag_SelectCategory_20_AutoExpandCollapse_Runnable(final TVIPrefTagCategory selectedFolderItem,
                                                                    final ITreeSelection treeSelection,
                                                                    final TreePath selectedTreePath) {
      _isExpandingSelection = true;
      {
         final Tree tree = _tagViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _tagViewer.getExpandedState(selectedTreePath);

            /*
             * collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _tagViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _tagViewer.setExpandedState(treePath, false);
            }

            /*
             * expand and select selected folder
             */
            _tagViewer.setExpandedTreePaths(new TreePath[] { selectedTreePath });
            _tagViewer.setSelection(treeSelection, true);

            if (_isBehaviourAutoExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _tagViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * set top item to the previous top item, otherwise the expanded/collapse item is
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
      _isExpandingSelection = false;
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         final Object[] expandedElements = _tagViewer.getExpandedElements();
         final Object[] checkedElements = _tagViewer.getCheckedElements();
         final ISelection selection = _tagViewer.getSelection();

         /*
          * Exclude categories as it would check ALL children
          */
         final ArrayList<TVIPrefTag> allTags = new ArrayList<>();
         for (final Object object : checkedElements) {

            if (object instanceof TVIPrefTag) {
               allTags.add((TVIPrefTag) object);
            }
         }

         _tagViewer.getTree().dispose();

         createUI_20_TagViewer(_viewerContainer);
         _viewerContainer.layout();

         reloadViewer_SetContent();

         _tagViewer.setExpandedElements(expandedElements);
         _tagViewer.setCheckedElements(allTags.toArray());

         _isInUIUpdate = true;
         {
            _tagViewer.setSelection(selection);
         }
         _isInUIUpdate = false;
      }
      _viewerContainer.setRedraw(true);

      return _tagViewer;
   }

   /**
    * Reload the content of the tag viewer
    */
   @Override
   public void reloadViewer() {

      final Tree tree = _tagViewer.getTree();
      tree.setRedraw(false);
      {
         final Object[] expandedElements = _tagViewer.getExpandedElements();

         reloadViewer_SetContent();

         _tagViewer.setExpandedElements(expandedElements);
      }
      tree.setRedraw(true);
   }

   private void reloadViewer_SetContent() {

      _rootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
      _tagViewer.setInput(this);

      loadAllTagItems();
   }

   private void restoreSelection() {

      // try to use selection from selection service
      onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

      if (_allSelectedTours.size() == 0) {

         // a tour is not displayed, find a tour provider which provides a tour
         Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {

               // validate widget
               if (_parent.isDisposed()) {
                  return;
               }

               /*
                * check if tour was set from a selection provider
                */
               if (_allSelectedTours.size() > 0) {
                  return;
               }

               final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
               if (selectedTours != null && selectedTours.size() > 0) {
                  updateUI_Tags_FromTourData(selectedTours, false);
               }
            }
         });
      }
   }

   private void restoreState() {

      _actionTagFilter.setChecked(_isShowOnlyCheckedTags);

      _isBehaviourSingleExpandedOthersCollapse = Util.getStateBoolean(
            _state,
            STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS,
            false);
      _actionSingleExpandCollapseOthers.setChecked(_isBehaviourSingleExpandedOthersCollapse);

      updateUI_TagLayoutAction();
      updateUI_TagFilter();
   }

   private void restoreState_BeforeUI() {

      _isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
      _isShowOnlyCheckedTags = Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, false);

      // update sorting comparator
      final String sortColumnId = Util.getStateString(_state, STATE_SORT_COLUMN_ID, COLUMN_TAGS);
      final int sortDirection = Util.getStateInt(_state, STATE_SORT_COLUMN_DIRECTION, TagViewerComparator.ASCENDING);

      _tagViewerComparator.__sortColumnId = sortColumnId;
      _tagViewerComparator.__sortDirection = sortDirection;
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);
      _state.put(STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, _isShowOnlyCheckedTags);

      _state.put(STATE_SORT_COLUMN_ID, _tagViewerComparator.__sortColumnId);
      _state.put(STATE_SORT_COLUMN_DIRECTION, _tagViewerComparator.__sortDirection);

      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpandCollapseOthers.isChecked());
   }

   @Override
   public void setFocus() {

      _tagViewer.getTree().setFocus();
   }

   private void setTagsDirty() {

      _isTagDirty = true;

      enableControls();

      firePropertyChange(PROP_DIRTY);
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Update view header which shows the number of selected tour(s) and tags.
    */
   private void updateUI_NumberOfTours() {

      final String headerText;

      final int numSelectedTours = _allSelectedTours.size();
      final int numTags = _allCheckedTagIds.size();

      if (numSelectedTours == 0) {

         // a tour is not selected

         headerText = Messages.UI_Label_no_chart_is_selected;

      } else if (numSelectedTours == 1) {

         // 1 tour is selected

         headerText = NLS.bind(Messages.Tour_Tags_Title_OneTour,
               TourManager.getTourTitle(_allSelectedTours.get(0)),
               numTags);

      } else {

         // multiple tours are selected

         final int numTaggedTours = _allTaggedTours.size();

         headerText = NLS.bind(Messages.Tour_Tags_Title_MultipleTours,
               new Object[] {
                     numSelectedTours,
                     numTaggedTours,
                     numTags
               });
      }

      _lblHeader.setText(headerText);
      _lblHeader.setToolTipText(headerText);
      _lblHeader.getParent().layout();

   }

   /**
    * Set the sort column direction indicator for a column
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final int direction =
            sortDirection == TagViewerComparator.ASCENDING ? SWT.UP
                  : sortDirection == TagViewerComparator.DESCENDING ? SWT.DOWN
                        : SWT.NONE;

      final Tree tree = _tagViewer.getTree();
      final TreeColumn tc = getSortColumn(sortColumnId);

      tree.setSortColumn(tc);
      tree.setSortDirection(direction);
   }

   /**
    * Set tag viewer filter which will refilter it
    */
   private void updateUI_TagFilter() {

      if (_isShowOnlyCheckedTags) {

         _actionTagFilter.setToolTipText(Messages.Tour_Tags_Action_TagCheckFilter_AllTags_Tooltip);

         _tagViewer.addFilter(_tagFilter);

      } else {

         _actionTagFilter.setToolTipText(Messages.Tour_Tags_Action_TagCheckFilter_OnlyTaggedTours_Tooltip);

         _tagViewer.removeFilter(_tagFilter);
      }
   }

   private void updateUI_TagLayoutAction() {

      if (_isHierarchicalLayout) {

         // hierarchy is displayed -> show icon/tooltip for flat view

         _actionTagLayout.setToolTipText(Messages.Tour_Tags_Action_Layout_Flat_Tooltip);
         _actionTagLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Flat));
         _actionTagLayout.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Flat_Disabled));

      } else {

         // flat view is displayed -> show icon/tooltip for hierarchy view

         _actionTagLayout.setToolTipText(Messages.Tour_Tags_Action_Layout_Hierarchical_Tooltip);
         _actionTagLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Hierarchical));
         _actionTagLayout.setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Hierarchical_Disabled));
      }
   }

   /**
    * First update internal lists of the selected tours and available tags and then update the UI.
    *
    * @param allTourData
    * @param isForceUpdate
    *           Ignore previous displayed tags and force a UI update
    */
   private void updateUI_Tags_FromTourData(final ArrayList<TourData> allTourData, final boolean isForceUpdate) {

      final int allTourData_Hash = allTourData.hashCode();

      // check if tags are already checked with the previous tour data
      if (isForceUpdate == false && allTourData_Hash == _hash_AllTourData) {
         return;
      }

      _hash_AllTourData = allTourData_Hash;

      _allSelectedTours.clear();
      _allSelectedTours.addAll(allTourData);

      /*
       * Get all tag id's
       */
      _allCheckedTagIds.clear();
      _allTaggedTours.clear();

      for (final TourData tourData : _allSelectedTours) {

         final Set<TourTag> allTourTags = tourData.getTourTags();

         if (allTourTags != null && allTourTags.size() > 0) {

            _allTaggedTours.add(tourData);

            for (final TourTag tourTag : allTourTags) {
               _allCheckedTagIds.add(tourTag.getTagId());
            }
         }
      }

      /*
       * Get all tag tree items which should be checked
       */
      final ArrayList<TVIPrefTag> tagItems = new ArrayList<>(_allCheckedTagIds.size());

      if (_allCheckedTagIds.size() > 0) {

         final ArrayList<TreeViewerItem> rootItems = _rootItem.getFetchedChildren();

         for (final long tagId : _allCheckedTagIds) {

            // recursive !!!
            getTagItems(rootItems, tagItems, tagId);
         }
      }

      updateUI_NumberOfTours();

      _tagViewer.refresh();

      // update UI
      _tagViewer.setCheckedElements(tagItems.toArray());

      enableControls();
   }

   private void updateUI_Tags_FromTourData(final TourData tourData) {

      final ArrayList<TourData> allTourData = new ArrayList<>();
      allTourData.add(tourData);

      updateUI_Tags_FromTourData(allTourData, false);
   }
}
