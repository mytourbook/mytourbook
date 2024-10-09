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
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandAll;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ColumnWeightData;
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
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class PrefPageTagGroups extends PreferencePage implements IWorkbenchPreferencePage, ITreeViewer {

   public static final String  ID                                        = "net.tourbook.preferences.PrefPageTagGroups"; //$NON-NLS-1$

   private static final String STATE_IS_HIERARCHICAL_LAYOUT              = "STATE_IS_HIERARCHICAL_LAYOUT";               //$NON-NLS-1$
   private static final String STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED = "STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED";  //$NON-NLS-1$
   private static final String STATE_SELECTED_TAG_GROUP                  = "STATE_SELECTED_TAG_GROUP";                   //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final IDialogSettings      _state      = TourbookPlugin.getState("net.tourbook.preferences.PrefPageTagGroups");   //$NON-NLS-1$
   private static final IPreferenceStore     _prefStore  = TourbookPlugin.getDefault().getPreferenceStore();

// SET_FORMATTING_ON

   private IPropertyChangeListener           _prefChangeListener;
   private ITourEventListener                _tourEventListener;

   private TableViewer                       _tagGroupViewer;

   private ContainerCheckedTreeViewer        _tagViewer;
   private TVIPrefTagRoot                    _tagViewerRootItem;
   private TagFilter                         _tagFilter                               = new TagFilter();

   private boolean                           _tagViewerItem_IsChecked;
   private boolean                           _tagViewerItem_IsKeyPressed;
   private Object                            _tagViewerItem_Data;

   private long                              _expandRunnableCounter;
   private boolean                           _isBehaviourSingleExpandedOthersCollapse = true;
   private boolean                           _isBehaviourAutoExpandCollapse           = true;
   private boolean                           _isExpandingSelection;
   private boolean                           _isHierarchicalLayout;
   private boolean                           _isInCollapseAll;
   private boolean                           _isModified;
   private boolean                           _isShowOnlyCheckedTags;

   private TagGroup                          _selectedTagGroup;

   private ActionCollapseAllWithoutSelection _actionCollapseAll;
   private ActionExpandAll                   _actionExpandAll;
   private ActionTag_Filter                  _actionTag_Filter;
   private ActionTag_Layout                  _actionTag_Layout;

   /*
    * UI controls
    */
   private Composite _containerTagGroupHeader;

   private ToolBar   _toolBarAllTags;

   private Button    _btnNew;
   private Button    _btnRename;
   private Button    _btnDelete;

   private Label     _lblActionSpacer;

   private Image     _imgTag;
   private Image     _imgTagRoot;
   private Image     _imgTagCategory;

   private Composite _parent;

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

   private class ActionTag_Filter extends Action {

      public ActionTag_Filter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText(Messages.Tour_Tags_Action_TagCheckFilter_OnlyTaggedTours_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagsChecked));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagsChecked_Disabled));
      }

      @Override
      public void run() {
         onTag_Filter();
      }
   }

   private class ActionTag_Layout extends Action {

      ActionTag_Layout() {

         super(Messages.Tour_Tags_Action_Layout_Flat_Tooltip, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat));
      }

      @Override
      public void run() {

         onTag_Layout();
      }
   }

   private class TagFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_selectedTagGroup == null) {

            return false;
         }

         if (element instanceof TVIPrefTag) {

            final TVIPrefTag tviTag = (TVIPrefTag) element;

            final TourTag tourTag = tviTag.getTourTag();

            if (_selectedTagGroup.tourTags.contains(tourTag)) {
               return true;
            }
         }

         return false;
      }
   }

   private class TagGroupViewer_Comparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final TagGroup group1 = (TagGroup) e1;
         final TagGroup group2 = (TagGroup) e2;

         return group1.name.compareTo(group2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private final class TagGroupViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

         return TagGroupManager.getTagGroups().toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer,
                               final Object oldInput,
                               final Object newInput) {}
   }

   /**
    * Sort the tags and categories
    */
   private static final class TagViewer_Comparator extends ViewerComparator {
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

   private final class TagViewer_ContentProvider implements ITreeContentProvider {

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
      public void inputChanged(final Viewer viewer,
                               final Object oldInput,
                               final Object newInput) {}
   }

   public PrefPageTagGroups() {}

   public PrefPageTagGroups(final String title) {
      super(title);
   }

   public PrefPageTagGroups(final String title, final ImageDescriptor image) {
      super(title, image);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         if (propertyChangeEvent.getProperty().equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            updateViewers_All();

            enableControls();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (workbenchPart, tourEventId, eventData) -> {

         if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED) {

            // reload tag viewer

            updateViewers_Tags();

            // reselect tags
            if (_selectedTagGroup != null) {

               updateUI_TagViewer(_selectedTagGroup.tourTags);
            }

            enableControls();
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _actionExpandAll = new ActionExpandAll(this);
      _actionCollapseAll = new ActionCollapseAllWithoutSelection(this);
      _actionTag_Filter = new ActionTag_Filter();
      _actionTag_Layout = new ActionTag_Layout();
   }

   @Override
   protected Control createContents(final Composite parent) {

      _parent = parent;

      initUI();

      restoreStateBeforeUI();

      final Composite ui = createUI(parent);

      createActions();
      fillToolbar();

      // this must be called AFTER the toolbar is created and filled !!!
      updateUI_TopGridRowHeight();

      addPrefListener();
      addTourEventListener();

      updateViewers_All();

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
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TagGroup tagGroup = ((TagGroup) cell.getElement());

            final StyledString styledString = new StyledString();

            styledString.append(tagGroup.name, net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);
            styledString.append(UI.SPACE3 + tagGroup.tourTags.size(), net.tourbook.ui.UI.TOTAL_STYLER);

            final String text = styledString.getString();

            if (UI.IS_SCRAMBLE_DATA) {
//               text = UI.scrambleText(text);
            }

            cell.setText(text);
            cell.setStyleRanges(styledString.getStyleRanges());

         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(1));

      _tagGroupViewer.setUseHashlookup(true);
      _tagGroupViewer.setContentProvider(new TagGroupViewer_ContentProvider());
      _tagGroupViewer.setComparator(new TagGroupViewer_Comparator());

      _tagGroupViewer.addSelectionChangedListener(selectionChangedEvent -> onTagGroup_Select());
      _tagGroupViewer.addDoubleClickListener(doubleClickEvent -> onTagGroup_Rename());
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
      _tagViewer.setContentProvider(new TagViewer_ContentProvider());
      _tagViewer.setComparator(new TagViewer_Comparator());

      _tagViewer.addCheckStateListener(checkStateChangedEvent -> updateUI_Tags_From_TagIds());
      _tagViewer.addSelectionChangedListener(selectionChangedEvent -> onTag_Select(selectionChangedEvent));

      /*
       * Create columns
       */
      TreeViewerColumn tvc;
      TreeColumn tvcColumn;

      // column: tag categories + tags
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
                     categoryString = UI.SLASH + categoryCounter;
                  }
                  styledString.append(UI.SPACE3 + tagCounter + categoryString, net.tourbook.ui.UI.TOTAL_STYLER);
               }

            } else {
               styledString.append(element.toString());
            }

            String text = styledString.getString();

            if (UI.IS_SCRAMBLE_DATA) {
               text = UI.scrambleText(text);
            }

            cell.setText(text);
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
         _btnNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onTagGroup_New()));
         setButtonLayoutData(_btnNew);

         // button: rename
         _btnRename = new Button(container, SWT.NONE);
         _btnRename.setText(Messages.Pref_TourTypeFilter_button_rename);
         _btnRename.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onTagGroup_Rename()));
         setButtonLayoutData(_btnRename);

         // button: delete
         _btnDelete = new Button(container, SWT.NONE);
         _btnDelete.setText(Messages.App_Action_Delete_WithConfirm);
         _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onTagGroup_Delete()));
         setButtonLayoutData(_btnDelete);
      }
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   private void enableControls() {

      final TagGroup tagGroup = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();

      final boolean areTagsAvailable = TagGroupManager.getTagGroups().size() > 0;
      final boolean isGroupSelected = tagGroup != null;
      final boolean isShowAllTags = _isShowOnlyCheckedTags == false;

// SET_FORMATTING_OFF

      _btnRename.setEnabled(isGroupSelected);
      _btnDelete.setEnabled(isGroupSelected);

      _actionCollapseAll            .setEnabled(_isHierarchicalLayout);
      _actionExpandAll              .setEnabled(_isHierarchicalLayout);
      _actionTag_Filter             .setEnabled(areTagsAvailable);
      _actionTag_Layout             .setEnabled(areTagsAvailable && isShowAllTags);

// SET_FORMATTING_ON

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
       * Toolbar: All tags
       */
      final ToolBarManager tbmAllTags = new ToolBarManager(_toolBarAllTags);

      tbmAllTags.add(_actionTag_Filter);
      tbmAllTags.add(_actionTag_Layout);
      tbmAllTags.add(_actionExpandAll);
      tbmAllTags.add(_actionCollapseAll);

      tbmAllTags.update(true);
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
   public void init(final IWorkbench workbench) {

      noDefaultAndApplyButton();
   }

   private void initUI() {

      _imgTag = TourbookPlugin.getImageDescriptor(Images.Tag).createImage();
      _imgTagRoot = TourbookPlugin.getImageDescriptor(Images.Tag_Root).createImage();
      _imgTagCategory = TourbookPlugin.getImageDescriptor(Images.Tag_Category).createImage();

      _parent.addDisposeListener(disposeEvent -> onDispose());
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

   @Override
   public boolean okToLeave() {

      saveChanges();

      return true;
   }

   private void onDispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();
   }

   private void onTag_Filter() {

      // toggle tag filter
      _isShowOnlyCheckedTags = !_isShowOnlyCheckedTags;

      updateUI_TagsAfterFiltering();
   }

   /**
    */
   private void onTag_Layout() {

      // toggle layout
      _isHierarchicalLayout = !_isHierarchicalLayout;

      updateUI_TagLayoutAction();
      updateViewers_Tags();

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

         updateUI_Tags_From_TagIds();

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

   private void onTagGroup_Delete() {

      final TagGroup tagGroup = (TagGroup) _tagGroupViewer.getStructuredSelection().getFirstElement();

      if (tagGroup == null) {
         return;
      }

      if (new MessageDialog(

            _parent.getShell(),

            "Delete Tag Group",
            null, // no title image

            "Delete selected tag group \"%s\" ?".formatted(tagGroup.name),
            MessageDialog.CONFIRM,

            0, // default index

            Messages.App_Action_Delete,
            Messages.App_Action_Cancel

      ).open() != IDialogConstants.OK_ID) {

         return;
      }

      _isModified = true;

      // update model
      TagGroupManager.removeTagGroup(tagGroup);

      // update UI
      final Table groupTable = _tagGroupViewer.getTable();
      final int selectionIndex = groupTable.getSelectionIndex();

      _tagGroupViewer.remove(tagGroup);

      // select next tag group
      final int nextIndex = Math.min(groupTable.getItemCount() - 1, selectionIndex);

      if (nextIndex >= 0) {

         _tagGroupViewer.setSelection(new StructuredSelection(_tagGroupViewer.getElementAt(nextIndex)));

      } else {

         // all groups are deleted

         _selectedTagGroup = null;

         // uncheck all tags
         _tagViewer.setCheckedElements(new Object[0]);

         // deselect tag filter
         _isShowOnlyCheckedTags = false;

         updateUI_TagsAfterFiltering();

         enableControls();
      }
   }

   private void onTagGroup_New() {

      final InputDialog inputDialog = new InputDialog(getShell(),
            "New Tag Group",
            "Tag group name",
            UI.EMPTY_STRING,
            getNameValidator());

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // create new tag group
      final TagGroup tagGroup = new TagGroup();
      tagGroup.name = inputDialog.getValue().trim();

      // update model
      TagGroupManager.addTagGroup(tagGroup);

      // update UI
      _tagGroupViewer.setInput(new Object());

      // select new group
      _tagGroupViewer.setSelection(new StructuredSelection(tagGroup), true);

      _tagViewer.getTree().setFocus();
   }

   private void onTagGroup_Rename() {

      final TagGroup tagGroup = (TagGroup) (_tagGroupViewer.getStructuredSelection()).getFirstElement();

      final InputDialog inputDialog = new InputDialog(getShell(),
            "Rename Tag Group",
            "Name for the tag group",
            tagGroup.name,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // update model
      tagGroup.name = inputDialog.getValue().trim();

      // update UI
      _tagGroupViewer.refresh();

      _tagGroupViewer.setSelection(new StructuredSelection(_selectedTagGroup), true);
      _tagGroupViewer.getTable().setFocus();

      // reselect group tags
      onTagGroup_Select();
   }

   private void onTagGroup_Select() {

      final TagGroup tagGroup = (TagGroup) (_tagGroupViewer.getStructuredSelection()).getFirstElement();

      if (tagGroup == null) {
         return;
      }

      _selectedTagGroup = tagGroup;

      _isModified = true;

      updateUI_TagViewer(tagGroup.tourTags);

      enableControls();
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
   public boolean performCancel() {

      saveState();

      return true;
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

      saveChanges();

      return true;
   }

   private void restoreState() {

      /*
       * Tag filter
       */
      _actionTag_Filter.setChecked(_isShowOnlyCheckedTags);

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

      updateUI_TagFilter();
      updateUI_TagLayoutAction();
   }

   private void restoreStateBeforeUI() {

      _isHierarchicalLayout = Util.getStateBoolean(_state, STATE_IS_HIERARCHICAL_LAYOUT, true);
      _isShowOnlyCheckedTags = Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, false);
   }

   private void saveChanges() {

      if (_isModified) {

         _isModified = false;

         TagGroupManager.saveState();
      }

      saveState();
   }

   private void saveState() {

      _state.put(STATE_IS_HIERARCHICAL_LAYOUT, _isHierarchicalLayout);
      _state.put(STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, _isShowOnlyCheckedTags);

      if (_selectedTagGroup != null) {

         _state.put(STATE_SELECTED_TAG_GROUP, _selectedTagGroup.name);
      }
   }

   /**
    * Set tag viewer filter which will refilter it
    */
   private void updateUI_TagFilter() {

      if (_isShowOnlyCheckedTags) {

         _actionTag_Filter.setToolTipText(Messages.Tour_Tags_Action_TagCheckFilter_AllTags_Tooltip);

         _tagViewer.setFilters(_tagFilter);

      } else {

         _actionTag_Filter.setToolTipText(Messages.Tour_Tags_Action_TagCheckFilter_OnlyTaggedTours_Tooltip);

         _tagViewer.setFilters();
      }
   }

   private void updateUI_TagLayoutAction() {

      if (_isHierarchicalLayout) {

         // hierarchy is displayed -> show icon/tooltip for flat view

         _actionTag_Layout.setToolTipText(Messages.Tour_Tags_Action_Layout_Flat_Tooltip);

         _actionTag_Layout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat));
         _actionTag_Layout.setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Flat_Disabled));

      } else {

         // flat view is displayed -> show icon/tooltip for hierarchy view

         _actionTag_Layout.setToolTipText(Messages.Tour_Tags_Action_Layout_Hierarchical_Tooltip);

         _actionTag_Layout.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Hierarchical));
         _actionTag_Layout.setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TagLayout_Hierarchical_Disabled));
      }
   }

   private void updateUI_Tags_From_TagIds() {

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

      // update model
      _selectedTagGroup.tourTags.clear();
      _selectedTagGroup.tourTags.addAll(allCheckedTourTags);

      // update UI
      _tagGroupViewer.update(_selectedTagGroup, null);

      _isModified = true;

      enableControls();
   }

   private void updateUI_TagsAfterFiltering() {

      _parent.setRedraw(false);
      {
         if (_isShowOnlyCheckedTags && _isHierarchicalLayout) {

            // tag viewer must not display a tree -> show flat viewer

            updateUI_TagLayoutAction();

            onTag_Layout();

         } else {

            updateUI_TagFilter();

            // tags in the tree hierarchy must be rechecked otherwise they are not checked
            _parent.getDisplay().asyncExec(() -> updateUI_Tags_From_TagIds());
         }
      }
      _parent.setRedraw(true);
   }

   private void updateUI_TagViewer(final Set<TourTag> allTourTags) {

      final List<TVIPrefTag> allTagItems = new ArrayList<>(allTourTags.size());

      if (allTourTags.size() > 0) {

         // get all tag viewer items which should be checked

         final List<TreeViewerItem> allRootItems = _tagViewerRootItem.getFetchedChildren();

         for (final TourTag tag : allTourTags) {

            // Is recursive !!!
            getTagItems(allRootItems, allTagItems, tag.getTagId());
         }
      }

      _parent.setRedraw(false);
      {

         if (_isShowOnlyCheckedTags) {

            // show all tags -> disable filter

            _tagViewer.setFilters();
         }

         /**
          * !!! VERY IMPORTANT !!!
          * <p>
          * <p>
          * <p>
          * Uncheck all, otherwise a second selection hides the grayed state
          */
         _tagViewer.setCheckedElements(new Object[0]);

         // update UI
         _tagViewer.setCheckedElements(allTagItems.toArray());

         if (_isShowOnlyCheckedTags) {

            // enable filter

            _tagViewer.setFilters(_tagFilter);
         }
      }
      _parent.setRedraw(true);
   }

   private void updateUI_TopGridRowHeight() {

      final Point toolbarSize = _toolBarAllTags.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      final int toolbarHeight = toolbarSize.y;

      GridData gd = (GridData) _lblActionSpacer.getLayoutData();
      gd.heightHint = toolbarHeight;

      gd = (GridData) _containerTagGroupHeader.getLayoutData();
      gd.heightHint = toolbarHeight;
   }

   private void updateViewers_All() {

      _tagGroupViewer.setInput(new Object());

      updateViewers_Tags();
   }

   /**
    * Load tag viewer
    */
   private void updateViewers_Tags() {

      _tagViewerRootItem = new TVIPrefTagRoot(_tagViewer, _isHierarchicalLayout);
      _tagViewer.setInput(this);

      loadAllTagItems();
   }

}
