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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
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
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;

public class TourTags_View extends ViewPart implements ITreeViewer, ITourViewer {

   static public final String                ID                                       = "net.tourbook.ui.views.tagging.TourTags_View"; //$NON-NLS-1$

   private static final String               STATE_IS_HIERARCHICAL_LAYOUT             = "STATE_IS_HIERARCHICAL_LAYOUT";                //$NON-NLS-1$

   private final IDialogSettings             _state                                   = TourbookPlugin.getState("TourTagsView");       //$NON-NLS-1$

   private ContainerCheckedTreeViewer        _tagViewer;
   private ColumnManager                     _columnManager;
   private IContextMenuProvider              _viewerContextMenuProvider               = new TreeContextMenuProvider();
   private TVIPrefTagRoot                    _rootItem;

   private boolean                           _tagViewerItem_IsChecked;
   private boolean                           _tagViewerItem_IsKeyPressed;
   private Object                            _tagViewerItem_Data;

   private boolean                           _isBehaviourSingleExpandedOthersCollapse = true;
   private boolean                           _isBehaviourAutoExpandCollapse           = true;
   private boolean                           _isExpandingSelection;
   private boolean                           _isHierarchicalLayout;
   private boolean                           _isInCollapseAll;
   private boolean                           _isInUIUpdate;

   private OpenDialogManager                 _openDlgMgr                              = new OpenDialogManager();

   private long                              _expandRunnableCounter;

   private ArrayList<TourData>               _selectedTours;

   private ActionCollapseAllWithoutSelection _actionCollapseAll;
   private ActionExpandAll                   _actionExpandAll;
   private ActionSaveTour                    _actionSaveTour;
   private ActionTagLayout                   _actionTagLayout;
   private Action_TourChart_Options          _actionTourTagOptions;

   private PixelConverter                    _pc;

   /*
    * Image resources
    */
   private Image _imgTag         = TourbookPlugin.getImageDescriptor(Messages.Image__tag).createImage();
   private Image _imgTagRoot     = TourbookPlugin.getImageDescriptor(Messages.Image__tag_root).createImage();
   private Image _imgTagCategory = TourbookPlugin.getImageDescriptor(Messages.Image__tag_category).createImage();

   /*
    * UI controls
    */
   private Composite   _parent;
   private Composite   _viewerContainer;

   private Menu        _treeContextMenu;
   private MenuManager _viewerMenuManager;

   private class Action_TourChart_Options extends ActionToolbarSlideout {

      @Override
      protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

         return new SlideoutTourTagOptions(_parent, toolbar);
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private class ActionCollapseAllWithoutSelection extends ActionCollapseAll {

      public ActionCollapseAllWithoutSelection(final ITreeViewer treeViewerProvider) {
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

   private class ActionSaveTour extends Action {

      public ActionSaveTour() {

         super(null, AS_PUSH_BUTTON);

         setToolTipText(Messages.app_action_save_tour_tooltip);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__save_disabled));

         setEnabled(false);
      }

      @Override
      public void run() {
         action_SaveTour();
      }
   }

   private class ActionTagLayout extends Action {

      ActionTagLayout() {

         super(Messages.action_tagView_flat_layout, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Flat));
      }

      @Override
      public void run() {
         onAction_Layout();
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

   /**
    * Sort the tags and categories
    */
   private final static class TagViewerComparator extends ViewerComparator {
      @Override
      public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

         if (obj1 instanceof TVIPrefTag && obj2 instanceof TVIPrefTag) {

            // sort tags by name
            final TourTag tourTag1 = ((TVIPrefTag) (obj1)).getTourTag();
            final TourTag tourTag2 = ((TVIPrefTag) (obj2)).getTourTag();

            return tourTag1.getTagName().compareTo(tourTag2.getTagName());

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
   }

   private final class TagViewerContentProvicer implements ITreeContentProvider {

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

   private void action_SaveTour() {
      // TODO Auto-generated method stub

   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   private void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDlgMgr.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

      _actionSaveTour = new ActionSaveTour();
      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
      _actionTagLayout = new ActionTagLayout();
      _actionTourTagOptions = new Action_TourChart_Options();
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager("#PopupMenu"); //$NON-NLS-1$
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
//            fillContextMenu(manager);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);
      createMenuManager();
      restoreStateBeforeUI();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      createUI(parent);

      createActions();
      fillToolbar();

      // load tag viewer
      reloadViewer();

      restoreState();
      enableControls();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults()
//            .grab(true, true)
//            .hint(400, 600)
//            .applyTo(container);
      GridLayoutFactory
            .fillDefaults()
            .spacing(0, 0)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
//         createUI_10_AllTags_Header(container);

         _viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
         GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
         {
            createUI_20_TagViewer(_viewerContainer);
         }
      }
   }

   private void createUI_10_AllTags_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

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

      _tagViewer.setContentProvider(new TagViewerContentProvicer());
      _tagViewer.setComparer(new TagComparer());
      _tagViewer.setComparator(new TagViewerComparator());
      _tagViewer.setUseHashlookup(true);

      _tagViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
//            onTagViewer_DoubleClick();
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

      _columnManager.createHeaderContextMenu(tree, _viewerContextMenuProvider);
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
   }

   private void defineColumn_10_Tags() {

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "tags", SWT.LEAD);

      colDef.setColumnLabel(Messages.Pref_TourTag_Column_TagsAndCategories);
      colDef.setColumnHeaderText(Messages.Pref_TourTag_Column_TagsAndCategories);
      colDef.setColumnHeaderToolTipText(Messages.Pref_TourTag_Column_TagsAndCategories);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(40));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);
//      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final StyledString styledString = new StyledString();

            final Object element = cell.getElement();
            if (element instanceof TVIPrefTag) {

               final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

               styledString.append(tourTag.getTagName(), net.tourbook.ui.UI.TAG_STYLER);
               cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

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

      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "notes", SWT.LEAD);

      colDef.setColumnLabel(Messages.Pref_TourTag_Column_Notes);
      colDef.setColumnHeaderText(Messages.Pref_TourTag_Column_Notes);
      colDef.setColumnHeaderToolTipText(Messages.Pref_TourTag_Column_Notes);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setIsDefaultColumn();
//      colDef.setColumnSelectionListener(_columnSortListener);

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

   @Override
   public void dispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();
   }

   private void enableControls() {

      _actionCollapseAll.setEnabled(_isHierarchicalLayout);
      _actionExpandAll.setEnabled(_isHierarchicalLayout);
   }

   private void expandCollapseFolder(final TVIPrefTagCategory treeItem) {

      if (_tagViewer.getExpandedState(treeItem)) {

         // collapse folder

         _tagViewer.collapseToLevel(treeItem, 1);
      }
   }

   private void fillToolbar() {

      final IActionBars actionBars = getViewSite().getActionBars();

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = actionBars.getToolBarManager();

      tbm.add(_actionSaveTour);
      tbm.add(_actionTagLayout);
      tbm.add(_actionExpandAll);
      tbm.add(_actionCollapseAll);
      tbm.add(_actionTourTagOptions);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
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

   private void onAction_Layout() {

      // toggle layout
      _isHierarchicalLayout = !_isHierarchicalLayout;

      updateUI_ActionLayout();

      recreateViewer(_tagViewer);

      enableControls();
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

            final boolean isChecked = _tagViewer.getChecked(tviTag);

            _tagViewer.setChecked(tviTag, !isChecked);
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

         // prevent entless loops
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
          * Exclude categories as it whould check ALL children
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
         _tagViewer.setCheckedElements(allTags.toArray(new TVIPrefTag[allTags.size()]));

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

   private void restoreState() {

      updateUI_ActionLayout();
   }

   private void restoreStateBeforeUI() {

      _isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
   }

   @PersistState
   private void saveState() {

      _columnManager.saveState(_state);

      _state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);
   }

   @Override
   public void setFocus() {

      _tagViewer.getTree().setFocus();
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * update tourdata from the fields
    */
   private void updateModelFromUI() {

      final Object[] checkeckElements = _tagViewer.getCheckedElements();

      for (final Object treeItem : checkeckElements) {

         System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ()") //$NON-NLS-1$ //$NON-NLS-2$
               + ("\t: " + treeItem)); //$NON-NLS-1$
// TODO remove SYSTEM.OUT.PRINTLN

      }
   }

   private void updateTagModel() {

      reloadViewer_SetContent();

      if (_selectedTours == null) {
         return;
      }

      // get all tags from all selectedtours
      final Set<Long> allTagIds = new HashSet<>();
      for (final TourData tourData : _selectedTours) {
         final Set<TourTag> allTourTags = tourData.getTourTags();

         if (allTourTags != null) {

            for (final TourTag tourTag : allTourTags) {
               allTagIds.add(tourTag.getTagId());
            }
         }
      }

      final ArrayList<TVIPrefTag> tagItems = new ArrayList<>(allTagIds.size());

      if (allTagIds.size() > 0) {

         // get all tag viewer items which should be checked

         final ArrayList<TreeViewerItem> rootItems = _rootItem.getFetchedChildren();

         for (final long tagId : allTagIds) {

            // Is recursive !!!
            getTagItems(rootItems, tagItems, tagId);
         }
      }

      // update UI
      _tagViewer.setCheckedElements(tagItems.toArray());
   }

   private void updateUI_ActionLayout() {

      if (_isHierarchicalLayout) {

         _actionTagLayout.setToolTipText(Messages.Action_TourTags_Layout_Flat_Tooltip);
         _actionTagLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Flat));

      } else {

         _actionTagLayout.setToolTipText(Messages.Action_TourTags_Layout_Hierarchical_Tooltip);
         _actionTagLayout.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__TagLayout_Hierarchical));
      }
   }
}
