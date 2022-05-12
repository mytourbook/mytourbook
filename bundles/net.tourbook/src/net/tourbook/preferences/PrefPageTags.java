/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.MouseListener.mouseDownAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.ITreeViewer;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.Dialog_TourTag;
import net.tourbook.tag.Dialog_TourTag_Category;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tag.TVIPrefTagCategory;
import net.tourbook.tag.TVIPrefTagRoot;
import net.tourbook.tag.TagManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.action.ActionCollapseAll;
import net.tourbook.ui.action.ActionExpandSelection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageTags extends PreferencePage implements IWorkbenchPreferencePage, ITourViewer, ITreeViewer {

   public static final String       ID            = "net.tourbook.preferences.PrefPageTags"; //$NON-NLS-1$

   private static final String      SORT_PROPERTY = "sort";                                  //$NON-NLS-1$

   private final IPreferenceStore   _prefStore    = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener  _prefChangeListener;

   private TreeViewer               _tagViewer;
   private TVIPrefTagRoot           _rootItem;

   private ActionCollapseAll        _action_CollapseAll;
   private Action_DeleteTag         _action_DeleteTag;
   private Action_DeleteTagCategory _action_DeleteCategory;
   private ActionExpandSelection    _action_ExpandSelection;

   private boolean                  _isMouseContextMenu;
   private boolean                  _isModified   = false;
   private boolean                  _isSelectedWithKeyboard;

   private long                     _dragStartTime;

   /*
    * Image resources
    */
   private Image _imgTag         = TourbookPlugin.getImageDescriptor(Images.Tag).createImage();
   private Image _imgTagRoot     = TourbookPlugin.getImageDescriptor(Images.Tag_Root).createImage();
   private Image _imgTagCategory = TourbookPlugin.getImageDescriptor(Images.Tag_Category).createImage();

   /*
    * UI controls
    */
   private ToolBar _toolBar;

   private Button  _btnDuplicateTag;
   private Button  _btnEditTagOrCategory;
   private Button  _btnNewTag;
   private Button  _btnNewTagCategory;
   private Button  _btnReset;

   private class Action_DeleteTag extends Action {

      Action_DeleteTag() {

         super(Messages.Action_Tag_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
      }

      @Override
      public void run() {
         onAction_DeleteTagOrCategory();
      }
   }

   private class Action_DeleteTagCategory extends Action {

      Action_DeleteTagCategory() {

         super(Messages.Action_Tag_DeleteCategory, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
         setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
      }

      @Override
      public void run() {
         onAction_DeleteTagOrCategory();
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

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    * <br>
    * The comparer is necessary to set and restore the expanded elements <br>
    * <br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    */
   private class TagViewerComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {

            return true;

         } else if (a instanceof TVIPrefTag && b instanceof TVIPrefTag) {

            return ((TVIPrefTag) a).getTourTag().getTagId() == ((TVIPrefTag) b).getTourTag().getTagId();

         } else if (a instanceof TVIPrefTagCategory && b instanceof TVIPrefTagCategory) {

            return ((TVIPrefTagCategory) a).getTourTagCategory().getCategoryId() == ((TVIPrefTagCategory) b).getTourTagCategory().getCategoryId();
         }

         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return 0;
      }

   }

   private final class TagViewerContentProvider implements ITreeContentProvider {

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

   public PrefPageTags() {}

   public PrefPageTags(final String title) {
      super(title);
   }

   public PrefPageTags(final String title, final ImageDescriptor image) {
      super(title, image);
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _tagViewer.getTree().setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tagViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not display the new color
             */
            _tagViewer.getTree().redraw();
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void createActions() {

      _action_DeleteTag = new Action_DeleteTag();
      _action_DeleteCategory = new Action_DeleteTagCategory();

      _action_ExpandSelection = new ActionExpandSelection(this, true);
      _action_CollapseAll = new ActionCollapseAll(this);
   }

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      createActions();
      fillToolbar();

      updateTagViewer();
      enableControls();
      addPrefListener();

      return ui;
   }

   private void createNewTourTag(final String tagName, final String tagNotes) {

      TourTag savedTag = null;

      // create new tour tag + item
      final TourTag tourTag = new TourTag(tagName);
      tourTag.setNotes(tagNotes);
      final TVIPrefTag tagItem = new TVIPrefTag(_tagViewer, tourTag);

      final Object parentItem = ((StructuredSelection) _tagViewer.getSelection()).getFirstElement();
      if (parentItem == null) {

         // a parent is not selected, this will be a root tag

         tourTag.setRoot(true);

         /*
          * Update model
          */
         tagItem.setParentItem(_rootItem);
         _rootItem.getFetchedChildren().add(tagItem);

         // persist tag
         savedTag = TourDatabase.saveEntity(tourTag, TourDatabase.ENTITY_IS_NOT_SAVED, TourTag.class);

         if (savedTag != null) {

            // update item
            tagItem.setTourTag(savedTag);

            /*
             * Update UI
             */
            _tagViewer.add(_rootItem, tagItem);
         }

      } else if (parentItem instanceof TVIPrefTagCategory) {

         // parent is a category

         final TVIPrefTagCategory parentCategoryItem = (TVIPrefTagCategory) parentItem;
         TourTagCategory parentTagCategory = parentCategoryItem.getTourTagCategory();

         /*
          * update model
          */

         // set parent into tag
         tagItem.setParentItem(parentCategoryItem);

         /*
          * persist tag without new category otherwise an exception "detached entity passed to
          * persist: net.tourbook.data.TourTagCategory" is raised
          */
         savedTag = TourDatabase.saveEntity(tourTag, TourDatabase.ENTITY_IS_NOT_SAVED, TourTag.class);
         if (savedTag != null) {

            // update item
            tagItem.setTourTag(savedTag);

            // update parent category
            final EntityManager em = TourDatabase.getInstance().getEntityManager();
            {

               final TourTagCategory parentTagCategoryEntity = em.find(
                     TourTagCategory.class,
                     parentTagCategory.getCategoryId());

               // set new entity
               parentTagCategory = parentTagCategoryEntity;
               parentCategoryItem.setTourTagCategory(parentTagCategoryEntity);

               // set tag into parent category
               final Set<TourTag> lazyTourTags = parentTagCategoryEntity.getTourTags();
               lazyTourTags.add(tourTag);

               parentTagCategory.setTagCounter(lazyTourTags.size());
            }
            em.close();

            // persist parent category
            final TourTagCategory savedParent = TourDatabase.saveEntity(
                  parentTagCategory,
                  parentTagCategory.getCategoryId(),
                  TourTagCategory.class);

            if (savedParent != null) {

               // update item
               parentCategoryItem.setTourTagCategory(savedParent);

               // set category in tag,
// this seems to be not necessary
//               tourTag.setTagCategory(parentTagCategory);

               // persist tag with category
               savedTag = TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

            }

         }

         if (savedTag != null) {

            // clear tour tag list
            TourDatabase.clearTourTags();

            /*
             * update viewer
             */
            parentCategoryItem.clearChildren();

            _tagViewer.add(parentCategoryItem, tagItem);
            _tagViewer.update(parentCategoryItem, null);

            _tagViewer.expandToLevel(parentCategoryItem, 1);
         }
      }

      if (savedTag != null) {

         // show new tag in viewer
         _tagViewer.reveal(tagItem);

         _isModified = true;

         fireModifyEvent();
      }

      setFocusToViewer();
   }

   private Composite createUI(final Composite parent) {

      // container
      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            //            .spacing(SWT.DEFAULT, 0)
            .numColumns(2)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_10_Title(container);

         createUI_20_TagViewer(container);
         createUI_30_Buttons(container);

         createUI_40_Bottom(container);
      }

      // spacer
      new Label(parent, SWT.NONE);

      return container;
   }

   private void createUI_10_Title(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         // label: title
         final Label label = new Label(container, SWT.WRAP);
         label.setText(Messages.pref_tourtag_viewer_title);
         GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

         // toolbar
         _toolBar = new ToolBar(container, SWT.FLAT);
      }

      // spacer
      new Label(parent, SWT.NONE);
   }

   private void createUI_20_TagViewer(final Composite parent) {

      /*
       * create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(400, 500)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * create viewer
       */
      final Tree tree = new Tree(layoutContainer, SWT.H_SCROLL | SWT.V_SCROLL
//            | SWT.BORDER
            | SWT.MULTI
            | SWT.FULL_SELECTION);

      tree.setHeaderVisible(true);
      tree.setLinesVisible(getPreferenceStore().getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      tree.addListener(SWT.MouseDoubleClick, this::onTagTree_DoubleClick);

      tree.addKeyListener(keyPressedAdapter(keyEvent -> {
         _isSelectedWithKeyboard = true;

         switch (keyEvent.keyCode) {

         case SWT.DEL:
            // delete tag/category only when the delete action is enabled
            if (_action_DeleteTag.isEnabled() || _action_DeleteCategory.isEnabled()) {
               onAction_DeleteTagOrCategory();
            }

            break;

         case SWT.F2:
            onAction_Edit_TagOrCategory();
            break;
         }
      }));

      tree.addMouseListener(mouseDownAdapter(mouseEvent -> _isMouseContextMenu = mouseEvent.button == 3));

      _tagViewer = new TreeViewer(tree);

      _tagViewer.setContentProvider(new TagViewerContentProvider());
      _tagViewer.setComparator(new TagViewerComparator());
      _tagViewer.setComparer(new TagViewerComparer());
      _tagViewer.setUseHashlookup(true);

      _tagViewer.addDoubleClickListener(doubleClickEvent -> onTagViewer_DoubleClick());

      _tagViewer.addSelectionChangedListener(this::onTagViewer_Selection);

      _tagViewer.addDragSupport(
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
                  final ISelection selection = _tagViewer.getSelection();

                  //                  System.out.println("dragStart");
                  transfer.setSelection(selection);
                  _dragStartTime = event.time & 0xFFFFFFFFL;
                  transfer.setSelectionSetTime(_dragStartTime);

                  event.doit = !selection.isEmpty();
               }
            });

      _tagViewer.addDropSupport(
            DND.DROP_MOVE,
            new Transfer[] { LocalSelectionTransfer.getTransfer() },
            new TagDropAdapter(this, _tagViewer));

      defineAllColumns(treeLayout);

      createUI_22_ContextMenu(tree);
   }

   private void createUI_22_ContextMenu(final Tree tree) {

      final MenuManager menuManager = new MenuManager();

      menuManager.setRemoveAllWhenShown(true);

      menuManager.addMenuListener(menuManager1 -> {
         fillContextMenu(menuManager1);
         enableControls();
      });

      tree.setMenu(menuManager.createContextMenu(tree));
   }

   private void createUI_30_Buttons(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().indent(5, 0).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            // Button: new tag

            _btnNewTag = new Button(container, SWT.NONE);
            _btnNewTag.setText(Messages.pref_tourtag_btn_new_tag);
            _btnNewTag.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction_NewTag()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnNewTag);
         }

         {
            // Button: new tag category

            _btnNewTagCategory = new Button(container, SWT.NONE);
            _btnNewTagCategory.setText(Messages.pref_tourtag_btn_new_tag_category);
            _btnNewTagCategory.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction_NewCategory()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnNewTagCategory);
         }

         {
            // Button: edit tag or category

            _btnEditTagOrCategory = new Button(container, SWT.NONE);
            _btnEditTagOrCategory.setText(Messages.Action_TagCategory_EditCategory);
            _btnEditTagOrCategory.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction_Edit_TagOrCategory()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnEditTagOrCategory);
         }

         {
            // Button: Duplicate tag

            _btnDuplicateTag = new Button(container, SWT.NONE);
            _btnDuplicateTag.setText(Messages.PrefPage_TourTag_Button_DuplicateTag);
            _btnDuplicateTag.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction_DuplicateTag()));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_btnDuplicateTag);
         }

         {
            // Button: reset

            _btnReset = new Button(container, SWT.NONE);
            _btnReset.setText(Messages.pref_tourtag_btn_reset);
            _btnReset.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onAction_Reset()));
            GridDataFactory.fillDefaults().grab(true, false).indent(0, 50).applyTo(_btnReset);
         }
      }
   }

   private void createUI_40_Bottom(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         final Label label = new Label(container, SWT.WRAP);
         label.setText(Messages.pref_tourtag_hint);
         GridDataFactory.swtDefaults().grab(true, false).applyTo(label);

         final Link link = new Link(container, SWT.WRAP);
         GridDataFactory.swtDefaults().grab(true, false).applyTo(link);
         link.setText(Messages.Pref_TourTag_Link_AppearanceOptions);
         link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> PreferencesUtil.createPreferenceDialogOn(getShell(),
               PrefPageAppearance.ID,
               null,
               null)));
      }
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

                  String tagName = tourTag.getTagName();
                  if (UI.IS_SCRAMBLE_DATA) {
                     tagName = UI.scrambleText(tagName);
                  }

                  styledString.append(tagName, net.tourbook.ui.UI.TAG_STYLER);
                  cell.setImage(tourTag.isRoot() ? _imgTagRoot : _imgTag);

               } else if (element instanceof TVIPrefTagCategory) {

                  final TVIPrefTagCategory tourTagCategoryItem = (TVIPrefTagCategory) element;
                  final TourTagCategory tourTagCategory = tourTagCategoryItem.getTourTagCategory();

                  cell.setImage(_imgTagCategory);

                  String categoryName = tourTagCategory.getCategoryName();
                  if (UI.IS_SCRAMBLE_DATA) {
                     categoryName = UI.scrambleText(categoryName);
                  }
                  styledString.append(categoryName, net.tourbook.ui.UI.TAG_CATEGORY_STYLER);

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
         treeLayout.setColumnData(tvcColumn, new ColumnWeightData(80, true));
      }
   }

   @Override
   public void dispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void doLiveUpdate() {

      _isModified = true;

      fireModifyEvent();
   }

   private void enableControls() {

      final IStructuredSelection selection = _tagViewer.getStructuredSelection();
      final Object firstElement = selection.getFirstElement();

      /*
       * Count number of selected tours/tags/categories
       */
      int numTags = 0;
      int numCategorys = 0;
      int numOtherItems = 0;

      for (final Object treeItem : selection) {

         if (treeItem instanceof TVIPrefTag) {
            numTags++;
         } else if (treeItem instanceof TVIPrefTagCategory) {
            numCategorys++;
         } else {
            numOtherItems++;
         }
      }

      final boolean isTagSelected = numTags > 0 && numCategorys == 0 && numOtherItems == 0;
      final boolean isCategorySelected = numCategorys == 1 && numTags == 0 && numOtherItems == 0;

      final boolean isSelection = firstElement != null;

      _btnDuplicateTag.setEnabled(isTagSelected && numTags == 1);
      _btnNewTag.setEnabled((isSelection == false || isCategorySelected && isTagSelected == false));
      _btnNewTagCategory.setEnabled((isSelection == false || isCategorySelected && isTagSelected == false));

      final boolean isOneItemSelected = selection.size() == 1;
      if (isOneItemSelected) {

         if (isTagSelected) {

            _btnEditTagOrCategory.setText(Messages.Action_Tag_Edit);
            _btnEditTagOrCategory.setEnabled(true);

         } else if (isCategorySelected) {

            _btnEditTagOrCategory.setText(Messages.Action_TagCategory_EditCategory);
            _btnEditTagOrCategory.setEnabled(true);
         }

      } else {

         _btnEditTagOrCategory.setEnabled(false);
      }

      _btnReset.setEnabled(true);
      _tagViewer.getTree().setEnabled(true);

      _action_CollapseAll.setEnabled(true);
      _action_ExpandSelection.setEnabled(true);

      _action_DeleteTag.setEnabled(isTagSelected);
      _action_DeleteCategory.setEnabled(isOneItemSelected && isCategorySelected);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_action_CollapseAll);
      menuMgr.add(_action_ExpandSelection);

      menuMgr.add(new Separator());
      menuMgr.add(_action_DeleteTag);
      menuMgr.add(_action_DeleteCategory);
   }

   /**
    * set the toolbar action after the {@link #_tagViewer} is created
    */
   private void fillToolbar() {

      final ToolBarManager tbm = new ToolBarManager(_toolBar);

      tbm.add(_action_ExpandSelection);
      tbm.add(_action_CollapseAll);

      tbm.update(true);
   }

   private void fireModifyEvent() {

      if (_isModified) {

         _isModified = false;

         // remove old tags from cached tours
         TourDatabase.clearTourTags();

         TagMenuManager.updateRecentTagNames();

         TourManager.getInstance().clearTourDataCache();

         // fire modify event
         TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      }
   }

   @Override
   public ColumnManager getColumnManager() {
      return null;
   }

   public long getDragStartTime() {
      return _dragStartTime;
   }

   public TVIPrefTagRoot getRootItem() {
      return _rootItem;
   }

   @Override
   public TreeViewer getTreeViewer() {
      return _tagViewer;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tagViewer;
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   @Override
   public boolean isValid() {

      return true;
   }

   private void onAction_DeleteTagOrCategory() {

      final ITreeSelection structuredSelection = _tagViewer.getStructuredSelection();
      final Object firstSelectedItem = structuredSelection.getFirstElement();

      if (firstSelectedItem instanceof TVIPrefTag) {

         // delete tag

         final List<?> allSelection = structuredSelection.toList();

         final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

         final List<TourTag> allSelectedTags = new ArrayList<>();
         for (final Object object : allSelection) {

            if (object instanceof TVIPrefTag) {

               final TVIPrefTag tviTag = (TVIPrefTag) object;

               allSelectedTags.add(allTourTags.get(tviTag.getTourTag().getTagId()));
            }
         }

         if (allSelectedTags.size() > 0) {

            if (TagManager.deleteTourTag(allSelectedTags)) {

               // update tag viewer with new loaded structure
               updateTagViewer();
            }
         }

      } else if (firstSelectedItem instanceof TVIPrefTagCategory) {

         // delete category

         final TourTagCategory tourTagCategory = ((TVIPrefTagCategory) firstSelectedItem).getTourTagCategory();

         if (TagManager.deleteTourTagCategory(tourTagCategory.getCategoryId(), tourTagCategory.getCategoryName())) {

            updateTagViewer();
         }

      }

      setFocusToViewer();
   }

   private void onAction_DuplicateTag() {

      final Object parentItem = ((StructuredSelection) _tagViewer.getStructuredSelection()).getFirstElement();
      final TourTag tagToDuplicate = ((TVIPrefTag) parentItem).getTourTag();

      //Deselecting the current selection so that the duplicate tag is a root tag
      _tagViewer.setSelection(null);

      createNewTourTag(tagToDuplicate.getTagName() + UI.SPACE + Messages.PrefPage_TourTag_Label_Duplicate,
            tagToDuplicate.getNotes());
   }

   /**
    * Edit selected tag/category
    */
   private void onAction_Edit_TagOrCategory() {

      final Object firstElement = _tagViewer.getStructuredSelection().getFirstElement();

      String dlgMessage;

      if (firstElement instanceof TVIPrefTag) {

         final TVIPrefTag tourTagItem = ((TVIPrefTag) firstElement);
         final TourTag tourTag = tourTagItem.getTourTag();

         dlgMessage = NLS.bind(Messages.Dialog_TourTag_EditTag_Message, tourTag.getTagName());

         if (new Dialog_TourTag(getShell(), dlgMessage, tourTag).open() != Window.OK) {

            setFocusToViewer();

            return;
         }

         // update model
         TourDatabase.saveEntity(tourTag, tourTag.getTagId(), TourTag.class);

         // update UI
         _tagViewer.update(tourTagItem, new String[] { SORT_PROPERTY });

      } else if (firstElement instanceof TVIPrefTagCategory) {

         final TVIPrefTagCategory tagCategoryItem = (TVIPrefTagCategory) firstElement;
         final TourTagCategory tourTagCategory = tagCategoryItem.getTourTagCategory();

         dlgMessage = NLS.bind(Messages.Dialog_TourTagCategory_EditCategory_Message, tourTagCategory.getCategoryName());

         if (new Dialog_TourTag_Category(getShell(), dlgMessage, tourTagCategory).open() != Window.OK) {

            setFocusToViewer();

            return;
         }

         // update model
         TourDatabase.saveEntity(tourTagCategory, tourTagCategory.getCategoryId(), TourTagCategory.class);

         // update UI
         _tagViewer.update(tagCategoryItem, new String[] { SORT_PROPERTY });

      } else {

         return;
      }

      _isModified = true;

      setFocusToViewer();
   }

   private void onAction_NewCategory() {

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.pref_tourtag_dlg_new_tag_category_title,
            Messages.pref_tourtag_dlg_new_tag_category_message,
            UI.EMPTY_STRING,
            null);

      if (inputDialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      // create tag category + tree item
      final TourTagCategory newCategory = new TourTagCategory(inputDialog.getValue().trim());
      final TVIPrefTagCategory newCategoryItem = new TVIPrefTagCategory(_tagViewer, newCategory);

      final Object parentElement = ((StructuredSelection) _tagViewer.getSelection()).getFirstElement();
      TourTagCategory savedNewCategory = null;

      if (parentElement == null) {

         // a parent is not selected, this will be a root category

         newCategory.setRoot(true);

         /*
          * Update model
          */
         _rootItem.getFetchedChildren().add(newCategoryItem);

         // persist new category
         savedNewCategory = TourDatabase.saveEntity(newCategory, newCategory.getCategoryId(), TourTagCategory.class);
         if (savedNewCategory != null) {

            // update item
            newCategoryItem.setTourTagCategory(savedNewCategory);

            /*
             * Update UI
             */
            _tagViewer.add(_rootItem, newCategoryItem);
         }

      } else if (parentElement instanceof TVIPrefTagCategory) {

         // parent is a category

         final TVIPrefTagCategory parentCategoryItem = (TVIPrefTagCategory) parentElement;
         final TourTagCategory parentCategory = parentCategoryItem.getTourTagCategory();

         /*
          * update model
          */

         final EntityManager em = TourDatabase.getInstance().getEntityManager();

         // persist new category
         savedNewCategory = TourDatabase.saveEntity(newCategory, newCategory.getCategoryId(), TourTagCategory.class);
         if (savedNewCategory != null) {

            // update item
            newCategoryItem.setTourTagCategory(savedNewCategory);

            /*
             * Update parent category
             */
            final TourTagCategory parentCategoryEntity = em.find(
                  TourTagCategory.class,
                  parentCategory.getCategoryId());

            // set tag in parent category
            final Set<TourTagCategory> lazyTourTagCategories = parentCategoryEntity.getTagCategories();
            lazyTourTagCategories.add(savedNewCategory);

            // update number of categories
            parentCategoryEntity.setCategoryCounter(lazyTourTagCategories.size());

            /*
             * Persist parent category
             */
            final TourTagCategory savedParentCategory = TourDatabase.saveEntity(
                  parentCategoryEntity,
                  parentCategoryEntity.getCategoryId(),
                  TourTagCategory.class);

            if (savedParentCategory != null) {

               // update item
               parentCategoryItem.setTourTagCategory(savedParentCategory);

               /*
                * Update UI
                */
               parentCategoryItem.clearChildren();

               _tagViewer.add(parentCategoryItem, newCategoryItem);

               _tagViewer.expandToLevel(parentCategoryItem, 1);
            }
         }

         em.close();

      }

      if (savedNewCategory != null) {

         // reveal new tag in viewer
         _tagViewer.reveal(newCategoryItem);

         _isModified = true;
      }

      setFocusToViewer();
   }

   /**
    * <pre>
    *
    * category   --- category
    * category   --- tag
    *          +-- tag
    * category   --- category
    *          +-- category --- tag
    *                    +-- tag
    *          +-- tag
    *          +-- tag
    *          +-- tag
    * tag
    * tag
    * </pre>
    */
   private void onAction_NewTag() {

      final InputDialog inputDialog = new InputDialog(
            getShell(),
            Messages.pref_tourtag_dlg_new_tag_title,
            Messages.pref_tourtag_dlg_new_tag_message,
            UI.EMPTY_STRING,
            null);

      if (inputDialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      createNewTourTag(inputDialog.getValue().trim(), UI.EMPTY_STRING);
   }

   private void onAction_Reset() {

      final MessageDialog dialog = new MessageDialog(
            Display.getCurrent().getActiveShell(),
            Messages.pref_tourtag_dlg_reset_title,
            null,
            Messages.pref_tourtag_dlg_reset_message,
            MessageDialog.QUESTION,
            new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL },
            1);

      if (dialog.open() != Window.OK) {
         setFocusToViewer();
         return;
      }

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         TourLogManager.showLogView();
         TourLogManager.log_TITLE("RESET TAG STRUCTURE"); //$NON-NLS-1$

         {
            String sql;
            int result;

            // remove join table tag->category
            final Statement stmt1 = conn.createStatement();
            {
               sql = "DELETE FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG; //$NON-NLS-1$
               result = stmt1.executeUpdate(sql);
               TourLogManager.log_INFO("Deleted " + result + " entries from " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG); //$NON-NLS-1$ //$NON-NLS-2$
            }
            stmt1.close();

            // remove jointable category<->category
            final Statement stmt2 = conn.createStatement();
            {
               sql = "DELETE FROM " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY; //$NON-NLS-1$
               result = stmt2.executeUpdate(sql);
               TourLogManager.log_INFO("Deleted " + result + " entries from " + TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY); //$NON-NLS-1$ //$NON-NLS-2$
            }
            stmt2.close();

            // set tags to root
            final Statement stmt3 = conn.createStatement();
            {
               sql = "UPDATE " + TourDatabase.TABLE_TOUR_TAG + " SET isRoot=1"; //$NON-NLS-1$ //$NON-NLS-2$
               result = stmt3.executeUpdate(sql);
               TourLogManager.log_INFO("Set " + result + " tour tags to root"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            stmt3.close();

            // set categories to root
            final Statement stmt4 = conn.createStatement();
            {
               sql = "UPDATE " + TourDatabase.TABLE_TOUR_TAG_CATEGORY + " SET isRoot=1"; //$NON-NLS-1$ //$NON-NLS-2$
               result = stmt4.executeUpdate(sql);
               TourLogManager.log_INFO("Set " + result + " tour categories to root"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            stmt4.close();
         }

         // update tag viewer with new loaded structure
         updateTagViewer();

         doLiveUpdate();

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }

      setFocusToViewer();
   }

   private void onTagTree_DoubleClick(final Event event) {

      final boolean isCtrl = (event.stateMask & SWT.CTRL) != 0;

      if (isCtrl) {

         onAction_Edit_TagOrCategory();
      }
   }

   private void onTagViewer_DoubleClick() {

      final Object selection = ((IStructuredSelection) _tagViewer.getSelection()).getFirstElement();

      if (selection instanceof TVIPrefTag) {

         // tag is selected

         onAction_Edit_TagOrCategory();

      } else if (selection instanceof TVIPrefTagCategory) {

         // expand/collapse current item

         final TreeViewerItem tourItem = (TreeViewerItem) selection;

         if (_tagViewer.getExpandedState(tourItem)) {
            _tagViewer.collapseToLevel(tourItem, 1);
         } else {
            _tagViewer.expandToLevel(tourItem, 1);
         }
      }
   }

   private void onTagViewer_Selection(final SelectionChangedEvent event) {

      if (_isMouseContextMenu) {
         return;
      }

      final Object selectedItem = ((IStructuredSelection) (event.getSelection())).getFirstElement();

      if (selectedItem instanceof TVIPrefTag) {

         // a tag is selected

      } else if (selectedItem instanceof TVIPrefTagCategory) {

         // expand/collapse category

         if (_isSelectedWithKeyboard == false) {

            if (_tagViewer.getExpandedState(selectedItem)) {
               _tagViewer.collapseToLevel(selectedItem, 1);
            } else {
               _tagViewer.expandToLevel(selectedItem, 1);
            }
         }
      }

      // reset state
      _isSelectedWithKeyboard = false;

      enableControls();
   }

   @Override
   public boolean performCancel() {

      fireModifyEvent();

      return true;
   }

   @Override
   public boolean performOk() {

      fireModifyEvent();

      return true;
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {
      return null;
   }

   @Override
   public void reloadViewer() {}

   private void setFocusToViewer() {

      // set focus back to the tree
      _tagViewer.getTree().setFocus();
   }

   public void setIsModified() {
      _isModified = true;
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Update tag viewer with new loaded structure
    */
   private void updateTagViewer() {

      final Tree tree = _tagViewer.getTree();
      tree.setRedraw(false);
      {
         final Object[] expandedElements = _tagViewer.getExpandedElements();

         _rootItem = new TVIPrefTagRoot(_tagViewer, true);
         _tagViewer.setInput(_rootItem);
         _tagViewer.setExpandedElements(expandedElements);
      }
      tree.setRedraw(true);

      enableControls();
   }

}
