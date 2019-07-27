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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.preferences.PrefPageTags;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class Dialog_SetTags extends TitleAreaDialog implements ITreeViewer {

   private static final String               STATE_IS_HIERARCHICAL_LAYOUT             = "STATE_IS_HIERARCHICAL_LAYOUT"; //$NON-NLS-1$

   private final IDialogSettings             _state;

   private TagMenuManager                    _tagMenuManager;

   private ContainerCheckedTreeViewer        _tagViewer;
   private TVIPrefTagRoot                    _rootItem;

   private boolean                           _tagViewerItem_IsChecked;
   private boolean                           _tagViewerItem_IsKeyPressed;
   private Object                            _tagViewerItem_Data;

   private boolean                           _isBehaviourSingleExpandedOthersCollapse = true;
   private boolean                           _isBehaviourAutoExpandCollapse           = true;
   private boolean                           _isExpandingSelection;
   private boolean                           _isHierarchicalLayout;
   private boolean                           _isInCollapseAll;

   private long                              _expandRunnableCounter;

   private ArrayList<TourData>               _selectedTours;
   private ActionCollapseAllWithoutSelection _actionCollapseAll;
   private ActionExpandAll                   _actionExpandAll;
   private ActionOpenPrefDialog              _actionOpenPrefTags;
   private ActionTag_LayoutFlat              _actionTag_LayoutFlat;
   private ActionTag_LayoutHierarchical      _actionTag_LayoutHierarchical;

   /*
    * Image resources
    */
   private Image _imgTag         = TourbookPlugin.getImageDescriptor(Messages.Image__tag).createImage();
   private Image _imgTagRoot     = TourbookPlugin.getImageDescriptor(Messages.Image__tag_root).createImage();
   private Image _imgTagCategory = TourbookPlugin.getImageDescriptor(Messages.Image__tag_category).createImage();

   /*
    * UI controls
    */
   private ToolBar _toolBarAllTags;

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

   private class ActionTag_LayoutFlat extends Action {

      ActionTag_LayoutFlat() {

         super(Messages.action_tagView_flat_layout, AS_RADIO_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_flat));
      }

      @Override
      public void run() {
//         onTag_Layout(false);
      }
   }

   private class ActionTag_LayoutHierarchical extends Action {

      ActionTag_LayoutHierarchical() {

         super(Messages.action_tagView_flat_hierarchical, AS_RADIO_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__layout_hierarchical));
      }

      @Override
      public void run() {
//         onTag_Layout(true);
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

   public Dialog_SetTags(final Shell parentShell, final TagMenuManager tagMenuManager) {

      super(parentShell);

      _tagMenuManager = tagMenuManager;

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__tag_category).createImage());

      _state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_SetTags_ShellTitle);

      shell.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_SetTags_Title);
      setMessage(Messages.Dialog_SetTags_Message);
   }

   private void createActions() {

      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
      _actionOpenPrefTags = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
      _actionTag_LayoutFlat = new ActionTag_LayoutFlat();
      _actionTag_LayoutHierarchical = new ActionTag_LayoutHierarchical();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      initUI(parent);
      restoreStateBeforeUI();

      createUI(dlgAreaContainer);

      createActions();
      fillToolbar();

      // set root item
      _rootItem = new TVIPrefTagRoot(_tagViewer, true);

      updateUIFromModel();

      restoreState();
      enableControls();

      return dlgAreaContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory
            .fillDefaults()
            .spacing(0, 2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_342_AllTags_Header(container);
         createUI_344_AllTags_Viewer(container);
      }
   }

   private void createUI_342_AllTags_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
//         {
//            // Label: All Tags
//            _lblAllTags = new Label(container, SWT.NONE);
//            _lblAllTags.setText(Messages.Slideout_TourTagFilter_Label_AllTags);
//            GridDataFactory
//                  .fillDefaults()//
//                  .align(SWT.FILL, SWT.CENTER)
//                  .grab(true, false)
//                  .applyTo(_lblAllTags);
//         }
         {
            // toolbar
            _toolBarAllTags = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(container);
         }
      }
   }

   private void createUI_344_AllTags_Viewer(final Composite parent) {

      /*
       * Create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .hint(200, 100)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * create viewer
       */
      final Tree tree = new Tree(layoutContainer,

            SWT.CHECK
                  | SWT.H_SCROLL
                  | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(true);

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

      _tagViewer = new ContainerCheckedTreeViewer(tree);

      _tagViewer.setUseHashlookup(true);
      _tagViewer.setContentProvider(new TagViewerContentProvicer());
      _tagViewer.setComparator(new TagViewerComparator());

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

      defineAllColumns(treeLayout);
   }
   private void defineAllColumns(final TreeColumnLayout treeLayout) {

      TreeViewerColumn tvc;
      TreeColumn tvcColumn;

      {
         // column: tags + tag categories

         tvc = new TreeViewerColumn(_tagViewer, SWT.LEAD);
         tvcColumn = tvc.getColumn();
         tvcColumn.setText(Messages.Pref_TourTag_Column_TagsAndCategories);

         tvc.setLabelProvider(new StyledCellLabelProvider() {
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

//                  styledString.append("  ...", StyledString.COUNTER_STYLER);

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
         treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
      }
      {
         // column: notes

         tvc = new TreeViewerColumn(_tagViewer, SWT.LEAD);
         tvcColumn = tvc.getColumn();
         tvcColumn.setText(Messages.Pref_TourTag_Column_Notes);

         tvc.setLabelProvider(new CellLabelProvider() {
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
         treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
      }
   }

   private void enableControls() {

   }

   private void expandCollapseFolder(final TVIPrefTagCategory treeItem) {

      if (_tagViewer.getExpandedState(treeItem)) {

         // collapse folder

         _tagViewer.collapseToLevel(treeItem, 1);
      }
   }

   /**
    * set the toolbar action after the {@link #_tagViewer} is created
    */
   private void fillToolbar() {

      /*
       * Toolbar: All tags
       */
      final ToolBarManager tbmAllTags = new ToolBarManager(_toolBarAllTags);

      tbmAllTags.add(_actionTag_LayoutFlat);
      tbmAllTags.add(_actionTag_LayoutHierarchical);
      tbmAllTags.add(_actionExpandAll);
      tbmAllTags.add(_actionCollapseAll);
      tbmAllTags.add(_actionOpenPrefTags);

      tbmAllTags.update(true);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private TVIPrefTag getTagTreeItem(final TourTag tourTag) {

      final long requestedTourTagId = tourTag.getTagId();

      final ArrayList<TreeViewerItem> tviChildren = _rootItem.getFetchedChildren();

      return getTagTreeItem_Children(requestedTourTagId, tviChildren);
   }

   /**
    * Recursive !!!
    *
    * @param requestedTourTagId
    * @param tviChildren
    * @return Returns tree item for the tour tag or <code>null</code> when not found
    */
   private TVIPrefTag getTagTreeItem_Children(final long requestedTourTagId, final ArrayList<TreeViewerItem> tviChildren) {

      for (final TreeViewerItem treeViewerItem : tviChildren) {

         if (treeViewerItem instanceof TVIPrefTag) {

            final TVIPrefTag tviTag = (TVIPrefTag) treeViewerItem;

            if (tviTag.getTourTag().getTagId() == requestedTourTagId) {
               return tviTag;
            }

         } else if (treeViewerItem instanceof TVIPrefTagCategory) {

            final TVIPrefTagCategory tviTagCategory = (TVIPrefTagCategory) treeViewerItem;

            final TVIPrefTag tviTagFromChilren = getTagTreeItem_Children(requestedTourTagId, tviTagCategory.getFetchedChildren());

            if (tviTagFromChilren != null) {
               return tviTagFromChilren;
            }
         }
      }

      return null;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tagViewer;
   }

   private void initUI(final Composite parent) {

   }

   @Override
   protected void okPressed() {

      saveState();
      updateModelFromUI();

      super.okPressed();
   }

   private void onDispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();
   }

   private void onTag_Select(final SelectionChangedEvent event) {

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

//         update_FromTagViewer();

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

   private void restoreState() {

      /*
       * Set layout actions after the UI is created
       */
      if (_isHierarchicalLayout) {
         _actionTag_LayoutHierarchical.setChecked(true);
      } else {
         _actionTag_LayoutFlat.setChecked(true);
      }
   }

   private void restoreStateBeforeUI() {

      // layout
      _isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
   }

   private void saveState() {

      _state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);
   }
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

      _rootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
      _tagViewer.setInput(this);

      loadAllTagItems();
   }

   private void updateUIFromModel() {

      // show contents in the viewers
      _tagViewer.setInput(this);

      // a tour must be selected
      _selectedTours = _tagMenuManager.getTourProvider().getSelectedTours();

      // get all tags for all tours
      final Set<TourTag> selectedTourTags = new HashSet<>();
      for (final TourData tourData : _selectedTours) {
         final Set<TourTag> tags = tourData.getTourTags();
         if (tags != null) {
            selectedTourTags.addAll(tags);
         }
      }

      // get tag tree items from the viewer, this will also load all tree children
      final ArrayList<TVIPrefTag> allTagItems = new ArrayList<>();
      for (final TourTag tourTag : selectedTourTags) {

         final TVIPrefTag tviTag = getTagTreeItem(tourTag);

         if (tviTag != null) {
            allTagItems.add(tviTag);
         }
      }

      final TVIPrefTag[] checkedItems = allTagItems.toArray(new TVIPrefTag[allTagItems.size()]);

      // check tags
      _tagViewer.setCheckedElements(checkedItems);

      // set parent gray state
//      _tagViewer.setGrayedElements(checkedItems);
      _tagViewer.setExpandedElements(checkedItems);

//      for (final TVIPrefTag tviPrefTag : checkedItems) {
//
//         final TreeViewerItem tviParent = tviPrefTag.getParentItem();
////         final boolean isVisible = _tagViewer.setParentsGrayed(tviParent, true);
//      }

   }
}
