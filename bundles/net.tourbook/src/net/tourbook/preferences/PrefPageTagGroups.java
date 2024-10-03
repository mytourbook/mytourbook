/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tag.TagGroup;
import net.tourbook.tag.TagGroupManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PrefPageTagGroups extends PreferencePage implements IWorkbenchPreferencePage, ITreeViewer {

   public static final String  ID                           = "net.tourbook.preferences.PrefPageTagGroups"; //$NON-NLS-1$

   private static final String STATE_IS_HIERARCHICAL_LAYOUT = "STATE_IS_HIERARCHICAL_LAYOUT";               //$NON-NLS-1$
   private static final String STATE_SELECTED_TAG_GROUP     = "STATE_SELECTED_TAG_GROUP";                   //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final IDialogSettings      _state      = TourbookPlugin.getState("net.tourbook.preferences.PrefPageTagsGroups");   //$NON-NLS-1$
   private static final IPreferenceStore     _prefStore  = TourbookPlugin.getDefault().getPreferenceStore();

// SET_FORMATTING_ON

   private SelectionListener                 _defaultSelectionListener;
   private MouseWheelListener                _defaultMouseWheelListener;
   private IPropertyChangeListener           _prefChangeListener;

   private TableViewer                       _tagGroupViewer;

   private ContainerCheckedTreeViewer        _tagViewer;
   private TVIPrefTagRoot                    _tagViewerRootItem;

   private boolean                           _tagViewerItem_IsChecked;
   private boolean                           _tagViewerItem_IsKeyPressed;
   private Object                            _tagViewerItem_Data;

   private long                              _dragStartViewerLeft;

   private long                              _expandRunnableCounter;
   private boolean                           _isBehaviourSingleExpandedOthersCollapse = true;
   private boolean                           _isBehaviourAutoExpandCollapse           = true;
   private boolean                           _isExpandingSelection;
   private boolean                           _isHierarchicalLayout;
   private boolean                           _isInCollapseAll;
   private boolean                           _isInUpdateUI;
   private boolean                           _isInUpdateUIAfterDelete;
   private boolean                           _isLiveUpdate;
   private boolean                           _isModified;

   private TagGroup                          _selectedTagGroup;

   private ActionCollapseAllWithoutSelection _actionCollapseAll;
   private ActionExpandAll                   _actionExpandAll;
   private ActionTag_LayoutFlat              _actionTag_LayoutFlat;
   private ActionTag_LayoutHierarchical      _actionTag_LayoutHierarchical;

   /*
    * UI controls
    */
   private Composite _containerTagGroupHeader;

   private ToolBar   _toolBarAllTags;

   private Button    _btnNew;
   private Button    _btnRename;
   private Button    _btnDelete;
   private Button    _btnUp;
   private Button    _btnDown;

   private Label     _lblActionSpacer;

   private Image     _imgTag;
   private Image     _imgTagRoot;
   private Image     _imgTagCategory;

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

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat));
      }

      @Override
      public void run() {
         onTag_Layout(false);
      }
   }

   private class ActionTag_LayoutHierarchical extends Action {

      ActionTag_LayoutHierarchical() {

         super(Messages.action_tagView_flat_hierarchical, AS_RADIO_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Hierarchical));
      }

      @Override
      public void run() {
         onTag_Layout(true);
      }
   }

   /**
    * Sort the tags and categories
    */
   private static final class TagViewerComparator extends ViewerComparator {
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

   private final class TagViewerContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {
         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _tagViewerRootItem.getFetchedChildrenAsArray();
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

   public PrefPageTagGroups() {}

   public PrefPageTagGroups(final String title) {
      super(title);
   }

   public PrefPageTagGroups(final String title, final ImageDescriptor image) {
      super(title, image);
   }

   private void actionTagGroup_Delete() {

      final TagGroup groupItem = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();

      if (groupItem == null) {
         return;
      }

      // update model
      TagGroupManager.getTagGroups().remove(groupItem);

      // update UI
      final Table groupTable = _tagGroupViewer.getTable();
      final int selectionIndex = groupTable.getSelectionIndex();

      _tagGroupViewer.remove(groupItem);

      // select next filter item
      final int nextIndex = Math.min(groupTable.getItemCount() - 1, selectionIndex);

      if (nextIndex >= 0) {

         _tagGroupViewer.setSelection(new StructuredSelection(_tagGroupViewer.getElementAt(nextIndex)));

      } else {

         _selectedTagGroup = null;

         enableControls();
      }

      _isModified = true;
   }

   private void actionTagGroup_MoveDown() {

      final TagGroup groupItem = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();

      if (groupItem == null) {
         return;
      }

      final Table groupTable = _tagGroupViewer.getTable();
      final int selectionIndex = groupTable.getSelectionIndex();

      if (selectionIndex < groupTable.getItemCount() - 1) {

         _tagGroupViewer.remove(groupItem);
         _tagGroupViewer.insert(groupItem, selectionIndex + 1);

         // reselect moved item
         _tagGroupViewer.setSelection(new StructuredSelection(groupItem));

         if (groupTable.getSelectionIndex() == groupTable.getItemCount() - 1) {
            _btnUp.setFocus();
         } else {
            _btnDown.setFocus();
         }

         _isModified = true;
      }
   }

   private void actionTagGroup_MoveUp() {

      final TagGroup groupItem = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();

      if (groupItem == null) {
         return;
      }

      final Table groupTable = _tagGroupViewer.getTable();

      final int selectionIndex = groupTable.getSelectionIndex();
      if (selectionIndex > 0) {
         _tagGroupViewer.remove(groupItem);
         _tagGroupViewer.insert(groupItem, selectionIndex - 1);

         // reselect moved item
         _tagGroupViewer.setSelection(new StructuredSelection(groupItem));

         if (groupTable.getSelectionIndex() == 0) {
            _btnDown.setFocus();
         } else {
            _btnUp.setFocus();
         }

         _isModified = true;
      }
   }

   private void actionTagGroup_New() {

      final InputDialog inputDialog = new InputDialog(getShell(),
            "Tag Groups",
            "Tag group name",
            UI.EMPTY_STRING,
            getNameValidator());

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      // create new tag group
      final TagGroup tagGroup = new TagGroup();
      tagGroup.name = inputDialog.getValue().trim();

      // update model
      TagGroupManager.getTagGroups().add(tagGroup);

      // update UI
      _tagGroupViewer.setInput(new Object());

      // select new group
      _tagGroupViewer.setSelection(new StructuredSelection(tagGroup), true);

      _tagGroupViewer.getTable().setFocus();

      _isModified = true;
   }

   private void actionTagGroup_Rename() {

      final TagGroup tagGroup = (TagGroup) (_tagGroupViewer.getStructuredSelection()).getFirstElement();

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypeFilter_dlg_rename_title,
            Messages.Pref_TourTypeFilter_dlg_rename_message,
            tagGroup.name,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      // update model
      tagGroup.name = inputDialog.getValue().trim();

      // update viewer
      _tagGroupViewer.update(tagGroup, null);

      _isModified = true;
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         if (propertyChangeEvent.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            updateViewers();

            enableControls();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void createActions() {

      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
      _actionTag_LayoutFlat = new ActionTag_LayoutFlat();
      _actionTag_LayoutHierarchical = new ActionTag_LayoutHierarchical();
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      restoreStateBeforeUI();

      final Composite ui = createUI(parent);

      createActions();
      fillToolbar();

      // this must be called AFTER the toolbar is created and filled !!!
      updateUI_HeaderHeight();

      addPrefListener();

      updateViewers();

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI_10_TagGroups(container);
         createUI_20_AllTags(container);
         createUI_30_TagGroupActions(container);
      }

      // hint to use drag & drop
      final Label label = UI.createLabel(parent, "Hint: The group list can be rearranged with drag && drop.", SWT.WRAP);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      // spacer
//      UI.createSpacer_Horizontal(parent);

      return container;
   }

   private void createUI_10_TagGroups(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {

         _containerTagGroupHeader = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerTagGroupHeader);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_containerTagGroupHeader);
//         _containerTagGroupHeader.setBackground(UI.SYS_COLOR_CYAN);
         {
            final Label label = UI.createLabel(_containerTagGroupHeader, "Tag &Groups");
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
//                  .indent(0, 5)
                  .applyTo(label);
//            label.setBackground(UI.SYS_COLOR_YELLOW);
         }

         createUI_12_TagGroups_Viewer(container);
      }
   }

   private void createUI_12_TagGroups_Viewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(70, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(tableLayouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      TableViewerColumn tvc;

      _tagGroupViewer = new TableViewer(table);

      // column: group name
      tvc = new TableViewerColumn(_tagGroupViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TagGroup tagGroup = ((TagGroup) cell.getElement());

            cell.setText(tagGroup.name);
         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(1));

      _tagGroupViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {

            return TagGroupManager.getTagGroups().toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _tagGroupViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectTagGroup());
      _tagGroupViewer.addDoubleClickListener(doubleClickEvent -> actionTagGroup_Rename());

      /*
       * Set drag adapter
       */
      _tagGroupViewer.addDragSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            new DragSourceListener() {

               @Override
               public void dragFinished(final DragSourceEvent event) {

                  final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

                  if (event.doit == false) {
                     return;
                  }

                  transfer.setSelection(null);
                  transfer.setSelectionSetTime(0);
               }

               @Override
               public void dragSetData(final DragSourceEvent event) {
                  // data are set in LocalSelectionTransfer
               }

               @Override
               public void dragStart(final DragSourceEvent event) {

                  final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
                  final ISelection selection = _tagGroupViewer.getSelection();

                  transfer.setSelection(selection);
                  transfer.setSelectionSetTime(_dragStartViewerLeft = event.time & 0xFFFFFFFFL);

                  event.doit = !selection.isEmpty();
               }
            });

      /*
       * set drop adapter
       */
      final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_tagGroupViewer) {

         private Widget _tableItem;

         @Override
         public void dragOver(final DropTargetEvent dropEvent) {

            // keep table item
            _tableItem = dropEvent.item;

            super.dragOver(dropEvent);
         }

         @Override
         public boolean performDrop(final Object data) {

            if (data instanceof StructuredSelection) {
               final StructuredSelection selection = (StructuredSelection) data;

               if (selection.getFirstElement() instanceof TagGroup) {

                  final TagGroup groupItem = (TagGroup) selection.getFirstElement();

                  final int location = getCurrentLocation();
                  final Table groupTable = _tagGroupViewer.getTable();

                  /*
                   * check if drag was started from this filter, remove the filter item before the
                   * new filter is inserted
                   */
                  if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStartViewerLeft) {
                     _tagGroupViewer.remove(groupItem);
                  }

                  int filterIndex;

                  if (_tableItem == null) {

                     _tagGroupViewer.add(groupItem);
                     filterIndex = groupTable.getItemCount() - 1;

                  } else {

                     // get index of the target in the table
                     filterIndex = groupTable.indexOf((TableItem) _tableItem);
                     if (filterIndex == -1) {
                        return false;
                     }

                     if (location == LOCATION_BEFORE) {
                        _tagGroupViewer.insert(groupItem, filterIndex);
                     } else if (location == LOCATION_AFTER || location == LOCATION_ON) {
                        _tagGroupViewer.insert(groupItem, ++filterIndex);
                     }
                  }

                  // reselect filter item
                  _tagGroupViewer.setSelection(new StructuredSelection(groupItem));

                  // set focus to selection
                  groupTable.setSelection(filterIndex);
                  groupTable.setFocus();

                  _isModified = true;

                  return true;
               }
            }

            return false;
         }

         @Override
         public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

            final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
            if (selection instanceof StructuredSelection) {
               final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
               if (target == dragFilter) {
                  return false;
               }
            }

            if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
               return false;
            }

            return true;
         }

      };

      _tagGroupViewer.addDropSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            viewerDropAdapter);
   }

   private void createUI_20_AllTags(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);

      {
         createUI_22_AllTags_Header(container);
         createUI_24_AllTags_Viewer(container);
      }
   }

   private void createUI_22_AllTags_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            // Label: All Tags
            final Label label = new Label(container, SWT.NONE);
            label.setText("Available &Tags");
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.END)
                  .grab(true, true)
                  .applyTo(label);
         }
         {
            // toolbar
            _toolBarAllTags = new ToolBar(container, SWT.FLAT);
         }
      }
   }

   private void createUI_24_AllTags_Viewer(final Composite parent) {

      /*
       * Create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(100, 100)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * Create viewer tree
       */
      final Tree tree = new Tree(layoutContainer,
            SWT.H_SCROLL | SWT.V_SCROLL
                  | SWT.MULTI
                  | SWT.CHECK
                  | SWT.FULL_SELECTION);

      tree.setHeaderVisible(false);

      tree.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

         /*
          * The tag treeviewer selection event can have another selection !!!
          */

         _tagViewerItem_IsChecked = selectionEvent.detail == SWT.CHECK;

         if (_tagViewerItem_IsChecked) {

            /*
             * Item can be null when <ctrl>+A is pressed !!!
             */
            final Widget item = selectionEvent.item;

            _tagViewerItem_Data = item.getData();
         }
      }));

      tree.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> _tagViewerItem_IsKeyPressed = true));

      layoutContainer.addTraverseListener(traverseEvent -> onTraverse_TagContainer(tree, traverseEvent));

      /*
       * Create viewer
       */
      _tagViewer = new ContainerCheckedTreeViewer(tree);

      _tagViewer.setUseHashlookup(true);
      _tagViewer.setContentProvider(new TagViewerContentProvider());
      _tagViewer.setComparator(new TagViewerComparator());

      _tagViewer.addCheckStateListener(checkStateChangedEvent -> update_FromTagViewer());
      _tagViewer.addSelectionChangedListener(selectionChangedEvent -> onTag_Select(selectionChangedEvent));

      /**
       * This check state provider is necessary to fix
       * <p>
       * https://bugs.eclipse.org/bugs/show_bug.cgi?id=170521
       * <p>
       * Refreshing ContainerCheckedTreeViewer does not restore checked/grayed states correctly.
       */
//      _tagViewer.setCheckStateProvider(new ICheckStateProvider() {
//
//         @Override
//         public boolean isChecked(final Object element) {
//
//            if (isGrayed(element)) {
//               return true;
//            }
//
//            return false;
//         }
//
//         @Override
//         public boolean isGrayed(final Object element) {
//
//            return false;
//         }
//      });

      /*
       * Create columns
       */
      TreeViewerColumn tvc;
      TreeColumn tvcColumn;

      // column: tags + tag categories
      tvc = new TreeViewerColumn(_tagViewer, SWT.LEAD);
      tvcColumn = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final StyledString styledString = new StyledString();

            final Object element = cell.getElement();

            if (element instanceof TVIPrefTag) {

               final TourTag tourTag = ((TVIPrefTag) element).getTourTag();

               styledString.append(tourTag.getTagName(), net.tourbook.ui.UI.CONTENT_SUB_CATEGORY_STYLER);
               cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

            } else if (element instanceof TVIPrefTagCategory) {

               final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
               final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

               cell.setImage(_imgTagCategory);

               styledString.append(tourTagCategory.getCategoryName(), net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);

               // get number of categories
               final int categoryCounter = tourTagCategory.getNumberOfCategories();
               final int tagCounter = tourTagCategory.getNumberOfTags();
               if (categoryCounter == -1 && tagCounter == -1) {

//                  styledString.append("  ...", StyledString.COUNTER_STYLER);

               } else {

                  String categoryString = UI.EMPTY_STRING;
                  if (categoryCounter > 0) {
                     categoryString = "/" + categoryCounter; //$NON-NLS-1$
                  }
                  styledString.append(UI.SPACE3 + tagCounter + categoryString, StyledString.QUALIFIER_STYLER);
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

   private void createUI_30_TagGroupActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
      {
         _lblActionSpacer = UI.createSpacer_Horizontal(container);
         GridDataFactory.fillDefaults().applyTo(_lblActionSpacer);

         // button: new
         _btnNew = new Button(container, SWT.NONE);
         _btnNew.setText(Messages.Pref_TourTypeFilter_button_new);
         _btnNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTagGroup_New()));
         setButtonLayoutData(_btnNew);

         // button: rename
         _btnRename = new Button(container, SWT.NONE);
         _btnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
         _btnRename.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTagGroup_Rename()));
         setButtonLayoutData(_btnRename);

         // button: delete
         _btnDelete = new Button(container, SWT.NONE);
         _btnDelete.setText(Messages.Pref_TourTypeFilter_button_remove);
         _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTagGroup_Delete()));
         setButtonLayoutData(_btnDelete);

         UI.createSpacer_Horizontal(container);

         // button: up
         _btnUp = new Button(container, SWT.NONE);
         _btnUp.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up);
         _btnUp.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTagGroup_MoveUp()));
         setButtonLayoutData(_btnUp);

         // button: down
         _btnDown = new Button(container, SWT.NONE);
         _btnDown.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down);
         _btnDown.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> actionTagGroup_MoveDown()));
         setButtonLayoutData(_btnDown);
      }
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final Table groupTable = _tagGroupViewer.getTable();
      final int numGroups = groupTable.getItemCount();
      final int selectionIndex = groupTable.getSelectionIndex();

      final TagGroup tagGroup = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();
      final boolean isGroupSelected = tagGroup != null;

      _btnUp.setEnabled(isGroupSelected && selectionIndex > 0);
      _btnDown.setEnabled(isGroupSelected && selectionIndex < numGroups - 1);

      _btnRename.setEnabled(isGroupSelected);
      _btnDelete.setEnabled(isGroupSelected);

      _actionCollapseAll.setEnabled(_isHierarchicalLayout);
      _actionExpandAll.setEnabled(_isHierarchicalLayout);

      _tagViewer.getTree().setEnabled(isGroupSelected);
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
       * Toolbar: Tag cloud
       */
//      final ToolBarManager tbmTagCloud = new ToolBarManager(_toolBarTagCloud);
//
//      tbmTagCloud.add(_actionTagCloud_CheckAll);
//      tbmTagCloud.add(_actionTagCloud_UncheckAll);
//
//      tbmTagCloud.update(true);

      /*
       * Toolbar: All tags
       */
      final ToolBarManager tbmAllTags = new ToolBarManager(_toolBarAllTags);

      tbmAllTags.add(_actionTag_LayoutFlat);
      tbmAllTags.add(_actionTag_LayoutHierarchical);
      tbmAllTags.add(_actionExpandAll);
      tbmAllTags.add(_actionCollapseAll);

      tbmAllTags.update(true);
   }

   /**
    * Fire modify event only when live update is selected
    */
   private void fireModifyEvent() {

      if (_isLiveUpdate) {
//         TourTagFilterManager.fireFilterModifyEvent();
      }
   }

   /**
    * Get the input validator for the receiver.
    *
    * @return IInputValidator
    */
   private IInputValidator getNameValidator() {

      return newText -> {

         if (newText.trim().length() > 0) {
            return null;
         }

         return "Name must not be empty";
      };
   }

   private long[] getTagIds_FromTagViewer() {

      final LongHashSet tagIds = new LongHashSet();

      final Object[] checkedElements = _tagViewer.getCheckedElements();

      for (final Object object : checkedElements) {

         if (object instanceof TVIPrefTag) {

            final TVIPrefTag tagItem = (TVIPrefTag) object;
            final long tagId = tagItem.getTourTag().getTagId();

            tagIds.add(tagId);
         }
      }

      return tagIds.toArray();
   }

   /**
    * Traverses all tag viewer items until a tag items is found Recursive !
    *
    * @param parentItems
    * @param tagItems
    * @param tagId
    *
    * @return Returns <code>true</code> when the tag id is found
    */
   private boolean getTagItems(final List<TreeViewerItem> parentItems,
                               final List<TVIPrefTag> tagItems,
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
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _imgTag = TourbookPlugin.getImageDescriptor(Images.Tag).createImage();
      _imgTagRoot = TourbookPlugin.getImageDescriptor(Images.Tag_Root).createImage();
      _imgTagCategory = TourbookPlugin.getImageDescriptor(Images.Tag_Category).createImage();

      parent.addDisposeListener(disposeEvent -> onDispose());

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeProperty());

      _defaultMouseWheelListener = mouseEvent -> {
         net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeProperty();
      };
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

         final ArrayList<TreeViewerItem> rootItems = _tagViewerRootItem.getFetchedChildren();

         for (final long tagId : tagIds) {

            // Is recursive !!!
            getTagItems(rootItems, tagItems, tagId);
         }
      }
   }

   /**
    * Property was changed, fire a property change event
    */
   private void onChangeProperty() {

      _isModified = true;
   }

   private void onDispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();
   }

   private void onSelectTagGroup() {

      final TagGroup tagGroup = (TagGroup) (_tagGroupViewer.getStructuredSelection()).getFirstElement();

      if (tagGroup == null) {
         return;
      }

      _selectedTagGroup = tagGroup;

      updateUI_TagViewer(tagGroup.tourTags);

      enableControls();
   }

   /**
    * @param isHierarchicalLayout
    *           Is <code>true</code> when the layout is flat, otherwise it is hierarchical
    */
   private void onTag_Layout(final boolean isHierarchicalLayout) {

      // ignore layout when it is already set
      if (_isHierarchicalLayout == isHierarchicalLayout) {
         return;
      }

      _isHierarchicalLayout = isHierarchicalLayout;

      updateTagModel();

      // reselect tags
      if (_selectedTagGroup != null) {

         updateUI_TagViewer(_selectedTagGroup.tourTags);
      }

      enableControls();
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

         update_FromTagViewer();

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
    * expanded children are not visible (but they could) like when the triangle (+/-) icon in the
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

         final Tree tree = _tagViewer.getTree();

         /*
          * Run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         tree.getDisplay().asyncExec(new Runnable() {

            private long           __expandRunnableCounter = ++_expandRunnableCounter;

            private ITreeSelection __treeSelection         = treeSelection;
            private TreePath       __selectedTreePath      = selectedTreePath;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               if (tree.isDisposed()) {
                  return;
               }

               onTag_SelectCategory_20_AutoExpandCollapse_Runnable(
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
    * @param treeSelection
    * @param selectedTreePath
    */
   private void onTag_SelectCategory_20_AutoExpandCollapse_Runnable(final ITreeSelection treeSelection,
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
            _tagViewer.setExpandedTreePaths(selectedTreePath);
            _tagViewer.setSelection(treeSelection, true);

            if (_isBehaviourAutoExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _tagViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * win behavior: when an item is set to top which was collapsed before, it will be
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

   /**
    * Terrible solution to traverse to a tree
    *
    * @param tree
    * @param event
    */
   private void onTraverse_TagContainer(final Tree tree, final TraverseEvent event) {

      if (event.detail == SWT.TRAVERSE_TAB_NEXT) {

         tree.setFocus();

         final TreeItem[] selection = tree.getSelection();
         if (selection == null || selection.length == 0) {

            if (tree.getItemCount() > 0) {
               tree.setSelection(tree.getItem(0));
            }

         } else {

            tree.setSelection(selection);
         }
      }
   }

   @Override
   protected void performDefaults() {

      _isModified = true;

      super.performDefaults();

      // this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
   }

   @Override
   public boolean performOk() {

      saveState();

      return true;
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

      /*
       * Tag group
       */
      final String selectedGroupName = Util.getStateString(_state, STATE_SELECTED_TAG_GROUP, null);

      if (selectedGroupName != null) {

         for (final TagGroup tagGroup : TagGroupManager.getTagGroups()) {

            if (selectedGroupName.equals(tagGroup.name)) {

               _selectedTagGroup = tagGroup;

               break;
            }
         }

         if (_selectedTagGroup != null) {

            _tagGroupViewer.setSelection(new StructuredSelection(_selectedTagGroup), true);
         }
      }
   }

   private void restoreStateBeforeUI() {

      // layout
      _isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
   }

   private void saveState() {

      if (_isModified) {

         _isModified = false;

         TagGroupManager.saveState();

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
      }

      _state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);

      if (_selectedTagGroup != null) {

         _state.put(STATE_SELECTED_TAG_GROUP, _selectedTagGroup.name);
      }
   }

   private void update_FromTagViewer() {

      if (_selectedTagGroup == null) {
         return;
      }

      final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();
      final long[] allCheckedTagIds = getTagIds_FromTagViewer();

      final List<TourTag> allCheckedTourTags = new ArrayList<>();

      for (final long tagId : allCheckedTagIds) {

         final TourTag tourTag = allTourTags.get(tagId);

         allCheckedTourTags.add(tourTag);
      }

      _selectedTagGroup.tourTags.clear();
      _selectedTagGroup.tourTags.addAll(allCheckedTourTags);

      _isModified = true;

      enableControls();

      fireModifyEvent();
   }

   private void updateTagModel() {

      _tagViewerRootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
      _tagViewer.setInput(this);

      loadAllTagItems();
   }

   private void updateUI_HeaderHeight() {

      final Point toolbarSize = _toolBarAllTags.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      final int toolbarHeight = toolbarSize.y;

      GridData gd = (GridData) _lblActionSpacer.getLayoutData();
      gd.heightHint = toolbarHeight;

      gd = (GridData) _containerTagGroupHeader.getLayoutData();
      gd.heightHint = toolbarHeight;
   }

   private void updateUI_TagViewer(final List<TourTag> allTourTags) {

      final List<TVIPrefTag> allTagItems = new ArrayList<>(allTourTags.size());

      if (allTourTags.size() > 0) {

         // get all tag viewer items which should be checked

         final List<TreeViewerItem> allRootItems = _tagViewerRootItem.getFetchedChildren();

         for (final TourTag tag : allTourTags) {

            // Is recursive !!!
            getTagItems(allRootItems, allTagItems, tag.getTagId());
         }
      }

      // update UI
      _tagViewer.setCheckedElements(allTagItems.toArray());
   }

   private void updateViewers() {

      // show contents in the viewers
      _tagGroupViewer.setInput(new Object());

      // load tag viewer
      updateTagModel();
   }

}
