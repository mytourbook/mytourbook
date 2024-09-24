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
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tour.TourTypeFilterManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
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

public class PrefPageTagsGroups extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                                       = "net.tourbook.preferences.PrefPageTagsGroups";   //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                               = TourbookPlugin.getDefault().getPreferenceStore();

   private SelectionListener             _defaultSelectionListener;
   private MouseWheelListener            _defaultMouseWheelListener;
   private IPropertyChangeListener       _prefChangeListener;

   private TableViewer                   _tagGroupViewer;

   private ContainerCheckedTreeViewer    _tagViewer;
   private TVIPrefTagRoot                _tagViewerRootItem;

   private boolean                       _tagViewerItem_IsChecked;
   private boolean                       _tagViewerItem_IsKeyPressed;
   private Object                        _tagViewerItem_Data;

   private long                          _dragStartViewerLeft;

   private long                          _expandRunnableCounter;
   private boolean                       _isBehaviourSingleExpandedOthersCollapse = true;
   private boolean                       _isBehaviourAutoExpandCollapse           = true;
   private boolean                       _isExpandingSelection;
   private boolean                       _isHierarchicalLayout;
   private boolean                       _isInCollapseAll;
   private boolean                       _isInUpdateUI;
   private boolean                       _isInUpdateUIAfterDelete;
   private boolean                       _isLiveUpdate;
   private boolean                       _isModified;

   private ArrayList<TourType>           _tourTypes;
   private ArrayList<TourTypeFilter>     _filterList;

   private TourTypeFilter                _activeFilter;

   /*
    * UI controls
    */
   private Button  _btnNew;
   private Button  _btnRename;
   private Button  _btnRemove;
   private Button  _btnUp;
   private Button  _btnDown;

   private Button  _chkTourTypeContextMenu;

   private Label   _lblAllTags;

   private Spinner _spinnerRecentTourTypes;

   private Image   _imgTag;
   private Image   _imgTagRoot;
   private Image   _imgTagCategory;

   private ToolBar _toolBarAllTags;

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

   public PrefPageTagsGroups() {}

   public PrefPageTagsGroups(final String title) {
      super(title);
   }

   public PrefPageTagsGroups(final String title, final ImageDescriptor image) {
      super(title, image);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         if (propertyChangeEvent.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
            updateViewers();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();

      addPrefListener();

      updateViewers();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      Label label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Pref_TourTypes_root_title);
      label.setLayoutData(new GridData(SWT.NONE, SWT.NONE, true, false));

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         createUI_100_FilterViewer(container);
         createUI_200_AllTags(container);
         createUI_400_Actions(container);
      }

      // hint to use drag & drop
      label = new Label(parent, SWT.WRAP);
      label.setText(Messages.Pref_TourTypes_dnd_hint);
      label.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

      createUI_500_Options(parent);

      // spacer
      new Label(parent, SWT.WRAP);

      return container;
   }

   private void createUI_100_FilterViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(200, 500)
            .applyTo(layouter);

      final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      TableViewerColumn tvc;

      _tagGroupViewer = new TableViewer(table);

      // column: name + image
      tvc = new TableViewerColumn(_tagGroupViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourTypeFilter filter = ((TourTypeFilter) cell.getElement());
            final int filterType = filter.getFilterType();

            String filterName = null;
            Image filterImage = null;

            // set filter name/image
            switch (filterType) {
            case TourTypeFilter.FILTER_TYPE_DB:
               final TourType tourType = filter.getTourType();
               filterName = tourType.getName();
               filterImage = TourTypeImage.getTourTypeImage(tourType.getTypeId());
               break;

            case TourTypeFilter.FILTER_TYPE_SYSTEM:
               filterName = filter.getSystemFilterName();
               filterImage = UI.IMAGE_REGISTRY.get(net.tourbook.ui.UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
               break;

            case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
               filterName = filter.getTourTypeSet().getName();
               filterImage = UI.IMAGE_REGISTRY.get(net.tourbook.ui.UI.IMAGE_TOUR_TYPE_FILTER);
               break;

            default:
               break;
            }

            cell.setText(filterName);
            cell.setImage(filterImage);
         }
      });
      layouter.addColumnData(new ColumnWeightData(1));

      _tagGroupViewer.setContentProvider(new IStructuredContentProvider() {
         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            return _filterList.toArray();
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _tagGroupViewer.addSelectionChangedListener(selectionChangedEvent -> onSelectFilter());
      _tagGroupViewer.addDoubleClickListener(doubleClickEvent -> onRenameFilterSet());

      /*
       * set drag adapter
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

               if (selection.getFirstElement() instanceof TourTypeFilter) {

                  final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();

                  final int location = getCurrentLocation();
                  final Table filterTable = _tagGroupViewer.getTable();

                  /*
                   * check if drag was started from this filter, remove the filter item before the
                   * new filter is inserted
                   */
                  if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStartViewerLeft) {
                     _tagGroupViewer.remove(filterItem);
                  }

                  int filterIndex;

                  if (_tableItem == null) {

                     _tagGroupViewer.add(filterItem);
                     filterIndex = filterTable.getItemCount() - 1;

                  } else {

                     // get index of the target in the table
                     filterIndex = filterTable.indexOf((TableItem) _tableItem);
                     if (filterIndex == -1) {
                        return false;
                     }

                     if (location == LOCATION_BEFORE) {
                        _tagGroupViewer.insert(filterItem, filterIndex);
                     } else if (location == LOCATION_AFTER || location == LOCATION_ON) {
                        _tagGroupViewer.insert(filterItem, ++filterIndex);
                     }
                  }

                  // reselect filter item
                  _tagGroupViewer.setSelection(new StructuredSelection(filterItem));

                  // set focus to selection
                  filterTable.setSelection(filterIndex);
                  filterTable.setFocus();

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

   private Composite createUI_200_AllTags(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory
            .fillDefaults()
            .spacing(0, 2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_310_AllTags_Header(container);
         createUI_320_AllTags_Viewer(container);
      }

      return container;
   }

   private void createUI_310_AllTags_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      containerTag.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            // Label: All Tags
            _lblAllTags = new Label(container, SWT.NONE);
            _lblAllTags.setText(Messages.Slideout_TourTagFilter_Label_AllTags);
            GridDataFactory
                  .fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .applyTo(_lblAllTags);
         }
         {
            // toolbar
            _toolBarAllTags = new ToolBar(container, SWT.FLAT);
         }
      }
   }

   private void createUI_320_AllTags_Viewer(final Composite parent) {

      /*
       * Create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(200, 100)
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

      /*
       * create columns
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

   private void createUI_400_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
      {
         // button: new
         _btnNew = new Button(container, SWT.NONE);
         _btnNew.setText(Messages.Pref_TourTypeFilter_button_new);
         _btnNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onNewFilterSet()));
         setButtonLayoutData(_btnNew);

         // button: rename
         _btnRename = new Button(container, SWT.NONE);
         _btnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
         _btnRename.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onRenameFilterSet()));
         setButtonLayoutData(_btnRename);

         // button: delete
         _btnRemove = new Button(container, SWT.NONE);
         _btnRemove.setText(Messages.Pref_TourTypeFilter_button_remove);
         _btnRemove.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onDeleteFilterSet()));
         setButtonLayoutData(_btnRemove);

         UI.createSpacer_Horizontal(container);

         // button: up
         _btnUp = new Button(container, SWT.NONE);
         _btnUp.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up);
         _btnUp.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMoveUp()));
         setButtonLayoutData(_btnUp);

         // button: down
         _btnDown = new Button(container, SWT.NONE);
         _btnDown.setText(Messages.PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down);
         _btnDown.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onMoveDown()));
         setButtonLayoutData(_btnDown);
      }
   }

   private void createUI_500_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * show tour type context menu on mouse over
             */
            _chkTourTypeContextMenu = new Button(container, SWT.CHECK | SWT.WRAP);
            _chkTourTypeContextMenu.setText(Messages.Pref_Appearance_ShowTourTypeContextMenu);
            _chkTourTypeContextMenu.setToolTipText(Messages.Pref_Appearance_ShowTourTypeContextMenu_Tooltip);
            _chkTourTypeContextMenu.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> _isModified = true));
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(0, 10)
                  .applyTo(_chkTourTypeContextMenu);
         }
         {
            /*
             * number of recent tour types
             */
            final Label label = new Label(container, NONE);
            label.setText(Messages.Pref_Appearance_NumberOfRecent_TourTypes);
            label.setToolTipText(Messages.Pref_Appearance_NumberOfRecent_TourTypes_Tooltip);

            // spinner
            _spinnerRecentTourTypes = new Spinner(container, SWT.BORDER);
            _spinnerRecentTourTypes.setToolTipText(Messages.Pref_Appearance_NumberOfRecent_TourTypes_Tooltip);
            _spinnerRecentTourTypes.setMinimum(0);
            _spinnerRecentTourTypes.setMaximum(9);
            _spinnerRecentTourTypes.addSelectionListener(_defaultSelectionListener);
            _spinnerRecentTourTypes.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerRecentTourTypes);
         }
      }
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableButtons() {

      final IStructuredSelection selection = (IStructuredSelection) _tagGroupViewer.getSelection();

      final TourTypeFilter filterItem = (TourTypeFilter) selection.getFirstElement();
      final Table filterTable = _tagGroupViewer.getTable();

      _btnUp.setEnabled(filterItem != null && filterTable.getSelectionIndex() > 0);
      _btnDown.setEnabled(filterItem != null && filterTable.getSelectionIndex() < filterTable.getItemCount() - 1);

      _btnRename.setEnabled(filterItem != null && filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);
      _btnRemove.setEnabled(filterItem != null && filterItem.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET);
   }

   private void expandCollapseFolder(final TVIPrefTagCategory treeItem) {

      if (_tagViewer.getExpandedState(treeItem)) {

         // collapse folder

         _tagViewer.collapseToLevel(treeItem, 1);
      }
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
   public void init(final IWorkbench workbench) {
//      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _imgTag = TourbookPlugin.getImageDescriptor(Images.Tag).createImage();
      _imgTagRoot = TourbookPlugin.getImageDescriptor(Images.Tag_Root).createImage();
      _imgTagCategory = TourbookPlugin.getImageDescriptor(Images.Tag_Category).createImage();

      parent.addDisposeListener(disposeEvent -> onDisposeSlideout());

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeProperty());

      _defaultMouseWheelListener = mouseEvent -> {
         net.tourbook.common.UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeProperty();
      };
   }

   @Override
   public boolean isValid() {

      saveState();

      return true;
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

   private void onDeleteFilterSet() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _tagGroupViewer.getSelection())
            .getFirstElement();

      if (filterItem == null || filterItem.getFilterType() != TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
         return;
      }

      final Table filterTable = _tagGroupViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      _tagGroupViewer.remove(filterItem);

      // select next filter item
      final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
      _tagGroupViewer.setSelection(new StructuredSelection(_tagGroupViewer.getElementAt(nextIndex)));

      _isModified = true;
   }

   private void onDisposeSlideout() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();

//      saveState();
   }

   private void onMoveDown() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _tagGroupViewer.getSelection())
            .getFirstElement();

      if (filterItem == null) {
         return;
      }

      final Table filterTable = _tagGroupViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      if (selectionIndex < filterTable.getItemCount() - 1) {

         _tagGroupViewer.remove(filterItem);
         _tagGroupViewer.insert(filterItem, selectionIndex + 1);

         // reselect moved item
         _tagGroupViewer.setSelection(new StructuredSelection(filterItem));

         if (filterTable.getSelectionIndex() == filterTable.getItemCount() - 1) {
            _btnUp.setFocus();
         } else {
            _btnDown.setFocus();
         }

         _isModified = true;
      }
   }

   private void onMoveUp() {

      final TourTypeFilter filterItem = (TourTypeFilter) ((IStructuredSelection) _tagGroupViewer.getSelection())
            .getFirstElement();

      if (filterItem == null) {
         return;
      }

      final Table filterTable = _tagGroupViewer.getTable();

      final int selectionIndex = filterTable.getSelectionIndex();
      if (selectionIndex > 0) {
         _tagGroupViewer.remove(filterItem);
         _tagGroupViewer.insert(filterItem, selectionIndex - 1);

         // reselect moved item
         _tagGroupViewer.setSelection(new StructuredSelection(filterItem));

         if (filterTable.getSelectionIndex() == 0) {
            _btnDown.setFocus();
         } else {
            _btnUp.setFocus();
         }

         _isModified = true;
      }
   }

   private void onNewFilterSet() {

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypeFilter_dlg_new_title,
            Messages.Pref_TourTypeFilter_dlg_new_message,
            UI.EMPTY_STRING,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      // create new filterset
      final TourTypeFilterSet filterSet = new TourTypeFilterSet();
      filterSet.setName(inputDialog.getValue().trim());

      final TourTypeFilter tourTypeFilter = new TourTypeFilter(filterSet);

      // update model and viewer
      _tagGroupViewer.add(tourTypeFilter);
      _filterList.add(tourTypeFilter);

      // select new set
      _tagGroupViewer.setSelection(new StructuredSelection(tourTypeFilter), true);

//      _tourTypeViewer.getTable().setFocus();

      _isModified = true;
   }

   private void onRenameFilterSet() {

      final TourTypeFilter filter = (TourTypeFilter) ((StructuredSelection) _tagGroupViewer.getSelection())
            .getFirstElement();

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.Pref_TourTypeFilter_dlg_rename_title,
            Messages.Pref_TourTypeFilter_dlg_rename_message,
            filter.getFilterName(),
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      // update model
      filter.setName(inputDialog.getValue().trim());

      // update viewer
      _tagGroupViewer.update(filter, null);

      _isModified = true;
   }

   private void onSelectFilter() {

//      final TourTypeFilter filterItem = (TourTypeFilter) ((StructuredSelection) _tagGroupViewer.getSelection()).getFirstElement();
//
//      if (filterItem == null) {
//         return;
//      }
//
//      _activeFilter = filterItem;
//
//      final int filterType = filterItem.getFilterType();
//
//      Object[] tourTypes;
//      switch (filterType) {
//      case TourTypeFilter.FILTER_TYPE_SYSTEM:
//         final int systemFilter = filterItem.getSystemFilterId();
//         _tourTypeViewer.setAllChecked(systemFilter == TourTypeFilter.SYSTEM_FILTER_ID_ALL);
//         _tourTypeViewer.getTable().setEnabled(false);
//
//         break;
//
//      case TourTypeFilter.FILTER_TYPE_DB:
//         final TourType tourType = filterItem.getTourType();
//         _tourTypeViewer.setCheckedElements(new Object[] { tourType });
//         _tourTypeViewer.getTable().setEnabled(false);
//         break;
//
//      case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
//         _tourTypeViewer.getTable().setEnabled(true);
//         tourTypes = filterItem.getTourTypeSet().getTourTypes();
//         if (tourTypes == null) {
//            _tourTypeViewer.setAllChecked(false);
//         } else {
//            _tourTypeViewer.setCheckedElements(tourTypes);
//         }
//         break;
//
//      default:
//         break;
//      }

      enableButtons();
   }

   private void onSelectTourType() {

//      if (_activeFilter == null) {
//         return;
//      }
//
//      // set tour types for current filter set
//      if (_activeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_TOURTYPE_SET) {
//         _activeFilter.getTourTypeSet().setTourTypes(_tourTypeViewer.getCheckedElements());
//      }
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

      _chkTourTypeContextMenu.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU));
      _spinnerRecentTourTypes.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES));

      super.performDefaults();

      // this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
   }

   @Override
   public boolean performOk() {

      return isValid();
   }

   private void restoreState() {

      _chkTourTypeContextMenu.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU));
      _spinnerRecentTourTypes.setSelection(_prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES));
   }

   private void saveState() {

      if (_isModified) {

         _isModified = false;

         TourTypeFilterManager.writeXMLFilterFile(_tagGroupViewer);

         _prefStore.setValue(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU, _chkTourTypeContextMenu.getSelection());
         _prefStore.setValue(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES, _spinnerRecentTourTypes.getSelection());

         // fire modify event
         _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
      }
   }

   private void update_FromTagViewer() {

//      if (_selectedProfile == null) {
//         return;
//      }
//
//      final long[] tagIds_Checked = getTagIds_FromTagViewer();
//      final long[] tagIds_Unchecked = getTagIds_FromTagCloud_Unchecked();
//
//      updateTags_TagProfile(_selectedProfile, tagIds_Checked, tagIds_Unchecked);
//      updateTags_TagCloud(tagIds_Checked, tagIds_Unchecked);
//
//      enableControls();
//
//      fireModifyEvent();
   }

   private void updateTagModel() {

      _tagViewerRootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
      _tagViewer.setInput(this);

      loadAllTagItems();
   }

   private void updateViewers() {

      _filterList = TourTypeFilterManager.readTourTypeFilters();
      _tourTypes = TourDatabase.getAllTourTypes();

      // show contents in the viewers
      _tagGroupViewer.setInput(new Object());

      // load tag viewer
      updateTagModel();

      enableButtons();
   }

}
