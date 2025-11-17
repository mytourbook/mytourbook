/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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

import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.equipment.EquipmentGroup;
import net.tourbook.equipment.EquipmentGroupManager;
import net.tourbook.equipment.EquipmentManager;
import net.tourbook.tag.TVIPrefTag;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageEquipmentGroups extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String  ID                                        = "net.tourbook.preferences.PrefPageEquipmentGroups"; //$NON-NLS-1$

   private static final String STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED = "STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED";        //$NON-NLS-1$
   private static final String STATE_SELECTED_TAG_GROUP                  = "STATE_SELECTED_TAG_GROUP";                         //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final IDialogSettings      _state      = TourbookPlugin.getState(ID);
   private static final IPreferenceStore     _prefStore  = TourbookPlugin.getDefault().getPreferenceStore();

// SET_FORMATTING_ON

   private IPropertyChangeListener _prefChangeListener;
   private ITourEventListener      _tourEventListener;

   private TableViewer             _equipmentViewer_Groups;
   private CheckboxTableViewer     _equipmentViewer_Items;

   private List<Equipment>         _allEquipment;

   private EquipmentFilter         _equipmentFilter = new EquipmentFilter();

   /**
    * Complicated, it is necessary to check the checkbox, otherwise another item could be checked
    */
   private boolean                 _tagViewerItem_IsChecked;
   private boolean                 _tagViewerItem_IsKeyPressed;
   private Object                  _tagViewerItem_Data;

   private boolean                 _isModified;
   private boolean                 _isShowOnlyCheckedTags;

   private EquipmentGroup          _selectedEquipmentGroup;

   private ActionEquipment_Filter  _actionEquipment_Filter;

   /*
    * UI controls
    */
   private Composite _containerEquipmentGroupHeader;

   private ToolBar   _toolBarAllTags;

   private Button    _btnCheckAll;
   private Button    _btnDelete;
   private Button    _btnNew;
   private Button    _btnRename;
   private Button    _btnUncheckAll;

   private Label     _lblActionSpacer;

   private Image     _imgTag;
   private Image     _imgTagRoot;
   private Image     _imgTagCategory;

   private Composite _parent;

   private class ActionEquipment_Filter extends Action {

      public ActionEquipment_Filter() {

         super(UI.EMPTY_STRING, AS_CHECK_BOX);

         setToolTipText("Show only equipments which are checked");

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.Equipment_Checked));
      }

      @Override
      public void run() {
         onTag_Filter();
      }
   }

   private class EquipmentFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (_selectedEquipmentGroup == null) {

            return false;
         }

//         if (element instanceof TVIPrefTag) {
//
//            final TVIPrefTag tviTag = (TVIPrefTag) element;
//
//            final TourTag tourTag = tviTag.getTourTag();
//
//            if (_selectedTagGroup.tourTags.contains(tourTag)) {
//               return true;
//            }
//         }

         return false;
      }
   }

   private class EquipmentGroupViewer_Comparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         if (e1 == null || e2 == null) {
            return 0;
         }

         final EquipmentGroup group1 = (EquipmentGroup) e1;
         final EquipmentGroup group2 = (EquipmentGroup) e2;

         return group1.name.compareTo(group2.name);
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }
   }

   private final class EquipmentGroupViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {

         return EquipmentGroupManager.getEquipmentGroups().toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer,
                               final Object oldInput,
                               final Object newInput) {}
   }

   private final class EquipmentViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allEquipment.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public PrefPageEquipmentGroups() {}

   public PrefPageEquipmentGroups(final String title) {
      super(title);
   }

   public PrefPageEquipmentGroups(final String title, final ImageDescriptor image) {
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

      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void createActions() {

      _actionEquipment_Filter = new ActionEquipment_Filter();
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
         createUI_10_Equipment_Groups(container);
         createUI_20_Equipment_Items(container);
         createUI_30_Equipment_Actions(container);
      }

      // spacer
//      UI.createSpacer_Horizontal(parent);

      return container;
   }

   private void createUI_10_Equipment_Groups(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);
      {

         _containerEquipmentGroupHeader = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerEquipmentGroupHeader);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_containerEquipmentGroupHeader);
//         _containerTagGroupHeader.setBackground(UI.SYS_COLOR_CYAN);
         {
            final Label label = UI.createLabel(_containerEquipmentGroupHeader, "Equipment &Groups");
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
//                  .indent(0, 5)
                  .applyTo(label);
//            label.setBackground(UI.SYS_COLOR_YELLOW);
         }

         createUI_12_Equipment_Groups_Viewer(container);
      }
   }

   private void createUI_12_Equipment_Groups_Viewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(70, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(tableLayouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      TableViewerColumn tvc;

      _equipmentViewer_Groups = new TableViewer(table);

      // column: group name
      tvc = new TableViewerColumn(_equipmentViewer_Groups, SWT.NONE);
      tvc.setLabelProvider(new StyledCellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final EquipmentGroup tagGroup = ((EquipmentGroup) cell.getElement());

            final StyledString styledString = new StyledString();

            styledString.append(tagGroup.name, net.tourbook.ui.UI.CONTENT_CATEGORY_STYLER);
            styledString.append(UI.SPACE3 + tagGroup.allEquipment.size(), net.tourbook.ui.UI.TOTAL_STYLER);

            String text = styledString.getString();

            if (UI.IS_SCRAMBLE_DATA) {
               text = UI.scrambleText(text);
            }

            cell.setText(text);
            cell.setStyleRanges(styledString.getStyleRanges());

         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(1));

      _equipmentViewer_Groups.setUseHashlookup(true);
      _equipmentViewer_Groups.setContentProvider(new EquipmentGroupViewer_ContentProvider());
      _equipmentViewer_Groups.setComparator(new EquipmentGroupViewer_Comparator());

      _equipmentViewer_Groups.addSelectionChangedListener(selectionChangedEvent -> onEquipmentGroup_Select());
      _equipmentViewer_Groups.addDoubleClickListener(doubleClickEvent -> onEquipmentGroup_Rename());
   }

   private void createUI_20_Equipment_Items(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_GREEN);

      {
         createUI_22_Equipment_Items_Header(container);
         createUI_24_Equipment_Items_Viewer(container);
      }
   }

   private void createUI_22_Equipment_Items_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         {
            // Label: All Tags
            final Label label = new Label(container, SWT.NONE);
            label.setText("Available &Equipment");
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

   private void createUI_24_Equipment_Items_Viewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(50, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(
            tableLayouter,
            (SWT.CHECK
                  | SWT.SINGLE
                  | SWT.FULL_SELECTION));

      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      table.addKeyListener(KeyListener.keyPressedAdapter(keyEvent -> _tagViewerItem_IsKeyPressed = true));
      table.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentItem_TableSelect(selectionEvent)));

      _equipmentViewer_Items = new CheckboxTableViewer(table);

      _equipmentViewer_Items.setContentProvider(new EquipmentViewer_ContentProvider());

//      _equipmentViewer.addCheckStateListener(event -> onAction_Check(event));
//      _equipmentViewer.addDoubleClickListener(event -> onAction_DoubleClick(event));
      _equipmentViewer_Items.addSelectionChangedListener(event -> onEquipmentItem_ViewerSelect(event));

      defineAllColumn(tableLayouter);
   }

   private void createUI_30_Equipment_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(0, 0).applyTo(container);
      {
         _lblActionSpacer = UI.createSpacer_Horizontal(container);
         GridDataFactory.fillDefaults().applyTo(_lblActionSpacer);

         // button: new
         _btnNew = new Button(container, SWT.NONE);
         _btnNew.setText(Messages.App_Action_New);
         _btnNew.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_New()));
         setButtonLayoutData(_btnNew);

         // button: rename
         _btnRename = new Button(container, SWT.NONE);
         _btnRename.setText(Messages.App_Action_Rename);
         _btnRename.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_Rename()));
         setButtonLayoutData(_btnRename);

         // button: delete
         _btnDelete = new Button(container, SWT.NONE);
         _btnDelete.setText(Messages.App_Action_Delete_WithConfirm);
         _btnDelete.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onEquipmentGroup_Delete()));
         setButtonLayoutData(_btnDelete);
         {
            /*
             * Button: Check all
             */
            _btnCheckAll = new Button(container, SWT.PUSH);
            _btnCheckAll.setText(Messages.App_Action_CheckAll);
            _btnCheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(true)));
            setButtonLayoutData(_btnCheckAll);
         }
         {
            /*
             * Button: Uncheck all
             */
            _btnUncheckAll = new Button(container, SWT.PUSH);
            _btnUncheckAll.setText(Messages.App_Action_UncheckAll);
            _btnUncheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(false)));
            setButtonLayoutData(_btnUncheckAll);
         }
      }
   }

   private void defineAllColumn(final TableLayoutComposite tableLayouter) {

      defineColumn_10_EquipmentName(tableLayouter);
   }

   private void defineColumn_10_EquipmentName(final TableLayoutComposite tableLayouter) {

      TableViewerColumn tvc;

      tvc = new TableViewerColumn(_equipmentViewer_Items, SWT.NONE);

      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Equipment equipment = ((Equipment) cell.getElement());

            cell.setText(equipment.getName());
         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(20));
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);

      super.dispose();
   }

   private void enableControls() {

//      final EquipmentGroup tagGroup = (EquipmentGroup) _equipmentGroupViewer.getStructuredSelection().getFirstElement();
//
//      final boolean areTagsAvailable = EquipmentGroupManager.getTagGroups().size() > 0;
//      final boolean isGroupSelected = tagGroup != null;
//
//// SET_FORMATTING_OFF
//
//      _btnRename.setEnabled(isGroupSelected);
//      _btnDelete.setEnabled(isGroupSelected);
//
//      _actionTag_Filter    .setEnabled(areTagsAvailable);
//
//// SET_FORMATTING_ON
//
//      _equipmentViewer.getTable().setEnabled(isGroupSelected);
   }

   /**
    * Set the toolbar action after the {@link #_equipmentViewer_Items} is created
    */
   private void fillToolbar() {

      /*
       * Toolbar: All tags
       */
      final ToolBarManager tbmAllTags = new ToolBarManager(_toolBarAllTags);

      tbmAllTags.add(_actionEquipment_Filter);

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

      final Object[] checkedElements = _equipmentViewer_Items.getCheckedElements();

      for (final Object object : checkedElements) {

         if (object instanceof TVIPrefTag) {

            final TVIPrefTag tagItem = (TVIPrefTag) object;
            final long tagId = tagItem.getTourTag().getTagId();

            tagIds.add(tagId);
         }
      }

      return tagIds.toArray();
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

   @Override
   public boolean okToLeave() {

      saveChanges();

      return true;
   }

   private void onCheckAll(final boolean isChecked) {

      // update model
//      for (final TourAction tourAction : _allClonedActions) {
//         tourAction.isChecked = isChecked;
//      }

      // update UI
      _equipmentViewer_Items.setAllChecked(isChecked);

      // this is needed that the styler is applied !!!
      _equipmentViewer_Items.refresh();
   }

   private void onDispose() {

      _imgTag.dispose();
      _imgTagRoot.dispose();
      _imgTagCategory.dispose();
   }

   private void onEquipmentGroup_Delete() {

   }

   private void onEquipmentGroup_New() {

      final InputDialog inputDialog = new InputDialog(getShell(),
            "New Equipment Group",
            "Equipment group name",
            UI.EMPTY_STRING,
            getNameValidator());

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // create new tag group
      final EquipmentGroup tagGroup = new EquipmentGroup();
      tagGroup.name = inputDialog.getValue().trim();

      // update model
      EquipmentGroupManager.addEquipmentGroup(tagGroup);

      // update UI
      _equipmentViewer_Groups.setInput(new Object());

      // select new group
      _equipmentViewer_Groups.setSelection(new StructuredSelection(tagGroup), true);

      _equipmentViewer_Items.getTable().setFocus();
   }

   private void onEquipmentGroup_Rename() {

      final EquipmentGroup equipmentGroup = (EquipmentGroup) (_equipmentViewer_Groups.getStructuredSelection()).getFirstElement();

      final InputDialog inputDialog = new InputDialog(getShell(),
            "Rename Tag Group",
            "Equipment group name",
            equipmentGroup.name,
            null);

      inputDialog.open();

      if (inputDialog.getReturnCode() != Window.OK) {
         return;
      }

      _isModified = true;

      // update model
      equipmentGroup.name = inputDialog.getValue().trim();

      // update UI
      _equipmentViewer_Groups.refresh();

      _equipmentViewer_Groups.setSelection(new StructuredSelection(_selectedEquipmentGroup), true);
      _equipmentViewer_Groups.getTable().setFocus();

      // reselect group tags
      onEquipmentGroup_Select();
   }

   private void onEquipmentGroup_Select() {

//      final EquipmentGroup tagGroup = (EquipmentGroup) (_equipmentGroupViewer.getStructuredSelection()).getFirstElement();
//
//      if (tagGroup == null) {
//         return;
//      }
//
//      _selectedTagGroup = tagGroup;
//
//      _isModified = true;
//
//      updateUI_TagViewer(tagGroup.tourTags);
//
//      enableControls();
   }

   private void onEquipmentItem_TableSelect(final SelectionEvent selectionEvent) {

      _tagViewerItem_IsChecked = selectionEvent.detail == SWT.CHECK;

      if (_tagViewerItem_IsChecked) {

         /*
          * Item can be null when <ctrl>+A is pressed !!!
          */
         final Widget item = selectionEvent.item;

         _tagViewerItem_Data = item.getData();
      }
   }

   private void onEquipmentItem_ViewerSelect(final SelectionChangedEvent event) {

      if (_tagViewerItem_IsKeyPressed) {

         // ignore when selected with keyboard

         // reset state
         _tagViewerItem_IsKeyPressed = false;

         return;
      }

      Object selection;

      if (_tagViewerItem_IsChecked) {

         // the checkbox is selected

         selection = _tagViewerItem_Data;

      } else {

         // the row is selected

         selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
      }

      if (selection instanceof final Equipment equipment) {

         // tag is selected

         // toggle tag
         if (_tagViewerItem_IsChecked == false) {

            // tag is selected and NOT the checkbox !!!

            final boolean isChecked = _equipmentViewer_Items.getChecked(equipment);

            _equipmentViewer_Items.setChecked(equipment, !isChecked);
         }

//         updateUI_Tags_From_TagIds();
      }
   }

   private void onTag_Filter() {

      // toggle tag filter
      _isShowOnlyCheckedTags = !_isShowOnlyCheckedTags;

//      updateUI_TagsAfterFiltering();
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

   }

   private void restoreStateBeforeUI() {

      _isShowOnlyCheckedTags = Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, false);
   }

   private void saveChanges() {

      if (_isModified) {

         _isModified = false;

         EquipmentGroupManager.saveState();

         // fire modify event
         TourManager.fireEvent(TourEventId.TAG_STRUCTURE_CHANGED);
      }

      saveState();
   }

   private void saveState() {

      _state.put(STATE_IS_SHOW_ONLY_TAGS_WHICH_ARE_CHECKED, _isShowOnlyCheckedTags);

      if (_selectedEquipmentGroup != null) {

         _state.put(STATE_SELECTED_TAG_GROUP, _selectedEquipmentGroup.name);
      }
   }

   private void updateUI_TopGridRowHeight() {

      final Point toolbarSize = _toolBarAllTags.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      final int toolbarHeight = toolbarSize.y;

      GridData gd = (GridData) _lblActionSpacer.getLayoutData();
      gd.heightHint = toolbarHeight;

      gd = (GridData) _containerEquipmentGroupHeader.getLayoutData();
      gd.heightHint = toolbarHeight;
   }

   private void updateViewers_All() {

      _equipmentViewer_Groups.setInput(new Object());

      _allEquipment = EquipmentManager.getAllEquipment_Name();
      _equipmentViewer_Items.setInput(this);
   }

}
